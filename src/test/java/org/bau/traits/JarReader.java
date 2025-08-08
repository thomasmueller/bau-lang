package org.bau.traits;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Utility class for reading JAR files and iterating over .class files within them.
 */
public class JarReader {

    /**
     * Returns an iterator over all .class files in the given JAR file.
     *
     * @param jarFileName the path to the JAR file
     * @return an iterator of .class file names (without the .class extension)
     * @throws IOException if the JAR file cannot be read
     */
    public static Iterator<String> getClassFileIterator(String jarFileName) throws IOException {
        return new ClassFileIterator(jarFileName);
    }

    /**
     * Iterator implementation for .class files in a JAR file.
     */
    private static class ClassFileIterator implements Iterator<String> {
        private final JarFile jarFile;
        private final Enumeration<JarEntry> entries;
        private String nextClassName;

        public ClassFileIterator(String jarFileName) throws IOException {
            this.jarFile = new JarFile(jarFileName);
            this.entries = jarFile.entries();
            advance();
        }

        @Override
        public boolean hasNext() {
            return nextClassName != null;
        }

        @Override
        public String next() {
            if (nextClassName == null) {
                throw new NoSuchElementException();
            }
            String current = nextClassName;
            advance();
            return current;
        }

        private void advance() {
            nextClassName = null;
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                int slash = name.indexOf('/');
                if (slash < 0) {
                    continue;
                }
                name = name.substring(slash + 1);

                // Check if this is a .class file (not a directory)
                String suffix = ".java";
                if (name.endsWith(suffix) && !entry.isDirectory()) {
                    // Convert path to class name (remove .class extension and convert / to .)
                    nextClassName = name.substring(0, name.length() - suffix.length()).replace('/', '.');
                    break;
                }
            }

            // If we've reached the end, close the JAR file
            if (nextClassName == null) {
                try {
                    jarFile.close();
                } catch (IOException e) {
                    // Log or handle the exception as needed
                    System.err.println("Warning: Failed to close JAR file: " + e.getMessage());
                }
            }
        }
    }
}