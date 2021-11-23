package caching.ipfs.internal

import org.gradle.caching.BuildCacheEntryReader
import org.gradle.caching.BuildCacheEntryWriter
import org.gradle.caching.BuildCacheKey
import org.gradle.caching.BuildCacheService

class IpfsBuildCacheService: BuildCacheService {
    override fun close() {
        TODO("Not yet implemented")
    }

    override fun load(key: BuildCacheKey, reader: BuildCacheEntryReader): Boolean {
        TODO("Not yet implemented")
    }

    override fun store(key: BuildCacheKey, writer: BuildCacheEntryWriter) {
        TODO("Not yet implemented")
    }
}
