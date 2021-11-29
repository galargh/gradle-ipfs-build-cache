package caching.ipfs.internal

import io.libp2p.core.Discoverer
import io.libp2p.core.dsl.host
import io.libp2p.core.mux.StreamMuxerProtocol
import io.libp2p.core.pubsub.PubsubPublisherApi
import io.libp2p.core.pubsub.Subscriber
import io.libp2p.core.pubsub.Topic
import io.libp2p.discovery.MDnsDiscovery
import io.libp2p.etc.types.toByteArray
import io.libp2p.etc.types.toByteBuf
import io.libp2p.pubsub.gossip.Gossip
import io.libp2p.security.noise.NoiseXXSecureChannel
import io.libp2p.transport.tcp.TcpTransport
import org.apache.logging.log4j.LogManager
import org.gradle.caching.*
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap

class IpfsBuildCacheService: BuildCacheService {
    private val kvStore = ConcurrentHashMap<String, String>()
    private val gossip = Gossip()
    private val privateNetworkAddress = privateNetworkAddress()
    private val host = host {
        network {
            listen("/ip4/${privateNetworkAddress.hostAddress}/tcp/0")
        }
        transports {
            add(::TcpTransport)
        }
        secureChannels {
            add(::NoiseXXSecureChannel)
        }
        muxers {
            add(StreamMuxerProtocol.Mplex)
        }
        protocols {
            add(gossip)
        }
    }
    private val topic = Topic("gradle")
    private val publisher: PubsubPublisherApi
    private val subscriber = Subscriber {
        val (gradleHashCode, ipfsHashCode) =
                it.data.toByteArray().toString(StandardCharsets.UTF_8).split(",")
        store(gradleHashCode, ipfsHashCode)
    }
    private val discoverer: Discoverer

    init {
        host.start().get()
        gossip.subscribe(subscriber, topic)
        publisher = gossip.createPublisher(host.privKey)
        discoverer = MDnsDiscovery(host, address = privateNetworkAddress)
        discoverer.newPeerFoundListeners.add {
            logger.info("Found new peer ${it.peerId}")
            // TODO("Request all KV entries and populate the store on new connection.")
            host.network.connect(it.peerId, *it.addresses.toTypedArray())
        }
        discoverer.start().get()
    }

    private fun store(gradleHashCode: String, ipfsHashCode: String) {
        kvStore[gradleHashCode] = ipfsHashCode
    }

    private fun publish(gradleHashCode: String, ipfsHashCode: String) {
        logger.info("Publishing $gradleHashCode=$ipfsHashCode")
        store(gradleHashCode, ipfsHashCode)
        publisher.publish("$gradleHashCode,$ipfsHashCode".toByteArray().toByteBuf(), topic)
    }

    override fun close() {
        discoverer.stop().get()
        host.stop().get()
    }

    override fun load(key: BuildCacheKey, reader: BuildCacheEntryReader): Boolean {
        val gradleHashCode = key.hashCode
        val ipfsHashCode = kvStore[gradleHashCode] ?: return false
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
        publish(gradleHashCode, ipfsHashCode)
    }

    companion object {
        private val logger = LogManager.getLogger(IpfsBuildCacheService::class.java.name)

        private fun privateNetworkAddress(): InetAddress {
            val interfaces = NetworkInterface.getNetworkInterfaces().toList()
            val addresses = interfaces.flatMap { it.inetAddresses.toList() }
                    .filterIsInstance<Inet4Address>()
                    .filter { it.isSiteLocalAddress }
                    .sortedBy { it.hostAddress }
            return if (addresses.isNotEmpty())
                addresses[0]
            else
                InetAddress.getLoopbackAddress()
        }

    }
}
