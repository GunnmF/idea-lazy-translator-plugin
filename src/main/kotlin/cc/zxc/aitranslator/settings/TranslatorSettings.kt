package cc.zxc.aitranslator.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

/**
 * AI 翻译配置，持久化到 aiTranslator.xml
 */
@Service(Service.Level.APP)
@State(name = "AiTranslatorSettings", storages = [Storage("aiTranslator.xml")])
class TranslatorSettings : PersistentStateComponent<TranslatorSettings.State> {

    class State {
        /** OpenAI 兼容接口地址，如 https://api.openai.com/v1 、https://api.deepseek.com/v1 */
        var baseUrl: String = "https://api.openai.com/v1"
        var apiKey: String = ""
        var model: String = "gpt-4o-mini"

        /** 默认命名格式（快速替换时使用）：camelCase / PascalCase / snake_case / UPPER_SNAKE_CASE / kebab-case / raw */
        var defaultFormat: String = "camelCase"

        /** 模型温度，越低越稳定 */
        var temperature: Double = 0.1

        /** 请求超时时间（秒） */
        var timeoutSeconds: Int = 60
    }

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    companion object {
        fun getInstance(): TranslatorSettings =
            ApplicationManager.getApplication().getService(TranslatorSettings::class.java)
    }
}
