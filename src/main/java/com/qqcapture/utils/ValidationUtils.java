package com.qqcapture.utils;

import org.bukkit.Location;

public class ValidationUtils {
    
    public static boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    public static boolean isInteger(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    public static boolean isBoolean(String str) {
        return str != null && (str.equalsIgnoreCase("true") || str.equalsIgnoreCase("false"));
    }
    
    public static boolean isLocationValid(Location loc) {
        return loc != null && loc.getWorld() != null;
    }
    
    public static boolean isInBounds(Location loc, Location min, Location max) {
        if (!isLocationValid(loc) || !isLocationValid(min) || !isLocationValid(max)) {
            return false;
        }
        
        double minX = Math.min(min.getX(), max.getX());
        double maxX = Math.max(min.getX(), max.getX());
        double minY = Math.min(min.getY(), max.getY());
        double maxY = Math.max(min.getY(), max.getY());
        double minZ = Math.min(min.getZ(), max.getZ());
        double maxZ = Math.max(min.getZ(), max.getZ());
        
        return loc.getX() >= minX && loc.getX() <= maxX &&
               loc.getY() >= minY && loc.getY() <= maxY &&
               loc.getZ() >= minZ && loc.getZ() <= maxZ;
    }
    
    public static boolean isIntegerInRange(int value, int min, int max) {
        return value >= min && value <= max;
    }
    
    public static boolean isDoubleInRange(double value, double min, double max) {
        return value >= min && value <= max;
    }
    
    public static boolean isStringEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    public static boolean isStringNotEmpty(String str) {
        return !isStringEmpty(str);
    }
    
    public static boolean isValidColor(String color) {
        if (isStringEmpty(color)) return false;
        try {
            org.bukkit.ChatColor.valueOf(color.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    public static boolean isValidSound(String sound) {
        if (isStringEmpty(sound)) return false;
        try {
            org.bukkit.Sound.valueOf(sound.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
