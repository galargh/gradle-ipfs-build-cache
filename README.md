# IPFS Build Cache for Gradle

This Gradle plugin is an implementation of Gradle [remote build cache](https://docs.gradle.org/current/userguide/build_cache.html) which uses IPFS as a storage layer.

## How to add it to a project?

The project is not published yet. To test it:
1. Clone the project `git clone git@github.com:galargh/gradle-ipfs-build-cache.git`
2. Publish the plugin to Maven Local `gradlew publishToMavenLocal`
3. Configure the remote build cache as in [example](example/settings.gradle.kts) project

## How does it work?

The IPFS Build Cache plugin requires [IPFS daemon](https://docs.ipfs.io/how-to/command-line-quick-start/#take-your-node-online) to be running.

The IPFS Build Cache service runs in the Gradle daemon process. It is started before the first build with remote build cache enabled on that Gradle daemon.

On init, the IPFS Build Cache service starts peer discovery service. The peer discovery service uses [libp2p](https://github.com/libp2p/jvm-libp2p) to discover peers on the **local network** using [multicast DNS](https://github.com/libp2p/specs/blob/master/discovery/mdns.md). Once a peer is discovered, it tries to establish a connection for future use.

To store a build output, the IPFS Build Cache service adds it to IPFS by invoking an IPFS daemon command. The command returns a [content identifier(CID)](https://docs.ipfs.io/concepts/content-addressing/) which is stored in an in-memory map where the keys are Gradle input hashes and the values are CIDs. The CID can later be used to retrieve the build output from IPFS. Finally, on successful map update, the IPFS Build Cache service publishes the map to all connected peers using [gossipsub](https://github.com/libp2p/specs/blob/master/pubsub/gossipsub/gossipsub-v1.1.md). When a peer receives such an update, it adds all the key-value pairs from the received map to its in-memory map.

To load a build output, the IPFS Build Cache service checks if its in-memory map contains a key equal to the requested Gradle input hash. If it does, it reads the corresponding CID from the map and retrieves the build output from IPFS by invoking an IPFS daemon command.
