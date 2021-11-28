package caching.ipfs

import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * A simple unit test for the 'caching.ipfs.ipfs-build-cache' plugin.
 */
class IpfsBuildCachePluginTest {
    @Test fun `plugin is applied`() {
        // Create a test project and apply the plugin
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("caching.ipfs.ipfs-build-cache")
    }
}
