pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenLocal()
        maven("https://dl.cloudsmith.io/public/libp2p/jvm-libp2p/maven/")
    }
}

plugins {
    id("com.gradle.enterprise") version("3.7.2")
    // id("caching.ipfs.ipfs-build-cache") version "0.0.1"
}
buildCache {
    local {
        isEnabled = false
    }
    // remote<caching.ipfs.IpfsBuildCache> {
    //     isPush = true
    // }
}

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
    }
}

rootProject.name = "ipfs-build-cache"
include("plugin")
