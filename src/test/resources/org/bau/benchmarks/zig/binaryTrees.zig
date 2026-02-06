// The Computer Language Benchmarks Game
// https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

const std = @import("std");
const Allocator = std.mem.Allocator;

const Node = struct {
    left: ?*Node = null,
    right: ?*Node = null,

    pub fn buildTree(depth: u32, allocator: Allocator) !*Node {
        if (depth == 0) {
            const node = try allocator.create(Node);
            node.* = Node {
                .left = null,
                .right = null,
            };
            return node;
        }
        const leftNode = try Node.buildTree(depth - 1, allocator);
        const rightNode = try Node.buildTree(depth - 1, allocator);
        const node = try allocator.create(Node);
        node.* = Node {
            .left = leftNode,
            .right = rightNode,
        };
        return node;
    }

    pub fn nodeCount(self: *Node) u32 {
        var result: u32 = 1;
        if (self.left) |l| {
            result += l.nodeCount();
        }
        if (self.right) |r| {
            result += r.nodeCount();
        }
        return result;
    }

    pub fn destroy(self: *Node, allocator: Allocator) void {
        if (self.left) |l| {
            l.destroy(allocator);
        }
        if (self.right) |r| {
            r.destroy(allocator);
        }
        allocator.destroy(self);
    }

};

fn stretch(depth: u32, allocator: Allocator) !void {
    const node = try Node.buildTree(depth, allocator);
    const count = node.nodeCount();
    std.debug.print("stretch tree of depth {}\t check: {}\n", .{depth, count});
    node.destroy(allocator);
}

pub fn main() !void {
    const allocator = std.heap.c_allocator;
    var n: u32 = 10;
    var args = std.process.args();
    _ = args.next();
    if (args.next()) |arg| {
        n = try std.fmt.parseInt(u32, arg, 10);
    }
    const minDepth: u32 = 4;
    var maxDepth: u32 = n;
    if (minDepth + 2 > n) {
        maxDepth = minDepth + 2;
    }
    const stretchDepth: u32 = maxDepth + 1;
    try stretch(stretchDepth, allocator);
    const longLived = try Node.buildTree(maxDepth, allocator);
    var depth: u32 = minDepth;
    while (depth <= maxDepth) : (depth += 2) {
        const iterations: u32 = @as(u32, 1) << @intCast(maxDepth - depth + minDepth);
        var sum: u32 = 0;
        for (0 .. iterations) |_| {
            const node = try Node.buildTree(depth, allocator);
            sum += node.nodeCount();
            node.destroy(allocator);
        }
        std.debug.print("{}\t trees of depth {}\t check: {}\n", .{iterations, depth, sum});
    }
    std.debug.print("long lived tree of depth {}\t check: {}\n", .{maxDepth, longLived.nodeCount()});
    longLived.destroy(allocator);
}
