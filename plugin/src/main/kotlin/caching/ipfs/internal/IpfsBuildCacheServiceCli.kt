package caching.ipfs.internal

fun main() {
    val service = IpfsBuildCacheService()

    var message: String?
    do {
        message = readLine()?.trim()

        if (message == null || message.isEmpty())
            continue

        if (message == ":peers") {
            println(service.router.peers.map { it.peerId }.joinToString(", "))
        } else if (message.startsWith(":add")) {
            val (_, gradleHashCode, ipfsHashCode) = message.split(" ")
            service.kvStore[gradleHashCode] = ipfsHashCode
        } else if (message == ":publish") {
            service.publish()
        } else if (message == ":store") {
            println(service.kvStore)
        } else {
            println("not implemented yet: ${message}")
        }
    } while (":quit" != message)

    service.close()
}
