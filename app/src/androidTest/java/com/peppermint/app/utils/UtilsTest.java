package com.peppermint.app.utils;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.InstrumentationTestCase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Created by Nuno Luz on 21-02-2016.
 */
@RunWith(AndroidJUnit4.class)
public class UtilsTest extends InstrumentationTestCase {

    private Context mContext;

    @Before
    public void setup() throws Exception {
        super.setUp();
        injectInstrumentation(InstrumentationRegistry.getInstrumentation());
        mContext = getInstrumentation().getContext();
    }

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

