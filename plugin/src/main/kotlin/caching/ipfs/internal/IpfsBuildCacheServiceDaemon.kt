package caching.ipfs.internal

import io.libp2p.core.Discoverer
import io.libp2p.core.PeerId
import io.libp2p.core.dsl.host
import io.libp2p.core.mux.StreamMuxerProtocol
import io.libp2p.core.pubsub.PubsubPublisherApi
import io.libp2p.core.pubsub.Subscriber
import io.libp2p.core.pubsub.Topic
import io.libp2p.discovery.MDnsDiscovery
import io.libp2p.etc.types.toByteArray
import io.libp2p.etc.types.toByteBuf
import io.libp2p.pubsub.gossip.Gossip
import io.libp2p.pubsub.gossip.GossipRouter
import io.libp2p.security.noise.NoiseXXSecureChannel
import io.libp2p.transport.tcp.TcpTransport
import org.apache.commons.lang3.SerializationUtils
import org.apache.logging.log4j.LogManager
import org.gradle.caching.BuildCacheEntryReader
import org.gradle.caching.BuildCacheEntryWriter
import org.gradle.caching.BuildCacheException
import org.gradle.caching.BuildCacheKey
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.concurrent.ConcurrentHashMap

class IpfsBuildCacheServiceDaemon {
    // TODO("Add LRU eviction mechanism.")
    val kvStore = ConcurrentHashMap<String, String>()
    val router = GossipRouter()
    private val gossip = Gossip(router = router)
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
    // TODO("Make the topic name configurable.")
    private val topic = Topic("/gradle")
    private val publisher: PubsubPublisherApi
    private val subscriber = Subscriber {
        kvStore.putAll(
            SerializationUtils.deserialize(it.data.toByteArray()) as Map<String, String>
        )
        logger.info("Updated KV store")
    }
    private val discoverer: Discoverer

    init {
        host.start().get()
        logger.info("Started ${host.peerId} service on ${host.listenAddresses().joinToString(", ")}")
        gossip.subscribe(subscriber, topic)
        publisher = gossip.createPublisher(host.privKey)
        discoverer = MDnsDiscovery(host, address = privateNetworkAddress)
        discoverer.newPeerFoundListeners.add { newPeer ->
            if (newPeer.peerId != host.peerId && !isConnected(newPeer.peerId)) {
                logger.info("Found new peer ${newPeer.peerId}")
                // NOTE("Establishing the connection is a bit flaky.")
                host.network.connections
                    .filter { it.secureSession().remoteId == newPeer.peerId }
                    .forEach { host.network.disconnect(it) }
                host.network.connect(newPeer.peerId, *newPeer.addresses.toTypedArray()).get()
            }
        }
        discoverer.start().get()
        logger.info("Started peer discovery")
    }

    private fun isConnected(peerId: PeerId): Boolean {
        return router.peers.map { it.peerId }.contains(peerId)
    }

    fun publish() {
        publisher.publish(SerializationUtils.serialize(kvStore).toByteBuf(), topic)
        logger.info("Published KV store")
    }

    fun close() {
        // TODO("Persist KV store on disk.")
        discoverer.stop().get()
        logger.info("Stopped peer discovery")
        host.stop().get()
        logger.info("Stopped service")
    }

    fun load(key: BuildCacheKey, reader: BuildCacheEntryReader): Boolean {
        val gradleHashCode = key.hashCode
        val ipfsHashCode = kvStore[gradleHashCode] ?: return false
        val process = Runtime.getRuntime().exec("ipfs cat $ipfsHashCode")
        process.waitFor()
        if (process.exitValue() != 0) {
            throw BuildCacheException(process.errorStream.bufferedReader().use { it.readText() })
        }
        process.inputStream.use { reader.readFrom(it) }
        logger.info("Loaded $gradleHashCode=$ipfsHashCode")
        return true
    }

    fun store(key: BuildCacheKey, writer: BuildCacheEntryWriter) {
        val gradleHashCode = key.hashCode
        logger.info("Adding $gradleHashCode to KV store")
        val path = kotlin.io.path.createTempFile(gradleHashCode)
        path.toFile().outputStream().use { writer.writeTo(it) }
        val process = Runtime.getRuntime().exec("ipfs add ${path.toAbsolutePath()} -Q")
        process.waitFor()
        if (process.exitValue() != 0) {
            throw BuildCacheException(process.errorStream.bufferedReader().use { it.readText() })
        }
        val ipfsHashCode = process.inputStream.bufferedReader().use { it.readText() }
        kvStore[gradleHashCode] = ipfsHashCode
        logger.info("Added $gradleHashCode=$ipfsHashCode to KV store")
        publish()
    }


    companion object {
        private val logger = LogManager.getLogger(IpfsBuildCacheService::class.java.name)
        private val privateNetworkAddress: InetAddress

        init {
            val interfaces = NetworkInterface.getNetworkInterfaces().toList()
            val addresses = interfaces.flatMap { it.inetAddresses.toList() }
                .filterIsInstance<Inet4Address>()
                .filter { it.isSiteLocalAddress }
                .sortedBy { it.hostAddress }

            privateNetworkAddress = if (addresses.isNotEmpty()) {
                addresses[0]
            } else {
                InetAddress.getLoopbackAddress()
            }
        }
    }
}