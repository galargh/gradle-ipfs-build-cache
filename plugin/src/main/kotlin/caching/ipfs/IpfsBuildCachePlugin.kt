package caching.ipfs

import caching.ipfs.internal.IpfsBuildCacheServiceFactory
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

class IpfsBuildCachePlugin: Plugin<Settings> {
    override fun apply(settings: Settings) {
        settings.buildCache
                .registerBuildCacheService(IpfsBuildCache::class.java, IpfsBuildCacheServiceFactory::class.java)
    }
}
