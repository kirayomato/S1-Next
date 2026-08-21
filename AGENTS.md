# AGENTS.md

## 项目简介

S1-Next 是 Stage1st（S1 论坛，https://bbs.saraba1st.com）的第三方 Android 客户端。主语言 Kotlin，含少量 Java 遗留代码。帖子内容以 HTML 形式从服务端获取，经后处理渲染为富文本（Span）显示在 TextView 中。

## 技术栈与关键版本

| 项 | 值 |
|---|---|
| 构建 | Gradle 9.4.1（wrapper）+ AGP 9.2.1 |
| SDK | minSdk 32 / targetSdk 34 / compileSdk 37，Java 21 |
| UI | AndroidX + Material（锁定 1.12.0，勿升 1.13.0）+ DataBinding（仍用 kapt）+ ConstraintLayout |
| 列表 | RecyclerView + hannesdorfmann adapterdelegates3 + Paging 3 |
| 异步 | 旧代码 RxJava2 / RxAndroid / AutoDispose；新代码 Kotlin 协程（lifecycleScope / GlobalScope） |
| DI | Dagger 2（kapt） |
| 网络 | Retrofit 3 + OkHttp 5 + Jackson + RxJava2 adapter + 阿里云 HTTPDNS |
| 数据库 | Room 2.8（KSP，schema 输出到 `app/schemas`，库名 `s1.db`） |
| 图片 | Glide 5（KSP）+ PhotoView + LargeImageView |
| 其他 | jsoup（HTML 解析）、PaperParcel（Parcelable 生成，kapt）、腾讯 Bugly、Flipper（debug 调试） |

## 模块结构

- `:app` —— 主应用（`me.ykrank.s1next`），所有业务代码
- `:library` —— 通用工具库（`com.github.ykrank.androidtools`）：日志 `L`、EventBus、下载、网络、通用 UI 组件等，app 依赖它
- `:JKeyboardPanelSwitch` —— 键盘面板切换库

## 构建与运行

- 产品风味：`play` / `normal`（market 维度）；构建类型：`debug` / `alpha` / `release`（alpha/release 开启 minify + shrinkResources，使用 `proguard-rules.pro`）
- 常用任务：`./gradlew :app:assembleNormalDebug`、`:app:compileNormalDebugKotlin`、`:app:installNormalDebug`
- 签名与 HTTPDNS 凭据从 `local.properties` 读取（`storeFile`/`storePassword`/`keyAlias`/`keyPassword`/`httpDnsId`/`httpDnsSecret`），本地已配置 `sdk.dir`
- 版本号集中在 `app/build.gradle.kts`：`appVersionCode = 99`、`appVersionName = "3.5"`，输出形如 `3.5.99`

### ⚠️ 已知构建坑

`gradle/wrapper/gradle-wrapper.jar` 是 Gradle 2.10 时代的旧 jar（manifest 无 Main-Class），与 `gradle-wrapper.properties` 声明的 Gradle 9.4.1 不匹配。在 Git Bash 下直接 `./gradlew` 会报 `ClassNotFoundException: org.gradle.wrapper.GradleWrapperMain`。如遇此问题，从 Gradle 9.4.1 官方发行版中取出新版 `gradle-wrapper.jar` 替换，或用本机已安装的 gradle 执行。

## 包结构（app 模块，`me.ykrank.s1next`）

- `App.kt` —— `MultiDexApplication` 入口，初始化 Dagger 组件（`PreAppComponent` → `AppComponent`）
- `binding/` —— DataBinding 适配器（`TextViewBindingAdapter` 等）
- `data/api/` —— 网络层：`Api`（Retrofit 接口）、`app/`（新版 App API）、`business/PostFilter`（帖子 HTML 后处理）、`model/`（Post、Thread 等模型）
- `data/db/` —— Room（`DbModule`、`dbmodel/`、`biz/`），黑名单 `BlackListBiz` 在此
- `data/cache/`、`data/pref/` —— 缓存与 SharedPreferences（`GeneralPreferencesManager` 等）
- `view/activity/` —— Activity（ForumActivity、ThreadListActivity、PostListActivity、GalleryActivity 等）
- `view/page/` —— 页面级 Fragment：
  - `app/` 新版帖子页（AppPost 系列）、`post/postlist/` 楼层列表、`post/postedit/` 发帖/编辑、`post/viewmodel/`、`post/adapter/`、`post/prefetch/`、`edit/`、`login/`、`setting/`、`test/`
- `view/adapter/`、`view/dialog/`、`view/event/`（EventBus 事件）、`view/internal/`、`view/fragment/`
- `viewmodel/` —— `AppPostViewModel` 等
- `widget/span/` —— **帖子富文本渲染核心**：`TagHandler`、`GlideImageGetter`、`ImageClickableResizeSpan`、`PostMovementMethod`、`HtmlCompat` 等
- `widget/` —— 其余为下载（`download/`）、加密（`encrypt/`）、Glide 定制、图片、网络、SAF 文件、埋点、传图等

## 关键流程：帖子渲染管线

1. API 返回帖子 HTML（`Post.reply`）
2. `PostFilter`（`data/api/business/PostFilter.kt`）做后处理：引用 `<div class="reply_wrap">` → `<blockquote>`、黑名单引用隐藏、B 站/媒体标签替换、附件 `[attach]` → `<img>`/`<attach>`、代码块 div 清洗
3. `HtmlCompat.fromHtml` + `GlideImageGetter`（异步加载图片）+ `TagHandler` 将 HTML 转为 Spannable
4. `TagHandler.handleImg` 把图片占位符 `\uFFFC` 换成换行，并挂 `ImageClickableResizeSpan`（点击进 GalleryActivity 看大图）
5. 渲染到 `item_post.xml` 的 `tvReply`（TextView，`setSpannableFactory(FixedSpannableFactory())`）

## 代码约定

- UI 由 DataBinding + ViewModel（`ObservableField` 等）驱动，布局中 `binding.viewModel = ...` 绑定
- 页面间通信用 EventBus：`eventBus.postDefault(XxxEvent(...))`，事件类放 `view/event/`
- 日志统一用 `com.github.ykrank.androidtools.util.L`（`L.report`/`L.leaveMsg` 会上报 Bugly），不要直接 `println`
- 新代码优先协程，旧代码 RxJava2，改动旧文件时保持原有风格
- 资源：中文文案在 `res/values-zh/strings.xml`，多语言还有 `values-zh-rTW`、`values-night`
- 提交信息为中文短句（如"插入图片换行"），遵循仓库现有习惯

## 易踩坑提醒

- **帖子可复制**：`menu_post_selectable` 开关开启后 `tvReply.setTextIsSelectable(true)` 支持选中复制。复制输出来自 TextView 文本流，**不要把图片 URL 塞进文本流**（`TagHandler` 中图片占位符应为 `\n`，否则复制会带出图片链接）
- 设置 `setTextIsSelectable` 后需重置 `movementMethod`（见 `PostAdapterDelegate.setTextSelectable`），否则链接点击失效
- `FixedSpannableFactory` 已解决 TextView span 工厂问题，勿随意改动
- Material 版本锁定 1.12.0，升级到 1.13.0 会出问题（toml 中有 noinspection 注释）
- Room schema 变更需保留 `app/schemas` 下的迁移记录
