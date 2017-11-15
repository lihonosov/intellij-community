// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.diff;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationStarterEx;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

/**
 * @author Konstantin Bulenkov
 */
@SuppressWarnings({"UseOfSystemOutOrSystemErr", "CallToPrintStackTrace"})
public abstract class ApplicationStarterBase extends ApplicationStarterEx {
  private final String myCommandName;
  private final int[] myArgsCount;

  protected ApplicationStarterBase(String commandName, int... possibleArgumentsCount) {
    myCommandName = commandName;
    myArgsCount = possibleArgumentsCount;
  }

  @Override
  public String getCommandName() {
    return myCommandName;
  }

  @Override
  public boolean isHeadless() {
    return false;
  }

  @Override
  public void processExternalCommandLine(String[] args, @Nullable String currentDirectory) {
    if (!checkArguments(args)) {
      Messages.showMessageDialog(getUsageMessage(), StringUtil.toTitleCase(getCommandName()), Messages.getInformationIcon());
      return;
    }
    try {
      processCommand(args, currentDirectory);
    }
    catch (Exception e) {
      Messages.showMessageDialog(String.format("Error showing %s: %s", getCommandName(), e.getMessage()),
                                 StringUtil.toTitleCase(getCommandName()),
                                 Messages.getErrorIcon());
    }
    finally {
      saveAll();
    }
  }

  protected static void saveAll() {
    FileDocumentManager.getInstance().saveAllDocuments();
    ApplicationManager.getApplication().saveSettings();
  }

  private boolean checkArguments(String[] args) {
    return Arrays.binarySearch(myArgsCount, args.length - 1) != -1 && getCommandName().equals(args[0]);
  }

  public abstract String getUsageMessage();

  protected abstract void processCommand(String[] args, @Nullable String currentDirectory) throws Exception;

  @Override
  public void premain(String[] args) {
    if (!checkArguments(args)) {
      System.err.println(getUsageMessage());
      System.exit(1);
    }
  }

  @Override
  public void main(String[] args) {
    try {
      processCommand(args, null);
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
    catch (Throwable t) {
      t.printStackTrace();
      System.exit(2);
    }
    finally {
      saveAll();
    }

    System.exit(0);
  }

  public static VirtualFile findOrCreateFile(String path, @Nullable String currentDirectory) throws IOException {
    final VirtualFile file = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(path));
    if (file == null) {
      boolean result = new File(path).createNewFile();
      if (result) {
        return findFile(path, currentDirectory);
      }
      else {
        throw new FileNotFoundException("Can't create file " + path);
      }
    }
    return file;
  }

  /**
   * Get direct from file because IDEA cache files(see #IDEA-81067)
   */
  public static String getText(VirtualFile file) throws IOException {
    FileInputStream inputStream = new FileInputStream(file.getPath());
    try {
      return StreamUtil.readText(inputStream);
    }
    finally {
      inputStream.close();
    }
  }

  public static class OperationFailedException extends IOException {
    public OperationFailedException(@NotNull String message) {
      super(message);
    }
  }

  @NotNull
  public static VirtualFile findFile(final String path, @Nullable String currentDirectory) throws OperationFailedException {
    File ioFile = new File(path);
    if (!ioFile.isAbsolute() && currentDirectory != null) {
      ioFile = new File(currentDirectory, path);
    }
    final VirtualFile file = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(ioFile);
    if (file == null) {
      throw new OperationFailedException("Can't find file " + path);
    }
    return file;
  }

  @Override
  public boolean canProcessExternalCommandLine() {
    return true;
  }
}
