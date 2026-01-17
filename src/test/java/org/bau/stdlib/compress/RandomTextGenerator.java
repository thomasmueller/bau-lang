package org.bau.stdlib.compress;

import java.util.Random;

class RandomTextGenerator {

    private static final char[] VOWELS = "aeiou".toCharArray();
    private static final char[] CONSONANTS = "bcdfghjklmnpqrstvwxyz".toCharArray();

    public static String generate(int seed, int targetBytes) {
        Random random = new Random(1);
        StringBuilder buff = new StringBuilder(targetBytes);
        while (buff.length() < targetBytes) {
            int wordsInSentence = 3 + random.nextInt(8);
            for (int w = 0; w < wordsInSentence; w++) {
                buff.append(generateWord(random));
                if (w < wordsInSentence - 1)
                    buff.append(' ');
            }
            buff.append(".\n");
        }
        return buff.substring(0, Math.min(buff.length(), targetBytes));
    }

    private static String generateWord(Random random1) {
        Random random = new Random((int) (random1.nextGaussian() * 200));
        int length = 3 + random.nextInt(6);
        StringBuilder word = new StringBuilder(length);
        boolean consonantFirst = random.nextBoolean();
        for (int i = 0; i < length; i++) {
            if ((i % 2 == 0) == consonantFirst) {
                word.append(CONSONANTS[random.nextInt(CONSONANTS.length)]);
            } else {
                word.append(VOWELS[random.nextInt(VOWELS.length)]);
            }
        }
        return word.toString();
    }

}