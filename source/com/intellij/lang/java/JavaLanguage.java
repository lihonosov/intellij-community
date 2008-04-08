package com.intellij.lang.java;

import com.intellij.ide.highlighter.JavaFileHighlighter;
import com.intellij.lang.Language;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory;
import com.intellij.openapi.module.LanguageLevelUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.java.LanguageLevel;
import org.jetbrains.annotations.NotNull;

/**
 * Created by IntelliJ IDEA.
 * User: max
 * Date: Jan 22, 2005
 * Time: 11:16:59 PM
 * To change this template use File | Settings | File Templates.
 */
public class JavaLanguage extends Language {
  public JavaLanguage() {
    super("JAVA", "text/java", "application/x-java", "text/x-java");
    SyntaxHighlighterFactory.LANGUAGE_FACTORY.addExpicitExtension(this, new SyntaxHighlighterFactory() {
      @NotNull
      public SyntaxHighlighter getSyntaxHighlighter(final Project project, final VirtualFile virtualFile) {
        LanguageLevel languageLevel;
        if (project != null && virtualFile != null) {
          final Module module = ProjectRootManager.getInstance(project).getFileIndex().getModuleForFile(virtualFile);
          if (module != null) {
            languageLevel = LanguageLevelUtil.getEffectiveLanguageLevel(module);
          } else {
            languageLevel = LanguageLevel.HIGHEST;
          }
        } else {
          languageLevel = LanguageLevel.HIGHEST;
        }

        return new JavaFileHighlighter(languageLevel);
      }
    });
  }

  public String getDisplayName() {
    return "Java";
  }

  public boolean isCaseSensitive() {
    return true;
  }
}
