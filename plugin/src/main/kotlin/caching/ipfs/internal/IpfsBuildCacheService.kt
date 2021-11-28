package caching.ipfs.internal

import org.gradle.caching.*

class IpfsBuildCacheService: BuildCacheService {
    // TODO("Replace with p2p KV store")
    private val kvStore = mutableMapOf<String, String>()

    override fun close() {
        // NOTE("IPFS daemon is managed externally. There is nothing to close.")
    }

    override fun load(key: BuildCacheKey, reader: BuildCacheEntryReader): Boolean {
        val gradleHashCode = key.hashCode
        val ipfsHashCode = kvStore[gradleHashCode]
        val process = Runtime.getRuntime().exec("ipfs cat $ipfsHashCode")
        process.waitFor()
        if (process.exitValue() != 0) {
            throw BuildCacheException(process.errorStream.bufferedReader().use { it.readText() })
        }
        process.inputStream.use { reader.readFrom(it) }
        return true
    }

    override fun store(key: BuildCacheKey, writer: BuildCacheEntryWriter) {
        val gradleHashCode = key.hashCode
        val path = kotlin.io.path.createTempFile(gradleHashCode)
        path.toFile().outputStream().use { writer.writeTo(it) }
        val process = Runtime.getRuntime().exec("ipfs add ${path.toAbsolutePath()} -Q")
        process.waitFor()
        if (process.exitValue() != 0) {
            throw BuildCacheException(process.errorStream.bufferedReader().use { it.readText() })
        }
        val ipfsHashCode = process.inputStream.bufferedReader().use { it.readText() }
        kvStore[gradleHashCode] = ipfsHashCode
    }
}
