# 仓库协作指南

## 适用范围

本文件适用于整个仓库。

## 项目概览

S1-Next 是 STAGE1 论坛的 Android 客户端。项目采用 Gradle 多模块结构，使用 Kotlin DSL、Android Gradle Plugin 8.7.3、Gradle 8.9、Kotlin 2.0.0、Java 8 字节码，`compileSdk` 为 34，`targetSdk` 为 34。

模块划分：

- `:app`：主 Android 应用，namespace/application id 为 `me.ykrank.s1next`，minSdk 26。
- `:library`：共享 Android 工具、UI、网络基础层，namespace 为 `com.github.ykrank.androidtools`，minSdk 24。
- `:JKeyboardPanelSwitch`：内置键盘面板切换库，namespace 为 `cn.dreamtobe.kpswitch`，minSdk 17。

## 构建变体

应用有一个 flavor 维度 `market`，包含 `normal` 和 `play` 两个 flavor。build type 包含 `debug`、`alpha`、`release`。

- `debug`：application id 后缀为 `.debug`，版本名后缀为 `-debug`，使用启用 Flipper 的 source set。
- `alpha`：application id 后缀为 `.alpha`，版本名后缀为 `-alpha`，开启混淆和资源压缩，并配置 release/debug fallback。
- `release`：开启混淆和资源压缩。
- `BuildTypeModule` 和 `PreApp` 分别在 `app/src/debug`、`app/src/alpha`、`app/src/release` 中提供 build type 专属实现。
- `normal` 和 `play` source set 提供市场相关实现，例如更新逻辑和 Bugly 行为。

## 常用命令

- 构建常用本地 debug 包：`./gradlew :app:assembleNormalDebug`
- 构建 Play debug 包：`./gradlew :app:assemblePlayDebug`
- 构建 alpha 包：`./gradlew :app:assembleNormalAlpha`
- 构建 release 包：`./gradlew :app:assembleNormalRelease`
- 构建共享库：`./gradlew :library:assemble`
- 清理生成产物：`./gradlew clean`
- 检查依赖更新：`./gradlew dependencyUpdates`

当前仓库没有提交 `src/test` 或 `src/androidTest` 测试文件。后续如新增测试，优先运行匹配变体的任务，例如 `./gradlew :app:testNormalDebugUnitTest`。设备测试需要连接真机或模拟器。

## 关键路径

- `settings.gradle.kts`：模块引入和仓库策略。当前通过 `RepositoriesMode.FAIL_ON_PROJECT_REPOS` 禁止模块级仓库配置。
- `gradle/libs.versions.toml`：集中管理依赖和插件版本。
- `app/build.gradle.kts`：应用变体、签名配置、Room schema 的 KSP 参数、Hilt/Room/Glide KSP。
- `library/build.gradle.kts`：可复用 Android 工具库依赖，其中多数通过 `api` 暴露。
- `app/src/main/AndroidManifest.xml`：应用入口、权限、Activity、深链、Bugly/TalkingData 元数据。
- `app/src/main/java/me/ykrank/s1next/App.kt`：应用启动入口，初始化 pre-app component、主 app component、全局数据、Bugly 和生命周期回调。
- `app/src/main/java/me/ykrank/s1next/AppComponent.kt`：主 Dagger 图和注入目标。
- `app/src/main/java/me/ykrank/s1next/PreAppComponent.kt`：早期单例图，提供 context、preferences、cookies、object mapper、event bus、tracking 等。
- `app/src/main/java/me/ykrank/s1next/AppModule.kt`、`PreAppModule.kt`、`data/db/DbModule.kt`：Dagger provider 定义。
- `app/src/main/java/me/ykrank/s1next/data/api`：Retrofit API、Discuz/S1 URL helper、API model、transformer。
- `app/src/main/java/me/ykrank/s1next/data/db`：主 Room 数据库、DAO、实体和业务封装。
- `app/src/main/java/me/ykrank/s1next/data/cache`：基于 Room 的网络/数据缓存。
- `app/src/main/java/me/ykrank/s1next/data/pref`：基于 SharedPreferences 的设置和 manager。
- `app/src/main/java/me/ykrank/s1next/view`：Activity、Fragment、Adapter、Dialog、事件、转场。
- `app/src/main/java/me/ykrank/s1next/view/page`：较大的业务页面域，例如 app 登录、帖子列表/编辑、设置。
- `app/src/main/java/me/ykrank/s1next/viewmodel`：面向 UI 的 view model 和状态对象。
- `app/src/main/java/me/ykrank/s1next/widget`：网络 qualifier、Glide、host 检查、下载、span、上传、埋点等横切组件。
- `app/schemas`：导出的 Room schema。修改数据库版本或实体时需要同步维护。

## 架构说明

- UI 使用传统 Android XML/Data Binding，加 Material/AppCompat；不是 Jetpack Compose。除非任务明确要求，否则沿用现有 Activity/Fragment/XML 模式。
- `BaseActivity`、`BaseFragment`、`BaseRecyclerViewFragment`、`BaseViewPagerFragment` 提供生命周期、toolbar/drawer、注入、加载、重试和缓存提示等通用能力。新增页面优先复用这些基类。
- `:library` 放通用 base UI、adapter、event、Glide、network、tracking、upload 和工具代码。S1 业务逻辑应放在 `:app`；只有真正可复用的平台代码才放入 `:library`。
- Dagger 为手动 wiring。新增需要字段注入的类时，需要在 `AppComponent` 添加 `inject(...)` 方法，并按邻近类的生命周期风格调用 `App.appComponent.inject(this)`。
- 网络层使用 Retrofit + OkHttp + Jackson。`S1Service` 负责论坛/S1 接口，`AppService` 负责应用后端接口。OkHttp client 通过 `@Data`、`@Image`、`@AppData` 区分用途。
- debug/alpha 的 `BuildTypeModule` 会添加 Flipper 网络拦截器；release 构建使用普通 client。
- 加载代码同时存在 RxJava `Single` 和 coroutine `Flow<Resource<T>>`。修改时跟随所在功能附近的既有风格，不要为了单点改动做大范围迁移。
- `EventBus` 同时支持 Rx `PublishSubject` 和 Kotlin `SharedFlow`；使用调用方/监听方附近已有的风格。
- 持久化应用数据在 `AppDatabase` version 9 中，API 缓存数据在 `CacheDatabase` version 2 中。Room schema 已开启导出。

## 修改准则

- 保持 Kotlin/Java 互操作简单，并维持 Java 8 兼容。
- 不要编辑或提交 `local.properties` 中的密钥；签名信息和 HTTP DNS 值只应存在本地。
- 新增依赖应通过 `gradle/libs.versions.toml` 和对应模块的 `build.gradle.kts`，不要在模块 build 文件里新增仓库。
- 用户可见文案使用 string resource；必要时同步更新相关本地化资源（`values`、`values-zh`、`values-zh-rTW`）。
- 修改 Room entity/DAO 时，需要提升数据库版本，按需添加 auto/manual migration，并提交 `app/schemas` 下生成的 schema JSON。
- 修改 Data Binding layout 后，应构建相关变体，因为 binding 错误会在编译期暴露。
- 除非任务明确涉及键盘/面板行为，否则避免改动 `JKeyboardPanelSwitch`。
- 谨慎扩大 `:library` API：该模块很多依赖通过 `api` 暴露，改动可能广泛影响 `:app`。

## 验证建议

- 仅文档改动无需运行 Gradle 构建。
- 修改 Kotlin/Java/XML/resource 后，至少运行 `./gradlew :app:assembleNormalDebug`。
- 修改变体专属 source set 后，应编译受影响变体，例如 `:app:assemblePlayDebug`、`:app:assembleNormalAlpha` 或 `:app:assembleNormalRelease`。
- 数据库改动应确认 schema，并运行会触发 Room/KSP 的构建任务。
