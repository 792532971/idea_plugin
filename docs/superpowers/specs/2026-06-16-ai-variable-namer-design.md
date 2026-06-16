# AI Variable Namer Plugin — Design Spec

## 1. Purpose
为 PhpStorm 2026.1（基于 IntelliJ Platform）开发一个插件，通过侧边 Tool Window 将中文描述转换为英文变量名，支持多种命名风格。

## 2. Scope

### 2.1 Included
- 侧边 Tool Window「AI Variable Namer」。
- 「生成」标签页：输入中文描述，选择命名风格，调用 LLM 生成变量名，展示结果并支持复制。
- 「设置」标签页：配置 LLM 供应商、API Key、请求地址、模型名称、Temperature、默认命名风格。
- 配置持久化。
- 网络请求错误与模型返回异常的处理。

### 2.2 Not Included
- 编辑器右键菜单/快捷键触发。
- 多种 API 格式兼容（仅支持 Claude Messages API 兼容端点）。
- 批量生成或历史记录。

## 3. UI Design

### 3.1 Tool Window 标签页
1. **生成**
   - 多行文本框：中文描述。
   - 单选按钮组：camelCase、snake_case、PascalCase、SCREAMING_SNAKE_CASE。
   - 「生成变量名」按钮。
   - 结果显示区域。
   - 「复制到剪贴板」按钮。
2. **设置**
   - 供应商名称（文本）。
   - API Key（密码框）。
   - 请求地址（文本，兼容 Claude API，提示不以 `/` 结尾）。
   - 模型名称（文本）。
   - Temperature（数字输入，范围 0–1，默认 0.2）。
   - 默认命名风格（单选按钮组）。
   - 「保存」和「测试连接」按钮。

## 4. Architecture

```
VariableNamerToolWindowFactory
├── GeneratePanel
│   └── AiNamingService
│       └── Claude-compatible HTTP call
└── SettingsPanel
    └── PluginSettings (PersistentStateComponent)

NamingStyle (enum)
```

## 5. Data Flow

1. 用户在生成页输入中文描述并选择命名风格。
2. `GeneratePanel` 从 `PluginSettings` 读取 API 配置。
3. 调用 `AiNamingService.generateEnglishWords(chinese)`。
4. 服务构造 Claude Messages API 请求，system prompt 要求只返回空格分隔的英文单词。
5. 解析响应，提取单词列表。
6. 使用 `NamingStyle.convert(words)` 生成最终变量名。
7. 在生成页展示结果，用户可复制。

## 6. Key Components

### 6.1 NamingStyle
```kotlin
enum class NamingStyle {
    CAMEL_CASE,
    SNAKE_CASE,
    PASCAL_CASE,
    SCREAMING_SNAKE_CASE;

    fun convert(words: List<String>): String
}
```

### 6.2 PluginSettings
基于 `PersistentStateComponent`，保存：
- `providerName`
- `apiKey`
- `baseUrl`
- `modelName`
- `temperature`
- `defaultNamingStyle`

### 6.3 AiNamingService
- 依赖 OkHttp。
- POST `{baseUrl}/messages`。
- 请求体包含 `model`、`messages`、`max_tokens`、`temperature`、`system`。
- 解析响应 JSON 中的 `content[0].text`。
- 将文本按空格/标点拆分为单词列表。

## 7. Error Handling

| 场景 | 行为 |
|------|------|
| API Key 或地址为空 | 提示用户先完成设置 |
| 网络超时/失败 | 显示具体错误信息 |
| 模型返回非单词内容 | 尝试清理；清理失败则提示重试 |
| 返回空结果 | 提示「未能生成有效结果，请调整描述」 |

## 8. Dependencies

- `com.squareup.okhttp3:okhttp:4.12.0`
- `com.google.code.gson:gson:2.10.1`

## 9. Compatibility

- 目标平台：PhpStorm 2026.1（IntelliJ Platform 2026.1）。
- 使用 Kotlin 1.9.24（与现有模板一致）。
- JVM target 17。

## 10. Verification Criteria

- [ ] Tool Window 在 PhpStorm 中可见。
- [ ] 设置页能保存并持久化配置。
- [ ] 测试连接按钮能成功调用 API 并返回验证结果。
- [ ] 生成页能根据中文生成符合所选风格的变量名。
- [ ] 复制按钮能将结果写入剪贴板。
