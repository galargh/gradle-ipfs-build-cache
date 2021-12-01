pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenLocal()
        maven("https://dl.cloudsmith.io/public/libp2p/jvm-libp2p/maven/")
    }
}

/*plugins {
    id("caching.ipfs.ipfs-build-cache") version "0.0.1"
}
buildCache {
    local {
        isEnabled = false
    }
    remote<caching.ipfs.IpfsBuildCache> {
        isPush = true
    }
}*/

rootProject.name = "ipfs-build-cache"
include("plugin")

