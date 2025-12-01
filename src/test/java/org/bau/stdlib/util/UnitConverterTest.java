package org.bau.stdlib.util;

import static org.junit.Assert.assertEquals;

import org.bau.stdlib.util.UnitConverter.Angle;
import org.bau.stdlib.util.UnitConverter.Data;
import org.bau.stdlib.util.UnitConverter.Length;
import org.bau.stdlib.util.UnitConverter.Temperature;
import org.bau.stdlib.util.UnitConverter.Weight;
import org.junit.Test;

public class UnitConverterTest {

    @Test
    public void data() {
        assertEquals(1L, Data.ofBit(1).toBit());
        assertEquals(8L, Data.ofByte(1).toBit());
        assertEquals(8 * 1000L, Data.ofKilobyte(1).toBit());
        assertEquals(1000, Data.ofMegabyte(1).toKilobyte());
        assertEquals(1000, Data.ofGigabyte(1).toMegabyte());
        assertEquals(1000, Data.ofTerabyte(1).toGigabyte());
        assertEquals(2, Data.ofTerabyte(2).toTerabyte());
        assertEquals(1024, Data.ofKibibyte(1).toByte());
        assertEquals(1024, Data.ofMebibyte(1).toKibibyte());
        assertEquals(1024, Data.ofGibibyte(1).toMebibyte());
        assertEquals(1024, Data.ofTebibyte(1).toGibibyte());
        assertEquals(2, Data.ofTebibyte(2).toTebibyte());

        assertEquals("0 B", Data.ofByte(0).toHumanReadable());
        assertEquals("0 B", Data.ofByte(0).toHumanReadableBinary());
        assertEquals("1 B", Data.ofByte(1).toHumanReadable());
        assertEquals("1 B", Data.ofByte(1).toHumanReadableBinary());
        assertEquals("1 KB", Data.ofByte(1000).toHumanReadable());
        assertEquals("1000 B", Data.ofByte(1000).toHumanReadableBinary());
        assertEquals("2 KB", Data.ofByte(1024).toHumanReadable());
        assertEquals("1 KiB", Data.ofByte(1024).toHumanReadableBinary());
        assertEquals("1 MB", Data.ofByte(1_000_000).toHumanReadable());
        assertEquals("1 MiB", Data.ofByte(1L << 20).toHumanReadableBinary());
        assertEquals("1 GB", Data.ofByte(1_000_000_000).toHumanReadable());
        assertEquals("1 GiB", Data.ofByte(1L << 30).toHumanReadableBinary());
        assertEquals("1 TB", Data.ofByte(1_000_000_000_000L).toHumanReadable());
        assertEquals("1 TiB", Data.ofByte(1L << 40).toHumanReadableBinary());
        assertEquals("1000 TB", Data.ofByte(1_000_000_000_000_000L).toHumanReadable());
        assertEquals("1024 TiB", Data.ofByte(1L << 50).toHumanReadableBinary());
        assertEquals("-1000000 B", Data.ofByte(-1_000_000).toHumanReadable());
        assertEquals("-1000000 B", Data.ofByte(-1_000_000).toHumanReadableBinary());
    }

    @Test
    public void angle() {
        assertEquals(0, (int) (Angle.ofDegree(0.0).toRadians() * 1000));
        assertEquals(174, (int) (Angle.ofDegree(10.0).toRadians() * 1000));
        assertEquals(180, (int) (Angle.ofRadians(3.1416).toDegree()));
    }

    @Test
    public void length() {
        assertEquals(1, Length.ofMillimeter(10).toCentimeter());
        assertEquals(2, Length.ofCentimeter(20).toDecimeter());
        assertEquals(3, Length.ofDecimeter(30).toMeter());
        assertEquals(4, Length.ofMeter(4000).toKilometer());
        assertEquals(5_000_000, Length.ofKilometer(5).toMillimeter());
    }

    @Test
    public void temperature() {
        assertEquals(-1777, (int) (Temperature.ofFahrenheit(0).toCelsius() * 100));
        assertEquals(3777, (int) (Temperature.ofFahrenheit(100).toCelsius() * 100));
        assertEquals(3200, (int) (Temperature.ofCelsius(0).toFahrenheit() * 100));
        assertEquals(27314, (int) (Temperature.ofCelsius(0).toKelvin() * 100));
        assertEquals(-27314, (int) (Temperature.ofKelvin(0).toCelsius() * 100));
        assertEquals(-45966, (int) (Temperature.ofKelvin(0).toFahrenheit() * 100));
    }

    @Test
    public void weight() {
        assertEquals(1, Weight.ofMilligram(1000).toGram());
        assertEquals(1, Weight.ofGram(1000).toKilogram());
        assertEquals(1, Weight.ofKilogram(1000).toTon());
        assertEquals(1_000_000_000, Weight.ofTon(1).toMilligram());
    }

}
