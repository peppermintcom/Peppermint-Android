package com.peppermint.app.utils;

import junit.framework.TestCase;

import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;

import java.text.ParseException;

/**
 * Created by Nuno Luz on 21-02-2016.
 */
public class DateContainerTest extends TestCase {

    @Before
    public void setup() throws Exception {
    }

    @Test
    public void testParseDateTime() throws ParseException {
        String originalDate = "2015-01-01 12:34:56";

        DateContainer dc = new DateContainer(DateContainer.TYPE_DATETIME, originalDate);
        assertEquals(dc.getAsString(DateTimeZone.UTC), originalDate);

        assertEquals(dc.getDateTime().getYear(), 2015);
        assertEquals(dc.getDateTime().getMonthOfYear(), 01);
        assertEquals(dc.getDateTime().getDayOfMonth(), 01);
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

}

