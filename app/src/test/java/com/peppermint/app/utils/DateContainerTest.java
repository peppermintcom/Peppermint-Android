package com.peppermint.app.utils;

import com.peppermint.app.BuildConfig;

import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.text.ParseException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * Created by Nuno Luz on 21-02-2016.
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class)
public class DateContainerTest {

    @Test
    public void testParseDateTime() throws ParseException {
        String originalDate = "2015-01-01 12:34:56";

        DateContainer dc = new DateContainer(DateContainer.TYPE_DATETIME, originalDate);
        assertEquals(dc.getAsString(DateTimeZone.UTC), originalDate);

        assertEquals(dc.getDateTime().getYear(), 2015);
        assertEquals(dc.getDateTime().getMonthOfYear(), 1);
        assertEquals(dc.getDateTime().getDayOfMonth(), 1);
        assertEquals(dc.getDateTime().getHourOfDay(), 12);
        assertEquals(dc.getDateTime().getMinuteOfHour(), 34);
        assertEquals(dc.getDateTime().getSecondOfMinute(), 56);
    }

    @Test
    public void testParseDate() throws ParseException {
        String originalDate = "2015-01-01 12:34:56";

        DateContainer dc = new DateContainer(DateContainer.TYPE_DATE, originalDate);
        assertEquals(dc.getAsString(DateTimeZone.UTC), originalDate.substring(0, 10));
    }

    @Test
    public void testParseTime() throws ParseException {
        String originalDate = "2015-01-01 12:34:56";

        DateContainer dc = new DateContainer(DateContainer.TYPE_TIME, originalDate);
        assertEquals(dc.getAsString(DateTimeZone.UTC), originalDate.substring(11));
    }

    @Test
    public void testZeroTime() throws ParseException {
        String originalDate = "2015-01-01 12:34:56";

        DateContainer dc = new DateContainer(DateContainer.TYPE_DATETIME, originalDate);
        dc.zeroTime();

        assertEquals(dc.getAsString(DateTimeZone.UTC), originalDate.substring(0, 10) + " 00:00:00");
    }

    @Test
    public void testFriendlyLabelRelativeToToday() throws ParseException {
        DateContainer relativeDate = new DateContainer(DateContainer.TYPE_DATETIME, "2015-12-12 01:34:20");

        DateContainer dc = new DateContainer(DateContainer.TYPE_DATETIME, "2014-12-01 12:34:56");
        assertEquals(DateContainer.getDateAsStringRelativeTo(RuntimeEnvironment.application, dc, relativeDate, DateTimeZone.UTC), "1 Dec 2014");

        dc = new DateContainer(DateContainer.TYPE_DATETIME, "2015-12-12 12:34:56");
        assertEquals(DateContainer.getDateAsStringRelativeTo(RuntimeEnvironment.application, dc, relativeDate, DateTimeZone.UTC), "Today");

        dc = new DateContainer(DateContainer.TYPE_DATETIME, "2015-12-11 12:34:56");
        assertEquals(DateContainer.getDateAsStringRelativeTo(RuntimeEnvironment.application, dc, relativeDate, DateTimeZone.UTC), "Yesterday");

        dc = new DateContainer(DateContainer.TYPE_DATETIME, "2015-12-09 12:34:56");
        assertEquals(DateContainer.getDateAsStringRelativeTo(RuntimeEnvironment.application, dc, relativeDate, DateTimeZone.UTC), "Wednesday");

        dc = new DateContainer(DateContainer.TYPE_DATETIME, "2015-12-01 12:34:56");
        assertEquals(DateContainer.getDateAsStringRelativeTo(RuntimeEnvironment.application, dc, relativeDate, DateTimeZone.UTC), "1 Dec");
    }

    @Test
    public void testComparison() throws ParseException {
        DateContainer dc1 = new DateContainer(DateContainer.TYPE_DATETIME, "2015-12-12 01:34:20");
        DateContainer dc2 = new DateContainer(DateContainer.TYPE_DATETIME, "2015-12-13 01:34:20");
        assertTrue(dc1.compareTo(dc2) < 0);

        dc1 = new DateContainer(DateContainer.TYPE_TIME, "2015-12-12 01:34:20");
        dc2 = new DateContainer(DateContainer.TYPE_TIME, "2015-12-13 01:34:20");
        assertTrue(dc1.compareTo(dc2) == 0);

        dc1 = new DateContainer(DateContainer.TYPE_TIME, "2015-12-12 02:34:20");
        dc2 = new DateContainer(DateContainer.TYPE_TIME, "2015-12-12 01:34:20");
        assertTrue(dc1.compareTo(dc2) > 0);

        dc1 = new DateContainer(DateContainer.TYPE_DATE, "2016-01-12 01:34:20");
        dc2 = new DateContainer(DateContainer.TYPE_DATE, "2015-12-13 01:34:20");
        assertTrue(dc1.compareTo(dc2) > 0);
    }

}

