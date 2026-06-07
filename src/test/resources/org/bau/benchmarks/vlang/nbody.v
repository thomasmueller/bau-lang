// The Computer Language Benchmarks Game
// https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

import math
import os

const pi = 3.141592653589793
const solar_mass = 4.0 * pi * pi
const days_per_year = 365.24

struct Body {
mut:
    x    f64
    y    f64
    z    f64
    vx   f64
    vy   f64
    vz   f64
    mass f64
}

fn main() {
    println('V')
    mut len := 5
    mut n := 50_000_000
    if os.args.len > 2 {
        len = os.args[1].int()
        n = os.args[2].int()
    }
    mut bodies := []Body{len: len}
    if len > 0 {
        // sun
        bodies[0] = Body { 0, 0, 0, 0, 0, 0, solar_mass }
    }
    if len > 1 {
        // jupiter
        bodies[1] = Body {
            4.84143144246472090e0
            -1.16032004402742839e0
            -1.03622044471123109e-1
            1.66007664274403694e-3 * days_per_year
            7.69901118419740425e-3 * days_per_year
            -6.90460016972063023e-5 * days_per_year
            9.54791938424326609e-4 * solar_mass
        }
    }
    if len > 2 {
        // saturn
        bodies[2] = Body {
            8.34336671824457987e0
            4.12479856412430479e0
            -4.03523417114321381e-1
            -2.76742510726862411e-3 * days_per_year
            4.99852801234917238e-3 * days_per_year
            2.30417297573763929e-5 * days_per_year
            2.85885980666130812e-4 * solar_mass
        }
    }
    if len > 3 {
        // uranus
        bodies[3] = Body {
            1.28943695621391310e1
            -1.51111514016986312e1
            -2.23307578892655734e-1
            2.96460137564761618e-3 * days_per_year
            2.37847173959480950e-3 * days_per_year
            -2.96589568540237556e-5 * days_per_year
            4.36624404335156298e-5 * solar_mass
        }
    }
    if len > 4 {
        // neptune
        bodies[4] = Body {
            1.53796971148509165e1
            -2.59193146099879641e1
            1.79258772950371181e-1
            2.68067772490389322e-3 * days_per_year
            1.62824170038242295e-3 * days_per_year
            -9.51592254519715870e-5 * days_per_year
            5.15138902046611451e-5 * solar_mass
        }
    }
    offset_momentum(mut bodies, bodies.len)
    println('${energy(bodies, bodies.len):.9f}')
    for _ in 0 .. n {
        advance(mut bodies, bodies.len, 0.01)
    }
    println('${energy(bodies, bodies.len):.9f}')
}

fn offset_momentum(mut bodies []Body, length int) {
    mut px := 0.0
    mut py := 0.0
    mut pz := 0.0
    for i := 0; i < length; i++ {
        b := bodies[i]
        px += b.vx * b.mass
        py += b.vy * b.mass
        pz += b.vz * b.mass
    }
    bodies[0].vx = -px / solar_mass
    bodies[0].vy = -py / solar_mass
    bodies[0].vz = -pz / solar_mass
}

fn energy(bodies []Body, length int) f64 {
    mut e := 0.0
    for i := 0; i < length; i++ {
        b := bodies[i]
        sq := b.vx * b.vx + b.vy * b.vy + b.vz * b.vz
        e += 0.5 * b.mass * sq
        for j := i + 1; j < length; j++ {
            c := bodies[j]
            dx := b.x - c.x
            dy := b.y - c.y
            dz := b.z - c.z
            dsq := dx * dx + dy * dy + dz * dz
            e -= b.mass * c.mass / math.sqrt(dsq)
        }
    }
    return e
}

fn advance(mut bodies []Body, length int, dt f64) {
    for i := 0; i < length; i++ {
        for j := i + 1; j < length; j++ {
            dx := bodies[i].x - bodies[j].x
            dy := bodies[i].y - bodies[j].y
            dz := bodies[i].z - bodies[j].z
            dsq := dx * dx + dy * dy + dz * dz
            mag := dt / (dsq * math.sqrt(dsq))
            mj := bodies[j].mass * mag
            bodies[i].vx -= dx * mj
            bodies[i].vy -= dy * mj
            bodies[i].vz -= dz * mj
            mi := bodies[i].mass * mag
            bodies[j].vx += dx * mi
            bodies[j].vy += dy * mi
            bodies[j].vz += dz * mi
        }
    }
    for i := 0; i < length; i++ {
        bodies[i].x += bodies[i].vx * dt
        bodies[i].y += bodies[i].vy * dt
        bodies[i].z += bodies[i].vz * dt
    }
}