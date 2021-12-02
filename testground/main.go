package main

import (
	"github.com/testground/sdk-go/run"
)

var cases = map[string]interface{}{
	"from-cache": run.InitializedTestCaseFn(fromCache),
}

func main() {
	run.InvokeMap(cases)
}
