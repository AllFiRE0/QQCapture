package com.qqcapture.utils;

public class MathUtils {
    
    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
    
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
    
    public static long clamp(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }
    
    public static int percentage(int total, int part) {
        if (total == 0) {
            return 0;
        }
        return (int) ((double) part / total * 100);
    }
    
    public static double percentage(double total, double part) {
        if (total == 0) {
            return 0;
        }
        return (part / total) * 100;
    }
    
    public static int roundToInt(double value) {
        return (int) Math.round(value);
    }
    
    public static boolean isEven(int number) {
        return number % 2 == 0;
    }
    
    public static boolean isOdd(int number) {
        return number % 2 != 0;
    }
    
    public static int getRandomInt(int min, int max) {
        return (int) (Math.random() * (max - min + 1)) + min;
    }
    
    public static double getRandomDouble(double min, double max) {
        return Math.random() * (max - min) + min;
    }
}
