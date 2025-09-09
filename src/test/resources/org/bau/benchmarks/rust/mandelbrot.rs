// The Computer Language Benchmarks Game
// https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

use std::env;
use std::io::{self, Write};

fn main() {
    let args: Vec<String> = env::args().collect();
    let mut n = 200;
    if args.len() > 1 {
        n = args[1].parse().unwrap();
    }
    let w = n;
    let h = n;
    let limit = 2.0;
    let iter = 50;
    let mut stdout = io::stdout().lock();
    writeln!(stdout, "P4\n{} {}", w, h).unwrap();
    let mut byte_acc: u8 = 0;
    let mut bit_num = 0;
    for y in 0..h {
        for x in 0..w {
            let mut zr = 0.0;
            let mut zi = 0.0;
            let cr = 2.0 * x as f64 / w as f64 - 1.5;
            let ci = 2.0 * y as f64 / h as f64 - 1.0;
            let mut tr = 0.0;
            let mut ti = 0.0;
            let mut i = 0;
            while i < iter && (tr + ti <= limit * limit) {
                zi = 2.0 * zr * zi + ci;
                zr = tr - ti + cr;
                tr = zr * zr;
                ti = zi * zi;
                i += 1;
            }
            byte_acc <<= 1;
            if tr + ti <= limit * limit {
                byte_acc |= 0x01;
            }
            bit_num += 1;
            if bit_num == 8 {
                stdout.write_all(&[byte_acc]).unwrap();
                byte_acc = 0;
                bit_num = 0;
            } else if x == w - 1 {
                byte_acc <<= 8 - (w % 8);
                stdout.write_all(&[byte_acc]).unwrap();
                byte_acc = 0;
                bit_num = 0;
            }
        }
    }
}