#!/bin/sh

test_description="Retrieve build cache entry from IPFS"

: "${SHARNESS_TEST_SRCDIR:=.}"

. "$SHARNESS_TEST_SRCDIR/sharness.sh"

test_expect_success "Success is reported like this" "
    docker compose up --build --detach
    ( docker logs -f gradle-ipfs-build-cache-cache-feeder-1 & ) | grep -q \"Daemon is ready\"
    ( docker logs -f gradle-ipfs-build-cache-cache-reader-1 & ) | grep -q \"Daemon is ready\"
    docker exec gradle-ipfs-build-cache-cache-feeder-1 gradle help --build-cache
    docker exec gradle-ipfs-build-cache-cache-reader-1 gradle help --build-cache
    docker exec gradle-ipfs-build-cache-cache-feeder-1 gradle build --build-cache
    docker exec gradle-ipfs-build-cache-cache-reader-1 gradle build --build-cache
    # check FROM-CACHE
    echo success
"

test_expect_success "Commands are chained this way" "
    test x = 'x' &&
    test 2 -gt 1 &&
    echo success
"

return_42() {
    echo "Will return soon"
    return 42
}

test_expect_success "You can test for a specific exit code" "
    test_expect_code 42 return_42
"

test_expect_failure "We expect this to fail" "
    test 1 = 2
"

test_done

# vi: set ft=sh.sharness :
