package caching.ipfs.internal

fun main() {
    var message: String?
    do {
        message = readLine()?.trim()

        if (message == null || message.isEmpty())
            continue

        if (message == ":peers") {
            println(IpfsBuildCacheService.router.peers.map { it.peerId }.joinToString(", "))
        } else if (message.startsWith(":add")) {
            val (_, gradleHashCode, ipfsHashCode) = message.split(" ")
            IpfsBuildCacheService.kvStore[gradleHashCode] = ipfsHashCode
        } else if (message == ":publish") {
            IpfsBuildCacheService.publish()
        } else if (message == ":store") {
            println(IpfsBuildCacheService.kvStore)
        } else {
            println("not implemented yet: ${message}")
        }
    } while (":quit" != message)

    IpfsBuildCacheService.close()
}
