// The Computer Language Benchmarks Game
// https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

package main

import (
    "fmt"
    "math"
    "os"
    "strconv"
)

func main() {
    fmt.Println("Go")
    n := 50000000
    cnt := 5
    if len(os.Args) > 2 {
        cnt, _ = strconv.Atoi(os.Args[1])
        n, _ = strconv.Atoi(os.Args[2])
    }
    bodies := make([]Body, cnt)
    if cnt > 0 {
        // sun
        bodies[0] = Body { 0, 0, 0, 0, 0, 0, SOLAR_MASS }
    }    
    if cnt > 1 {
        // jupiter
        bodies[1] = Body {        
            4.84143144246472090e0,
            -1.16032004402742839e0,
            -1.03622044471123109e-1,
            1.66007664274403694e-3 * DAYS_PER_YEAR,
            7.69901118419740425e-3 * DAYS_PER_YEAR,
            -6.90460016972063023e-5 * DAYS_PER_YEAR,
            9.54791938424326609e-4 * SOLAR_MASS,
        }
    }
    if cnt > 2 {
        // saturn
        bodies[2] = Body {        
            8.34336671824457987e0,
            4.12479856412430479e0,
            -4.03523417114321381e-1,
            -2.76742510726862411e-3 * DAYS_PER_YEAR,
            4.99852801234917238e-3 * DAYS_PER_YEAR,
            2.30417297573763929e-5 * DAYS_PER_YEAR,
            2.85885980666130812e-4 * SOLAR_MASS,
        }
    }
    if cnt > 3 {
        // uranus
        bodies[3] = Body {        
            1.28943695621391310e1,
            -1.51111514016986312e1,
            -2.23307578892655734e-1,
            2.96460137564761618e-3 * DAYS_PER_YEAR,
            2.37847173959480950e-3 * DAYS_PER_YEAR,
            -2.96589568540237556e-5 * DAYS_PER_YEAR,
            4.36624404335156298e-5 * SOLAR_MASS,
        }
    }
    if cnt > 4 {
        // neptune
        bodies[4] = Body {        
            1.53796971148509165e1,
            -2.59193146099879641e1,
            1.79258772950371181e-1,
            2.68067772490389322e-3 * DAYS_PER_YEAR,
            1.62824170038242295e-3 * DAYS_PER_YEAR,
            -9.51592254519715870e-5 * DAYS_PER_YEAR,
            5.15138902046611451e-5 * SOLAR_MASS,
        }
    }
    offsetMomentum(bodies, len(bodies))
    fmt.Printf("%.9f\n", energy(bodies, len(bodies)))
    for i := 0; i < n; i++ {
        advance(bodies, len(bodies), 0.01)
    }
    fmt.Printf("%.9f\n", energy(bodies, len(bodies)))
}

const PI = 3.141592653589793
const SOLAR_MASS = 4.0 * PI * PI
const DAYS_PER_YEAR = 365.24

type Body struct {
    x, y, z float64
    vx, vy, vz float64
    mass float64
}

func offsetMomentum(bodies []Body, length int) {
    px, py, pz := 0.0, 0.0, 0.0
    for i := 0; i < length; i++ {
        b := &bodies[i]
        px += b.vx * b.mass
        py += b.vy * b.mass
        pz += b.vz * b.mass
    }
    bodies[0].vx = -px / SOLAR_MASS
    bodies[0].vy = -py / SOLAR_MASS
    bodies[0].vz = -pz / SOLAR_MASS
}

func energy(bodies []Body, length int) float64 {
    e := 0.0
    for i := 0; i < length; i++ {
        b := &bodies[i]
        sq := b.vx*b.vx + b.vy*b.vy + b.vz*b.vz
        e += 0.5 * b.mass * sq
        for j := i + 1; j < length; j++ {
            c := &bodies[j]
            dx := b.x - c.x
            dy := b.y - c.y
            dz := b.z - c.z
            dsq := dx * dx + dy * dy + dz * dz
            e -= b.mass * c.mass / math.Sqrt(dsq)
        }
    }
    return e
}

func advance(bodies []Body, length int, dt float64) {
    for i := 0; i < length; i++ {
        for j := i + 1; j < length; j++ {
            dx := bodies[i].x - bodies[j].x
            dy := bodies[i].y - bodies[j].y
            dz := bodies[i].z - bodies[j].z
            dsq := dx * dx + dy * dy + dz * dz
            mag := dt / (dsq * math.Sqrt(dsq))
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
