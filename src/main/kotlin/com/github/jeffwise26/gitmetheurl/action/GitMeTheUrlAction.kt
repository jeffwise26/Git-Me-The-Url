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
        val notification: Notification =
            try {
                val gitUrl = gitGitHubUrl(project, file)
                CopyPasteManager.getInstance().setContents(StringSelection(gitUrl));
                Notification(
                    MyBundle.message("notificationGroup"),
                    MyBundle.message("notificationMessage"),
                    NotificationType.INFORMATION
                )
            } catch (e: GitUrlParseError) {
                Notification(
                    MyBundle.message("notificationGroup"),
                    // todo bundle
                    e.message!!,
                    NotificationType.ERROR
                )
            } catch (e: GitUrlParseWarn) {
                Notification(
                    MyBundle.message("notificationGroup"),
                    // todo bundle
                    e.message!!,
                    NotificationType.WARNING
                )
            }
        Notifications.Bus.notify(notification)
        Timer(1000) { notification.expire() }.start()
    }

    private fun gitGitHubUrl(project: Project, file: VirtualFile): String {
        val editor: Editor? = FileEditorManager.getInstance(project).selectedTextEditor
        editor ?: throw GitUrlParseError("There is no editor environment")
        val currentFile = FileDocumentManager.getInstance().getFile(editor.document)
        val repository = GitUtil.getRepositoryManager(project).getRepositoryForFileQuick(currentFile)

        val remoteUrl = repository?.remotes?.firstOrNull()?.firstUrl
        remoteUrl ?: throw GitUrlParseError("Repository does not have remotes")

        val lineNumber = editor.caretModel.logicalPosition.line

        val basePath = project.basePath
        basePath ?: throw GitUrlParseError("Project does not have a base path")

        val filePath = file.path.substring(basePath.length)

        val currentBranch = repository.currentBranchName

        val remoteBranches = repository.branches.remoteBranches
        val isCurrentBranchOnRemote = remoteBranches.any {
            it.name.endsWith("/$currentBranch")
        }
        if (!isCurrentBranchOnRemote) {
            throw GitUrlParseError("Current branch is not on remote, url may not work")
        }

        val adjustedBase = remoteUrl
            .replace("github.com:", "github.com/")
            .replace(".git", "/blob/$currentBranch")
            .replace("git@", "https://www.")
        val githubUrl = "$adjustedBase$filePath#L${lineNumber + 1}"

        return githubUrl
    }
}

class GitUrlParseError(message: String) : Throwable(message)
class GitUrlParseWarn(message: String) : Throwable(message)
