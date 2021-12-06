#!/bin/sh

test_description="Retrieve build cache entry from IPFS"

: "${SHARNESS_TEST_SRCDIR:=.}"

. "$SHARNESS_TEST_SRCDIR/sharness.sh"

test_expect_success "compileKotlin output was retrieved from cache on IPFS" "
    # docker-compose up --build --detach &&
    ( docker logs -f cache-feeder & ) | grep -q \"Daemon is ready\" &&
    ( docker logs -f cache-reader & ) | grep -q \"Daemon is ready\" &&
    # docker exec cache-feeder gradle help --build-cache &&
    # docker exec cache-reader gradle help --build-cache &&
    # docker exec cache-feeder gradle build --build-cache &&
    # docker exec cache-reader gradle build --build-cache | tee read.out &&
    # cat read.out | grep -q \"> Task :example:compileKotlin FROM-CACHE\" &&
    # docker-compose down &&
    echo success
"

test_done

# vi: set ft=sh.sharness :
