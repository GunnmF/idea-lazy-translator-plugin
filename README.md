# Lazy Translator（简单翻译）

一个 JetBrains IDE 插件：选中代码里的中文，一键翻译成英文变量名并直接替换；选中英文则反向翻译为中文。

## 功能

- **翻译变量名**（`Alt+T`）：选中中文 → 翻译 → 弹窗选择命名格式 → 替换
- **快速翻译替换**（`Alt+Shift+T`）：无弹窗，按默认命名格式直接替换
- 支持命名格式：`camelCase` / `PascalCase` / `snake_case` / `UPPER_SNAKE_CASE` / `kebab-case` / `raw`
- 选中英文反向翻译为中文，原样替换
- **翻译引擎二选一**：
  - **AI**：任意 OpenAI 兼容接口（OpenAI / DeepSeek / Moonshot / Ollama 等）
  - **LibreTranslate**：开源翻译服务（官方或自建实例）
- 设置页「测试连接」（带 loading，可取消）
- 文本只发往你自己配置的接口，插件不收集任何数据

## 使用

1. `Settings → Tools → Lazy Translator`：
   - 选「翻译引擎」
   - AI：填 Base URL / API Key / Model；LibreTranslate：填服务地址（自建实例 API Key 可留空）
   - 可点「测试连接」验证
2. 编辑器里选中要翻译的文本，右键：
   - `翻译变量名…`（`Alt+T`）——弹窗选格式
   - `快速翻译替换`（`Alt+Shift+T`）——直接用默认格式替换

## 技术栈

- **语言**：Kotlin
- **平台**：IntelliJ Platform SDK（兼容 2024.2+，IDEA / WebStorm / GoLand / PyCharm 等）
- **构建工具**：Gradle + IntelliJ Platform Gradle Plugin

## 项目结构

```
src/main/kotlin/cc/zxc/lazytranslator/
├── actions/
│   ├── BaseTranslateAction.kt        # Action 基类：取选区、判方向、后台翻译、错误通知
│   ├── TranslateVariableAction.kt    # 弹窗选命名格式后替换（Alt+T）
│   └── QuickTranslateAction.kt       # 默认格式直接替换（Alt+Shift+T）
├── services/TranslatorService.kt     # 翻译引擎：OpenAI 兼容接口 / LibreTranslate
├── utils/Naming.kt                   # 命名格式转换 + AI 输出清洗
└── settings/
    ├── TranslatorSettings.kt         # 配置持久化（lazyTranslator.xml）
    └── TranslatorConfigurable.kt     # Settings → Tools → Lazy Translator 设置页
```

## 开发环境

- JDK 17+
- Gradle 8+
- 任意 JetBrains IDE

## 快速开始

```bash
# 启动沙箱 IDE（首次会下载 IntelliJ 平台，较大）
./gradlew runIde

# 打插件包（产物在 build/distributions/*.zip，可在任意 JetBrains IDE 中安装）
# 本地打包版本号自动递增（version.properties 里的 buildNumber 每次 +1）
./gradlew buildPlugin
```

## 发版（GitHub Actions）

推 `main` 分支自动触发 `.github/workflows/release.yml`：

1. 计算版本号：`gradle.properties` 的 major.minor + CI 构建号（如 `0.2.7`）
2. 该版本 Release 已存在且有 zip 则跳过（防重复发版，挂了会自动补齐）
3. 构建插件 zip → 自动建 tag（指向被构建的 commit）→ 上传 Release

升 minor/major 版本：改 `gradle.properties` 里的 `pluginVersion` 即可。

## 参考

- [IntelliJ Platform SDK 文档](https://plugins.jetbrains.com/docs/intellij/)
- [LibreTranslate](https://libretranslate.com/) / [自建文档](https://github.com/LibreTranslate/LibreTranslate)
