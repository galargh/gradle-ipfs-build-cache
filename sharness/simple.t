#!/bin/sh

test_description="Retrieve build cache entry from IPFS"

: "${SHARNESS_TEST_SRCDIR:=.}"

. "$SHARNESS_TEST_SRCDIR/sharness.sh"

test_expect_success "compileKotlin output was retrieved from cache on IPFS" "
    echo '### Starting cache writer and reader' &&
    docker-compose up --detach &&
    test_when_finished \"echo '### Stopping cache writer and reader' && docker-compose down\" &&
    echo '### Waiting for IPFS daemon in cache writer to be ready' &&
    ( docker logs -f cache-writer & ) | grep -q 'Daemon is ready' &&
    echo '### Waiting for IPFS daemon in cache reader to be ready' &&
    ( docker logs -f cache-reader & ) | grep -q 'Daemon is ready' &&
    echo '### Starting Gradle daemon in cache writer' &&
    docker exec cache-writer gradle help --build-cache &&
    echo '### Starting Gradle daemon in cache reader' &&
    docker exec cache-reader gradle help --build-cache &&
    echo '### Writing to cache from cache writer' &&
    docker exec cache-writer gradle build --build-cache &&
    echo '### Reading from cache in cache reader' &&
    docker exec cache-reader gradle build --build-cache | tee '$SHARNESS_TRASH_DIRECTORY/read.out' &&
    echo '### Checking if compileKotlin task was read from cache' &&
    cat '$SHARNESS_TRASH_DIRECTORY/read.out' | grep -q '> Task :compileKotlin FROM-CACHE' &&
    echo success
"

test_done

# vi: set ft=sh.sharness :
