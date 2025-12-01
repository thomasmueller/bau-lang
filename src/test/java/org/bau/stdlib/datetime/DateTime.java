package org.bau.stdlib.datetime;

public class DateTime {

    public static final long NANOS_PER_MILLI = 1000000L;
    public static final long MILLIS_PER_SECOND = 1000L;
    public static final long MILLIS_PER_MINUTE = 60 * MILLIS_PER_SECOND;
    public static final long MILLIS_PER_HOUR = 60 * MILLIS_PER_MINUTE;
    public static final long MILLIS_PER_DAY = 24 * MILLIS_PER_HOUR;
    public static final long MILLIS_PER_WEEK = 7 * MILLIS_PER_DAY;

    /**
     * A time range. Values that do not fit in a 64-bit value (that is, more than
     * 100'000 days) are not supported.
     */
    static class TimeConvert {
        final long millis;

        private TimeConvert(long millis) {
            this.millis = millis;
        }

        static TimeConvert ofMillis(long millis) {
            return new TimeConvert(millis);
        }

        static TimeConvert ofSeconds(long seconds) {
            return new TimeConvert(seconds * MILLIS_PER_SECOND);
        }

        static TimeConvert ofMinutes(long minutes) {
            return new TimeConvert(minutes * MILLIS_PER_MINUTE);
        }

        static TimeConvert ofHours(long hours) {
            return new TimeConvert(hours * MILLIS_PER_HOUR);
        }

        static TimeConvert ofDays(long days) {
            return new TimeConvert(days * MILLIS_PER_DAY);
        }

        static TimeConvert ofWeeks(long weeks) {
            return new TimeConvert(weeks * MILLIS_PER_HOUR);
        }

        long toNanos() {
            return millis * NANOS_PER_MILLI;
        }

        long toMillis() {
            return millis;
        }

        long toSeconds() {
            return millis / MILLIS_PER_SECOND;
        }

        long toMinutes() {
            return millis / MILLIS_PER_MINUTE;
        }

        long toHours() {
            return millis / MILLIS_PER_HOUR;
        }

        long toDays() {
            return millis / MILLIS_PER_DAY;
        }

        long toWeeks() {
            return millis / MILLIS_PER_WEEK;
        }

    };

    public static long getTimeMillis(long hour, long minute, long second) {
        return MILLIS_PER_HOUR * hour + MILLIS_PER_MINUTE * minute + MILLIS_PER_SECOND * second;
    }

    public static long getDayOfWeek(long year, int month, int day) {
        return getDayOfWeek(getDateEpochMillis(year, month, day));
    }

    public static int getLengthOfMonth(long year, int month) {
        int mask = 0xeefbb3 + (isLeapYear(year) ? 4 : 0);
        return 28 + ((mask >> ((month - 1) * 2)) & 3);
    }

    public static int getDayOfWeek(long epochMillis) {
        int epochDay = (int) (epochMillis / MILLIS_PER_DAY);
        return (epochDay % 7 + 10) % 7 + 1;
    }

    public static long getDateEpochMillis(long year, int month, int day) {
        return getEpochDay(year, month, day) * MILLIS_PER_DAY;
    }

    public static int getDayOfYear(long year, int month, int day) {
        long epochDay = getEpochDay(year, month, day);
        return (int) (epochDay - getEpochDay(year, 1, 1)) + 1;
    }

    public static boolean isValidDate(long year, int month, int day) {
        long epochDay = getEpochDay(year, month, day);
        DateTimeInfo info = getDateTimeInfo(epochDay * MILLIS_PER_DAY);
        return year == info.year && month == info.month && day == info.day;
    }

    public static boolean isLeapYear(long year) {
        // https://hueffner.de/falk/blog/a-leap-year-check-in-three-instructions.html
        return ((year * 1073750999L) & 3221352463L) <= 126976;
        // return (year & 3) == 0 && (year % 100 != 0 || year % 400 == 0);
    }

    public static class DateTimeInfo {
        int year;
        int month;
        int day;
        int hour;
        int minute;
        int second;
        int millis;
    }

    public static DateTimeInfo getDateTimeInfo(long epochMillis) {
        DateTimeInfo info = new DateTimeInfo();
        long seconds = epochMillis / MILLIS_PER_SECOND;
        info.millis = (int) (epochMillis % MILLIS_PER_SECOND);
        info.second = (int) (seconds % 60);
        info.minute = (int) ((seconds / 60) % 60);
        info.hour = (int) ((seconds / 3600) % 24);
        // see https://www.benjoffe.com/fast-date
        int r = (int) (11260485 - epochMillis / MILLIS_PER_DAY);
        int c = (int) (r * 3853261555L >>> 47);
        int j = r + c - (c >>> 2);
        int y = (int) ((j * 3010298776L) >>> 40);
        int n = 979360 - (j - ((y * 1461) >>> 2)) * 2141;
        int m = n >>> 16;
        int b = m > 12 ? 1 : 0;
        info.year = 32799 - y + b;
        info.month = b == 1 ? m - 12 : m;
        info.day = (int)(((n & 0xffff) * 2006057L) >>> 32) + 1;
        return info;
    }

    private static long getEpochDay(long y, int m, int d) {
        long b = m <= 2 ? 1 : 0;
        y += 5880000 - b;
        long c = y / 100;
        long s = b == 1 ? 8829 : -2919;
        long yd = y * 365 + (y >>> 2) - c + (c >>> 2);
        long md = (979 * m + s) >>> 5;
        return yd + md + d - 2148345369L;
    }

    public static DateTimeInfo parseDateTime(String data, String format) {
        if (data.length() < format.length()) {
            return null;
        }
        DateTimeInfo result = new DateTimeInfo();
        char[] buff = format.toCharArray();
        char[] array = data.toCharArray();
        for (int i = 0; i < buff.length; i++) {
            char f = buff[i];
            int value;
            switch (f) {
            case 'Y':
                value = parseInt(buff, array, i, 4);
                if (value >= 0) {
                    result.year = value;
                    i += 3;
                } else {
                    value = parseInt(buff, array, i, 2);
                    if (value >= 0) {
                        result.year = 2000 + value;
                        i++;
                    }
                }
                break;
            case 'M':
                value = parseInt(buff, array, i, 2);
                if (value >= 0) {
                    result.month = value;
                    i++;
                }
                break;
            case 'D':
                value = parseInt(buff, array, i, 2);
                if (value >= 0) {
                    result.day = value;
                    i++;
                }
                break;
            case 'h':
                value = parseInt(buff, array, i, 2);
                if (value >= 0) {
                    result.hour = value;
                    i++;
                }
                break;
            case 'm':
                value = parseInt(buff, array, i, 2);
                if (value >= 0) {
                    result.minute = value;
                    i++;
                }
                break;
            case 's':
                value = parseInt(buff, array, i, 2);
                if (value >= 0) {
                    result.second = value;
                    i++;
                }
                break;
            case 'f':
                value = parseInt(buff, array, i, 3);
                if (value >= 0) {
                    result.millis = value;
                    i+= 2;
                }
                break;
            }
        }
        return result;
    }

    private static int parseInt(char[] buff, char[] value, int index, int len) {
        char match = buff[index];
        int result = value[index] - '0';
        for (int i = 1; i < len; i++) {
            if (index + i > buff.length || buff[index + i] != match) {
                return -1;
            }
            result = result * 10 + (value[index + i] - '0');
        }
        return result;
    }

    public static String formatDateTime(DateTimeInfo info, String format) {
        char[] buff = format.toCharArray();
        for (int i = 0; i < buff.length; i++) {
            char f = buff[i];
            switch (f) {
            case 'Y':
                i = writeInt(buff, i, 4, info.year);
                i = writeInt(buff, i, 2, info.year);
                break;
            case 'M':
                i = writeInt(buff, i, 2, info.month);
                break;
            case 'D':
                i = writeInt(buff, i, 2, info.day);
                break;
            case 'h':
                i = writeInt(buff, i, 2, info.hour);
                break;
            case 'm':
                i = writeInt(buff, i, 2, info.minute);
                break;
            case 's':
                i = writeInt(buff, i, 2, info.second);
                break;
            case 'f':
                i = writeInt(buff, i, 3, info.millis);
                break;
            }
        }
        return new String(buff);
    }

    private static int writeInt(char[] buff, int index, int len, long value) {
        char match = buff[index];
        int i = 1;
        for (; i < len; i++) {
            if (index + i > buff.length || buff[index + i] != match) {
                return index;
            }
        }
        while (i > 0) {
            i--;
            buff[index + i] = (char) ('0' + (value % 10));
            value /= 10;
        }
        return index + len;
    }

}
