// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.vcs.readOnlyHandler;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ListWithSelection;

import javax.swing.*;

final class FileInfo {
  private final VirtualFile myFile;
  private final Icon myIcon;
  private final ListWithSelection<HandleType> myHandleType = new ListWithSelection<>();

  FileInfo(VirtualFile file, Icon icon, Project project) {
    myFile = file;
    myIcon = icon;
    myHandleType.add(HandleType.USE_FILE_SYSTEM);
    myHandleType.selectFirst();
    for(HandleTypeFactory factory: HandleTypeFactory.EP_NAME.getExtensions(project)) {
      final HandleType handleType = factory.createHandleType(file);
      if (handleType != null) {
        myHandleType.add(handleType);
        myHandleType.select(handleType);
      }
    }
  }

  public VirtualFile getFile() {
    return myFile;
  }

  Icon getIcon() {
    return myIcon;
  }

  public HandleType getSelectedHandleType() {
    return myHandleType.getSelection();
  }

  public boolean hasVersionControl() {
    return myHandleType.size() > 1;
  }

  public ListWithSelection<HandleType> getHandleType(){
    return myHandleType;
  }
}
