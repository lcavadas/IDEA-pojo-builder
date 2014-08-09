package com.agentparadigm.builder;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;

public class BuilderCreator implements Runnable {

  String getMethodTemplate = "public static %s get() {return new %s();}";
  String buildMethodTemplate = "public %s build() {%s obj = new %s(); %s return obj;}";
  String withMethodTemplate = "public %s with%s(%s %s) {this.%s = %s; return this;}";
  String withListMethodTemplate = "public %s with%s(%s... %s) {if(this.%s == null){this.%s = new ArrayList<>();}this.%s.addAll(Arrays.asList(%s)); return this;}";
  String withMapMethodTemplate = "public %s with%s(%s key,%s value) {if(this.%s == null){this.%s = new HashMap<>();}this.%s.put(key, value); return this;}";

  private PsiDirectory psiDirectory;
  private PsiClass originalClass;
  private Project project;

  public BuilderCreator(Project project, PsiDirectory psiDirectory, PsiClass originalClass) {
    this.project = project;
    this.psiDirectory = psiDirectory;
    this.originalClass = originalClass;
  }

  @Override
  public void run() {
    PsiElementFactory elementFactory = PsiElementFactory.SERVICE.getInstance(project);
    PsiClass newClass = JavaDirectoryService.getInstance().createClass(psiDirectory, originalClass.getName() + "Builder");
    PsiJavaFile newFile = (PsiJavaFile) newClass.getContainingFile();
    PsiJavaFile originalFile = (PsiJavaFile) originalClass.getContainingFile();
    newFile.setPackageName(originalFile.getPackageName());

    PsiMethod constructor = elementFactory.createConstructor();
    newClass.add(constructor);

    newClass.add(elementFactory.createMethodFromText(
        String.format(
            getMethodTemplate,
            newClass.getName(),
            newClass.getName()
        ), null));

    for (PsiField psiField : originalClass.getFields()) {
      newClass.add(elementFactory.createField(psiField.getName(), psiField.getType()));

      String fieldCamelCase = getCamelCasedFieldName(psiField);

      newClass.add(elementFactory.createMethodFromText(
          String.format(
              withMethodTemplate,
              newClass.getName(),
              fieldCamelCase,
              psiField.getType().getCanonicalText(),
              psiField.getName(),
              psiField.getName(),
              psiField.getName()
          ), null));


      if (psiField.getType().isAssignableFrom(elementFactory.createTypeFromText(CommonClassNames.JAVA_UTIL_LIST, null))
          && psiField.getType().getPresentableText().contains("<")) { //only if it has a generic form
        newFile.importClass(JavaPsiFacade.getInstance(project).findClass("java.util.ArrayList", GlobalSearchScope.allScope(project)));
        newFile.importClass(JavaPsiFacade.getInstance(project).findClass("java.util.Arrays", GlobalSearchScope.allScope(project)));
        psiField.getType().getCanonicalText();
        newClass.add(elementFactory.createMethodFromText(
            String.format(
                withListMethodTemplate,
                newClass.getName(),
                fieldCamelCase,
                psiField.getType().getPresentableText().replaceFirst(".*<", "").replaceFirst(">.*", ""),
                psiField.getName(),
                psiField.getName(),
                psiField.getName(),
                psiField.getName(),
                psiField.getName()
            ), null));
      }


      //public %s with%s(%s key,%s value) {if(this.%s == null){this.%s = new HashMap<>();}this.%s.put(key, value); return this;}
      if (psiField.getType().isAssignableFrom(elementFactory.createTypeFromText(CommonClassNames.JAVA_UTIL_MAP, null))
          && psiField.getType().getPresentableText().contains("<")) { //only if it has a generic form
        newFile.importClass(JavaPsiFacade.getInstance(project).findClass("java.util.HashMap", GlobalSearchScope.allScope(project)));
        psiField.getType().getCanonicalText();
        String[] types = psiField.getType().getPresentableText().replaceFirst(".*<", "").replaceFirst(">.*", "").split(",");
        newClass.add(elementFactory.createMethodFromText(
            String.format(
                withMapMethodTemplate,
                newClass.getName(),
                fieldCamelCase,
                types[0],
                types[1],
                psiField.getName(),
                psiField.getName(),
                psiField.getName()
            ), null));
      }

      newFile.importClass(psiField.getContainingClass());
    }

    newClass.add(elementFactory.createMethodFromText(
        String.format(
            buildMethodTemplate,
            originalClass.getName(),
            originalClass.getName(),
            originalClass.getName(),
            generateSettersForFields(newClass)
        ), null));
  }

  private String getCamelCasedFieldName(PsiField psiField) {
    char[] chars = psiField.getName().toCharArray();
    chars[0] -= 32;
    return new String(chars);
  }

  private String generateSettersForFields(PsiClass newClass) {
    StringBuilder str = new StringBuilder();
    for (PsiField psiField : newClass.getFields()) {
      str.append("obj.set").append(getCamelCasedFieldName(psiField)).append("(").append(psiField.getName()).append(");");
    }
    return str.toString();
  }
}