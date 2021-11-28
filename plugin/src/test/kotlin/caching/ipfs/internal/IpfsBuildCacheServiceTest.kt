package caching.ipfs.internal

import org.gradle.caching.BuildCacheEntryReader
import org.gradle.caching.BuildCacheEntryWriter
import org.gradle.caching.BuildCacheKey
import org.gradle.internal.hash.HashCode
import java.io.InputStream
import java.io.OutputStream
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A simple unit test for the 'caching.ipfs.ipfs-build-cache' plugin.
 */
class IpfsBuildCacheServiceTest {
    @Test fun `artifact is cached`() {
        val entry = "build cache entry"
        val key = BuildCacheKeyImpl()
        val writer = BuildCacheEntryWriterImpl(entry.toByteArray())
        val service = IpfsBuildCacheService()
        val reader = BuildCacheEntryReaderImpl()

        service.store(key, writer)
        service.load(key, reader)

        assertEquals(entry, reader.text)
    }

    class BuildCacheKeyImpl constructor(private val hashCode: HashCode = HashCode.fromString("01234567abcdef"))
        : BuildCacheKey {
        override fun getDisplayName(): String {
            return hashCode.toString()
        }

        override fun getHashCode(): String {
            return hashCode.toString()
        }

        override fun toByteArray(): ByteArray {
            return hashCode.toByteArray()
        }
    }

    class BuildCacheEntryWriterImpl constructor(private val byteArray: ByteArray) : BuildCacheEntryWriter {
        override fun writeTo(output: OutputStream) {
            output.use { it.write(byteArray) }
        }

        override fun getSize(): Long {
            return byteArray.size.toLong()
        }
    }

    class BuildCacheEntryReaderImpl : BuildCacheEntryReader {
        var text: String = ""

        override fun readFrom(input: InputStream) {
            text = input.bufferedReader().use { it.readText() }
        }
    }
}
