// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.internal.statistic.eventLog.validator.storage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.intellij.internal.statistic.config.SerializationHelper;
import com.intellij.internal.statistic.eventLog.EventLogBuild;
import com.intellij.internal.statistic.eventLog.EventLogConfiguration;
import com.intellij.internal.statistic.eventLog.StatisticsEventLogProviderUtil;
import com.intellij.internal.statistic.eventLog.validator.IntellijSensitiveDataValidator;
import com.intellij.internal.statistic.eventLog.validator.rules.beans.EventGroupRules;
import com.intellij.internal.statistic.eventLog.validator.storage.persistence.EventLogMetadataPersistence;
import com.intellij.internal.statistic.eventLog.validator.storage.persistence.EventLogTestMetadataPersistence;
import com.jetbrains.fus.reporting.model.metadata.EventGroupRemoteDescriptors;
import com.jetbrains.fus.reporting.model.metadata.EventGroupRemoteDescriptors.EventGroupRemoteDescriptor;
import com.jetbrains.fus.reporting.model.metadata.EventGroupRemoteDescriptors.GroupRemoteRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public final class ValidationTestRulesPersistedStorage implements IntellijValidationRulesStorage {
  private final ConcurrentMap<String, EventGroupRules> eventsValidators = new ConcurrentHashMap<>();
  private final Object myLock = new Object();
  private final @NotNull EventLogTestMetadataPersistence myTestMetadataPersistence;
  private final @NotNull EventLogMetadataPersistence myMetadataPersistence;
  private final @NotNull String myRecorderId;
  private final @NotNull AtomicBoolean myIsInitialized;

  ValidationTestRulesPersistedStorage(@NotNull String recorderId) {
    myRecorderId = recorderId;
    myIsInitialized = new AtomicBoolean(false);
    myTestMetadataPersistence = new EventLogTestMetadataPersistence(recorderId);
    myMetadataPersistence = new EventLogMetadataPersistence(recorderId);
    updateValidators();
  }

  @Override
  public @Nullable EventGroupRules getGroupRules(@NotNull String groupId) {
    return eventsValidators.get(groupId);
  }

  @Override
  public void update() {
    updateValidators();
  }

  @Override
  public void reload() {
    updateValidators();
  }

  @Override
  public boolean isUnreachable() {
    return !myIsInitialized.get();
  }

  private void updateValidators() {
    synchronized (myLock) {
      eventsValidators.clear();
      myIsInitialized.set(false);
      EventGroupRemoteDescriptors productionGroups = EventLogTestMetadataPersistence.loadCachedEventGroupsSchemes(myMetadataPersistence);
      EventGroupRemoteDescriptors testGroups = EventLogTestMetadataPersistence.loadCachedEventGroupsSchemes(myTestMetadataPersistence);
      final Map<String, EventGroupRules> result = createValidators(testGroups, productionGroups.rules);

      eventsValidators.putAll(result);
      myIsInitialized.set(true);
    }
  }

  public @NotNull EventGroupRemoteDescriptors loadProductionGroups() {
    return EventLogTestMetadataPersistence.loadCachedEventGroupsSchemes(myMetadataPersistence);
  }

  private @NotNull Map<String, EventGroupRules> createValidators(@NotNull EventGroupRemoteDescriptors groups,
                                                                 @Nullable EventGroupRemoteDescriptors.GroupRemoteRule productionRules) {
    final GroupRemoteRule rules = merge(groups.rules, productionRules);
    GlobalRulesHolder globalRulesHolder = new GlobalRulesHolder(rules);
    final EventLogBuild build = EventLogBuild.fromString(EventLogConfiguration.getInstance().getBuild());
    return ValidationRulesPersistedStorage.createValidators(build, groups, globalRulesHolder, myRecorderId);
  }

  public void addTestGroup(@NotNull GroupValidationTestRule group) throws IOException {
    EventLogTestMetadataPersistence.addTestGroup(myRecorderId, group);
    updateValidators();
  }

  private void cleanup() {
    synchronized (myLock) {
      eventsValidators.clear();
      myTestMetadataPersistence.cleanup();
    }
  }

  public @NotNull List<GroupValidationTestRule> loadValidationTestRules() throws JsonProcessingException {
    ArrayList<EventGroupRemoteDescriptor> testGroupsSchemes =
      EventLogTestMetadataPersistence.loadCachedEventGroupsSchemes(myTestMetadataPersistence).groups;
    ArrayList<GroupValidationTestRule> groups = new ArrayList<>();
    for (EventGroupRemoteDescriptor group : testGroupsSchemes) {
      if (group.id == null || group.rules == null || group.rules.event_id == null) continue;
      Set<String> eventIds = group.rules.event_id;
      if (eventIds.contains(EventLogTestMetadataPersistence.TEST_RULE)) {
        groups.add(new GroupValidationTestRule(group.id, false));
      }
      else {
        groups.add(new GroupValidationTestRule(group.id, true,
                                               SerializationHelper.INSTANCE.serialize(group.rules)));
      }
    }
    return groups;
  }

  public void updateTestGroups(@NotNull List<GroupValidationTestRule> groups) throws IOException {
    myTestMetadataPersistence.updateTestGroups(groups);
    updateValidators();
  }

  private static @Nullable GroupRemoteRule merge(@Nullable GroupRemoteRule testRules, @Nullable GroupRemoteRule productionTestRules) {
    if (testRules == null) return productionTestRules;
    if (productionTestRules == null) return testRules;

    final GroupRemoteRule rule = new GroupRemoteRule();
    copyRules(rule, productionTestRules);
    copyRules(rule, testRules);
    return rule;
  }

  private static void copyRules(@NotNull GroupRemoteRule to, @NotNull GroupRemoteRule from) {
    if (to.getEnums() == null) {
      to.setEnums(new HashMap<>());
    }
    if (to.regexps == null) {
      to.regexps = new HashMap<>();
    }

    if (from.getEnums() != null) {
      to.getEnums().putAll(from.getEnums());
    }
    if (from.regexps != null) {
      to.regexps.putAll(from.regexps);
    }
  }

  public static void cleanupAll() {
    List<String> recorders = StatisticsEventLogProviderUtil.getEventLogProviders().stream().
      filter(provider -> provider.isRecordEnabled()).
      map(provider -> provider.getRecorderId()).
      collect(Collectors.toList());
    cleanupAll(recorders);
  }

  public static void cleanupAll(List<String> recorders) {
    for (String recorderId : recorders) {
      ValidationTestRulesPersistedStorage testStorage = getTestStorage(recorderId, false);
      if (testStorage != null) {
        testStorage.cleanup();
      }
    }
  }

  public static @Nullable ValidationTestRulesPersistedStorage getTestStorage(@NotNull String recorderId, boolean initIfNeeded) {
    IntellijSensitiveDataValidator validator =
      initIfNeeded ? IntellijSensitiveDataValidator.getInstance(recorderId) : IntellijSensitiveDataValidator.getIfInitialized(recorderId);
    IntellijValidationRulesStorage storage = validator != null ? validator.getValidationRulesStorage() : null;
    return storage instanceof ValidationTestRulesStorageHolder ? ((ValidationTestRulesStorageHolder)storage).getTestGroupStorage() : null;
  }

  public int size() {
    synchronized (myLock) {
      return eventsValidators.size();
    }
  }
}
