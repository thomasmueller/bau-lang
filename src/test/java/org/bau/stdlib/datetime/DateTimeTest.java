package org.bau.stdlib.datetime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import org.bau.stdlib.datetime.DateTime.DateTimeInfo;
import org.bau.stdlib.datetime.DateTime.TimeConvert;
import org.junit.Test;

public class DateTimeTest {

    @Test
    public void duration() {
        // many ways to do the same
        assertEquals(900, TimeUnit.MINUTES.toSeconds(15));
        assertEquals(900, Duration.ofMinutes(15).toSeconds());
        assertEquals(900, Duration.ofMinutes(15).getSeconds());
        assertEquals(900, TimeUnit.SECONDS.convert(15, TimeUnit.MINUTES));
        assertEquals(900, Duration.of(15, ChronoUnit.MINUTES).toSeconds());
        assertEquals(900, 15 * ChronoUnit.MINUTES.getDuration().toSeconds());
        assertEquals(900, Duration.from(Duration.ofMinutes(15)).getSeconds());
        assertEquals(900, Duration.of(15, ChronoUnit.MINUTES).get(ChronoUnit.SECONDS));

        assertEquals(900, TimeConvert.ofMinutes(15).toSeconds());
        assertEquals(Duration.ofDays(1).toMillis(), TimeConvert.ofDays(1).toMillis());
        assertEquals(Duration.ofHours(1).toMinutes(), TimeConvert.ofHours(1).toMinutes());
        assertEquals(Duration.ofDays(1).toHours(), TimeConvert.ofDays(1).toHours());
        assertEquals(Duration.ofHours(48).toDays(), TimeConvert.ofHours(48).toDays());
        assertEquals(Duration.ofMillis(1000).toSeconds(), TimeConvert.ofMillis(1000).toSeconds());
        assertEquals(Duration.ofSeconds(60).toMillis(), TimeConvert.ofSeconds(60).toMillis());

        assertEquals(1000, TimeConvert.ofDays(1000).toDays());
        assertEquals(10000, TimeConvert.ofDays(10000).toDays());
        assertEquals(24_000_000_000L, TimeConvert.ofDays(1_000_000_000).toHours());
        assertEquals(-1_000_000_000, TimeConvert.ofDays(-1_000_000_000).toDays());
        assertEquals(1_000_000, TimeConvert.ofDays(7_000_000).toWeeks());
        assertEquals(1_000_000_000L, TimeConvert.ofSeconds(1).toNanos());
    }

    @Test
    public void formatDateTime() {
        DateTimeInfo info = new DateTimeInfo();
        info.year = 2001;
        info.month = 12;
        info.day = 31;
        info.hour = 23;
        info.minute = 59;
        info.second = 36;
        info.millis = 123;
        assertEquals("2001-12-31T23:59:36.123", DateTime.formatDateTime(info, "YYYY-MM-DDThh:mm:ss.fff"));
        assertEquals("01-12-31 23:59:36", DateTime.formatDateTime(info, "YY-MM-DD hh:mm:ss"));
        assertEquals("23:59:36", DateTime.formatDateTime(info, "hh:mm:ss"));
        assertEquals("year: 2001, month: 12, day: 31, hours: 23, minutes: 59, seconds: 36, millis: 123",
                DateTime.formatDateTime(info,
                        "year: YYYY, month: MM, day: DD, hours: hh, minutes: mm, seconds: ss, millis: fff"));
    }

    @Test
    public void parseDateTime() {
        DateTimeInfo info = DateTime.parseDateTime("2001-12-31T23:59:36.123", "YYYY-MM-DDThh:mm:ss.fff");
        assertEquals(2001, info.year);
        assertEquals(12, info.month);
        assertEquals(31, info.day);
        assertEquals(23, info.hour);
        assertEquals(59, info.minute);
        assertEquals(36, info.second);
        assertEquals(123, info.millis);
        info = DateTime.parseDateTime("01-12-31 23:59:36", "YY-MM-DD hh:mm:ss");
        assertEquals(2001, info.year);
        assertEquals(12, info.month);
        assertEquals(31, info.day);
        assertEquals(23, info.hour);
        assertEquals(59, info.minute);
        assertEquals(36, info.second);
        assertEquals(0, info.millis);
    }

    @Test
    public void millisToDateConversion() {
        LocalDateTime t = LocalDateTime.of(2001, 12, 31, 23, 59);
        Instant instant = t.toInstant(ZoneOffset.UTC);
        long millisecondsSinceEpoch = instant.toEpochMilli();
        DateTimeInfo info = DateTime.getDateTimeInfo(millisecondsSinceEpoch);
        assertEquals(2001, info.year);
        assertEquals(12, info.month);
        assertEquals(31, info.day);
        assertEquals(23, info.hour);
        assertEquals(59, info.minute);
        for(int year = 0; year < 3_000; year++) {
            assertEquals("year: " + year, DateTime.isLeapYear(year), Year.isLeap(year));
            for(int month = 0; month <= 13; month++) {
                if (month >= 1 && month <= 12) {
                    int daysForMonth = YearMonth.of((int) year, month).lengthOfMonth();
                    int got = DateTime.getLengthOfMonth(year, month);
                    assertEquals(daysForMonth, got);
                }
                for(int day = -1; day < 34; day++) {
                    if (day > 0 && month > 0 && month <= 12 &&
                            YearMonth.of(year, month).isValidDay(day)) {
                        assertTrue(DateTime.isValidDate(year, month, day));
                    } else {
                        assertFalse(DateTime.isValidDate(year, month, day));
                        continue;
                    }
                    t = LocalDateTime.of(year, month, day, 0, 0);
                    instant = t.toInstant(ZoneOffset.UTC);
                    long millis = instant.toEpochMilli();
                    assertEquals(t.getDayOfWeek().getValue(), DateTime.getDayOfWeek(year, month, day));
                    assertEquals(t.getDayOfWeek().getValue(), DateTime.getDayOfWeek(millis));
                    assertEquals(t.getDayOfYear(), DateTime.getDayOfYear(year, month, day));
                    info = DateTime.getDateTimeInfo(millis);
                    assertEquals(year, info.year);
                    assertEquals(month, info.month);
                    assertEquals(day, info.day);
                    assertEquals(0, info.hour);
                    assertEquals(0, info.minute);
                    assertEquals(millis, DateTime.getDateEpochMillis(year, month, day));

                }
            }
        }
    }

}
