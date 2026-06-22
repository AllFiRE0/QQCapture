package com.qqcapture.managers;

import com.qqcapture.QQCapture;
import com.qqcapture.models.Template;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class ConditionManager {
    private final QQCapture plugin;
    
    public ConditionManager(QQCapture plugin) {
        this.plugin = plugin;
    }
    
    public boolean checkPlayerConditions(Player player, Template template) {
        String condition = template.getCondition();
        
        // No condition
        if (condition == null || condition.isEmpty()) {
            return true;
        }
        
        // Parse and check condition
        return evaluateCondition(player, condition);
    }
    
    public boolean checkAllPlayersConditions(List<Player> players, Template template) {
        String condition = template.getAllPlayersCondition();
        
        // No condition
        if (condition == null || condition.isEmpty()) {
            return true;
        }
        
        // Check if condition applies to all players
        for (Player player : players) {
            if (!evaluateCondition(player, condition)) {
                return false;
            }
        }
        return true;
    }
    
    public boolean checkRules(Player player, Template template, List<Player> allPlayers) {
        Map<String, Map<String, String>> rules = template.getRules();
        if (rules == null || rules.isEmpty()) {
            return true;
        }
        
        for (Map<String, String> rule : rules.values()) {
            String type = rule.get("type");
            String value = rule.get("value");
            
            if (type == null || value == null) {
                continue;
            }
            
            // Handle target: and other: prefixes
            if (type.startsWith("target:")) {
                String targetType = type.substring(7);
                if (!checkSinglePlayerCondition(player, targetType, value)) {
                    return false;
                }
            } else if (type.startsWith("other:")) {
                String otherType = type.substring(6);
                if (!checkAllPlayersCondition(allPlayers, otherType, value)) {
                    return false;
                }
            } else {
                // Regular condition
                if (!checkSinglePlayerCondition(player, type, value)) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    private boolean evaluateCondition(Player player, String condition) {
        // Handle AND operator
        if (condition.contains("&&")) {
            String[] parts = condition.split("&&");
            for (String part : parts) {
                if (!evaluateSingleCondition(player, part.trim())) {
                    return false;
                }
            }
            return true;
        }
        
        // Handle OR operator
        if (condition.contains("~~")) {
            String[] parts = condition.split("~~");
            for (String part : parts) {
                if (evaluateSingleCondition(player, part.trim())) {
                    return true;
                }
            }
            return false;
        }
        
        // Single condition
        return evaluateSingleCondition(player, condition);
    }
    
    private boolean evaluateSingleCondition(Player player, String condition) {
        // Parse condition
        String[] operators = {">=", "<=", "==", "!=", ">", "<", "!~", "!-", "-!", "~"};
        
        for (String operator : operators) {
            if (condition.contains(operator)) {
                String[] parts = condition.split(Pattern.quote(operator), 2);
                if (parts.length == 2) {
                    String left = parts[0].trim();
                    String right = parts[1].trim();
                    
                    // Replace placeholders
                    left = plugin.getPlaceholderManager().parsePlaceholders(player, left);
                    right = plugin.getPlaceholderManager().parsePlaceholders(player, right);
                    
                    return compareValues(left, right, operator);
                }
            }
        }
        
        return false;
    }
    
    private boolean compareValues(String left, String right, String operator) {
        // Try numeric comparison
        try {
            double leftNum = Double.parseDouble(left);
            double rightNum = Double.parseDouble(right);
            
            switch (operator) {
                case ">=": return leftNum >= rightNum;
                case "<=": return leftNum <= rightNum;
                case ">": return leftNum > rightNum;
                case "<": return leftNum < rightNum;
                case "==": return leftNum == rightNum;
                case "!=": return leftNum != rightNum;
            }
        } catch (NumberFormatException e) {
            // String comparison
            switch (operator) {
                case "==": return left.equals(right);
                case "!=": return !left.equals(right);
                case "!~": return left.contains(right);
                case "!-": return left.startsWith(right);
                case "-!": return left.endsWith(right);
                case "~": return left.contains(right);
            }
        }
        
        return false;
    }
    
    private boolean checkSinglePlayerCondition(Player player, String type, String value) {
        String playerValue = plugin.getPlaceholderManager().parsePlaceholders(player, type);
        return evaluateSingleCondition(player, playerValue + "==" + value);
    }
    
    private boolean checkAllPlayersCondition(List<Player> players, String type, String value) {
        for (Player player : players) {
            String playerValue = plugin.getPlaceholderManager().parsePlaceholders(player, type);
            if (!evaluateSingleCondition(player, playerValue + "==" + value)) {
                return false;
            }
        }
        return true;
    }
}
