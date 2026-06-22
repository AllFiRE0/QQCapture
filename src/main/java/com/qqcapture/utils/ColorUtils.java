package com.qqcapture.utils;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtils {
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern CMI_PATTERN = Pattern.compile("\\{#[A-Fa-f0-9]{6}\\}");
    private static final Pattern SQUARE_PATTERN = Pattern.compile("\\[#[A-Fa-f0-9]{6}\\]");
    private static final Pattern ANGLE_PATTERN = Pattern.compile("<#[A-Fa-f0-9]{6}>");
    private static final Pattern MINI_MESSAGE_PATTERN = Pattern.compile("color:#([A-Fa-f0-9]{6})");
    
    public static String colorize(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        
        // Replace MiniMessage tags (gradient, color, etc.)
        message = replaceMiniMessage(message);
        
        // Replace HEX colors with &# format
        Matcher hexMatcher = HEX_PATTERN.matcher(message);
        while (hexMatcher.find()) {
            String hex = hexMatcher.group(1);
            message = message.replace("&#" + hex, net.md_5.bungee.api.ChatColor.of("#" + hex).toString());
        }
        
        // Replace CMI format {#FF5555}
        Matcher cmiMatcher = CMI_PATTERN.matcher(message);
        while (cmiMatcher.find()) {
            String match = cmiMatcher.group();
            String hex = match.substring(2, 8);
            message = message.replace(match, net.md_5.bungee.api.ChatColor.of("#" + hex).toString());
        }
        
        // Replace square bracket format [#FF5555]
        Matcher squareMatcher = SQUARE_PATTERN.matcher(message);
        while (squareMatcher.find()) {
            String match = squareMatcher.group();
            String hex = match.substring(2, 8);
            message = message.replace(match, net.md_5.bungee.api.ChatColor.of("#" + hex).toString());
        }
        
        // Replace angle bracket format <#FF5555>
        Matcher angleMatcher = ANGLE_PATTERN.matcher(message);
        while (angleMatcher.find()) {
            String match = angleMatcher.group();
            String hex = match.substring(2, 8);
            message = message.replace(match, net.md_5.bungee.api.ChatColor.of("#" + hex).toString());
        }
        
        // Replace vanilla color codes
        message = ChatColor.translateAlternateColorCodes('&', message);
        
        // Replace RGB format &x&F&F&F&F&F&F
        message = replaceVanillaRGB(message);
        
        // Replace color names
        message = replaceColorNames(message);
        
        return message;
    }
    
    private static String replaceMiniMessage(String message) {
        // Replace gradient tags
        message = message.replaceAll("<gradient:#([A-Fa-f0-9]{6}):#([A-Fa-f0-9]{6})>", "&#$1");
        message = message.replaceAll("</gradient>", "");
        
        // Replace color tags
        Matcher colorMatcher = MINI_MESSAGE_PATTERN.matcher(message);
        while (colorMatcher.find()) {
            String hex = colorMatcher.group(1);
            message = message.replace("color:#" + hex, net.md_5.bungee.api.ChatColor.of("#" + hex).toString());
        }
        
        return message;
    }
    
    private static String replaceVanillaRGB(String message) {
        Pattern rgbPattern = Pattern.compile("&x(&[A-Fa-f0-9]){6}");
        Matcher rgbMatcher = rgbPattern.matcher(message);
        
        while (rgbMatcher.find()) {
            String match = rgbMatcher.group();
            String hex = match.replace("&x", "").replace("&", "");
            message = message.replace(match, net.md_5.bungee.api.ChatColor.of("#" + hex).toString());
        }
        
        return message;
    }
    
    private static String replaceColorNames(String message) {
        String[] colors = {
            "black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple",
            "gold", "gray", "dark_gray", "blue", "green", "aqua", "red", "light_purple",
            "yellow", "white"
        };
        
        for (String color : colors) {
            String nameColor = "#" + color;
            try {
                ChatColor chatColor = ChatColor.valueOf(color.toUpperCase());
                message = message.replace(nameColor, chatColor.toString());
            } catch (IllegalArgumentException e) {
                // Ignore invalid color names
            }
        }
        
        return message;
    }
}
