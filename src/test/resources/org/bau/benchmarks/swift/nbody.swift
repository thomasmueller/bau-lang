// The Computer Language Benchmarks Game
// https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

import Foundation

let PI = 3.141592653589793
let SOLAR_MASS = 4.0 * PI * PI
let DAYS_PER_YEAR = 365.24

struct Body {
    var x, y, z: Double
    var vx, vy, vz: Double
    var mass: Double
}

func offsetMomentum(_ bodies: inout [Body], _ length: Int) {
    var px = 0.0
    var py = 0.0
    var pz = 0.0
    for i in 0..<length {
        let b = bodies[i]
        px += b.vx * b.mass
        py += b.vy * b.mass
        pz += b.vz * b.mass
    }
    bodies[0].vx = -px / SOLAR_MASS
    bodies[0].vy = -py / SOLAR_MASS
    bodies[0].vz = -pz / SOLAR_MASS
}

func energy(_ bodies: [Body], _ length: Int) -> Double {
    var e = 0.0
    for i in 0..<length {
        let b = bodies[i]
        let sq = b.vx * b.vx +
                 b.vy * b.vy +
                 b.vz * b.vz
        e += 0.5 * b.mass * sq
        for j in (i + 1)..<length {
            let c = bodies[j]
            let dx = b.x - c.x
            let dy = b.y - c.y
            let dz = b.z - c.z
            let dsq = dx * dx + dy * dy + dz * dz
            e -= b.mass * c.mass / sqrt(dsq)
        }
    }
    return e
}

func advance(_ bodies: inout [Body], _ length: Int, _ dt: Double) {
    for i in 0..<length {
        for j in (i + 1)..<length {
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
        }
    }
    for i in 0..<length {
        bodies[i].x += bodies[i].vx * dt
        bodies[i].y += bodies[i].vy * dt
        bodies[i].z += bodies[i].vz * dt
    }
}

func main() {
    print("Swift")
    var len = 5
    var n = 50_000_000
    if CommandLine.arguments.count > 2 {
        len = Int(CommandLine.arguments[1]) ?? len
        n = Int(CommandLine.arguments[2]) ?? n
    }
    var bodies: [Body] = []
    bodies.reserveCapacity(len)
    if len > 0 {
        // sun
        bodies.append(Body(x: 0, y: 0, z: 0, vx: 0, vy: 0, vz: 0, mass: SOLAR_MASS))
    }
    if len > 1 {
        // jupiter
        bodies.append(Body(
            x: 4.84143144246472090e0,
            y: -1.16032004402742839e0,
            z: -1.03622044471123109e-1,
            vx: 1.66007664274403694e-3 * DAYS_PER_YEAR,
            vy: 7.69901118419740425e-3 * DAYS_PER_YEAR,
            vz: -6.90460016972063023e-5 * DAYS_PER_YEAR,
            mass: 9.54791938424326609e-4 * SOLAR_MASS
        ))
    }
    if len > 2 {
        // saturn
        bodies.append(Body(
            x: 8.34336671824457987e0,
            y: 4.12479856412430479e0,
            z: -4.03523417114321381e-1,
            vx: -2.76742510726862411e-3 * DAYS_PER_YEAR,
            vy: 4.99852801234917238e-3 * DAYS_PER_YEAR,
            vz: 2.30417297573763929e-5 * DAYS_PER_YEAR,
            mass: 2.85885980666130812e-4 * SOLAR_MASS
        ))
    }
    if len > 3 {
        // uranus
        bodies.append(Body(
            x: 1.28943695621391310e1,
            y: -1.51111514016986312e1,
            z: -2.23307578892655734e-1,
            vx: 2.96460137564761618e-3 * DAYS_PER_YEAR,
            vy: 2.37847173959480950e-3 * DAYS_PER_YEAR,
            vz: -2.96589568540237556e-5 * DAYS_PER_YEAR,
            mass: 4.36624404335156298e-5 * SOLAR_MASS
        ))
    }
    if len > 4 {
        // neptune
        bodies.append(Body(
            x: 1.53796971148509165e1,
            y: -2.59193146099879641e1,
            z: 1.79258772950371181e-1,
            vx: 2.68067772490389322e-3 * DAYS_PER_YEAR,
            vy: 1.62824170038242295e-3 * DAYS_PER_YEAR,
            vz: -9.51592254519715870e-5 * DAYS_PER_YEAR,
            mass: 5.15138902046611451e-5 * SOLAR_MASS
        ))
    }
    offsetMomentum(&bodies, bodies.count)
    print(String(format: "%.9f", energy(bodies, bodies.count)))
    for _ in 0..<n {
        advance(&bodies, bodies.count, 0.01)
    }
    print(String(format: "%.9f", energy(bodies, bodies.count)))
}

main()