// The Computer Language Benchmarks Game
// https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

const std = @import("std");

pub fn main() !void {
    var n: usize = 200;
    var args = std.process.args();
    _ = args.next();
    if (args.next()) |arg| {
        n = try std.fmt.parseInt(usize, arg, 10);
    }
    var stdout = std.fs.File.stdout().writerStreaming(&.{});
    const w = n;
    const h = n;
    var bitNum: u8 = 0;
    var byteAcc: u8 = 0;
    const iter = 50;
    const limit = 2.0;
    try stdout.interface.print("P4\n{d} {d}\n", .{ w, h });
    var y: usize = 0;
    while (y < h) : (y += 1) {
        var x: usize = 0;
        while (x < w) : (x += 1) {
            var Zr: f64 = 0.0;
            var Zi: f64 = 0.0;
            var Tr: f64 = 0.0;
            var Ti: f64 = 0.0;
            const Cr = 2.0 * @as(f64, @floatFromInt(x)) / @as(f64, @floatFromInt(w)) - 1.5;
            const Ci = 2.0 * @as(f64, @floatFromInt(y)) / @as(f64, @floatFromInt(h)) - 1.0;
            var i: usize = 0;
            while (i < iter and Tr + Ti <= limit * limit) : (i += 1) {
                Zi = 2.0 * Zr * Zi + Ci;
                Zr = Tr - Ti + Cr;
                Tr = Zr * Zr;
                Ti = Zi * Zi;
            }
            byteAcc <<= 1;
            if (Tr + Ti <= limit * limit) {
                byteAcc |= 0x01;
            }
            bitNum += 1;
            if (bitNum == 8) {
                try stdout.interface.writeByte(byteAcc);
                byteAcc = 0;
                bitNum = 0;
            } else if (x == w - 1) {
                byteAcc <<= @as(u3, @intCast(8 - w % 8));
                try stdout.interface.writeByte(byteAcc);
                byteAcc = 0;
                bitNum = 0;
            }
        }
    }
}
