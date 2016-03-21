package com.peppermint.app.utils;

import android.content.Context;
import android.support.annotation.NonNull;

import com.peppermint.app.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Created by Nuno Luz on 14-09-2015.
 *
 * Calendar wrapper with formatting and manipulation methods.
 */
public class DateContainer implements Comparable<DateContainer>, Cloneable {

    public static final TimeZone UTC = TimeZone.getTimeZone("UTC");

	public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String TIME_FORMAT = "HH:mm:ss";
    public static final String DATETIME_FORMAT = "yyyy-MM-dd' 'HH:mm:ss";

    public static final String FRIENDLY_AMPM_TIME_FORMAT = "K:mm a";
    public static final String FRIENDLY_FULL_DATE_FORMAT = "d MMM yyyy";
    public static final String FRIENDLY_MONTH_DATE_FORMAT = "d MMM";
    public static final String FRIENDLY_WEEK_DATE_FORMAT = "EEEE";

	private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat(DATE_FORMAT);
	private static final SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat(TIME_FORMAT);
	private static final SimpleDateFormat DATE_TIME_FORMATTER = new SimpleDateFormat(DATETIME_FORMAT);

    public static final int TYPE_DATE = 1;
    public static final int TYPE_TIME = 2;
    public static final int TYPE_DATETIME = 3;

	private int mType = TYPE_DATETIME;
	private Calendar mInputCalendar;

	public DateContainer() {
        DATE_FORMATTER.setTimeZone(UTC);
        TIME_FORMATTER.setTimeZone(UTC);
        DATE_TIME_FORMATTER.setTimeZone(UTC);
        mInputCalendar = Calendar.getInstance(UTC);
	}

    public DateContainer(int mType) {
        this();
        this.mType = mType;
    }

    public DateContainer(DateContainer dateContainer) {
        this(dateContainer.getType());
        mInputCalendar.setTimeZone(dateContainer.mInputCalendar.getTimeZone());
        mInputCalendar.setTimeInMillis(dateContainer.mInputCalendar.getTimeInMillis());
    }

    public DateContainer(int mType, String dateTimeStr) throws ParseException {
        this(mType);
        setFromString(dateTimeStr);
    }

	public DateContainer(int mType, Calendar mCalendar) {
        this(mType);
		this.mInputCalendar = mCalendar;
	}

	public void setFromString(String dateTimeStr) throws ParseException {
        mInputCalendar.setTime(DATE_TIME_FORMATTER.parse(dateTimeStr));
	}

	@Override
	public String toString() {
		return getAsString(UTC);
	}

	public synchronized String getAsString(TimeZone timeZone) {
        String dateTimeStr = null;
		switch (mType) {
		case TYPE_DATE:
            DATE_FORMATTER.setTimeZone(timeZone);
            dateTimeStr = DATE_FORMATTER.format(mInputCalendar.getTime());
            DATE_FORMATTER.setTimeZone(UTC);
			break;
		case TYPE_DATETIME:
            DATE_TIME_FORMATTER.setTimeZone(timeZone);
            dateTimeStr = DATE_TIME_FORMATTER.format(mInputCalendar.getTime());
            DATE_TIME_FORMATTER.setTimeZone(UTC);
			break;
		case TYPE_TIME:
            TIME_FORMATTER.setTimeZone(timeZone);
            dateTimeStr = TIME_FORMATTER.format(mInputCalendar.getTime());
            TIME_FORMATTER.setTimeZone(UTC);
			break;
		}
        return dateTimeStr;
	}

	public String getAsString(String customPattern, TimeZone timeZone) {
        if(customPattern == null) {
            return getAsString(timeZone);
        }
        SimpleDateFormat formatter = new SimpleDateFormat(customPattern);
        formatter.setTimeZone(timeZone);
        return formatter.format(mInputCalendar.getTime());
	}

    public Calendar getCalendar() {
        return mInputCalendar;
    }

    public void setCalendar(Calendar mCalendar) {
        this.mInputCalendar = mCalendar;
    }

    public int getType() {
        return mType;
    }

    public void setType(int mType) {
        this.mType = mType;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return new DateContainer(this);
    }

    @Override
    public boolean equals(Object o) {
        return equals(o, UTC);
    }

    public boolean equals(Object o, TimeZone timeZone) {
        if(o instanceof DateContainer) {
            return compareTo((DateContainer) o, timeZone) == 0;
        }

        if(o instanceof Calendar) {
            DateContainer tmpDateContainer = new DateContainer(mType, (Calendar) o);
            return compareTo(tmpDateContainer) == 0;
        }

        return super.equals(o);
    }

    @Override
    public int compareTo(@NonNull DateContainer dateContainer) {
        return compareTo(dateContainer, UTC);
    }

    public int compareTo(@NonNull DateContainer dateContainer, TimeZone timeZone) {
        int originalType = dateContainer.getType();
        dateContainer.setType(mType);
        String dateString = dateContainer.getAsString(timeZone);
        String thisDateString = getAsString(timeZone);
        dateContainer.setType(originalType);
        return thisDateString.compareTo(dateString);
    }

    /**
     * Zero the time part of the timestamp only (hours, minutes, seconds and ms)
     */
    public void zeroTime() {
        mInputCalendar.set(Calendar.HOUR_OF_DAY, 0);
        mInputCalendar.set(Calendar.MINUTE, 0);
        mInputCalendar.set(Calendar.SECOND, 0);
        mInputCalendar.set(Calendar.MILLISECOND, 0);
    }

    /**
     * Get the amount of ms since 00:00 for the current time.
     * @return the ms
     */
    public long getMillisOfDay() {
        return mInputCalendar.get(Calendar.MILLISECOND) + (((long) mInputCalendar.get(Calendar.SECOND)
                + ((long) mInputCalendar.get(Calendar.MINUTE) * 60l)
                + ((long) mInputCalendar.get(Calendar.HOUR_OF_DAY) * 3600l)) * 1000l);
	}

    /**
     * Get current timestamp in the "yyyy-MM-dd HH-mm-ss" format.
     * @return the current timestamp
     */
    public static String getCurrentUTCTimestamp() {
        return DATE_TIME_FORMATTER.format(Calendar.getInstance(UTC).getTime());
    }

    /**
     * Parse timestamps into a date object. The timestamp format must be "yyyy-MM-dd HH-mm-ss".
     * @param ts the timestamp string
     * @return the timestamp date instance
     * @throws ParseException
     */
    public static Calendar parseUTCTimestamp(String ts) throws ParseException {
        Calendar calendar = Calendar.getInstance(UTC);
        calendar.setTime(DATE_TIME_FORMATTER.parse(ts));
        return calendar;
    }

    /**
     * Same as getDateAsStringRelativeTo(context, date, todayDate)
     * See {@link #getDateAsStringRelativeTo(Context, DateContainer, DateContainer, TimeZone)}
     * @param context the app context to get friendly label strings
     * @param date the date
     * @return the friendly label
     */
    public static String getRelativeLabelToToday(Context context, DateContainer date, TimeZone timeZone) {
        return getDateAsStringRelativeTo(context, date, new DateContainer(), timeZone);
    }

    /**
     * Get a relative friendly label for the supplied date, relative to the relativeDate.
     * For instance:
     *
     * date = 2015-12-12, relativeToDate = 2015-12-12, the result = "Today"
     * date = 2015-12-11, relativeToDate = 2015-12-12, the result = "Yesterday"
     * date = 2015-12-09, relativeToDate = 2015-12-12, the result = "Wednesday"
     * date = 2015-12-01, relativeToDate = 2015-12-12, the result = "1 Dec"
     * date = 2015-11-11, relativeToDate = 2016-01-10, the result = "11 Nov 2015"
     *
     * @param context the app context to get friendly label strings
     * @param date the date
     * @param relativeToDate the relative to date
     * @return the friendly label
     */
    public static String getDateAsStringRelativeTo(Context context, DateContainer date, DateContainer relativeToDate, TimeZone timeZone) {
        Calendar tmpDate = (Calendar) relativeToDate.getCalendar().clone();
        tmpDate.setTimeZone(timeZone);

        Calendar refDate = (Calendar) date.getCalendar().clone();
        refDate.setTimeZone(timeZone);

        SimpleDateFormat friendlyFullDateFormat = new SimpleDateFormat(FRIENDLY_FULL_DATE_FORMAT);
        friendlyFullDateFormat.setTimeZone(timeZone);
        String label = friendlyFullDateFormat.format(refDate.getTime());

        boolean isSameDay = refDate.get(Calendar.DAY_OF_YEAR) == tmpDate.get(Calendar.DAY_OF_YEAR) &&
                refDate.get(Calendar.YEAR) == tmpDate.get(Calendar.YEAR);
        if(isSameDay) {
            label = context.getString(R.string.date_today);
        } else {
            tmpDate.add(Calendar.HOUR_OF_DAY, -24);
            isSameDay = refDate.get(Calendar.DAY_OF_YEAR) == tmpDate.get(Calendar.DAY_OF_YEAR) &&
                    refDate.get(Calendar.YEAR) == tmpDate.get(Calendar.YEAR);
            if(isSameDay) {
                label = context.getString(R.string.date_yesterday);
            } else {
                /*tmpDate.add(Calendar.HOUR_OF_DAY, -144); // go 1 week back
                if(refDate.compareTo(tmpDate) < 0) {*/
                    refDate.add(Calendar.HOUR_OF_DAY, 24);
                    if(refDate.get(Calendar.YEAR) == tmpDate.get(Calendar.YEAR)) {
                        SimpleDateFormat friendlyMonthDateFormat = new SimpleDateFormat(FRIENDLY_MONTH_DATE_FORMAT);
                        friendlyMonthDateFormat.setTimeZone(timeZone);
                        label = friendlyMonthDateFormat.format(refDate.getTime());
                    }
                /*} else {
                    SimpleDateFormat friendlyWeekDateFormat = new SimpleDateFormat(FRIENDLY_WEEK_DATE_FORMAT);
                    friendlyWeekDateFormat.setTimeZone(timeZone);
                    label = friendlyWeekDateFormat.format(refDate.getTime());
                }*/
            }
        }

        return label;
    }
}
