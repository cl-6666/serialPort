package com.cl.myapplication.util;

import java.text.SimpleDateFormat;
import java.util.Date;


public class TimeUtil {

    public static final SimpleDateFormat DEFAULT_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public static String currentTime() {
        Date date = new Date();
        return DEFAULT_FORMAT.format(date);
    }
}
