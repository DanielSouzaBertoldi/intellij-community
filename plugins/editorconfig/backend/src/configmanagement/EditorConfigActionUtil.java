// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.editorconfig.configmanagement;

import com.intellij.application.options.CodeStyle;
import com.intellij.editorconfig.common.EditorConfigBundle;
import com.intellij.ide.actions.ShowSettingsUtilImpl;
import com.intellij.ide.actions.searcheverywhere.SearchEverywhereManager;
import com.intellij.ide.actions.searcheverywhere.SearchEverywhereManagerImpl;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import org.editorconfig.Utils;
import org.editorconfig.settings.EditorConfigSettings;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class EditorConfigActionUtil {

  public static AnAction[] createNavigationActions(@NotNull PsiFile file) {
    EditorConfigNavigationActionsFactory navigationActionsFactory =
      EditorConfigNavigationActionsFactory.Companion.getInstance(file);
    if (navigationActionsFactory == null) {
      return AnAction.EMPTY_ARRAY;
    }
    List<AnAction> actions = new ArrayList<>(navigationActionsFactory.getNavigationActions(file.getProject(), file.getVirtualFile()));
    return actions.toArray(AnAction.EMPTY_ARRAY);
  }

  @Contract("_, _ -> new")
  public static @NotNull AnAction createDisableAction(@NotNull Project project, @NotNull @Nls String message) {
    return DumbAwareAction.create(
      message,
      e -> {
        setEditorConfigEnabled(project, false);
      });
  }

  @Contract(" -> new")
  public static @NotNull AnAction createShowEditorConfigFilesAction() {
    return new DumbAwareAction(EditorConfigBundle.message("editor.config.files.show")) {
      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project != null) {
          showEditorConfigFiles(e.getProject(), e);
        }
      }
    };
  }

  public static void showEditorConfigFiles(@NotNull Project project, @NotNull AnActionEvent event) {
    SearchEverywhereManager seManager = SearchEverywhereManager.getInstance(project);
    String searchProviderID = SearchEverywhereManagerImpl.ALL_CONTRIBUTORS_GROUP_ID;
    if (seManager.isShown()) {
      if (!searchProviderID.equals(seManager.getSelectedTabID())) {
        seManager.setSelectedTabID(searchProviderID);
      }
    }
    seManager.show(searchProviderID, Utils.EDITOR_CONFIG_FILE_NAME, event);
  }


  public static void setEditorConfigEnabled(@NotNull Project project, boolean ecEnabled) {
    final EditorConfigSettings settings = CodeStyle.getSettings(project).getCustomSettings(EditorConfigSettings.class);
    if (settings.ENABLED == ecEnabled) return;
    settings.ENABLED = ecEnabled;
    CodeStyleSettingsManager.getInstance(project).notifyCodeStyleSettingsChanged();
  }
}