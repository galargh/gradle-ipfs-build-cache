package caching.ipfs.internal

import caching.ipfs.IpfsBuildCache
import org.gradle.caching.BuildCacheService
import org.gradle.caching.BuildCacheServiceFactory

class IpfsBuildCacheServiceFactory: BuildCacheServiceFactory<IpfsBuildCache> {
    override fun createBuildCacheService(p0: IpfsBuildCache, p1: BuildCacheServiceFactory.Describer): BuildCacheService {
        TODO("Not yet implemented")
    }
}
