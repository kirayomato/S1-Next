pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven("http://maven.aliyun.com/nexus/content/repositories/releases/") {
            name = "aliyun"
            //一定要添加这个配置
            isAllowInsecureProtocol = true
        }
        // 阿里jcenter镜像
        maven("https://maven.aliyun.com/repository/jcenter")
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "S1-Next"
include(":app")
include(":library")
include(":JKeyboardPanelSwitch")