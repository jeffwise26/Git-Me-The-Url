package com.github.jeffwise26.gitmetheurl.action

import com.github.jeffwise26.gitmetheurl.MyBundle
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import git4idea.GitUtil
import java.awt.datatransfer.StringSelection
import javax.swing.Timer

class GitMeTheUrlAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val file = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.VIRTUAL_FILE)
        file ?: return
        val project = e.project
        project ?: return
        val gitUrl = gitGitHubUrl(project, file)
        CopyPasteManager.getInstance().setContents(StringSelection(gitUrl));
        val notification = Notification(
            MyBundle.message("notificationGroup"),
            MyBundle.message("notificationMessage"),
            NotificationType.INFORMATION
        )
        Notifications.Bus.notify(notification)
        Timer(1000) { notification.expire() }.start()
    }

    fun gitGitHubUrl(project: Project, file: VirtualFile): String {
        val editor: Editor? = FileEditorManager.getInstance(project).selectedTextEditor

        val currentFile = FileDocumentManager.getInstance().getFile(editor!!.getDocument())
        val repository = GitUtil.getRepositoryManager(project).getRepositoryForFileQuick(currentFile)
        return repository!!.remotes.joinToString(",")
    }
}
