package dev.rubasace.linkedin.games.ldrbot.util;

import dev.rubasace.linkedin.games.ldrbot.user.UserInfo;

import java.time.Duration;
import java.time.LocalDate;

public class FormatUtils {

    public static String formatUserMention(final UserInfo userInfo) {
        if (userInfo.userName() != null) {
            return "@" + userInfo.userName();
        } else {
            String fullName = userInfo.firstName();
            if (userInfo.lastName() != null) {
                fullName += " " + userInfo.lastName();
            }
            return "<a href=\"tg://user?id=" + userInfo.id() + "\">" + fullName + "</a>";
        }
    }

    public static String formatDuration(Duration d) {
        long minutes = d.toMinutes();
        long seconds = d.minusMinutes(minutes).getSeconds();
        return String.format("%02d:%02d", minutes, seconds);
    }

    public static String formatDate(final LocalDate localDate) {
        return getDayOfWeek(localDate) + " " + localDate.getDayOfMonth() + " " + getMonth(localDate) + " " + localDate.getYear();
    }

    private static String getMonth(final LocalDate localDate) {
        return switch (localDate.getMonth()) {
            case JANUARY -> "Jan";
            case FEBRUARY -> "Feb";
            case MARCH -> "Mar";
            case APRIL -> "Apr";
            case MAY -> "May";
            case JUNE -> "Jun";
            case JULY -> "Jul";
            case AUGUST -> "Aug";
            case SEPTEMBER -> "Sep";
            case OCTOBER -> "Oct";
            case NOVEMBER -> "Nov";
            case DECEMBER -> "Dec";
        };
    }

    private static String getDayOfWeek(final LocalDate localDate) {
        return switch (localDate.getDayOfWeek()) {
            case MONDAY -> "Mon";
            case TUESDAY -> "Tue";
            case WEDNESDAY -> "Wed";
            case THURSDAY -> "Thu";
            case FRIDAY -> "Fri";
            case SATURDAY -> "Sat";
            case SUNDAY -> "Sun";
        };
    }
}
