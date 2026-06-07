// The Computer Language Benchmarks Game
// https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

package org.bau.benchmarks;

public class NBody {

    static double PI = 3.141592653589793;
    static double SOLAR_MASS = 4 * PI * PI;
    static double DAYS_PER_YEAR = 365.24;

    public static void main(String[] args) {
        System.out.println("Java");
        int len = 5;
        int n = 50000000;
        if (args.length > 1) {
            len = Integer.parseInt(args[0]);
            n = Integer.parseInt(args[1]);
        }
        var bodies = new Body[len];
        if (len > 0) {
            // sun
            bodies[0] = new Body(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, SOLAR_MASS);
        }
        if (len > 1) {
            // jupiter
            bodies[1] = new Body(
                4.84143144246472090e0,
                -1.16032004402742839e0,
                -1.03622044471123109e-1,
                1.66007664274403694e-3 * DAYS_PER_YEAR,
                7.69901118419740425e-3 * DAYS_PER_YEAR,
                -6.90460016972063023e-5 * DAYS_PER_YEAR,
                9.54791938424326609e-4 * SOLAR_MASS);
        }
        if (len > 2) {
            // saturn
            bodies[2] = new Body(
                8.34336671824457987e0,
                4.12479856412430479e0,
                -4.03523417114321381e-1,
                -2.76742510726862411e-3 * DAYS_PER_YEAR,
                4.99852801234917238e-3 * DAYS_PER_YEAR,
                2.30417297573763929e-5 * DAYS_PER_YEAR,
                2.85885980666130812e-4 * SOLAR_MASS);
        }
        if (len > 3) {
            // uranus
            bodies[3] = new Body(
                1.28943695621391310e1,
                -1.51111514016986312e1,
                -2.23307578892655734e-1,
                2.96460137564761618e-3 * DAYS_PER_YEAR,
                2.37847173959480950e-3 * DAYS_PER_YEAR,
                -2.96589568540237556e-5 * DAYS_PER_YEAR,
                4.36624404335156298e-5 * SOLAR_MASS);
        }
        if (len > 4) {
            // neptune
            bodies[4] = new Body(
                1.53796971148509165e1,
                -2.59193146099879641e1,
                1.79258772950371181e-1,
                2.68067772490389322e-3 * DAYS_PER_YEAR,
                1.62824170038242295e-3 * DAYS_PER_YEAR,
                -9.51592254519715870e-5 * DAYS_PER_YEAR,
                5.15138902046611451e-5 * SOLAR_MASS);
        };
        offsetMomentum(bodies);
        System.out.printf("%.9f\n", energy(bodies));
        for (int i = 0; i < n; i++)
            advance(bodies, 0.01);
        System.out.printf("%.9f\n", energy(bodies));
    }

    static void offsetMomentum(Body[] bodies) {
        double px = 0.0, py = 0.0, pz = 0.0;
        for (int i = 0; i < bodies.length; i++) {
            Body b = bodies[i];
            px += b.vx * b.mass;
            py += b.vy * b.mass;
            pz += b.vz * b.mass;
        }
        bodies[0].vx = -px / SOLAR_MASS;
        bodies[0].vy = -py / SOLAR_MASS;
        bodies[0].vz = -pz / SOLAR_MASS;
    }

    static double energy(Body[] bodies) {
        double e = 0.0;
        for (int i = 0; i < bodies.length; i++) {
            Body b = bodies[i];
            double sq = b.vx * b.vx + b.vy * b.vy + b.vz * b.vz;
            e += 0.5 * b.mass * sq;
            for (int j = i + 1; j < bodies.length; j++) {
                Body c = bodies[j];
                double dx = b.x - c.x;
                double dy = b.y - c.y;
                double dz = b.z - c.z;
                double dsq = dx * dx + dy * dy + dz * dz;
                e -= b.mass * c.mass / Math.sqrt(dsq);
            }
        }
        return e;
    }

    static void advance(Body[] bodies, double dt) {
        for (int i = 0; i < bodies.length; i++) {
            for (int j = i + 1; j < bodies.length; j++) {
                double dx = bodies[i].x - bodies[j].x;
                double dy = bodies[i].y - bodies[j].y;
                double dz = bodies[i].z - bodies[j].z;
                double dsq = dx * dx + dy * dy + dz * dz;
                double mag = dt / (dsq * Math.sqrt(dsq));
                double mj = bodies[j].mass * mag;
                bodies[i].vx -= dx * mj;
                bodies[i].vy -= dy * mj;
                bodies[i].vz -= dz * mj;
                double mi = bodies[i].mass * mag;
                bodies[j].vx += dx * mi;
                bodies[j].vy += dy * mi;
                bodies[j].vz += dz * mi;
            }
        }
        for (int i = 0; i < bodies.length; i++) {
            bodies[i].x += bodies[i].vx * dt;
            bodies[i].y += bodies[i].vy * dt;
            bodies[i].z += bodies[i].vz * dt;
        }
    }

    static class Body {
        double x, y, z;
        double vx, vy, vz;
        double mass;

        Body(double x, double y, double z, double vx, double vy, double vz, double mass) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.vx = vx;
            this.vy = vy;
            this.vz = vz;
            this.mass = mass;
        }
    }

}