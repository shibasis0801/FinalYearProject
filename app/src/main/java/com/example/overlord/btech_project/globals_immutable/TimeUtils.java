package com.example.overlord.btech_project.globals_immutable;

import com.google.firebase.database.Exclude;

import java.util.Calendar;

/**
 * Created by overlord on 15/4/18.
 */

public class TimeUtils {
    @Exclude
    public static int getSecondsFromTimestamp(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp / ( 1000 * 1000 ));
        return calendar.get(Calendar.SECOND);
    }

}
