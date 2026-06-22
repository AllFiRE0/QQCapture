package com.qqcapture.utils;

public class TimerUtils {
    
    public static String formatTime(long millis, String format) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        seconds %= 60;
        minutes %= 60;
        
        switch (format) {
            case "HH:mm:ss":
                return String.format("%02d:%02d:%02d", hours, minutes, seconds);
            case "mm:ss":
                return String.format("%02d:%02d", minutes, seconds);
            case "HH:mm":
                return String.format("%02d:%02d", hours, minutes);
            case "ss":
                return String.format("%02d", seconds);
            default:
                return String.format("%02d:%02d", minutes, seconds);
        }
    }
    
    public static String formatTime(long millis) {
        return formatTime(millis, "mm:ss");
    }
    
    public static long ticksToMillis(long ticks) {
        return ticks * 50;
    }
    
    public static long millisToTicks(long millis) {
        return millis / 50;
    }
    
    public static int secondsToTicks(int seconds) {
        return seconds * 20;
    }
    
    public static int ticksToSeconds(int ticks) {
        return ticks / 20;
    }
}
