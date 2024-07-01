package org.bau;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.bau.parser.Parser;
import org.junit.Test;

public class TestNull {

    @Test
    public void preventNullReturn() {
        try {
            new Parser("""
type Value
    data int

fun get(key int) Value
    if key <= 0
        return null
    result : Value
    result.data = key * 10
    return result

fun test()
    a : get(0)
    println(a.data)

test()
                    """).parse();
            fail();
        } catch (Exception e) {
            assertTrue(e.getMessage(), e.getMessage().indexOf("may not be 'null'") > 0);
        }
    }

    @Test
    public void preventNullAccess() {
        try {
            new Parser("""
type Value
    data int

fun get(key int) Value?
    if key <= 0
        return null
    result : new(Value)
    result.data = key * 10
    return result

fun test()
    a : get(0)
    println(a.data)

test()
                    """).parse();
            fail();
        } catch (Exception e) {
            assertTrue(e.getMessage().indexOf("'a' could be null here") > 0);
        }
    }       
  
}
