//! The Computer Language Benchmarks Game
//! https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

extern crate num_bigint;
extern crate num_traits;

use num_bigint::BigInt;
use num_bigint::ToBigInt;
use num_traits::ToPrimitive;
use std::env;
use std::fmt::Write;

fn main() {
    let args: Vec<String> = env::args().collect();
    let n: usize = if args.len() > 1 {
        args[1].parse().unwrap()
    } else {
        10000
    };
    let mut pi = PiDigits::new();
    pi.run(n);
}

struct PiDigits {
    ten: BigInt,
    acc: BigInt,
    den: BigInt,
    num: BigInt,
}

impl PiDigits {
    fn new() -> Self {
        Self {
            ten: 10.to_bigint().unwrap(),
            acc: BigInt::ZERO,
            den: 1.to_bigint().unwrap(),
            num: 1.to_bigint().unwrap(),
        }
    }

    fn extract_digit(&self, nth: i64) -> i64 {
        let d = (&self.num * nth + &self.acc) / &self.den;
        d.to_i64().unwrap()
    }

    fn eliminate_digit(&mut self, d: i64) {
        let d_big = BigInt::from(d);
        self.acc = (&self.acc - &self.den * &d_big) * &self.ten;
        self.num = &self.num * &self.ten;
    }

    fn next_term(&mut self, k: i64) {
        let k2p1 = BigInt::from(2 * k + 1);
        self.acc = (&self.acc + (&self.num << 1)) * &k2p1;
        self.den = &self.den * &k2p1;
        self.num = &self.num * BigInt::from(k);
    }

    fn run(&mut self, n: usize) {
        let mut k = 0;
        let mut i = 0;
        let mut output = String::new();
        while i < n {
            k += 1;
            self.next_term(k);
            if self.num > self.acc {
                continue;
            }
            let d = self.extract_digit(3);
            if d != self.extract_digit(4) {
                continue;
            }
            self.eliminate_digit(d);
            output.push(char::from_u32((b'0' + (d as u8)) as u32).unwrap());
            i += 1;
            if i % 10 == 0 {
                write!(&mut output, " : {}", i).unwrap();
                println!("{}", output);
                output.truncate(0);
            }
        }
    }
}
