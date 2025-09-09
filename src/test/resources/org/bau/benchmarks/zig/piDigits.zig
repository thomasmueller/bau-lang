// The Computer Language Benchmarks Game
// https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

const std = @import("std");

var acc: std.math.big.int.Managed = undefined;
var den: std.math.big.int.Managed = undefined;
var num: std.math.big.int.Managed = undefined;
var ten: std.math.big.int.Managed = undefined;

pub fn main() !void {
    acc = try bigInit();
    defer acc.deinit();
    den = try bigInit();
    defer den.deinit();
    num = try bigInit();
    defer num.deinit();
    ten = try bigInit();
    defer ten.deinit();
    var n: usize = 10000;
    var args = std.process.args();
    _ = args.next();
    if (args.next()) |arg| {
        n = try std.fmt.parseInt(usize, arg, 10);
    }
    try acc.set(0);
    try den.set(1);
    try num.set(1);
    try ten.set(10);
    var i: usize = 0;
    var k: usize = 0;
    var stdout = std.fs.File.stdout().writerStreaming(&.{});
    while (i < n) {
        k += 1;
        try next_term(k);
        if (num.order(acc) == .gt) {
            continue;
        }
        const d3 = try extract_digit(3);
        const d4 = try extract_digit(4);
        if (d3 != d4) {
            continue;
        }
        try eliminate_digit(d3);
        try stdout.interface.print("{c}", .{'0' + @as(u8, @intCast(d3))});
        i += 1;
        if (i % 10 == 0) {
            try stdout.interface.print("\t:{d}\n", .{ i });
        }
    }
}

fn extract_digit(nth: usize) !usize {
    var tmp1 = try bigInit();
    defer tmp1.deinit();
    var tmp2 = try bigInit();
    defer tmp2.deinit();
    var tmp3 = try bigInit();
    defer tmp3.deinit();
    var nth1 = try bigInit();
    defer nth1.deinit();
    try nth1.set(nth);
    try tmp1.mul(&num, &nth1);
    try tmp1.add(&tmp1, &acc);
    try std.math.big.int.Managed.divTrunc(&tmp2, &tmp3, &tmp1, &den);
    return try tmp2.toInt(usize);
}

fn eliminate_digit(d: usize) !void {
    var tmp = try bigInit();
    defer tmp.deinit();
    var d1 = try bigInit();
    defer d1.deinit();
    try d1.set(d);
    try tmp.mul(&den, &d1);
    try acc.sub(&acc, &tmp);
    try acc.mul(&acc, &ten);
    try num.mul(&num, &ten);
}

fn next_term(k: usize) !void {
    var k2 = try bigInit();
    defer k2.deinit();
    try k2.set(k * 2 + 1);
    var tmp = try bigInit();
    defer tmp.deinit();
    try tmp.shiftLeft(&num, 1);
    try acc.add(&acc, &tmp);
    try acc.mul(&acc, &k2);
    try den.mul(&den, &k2);
    var k3 = try bigInit();
    defer k3.deinit();
    try k3.set(k);
    try num.mul(&num, &k3);
}

fn bigInit() !std.math.big.int.Managed {
    return try std.math.big.int.Managed.init(std.heap.c_allocator);
}
