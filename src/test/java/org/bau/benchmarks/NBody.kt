// The Computer Language Benchmarks Game
// https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

package org.bau.benchmarks

import kotlin.math.sqrt

object NBody {

    const val PI = 3.141592653589793
    const val SOLAR_MASS = 4.0 * PI * PI
    const val DAYS_PER_YEAR = 365.24

    @JvmStatic
    fun main(args: Array<String>) {
        println("Kotlin")
        var len = 5
        var n = 50_000_000
        if (args.size > 1) {
            len = args[0].toInt()
            n = args[1].toInt()
        }
        val bodies = arrayOfNulls<Body>(len)
        if (len > 0) {
            // sun
            bodies[0] = Body(
                0.0, 0.0, 0.0,
                0.0, 0.0, 0.0,
                SOLAR_MASS
            )
        }
        if (len > 1) {
            // jupiter
            bodies[1] = Body(
                4.84143144246472090e0,
                -1.16032004402742839e0,
                -1.03622044471123109e-1,
                1.66007664274403694e-3 * DAYS_PER_YEAR,
                7.69901118419740425e-3 * DAYS_PER_YEAR,
                -6.90460016972063023e-5 * DAYS_PER_YEAR,
                9.54791938424326609e-4 * SOLAR_MASS
            )
        }
        if (len > 2) {
            // saturn
            bodies[2] = Body(
                8.34336671824457987e0,
                4.12479856412430479e0,
                -4.03523417114321381e-1,
                -2.76742510726862411e-3 * DAYS_PER_YEAR,
                4.99852801234917238e-3 * DAYS_PER_YEAR,
                2.30417297573763929e-5 * DAYS_PER_YEAR,
                2.85885980666130812e-4 * SOLAR_MASS
            )
        }
        if (len > 3) {
            // uranus
            bodies[3] = Body(
                1.28943695621391310e1,
                -1.51111514016986312e1,
                -2.23307578892655734e-1,
                2.96460137564761618e-3 * DAYS_PER_YEAR,
                2.37847173959480950e-3 * DAYS_PER_YEAR,
                -2.96589568540237556e-5 * DAYS_PER_YEAR,
                4.36624404335156298e-5 * SOLAR_MASS
            )
        }
        if (len > 4) {
            // neptune
            bodies[4] = Body(
                1.53796971148509165e1,
                -2.59193146099879641e1,
                1.79258772950371181e-1,
                2.68067772490389322e-3 * DAYS_PER_YEAR,
                1.62824170038242295e-3 * DAYS_PER_YEAR,
                -9.51592254519715870e-5 * DAYS_PER_YEAR,
                5.15138902046611451e-5 * SOLAR_MASS
            )
        }
        offsetMomentum(bodies)
        System.out.printf("%.9f%n", energy(bodies))
        for (i in 0 until n) {
            advance(bodies, 0.01)
        }
        System.out.printf("%.9f%n", energy(bodies))
    }

    fun offsetMomentum(bodies: Array<Body?>) {
        var px = 0.0
        var py = 0.0
        var pz = 0.0
        for (i in bodies.indices) {
            val b = bodies[i]!!
            px += b.vx * b.mass
            py += b.vy * b.mass
            pz += b.vz * b.mass
        }
        bodies[0]!!.vx = -px / SOLAR_MASS
        bodies[0]!!.vy = -py / SOLAR_MASS
        bodies[0]!!.vz = -pz / SOLAR_MASS
    }

    fun energy(bodies: Array<Body?>): Double {
        var e = 0.0
        for (i in bodies.indices) {
            val b = bodies[i]!!
            val sq = b.vx * b.vx + b.vy * b.vy + b.vz * b.vz
            e += 0.5 * b.mass * sq
            for (j in i + 1 until bodies.size) {
                val c = bodies[j]!!
                val dx = b.x - c.x
                val dy = b.y - c.y
                val dz = b.z - c.z
                val dsq = dx * dx + dy * dy + dz * dz
                e -= b.mass * c.mass / sqrt(dsq)
            }
        }
        return e
    }

    fun advance(bodies: Array<Body?>, dt: Double) {
        for (i in bodies.indices) {
            for (j in i + 1 until bodies.size) {
                val dx = bodies[i]!!.x - bodies[j]!!.x
                val dy = bodies[i]!!.y - bodies[j]!!.y
                val dz = bodies[i]!!.z - bodies[j]!!.z
                val dsq = dx * dx + dy * dy + dz * dz
                val mag = dt / (dsq * sqrt(dsq))
                val mj = bodies[j]!!.mass * mag
                bodies[i]!!.vx -= dx * mj
                bodies[i]!!.vy -= dy * mj
                bodies[i]!!.vz -= dz * mj
                val mi = bodies[i]!!.mass * mag
                bodies[j]!!.vx += dx * mi
                bodies[j]!!.vy += dy * mi
                bodies[j]!!.vz += dz * mi
            }
        }
        for (i in bodies.indices) {
            bodies[i]!!.x += bodies[i]!!.vx * dt
            bodies[i]!!.y += bodies[i]!!.vy * dt
            bodies[i]!!.z += bodies[i]!!.vz * dt
        }
    }

    class Body(
        var x: Double,
        var y: Double,
        var z: Double,
        var vx: Double,
        var vy: Double,
        var vz: Double,
        var mass: Double
    )
}