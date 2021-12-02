ARG source_directory=.

FROM gradle:7.3.0-jdk11 AS gradle-builder
WORKDIR /build
COPY ${source_directory} .
RUN gradle publish --no-daemon --no-watch-fs

FROM golang:1.16-alpine AS go-builder
WORKDIR /build
ENV CGO_ENABLED 0
COPY ${source_directory}/testground .
RUN go build -a

FROM alpine AS ipfs-downloader
WORKDIR /download
RUN wget https://dist.ipfs.io/go-ipfs/v0.10.0/go-ipfs_v0.10.0_linux-amd64.tar.gz
RUN tar -xvzf go-ipfs_v0.10.0_linux-amd64.tar.gz

FROM --platform=linux/amd64 gradle:7.3.0-jdk11
COPY --from=gradle-builder /build/plugin/build/.m2 /root/.m2
COPY --from=go-builder /build/testground /usr/local/bin/testground
COPY --from=ipfs-downloader /download/go-ipfs/ipfs /usr/local/bin/ipfs
RUN ipfs init --profile server
# IPFS
EXPOSE 4001
EXPOSE 4001/udp
EXPOSE 5001
EXPOSE 8080
EXPOSE 8081
# testground
EXPOSE 6060
ENTRYPOINT ["testground"]
