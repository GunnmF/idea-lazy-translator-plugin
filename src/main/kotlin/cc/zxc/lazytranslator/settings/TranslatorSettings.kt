package cc.zxc.lazytranslator.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

/**
 * 简单翻译（Lazy Translator）配置，持久化到 lazyTranslator.xml
 */
@Service(Service.Level.APP)
@State(name = "LazyTranslatorSettings", storages = [Storage("lazyTranslator.xml")])
class TranslatorSettings : PersistentStateComponent<TranslatorSettings.State> {

    class State {
        /** 翻译引擎：AI（OpenAI 兼容）或 LibreTranslate，二选一 */
        var engineType: String = ENGINE_AI

        /** OpenAI 兼容接口地址，如 https://api.openai.com/v1 、https://api.deepseek.com/v1 */
        var baseUrl: String = "https://api.openai.com/v1"
        var apiKey: String = ""
        var model: String = "gpt-4o-mini"

        /** LibreTranslate 服务地址 */
        var libreBaseUrl: String = "https://libretranslate.com"

        /** LibreTranslate API Key（官方实例需要，自建通常可留空） */
        var libreApiKey: String = ""

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
        const val ENGINE_AI = "AI"
        const val ENGINE_LIBRE = "LibreTranslate"

        fun getInstance(): TranslatorSettings =
            ApplicationManager.getApplication().getService(TranslatorSettings::class.java)
    }
}
