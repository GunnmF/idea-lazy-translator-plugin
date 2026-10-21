package cc.zxc.aitranslator.actions

import cc.zxc.aitranslator.utils.Naming
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project

/**
 * 翻译 Action 基类：负责取选区、判方向、后台翻译、错误通知、替换文本。
 * 子类只需实现翻译完成后的处理（弹窗选格式 / 直接替换）。
 */
abstract class BaseTranslateAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabledAndVisible =
            e.project != null && editor != null && editor.selectionModel.hasSelection()
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val selection = editor.selectionModel
        val text = selection.selectedText?.trim()
        if (text.isNullOrEmpty()) {
            notify(project, "请先选中要翻译的文本", NotificationType.WARNING)
            return
        }

        // 含中文 → 变量名场景（译为简短英文）；否则 → 反向翻译为中文
        val concise = Naming.containsCjk(text)

        cc.zxc.aitranslator.services.TranslatorService.translateAsync(text, concise)
            .whenComplete { translated, error ->
                ApplicationManager.getApplication().invokeLater {
                    when {
                        error != null -> notify(
                            project,
                            "翻译失败：${rootMessage(error)}",
                            NotificationType.ERROR
                        )

                        translated.isNullOrBlank() -> notify(
                            project,
                            "翻译结果为空",
                            NotificationType.WARNING
                        )

                        else -> onTranslated(project, editor, text, translated, concise)
                    }
                }
            }
    }

    /** 翻译成功后的处理，由子类实现 */
    protected abstract fun onTranslated(
        project: Project,
        editor: Editor,
        original: String,
        translated: String,
        concise: Boolean
    )

    /** 用新文本替换当前选区 */
    protected fun replaceSelection(project: Project, editor: Editor, newText: String) {
        val selection = editor.selectionModel
        if (!selection.hasSelection()) return
        WriteCommandAction.runWriteCommandAction(project, "Lazy Translate", null, Runnable {
            editor.document.replaceString(selection.selectionStart, selection.selectionEnd, newText)
            selection.removeSelection()
        })
    }

    protected fun notify(project: Project, content: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Lazy Translator")
            .createNotification(content, type)
            .notify(project)
    }

    private fun rootMessage(t: Throwable): String {
        var e: Throwable = t
        while (e.cause != null) e = e.cause!!
        return e.message ?: e.toString()
    }
}
