package caching.ipfs

import kotlin.test.Test
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder

/**
 * A simple functional test for the 'caching.ipfs.ipfs-build-cache' plugin.
 */
class IpfsBuildCachePluginFunctionalTest {
    @get:Rule val tempFolder = TemporaryFolder()

    private fun getProjectDir() = tempFolder.root
    private fun getBuildFile() = getProjectDir().resolve("build.gradle")
    private fun getSettingsFile() = getProjectDir().resolve("settings.gradle")

    @Test fun `can apply plugin`() {
        // Setup the test build
        getSettingsFile().writeText("""
plugins {
    id('caching.ipfs.ipfs-build-cache')
}
""")
        getBuildFile().writeText("")

        // Run the build
        val runner = GradleRunner.create()
        runner.withPluginClasspath()
        runner.withArguments("help")
        runner.withProjectDir(getProjectDir())
        runner.build();
    }
}
