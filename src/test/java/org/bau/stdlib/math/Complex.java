package org.bau.stdlib.math;

import java.util.Objects;

public class Complex {

    private final double re;
    private final double im;

    public Complex(double real, double imaginary) {
        this.re = real;
        this.im = imaginary;
    }

    public Complex add(Complex x) {
        return new Complex(re + x.re, im + x.im);
    }

    public Complex subtract(Complex x) {
        return new Complex(re - x.re, im - x.im);
    }

    public Complex multiply(Complex x) {
        return new Complex(re * x.re - im * x.im, re * x.im + im * x.re);
    }

    public Complex conjugate() {
        return new Complex(re, -im);
    }

    public double abs() {
        return Math.sqrt(re * re + im * im);
    }

    public Complex divide(Complex x) {
        return multiply(x.reciprocal());
    }

    public Complex square() {
        return new Complex(re * re - im * im, 2 * re * im);
    }

    public Complex reciprocal() {
        double scale = re * re + im * im;
        return new Complex(re / scale, -im / scale);
    }

    public Complex negate() {
        return new Complex(-re, -im);
    }

    public double re() {
        return re;
    }

    public double im() {
        return im;
    }

    public Complex exp() {
        double r = Math.exp(re);
        return new Complex(r * Math.cos(im), r * Math.sin(im));
    }

    public Complex sin() {
        return new Complex(Math.sin(re) * Math.cosh(im), Math.cos(re) * Math.sinh(im));
    }

    public Complex cos() {
        return new Complex(Math.cos(re) * Math.cosh(im), -Math.sin(re) * Math.sinh(im));
    }

    public Complex tan() {
        return sin().divide(cos());
    }

    public Complex sqrt() {
        if (re == 0.0 && im == 0.0) {
            return new Complex(0.0, 0.0);
        }
        double t = Math.sqrt((Math.abs(re) + abs()) / 2.0);
        if (re >= 0.0) {
            return new Complex(t, im / (2.0 * t));
        } else {
            return new Complex(Math.abs(im) / (2.0 * t), Math.copySign(1d, im) * t);
        }
    }

    public Complex pow(Complex x) {
        return log().multiply(x).exp();
    }

    public Complex log() {
        return new Complex(Math.log(abs()), Math.atan2(im, re));
    }

    @Override
    public String toString() {
        String rea = "" + re;
        String ima;
        if (im > 0) {
            ima = " + " + im + "i";
        } else if (im < 0) {
            ima = " - " + -im + "i";
        } else {
            ima = "";
        }
        return rea + ima;
    }

    public boolean equals(Object x) {
        if (x == null)
            return false;
        if (this.getClass() != x.getClass())
            return false;
        Complex that = (Complex) x;
        return (this.re == that.re) && (this.im == that.im);
    }

    public int hashCode() {
        return Objects.hash(re, im);
    }

}
