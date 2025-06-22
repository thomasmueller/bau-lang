// The Computer Language Benchmarks Game
// https://salsa.debian.org/benchmarksgame-team/benchmarksgame/
use std::env;

fn main() {
    let args: Vec<String> = env::args().collect();
    let n: usize = if args.len() > 1 {
        args[1].parse().unwrap()
    } else {
        12
    };
    println!("Pfannkuchen({}) = {}", n, fannkuch(n));
}

fn fannkuch(n: usize) -> i32 {
    let mut perm1 = vec![0; n];
    for i in 0..n {
        perm1[i] = i;
    }
    let mut perm = vec![0; n];
    let mut count = vec![0; n];
    let mut flips = 0;
    let mut nperm = 0;
    let mut checksum = 0;
    let mut r = n;
    while r > 0 {
        while r > 1 {
            count[r - 1] = r;
            r -= 1;
        }
        for i in 0..n {
            perm[i] = perm1[i];
        }
        // Count flips
        let mut f = 0;
        let mut k = perm[0];
        while k != 0 {
            let mut i = 0;
            while 2 * i < k {
                perm.swap(i, k - i);
                i += 1;
            }
            k = perm[0];
            f += 1;
        }
        if f > flips {
            flips = f;
        }
        if nperm & 1 == 0 {
            checksum += f;
        } else {
            checksum -= f;
        }
        // Generate next permutation
        let mut more = true;
        while more {
            if r == n {
                println!("{}", checksum);
                return flips;
            }
            let p0 = perm1[0];
            let mut i = 0;
            while i < r {
                perm1[i] = perm1[i + 1];
                i += 1;
            }
            perm1[r] = p0;
            count[r] -= 1;
            if count[r] > 0 {
                more = false;
            } else {
                r += 1;
            }
        }
        nperm += 1;
    }
    flips
}