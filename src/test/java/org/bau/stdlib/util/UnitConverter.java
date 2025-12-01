package org.bau.stdlib.util;

public class UnitConverter {

    public static class Data {
        long bits;
        private static final String UNITS = " KMGT";

        private Data(long b) {
            this.bits = b;
        }

        public static Data ofBit(long b) {
            return new Data(b);
        }

        public static Data ofByte(long b) {
            return new Data(b * 8);
        }

        public static Data ofKilobyte(long kb) {
            return new Data(kb * 8 * 1_000);
        }

        public static Data ofMegabyte(long mb) {
            return new Data(mb * 8 * 1_000_000);
        }

        public static Data ofGigabyte(long gb) {
            return new Data(gb * 8 * 1_000_000_000);
        }

        public static Data ofTerabyte(long tb) {
            return new Data(tb * 8 * 1_000_000_000_000L);
        }

        public static Data ofKibibyte(long kib) {
            return new Data(kib * 8 << 10);
        }

        public static Data ofMebibyte(long mib) {
            return new Data(mib * 8 << 20);
        }

        public static Data ofGibibyte(long gib) {
            return new Data(gib * 8 << 30);
        }

        public static Data ofTebibyte(long tib) {
            return new Data(tib * 8 << 40);
        }

        public long toBit() {
            return bits;
        }

        public long toByte() {
            return bits >> 3;
        }

        public long toKilobyte() {
            return (bits >> 3) / 1000;
        }

        public long toMegabyte() {
            return (bits >> 3) / 1_000_000;
        }

        public long toGigabyte() {
            return (bits >> 3) / 1_000_000_000;
        }

        public long toTerabyte() {
            return (bits >> 3) / 1_000_000_000_000L;
        }

        public long toKibibyte() {
            return bits >> 3 >> 10;
        }

        public long toMebibyte() {
            return bits >> 3 >> 20;
        }

        public long toGibibyte() {
            return bits >> 3 >> 30;
        }

        public long toTebibyte() {
            return bits >> 3 >> 40;
        }

        public String toHumanReadable(int scale, String suffix) {
            long x = bits >> 3;
            if (x < scale) {
                return x + " B";
            }
            int i = 0;
            while (x >= scale && i < UNITS.length() - 1) {
                x = (x + scale - 1) / scale;
                i++;
            }
            return x + " " + UNITS.charAt(i) + suffix + "B";

        }

        public String toHumanReadable() {
            return toHumanReadable(1000, "");
        }

        public String toHumanReadableBinary() {
            return toHumanReadable(1024, "i");
        }

    }

    public static class Angle {
        double radians;

        private Angle(double radians) {
            this.radians = radians;
        }

        public static Angle ofRadians(double radians) {
            return new Angle(radians);
        }

        public static Angle ofDegree(double degree) {
            return new Angle(degree * Math.PI / 180.);
        }

        public double toRadians() {
            return radians;
        }

        public double toDegree() {
            return radians * 180. / Math.PI;
        }
    }

    public static class Length {
        private final long mm;

        private Length(long mm) {
            this.mm = mm;
        }

        public static Length ofMillimeter(long mm) {
            return new Length(mm);
        }

        public static Length ofCentimeter(long cm) {
            return new Length(10 * cm);
        }

        public static Length ofDecimeter(long dm) {
            return new Length(100 * dm);
        }

        public static Length ofMeter(long m) {
            return new Length(1_000 * m);
        }

        public static Length ofKilometer(long km) {
            return new Length(1_000_000 * km);
        }

        public long toMillimeter() {
            return mm;
        }

        public long toCentimeter() {
            return mm / 10;
        }

        public long toDecimeter() {
            return mm / 100;
        }

        public long toMeter() {
            return mm / 1_000;
        }

        public long toKilometer() {
            return mm / 1_000_000;
        }

    }

    public static class Temperature {
        private final double celsius;

        private Temperature(double celsius) {
            this.celsius = celsius;
        }

        public static Temperature ofCelsius(double celsius) {
            return new Temperature(celsius);
        }

        public static Temperature ofKelvin(double kelvin) {
            return new Temperature(kelvin - 273.15);
        }

        public static Temperature ofFahrenheit(double fahrenheit) {
            return new Temperature((fahrenheit - 32) * 5.0 / 9.0);
        }

        public double toCelsius() {
            return celsius;
        }

        public double toFahrenheit() {
            return celsius * 9.0 / 5.0 + 32;
        }

        public double toKelvin() {
            return celsius + 273.15;
        }
    }

    public static class Weight {

        private final long mg;

        private Weight(long mg) {
            this.mg = mg;
        }

        static Weight ofMilligram(long mg) {
            return new Weight(mg);
        }

        static Weight ofGram(long g) {
            return new Weight(g * 1_000);
        }

        static Weight ofKilogram(long kg) {
            return new Weight(kg * 1_000_000);
        }

        static Weight ofTon(long t) {
            return new Weight(t * 1_000_000_000);
        }

        public long toMilligram() {
            return mg;
        }

        public long toGram() {
            return mg / 1_000;
        }

        public long toKilogram() {
            return mg / 1_000_000;
        }

        public long toTon() {
            return mg / 1_000_000_000;
        }

    }

}
