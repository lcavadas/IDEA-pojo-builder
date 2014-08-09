package com.agentparadigm.builder;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;

import java.util.ArrayList;
import java.util.List;

public class GenerateBuilderAction extends AnAction {
  public void actionPerformed(AnActionEvent event) {
    final Project project = event.getData(PlatformDataKeys.PROJECT);
    if (project == null) {
      return;
    }
    VirtualFile[] result = event.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY);
    for (VirtualFile virtualFile : result) {
      PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
      psiFile.acceptChildren(new GeneratorPsiVisitor(project, psiFile));
    }
  }
}
