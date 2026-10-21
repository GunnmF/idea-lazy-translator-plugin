package cc.zxc.lazytranslator.actions

import cc.zxc.lazytranslator.settings.TranslatorSettings
import cc.zxc.lazytranslator.utils.Naming
import com.intellij.notification.NotificationType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project

/**
 * 快速翻译替换：无弹窗，按设置里的默认命名格式直接替换。
 */
class QuickTranslateAction : BaseTranslateAction() {

    override fun onTranslated(
        project: Project,
        editor: Editor,
        original: String,
        translated: String,
        concise: Boolean
    ) {
        val output = if (!concise) {
            // 反向翻译（英文 → 中文）：原样替换
            translated
        } else {
            val format = Naming.Format.of(TranslatorSettings.getInstance().state.defaultFormat)
            if (format == Naming.Format.RAW) translated
            else Naming.toFormat(Naming.extractWords(translated), format)
        }

        replaceSelection(project, editor, output)
        notify(project, "已替换为：${output.take(50)}", NotificationType.INFORMATION)
    }
}
