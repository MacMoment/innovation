package com.staffsystem.plugin.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeUtil {

    private static final Pattern TIME_PATTERN = Pattern.compile(
        "(?:([0-9]+)y)?(?:([0-9]+)mo)?(?:([0-9]+)w)?(?:([0-9]+)d)?(?:([0-9]+)h)?(?:([0-9]+)m)?(?:([0-9]+)s)?",
        Pattern.CASE_INSENSITIVE
    );

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    // Time constants in milliseconds
    private static final long SECOND = 1000L;
    private static final long MINUTE = 60 * SECOND;
    private static final long HOUR = 60 * MINUTE;
    private static final long DAY = 24 * HOUR;
    private static final long WEEK = 7 * DAY;
    private static final long MONTH = 30 * DAY;
    private static final long YEAR = 365 * DAY;

    /**
     * Parse a duration string like "1d", "2h30m", "7d12h" into milliseconds
     * 
     * @param input Duration string
     * @return Duration in milliseconds, or -1 if invalid
     */
    public static long parseDuration(String input) {
        if (input == null || input.isEmpty()) {
            return -1;
        }

        // Handle special cases
        if (input.equalsIgnoreCase("permanent") || input.equalsIgnoreCase("perm")) {
            return -1;
        }

        Matcher matcher = TIME_PATTERN.matcher(input);
        if (!matcher.matches()) {
            return -1;
        }

        long duration = 0;

        String years = matcher.group(1);
        String months = matcher.group(2);
        String weeks = matcher.group(3);
        String days = matcher.group(4);
        String hours = matcher.group(5);
        String minutes = matcher.group(6);
        String seconds = matcher.group(7);

        if (years != null) {
            duration += Long.parseLong(years) * YEAR;
        }
        if (months != null) {
            duration += Long.parseLong(months) * MONTH;
        }
        if (weeks != null) {
            duration += Long.parseLong(weeks) * WEEK;
        }
        if (days != null) {
            duration += Long.parseLong(days) * DAY;
        }
        if (hours != null) {
            duration += Long.parseLong(hours) * HOUR;
        }
        if (minutes != null) {
            duration += Long.parseLong(minutes) * MINUTE;
        }
        if (seconds != null) {
            duration += Long.parseLong(seconds) * SECOND;
        }

        return duration > 0 ? duration : -1;
    }

    /**
     * Format a duration in milliseconds to a human-readable string
     * 
     * @param milliseconds Duration in milliseconds
     * @return Formatted string like "7 days, 12 hours"
     */
    public static String formatDuration(long milliseconds) {
        if (milliseconds < 0) {
            return "Permanent";
        }
        
        if (milliseconds == 0) {
            return "Instant";
        }

        StringBuilder sb = new StringBuilder();

        long years = milliseconds / YEAR;
        milliseconds %= YEAR;
        
        long months = milliseconds / MONTH;
        milliseconds %= MONTH;
        
        long weeks = milliseconds / WEEK;
        milliseconds %= WEEK;
        
        long days = milliseconds / DAY;
        milliseconds %= DAY;
        
        long hours = milliseconds / HOUR;
        milliseconds %= HOUR;
        
        long minutes = milliseconds / MINUTE;
        milliseconds %= MINUTE;
        
        long seconds = milliseconds / SECOND;

        if (years > 0) {
            sb.append(years).append(years == 1 ? " year" : " years");
        }
        if (months > 0) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(months).append(months == 1 ? " month" : " months");
        }
        if (weeks > 0) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(weeks).append(weeks == 1 ? " week" : " weeks");
        }
        if (days > 0) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(days).append(days == 1 ? " day" : " days");
        }
        if (hours > 0) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(hours).append(hours == 1 ? " hour" : " hours");
        }
        if (minutes > 0 && years == 0 && months == 0 && weeks == 0) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(minutes).append(minutes == 1 ? " minute" : " minutes");
        }
        if (seconds > 0 && years == 0 && months == 0 && weeks == 0 && days == 0) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(seconds).append(seconds == 1 ? " second" : " seconds");
        }

        return sb.isEmpty() ? "Less than a second" : sb.toString();
    }

    /**
     * Format a duration in milliseconds to a short string
     * 
     * @param milliseconds Duration in milliseconds
     * @return Short formatted string like "7d 12h"
     */
    public static String formatDurationShort(long milliseconds) {
        if (milliseconds < 0) {
            return "Perm";
        }
        
        if (milliseconds == 0) {
            return "0s";
        }

        StringBuilder sb = new StringBuilder();

        long days = milliseconds / DAY;
        milliseconds %= DAY;
        
        long hours = milliseconds / HOUR;
        milliseconds %= HOUR;
        
        long minutes = milliseconds / MINUTE;
        milliseconds %= MINUTE;
        
        long seconds = milliseconds / SECOND;

        if (days > 0) {
            sb.append(days).append("d ");
        }
        if (hours > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0 && days == 0) {
            sb.append(minutes).append("m ");
        }
        if (seconds > 0 && days == 0 && hours == 0) {
            sb.append(seconds).append("s");
        }

        return sb.toString().trim();
    }

    /**
     * Format a timestamp to a date string
     * 
     * @param timestamp Unix timestamp in milliseconds
     * @return Formatted date string
     */
    public static String formatDate(long timestamp) {
        if (timestamp < 0) {
            return "Never";
        }
        return DATE_FORMAT.format(new Date(timestamp));
    }

    /**
     * Get current timestamp
     * 
     * @return Current time in milliseconds
     */
    public static long now() {
        return System.currentTimeMillis();
    }
}
