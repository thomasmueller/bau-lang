// https://github.com/lemire/Code-used-on-Daniel-Lemire-s-blog/blob/master/2026/02/13/stress_gc.go

package main

import (
    "fmt"
    "time"
)

type Node struct {
    value int
    next  *Node
}

func createLinkedList(len int) *Node {
    head := &Node{value: 0}
    current := head
    for i := 1; i < len; i++ {
        current.next = &Node{value: i}
        current = current.next
    }
    return head
}

func main() {
    fmt.Println("Go")
    x := createLinkedList(50_000_000)
    count := 1_000_000
    len := 1000
    worst := int64(0)
    start := time.Now().UnixNano()
    last := start
    for i := 0; i < count; i++ {
        createLinkedList(len)
        now := time.Now().UnixNano()
        worst = max(worst, now - last)
        last = now
    }
    avg := (time.Now().UnixNano() - start) / int64(count)
    fmt.Printf("Max delay: %d ms, avg %d ms; dummy=%d\n", 
        worst / 1_000_000.0, avg / 1_000_000.0, x.value)
}
