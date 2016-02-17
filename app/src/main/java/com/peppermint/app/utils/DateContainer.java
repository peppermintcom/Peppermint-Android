package com.peppermint.app.utils;

import android.content.Context;

import com.peppermint.app.R;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.text.ParseException;

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

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormat.forPattern(DATE_FORMAT);
	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormat.forPattern(TIME_FORMAT);
	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern(DATETIME_FORMAT);

    public static final int TYPE_DATE = 1;
    public static final int TYPE_TIME = 2;
    public static final int TYPE_DATETIME = 3;

	private int mType = TYPE_DATE;
	private DateTime mInputCalendar;

	public DateContainer() {
        mInputCalendar = new DateTime(DateTimeZone.UTC);
	}

    public DateContainer(int mType) {
        this();
        this.mType = mType;
    }

    public DateContainer(DateContainer dateContainer) {
        mType = dateContainer.getType();
        mInputCalendar = new DateTime(dateContainer.mInputCalendar);
    }

    public DateContainer(int mType, String dateTimeStr) throws ParseException {
        this(mType);
        setFromString(dateTimeStr);
    }

	public DateContainer(int mType, DateTime mDateTime) {
        this.mType = mType;
		this.mInputCalendar = mDateTime;
	}

	public void setFromString(String dateTimeStr) throws ParseException {
        switch (mType) {
        case TYPE_DATE:
            mInputCalendar = DateTime.parse(dateTimeStr, DATE_FORMATTER);
            break;
        case TYPE_DATETIME:
            mInputCalendar = DateTime.parse(dateTimeStr, DATE_TIME_FORMATTER);
            break;
        case TYPE_TIME:
            mInputCalendar = DateTime.parse(dateTimeStr, TIME_FORMATTER);
            break;
        }
	}

	@Override
	public String toString() {
		return getAsString(DateTimeZone.UTC);
	}

	public String getAsString(DateTimeZone timeZone) {
        String dateTimeStr = null;
		switch (mType) {
		case TYPE_DATE:
            dateTimeStr = mInputCalendar.withZone(timeZone).toString(DATE_FORMATTER);
			break;
		case TYPE_DATETIME:
            dateTimeStr = mInputCalendar.withZone(timeZone).toString(DATE_TIME_FORMATTER);
			break;
		case TYPE_TIME:
            dateTimeStr = mInputCalendar.withZone(timeZone).toString(TIME_FORMATTER);
			break;
		}
        return dateTimeStr;
	}

	public String getAsString(String customPattern, DateTimeZone timeZone) {
        if(customPattern == null) {
            return getAsString(timeZone);
        }
        DateTimeFormatter formatter = DateTimeFormat.forPattern(customPattern);
		return mInputCalendar.withZone(timeZone).toString(formatter);
	}

    public DateTime getDateTime() {
        return mInputCalendar;
    }

    public void setDateTime(DateTime mDateTime) {
        this.mInputCalendar = mDateTime;
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
        DateTime dateTime = null;

        if(o instanceof DateContainer) {
            dateTime = ((DateContainer) o).getDateTime();
        } else if(o instanceof DateTime) {
            dateTime = (DateTime) o;
        }

        if(dateTime != null) {
            return mInputCalendar.compareTo(dateTime) == 0;
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
                return tmpThisDateContainer.getDateTime().compareTo(tmpOtherDateContainer.getDateTime());
            case TYPE_DATETIME:
                return mInputCalendar.compareTo(dateContainer.getDateTime());
        }

        throw new IllegalArgumentException("Invalid date type " + mType);
    }

    /**
     * Zero the time part of the timestamp only (hours, minutes, seconds and ms)
     */
    public void zeroTime() {
        mInputCalendar = mInputCalendar.millisOfDay().setCopy(0);
    }

    /**
     * Get the amount of ms since 00:00 for the current time.
     * @return the ms
     */
    public long getMillisOfDay() {
        return mInputCalendar.getMillisOfDay();
	}

    /**
     * Get current timestamp in the "yyyy-MM-dd HH-mm-ss" format.
     * @return the current timestamp
     */
    public static String getCurrentUTCTimestamp() {
        return (new DateTime(DateTimeZone.UTC)).toString(DATE_TIME_FORMATTER);
    }

    /**
     * Parse timestamps into a date object. The timestamp format must be "yyyy-MM-dd HH-mm-ss".
     * @param ts the timestamp string
     * @return the timestamp date instance
     * @throws ParseException
     */
    public static DateTime parseUTCTimestamp(String ts) throws ParseException {
        return DateTime.parse(ts, DATE_TIME_FORMATTER);
    }

    /**
     * Same as getDateAsStringRelativeTo(context, date, todayDate)
     * See {@link #getDateAsStringRelativeTo(Context, DateContainer, DateContainer, DateTimeZone)}
     * @param context the app context to get friendly label strings
     * @param date the date
     * @return the friendly label
     */
    public static String getRelativeLabelToToday(Context context, DateContainer date, DateTimeZone timeZone) {
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
    public static String getDateAsStringRelativeTo(Context context, DateContainer date, DateContainer relativeToDate, DateTimeZone timeZone) {
        int oldDateType = date.getType();
        date.setType(TYPE_DATE);
        DateTime tmpDate = new DateTime(relativeToDate.getDateTime());

        String label = date.getAsString(FRIENDLY_FULL_DATE_FORMAT, timeZone);

        boolean isSameDay = date.getDateTime().getDayOfYear() == tmpDate.getDayOfYear() && date.getDateTime().getYear() == tmpDate.getYear();
        if(isSameDay) {
            label = context.getString(R.string.date_today);
        } else {
            tmpDate = tmpDate.hourOfDay().addToCopy(-24);
            isSameDay = date.getDateTime().getDayOfYear() == tmpDate.getDayOfYear() && date.getDateTime().getYear() == tmpDate.getYear();
            if(isSameDay) {
                label = context.getString(R.string.date_yesterday);
            } else {
                tmpDate = tmpDate.hourOfDay().addToCopy(-144); // go 1 week back
                if(date.getDateTime().compareTo(tmpDate) < 0) {
                    if(date.getDateTime().getYear() == tmpDate.getYear()) {
                        label = date.getAsString(FRIENDLY_MONTH_DATE_FORMAT, timeZone);
                    }
                } else {
                    label = date.getAsString(FRIENDLY_WEEK_DATE_FORMAT, timeZone);
                }
            }
        }

        date.setType(oldDateType);
        return label;
    }
}
