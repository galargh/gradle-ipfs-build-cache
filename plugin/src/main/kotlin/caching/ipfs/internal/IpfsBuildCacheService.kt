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
import org.gradle.caching.*
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.concurrent.ConcurrentHashMap

class IpfsBuildCacheService: BuildCacheService {
    init {
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
        private val daemon = IpfsBuildCacheServiceDaemon()
    }
}
