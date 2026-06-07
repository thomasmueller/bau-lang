# The Computer Language Benchmarks Game
# https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

import math
import sys

PI = 3.141592653589793
SOLAR_MASS = 4.0 * PI * PI
DAYS_PER_YEAR = 365.24

class Body:
    def __init__(self, x, y, z, vx, vy, vz, mass):
        self.x = x
        self.y = y
        self.z = z
        self.vx = vx
        self.vy = vy
        self.vz = vz
        self.mass = mass

def offset_momentum(bodies, length):
    px, py, pz = 0.0, 0.0, 0.0
    for i in range(length):
        b = bodies[i]
        px += b.vx * b.mass
        py += b.vy * b.mass
        pz += b.vz * b.mass
    bodies[0].vx = -px / SOLAR_MASS
    bodies[0].vy = -py / SOLAR_MASS
    bodies[0].vz = -pz / SOLAR_MASS

def energy(bodies, length):
    e = 0.0
    for i in range(length):
        b = bodies[i]
        sq = b.vx * b.vx + b.vy * b.vy + b.vz * b.vz
        e += 0.5 * b.mass * sq
        for j in range(i + 1, length):
            c = bodies[j]
            dx = b.x - c.x
            dy = b.y - c.y
            dz = b.z - c.z
            dsq = dx * dx + dy * dy + dz * dz
            e -= b.mass * c.mass / math.sqrt(dsq)
    return e

def advance(bodies, length, dt):
    for i in range(length):
        b = bodies[i]
        for j in range(i + 1, length):
            c = bodies[j]
            dx = bodies[i].x - c.x
            dy = bodies[i].y - c.y
            dz = bodies[i].z - c.z
            dsq = dx * dx + dy * dy + dz * dz
            mag = dt / (dsq * math.sqrt(dsq))
            mj = c.mass * mag
            bodies[i].vx -= dx * mj
            bodies[i].vy -= dy * mj
            bodies[i].vz -= dz * mj
            mi = bodies[i].mass * mag
            c.vx += dx * mi
            c.vy += dy * mi
            c.vz += dz * mi
    for i in range(length):
        b = bodies[i]
        bodies[i].x += bodies[i].vx * dt
        bodies[i].y += bodies[i].vy * dt
        bodies[i].z += bodies[i].vz * dt

def main():
    print("Python")
    cnt = 5
    n = 50_000_000
    if len(sys.argv) > 2:
        cnt = int(sys.argv[1])
        n = int(sys.argv[2])
    bodies = []
    if cnt > 0:
        # sun
        bodies.append(Body(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, SOLAR_MASS))
    if cnt > 1:
        # jupiter
        bodies.append(Body(
            4.84143144246472090e0,
            -1.16032004402742839e0,
            -1.03622044471123109e-1,
            1.66007664274403694e-3 * DAYS_PER_YEAR,
            7.69901118419740425e-3 * DAYS_PER_YEAR,
            -6.90460016972063023e-5 * DAYS_PER_YEAR,
            9.54791938424326609e-4 * SOLAR_MASS))
    if cnt > 2:
        # saturn
        bodies.append(Body(
            8.34336671824457987e0,
            4.12479856412430479e0,
            -4.03523417114321381e-1,
            -2.76742510726862411e-3 * DAYS_PER_YEAR,
            4.99852801234917238e-3 * DAYS_PER_YEAR,
            2.30417297573763929e-5 * DAYS_PER_YEAR,
            2.85885980666130812e-4 * SOLAR_MASS))
    if cnt > 3:
        # uranus
        bodies.append(Body(
            1.28943695621391310e1,
            -1.51111514016986312e1,
            -2.23307578892655734e-1,
            2.96460137564761618e-3 * DAYS_PER_YEAR,
            2.37847173959480950e-3 * DAYS_PER_YEAR,
            -2.96589568540237556e-5 * DAYS_PER_YEAR,
            4.36624404335156298e-5 * SOLAR_MASS))
    if cnt > 4:
        # neptune
        bodies.append(Body(
            1.53796971148509165e1,
            -2.59193146099879641e1,
            1.79258772950371181e-1,
            2.68067772490389322e-3 * DAYS_PER_YEAR,
            1.62824170038242295e-3 * DAYS_PER_YEAR,
            -9.51592254519715870e-5 * DAYS_PER_YEAR,
            5.15138902046611451e-5 * SOLAR_MASS))
    offset_momentum(bodies, cnt)
    print(f"{energy(bodies, cnt):.9f}")
    for _ in range(n):
        advance(bodies, cnt, 0.01)
    print(f"{energy(bodies, cnt):.9f}")

main()