package com.intellij.xdebugger.impl.frame

import com.intellij.ide.dnd.DnDNativeTarget
import com.intellij.ide.util.PropertiesComponent
import com.intellij.ui.OnePixelSplitter
import com.intellij.util.ui.components.BorderLayoutPanel
import com.intellij.xdebugger.impl.XDebugSessionImpl
import com.intellij.xdebugger.impl.ui.XDebugSessionTabCustomizer
import javax.swing.JComponent
import javax.swing.JPanel

class XSplitterWatchesViewImpl(
  session: XDebugSessionImpl,
  watchesInVariables: Boolean,
  isVertical: Boolean,
  withToolbar: Boolean
) : XWatchesViewImpl(session, watchesInVariables, isVertical, withToolbar), DnDNativeTarget, XWatchesView {

  companion object {
    const val proportionKey = "debugger.immediate.window.in.watches.proportion.key"
  }

  override fun createMainPanel(localsPanelComponent: JComponent): JPanel {
    val session = mySession.get() ?: error("Not null session is expected here")
    val bottomLocalsComponentProvider = (session.debugProcess as? XDebugSessionTabCustomizer)?.bottomLocalsComponentProvider
                                        ?: error("BottomLocalsComponentProvider is not implemented to use SplitterWatchesVariablesView")

    val evaluatorComponent = bottomLocalsComponentProvider.createBottomLocalsComponent()
    val splitter = OnePixelSplitter(true, proportionKey, 0.01f, 0.99f)

    splitter.firstComponent = localsPanelComponent

    if (PropertiesComponent.getInstance().getBoolean("debugger.immediate.window.in.watches", true))
      splitter.secondComponent = evaluatorComponent

    return BorderLayoutPanel().addToCenter(splitter)
  }
}