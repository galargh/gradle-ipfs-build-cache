package main

import (
	"context"
	"fmt"
	"io/ioutil"
	"net"
	"os"
	"os/exec"
	"strings"
	"time"

	"github.com/testground/sdk-go/network"
	"github.com/testground/sdk-go/run"
	"github.com/testground/sdk-go/runtime"
)

func fromCache(runenv *runtime.RunEnv, initCtx *run.InitContext) error {
	ctx, cancel := context.WithTimeout(context.Background(), 300*time.Second)
	defer cancel()

	runenv.RecordMessage("before sync.MustBoundClient")
	client := initCtx.SyncClient
	netclient := initCtx.NetClient

	oldAddrs, err := net.InterfaceAddrs()
	if err != nil {
		return err
	}

	config := &network.Config{
		// Control the "default" network. At the moment, this is the only network.
		Network: "default",

		// Enable this network. Setting this to false will disconnect this test
		// instance from this network. You probably don't want to do that.
		Enable: true,
		Default: network.LinkShape{
			Latency:   100 * time.Millisecond,
			Bandwidth: 1 << 20, // 1Mib
		},
		CallbackState: "network-configured",
		RoutingPolicy: network.AllowAll,
	}

	runenv.RecordMessage("before netclient.MustConfigureNetwork")
	netclient.MustConfigureNetwork(ctx, config)

	seq := client.MustSignalAndWait(ctx, "ip-allocation", runenv.TestInstanceCount)

	// Make sure that the IP addresses don't change unless we request it.
	if newAddrs, err := net.InterfaceAddrs(); err != nil {
		return err
	} else if !sameAddrs(oldAddrs, newAddrs) {
		return fmt.Errorf("interfaces changed")
	}

	runenv.RecordMessage("I am %d", seq)

	ipC := byte((seq >> 8) + 1)
	ipD := byte(seq)

	config.IPv4 = runenv.TestSubnet
	config.IPv4.IP = append(config.IPv4.IP[0:2:2], ipC, ipD)
	config.IPv4.Mask = []byte{255, 255, 255, 0}
	config.CallbackState = "ip-changed"

	var (
		listener *net.TCPListener
		conn     *net.TCPConn
	)

	if seq == 1 {
		listener, err = net.ListenTCP("tcp4", &net.TCPAddr{Port: 1234})
		if err != nil {
			return err
		}
		defer listener.Close()
	}

	runenv.RecordMessage("before reconfiguring network")
	netclient.MustConfigureNetwork(ctx, config)

	switch seq {
	case 1:
		conn, err = listener.AcceptTCP()
	case 2:
		conn, err = net.DialTCP("tcp4", nil, &net.TCPAddr{
			IP:   append(config.IPv4.IP[:3:3], 1),
			Port: 1234,
		})
	default:
		return fmt.Errorf("expected at most two test instances")
	}
	if err != nil {
		return err
	}

	runenv.RecordMessage(conn.LocalAddr().String())

	defer conn.Close()

	buf := make([]byte, 1)

	runenv.RecordMessage("starting ipfs daemon")

	cmd := exec.Command("ipfs", "daemon")
	stdout, err := cmd.StdoutPipe()
	cmd.Stderr = cmd.Stdout
	if err != nil {
		return err
	}
	err = cmd.Start()
	if err != nil {
		return err
	}

	runenv.RecordMessage("waiting until ready")

	ready := "Daemon is ready"
	prev := ""
	for {
		bytes := make([]byte, len([]byte(ready)))
		_, err := stdout.Read(bytes)
		if err != nil {
			return err
		}
		curr := string(bytes)
		if strings.Contains(prev+curr, ready) {
			break
		}
		prev = curr
	}

	runenv.RecordMessage(ready)

	// wait till both sides are ready
	_, err = conn.Write([]byte{0})
	if err != nil {
		return err
	}

	_, err = conn.Read(buf)
	if err != nil {
		return err
	}

	runenv.RecordMessage("checking id")
	cmd = exec.Command("ipfs", "id")
	output, err := cmd.CombinedOutput()
	if err != nil {
		return err
	}
	runenv.RecordMessage(string(output))

	runenv.RecordMessage("checking peers")
	cmd = exec.Command("ipfs", "swarm", "peers")
	output, err = cmd.CombinedOutput()
	if err != nil {
		return err
	}
	runenv.RecordMessage(string(output))

	if seq == 1 {
		runenv.RecordMessage("starting my ipfs build cache daemon")
		file, err := ioutil.TempFile(os.TempDir(), "file-")
		if err != nil {
			return err
		}
		defer os.Remove(file.Name())
		_, err = file.Write([]byte("i'm adding this in container 1"))
		if err != nil {
			return err
		}
		cmd = exec.Command("ipfs", "add", "-Q", file.Name())
		output, err = cmd.CombinedOutput()
		if err != nil {
			return err
		}
		output = output[:len(output)-1]
		runenv.RecordMessage("published: " + string(output))

		// gradle help --build-cache
		_, err = conn.Write(output)
		if err != nil {
			return err
		}
		_, err = conn.Read(buf)
		if err != nil {
			return err
		}

		runenv.RecordMessage("filling the remote build cache")
		// gradle compileKotlin --build-cache
		_, err = conn.Write([]byte{0})
		if err != nil {
			return err
		}
		_, err = conn.Read(buf)
		if err != nil {
			return err
		}
	}

	if seq == 2 {
		cid := make([]byte, 256)
		_, err = conn.Read(cid)
		if err != nil {
			return err
		}
		runenv.RecordMessage("ipfs cat " + string(cid) + "EOC")

		cmd = exec.Command("ipfs", "cat", "QmSQnkUevN1qe3pdzDpDqJf494QULEae2tM3DZ3nDPQ4qv")
		output, err = cmd.CombinedOutput()
		if err != nil {
			return err
		}
		runenv.RecordMessage("retrieved: " + string(output))

		runenv.RecordMessage("starting my ipfs build cache daemon")
		// gradle help --build-cache
		_, err = conn.Write([]byte{0})
		if err != nil {
			return err
		}

		_, err = conn.Read(buf)
		if err != nil {
			return err
		}
		runenv.RecordMessage("reading the remote build cache")
		// gradle compileKotlin --build-cache
		_, err = conn.Write([]byte{0})
		if err != nil {
			return err
		}
	}

	// check output
	// stop gradle daemon
	// stop ipfs daemon

	return nil
}

func sameAddrs(a, b []net.Addr) bool {
	if len(a) != len(b) {
		return false
	}
	aset := make(map[string]bool, len(a))
	for _, addr := range a {
		aset[addr.String()] = true
	}
	for _, addr := range b {
		if !aset[addr.String()] {
			return false
		}
	}
	return true
}
