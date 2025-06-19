// The Computer Language Benchmarks Game
// https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

use std::env;

fn main() {
    let args: Vec<String> = env::args().collect();
    let mut n = 10;
    if args.len() > 1 {
        n = args[1].parse::<i32>().unwrap_or(10);
    }
    let min_depth = 4;
    let max_depth = if min_depth + 2 > n { min_depth + 2 } else { n };
    let stretch_depth = max_depth + 1;
    stretch(stretch_depth);
    let mut long_lived_tree = Tree::with(max_depth);
    for depth in (min_depth..=max_depth).step_by(2) {
        let iterations = 1 << (max_depth - depth + min_depth);
        let mut sum = 0;
        for _ in 0..iterations {
            sum += count(depth);
        }
        println!("{iterations}\t trees of depth {depth}\t check: {sum}");
    }
    let count = long_lived_tree.node_count();
    long_lived_tree.clear();
    println!("long lived tree of depth {max_depth}\t check: {count}");
}

fn stretch(depth: i32) {
    println!("stretch tree of depth {depth}\t check: {}", count(depth));
}

fn count(depth: i32) -> i32 {
    let mut t = Tree::with(depth);
    let c = t.node_count();
    t.clear();
    c
}

#[derive(Debug)]
struct Tree {
    left: Option<Box<Tree>>,
    right: Option<Box<Tree>>,
}

impl Tree {
    fn new(left: Option<Box<Tree>>, right: Option<Box<Tree>>) -> Self {
        Self { left, right }
    }

    fn with(depth: i32) -> Self {
        if depth == 0 {
            Tree::new(None, None)
        } else {
            Tree::new(
                Some(Box::new(Tree::with(depth - 1))),
                Some(Box::new(Tree::with(depth - 1))),
            )
        }
    }

    fn node_count(&self) -> i32 {
        let mut result = 1;
        if let Some(ref l) = self.left {
            result += l.node_count();
        }
        if let Some(ref r) = self.right {
            result += r.node_count();
        }
        result
    }

    fn clear(&mut self) {
        if let Some(ref mut l) = self.left {
            l.clear();
        }
        self.left = None;
        if let Some(ref mut r) = self.right {
            r.clear();
        }
        self.right = None;
    }
}