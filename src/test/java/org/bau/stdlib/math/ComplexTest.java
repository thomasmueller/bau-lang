package org.bau.stdlib.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ComplexTest {

    @Test
    public void test() {
        Complex a = new Complex(5.0, 6.0);
        Complex b = new Complex(-3.0, 4.0);
        assertEquals("5.0 + 6.0i", a.toString());
        assertEquals("-3.0 + 4.0i", b.toString());
        assertTrue(5.0 == a.re());
        assertTrue(6.0 == a.im());
        assertEquals("-11.0 + 60.0i", a.square().toString());
        assertEquals("-7.0 - 24.0i", b.square().toString());
        assertEquals("3.0 - 4.0i", b.negate().toString());
        assertEquals("2.0 + 10.0i", b.add(a).toString());
        assertEquals("8.0 + 2.0i", a.subtract(b).toString());
        assertEquals("-39.0 + 2.0i", a.multiply(b).toString());
        assertEquals("-39.0 + 2.0i", b.multiply(a).toString());
        assertEquals("0.36 - 1.52i", a.divide(b).toString());
        assertEquals("5.0 + 6.0i", a.divide(b).multiply(b).toString());
        assertEquals("5.0 - 6.0i", a.conjugate().toString());
        assertEquals("7.810249675906654", "" + a.abs());
        assertEquals("-6.685231390246571E-6 + 1.0000103108981198i", a.tan().toString());
        assertEquals("2.0", new Complex(4.0, 0).sqrt().toString());
        assertEquals("0.0 + 1.0i", new Complex(-1.0, 0).sqrt().toString());
        assertEquals("0.0", new Complex(0, 0).sqrt().toString());
        assertEquals("0.7071067811865476 + 0.7071067811865475i", new Complex(0.0, 1.0).sqrt().toString());

        Complex z = new Complex(3, 4);
        assertEquals("-13.128783081462158 - 15.200784463067954i", z.exp().toString());

        Complex x = new Complex(3, 4);
        Complex y = new Complex(5, 6);
        assertEquals("-1.8608933068808367 + 11.836767106764379i", x.pow(y).toString());

        assertEquals("1.6094379124341003 + 0.9272952180016122i", z.log().toString());

        assertTrue(x.equals(x));
        assertFalse(x.equals(y));
        assertFalse(x.hashCode() == y.hashCode());


    }
}
