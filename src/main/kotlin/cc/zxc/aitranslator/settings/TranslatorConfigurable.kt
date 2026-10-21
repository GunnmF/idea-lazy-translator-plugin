package cc.zxc.aitranslator.settings

import cc.zxc.aitranslator.services.TranslatorService
import cc.zxc.aitranslator.utils.Naming
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.Messages
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import javax.swing.JButton
import javax.swing.JComponent

/**
 * Settings → Tools → Lazy Translator 设置页
 */
class TranslatorConfigurable : Configurable {

    private val state get() = TranslatorSettings.getInstance().state
    private var root: DialogPanel? = null
    private lateinit var testButton: JButton

    /** 进行中的测试请求，非空时按钮变为取消入口 */
    private var testFuture: CompletableFuture<String>? = null

    override fun getDisplayName(): String = "Lazy Translator"

    override fun createComponent(): JComponent {
        val s = state
        val formats = Naming.Format.entries.map { it.label }

        val p = panel {
            group("AI 服务配置（OpenAI 兼容接口）") {
                row("Base URL:") {
                    textField()
                        .bindText(s::baseUrl)
                        .align(Align.FILL)
                        .comment("例：https://api.openai.com/v1 、https://api.deepseek.com/v1 、https://api.moonshot.cn/v1")
                }
                row("API Key:") {
                    passwordField()
                        .bindText(s::apiKey)
                        .align(Align.FILL)
                }
                row("Model:") {
                    textField()
                        .bindText(s::model)
                        .align(Align.FILL)
                        .comment("如 gpt-4o-mini 、deepseek-chat 、moonshot-v1-8k")
                }
                row {
                    testButton = button("测试连接") { onTestClicked() }.component
                }
            }
            group("翻译规则") {
                row("默认命名格式:") {
                    comboBox(formats)
                        .bindItem({ s.defaultFormat }, { s.defaultFormat = it ?: "camelCase" })
                        .comment("「快速翻译替换」直接使用的格式")
                }
                row("温度:") {
                    textField()
                        .bindText({ s.temperature.toString() }, { s.temperature = it.toDoubleOrNull() ?: 0.1 })
                        .comment("0~1，越低输出越稳定，翻译建议 0.1")
                }
                row("超时（秒）:") {
                    textField()
                        .bindText({ s.timeoutSeconds.toString() }, { s.timeoutSeconds = it.toIntOrNull() ?: 60 })
                }
            }
        }
        root = p
        return p
    }

    // ---------- 测试连接（带 loading + 可取消） ----------

    private fun onTestClicked() {
        testFuture?.let {
            it.cancel(true)
            return
        }

        root?.apply() // 先保存当前输入再测
        setBusy(busy = true)

        val f = TranslatorService.testConnectionAsync()
        testFuture = f
        f.whenComplete { reply, error ->
            invokeLater {
                if (testFuture !== f) return@invokeLater // 已被更新的请求取代
                testFuture = null
                setBusy(busy = false)
                when {
                    error == null -> Messages.showInfoMessage("连接成功，模型返回：$reply", "Lazy Translator")
                    error.isCancel() -> balloon("已取消测试连接。", NotificationType.INFORMATION)
                    else -> Messages.showErrorDialog("连接失败：${rootMessage(error)}", "Lazy Translator")
                }
            }
        }
    }

    private fun setBusy(busy: Boolean) {
        testButton.text = if (busy) "取消测试" else "测试连接"
        testButton.icon = if (busy) AnimatedIcon.Default() else null
    }

    private fun invokeLater(block: () -> Unit) =
        ApplicationManager.getApplication().invokeLater { block() }

    private fun Throwable?.isCancel(): Boolean {
        var t: Throwable? = this
        while (t != null) {
            if (t is CancellationException) return true
            t = t.cause
        }
        return false
    }

    private fun rootMessage(t: Throwable): String {
        var e: Throwable = t
        while (e.cause != null) e = e.cause!!
        return e.message ?: e.toString()
    }

    private fun balloon(content: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Lazy Translator")
            .createNotification(content, type)
            .notify(null)
    }

    // ---------- Configurable 生命周期 ----------

    override fun isModified(): Boolean = root?.isModified() ?: false

    override fun apply() {
        root?.apply()
    }

    override fun reset() {
        root?.reset()
    }

    override fun disposeUIResources() {
        testFuture?.cancel(true)
        testFuture = null
        root = null
    }
}
