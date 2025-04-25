package org.bau.stdlib.string;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StringFormatTest {

    @Test
    public void pad() {
        assertEquals("hello", String.format("%3s", "hello"));
        assertEquals("hello", StringFormat.pad("hello", 3));
        assertEquals("hello     ", String.format("%-10s", "hello"));
        assertEquals("hello     ", StringFormat.pad("hello", 10));
        assertEquals("       123", String.format("%10d", 123));
        assertEquals("       123", StringFormat.pad(123, 10));
        assertEquals("123", String.format("%2d", 123));
        assertEquals("123", StringFormat.pad(123, 2));
        assertEquals("     3.142", String.format("%10.3f", 3.1415));
        assertEquals("     3.142", StringFormat.pad(3.1415, 10, 3));
        assertEquals("      1.23", String.format("%10.2f", 1.2345));
        assertEquals("      1.23", StringFormat.pad(1.2345, 10, 2));
        assertEquals("3.142", String.format("%3.3f", 3.1415));
        assertEquals("3.142", StringFormat.pad(3.1415, 3, 3));
        assertEquals("-3.142", StringFormat.pad(-3.1415, 3, 3));
        assertEquals("0012abcd34", String.format("%010x", 0x12abcd34));
        assertEquals("0012abcd34", StringFormat.hex(0x12abcd34, 10));
        assertEquals("0012abcd34", String.format("%010x", 0x12abcd34));
        assertEquals("12abcd34", StringFormat.hex(0x12abcd34, 4));
        assertEquals("12abcd34", StringFormat.hex(0x12abcd34));
    }

    @Test
    public void format() {
        assertEquals("Hello 123 world",
                StringFormat.format("Hello ", "123", " world"));
    }

    @Test
    public void sql() {
        assertEquals("select * from users where password='123' limit 1",
                StringFormat.sql("select * from users where password='",
                        "123",
                        "' limit 1"));
        assertEquals("select * from users where password='attack '' or ''''=''' limit 1",
                StringFormat.sql("select * from users where password='",
                        "attack ' or ''='",
                        "' limit 1"));
        assertEquals("select * from users where password='123' and user='abc' limit 1",
                StringFormat.sql("select * from users where password='",
                        "123",
                        "' and user='abc' limit 1"));
    }

}
