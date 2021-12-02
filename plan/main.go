package main

import (
	"errors"
	"fmt"

	"github.com/testground/sdk-go/run"
	"github.com/testground/sdk-go/runtime"
)

func main() {
	run.Invoke(runf)
}

// Pick a different example function to run
// depending on the name of the test case.
func runf(runenv *runtime.RunEnv) error {
	switch c := runenv.TestCase; c {
	default:
		msg := fmt.Sprintf("Unknown Testcase %s", c)
		return errors.New(msg)
	}
}
