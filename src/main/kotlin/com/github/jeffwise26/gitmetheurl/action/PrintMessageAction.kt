package com.github.jeffwise26.gitmetheurl.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages

class PrintMessageAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        Messages.showMessageDialog(e.project, "Hello from git me the url!", "Message", Messages.getInformationIcon())
    }
}
