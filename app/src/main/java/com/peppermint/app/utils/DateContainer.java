package com.peppermint.app.utils;

import android.content.Context;

import com.peppermint.app.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Nuno Luz on 14-09-2015.
 *
 * Calendar wrapper with formatting and manipulation methods.
 */
public class DateContainer implements Comparable<DateContainer>, Cloneable {

	public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String TIME_FORMAT = "HH:mm:ss";
    public static final String DATETIME_FORMAT = "yyyy-MM-dd' 'HH:mm:ss";

    public static final String FRIENDLY_AMPM_TIME_FORMAT = "K:mm a";
    public static final String FRIENDLY_FULL_DATE_FORMAT = "d MMM yyyy";
    public static final String FRIENDLY_MONTH_DATE_FORMAT = "d MMM";
    public static final String FRIENDLY_WEEK_DATE_FORMAT = "EEEE";

	private static final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
	private static final SimpleDateFormat timeFormat = new SimpleDateFormat(TIME_FORMAT);
	private static final SimpleDateFormat dateTimeFormat = new SimpleDateFormat(DATETIME_FORMAT);

    public static final int TYPE_DATE = 1;
    public static final int TYPE_TIME = 2;
    public static final int TYPE_DATETIME = 3;

	private int mType = TYPE_DATE;
	private Calendar mCalendar = Calendar.getInstance();

	public DateContainer() {
	}

    public DateContainer(int mType) {
        this.mType = mType;
    }

    public DateContainer(DateContainer dateContainer) {
        mType = dateContainer.getType();
        mCalendar = (Calendar) dateContainer.getCalendar().clone();
    }

    public DateContainer(int mType, String dateTimeStr) throws ParseException {
        this(mType);
        setFromString(dateTimeStr);
    }

	public DateContainer(int mType, Calendar mCalendar) {
        this.mType = mType;
		this.mCalendar = mCalendar;
	}

	public void setFromString(String dateTimeStr) throws ParseException {
        switch (mType) {
        case TYPE_DATE:
            mCalendar.setTime(dateFormat.parse(dateTimeStr));
            break;
        case TYPE_DATETIME:
            mCalendar.setTime(dateTimeFormat.parse(dateTimeStr));
            break;
        case TYPE_TIME:
            mCalendar.setTime(timeFormat.parse(dateTimeStr));
            break;
        }
	}

	@Override
	public String toString() {
		return getAsString();
	}

	public String getAsString() {
        String dateTimeStr = null;
		switch (mType) {
		case TYPE_DATE:
            dateTimeStr = dateFormat.format(mCalendar.getTime());
			break;
		case TYPE_DATETIME:
            dateTimeStr = dateTimeFormat.format(mCalendar.getTime());
			break;
		case TYPE_TIME:
            dateTimeStr = timeFormat.format(mCalendar.getTime());
			break;
		}
        return dateTimeStr;
	}

	public String getAsString(String customPattern) {
        if(customPattern == null) {
            return getAsString();
        }

		SimpleDateFormat f = new SimpleDateFormat(customPattern);
		return f.format(mCalendar.getTime());
	}

    public Calendar getCalendar() {
        return mCalendar;
    }

    public void setCalendar(Calendar mCalendar) {
        this.mCalendar = mCalendar;
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
        DateContainer dateContainer = null;

        if(o instanceof DateContainer) {
            dateContainer = (DateContainer) o;
        } else if(o instanceof Calendar) {
            dateContainer = new DateContainer(TYPE_DATETIME, (Calendar) o);
        }

        if(dateContainer != null) {
            return compareTo(dateContainer) == 0;
        }

        return super.equals(o);
    }

    @Override
    public int compareTo(DateContainer dateContainer) {
        switch (mType) {
            case TYPE_TIME:
                return (int) (getMillisOfDay() - dateContainer.getMillisOfDay());
            case TYPE_DATE:
                DateContainer tmpOtherDateContainer = new DateContainer(dateContainer);
                tmpOtherDateContainer.zeroTime();
                DateContainer tmpThisDateContainer = new DateContainer(this);
                tmpThisDateContainer.zeroTime();
                return tmpThisDateContainer.getCalendar().compareTo(tmpOtherDateContainer.getCalendar());
            case TYPE_DATETIME:
                return mCalendar.compareTo(dateContainer.getCalendar());
        }

        throw new IllegalArgumentException("Invalid date type " + mType);
    }

    /**
     * Zero the time part of the timestamp only (hours, minutes, seconds and ms)
     */
    public void zeroTime() {
        mCalendar.set(Calendar.MILLISECOND, 0);
        mCalendar.set(Calendar.SECOND, 0);
        mCalendar.set(Calendar.MINUTE, 0);
        mCalendar.set(Calendar.HOUR_OF_DAY, 0);
    }

    /**
     * Get the amount of ms since 00:00 for the current time.
     * @return the ms
     */
    public long getMillisOfDay() {
		return mCalendar.get(Calendar.MILLISECOND) + (((long) mCalendar.get(Calendar.SECOND)
				+ ((long) mCalendar.get(Calendar.MINUTE) * 60l)
				+ ((long) mCalendar.get(Calendar.HOUR_OF_DAY) * 3600l)) * 1000l);
	}

    /**
     * Get current timestamp in the "yyyy-MM-dd HH-mm-ss" format.
     * @return the current timestamp
     */
    public static String getCurrentTimestamp() {
        return dateTimeFormat.format(Calendar.getInstance().getTime());
    }

    /**
     * Parse timestamps into a date object. The timestamp format must be "yyyy-MM-dd HH-mm-ss".
     * @param ts the timestamp string
     * @return the timestamp date instance
     * @throws ParseException
     */
    public static Date parseTimestamp(String ts) throws ParseException {
        return dateTimeFormat.parse(ts);
    }

    /**
     * Same as getDateAsStringRelativeTo(context, date, todayDate)
     * See {@link #getDateAsStringRelativeTo(Context, DateContainer, DateContainer)}
     * @param context the app context to get friendly label strings
     * @param date the date
     * @return the friendly label
     */
    public static String getRelativeLabelToToday(Context context, DateContainer date) {
        return getDateAsStringRelativeTo(context, date, new DateContainer());
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
    public static String getDateAsStringRelativeTo(Context context, DateContainer date, DateContainer relativeToDate) {
        int oldDateType = date.getType();
        date.setType(TYPE_DATE);
        DateContainer tmpDate = new DateContainer(TYPE_DATE, (Calendar) relativeToDate.getCalendar().clone());

        String label = date.getAsString(FRIENDLY_FULL_DATE_FORMAT);

        if(date.equals(tmpDate)) {
            label = context.getString(R.string.date_today);
        } else {
            tmpDate.getCalendar().add(Calendar.HOUR, -24);
            if(date.equals(tmpDate)) {
                label = context.getString(R.string.date_yesterday);
            } else {
                tmpDate.getCalendar().add(Calendar.HOUR, -144); // go 1 week back
                if(date.compareTo(tmpDate) < 0) {
                    if(date.getCalendar().get(Calendar.YEAR) == tmpDate.getCalendar().get(Calendar.YEAR)) {
                        label = date.getAsString(FRIENDLY_MONTH_DATE_FORMAT);
                    }
                } else {
                    label = date.getAsString(FRIENDLY_WEEK_DATE_FORMAT);
                }
            }
        }

        date.setType(oldDateType);
        return label;
    }
}
