package caching.ipfs.internal

import caching.ipfs.IpfsBuildCache
import org.gradle.caching.BuildCacheService
import org.gradle.caching.BuildCacheServiceFactory

class IpfsBuildCacheServiceFactory: BuildCacheServiceFactory<IpfsBuildCache> {
    override fun createBuildCacheService(configuration: IpfsBuildCache,
                                         describer: BuildCacheServiceFactory.Describer): BuildCacheService {
        describer.type("IPFS")
        // TODO("Check if IPFS daemon is up and running. Alternatively, manage IPFS daemon internally.")
        return IpfsBuildCacheService()
    }
}
