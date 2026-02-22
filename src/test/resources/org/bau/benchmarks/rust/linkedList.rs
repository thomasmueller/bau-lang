// https://lemire.me/blog/2026/02/15/how-bad-can-python-stop-the-world-pauses-get/

use std::time::Instant;

struct Node {
    value: i32,
    next: Option<Box<Node>>,
}

fn create_linked_list(len: usize) -> Box<Node> {
    let mut head = Box::new(Node {
        value: 0,
        next: None,
    });
    let mut current = &mut head;
    for i in 1..len {
        current.next = Some(Box::new(Node {
            value: i as i32,
            next: None,
        }));
        current = current.next.as_mut().unwrap();
    }
    head
}

impl Drop for Node {
    fn drop(&mut self) {
        // Take the next node out so Rust doesn't recursively drop it.
        let mut current = self.next.take();
        while let Some(mut boxed) = current {
            // Detach the next pointer before boxed goes out of scope.
            current = boxed.next.take();
            // boxed is dropped here, but its `next` is already None,
            // so no recursive drop happens.
        }
    }
}

fn main() {
    println!("Starting stress test...");
    let x = create_linked_list(5000);
    let count: usize = 1_000_000;
    let len: usize = 1000;
    let mut worst: u64 = 0;
    let mut last = Instant::now();
    for _ in 0..count {
        create_linked_list(len);
        let now = Instant::now();
        let elapsed = (now - last).as_nanos() as u64;
        worst = std::cmp::max(worst, elapsed);
        last = now;
    }
    println!("Max delay: {} ms; dummy={}", (worst as f64) / 1_000_000.0, x.value);
}