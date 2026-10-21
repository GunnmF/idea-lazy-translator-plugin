package cc.zxc.aitranslator.actions

import cc.zxc.aitranslator.utils.Naming
import com.intellij.notification.NotificationType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep

/**
 * 翻译变量名：选中中文 → AI 翻译 → 弹窗选择命名格式 → 替换。
 * 选中英文 → 直接替换为中文译文。
 */
class TranslateVariableAction : BaseTranslateAction() {

    override fun onTranslated(
        project: Project,
        editor: Editor,
        original: String,
        translated: String,
        concise: Boolean
    ) {
        if (!concise) {
            // 反向翻译（英文 → 中文）：不做命名格式化，直接替换
            replaceSelection(project, editor, translated)
            notify(project, "已替换为：${translated.take(50)}", NotificationType.INFORMATION)
            return
        }

        val items = Naming.previews(translated)
        val step = object : BaseListPopupStep<Pair<Naming.Format, String>>(
            "选择命名格式（原文：$original）", items
        ) {
            override fun getTextFor(value: Pair<Naming.Format, String>): String =
                "${value.second}   —  ${value.first.label}"

            override fun onChosen(
                selectedValue: Pair<Naming.Format, String>?,
                finalChoice: Boolean
            ): PopupStep<*>? {
                if (selectedValue != null) {
                    replaceSelection(project, editor, selectedValue.second)
                    notify(
                        project,
                        "已替换为 ${selectedValue.first.label}: ${selectedValue.second}",
                        NotificationType.INFORMATION
                    )
                }
                return FINAL_CHOICE
            }
        }
        JBPopupFactory.getInstance().createListPopup(step).showInBestPositionFor(editor)
    }
}
