// The Computer Language Benchmarks Game
// https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

use std::env;
use std::io::{self, Write, BufWriter};

fn main() -> io::Result<()> {
    let args: Vec<String> = env::args().collect();
    let n: usize = if args.len() > 1 {
        args[1].parse().unwrap_or(6000)
    } else {
        6000
    };
    let inv_n = 2.0 / n as f64;
    let mut crb = vec![0.0f64; n + 7]; // padding as in Java
    for i in 0..n {
        crb[i] = i as f64 * inv_n - 1.5;
    }
    let line_len = (n - 1) / 8 + 1;
    let mut data = vec![0u8; n * line_len];
    for y in 0..n {
        let ciby = y as f64 * inv_n - 1.0;
        let offset = y * line_len;
        for x in 0..line_len {
            data[offset + x] = get_byte(&crb, ciby, x * 8);
        }
    }
    let stdout = io::stdout();
    let mut writer = BufWriter::new(stdout.lock());
    write!(writer, "P4\n{} {}\n", n, n)?;
    writer.write_all(&data)?;
    writer.flush()?;
    Ok(())
}

fn get_byte(crb: &[f64], ciby: f64, x: usize) -> u8 {
    let mut res = 0;
    for i in (0..8).step_by(2) {
        let mut zr1 = crb[x + i];
        let mut zi1 = ciby;
        let mut zr2 = crb[x + i + 1];
        let mut zi2 = ciby;
        let mut b = 0;
        let mut j = 49;
        while j > 0 {
            let nzr1 = zr1 * zr1 - zi1 * zi1 + crb[x + i];
            zi1 = 2.0 * zr1 * zi1 + ciby;
            zr1 = nzr1;
            let nzr2 = zr2 * zr2 - zi2 * zi2 + crb[x + i + 1];
            zi2 = 2.0 * zr2 * zi2 + ciby;
            zr2 = nzr2;
            if zr1 * zr1 + zi1 * zi1 > 4.0 {
                b |= 2;
                if b == 3 { break; }
            }
            if zr2 * zr2 + zi2 * zi2 > 4.0 {
                b |= 1;
                if b == 3 { break; }
            }
            j -= 1;
        }
        res = (res << 2) | b;
    }
    !res as u8
}