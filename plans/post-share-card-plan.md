# 帖子楼层长图分享技术方案

## 背景

当前帖子页楼层菜单中的“分享”只会生成纯文本链接并调用系统分享。后续希望分享体验升级为：

- 先进入楼层选择模式，可选择当前已加载页中的多个楼层。
- 默认只选择触发分享的当前楼层。
- 点击确定后生成长图卡片，并通过系统分享图片。
- 首页搜索输入可解析的帖子链接或分享神秘代码时，在搜索结果顶部插入解析卡片，快速跳转对应帖子/楼层。

本方案只覆盖第一版闭环，后续随着帖子详情逐步迁移到自定义渲染，可复用同一份分享渲染中间层继续演进。

## 目标

- 支持当前已加载页内多楼层选择分享。
- 生成稳定、可读、可传播的长图卡片。
- 长图中真实展示正文图片，但做高度和数量保护。
- 分享卡片底部包含当前链接二维码和结构化文本信息。
- 首页搜索顶部识别并展示可解析的帖子链接或神秘代码。
- 尽量复用现有帖子链接解析和新帖子渲染拆块逻辑。

## 非目标

- 第一版不自动跨页加载并选择楼层。
- 第一版不做复杂图片编辑、涂抹、水印自定义。
- 第一版不保证所有复杂 HTML 结构都 100% 自定义渲染；复杂块允许局部 fallback。
- 第一版不改变现有普通文本分享入口以外的系统分享行为。

## 交互设计

### 入口

帖子楼层菜单点击“分享”后，不再立即调起系统分享，而是进入选择模式。

### 选择模式

- 默认选中当前楼层。
- 只允许选择当前已加载页内的楼层。
- Toolbar 切换为选择态：
  - 左侧：取消。
  - 标题：`已选择 1 楼`。
  - 右侧：确定。
- 楼层卡片显示选择控件，点击卡片或选择控件切换选中状态。
- 取消后恢复普通阅读态。
- 确定后退出选择态，进入长图生成流程。

### 长图生成与分享

- 生成期间展示进度弹窗或 loading 状态。
- 生成成功后通过系统分享 sheet 分享图片 URI。
- 生成失败时提示错误，并保留用户当前阅读位置。

## 分享卡片设计

### 布局结构

1. 顶部品牌区
   - App 图标。
   - App 名称，例如 `S1 Next` 或本地化名称。
   - 可选副标题：`来自 Stage1 论坛`。

2. 楼层内容区
   - 按页面内顺序展示被选中的楼层。
   - 每个楼层包含：
     - 作者头像或占位。
     - 作者名。
     - 楼层号，例如 `#3681`。
     - 时间。
     - 正文内容。
   - 多楼层之间使用低对比度分隔。

3. 底部分享区
   - 左侧：当前链接二维码。
   - 右侧两行文案：
     - 第一行：`帖子 2038487 · 第 2 页 · #3681`
     - 第二行：`神秘代码：2038487-2-3681`

### 文案规则

- `帖子 {threadId}`
- 如果 page 可用，追加 `第 {page} 页`。
- 如果第一个分享楼层可用，追加 `#{floor}`。
- 神秘代码格式：
  - page 和 floor 都有：`{threadId}-{page}-{floor}`
  - 只有 page：`{threadId}-{page}`
  - 只有 threadId：`{threadId}`

### 二维码链接

- 优先使用第一个选中楼层的稳定楼层链接：
  - `Api.getPostListUrlForBrowser(threadId, page) + "#pid{post.id}"`
- 如果没有楼层 pid，则使用当前页链接：
  - `Api.getPostListUrlForBrowser(threadId, page)`

## 技术设计

### 数据模型

新增分享选择状态模型：

```kotlin
data class PostShareSelectionState(
    val enabled: Boolean,
    val selectedPostIds: Set<Int>,
    val sourcePostId: Int?,
)
```

新增分享请求模型：

```kotlin
data class PostShareRequest(
    val threadId: String,
    val threadTitle: String?,
    val page: Int,
    val posts: List<Post>,
)
```

新增分享代码模型：

```kotlin
data class PostShareCode(
    val threadId: String,
    val page: Int?,
    val floor: String?,
)
```

注意：现有 `ThreadLink.quotePostId` 表示 pid，不等同于楼层号。神秘代码中的第三段是楼层号，因此不应直接塞进 `quotePostId`。建议新增 `PostShareCode`，内部复用 `ThreadLink.parse2()` 解析 thread/page，再额外解析可选 floor。

### 楼层选择模式接入

当前帖子页已有两套渲染路径：

- 旧路径：`PostListRecyclerViewAdapter` + `PostAdapterDelegate`
- 新路径：`HybridPostListRecyclerViewAdapter` + `PostRenderItem`

选择模式建议放在页面层统一管理，adapter 只接收状态并渲染选择 UI：

- `PostListFragment` 或 `PostListPagerFragment` 持有选择状态。
- 触发楼层分享时发起 `EnterPostShareSelectionEvent(postId)` 或直接回调页面。
- 页面更新 adapter 状态：
  - 当前页 posts。
  - selected ids。
  - selection enabled。
- adapter 根据 post id 显示 checkbox/选中背景。

第一版可优先覆盖新渲染路径；如果旧渲染仍可配置开启，则需要旧路径也提供同样选择 UI，避免用户关闭新渲染后分享入口失效。

### 长图渲染管线

不建议把整帖重新丢给 Android 内置 HTML/span 渲染。分享长图应复用新渲染中间层：

```text
List<Post>
  -> PostRenderMapper.map(posts)
  -> List<PostRenderItem>
  -> SharePostCardRenderer.render(...)
  -> Bitmap / Uri
  -> ACTION_SEND image/*
```

`PostRenderMapper` 已处理：

- 普通文本块：`TextBlock`
- 独立大图：`ImageBlock`
- 复杂 HTML：`FallbackHtmlBlock`
- 小表情不作为独立图片拆出

`SharePostCardRenderer` 第一版策略：

- `Header/Footer` 不直接复用帖子页 layout，改用分享卡片专用紧凑样式。
- `TextBlock`：可使用 Jsoup + HtmlCompat 处理块内富文本，后续逐步替换为纯自定义文本 renderer。
- `ImageBlock`：真实加载图片，按卡片宽度缩放。
- `FallbackHtmlBlock`：第一版使用 HtmlCompat/TextView fallback，后续跟随新渲染升级。

### 图片约束

为了避免长图过高或生成 OOM：

- 单图最大高度：建议不超过卡片内容宽度的 1.6 倍。
- 单楼层最大图片数：建议默认 9 张，超过后显示“还有 N 张图片”。
- 整张分享图最大高度：建议 16000px；超过时提示用户减少选择楼层，或后续支持分页生成多图。
- 图片加载失败时显示占位图，不中断整体生成。

### Bitmap 与分享 URI

- 生成 Bitmap 后写入 app cache。
- 使用现有 FileProvider 配置暴露临时 URI；如果当前没有合适 path，需要补充 provider paths。
- 分享 Intent：

```kotlin
Intent(Intent.ACTION_SEND).apply {
    type = "image/png"
    putExtra(Intent.EXTRA_STREAM, uri)
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
}
```

### 二维码

需要确认当前依赖是否已有二维码库。若没有：

- 优先引入稳定轻量库，例如 ZXing core。
- 依赖新增到 `gradle/libs.versions.toml`。
- 二维码生成封装为 `QrCodeBitmapFactory`，输入 URL 输出 Bitmap。

## 首页搜索解析卡片

### 解析规则

搜索提交时先尝试解析 query：

1. `PostShareCode.parse(query)`
   - 支持 `2038487-2-3681`
   - 支持 `2038487-2`
   - 支持 `2038487`
2. `ThreadLink.parse2(query)`
   - 复用现有链接、tid、tid-page 解析能力。
3. URL 中包含 `#pidxxx` 或 redirect findpost 时继续复用 `ThreadLink.parse()`。

### 搜索结果插入

当前 `SearchRecyclerViewAdapter` 只支持：

- `ForumSearchResult`
- `UserSearchResult`

建议新增：

- `ParsedThreadLinkSearchResult`
- `SearchParsedThreadAdapterDelegate`
- `item_search_parsed_thread.xml`

网络搜索流程不变，但论坛搜索结果返回前将解析卡片插入顶部：

```kotlin
val parsed = ParsedThreadLinkSearchResult.fromQuery(query)
val results = buildList<SearchResult> {
    parsed?.let { add(it) }
    addAll(forumSearchResults)
}
```

用户搜索模式下是否展示解析卡片需要产品取舍。建议只在“论坛”搜索类型展示，避免用户搜 uid/name 时插入帖子结果。

### 点击行为

- 如果只有 thread/page：`PostListGatewayActivity.start(context, threadLink)`。
- 如果有 floor：
  - 第一版打开对应 page，并在页面标题/搜索结果卡片中明确展示 `#floor`。
  - 后续可扩展 PostListActivity intent，进入页面后按楼层号滚动高亮。

## 分阶段实施

### 阶段一：选择模式

- 增加分享选择状态。
- 帖子页菜单“分享”进入选择模式。
- 新旧渲染路径展示选择态。
- 确认默认选中当前楼层、取消、确定流程。

### 阶段二：长图生成

- 新增 `SharePostCardRenderer`。
- 接入 `PostRenderMapper`。
- 支持顶部品牌区、楼层内容区、底部二维码区。
- 支持真实图片加载和高度保护。
- 输出图片 URI 并调起系统分享。

### 阶段三：首页搜索解析

- 新增 `PostShareCode`。
- 新增解析结果 item 和 adapter delegate。
- 搜索论坛时顶部插入解析结果。
- 点击复用 `PostListGatewayActivity`。

### 阶段四：体验完善

- 进入页面后按楼层号滚动/高亮。
- 长图过高时支持生成多图。
- 复杂 HTML 块逐步从 fallback 迁移到自定义渲染。
- 记录分享埋点和失败原因。

## 风险与注意事项

- `ThreadLink.quotePostId` 是 pid，不是楼层号，神秘代码不要误用。
- 离屏渲染需要控制图片加载生命周期，避免 Activity 销毁后继续生成。
- 生成长图涉及 Bitmap 内存，必须做高度和图片数量限制。
- 分享 URI 需要 FileProvider 授权，否则外部应用无法读取。
- 首页搜索插入解析结果时要保留原有空结果/错误展示逻辑。
- 如果用户关闭新版帖子渲染，选择分享入口仍需要可用。
