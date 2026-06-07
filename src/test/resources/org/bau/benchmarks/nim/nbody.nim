# The Computer Language Benchmarks Game
# https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

import os, math, strutils, std/strformat

type
    Body = object
        x, y, z: float64
        vx, vy, vz: float64
        mass: float64

const
    PI = 3.141592653589793
    SOLAR_MASS = 4.0 * PI * PI
    DAYS_PER_YEAR = 365.24

proc offsetMomentum(bodies: var openArray[Body]) =
    var px = 0.0
    var py = 0.0
    var pz = 0.0
    for i in 0 ..< bodies.len:
        let b = addr bodies[i]
        px += b.vx * b.mass
        py += b.vy * b.mass
        pz += b.vz * b.mass
    bodies[0].vx = -px / SOLAR_MASS
    bodies[0].vy = -py / SOLAR_MASS
    bodies[0].vz = -pz / SOLAR_MASS

proc energy(bodies: openArray[Body]): float64 =
    var e = 0.0
    for i in 0 ..< bodies.len:
        let b = addr bodies[i]
        let sq = b.vx * b.vx + b.vy * b.vy + b.vz * b.vz
        e += 0.5 * b.mass * sq
        for j in (i + 1) ..< bodies.len:
            let c = addr bodies[j]
            let dx = b.x - c.x
            let dy = b.y - c.y
            let dz = b.z - c.z
            let dsq = dx * dx + dy * dy + dz * dz
            e -= b.mass * c.mass / sqrt(dsq)
    result = e

proc advance(bodies: var openArray[Body], dt: float64) =
    for i in 0 ..< bodies.len:
        for j in (i + 1) ..< bodies.len:
            let dx = bodies[i].x - bodies[j].x
            let dy = bodies[i].y - bodies[j].y
            let dz = bodies[i].z - bodies[j].z
            let dsq = dx * dx + dy * dy + dz * dz
            let mag = dt / (dsq * sqrt(dsq))
            let mj = bodies[j].mass * mag
            bodies[i].vx -= dx * mj
            bodies[i].vy -= dy * mj
            bodies[i].vz -= dz * mj
            let mi = bodies[i].mass * mag
            bodies[j].vx += dx * mi
            bodies[j].vy += dy * mi
            bodies[j].vz += dz * mi
    for i in 0 ..< bodies.len:
        bodies[i].x += bodies[i].vx * dt
        bodies[i].y += bodies[i].vy * dt
        bodies[i].z += bodies[i].vz * dt

proc main() =
    echo &"Nim"
    let args = commandLineParams()
    var len = 5
    var n = 50_000_000
    if args.len > 1:
        len = parseInt(args[0])
        n = parseInt(args[1])
    var bodies = newSeq[Body](len)
    if len > 0:
        # sun
        bodies[0] = Body(x: 0, y: 0, z: 0, vx: 0, vy: 0, vz: 0, mass: SOLAR_MASS)
    if len > 1:
        # jupiter
        bodies[1] = Body(
            x: 4.84143144246472090e0,
            y: -1.16032004402742839e0,
            z: -1.03622044471123109e-1,
            vx: 1.66007664274403694e-3 * DAYS_PER_YEAR,
            vy: 7.69901118419740425e-3 * DAYS_PER_YEAR,
            vz: -6.90460016972063023e-5 * DAYS_PER_YEAR,
            mass: 9.54791938424326609e-4 * SOLAR_MASS)
    if len > 2:
        # saturn
        bodies[2] = Body(
            x: 8.34336671824457987e0,
            y: 4.12479856412430479e0,
            z: -4.03523417114321381e-1,
            vx: -2.76742510726862411e-3 * DAYS_PER_YEAR,
            vy: 4.99852801234917238e-3 * DAYS_PER_YEAR,
            vz: 2.30417297573763929e-5 * DAYS_PER_YEAR,
            mass: 2.85885980666130812e-4 * SOLAR_MASS)
    if len > 3:
        # uranus
        bodies[3] = Body(
            x: 1.28943695621391310e1,
            y: -1.51111514016986312e1,
            z: -2.23307578892655734e-1,
            vx: 2.96460137564761618e-3 * DAYS_PER_YEAR,
            vy: 2.37847173959480950e-3 * DAYS_PER_YEAR,
            vz: -2.96589568540237556e-5 * DAYS_PER_YEAR,
            mass: 4.36624404335156298e-5 * SOLAR_MASS)
    if len > 4:
        # neptune
        bodies[4] = Body(
            x: 1.53796971148509165e1,
            y: -2.59193146099879641e1,
            z: 1.79258772950371181e-1,
            vx: 2.68067772490389322e-3 * DAYS_PER_YEAR,
            vy: 1.62824170038242295e-3 * DAYS_PER_YEAR,
            vz: -9.51592254519715870e-5 * DAYS_PER_YEAR,
            mass: 5.15138902046611451e-5 * SOLAR_MASS)
    offsetMomentum(bodies)
    echo formatFloat(energy(bodies), ffDecimal, 9)
    for i in 0 ..< n:
      advance(bodies, 0.01)
    echo formatFloat(energy(bodies), ffDecimal, 9)

when isMainModule:
    main()
