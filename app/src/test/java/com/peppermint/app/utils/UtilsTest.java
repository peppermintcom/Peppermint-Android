package com.peppermint.app.utils;

import com.peppermint.app.BuildConfig;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import static junit.framework.Assert.assertEquals;

/**
 * Created by Nuno Luz on 21-02-2016.
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class)
public class UtilsTest {

    @Test
    public void testGetFriendlyDuration() {
        assertEquals(Utils.getFriendlyDuration(1000), "00:01");
        assertEquals(Utils.getFriendlyDuration(30000), "00:30");
        assertEquals(Utils.getFriendlyDuration(61000), "01:01");
        assertEquals(Utils.getFriendlyDuration(120000), "02:00");
        assertEquals(Utils.getFriendlyDuration(5955000), "99:15");
        assertEquals(Utils.getFriendlyDuration(3651000), "60:51");
    }

}

