// The Computer Language Benchmarks Game
// https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

use std::env;

fn main() {
    println!("Rust");
    let mut len: usize = 5;
    let mut n: usize = 50_000_000;
    let args: Vec<String> = env::args().collect();
    if args.len() > 2 {
        len = args[1].parse().unwrap();
        n = args[2].parse().unwrap();
    }
    let mut bodies: Vec<Body> = Vec::with_capacity(len);
    if len > 0 {
        // sun
        bodies.push(Body { x: 0.0, y: 0.0, z: 0.0, vx: 0.0, vy: 0.0, vz: 0.0, mass: SOLAR_MASS });
    }        
    if len > 1 {
        // jupiter
        bodies.push(Body {
            x: 4.84143144246472090e0,
            y: -1.16032004402742839e0,
            z: -1.03622044471123109e-1,
            vx: 1.66007664274403694e-3 * DAYS_PER_YEAR,
            vy: 7.69901118419740425e-3 * DAYS_PER_YEAR,
            vz: -6.90460016972063023e-5 * DAYS_PER_YEAR,
            mass: 9.54791938424326609e-4 * SOLAR_MASS
        });
    }
    if len > 2 {
        // saturn
        bodies.push(Body {
            x: 8.34336671824457987e0,
            y: 4.12479856412430479e0,
            z: -4.03523417114321381e-1,
            vx: -2.76742510726862411e-3 * DAYS_PER_YEAR,
            vy: 4.99852801234917238e-3 * DAYS_PER_YEAR,
            vz: 2.30417297573763929e-5 * DAYS_PER_YEAR,
            mass: 2.85885980666130812e-4 * SOLAR_MASS
        });
    }
    if len > 3 {
        // uranus
        bodies.push(Body {
            x: 1.28943695621391310e1,
            y: -1.51111514016986312e1,
            z: -2.23307578892655734e-1,
            vx: 2.96460137564761618e-3 * DAYS_PER_YEAR,
            vy: 2.37847173959480950e-3 * DAYS_PER_YEAR,
            vz: -2.96589568540237556e-5 * DAYS_PER_YEAR,
            mass: 4.36624404335156298e-5 * SOLAR_MASS
        });
    }
    if len > 4 {
        // neptune
        bodies.push(Body {
            x: 1.53796971148509165e1,
            y: -2.59193146099879641e1,
            z: 1.79258772950371181e-1,
            vx: 2.68067772490389322e-3 * DAYS_PER_YEAR,
            vy: 1.62824170038242295e-3 * DAYS_PER_YEAR,
            vz: -9.51592254519715870e-5 * DAYS_PER_YEAR,
            mass: 5.15138902046611451e-5 * SOLAR_MASS
        });
    }
    offset_momentum(&mut bodies, len);
    println!("{:.9}", energy(&bodies, len));
    for _ in 0..n {
        advance(&mut bodies, len, 0.01);
    }
    println!("{:.9}", energy(&bodies, len));
}

const PI: f64 = 3.141592653589793;
const SOLAR_MASS: f64 = 4.0 * PI * PI;
const DAYS_PER_YEAR: f64 = 365.24;

#[derive(Clone, Copy)]
struct Body {
    x: f64,
    y: f64,
    z: f64,
    vx: f64,
    vy: f64,
    vz: f64,
    mass: f64,
}

fn offset_momentum(bodies: &mut [Body], len: usize) {
    let mut px = 0.0;
    let mut py = 0.0;
    let mut pz = 0.0;
    for i in 0..len {
        let b = &bodies[i];
        px += b.vx * b.mass;
        py += b.vy * b.mass;
        pz += b.vz * b.mass;
    }
    bodies[0].vx = -px / SOLAR_MASS;
    bodies[0].vy = -py / SOLAR_MASS;
    bodies[0].vz = -pz / SOLAR_MASS;
}

fn energy(bodies: &[Body], len: usize) -> f64 {
    let mut e = 0.0;
    for i in 0..len {
        let b = &bodies[i];
        let sq = b.vx * b.vx + b.vy * b.vy + b.vz * b.vz;
        e += 0.5 * b.mass * sq;
        for j in (i + 1)..len {
            let c = &bodies[j];
            let dx = b.x - c.x;
            let dy = b.y - c.y;
            let dz = b.z - c.z;
            let dsq = dx * dx + dy * dy + dz * dz;
            e -= b.mass * c.mass / dsq.sqrt();
        }
    }
    e
}

fn advance(bodies: &mut [Body], len: usize, dt: f64) {
    for i in 0..len {
        for j in (i + 1)..len {
            let dx = bodies[i].x - bodies[j].x;
            let dy = bodies[i].y - bodies[j].y;
            let dz = bodies[i].z - bodies[j].z;
            let dsq = dx * dx + dy * dy + dz * dz;
            let mag = dt / (dsq * dsq.sqrt());
            let mj = bodies[j].mass * mag;
            bodies[i].vx -= dx * mj;
            bodies[i].vy -= dy * mj;
            bodies[i].vz -= dz * mj;
            let mi = bodies[i].mass * mag;
            bodies[j].vx += dx * mi;
            bodies[j].vy += dy * mi;
            bodies[j].vz += dz * mi;
        }
    }
    for i in 0..len {
        bodies[i].x += bodies[i].vx * dt;
        bodies[i].y += bodies[i].vy * dt;
        bodies[i].z += bodies[i].vz * dt;
    }
}
