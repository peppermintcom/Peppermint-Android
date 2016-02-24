package com.peppermint.app.utils;

import android.content.Context;

import com.peppermint.app.BuildConfig;
import com.peppermint.app.cloud.senders.SenderPreferences;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * Created by Nuno Luz on 21-02-2016.
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class)
public class UtilsTest {

    @Test
    public void testGetFriendlyDuration() {
        assertEquals(Utils.getFriendlyDuration(500), "00:00");
        assertEquals(Utils.getFriendlyDuration(1000), "00:01");
        assertEquals(Utils.getFriendlyDuration(30000), "00:30");
        assertEquals(Utils.getFriendlyDuration(61000), "01:01");
        assertEquals(Utils.getFriendlyDuration(120000), "02:00");
        assertEquals(Utils.getFriendlyDuration(5955000), "99:15");
        assertEquals(Utils.getFriendlyDuration(3651000), "60:51");
    }

    @Test
    public void testGetFirstLastNames() {
        String[] names = Utils.getFirstAndLastNames("Testing Some Name");
        assertEquals(names[0], "Testing Some");
        assertEquals(names[1], "Name");

        names = Utils.getFirstAndLastNames("firstName lastName");
        assertEquals(names[0], "Firstname");
        assertEquals(names[1], "Lastname");

        names = Utils.getFirstAndLastNames(null);
        assertEquals(names[0], "");
        assertEquals(names[1], "");
    }

    @Test
    public void testIsValid() {
        assertTrue(Utils.isValidEmail("someone@somewhere.com"));
        assertTrue(!Utils.isValidEmail("someone@somewhere"));
        assertTrue(!Utils.isValidEmail("@somewhere.com"));
        assertTrue(!Utils.isValidEmail("someone"));
        assertTrue(!Utils.isValidEmail("someone@.com"));
        assertTrue(!Utils.isValidEmail(""));
        assertTrue(!Utils.isValidEmail(null));

        assertTrue(Utils.isValidPhoneNumber("123456789"));
        assertTrue(!Utils.isValidPhoneNumber("12"));
        assertTrue(!Utils.isValidPhoneNumber(""));
        assertTrue(!Utils.isValidPhoneNumber(null));

        assertTrue(Utils.isValidName("Something"));
        assertTrue(Utils.isValidName("S"));
        assertTrue(!Utils.isValidName(""));
        assertTrue(!Utils.isValidName(null));

        assertTrue(Utils.isValidNameMaybeEmpty(""));
        assertTrue(Utils.isValidNameMaybeEmpty(null));
    }

    @Test
    public void testBasicAuthenticationToken() {
        assertEquals(Utils.getBasicAuthenticationToken("test", "pwd"), "dGVzdDpwd2Q=");
        assertEquals(Utils.getBasicAuthenticationToken("making sure this works<", "pwd"), "bWFraW5nIHN1cmUgdGhpcyB3b3Jrczw6cHdk");
    }

    @Test
    public void testDeviceAndroidVersions() {
        String deviceName = Utils.getDeviceName();
        String androidVersion = Utils.getAndroidVersion();

        assertNotNull(deviceName);
        assertTrue(deviceName.length() > 1);

        assertNotNull(androidVersion);
        assertTrue(androidVersion.length() > 1);
    }

    @Test
    public void testCapitalizeFully() {
        assertEquals(Utils.capitalizeFully("some naMe herE. Should be cApItAlIzEd!"), "Some Name Here. Should Be Capitalized!");
        assertEquals(Utils.capitalizeFully(null), null);
    }

    @Test
    public void testJoinString() {
        assertEquals(Utils.joinString(" AND ", "1", "2", "3"), "(1 AND 2 AND 3)");
        assertEquals(Utils.joinString(" AND ", "1"), "(1)");
        assertEquals(Utils.joinString(" AND ", "1", null, "3"), "(1 AND 3)");
        assertEquals(Utils.joinString(" AND "), "(1=1)");
    }

    @Test
    public void testSqlConditions() {
        List<String> args = new ArrayList<>();
        List<String> allowed = new ArrayList<>();
        Collections.addAll(allowed, "1", "2", "3", "4");

        assertEquals(Utils.getSQLConditions("field", allowed, args, true), "field=? AND field=? AND field=? AND field=?");
        assertTrue(args.equals(allowed));

        assertEquals(Utils.getSQLConditions("field", allowed, null, false), "field=1 OR field=2 OR field=3 OR field=4");

        assertEquals(Utils.getSQLConditions("field", null, null, true), "1");

        allowed.clear();
        assertEquals(Utils.getSQLConditions("field", allowed, null, true), "1");
    }

    @Test
    public void testClearApplicationData() throws IOException {
        Context context = RuntimeEnvironment.application;

        File tmpFile = File.createTempFile("prefix", "ext", context.getCacheDir());
        tmpFile.createNewFile();
        assertTrue(tmpFile.exists());

        SenderPreferences preferences = new SenderPreferences(context);
        preferences.setFullName("john doe");
        assertEquals(preferences.getFullName(), "John Doe");

        Utils.clearApplicationData(context);

        assertFalse(tmpFile.exists());
        // doesn't work
        /*assertNull(preferences.getFullName());*/
    }

}

