package cc.zxc.lazytranslator.utils

/**
 * 命名格式转换工具（移植自 vscode-ai-translator 的 naming.ts）
 *
 * 支持：
 * - camelCase        小驼峰：userName
 * - PascalCase       大驼峰：UserName
 * - snake_case       下划线：user_name
 * - UPPER_SNAKE_CASE 大写下划线：USER_NAME
 * - kebab-case       中横线：user-name
 * - raw              原样输出
 */
object Naming {

    enum class Format(val label: String) {
        CAMEL("camelCase"),
        PASCAL("PascalCase"),
        SNAKE("snake_case"),
        UPPER_SNAKE("UPPER_SNAKE_CASE"),
        KEBAB("kebab-case"),
        RAW("raw");

        companion object {
            fun of(label: String): Format = entries.firstOrNull { it.label == label } ?: CAMEL
        }
    }

    /** 匹配任何「单词片段」：英文单词、数字、单个汉字 */
    private val TOKEN_REGEX = Regex("[A-Za-z0-9]+|[一-龥]")

    /** 是否含中文 */
    fun containsCjk(input: String): Boolean = input.any { it in '一'..'龥' }

    /**
     * 将词数组转成目标命名格式
     */
    fun toFormat(words: List<String>, format: Format): String {
        val cleaned = words.map { it.lowercase().trim() }.filter { it.isNotEmpty() }
        if (cleaned.isEmpty()) return ""

        fun String.cap() = replaceFirstChar { it.uppercase() }

        return when (format) {
            Format.CAMEL -> cleaned.first() + cleaned.drop(1).joinToString("") { it.cap() }
            Format.PASCAL -> cleaned.joinToString("") { it.cap() }
            Format.SNAKE -> cleaned.joinToString("_")
            Format.UPPER_SNAKE -> cleaned.joinToString("_").uppercase()
            Format.KEBAB -> cleaned.joinToString("-")
            Format.RAW -> cleaned.joinToString(" ")
        }
    }

    /**
     * 从 AI 返回的可能含空格/换行/标点的结果中提取有效词
     *
     * AI 经常返回 "User Name" / "user name" / "user_name" 等多种形式，统一提取单词。
     */
    fun extractWords(aiOutput: String): List<String> =
        aiOutput
            .replace(Regex("[`*_~#\\[\\](){}|\\\\]"), " ")
            .split(Regex("[\\s,;.，。；、!?/\\\\]+"))
            .flatMap { part -> part.split(Regex("(?=[A-Z])|[\\s_\\-]+")) }
            .map { it.trim() }
            .filter { it.isNotEmpty() && TOKEN_REGEX.containsMatchIn(it) }

    /**
     * 获取所有命名格式的预览（用于弹窗选择显示）
     *
     * @return (格式, 预览文本) 列表，raw 直接使用 AI 原样输出
     */
    fun previews(aiOutput: String): List<Pair<Format, String>> {
        val words = extractWords(aiOutput)
        return Format.entries.map { f ->
            f to if (f == Format.RAW) aiOutput.trim() else toFormat(words, f)
        }
    }
}
