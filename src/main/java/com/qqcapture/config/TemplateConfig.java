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
    
    public TemplateConfig(QQCapture plugin) {
        this.plugin = plugin;
        this.templates = new LinkedHashMap<>();
        this.validationErrors = new ArrayList<>();
        this.validationWarnings = new ArrayList<>();
    }
    
    /**
     * Загружает все шаблоны из конфигурации
     */
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
                        if (plugin.getConfigManager().isDebug()) {
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
        
        // Log validation results
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
    
    /**
     * Парсит один шаблон из конфигурации
     */
    private Template parseTemplate(String name, ConfigurationSection section) {
        Template.Builder builder = new Template.Builder(name);
        
        // --- BossBar секция ---
        ConfigurationSection bossBarSection = section.getConfigurationSection("bossbar");
        if (bossBarSection != null) {
            // Color
            String color = bossBarSection.getString("color", "GREEN");
            if (isValidBossBarColor(color)) {
                builder.bossBarColor(color);
            } else {
                validationWarnings.add("Template '" + name + "': Invalid bossbar color '" + color + "', using GREEN");
                builder.bossBarColor("GREEN");
            }
            
            // Update interval
            int updateInterval = bossBarSection.getInt("обновлять-боссбар-каждые-N-тиков", 20);
            if (updateInterval < 1) {
                validationWarnings.add("Template '" + name + "': Bossbar update interval too low (" + updateInterval + "), using 1");
                updateInterval = 1;
            }
            builder.bossBarUpdateTicks(updateInterval);
            
            // Texts
            builder.startText(bossBarSection.getString("start-text", 
                "<gradient:#00FF00:#55FF55>Ивент начался!</gradient>"));
            builder.progressText(bossBarSection.getString("progress-text", 
                "<gradient:#FF0000:#FFAA00>Прогресс: %current%/%max%</gradient>"));
            builder.endText(bossBarSection.getString("end-text", 
                "<gradient:#FF5555:#FF0000>Ивент завершился!</gradient>"));
            
            // Segments
            int segments = bossBarSection.getInt("segments", 12);
            if (!isValidSegments(segments)) {
                validationWarnings.add("Template '" + name + "': Invalid segments '" + segments + "', using 12");
                segments = 12;
            }
            builder.segments(segments);
            
            // Update ticks
            int updateTicks = bossBarSection.getInt("update-ticks", 5);
            if (updateTicks < 1) {
                validationWarnings.add("Template '" + name + "': Update ticks too low (" + updateTicks + "), using 1");
                updateTicks = 1;
            }
            builder.updateTicks(updateTicks);
            
            // Delays
            builder.startDelay(Math.max(0, bossBarSection.getInt("start-delay", 3)));
            builder.endDelay(Math.max(0, bossBarSection.getInt("end-delay", 3)));
            
            // Timer format
            String timerFormat = bossBarSection.getString("timer-format", "mm:ss");
            if (!isValidTimerFormat(timerFormat)) {
                validationWarnings.add("Template '" + name + "': Invalid timer format '" + timerFormat + "', using mm:ss");
                timerFormat = "mm:ss";
            }
            builder.timerFormat(timerFormat);
            
            // Send on rejoin
            builder.sendOnRejoin(bossBarSection.getBoolean("отправлять-боссбар-когда-игрок-перезаходит-на-сервер", true));
            
            // Boss bar text
            String bossBarText = bossBarSection.getString("текст-боссбара", 
                "Ивент: %current%/%max% (Участников: %players% Групп: %groups%)");
            builder.bossBarText(bossBarText);
        } else {
            // Default bossbar settings
            builder.bossBarColor("GREEN")
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
        int minPlayers = section.getInt("min-players", 2);
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
        String teamMultiplierType = section.getString("type-team-multiplier", "индивидуально");
        if (!isValidTeamMultiplierType(teamMultiplierType)) {
            validationWarnings.add("Template '" + name + "': Invalid team multiplier type '" + teamMultiplierType + "', using 'индивидуально'");
            teamMultiplierType = "индивидуально";
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
        
        // --- Commands ---
        boolean tickCommand = section.getBoolean("tick-command", false);
        builder.tickCommand(tickCommand);
        
        List<String> commands = section.getStringList("commands");
        if (commands == null) {
            commands = new ArrayList<>();
        }
        
        // Validate commands
        List<String> validatedCommands = validateCommands(commands, name);
        builder.commands(validatedCommands);
        
        return builder.build();
    }
    
    /**
     * Парсит правила из конфигурации
     */
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
    
    /**
     * Парсит значение правила
     */
    private Map<String, String> parseRuleValue(String value) {
        Map<String, String> rule = new HashMap<>();
        
        // Check for target: or other: prefix
        if (value.startsWith("target:") || value.startsWith("other:")) {
            String[] parts = value.split(":", 2);
            if (parts.length == 2) {
                String prefix = parts[0];
                String rest = parts[1];
                
                // Check for operator in rest
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
            // Regular rule format: type: value
            String[] parts = value.split(":", 2);
            if (parts.length == 2) {
                rule.put("type", parts[0].trim());
                rule.put("value", parts[1].trim());
                return rule;
            }
        }
        
        return null;
    }
    
    /**
     * Парсит строку координат в Location
     */
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
    
    /**
     * Валидирует шаблон
     */
    private boolean validateTemplate(Template template) {
        boolean valid = true;
        
        // Check if need amount is reachable with current multiplier
        if (template.getMultiplier() > 0) {
            double pointsPerTick = template.getNeedAmount() * template.getMultiplier();
            if (pointsPerTick < 0.001) {
                validationWarnings.add("Template '" + template.getName() + 
                    "': Points per tick is very low (" + pointsPerTick + "), capture may take very long");
            }
        }
        
        // Check if min players > 0 but no multiplier
        if (template.getMinPlayers() > 0 && template.getMultiplier() == 0 && template.getTeamMultiplier() == 0) {
            validationWarnings.add("Template '" + template.getName() + 
                "': Min players > 0 but all multipliers are 0, no points will be awarded");
        }
        
        // Check if commands use placeholders that might not exist
        for (String command : template.getCommands()) {
            if (command.contains("%qqcapture_") && !command.contains(template.getName())) {
                validationWarnings.add("Template '" + template.getName() + 
                    "': Command uses placeholder with different template name: " + command);
            }
        }
        
        return valid;
    }
    
    /**
     * Валидирует команды
     */
    private List<String> validateCommands(List<String> commands, String templateName) {
        List<String> validated = new ArrayList<>();
        Set<String> validPrefixes = new HashSet<>(Arrays.asList(
            "asConsole", "asPlayer", "message", "gMessage", 
            "sound", "gSound", "actionbar", "gActionbar", 
            "title", "delay", "random", "check"
        ));
        
        for (String command : commands) {
            if (command == null || command.trim().isEmpty()) {
                continue;
            }
            
            String trimmed = command.trim();
            
            // Check for valid format
            if (trimmed.contains("! ")) {
                String[] parts = trimmed.split("! ", 2);
                String prefix = parts[0].trim();
                String content = parts[1].trim();
                
                // Handle check: prefix
                if (prefix.startsWith("check:")) {
                    String checkPart = prefix.substring(6);
                    if (!checkPart.contains("! ")) {
                        validationWarnings.add("Template '" + templateName + 
                            "': Invalid check format: " + trimmed);
                        continue;
                    }
                }
                
                // Handle random: prefix
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
                }
                
                // Check if prefix is valid
                String mainPrefix = prefix;
                if (mainPrefix.startsWith("check:") || mainPrefix.startsWith("random:")) {
                    // Extract actual prefix
                    if (mainPrefix.startsWith("check:")) {
                        String[] checkParts = mainPrefix.split("! ", 2);
                        if (checkParts.length == 2) {
                            mainPrefix = checkParts[1].trim();
                        }
                    } else if (mainPrefix.startsWith("random:")) {
                        String[] randomParts = mainPrefix.split("! ", 2);
                        if (randomParts.length == 2) {
                            mainPrefix = randomParts[1].trim();
                        }
                    }
                }
                
                // Check if main prefix is valid
                if (!validPrefixes.contains(mainPrefix) && 
                    !mainPrefix.startsWith("check:") && 
                    !mainPrefix.startsWith("random:")) {
                    validationWarnings.add("Template '" + templateName + 
                        "': Unknown command prefix '" + mainPrefix + "' in: " + trimmed);
                }
                
                validated.add(trimmed);
            } else if (trimmed.startsWith("delay! ")) {
                // Validate delay format
                String[] parts = trimmed.split("! ", 2);
                if (parts.length == 2) {
                    try {
                        int delay = Integer.parseInt(parts[1].trim());
                        if (delay < 0) {
                            validationWarnings.add("Template '" + templateName + 
                                "': Delay cannot be negative (" + delay + ")");
                            continue;
                        }
                        validated.add(trimmed);
                    } catch (NumberFormatException e) {
                        validationWarnings.add("Template '" + templateName + 
                            "': Invalid delay format: " + trimmed);
                        continue;
                    }
                } else {
                    validationWarnings.add("Template '" + templateName + 
                        "': Invalid delay format: " + trimmed);
                    continue;
                }
            } else {
                validationWarnings.add("Template '" + templateName + 
                    "': Unknown command format (missing '! '): " + trimmed);
            }
        }
        
        return validated;
    }
    
    /**
     * Проверяет валидность цвета боссбара
     */
    private boolean isValidBossBarColor(String color) {
        try {
            org.bukkit.boss.BarColor.valueOf(color.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * Проверяет валидность сегментов
     */
    private boolean isValidSegments(int segments) {
        return segments == 1 || segments == 6 || segments == 10 || segments == 12 || segments == 20;
    }
    
    /**
     * Проверяет валидность формата таймера
     */
    private boolean isValidTimerFormat(String format) {
        return format.equals("HH:mm:ss") || 
               format.equals("mm:ss") || 
               format.equals("HH:mm") || 
               format.equals("ss");
    }
    
    /**
     * Проверяет валидность типа командного множителя
     */
    private boolean isValidTeamMultiplierType(String type) {
        return type.equalsIgnoreCase("individual") || 
               type.equalsIgnoreCase("shared") || 
               type.equalsIgnoreCase("disabled");
    }
    
    /**
     * Проверяет валидность флагов региона
     */
    private boolean isValidRegionFlags(String flags) {
        if (flags.isEmpty()) return true;
        
        // Basic validation: check if it has proper format
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
