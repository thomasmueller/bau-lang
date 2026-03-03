# https://lemire.me/blog/2026/02/15/how-bad-can-python-stop-the-world-pauses-get/

import time

class Node:
    def __init__(self, value):
        self.value = value
        self.next = None
    def add_next(self, node):
        self.next = node

def create_linked_list(limit):
    """ create a linked list of length 'limit' """
    head = Node(0)
    current = head
    for i in range(1, limit):
        new_node = Node(i)
        current.add_next(new_node)
        current = new_node
    return head

def main():
    print("Python")
    x = create_linked_list(50_000_000)
    count = 1_000_000
    len = 1000
    worst = 0.0
    last = time.perf_counter_ns()
    for i in range(1_000_000):
        create_linked_list(1000)
        now = time.perf_counter_ns()
        worst = max(worst, now - last)
        last = now
    print(f"Max delay: {worst / 1_000_000} ms; dummy value: {x.value}", flush=True)

main()