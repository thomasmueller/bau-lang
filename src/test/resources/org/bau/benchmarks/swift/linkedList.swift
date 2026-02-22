// https://lemire.me/blog/2026/02/15/how-bad-can-python-stop-the-world-pauses-get/

import Foundation

final class Node {
    var value: Int
    var next: Node?

    init(value: Int, next: Node? = nil) {
        self.value = value
        self.next = next
    }
}

func createLinkedList(_ len: Int) -> Node {
    let head = Node(value: 0)
    var current = head
    for i in 1..<len {
        let newNode = Node(value: i)
        current.next = newNode
        current = newNode
    }
    return head
}

print("Starting stress test...")
let x = createLinkedList(50_000_000)
let count = 1_000_000
let len = 1000
var worst: UInt64 = 0
let start = DispatchTime.now().uptimeNanoseconds
var last = start
for _ in 0..<count {
    _ = createLinkedList(len)
    let now = DispatchTime.now().uptimeNanoseconds
    let delta = now - last
    if delta > worst {
        worst = delta
    }
    last = now
}
let total = DispatchTime.now().uptimeNanoseconds - start
let avg = total / UInt64(count)
print(String(
    format: "Max delay: %.3f ms, avg %.3f ms; dummy=%d",
    Double(worst) / 1_000_000.0,
    Double(avg) / 1_000_000.0,
    x.value
))
