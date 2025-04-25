package org.bau.stdlib.csv;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

public class Csv {

    private static final int BUFFER_SIZE = 4 * 1024;

    private final Reader reader;
    private final Writer writer;

    private char escape = '\"';
    private char delimiter = '\"';
    private char separator = ',';
    private String newline = "\n";

    private char[] buffer;
    private int pos;
    private int start = -1;
    private int end;
    private boolean endOfLine, endOfFile;

    public static Writer openWriter(String fileName) throws IOException {
        OutputStream out = new FileOutputStream(fileName);
        out = new BufferedOutputStream(out);
        return new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
    }

    public static Reader openReader(String fileName) throws IOException {
        InputStream in = new FileInputStream(fileName);
        in = new BufferedInputStream(in);
        Reader input = new InputStreamReader(in, StandardCharsets.UTF_8);
        return new BufferedReader(input);
    }

    public static Csv newWriter(Writer output) throws IOException {
        return new Csv(output, null);
    }

    public static Csv newReader(Reader input) throws IOException {
        input.mark(1);
        int bom = input.read();
        if (bom != 0xfeff) {
            // Microsoft Excel compatibility
            // ignore pseudo-BOM
            input.reset();
        }
        return new Csv(null, input);
    }

    private Csv(Writer output, Reader input) throws IOException {
        this.writer = output;
        this.reader = input;
        this.buffer = new char[BUFFER_SIZE * 2];
    }

    public void writeRow(String[] values) throws IOException {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                writer.write(separator);
            }
            String s = values[i];
            if (s != null) {
                if (escape != 0 && delimiter != 0) {
                    writer.write(delimiter);
                    writer.write(escape(s));
                    writer.write(delimiter);
                } else {
                    writer.write(s);
                }
            }
        }
        writer.write(newline);
    }

    private String escape(String data) {
        if (data.indexOf(delimiter) < 0) {
            if (escape == delimiter || data.indexOf(escape) < 0) {
                return data;
            }
        }
        int length = data.length();
        StringBuilder buff = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            char c = data.charAt(i);
            if (c == delimiter || c == escape) {
                buff.append(escape);
            }
            buff.append(c);
        }
        return buff.toString();
    }

    private int nextChar() throws IOException {
        if (pos >= end) {
            return fillBuffer();
        }
        return buffer[pos++];
    }

    private int fillBuffer() throws IOException {
        if (endOfFile) {
            return -1;
        }
        int keep;
        if (start >= 0) {
            keep = pos - start;
            if (keep > 0) {
                char[] src = buffer;
                if (keep + BUFFER_SIZE > src.length) {
                    buffer = new char[src.length * 2];
                }
                System.arraycopy(src, start, buffer, 0, keep);
            }
            start = 0;
        } else {
            keep = 0;
        }
        pos = keep;
        int len = reader.read(buffer, keep, BUFFER_SIZE);
        if (len == -1) {
            // ensure pos > end
            // even after push back
            end = -1024;
            endOfFile = true;
            // ensure the right number of characters are read
            // in case the input buffer is still used
            pos++;
            return -1;
        }
        end = keep + len;
        return buffer[pos++];
    }

    private String readValue() throws IOException {
        endOfLine = false;
        start = pos;
        while (true) {
            int c = nextChar();
            if (c == delimiter) {
                boolean isEscaped = false;
                start = pos;
                int sep = 1;
                while (true) {
                    c = nextChar();
                    if (c == delimiter) {
                        c = nextChar();
                        if (c != delimiter) {
                            sep = 2;
                            break;
                        }
                        isEscaped = true;
                    } else if (c == escape) {
                        c = nextChar();
                        if (c < 0) {
                            break;
                        }
                        isEscaped = true;
                    } else if (c < 0) {
                        break;
                    }
                }
                String s = new String(buffer,
                        start, pos - start - sep);
                if (isEscaped) {
                    s = unescape(s);
                }
                start = -1;
                while (true) {
                    if (c == separator) {
                        break;
                    } else if (c == '\n' || c < 0 || c == '\r') {
                        endOfLine = true;
                        break;
                    } else if (c == ' ') {
                        // ignore
                    } else {
                        pos--;
                        break;
                    }
                    c = nextChar();
                }
                return s;
            } else if (c == '\n' || c < 0 || c == '\r') {
                endOfLine = true;
                return null;
            } else if (c == separator) {
                return null;
            } else if (c <= ' ') {
                // ignore
            } else {
                // remove delimiters
                while (true) {
                    c = nextChar();
                    if (c == separator) {
                        break;
                    } else if (c == '\n' || c < 0 || c == '\r') {
                        endOfLine = true;
                        break;
                    }
                }
                String s = new String(buffer,
                        start, pos - start - 1);
                start = -1;
                return s;
            }
        }
    }

    private String unescape(String s) {
        StringBuilder buff = new StringBuilder(s.length());
        int start = 0;
        char[] chars = null;
        while (true) {
            int i = s.indexOf(escape, start);
            if (i < 0) {
                i = s.indexOf(delimiter, start);
                if (i < 0) {
                    break;
                }
            }
            if (chars == null) {
                chars = s.toCharArray();
            }
            buff.append(chars, start, i - start);
            if (i == s.length() - 1) {
                start = s.length();
                break;
            }
            buff.append(chars[i + 1]);
            start = i + 2;
        }
        buff.append(s, start, s.length());
        return buff.toString();
    }

    public String[] readRow(int columnCount) throws IOException {
        if (reader == null) {
            return null;
        }
        String[] row = new String[columnCount];
        int i = 0;
        while (true) {
            String v = readValue();
            if (v == null) {
                if (endOfLine) {
                    if (i == 0) {
                        if (endOfFile) {
                            return null;
                        }
                        // empty line
                        continue;
                    }
                    break;
                }
            }
            if (i < row.length) {
                row[i++] = v;
            }
            if (endOfLine) {
                break;
            }
        }
        return row;
    }

    public void close() throws IOException {
        if (reader != null) {
            reader.close();
        }
        if (writer != null) {
            writer.close();
        }
    }

    public void setSeparator(char separator) {
        this.separator = separator;
    }

    public void setDelimiter(char delimiter) {
        this.delimiter = delimiter;
    }

    public void setEscape(char escape) {
        this.escape = escape;
    }

    public void setNewline(String newline) {
        this.newline = newline;
    }

}
