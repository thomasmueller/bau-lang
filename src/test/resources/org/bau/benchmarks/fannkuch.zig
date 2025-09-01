// The Computer Language Benchmarks Game
// https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

const std = @import("std");

pub fn main() !void {
    var stdout = std.fs.File.stdout().writerStreaming(&.{});
    var args = std.process.args();
    _ = args.next();
    var n: u32 = 4;
    if (args.next()) |arg| {
        n = try std.fmt.parseInt(u32, arg, 10);
    }
    const flips = fannkuch(n);
    try stdout.interface.print("Pfannkuchen({}) = {}\n", .{n, flips});
}

fn fannkuch(n: u32) u32 {
    var perm1: [128]u32 = undefined;
    var perm: [128]u32 = undefined;
    var count: [128]u32 = undefined;
    for (0 .. n) |i| {
        perm1[i] = @intCast(i);
    }
    var f: u32 = 0;
    var k: u32 = 0;
    var r: u32 = n;
    var flips: u32 = 0;
    var nperm: u32 = 0;
    var checksum: i32 = 0;
    while (r > 0) {
        while (r > 1) : (r -= 1) {
            count[r - 1] = r;
        }
        for (0 .. n) |i| {
            perm[i] = perm1[i];
        }
        // Count flips and update max and checksum
        f = 0;
        k = perm[0];
        while (k != 0) : (f += 1) {
            for (0 .. ((k + 1) / 2)) |i| {
                const t = perm[i];
                perm[i] = perm[k - i];
                perm[k - i] = t;
            }
            k = perm[0];
        }
        if (f > flips) {
            flips = f;
        }
        if ((nperm & 1) == 0) {
            checksum += @intCast(f);
        } else {
            checksum -= @intCast(f);
        }
        // Use incremental change to generate another permutation
        while (true) {
            if (r == n) {
                std.debug.print("{}\n", .{checksum});
                return flips;
            }
            const p0 = perm1[0];
            var i: u32 = 0;
            while (i < r) : (i += 1) {
                const j = i + 1;
                perm1[i] = perm1[j];
            }
            perm1[r] = p0;
            count[r] -= 1;
            if (count[r] > 0) {
                break;
            }
            r += 1;
        }
        nperm += 1;
    }
    return flips;
}
