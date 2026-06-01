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
- 第一版不新增独立的纯文本分享入口；帖子页分享统一进入长图选择分享流程，生成失败时可按需降级到现有文本分享。

## 交互设计

### 入口

帖子页的两个分享入口统一进入同一个选择分享流程：

- 顶部 toolbar 的“分享”。
- 楼层菜单中的“分享”。

两者不再立即调起纯文本系统分享，而是先进入楼层选择模式。

### 选择模式

- 从楼层菜单进入时，默认选中触发分享的当前楼层。
- 从顶部 toolbar 进入时，默认选中当前页第一个可见楼层；如果当前页尚无已加载楼层，则提示暂无可分享内容。
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

其中 `sourcePostId` 只表示触发分享的来源楼层：

- 楼层菜单入口：`sourcePostId` 为该楼层 pid，并默认选中该 pid。
- 顶部 toolbar 入口：`sourcePostId` 为 `null`，默认选中当前页第一个可见楼层。
- 最终生成 `PostShareRequest.posts` 时，不使用 `selectedPostIds` 的 set 顺序，而是按当前页 `currentPosts` 的自然顺序筛出选中楼层，保证长图顺序稳定。

新增分享选择事件：

```kotlin
data class EnterPostShareSelectionEvent(
    val threadId: String?,
    val pageNum: Int,
    val postId: Int,
)
```

楼层菜单没有稳定持有 `PostListPagerFragment`，因此用 EventBus 从楼层 action 发事件。`PostListPagerFragment` 收到事件后需要同时校验：

- `threadId == null || threadId == mThreadId`
- `pageNum == mPageNum`
- `postId` 存在于当前 `currentPosts`

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

选择模式建议放在页面层统一管理，adapter 只接收状态并渲染选择 UI。顶部 toolbar 分享和楼层菜单分享共用同一套入口：

- `PostListPagerFragment` 持有选择状态，因为它已持有当前页 `currentPosts`、`mThreadInfo`、`mPageNum` 和 adapter。
- `PostListFragment` 负责把顶部 toolbar 切换为选择态，并把 toolbar “分享”转发给当前 `curPostPageFragment`。
- 顶部 toolbar 分享调用当前页 `startPostShareSelection(sourcePostId = null)`。
- 楼层菜单分享发起 `EnterPostShareSelectionEvent(pageNum, postId)`，当前页收到后调用 `startPostShareSelection(sourcePostId = postId)`。
- 页面更新 adapter 状态：
  - 当前页 posts。
  - selected ids。
  - selection enabled。
- adapter 根据 post id 显示 checkbox/选中背景。

第一版可优先覆盖新渲染路径；如果旧渲染仍可配置开启，则需要旧路径也提供同样选择 UI，避免用户关闭新渲染后分享入口失效。

现有代码注意点：

- 楼层菜单的新渲染入口在 `PostRenderActions`，旧渲染入口通过 `PostViewModel.showFloorActionMenu()` 间接调用同一套 action。
- `PostViewModel` 调用楼层菜单时没有直接持有 `PostListPagerFragment`，因此建议用 EventBus 发选择事件，而不是只依赖 fragment 回调。
- 顶部 toolbar 的分享入口在 `PostListFragment`，需要通过 `curPostPageFragment` 转发到当前页，避免父 fragment 复制当前页 posts 状态。

### 选择状态流

建议新增一组窄接口，避免 adapter 直接依赖 fragment 具体类型：

```kotlin
interface PostShareSelectionOwner {
    val postShareSelectionState: PostShareSelectionState
    fun togglePostShareSelection(postId: Int)
}
```

`PostListPagerFragment` 实现该接口，并提供：

- `startPostShareSelection(sourcePostId: Int?)`
- `cancelPostShareSelection()`
- `confirmPostShareSelection()`
- `selectedSharePosts(): List<Post>`

默认选中规则：

- `sourcePostId != null`：选中该 pid。
- `sourcePostId == null`：优先选中当前 RecyclerView 第一个可见楼层对应的 pid；若当前可见 item 在 hybrid 渲染下是正文块/图片块，则通过 `HybridPostListRecyclerViewAdapter.postPositionForAdapterPosition()` 映射回 post；映射失败时回退到 `currentPosts.firstOrNull()`。
- 当前页无可选楼层：提示 `post_share_no_data`，不进入选择态。

选择态更新规则：

- 每次选择变化后，通知 adapter 局部刷新对应 post 的所有渲染块。
- 如果用户取消最后一个选中楼层，保留选择态但禁用确定按钮，或者直接禁止取消最后一个选中楼层。第一版建议禁止取消最后一个选中楼层，避免确定按钮和空状态分支复杂化。
- 确定时按当前页顺序收集选中楼层，构造 `PostShareRequest`。

### Toolbar 选择态

`PostListFragment` 维护 toolbar 选择态显示，不持有具体 selected ids：

- `isPostShareSelectionMode: Boolean`
- `postShareSelectedCount: Int`

进入选择态：

- 隐藏 `toolbar_page_jump`。
- `activity.title = getString(R.string.post_share_selected_count, count)`。
- `onPrepareOptionsMenu()` 隐藏普通帖子菜单项，只显示“确定”菜单项。
- `android.R.id.home` 在选择态下作为取消处理，调用 `curPostPageFragment?.cancelPostShareSelection()`，不关闭 Activity。

退出选择态：

- 恢复 `toolbar_page_jump`。
- 恢复原帖子标题。
- 恢复普通帖子菜单项。

实现上可在 `fragment_post.xml` 新增默认隐藏的 `menu_post_share_confirm`，标题复用 `dialog_button_text_confirm`；选择态下只显示该 item。

### Adapter 选择 UI

新旧渲染路径都需要接受 `PostShareSelectionOwner`：

- 旧路径：`PostListRecyclerViewAdapter` 将 owner 传给 `PostAdapterDelegate`，在 `item_post.xml` 增加选择控件和选中背景。
- 新路径：`HybridPostListRecyclerViewAdapter` 将 owner 传给 header/text/image/fallback/footer delegates。
- 新路径建议只在 `PostRenderItem.Header` 展示明确的 checkbox，但正文块、图片块、fallback、footer 在选择态下点击时仍切换所属 post，满足“点击卡片切换”的交互。
- 选择态下楼层长按菜单不应再弹出；点击优先用于切换选择。

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

### 长图生成实现

第一版建议使用专用分享 View 离屏绘制，而不是截取当前 RecyclerView：

```kotlin
class SharePostCardRenderer(
    private val context: Context,
    private val imageLoader: ShareImageLoader,
    private val qrCodeBitmapFactory: QrCodeBitmapFactory,
) {
    suspend fun renderToBitmap(request: PostShareRequest): Bitmap
}
```

实现边界：

- `PostListPagerFragment.confirmPostShareSelection()` 只负责构造 `PostShareRequest` 和展示 loading。
- `SharePostCardRenderer` 负责把 `PostRenderMapper.map(request.posts)` 的结果转成分享专用布局。
- 图片加载通过 `ShareImageLoader` 封装，内部复用 Glide + `ImageBiz`，在 `Dispatchers.IO` 预加载正文图片 bitmap。
- View 创建、measure、layout、draw 必须回到主线程执行；文件写入在 `Dispatchers.IO` 执行。
- 生成过程绑定 fragment `viewLifecycleOwner.lifecycleScope`，fragment destroy 后取消 coroutine，不继续持有 Activity/View。

推荐输出流程：

```text
selected posts
  -> PostShareRequest
  -> PostRenderMapper.map(posts)
  -> preload ImageBlock bitmaps with limits
  -> build share-only View tree
  -> measure/layout with fixed card width
  -> draw to Bitmap
  -> write PNG to cache/post_share
  -> FileProvider Uri
  -> ACTION_SEND image/png
```

尺寸建议：

- 分享图固定内容宽度建议 1080px；在小屏或低内存设备上可降到 `min(displayMetrics.widthPixels, 1080)`。
- UI 样式使用分享专用 dimens，不直接复用帖子页大段 padding，避免长图过高。
- 先按限制后的图片高度估算总高度，超过 `MAX_SHARE_CARD_HEIGHT_PX` 时提前失败并提示用户减少楼层选择。

缓存文件：

- 目录：`context.cacheDir/post_share/`
- 文件名：`post_share_{threadId}_{timestamp}.png`
- 生成前可清理该目录中较旧的分享图，避免缓存持续增长。

Loading：

- 现有 `SimpleProgressDialogFragment` 可复用为第一版生成中提示。
- 失败时关闭 loading，提示用户原因；如果保留文本分享降级，则降级内容应复用原来的标题 + 楼层链接格式。

### 图片约束

为了避免长图过高或生成 OOM：

- 单图最大高度：建议不超过卡片内容宽度的 1.6 倍。
- 单楼层最大图片数：建议默认 9 张，超过后显示“还有 N 张图片”。
- 整张分享图最大高度：建议 16000px；超过时提示用户减少选择楼层，或后续支持分页生成多图。
- 图片加载失败时显示占位图，不中断整体生成。

### Bitmap 与分享 URI

- 生成 Bitmap 后写入 app cache。
- 当前仓库未发现已有 FileProvider 配置，需要新增 `androidx.core.content.FileProvider` provider 和 cache-path XML，用于暴露生成的分享图片临时 URI。
- 分享 Intent：

```kotlin
Intent(Intent.ACTION_SEND).apply {
    type = "image/png"
    putExtra(Intent.EXTRA_STREAM, uri)
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
}
```

### 二维码

- 当前依赖未发现二维码库，优先引入稳定轻量库，例如 ZXing core。
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

现有 `ThreadLink.parse()` 已支持 redirect findpost 的 `pid`，但不解析 `thread-xxx-page.html#pidyyy` 这类 anchor。由于分享二维码优先生成 `#pid{post.id}` 稳定楼层链接，搜索解析阶段需要补充 `#pid(\\d+)` 解析并映射到 `quotePostId`。

`PostShareCode` 的职责边界：

- 只解析纯文本神秘代码，例如 `2038487-2-3681`。
- 第三段保留为楼层号 `floor`，不写入 `ThreadLink.quotePostId`。
- `toThreadLink()` 只输出 thread/page；如果包含 floor，第一版仍只打开对应 page，并在解析卡片展示楼层号。
- 后续如果实现“按楼层号滚动”，再在 `PostListActivity`/`PostListFragment` intent 中新增 floor 字段，不复用 `quotePostId`。

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

实现细节：

- `ParsedThreadLinkSearchResult` 继承 `SearchResult`，并实现稳定 id，避免插入顶部时 RecyclerView 刷新跳动。
- `SearchRecyclerViewAdapter` 增加 `SearchParsedThreadAdapterDelegate`，位置应早于普通 forum/user delegate 无硬性要求，但类型判断必须明确。
- `SearchActivity.searchFor(query)` 在论坛搜索分支中先计算 `parsed`，网络结果回来后合并。网络失败时可以仍展示解析卡片并附带错误提示；第一版可保持现有失败展示逻辑，仅在网络成功时插入。
- 解析卡片点击直接调用 `PostListGatewayActivity.start(context, threadLink)`；不要依赖卡片内 HTML span/movementMethod。
- 卡片展示文案使用 string resource，并同步 `values`、`values-zh`、`values-zh-rTW`。

### 点击行为

- 如果只有 thread/page：`PostListGatewayActivity.start(context, threadLink)`。
- 如果有 floor：
  - 第一版打开对应 page，并在页面标题/搜索结果卡片中明确展示 `#floor`。
  - 后续可扩展 PostListActivity intent，进入页面后按楼层号滚动高亮。

## 分阶段实施

### 阶段一：选择模式

- 增加分享选择状态。
- 顶部 toolbar “分享”和楼层菜单“分享”进入同一个选择模式。
- 顶部 toolbar 入口默认选中当前页第一个可见楼层；楼层菜单入口默认选中当前楼层。
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
