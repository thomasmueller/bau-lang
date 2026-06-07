// The Computer Language Benchmarks Game
// https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

const std = @import("std");
const math = std.math;

const PI = 3.141592653589793;
const SOLAR_MASS = 4.0 * PI * PI;
const DAYS_PER_YEAR = 365.24;

const Body = struct {
    x: f64,
    y: f64,
    z: f64,
    vx: f64,
    vy: f64,
    vz: f64,
    mass: f64,
};

pub fn main() !void {
    std.debug.print("Zig\n", .{});
    var len: usize = 5;
    var n: usize = 50_000_000;
    const args = try std.process.argsAlloc(std.heap.page_allocator);
    defer std.process.argsFree(std.heap.page_allocator, args);
    if (args.len > 2) {
        len = try std.fmt.parseInt(usize, args[1], 10);
        n = try std.fmt.parseInt(usize, args[2], 10);
    }
    var bodies = try std.heap.page_allocator.alloc(Body, len);
    defer std.heap.page_allocator.free(bodies);    
    if (len > 0) {
        // sun
        bodies[0] = Body { .x = 0, .y = 0, .z = 0, .vx = 0, .vy = 0, .vz = 0, .mass = SOLAR_MASS };
    }        
    if (len > 1) {
        // jupiter
        bodies[1] = Body {
            .x = 4.84143144246472090e0,
            .y = -1.16032004402742839e0,
            .z = -1.03622044471123109e-1,
            .vx = 1.66007664274403694e-3 * DAYS_PER_YEAR,
            .vy = 7.69901118419740425e-3 * DAYS_PER_YEAR,
            .vz = -6.90460016972063023e-5 * DAYS_PER_YEAR,
            .mass = 9.54791938424326609e-4 * SOLAR_MASS,
        };
    }
    if (len > 2) {
        // saturn
        bodies[2] = Body {
            .x = 8.34336671824457987e0,
            .y = 4.12479856412430479e0,
            .z = -4.03523417114321381e-1,
            .vx = -2.76742510726862411e-3 * DAYS_PER_YEAR,
            .vy = 4.99852801234917238e-3 * DAYS_PER_YEAR,
            .vz = 2.30417297573763929e-5 * DAYS_PER_YEAR,
            .mass = 2.85885980666130812e-4 * SOLAR_MASS,
        };
    }
    if (len > 3) {
        // uranus
        bodies[3] = Body {
            .x = 1.28943695621391310e1,
            .y = -1.51111514016986312e1,
            .z = -2.23307578892655734e-1,
            .vx = 2.96460137564761618e-3 * DAYS_PER_YEAR,
            .vy = 2.37847173959480950e-3 * DAYS_PER_YEAR,
            .vz = -2.96589568540237556e-5 * DAYS_PER_YEAR,
            .mass = 4.36624404335156298e-5 * SOLAR_MASS,
        };
    }
    if (len > 4) {
        // neptune
        bodies[4] = Body {
            .x = 1.53796971148509165e1,
            .y = -2.59193146099879641e1,
            .z = 1.79258772950371181e-1,
            .vx = 2.68067772490389322e-3 * DAYS_PER_YEAR,
            .vy = 1.62824170038242295e-3 * DAYS_PER_YEAR,
            .vz = -9.51592254519715870e-5 * DAYS_PER_YEAR,
            .mass = 5.15138902046611451e-5 * SOLAR_MASS,
        };
    }
    offsetMomentum(bodies[0..], bodies.len);
    std.debug.print("{d:.9}\n", .{energy(bodies[0..], bodies.len)});
    var i: usize = 0;
    while (i < n) : (i += 1) {
        advance(bodies[0..], bodies.len, 0.01);
    }
    std.debug.print("{d:.9}\n", .{energy(bodies[0..], bodies.len)});
}

fn offsetMomentum(bodies: []Body, length: usize) void {
    var px: f64 = 0.0;
    var py: f64 = 0.0;
    var pz: f64 = 0.0;
    var i: usize = 0;
    while (i < length) : (i += 1) {
        const b = &bodies[i];
        px += b.vx * b.mass;
        py += b.vy * b.mass;
        pz += b.vz * b.mass;
    }
    bodies[0].vx = -px / SOLAR_MASS;
    bodies[0].vy = -py / SOLAR_MASS;
    bodies[0].vz = -pz / SOLAR_MASS;
}

fn energy(bodies: []const Body, length: usize) f64 {
    var e: f64 = 0.0;
    var i: usize = 0;
    while (i < length) : (i += 1) {
        const b = &bodies[i];
        const sq =
            b.vx * b.vx +
            b.vy * b.vy +
            b.vz * b.vz;
        e += 0.5 * b.mass * sq;
        var j: usize = i + 1;
        while (j < length) : (j += 1) {
            const c = &bodies[j];
            const dx = b.x - c.x;
            const dy = b.y - c.y;
            const dz = b.z - c.z;
            const dsq = dx * dx + dy * dy + dz * dz;
            e -= b.mass * c.mass / @sqrt(dsq);
        }
    }
    return e;
}

fn advance(bodies: []Body, length: usize, dt: f64) void {
    var i: usize = 0;
    while (i < length) : (i += 1) {
        var j: usize = i + 1;
        while (j < length) : (j += 1) {
            const dx = bodies[i].x - bodies[j].x;
            const dy = bodies[i].y - bodies[j].y;
            const dz = bodies[i].z - bodies[j].z;
            const dsq = dx * dx + dy * dy + dz * dz;
            const mag = dt / (dsq * @sqrt(dsq));
            const mj = bodies[j].mass * mag;
            bodies[i].vx -= dx * mj;
            bodies[i].vy -= dy * mj;
            bodies[i].vz -= dz * mj;
            const mi = bodies[i].mass * mag;
            bodies[j].vx += dx * mi;
            bodies[j].vy += dy * mi;
            bodies[j].vz += dz * mi;
        }
    }
    i = 0;
    while (i < length) : (i += 1) {
        bodies[i].x += bodies[i].vx * dt;
        bodies[i].y += bodies[i].vy * dt;
        bodies[i].z += bodies[i].vz * dt;
    }
}
