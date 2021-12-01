package caching.ipfs.internal

import org.gradle.caching.*

class IpfsBuildCacheService: BuildCacheService {
    init {
        // TODO("Recreate the daemon if the configuration changes.")
        IpfsBuildCacheService
    }

    override fun close() {
        // NOTE("This is called when a build finishes, not when the Gradle daemon shuts down as we would want.")
        // daemon.close()
    }

    override fun load(key: BuildCacheKey, reader: BuildCacheEntryReader): Boolean {
        return daemon.load(key, reader)
    }

    override fun store(key: BuildCacheKey, writer: BuildCacheEntryWriter) {
        daemon.store(key, writer)
    }

    companion object {
        private val daemon = IpfsBuildCacheDaemon()
    }
}
