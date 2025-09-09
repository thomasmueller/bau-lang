// https://github.com/jabbalaci/SpeedTests

const std = @import("std");

const LIMIT: u64 = 440_000_000;

pub fn main() !void {
    var stdout = std.fs.File.stdout().writerStreaming(&.{});
    const cache = getCache();
    var i: u64 = 0;
    while (i < LIMIT) : (i += 1) {
        if (isMunchausen(i, &cache)) {
            try stdout.interface.print("{}\n", .{i});
        }
    }
}

fn isMunchausen(number: u64, cache: *const [10]u64) bool {
    var n = number;
    var total: u64 = 0;
    while (n > 0) {
        const digit = n % 10;
        total += cache[digit];
        if (total > number) {
            return false;
        }
        n /= 10;
    }
    return total == number;
}

fn getCache() [10]u64 {
    var result: [10]u64 = [_]u64{0} ** 10;
    var i: u64 = 1;
    while (i <= 9) : (i += 1) {
        result[i] = powInt(i, i);
    }

    return result;
}

fn powInt(base: u64, exp: u64) u64 {
    var result: u64 = 1;
    var e = exp;
    while (e > 0) : (e -= 1) {
        result *= base;
    }
    return result;
}
