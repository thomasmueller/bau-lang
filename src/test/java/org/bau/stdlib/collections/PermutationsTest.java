package org.bau.stdlib.collections;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PermutationsTest {

    @Test
    public void permutations() {
        int len = 3;
        Character[] input = new Character[len];
        for(int i=0; i<len; i++) {
            input[i] = (char)('a' + i);
        }
        assertEquals("abc;acb;bac;bca;cab;cba;", getList(input, 3));
        assertEquals("ab;ac;ba;bc;ca;cb;", getList(input, 2));
        assertEquals("a;b;c;", getList(input, 1));
    }

    private String getList(Character[] input, int len) {
        Character[] output = new Character[len];
        Permutations<Character> perm = new Permutations<>(input, output);
        StringBuffer buff = new StringBuffer();
        while(perm.next()) {
                for(int i=0; i<len; i++) {
                    buff.append(output[i]);
                }
                buff.append(';');
        }
        return buff.toString();
    }
}
