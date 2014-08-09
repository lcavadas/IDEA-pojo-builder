package com.agentparadigm.builder;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;

public class GeneratorPsiVisitor extends PsiElementVisitor {
  Project project;
  PsiFile psiFile;

  public GeneratorPsiVisitor(Project project, PsiFile psiFile) {
    this.project = project;
    this.psiFile = psiFile;
  }

  @Override
  public void visitElement(PsiElement element) {
    if (element instanceof PsiClass) {
      if (project == null) {
        return;
      }
      PsiDocumentManager.getInstance(project).commitAllDocuments();
      if (element instanceof PsiClass && psiFile != null) {
        process(project, (PsiClass) element, psiFile);
      }
    }
  }

  private void process(Project project, PsiClass psiClass, PsiFile psiFile) {
    if (psiClass.isInterface()) {
      return;
    }

    PsiFile psiJavaFile = psiClass.getContainingFile();
    PsiDirectory psiDirectory = psiJavaFile.getContainingDirectory();

    if (psiDirectory == null || !psiDirectory.isWritable()) {
      psiDirectory = psiFile.getContainingDirectory();
    }

    if (psiDirectory == null || !psiDirectory.isWritable()) {
      return;
    }

    createBuilder(project, psiClass, psiDirectory);
  }

  private void createBuilder(Project project, final PsiClass psiClass, final PsiDirectory psiDirectory) {
    psiDirectory.checkCreateFile(psiClass.getName() + "Builder.java");
    final BuilderCreator runnable = new BuilderCreator(project, psiDirectory, psiClass);
    CommandProcessor.getInstance().executeCommand(project,
        new Runnable() {
          public void run() {
            ApplicationManager.getApplication().runWriteAction(runnable);
          }
        }, "Create Class Builder", null);
  }
}
