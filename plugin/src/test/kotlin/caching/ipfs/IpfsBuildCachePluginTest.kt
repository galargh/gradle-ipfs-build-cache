package caching.ipfs

import caching.ipfs.internal.IpfsBuildCacheServiceFactory
import org.gradle.api.initialization.Settings
import org.gradle.caching.configuration.BuildCacheConfiguration
import org.mockito.Mockito
import kotlin.test.Test

/**
 * A simple unit test for the 'caching.ipfs.ipfs-build-cache' plugin.
 */
class IpfsBuildCachePluginTest {
    @Test fun `plugin is applied`() {
        // Create mock settings and apply the plugin
        val buildCache = Mockito.mock(BuildCacheConfiguration::class.java)
        val settings = Mockito.mock(Settings::class.java)
        Mockito.`when`(settings.getBuildCache()).thenReturn(buildCache)

        val plugin = IpfsBuildCachePlugin()
        plugin.apply(settings)

        Mockito.verify(buildCache)
                .registerBuildCacheService(IpfsBuildCache::class.java, IpfsBuildCacheServiceFactory::class.java)
    }
}
