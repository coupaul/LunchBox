package org.apache.commons.lang3.time;

import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public interface DateParser {

    Date parse(String s) throws ParseException;

    Date parse(String s, ParsePosition parseposition);

    String getPattern();

    TimeZone getTimeZone();

    Locale getLocale();

    Object parseObject(String s) throws ParseException;

    Object parseObject(String s, ParsePosition parseposition);
}