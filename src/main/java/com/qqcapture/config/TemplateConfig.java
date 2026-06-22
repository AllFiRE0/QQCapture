package com.qqcapture.config;

import com.qqcapture.QQCapture;
import com.qqcapture.models.Template;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class TemplateConfig {
    private final QQCapture plugin;
    private final Map<String, Template> templates;
    private final List<String> validationErrors;
    private final List<String> validationWarnings;
    private boolean debugMode;
    
    public TemplateConfig(QQCapture plugin) {
        this.plugin = plugin;
        this.templates = new LinkedHashMap<>();
        this.validationErrors = new ArrayList<>();
        this.validationWarnings = new ArrayList<>();
        this.debugMode = false;
    }
    
    public Map<String, Template> loadTemplates(FileConfiguration config) {
        templates.clear();
        validationErrors.clear();
        validationWarnings.clear();
        
        ConfigurationSection templatesSection = config.getConfigurationSection("templates");
        if (templatesSection == null) {
            validationErrors.add("No 'templates' section found in config.yml!");
            return templates;
        }
        
        for (String templateName : templatesSection.getKeys(false)) {
            try {
                ConfigurationSection templateSection = templatesSection.getConfigurationSection(templateName);
                if (templateSection != null) {
                    Template template = parseTemplate(templateName, templateSection);
                    if (template != null && validateTemplate(template)) {
                        templates.put(templateName.toLowerCase(), template);
                        if (debugMode) {
                            plugin.getLogger().info("✓ Loaded template: " + templateName);
                        }
                    }
                }
            } catch (Exception e) {
                validationErrors.add("Failed to load template '" + templateName + "': " + e.getMessage());
                plugin.getLogger().severe("Failed to load template: " + templateName);
                e.printStackTrace();
            }
        }
        
        if (!validationErrors.isEmpty()) {
            plugin.getLogger().warning("Template validation errors:");
            for (String error : validationErrors) {
                plugin.getLogger().warning("  ✗ " + error);
            }
        }
        
        if (!validationWarnings.isEmpty()) {
            plugin.getLogger().info("Template validation warnings:");
            for (String warning : validationWarnings) {
                plugin.getLogger().info("  ⚠ " + warning);
            }
        }
        
        plugin.getLogger().info("Loaded " + templates.size() + " templates successfully!");
        return templates;
    }
    
    public void setDebugMode(boolean debug) {
        this.debugMode = debug;
    }
    
    private Template parseTemplate(String name, ConfigurationSection section) {
        Template.Builder builder = new Template.Builder(name);
        
        // --- BossBar секция ---
        ConfigurationSection bossBarSection = section.getConfigurationSection("bossbar");
        if (bossBarSection != null) {
            // enabled - новый ключ
            boolean bossBarEnabled = bossBarSection.getBoolean("enabled", true);
            builder.bossBarEnabled(bossBarEnabled);
            
            String color = bossBarSection.getString("color", "GREEN");
            if (isValidBossBarColor(color)) {
                builder.bossBarColor(color);
            } else {
                validationWarnings.add("Template '" + name + "': Invalid bossbar color '" + color + "', using GREEN");
                builder.bossBarColor("GREEN");
            }
            
            // update-interval-ticks вместо обновлять-боссбар-каждые-N-тиков
            int updateInterval = bossBarSection.getInt("update-interval-ticks", 20);
            if (updateInterval < 1) {
                validationWarnings.add("Template '" + name + "': Bossbar update interval too low (" + updateInterval + "), using 1");
                updateInterval = 1;
            }
            builder.bossBarUpdateTicks(updateInterval);
            
            builder.startText(bossBarSection.getString("start-text", 
                "<gradient:#00FF00:#55FF55>Ивент начался!</gradient>"));
            builder.progressText(bossBarSection.getString("progress-text", 
                "<gradient:#FF0000:#FFAA00>Прогресс: %current%/%max%</gradient>"));
            builder.endText(bossBarSection.getString("end-text", 
                "<gradient:#FF5555:#FF0000>Ивент завершился!</gradient>"));
            
            int segments = bossBarSection.getInt("segments", 12);
            if (!isValidSegments(segments)) {
                validationWarnings.add("Template '" + name + "': Invalid segments '" + segments + "', using 12");
                segments = 12;
            }
            builder.segments(segments);
            
            int updateTicks = bossBarSection.getInt("update-ticks", 5);
            if (updateTicks < 1) {
                validationWarnings.add("Template '" + name + "': Update ticks too low (" + updateTicks + "), using 1");
                updateTicks = 1;
            }
            builder.updateTicks(updateTicks);
            
            builder.startDelay(Math.max(0, bossBarSection.getInt("start-delay", 3)));
            builder.endDelay(Math.max(0, bossBarSection.getInt("end-delay", 3)));
            
            String timerFormat = bossBarSection.getString("timer-format", "mm:ss");
            if (!isValidTimerFormat(timerFormat)) {
                validationWarnings.add("Template '" + name + "': Invalid timer format '" + timerFormat + "', using mm:ss");
                timerFormat = "mm:ss";
            }
            builder.timerFormat(timerFormat);
            
            // send-on-rejoin вместо отправлять-боссбар-когда-игрок-перезаходит-на-сервер
            boolean sendOnRejoin = bossBarSection.getBoolean("send-on-rejoin", true);
            builder.sendOnRejoin(sendOnRejoin);
            
            // text вместо текст-боссбара
            String bossBarText = bossBarSection.getString("text", 
                "Ивент: %current%/%max% (Участников: %players% Групп: %groups%)");
            builder.bossBarText(bossBarText);
        } else {
            // Default bossbar settings
            builder.bossBarEnabled(true)
                   .bossBarColor("GREEN")
                   .bossBarUpdateTicks(20)
                   .startText("<gradient:#00FF00:#55FF55>Ивент начался!</gradient>")
                   .progressText("<gradient:#FF0000:#FFAA00>Прогресс: %current%/%max%</gradient>")
                   .endText("<gradient:#FF5555:#FF0000>Ивент завершился!</gradient>")
                   .segments(12)
                   .updateTicks(5)
                   .startDelay(3)
                   .endDelay(3)
                   .timerFormat("mm:ss")
                   .sendOnRejoin(true)
                   .bossBarText("Ивент: %current%/%max% (Участников: %players% Групп: %groups%)");
        }
        
        // --- Conditions ---
        String condition = section.getString("condition", "");
        builder.condition(condition);
        
        String allPlayersCondition = section.getString("all-players-condition", "");
        builder.allPlayersCondition(allPlayersCondition);
        
        // --- Rules ---
        Map<String, Map<String, String>> rules = parseRules(section);
        builder.rules(rules);
        
        // --- Permission ---
        String permission = section.getString("permission", "");
        builder.permission(permission);
        
        // --- Player limits ---
        int minPlayers = section.getInt("min-players", -1);
        if (minPlayers < 0) {
            // Если не указано в шаблоне - берем из defaults
            minPlayers = plugin.getConfigManager().getDefaultMinPlayers();
        }
        if (minPlayers < 0) {
            validationWarnings.add("Template '" + name + "': Min players cannot be negative (" + minPlayers + "), using 0");
            minPlayers = 0;
        }
        builder.minPlayers(minPlayers);
        
        int maxPlayers = section.getInt("max-players", 30);
        if (maxPlayers < 0) {
            validationWarnings.add("Template '" + name + "': Max players cannot be negative (" + maxPlayers + "), using 0");
            maxPlayers = 0;
        }
        if (maxPlayers > 0 && minPlayers > maxPlayers) {
            validationWarnings.add("Template '" + name + "': Min players (" + minPlayers + ") > Max players (" + maxPlayers + "), adjusting");
            maxPlayers = Math.max(minPlayers, maxPlayers);
        }
        builder.maxPlayers(maxPlayers);
        
        // --- Capture settings ---
        int needAmount = section.getInt("need-amount", 100000);
        if (needAmount <= 0) {
            validationErrors.add("Template '" + name + "': Need amount must be > 0 (" + needAmount + ")");
            needAmount = 100000;
        }
        builder.needAmount(needAmount);
        
        int tickCapture = section.getInt("tick-capture", 120);
        if (tickCapture < 1) {
            validationWarnings.add("Template '" + name + "': Tick capture too low (" + tickCapture + "), using 1");
            tickCapture = 1;
        }
        builder.tickCapture(tickCapture);
        
        double multiplier = section.getDouble("multiplier", 0.00001);
        if (multiplier < 0) {
            validationWarnings.add("Template '" + name + "': Multiplier cannot be negative (" + multiplier + "), using 0");
            multiplier = 0;
        }
        builder.multiplier(multiplier);
        
        // --- Team multiplier ---
        String teamMultiplierType = section.getString("type-team-multiplier", "individual");
        if (!isValidTeamMultiplierType(teamMultiplierType)) {
            validationWarnings.add("Template '" + name + "': Invalid team multiplier type '" + teamMultiplierType + "', using 'individual'");
            teamMultiplierType = "individual";
        }
        builder.teamMultiplierType(teamMultiplierType);
        
        double teamMultiplier = section.getDouble("team-multiplier", 0.0);
        if (teamMultiplier < 0) {
            validationWarnings.add("Template '" + name + "': Team multiplier cannot be negative (" + teamMultiplier + "), using 0");
            teamMultiplier = 0;
        }
        builder.teamMultiplier(teamMultiplier);
        
        // --- Region ---
        String pos1Str = section.getString("pos1", "0,0,0");
        String pos2Str = section.getString("pos2", "0,-1,0");
        
        Location pos1 = parseLocation(pos1Str);
        Location pos2 = parseLocation(pos2Str);
        
        if (pos1 == null || pos2 == null) {
            validationErrors.add("Template '" + name + "': Invalid positions - pos1: " + pos1Str + ", pos2: " + pos2Str);
            pos1 = new Location(null, 0, 0, 0);
            pos2 = new Location(null, 0, -1, 0);
        }
        
        if (pos1.getWorld() == null || pos2.getWorld() == null) {
            World defaultWorld = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
            if (defaultWorld != null) {
                pos1.setWorld(defaultWorld);
                pos2.setWorld(defaultWorld);
            } else {
                validationErrors.add("Template '" + name + "': No default world available!");
            }
        }
        
        builder.pos1(pos1);
        builder.pos2(pos2);
        
        String regionName = section.getString("region-name", "");
        builder.regionName(regionName);
        
        String regionFlags = section.getString("region-flags", "");
        if (!regionFlags.isEmpty() && !isValidRegionFlags(regionFlags)) {
            validationWarnings.add("Template '" + name + "': Invalid region flags format");
            regionFlags = "";
        }
        builder.regionFlags(regionFlags);
        
        // --- Max duration ---
        int maxDuration = section.getInt("max-duration", 0);
        if (maxDuration < 0) {
            validationWarnings.add("Template '" + name + "': Max duration cannot be negative (" + maxDuration + "), using 0");
            maxDuration = 0;
        }
        builder.maxDuration(maxDuration);
        
        // --- Commands (разделенные на start/tick/end) ---
        List<String> startCommands = section.getStringList("start-commands");
        if (startCommands == null) startCommands = new ArrayList<>();
        List<String> validatedStartCommands = validateCommands(startCommands, name);
        builder.startCommands(validatedStartCommands);
        
        List<String> tickCommands = section.getStringList("tick-commands");
        if (tickCommands == null) tickCommands = new ArrayList<>();
        List<String> validatedTickCommands = validateCommands(tickCommands, name);
        builder.tickCommands(validatedTickCommands);
        
        List<String> endCommands = section.getStringList("end-commands");
        if (endCommands == null) endCommands = new ArrayList<>();
        List<String> validatedEndCommands = validateCommands(endCommands, name);
        builder.endCommands(validatedEndCommands);
        
        return builder.build();
    }
    
    private Map<String, Map<String, String>> parseRules(ConfigurationSection section) {
        Map<String, Map<String, String>> rules = new LinkedHashMap<>();
        ConfigurationSection rulesSection = section.getConfigurationSection("rules");
        
        if (rulesSection == null) {
            return rules;
        }
        
        for (String key : rulesSection.getKeys(false)) {
            String value = rulesSection.getString(key);
            if (value == null || value.isEmpty()) {
                validationWarnings.add("Rule '" + key + "' has empty value, skipping");
                continue;
            }
            
            Map<String, String> rule = parseRuleValue(value);
            if (rule != null) {
                rules.put(key, rule);
            } else {
                validationWarnings.add("Rule '" + key + "' has invalid format: " + value);
            }
        }
        
        return rules;
    }
    
    private Map<String, String> parseRuleValue(String value) {
        Map<String, String> rule = new HashMap<>();
        
        if (value.startsWith("target:") || value.startsWith("other:")) {
            String[] parts = value.split(":", 2);
            if (parts.length == 2) {
                String prefix = parts[0];
                String rest = parts[1];
                
                String[] operators = {">=", "<=", "==", "!=", ">", "<", "!~", "!-", "-!", "~"};
                for (String operator : operators) {
                    if (rest.contains(operator)) {
                        String[] conditionParts = rest.split(Pattern.quote(operator), 2);
                        if (conditionParts.length == 2) {
                            String left = conditionParts[0].trim();
                            String right = conditionParts[1].trim();
                            
                            if (prefix.equals("target")) {
                                rule.put("type", "target:" + left);
                                rule.put("value", operator + right);
                            } else {
                                rule.put("type", "other:" + left);
                                rule.put("value", operator + right);
                            }
                            return rule;
                        }
                    }
                }
            }
        } else {
            String[] parts = value.split(":", 2);
            if (parts.length == 2) {
                rule.put("type", parts[0].trim());
                rule.put("value", parts[1].trim());
                return rule;
            }
        }
        
        return null;
    }
    
    private Location parseLocation(String str) {
        if (str == null || str.isEmpty()) {
            return null;
        }
        
        try {
            String[] parts = str.split(",");
            if (parts.length == 3) {
                double x = Double.parseDouble(parts[0].trim());
                double y = Double.parseDouble(parts[1].trim());
                double z = Double.parseDouble(parts[2].trim());
                
                World world = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
                return new Location(world, x, y, z);
            }
        } catch (NumberFormatException e) {
            // Invalid number format
        }
        
        return null;
    }
    
    private boolean validateTemplate(Template template) {
        boolean valid = true;
        
        if (template.getMultiplier() > 0) {
            double pointsPerTick = template.getNeedAmount() * template.getMultiplier();
            if (pointsPerTick < 0.001) {
                validationWarnings.add("Template '" + template.getName() + 
                    "': Points per tick is very low (" + pointsPerTick + "), capture may take very long");
            }
        }
        
        if (template.getMinPlayers() > 0 && template.getMultiplier() == 0 && template.getTeamMultiplier() == 0) {
            validationWarnings.add("Template '" + template.getName() + 
                "': Min players > 0 but all multipliers are 0, no points will be awarded");
        }
        
        return valid;
    }
    
    private List<String> validateCommands(List<String> commands, String templateName) {
        List<String> validated = new ArrayList<>();
        Set<String> validPrefixes = new HashSet<>(Arrays.asList(
            "asConsole", "asPlayer", "message", "gMessage", 
            "sound", "gSound", "actionbar", "gActionbar", 
            "title", "delay", "random"
        ));
        
        for (String command : commands) {
            if (command == null || command.trim().isEmpty()) {
                continue;
            }
            
            String trimmed = command.trim();
            
            // Специальная обработка для check: формата (поддержка нескольких check:)
            if (trimmed.startsWith("check:")) {
                List<String> conditions = new ArrayList<>();
                String remaining = trimmed;
                String actualCommand = "";
                
                while (remaining.startsWith("check:")) {
                    int nextCheck = remaining.indexOf(" check:", 1);
                    int firstExclamation = remaining.indexOf("! ");
                    
                    if (nextCheck > 0 && nextCheck < firstExclamation) {
                        String condition = remaining.substring(6, nextCheck).trim();
                        conditions.add(condition);
                        remaining = remaining.substring(nextCheck);
                    } else if (firstExclamation > 0) {
                        String condition = remaining.substring(6, firstExclamation).trim();
                        conditions.add(condition);
                        actualCommand = remaining.substring(firstExclamation + 2).trim();
                        break;
                    } else {
                        validationWarnings.add("Template '" + templateName + 
                            "': Invalid check format (missing '! '): " + trimmed);
                        break;
                    }
                }
                
                if (conditions.isEmpty() || actualCommand.isEmpty()) {
                    validationWarnings.add("Template '" + templateName + 
                        "': Invalid check format: " + trimmed);
                    continue;
                }
                
                // Проверяем все условия
                boolean validConditions = true;
                String[] operators = {">=", "<=", "==", "!=", ">", "<", "!~", "!-", "-!", "~"};
                for (String condition : conditions) {
                    if (condition.isEmpty()) {
                        validationWarnings.add("Template '" + templateName + 
                            "': Empty condition in check command: " + trimmed);
                        validConditions = false;
                        break;
                    }
                    boolean hasOperator = false;
                    for (String op : operators) {
                        if (condition.contains(op)) {
                            hasOperator = true;
                            break;
                        }
                    }
                    if (!hasOperator) {
                        validationWarnings.add("Template '" + templateName + 
                            "': Condition has no operator: " + condition);
                        validConditions = false;
                        break;
                    }
                }
                
                if (validConditions) {
                    validated.add(trimmed);
                }
                continue;
            }
            
            // Обработка обычных команд с префиксом
            if (trimmed.contains("! ")) {
                int firstExclamation = trimmed.indexOf("! ");
                String prefix = trimmed.substring(0, firstExclamation).trim();
                String content = trimmed.substring(firstExclamation + 2).trim();
                
                if (prefix.startsWith("random:")) {
                    try {
                        String chanceStr = prefix.substring(7);
                        int chance = Integer.parseInt(chanceStr);
                        if (chance < 0 || chance > 100) {
                            validationWarnings.add("Template '" + templateName + 
                                "': Random chance must be 0-100 (" + chance + ")");
                            continue;
                        }
                    } catch (NumberFormatException e) {
                        validationWarnings.add("Template '" + templateName + 
                            "': Invalid random chance: " + trimmed);
                        continue;
                    }
                    validated.add(trimmed);
                    continue;
                }
                
                if (prefix.equals("delay")) {
                    try {
                        int delay = Integer.parseInt(content);
                        if (delay < 0) {
                            validationWarnings.add("Template '" + templateName + 
                                "': Delay cannot be negative (" + delay + ")");
                            continue;
                        }
                    } catch (NumberFormatException e) {
                        validationWarnings.add("Template '" + templateName + 
                            "': Invalid delay format: " + trimmed);
                        continue;
                    }
                    validated.add(trimmed);
                    continue;
                }
                
                if (!validPrefixes.contains(prefix)) {
                    validationWarnings.add("Template '" + templateName + 
                        "': Unknown command prefix '" + prefix + "' in: " + trimmed);
                }
                
                validated.add(trimmed);
            } else {
                validationWarnings.add("Template '" + templateName + 
                    "': Unknown command format (missing '! '): " + trimmed);
            }
        }
        
        return validated;
    }
    
    private boolean isValidBossBarColor(String color) {
        try {
            org.bukkit.boss.BarColor.valueOf(color.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    private boolean isValidSegments(int segments) {
        return segments == 1 || segments == 6 || segments == 10 || segments == 12 || segments == 20;
    }
    
    private boolean isValidTimerFormat(String format) {
        return format.equals("HH:mm:ss") || 
               format.equals("mm:ss") || 
               format.equals("HH:mm") || 
               format.equals("ss");
    }
    
    private boolean isValidTeamMultiplierType(String type) {
        return type.equalsIgnoreCase("individual") || 
               type.equalsIgnoreCase("shared") || 
               type.equalsIgnoreCase("disabled");
    }
    
    private boolean isValidRegionFlags(String flags) {
        if (flags.isEmpty()) return true;
        
        String clean = flags.replaceAll("[{}]", "");
        String[] entries = clean.split(",");
        
        for (String entry : entries) {
            String[] parts = entry.trim().split(":", 2);
            if (parts.length != 2) {
                return false;
            }
        }
        return true;
    }
    
    public Map<String, Template> getTemplates() {
        return templates;
    }
    
    public List<String> getValidationErrors() {
        return validationErrors;
    }
    
    public List<String> getValidationWarnings() {
        return validationWarnings;
    }
    
    public boolean hasErrors() {
        return !validationErrors.isEmpty();
    }
    
    public boolean hasWarnings() {
        return !validationWarnings.isEmpty();
    }
}
