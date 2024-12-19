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
    companion object {
        val GROUP = MyBundle.message("notificationGroup")
    }

    override fun actionPerformed(e: AnActionEvent) {
        val file = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.VIRTUAL_FILE)
        file ?: return
        val project = e.project
        project ?: return
        val gitUrl = gitGitHubUrl(project, file)
        val notification: Notification =
            when(gitUrl) {
                is GitHubUrlSuccess -> {
                    CopyPasteManager.getInstance().setContents(StringSelection(gitUrl.url));
                    Notification(
                        GROUP,
                        gitUrl.message,
                        NotificationType.INFORMATION
                    )
                }
                is GitHubUrlFail -> {
                    Notification(
                        GROUP,
                        gitUrl.message,
                        NotificationType.ERROR
                    )
                }

                is GitHubUrlWarn -> {
                    Notification(
                        GROUP,
                        gitUrl.message,
                        NotificationType.WARNING
                    )
                }
            }
        Notifications.Bus.notify(notification)
        Timer(1000)
        { notification.expire() }.start()
    }

    private fun gitGitHubUrl(project: Project, file: VirtualFile): GitHubUrl {
        val editor: Editor? = FileEditorManager.getInstance(project).selectedTextEditor
        editor ?: return GitHubUrlFail(MyBundle.message("notificationErrorEditor"))
        val currentFile = FileDocumentManager.getInstance().getFile(editor.document)
        val repository = GitUtil.getRepositoryManager(project).getRepositoryForFileQuick(currentFile)

        val remoteUrl = repository?.remotes?.firstOrNull()?.firstUrl
        remoteUrl ?: return GitHubUrlFail(MyBundle.message("notificationErrorRemote"))

        val lineNumber = editor.caretModel.logicalPosition.line

        val basePath = project.basePath
        basePath ?: return GitHubUrlFail(MyBundle.message("notificationErrorProject"))

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
}

sealed interface GitHubUrl
data class GitHubUrlSuccess(
    val url: String,
    val message: String
) : GitHubUrl

data class GitHubUrlFail(
    val message: String,
) : GitHubUrl

data class GitHubUrlWarn(
    val message: String,
) : GitHubUrl
