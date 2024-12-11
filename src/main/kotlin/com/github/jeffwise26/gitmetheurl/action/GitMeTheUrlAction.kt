package com.github.jeffwise26.gitmetheurl.action

import com.github.jeffwise26.gitmetheurl.MyBundle
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import javax.swing.Timer

class GitMeTheUrlAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val notification = Notification(
            MyBundle.message("notificationGroup"),
            MyBundle.message("notificationMessage"),
            NotificationType.INFORMATION
        )
        Notifications.Bus.notify(notification)
        Timer(1000) { notification.expire() }.start()
    }
}
