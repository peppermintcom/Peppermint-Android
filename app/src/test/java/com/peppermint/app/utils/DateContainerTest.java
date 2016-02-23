package com.peppermint.app.utils;

import com.peppermint.app.BuildConfig;

import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.text.ParseException;

import static junit.framework.Assert.assertEquals;

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

}

