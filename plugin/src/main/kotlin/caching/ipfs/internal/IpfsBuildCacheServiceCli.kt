package caching.ipfs.internal

fun main() {
    IpfsBuildCacheService

    help()

    var message: String?
    do {
        message = readLine()?.trim()

        if (message == null || message.isEmpty())
            continue

        if (message == ":help") {
            help()
        } else if (message == ":peers") {
            println(IpfsBuildCacheService.router.peers.map { it.peerId }.joinToString(", "))
        } else if (message.startsWith(":add")) {
            val (_, gradleHashCode, ipfsHashCode) = message.split(" ")
            IpfsBuildCacheService.kvStore[gradleHashCode] = ipfsHashCode
        } else if (message == ":publish") {
            IpfsBuildCacheService.publish()
        } else if (message == ":store") {
            println(IpfsBuildCacheService.kvStore)
        } else {
            println("not implemented yet: $message")
        }
    } while (":quit" != message)

    IpfsBuildCacheService.close()
}

fun help() {
    println("Command            | Action")
    println(":peers             | show connected peers")
    println(":add <KEY> <VALUE> | add key=value to the store")
    println(":publish           | publish store to connected peers")
    println(":store             | show current store contents")
    println(":quit              | stop the service and exit")
    println(":help              | show this message")
}