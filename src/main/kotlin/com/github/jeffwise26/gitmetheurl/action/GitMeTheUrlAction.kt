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
        val gitUrl = gitGitHubUrl(
            e.project,
            e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.VIRTUAL_FILE)
        )
        val notification: Notification = buildNotification(gitUrl)
        when (gitUrl) {
            is GitHubUrlSuccess -> CopyPasteManager.getInstance().setContents(StringSelection(gitUrl.url))
            else -> Unit
        }

        Notifications.Bus.notify(notification)
        Timer(1000)
        { notification.expire() }.start()
    }

    private fun buildNotification(gitUrl: GitHubUrl): Notification {
        val notificationType = when (gitUrl) {
            is GitHubUrlSuccess -> NotificationType.INFORMATION
            is GitHubUrlFail -> NotificationType.ERROR
            is GitHubUrlWarn -> NotificationType.WARNING
        }
        return Notification(
            GROUP,
            gitUrl.message,
            notificationType
        )
    }

    private fun gitGitHubUrl(project: Project?, file: VirtualFile?): GitHubUrl {
        file ?: return GitHubUrlFail(MyBundle.message("notificationErrorFile"))
        project ?: return GitHubUrlFail(MyBundle.message("notificationErrorProject"))

        val editor: Editor? = FileEditorManager.getInstance(project).selectedTextEditor
        editor ?: return GitHubUrlFail(MyBundle.message("notificationErrorEditor"))
        val currentFile = FileDocumentManager.getInstance().getFile(editor.document)
        val repository = GitUtil.getRepositoryManager(project).getRepositoryForFileQuick(currentFile)

        val remoteUrl = repository?.remotes?.firstOrNull()?.firstUrl
        remoteUrl ?: return GitHubUrlFail(MyBundle.message("notificationErrorRemote"))

        val lineNumber = editor.caretModel.logicalPosition.line

        val basePath = project.basePath
        basePath ?: return GitHubUrlFail(MyBundle.message("notificationErrorProjectBasePath"))

        val filePath = file.path.substring(basePath.length)

        val currentBranch = repository.currentBranchName

        val remoteBranches = repository.branches.remoteBranches
        val isCurrentBranchOnRemote = remoteBranches.any {
            it.name.endsWith("/$currentBranch")
        }
        if (!isCurrentBranchOnRemote) {
            return GitHubUrlFail(MyBundle.message("notificationWarnRemote"))
        }

        val adjustedBase = remoteUrl
            .replace("github.com:", "github.com/")
            .replace(".git", "/blob/$currentBranch")
            .replace("git@", "https://www.")
        val githubUrl = "$adjustedBase$filePath#L${lineNumber + 1}"

        return GitHubUrlSuccess(githubUrl, MyBundle.message("notificationMessage"))
    }

    companion object {
        val GROUP = MyBundle.message("notificationGroup")
    }
}

sealed class GitHubUrl(
    open val message: String,
)

data class GitHubUrlSuccess(
    val url: String,
    override val message: String
) : GitHubUrl(message)

data class GitHubUrlFail(
    override val message: String,
) : GitHubUrl(message)

data class GitHubUrlWarn(
    override val message: String,
) : GitHubUrl(message)
