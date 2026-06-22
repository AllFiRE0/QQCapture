package com.qqcapture.config;

import com.qqcapture.QQCapture;
import com.qqcapture.models.Template;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    private final QQCapture plugin;
    private FileConfiguration config;
    private Map<String, Template> templates;
    private boolean debug;
    private String language;
    private TemplateConfig templateConfig;
    
    // Дополнительные настройки
    private int defaultTickCapture;
    private int defaultNeedAmount;
    private double defaultMultiplier;
    private int defaultMinPlayers;
    private int defaultMaxPlayers;
    private String defaultTimerFormat;
    private boolean defaultSendOnRejoin;
    
    public ConfigManager(QQCapture plugin) {
        this.plugin = plugin;
        this.templates = new HashMap<>();
        this.templateConfig = new TemplateConfig(plugin);
        
        // Значения по умолчанию
        this.defaultTickCapture = 120;
        this.defaultNeedAmount = 100000;
        this.defaultMultiplier = 0.00001;
        this.defaultMinPlayers = 2;
        this.defaultMaxPlayers = 30;
        this.defaultTimerFormat = "mm:ss";
        this.defaultSendOnRejoin = true;
        
        reloadConfig();
    }
    
    public void reloadConfig() {
        // Перезагружаем конфиг
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        
        // Загружаем общие настройки
        this.debug = config.getBoolean("debug", false);
        this.language = config.getString("language", "ru");
        
        // Устанавливаем режим отладки в TemplateConfig ДО загрузки шаблонов
        templateConfig.setDebugMode(this.debug);
        
        // Загружаем глобальные настройки по умолчанию (если есть)
        loadDefaultSettings();
        
        // Загружаем шаблоны через TemplateConfig
        loadTemplates();
    }
    
    private void loadDefaultSettings() {
        ConfigurationSection defaultsSection = config.getConfigurationSection("defaults");
        if (defaultsSection != null) {
            this.defaultTickCapture = defaultsSection.getInt("tick-capture", 120);
            this.defaultNeedAmount = defaultsSection.getInt("need-amount", 100000);
            this.defaultMultiplier = defaultsSection.getDouble("multiplier", 0.00001);
            this.defaultMinPlayers = defaultsSection.getInt("min-players", 2);
            this.defaultMaxPlayers = defaultsSection.getInt("max-players", 30);
            this.defaultTimerFormat = defaultsSection.getString("timer-format", "mm:ss");
            this.defaultSendOnRejoin = defaultsSection.getBoolean("send-on-rejoin", true);
            
            if (debug) {
                plugin.getLogger().info("Loaded default settings:");
                plugin.getLogger().info("  tick-capture: " + defaultTickCapture);
                plugin.getLogger().info("  need-amount: " + defaultNeedAmount);
                plugin.getLogger().info("  multiplier: " + defaultMultiplier);
            }
        }
    }
    
    private void loadTemplates() {
        // Используем TemplateConfig для загрузки
        this.templates = templateConfig.loadTemplates(config);
        
        // Логируем результаты валидации
        if (templateConfig.hasErrors()) {
            plugin.getLogger().warning("Template validation completed with " + 
                templateConfig.getValidationErrors().size() + " errors!");
            for (String error : templateConfig.getValidationErrors()) {
                plugin.getLogger().warning("  ✗ " + error);
            }
        }
        
        if (templateConfig.hasWarnings()) {
            plugin.getLogger().info("Template validation completed with " + 
                templateConfig.getValidationWarnings().size() + " warnings.");
            if (debug) {
                for (String warning : templateConfig.getValidationWarnings()) {
                    plugin.getLogger().info("  ⚠ " + warning);
                }
            }
        }
    }
    
    /**
     * Получение шаблона с учетом регистра
     */
    public Template getTemplate(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        return templates.get(name.toLowerCase());
    }
    
    public boolean templateExists(String name) {
        return name != null && templates.containsKey(name.toLowerCase());
    }
    
    public Map<String, Template> getTemplates() {
        return new HashMap<>(templates);
    }
    
    /**
     * Получение всех имен шаблонов
     */
    public java.util.Set<String> getTemplateNames() {
        return templates.keySet();
    }
    
    /**
     * Получение количества шаблонов
     */
    public int getTemplateCount() {
        return templates.size();
    }
    
    /**
     * Релоад конкретного шаблона
     */
    public Template reloadTemplate(String name) {
        ConfigurationSection templatesSection = config.getConfigurationSection("templates");
        if (templatesSection == null) {
            return null;
        }
        
        ConfigurationSection templateSection = templatesSection.getConfigurationSection(name);
        if (templateSection == null) {
            return null;
        }
        
        try {
            Map<String, Template> loaded = templateConfig.loadTemplates(config);
            Template template = loaded.get(name.toLowerCase());
            if (template != null) {
                templates.put(name.toLowerCase(), template);
                if (debug) {
                    plugin.getLogger().info("Reloaded template: " + name);
                }
                return template;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to reload template: " + name);
            e.printStackTrace();
        }
        return null;
    }
    
    // Getters
    public boolean isDebug() {
        return debug;
    }
    
    public String getLanguage() {
        return language;
    }
    
    public FileConfiguration getConfig() {
        return config;
    }
    
    public TemplateConfig getTemplateConfig() {
        return templateConfig;
    }
    
    // Default settings getters
    public int getDefaultTickCapture() {
        return defaultTickCapture;
    }
    
    public int getDefaultNeedAmount() {
        return defaultNeedAmount;
    }
    
    public double getDefaultMultiplier() {
        return defaultMultiplier;
    }
    
    public int getDefaultMinPlayers() {
        return defaultMinPlayers;
    }
    
    public int getDefaultMaxPlayers() {
        return defaultMaxPlayers;
    }
    
    public String getDefaultTimerFormat() {
        return defaultTimerFormat;
    }
    
    public boolean isDefaultSendOnRejoin() {
        return defaultSendOnRejoin;
    }
    
    /**
     * Проверка наличия ошибок валидации
     */
    public boolean hasValidationErrors() {
        return templateConfig.hasErrors();
    }
    
    /**
     * Проверка наличия предупреждений валидации
     */
    public boolean hasValidationWarnings() {
        return templateConfig.hasWarnings();
    }
    
    /**
     * Получение всех ошибок валидации
     */
    public java.util.List<String> getValidationErrors() {
        return templateConfig.getValidationErrors();
    }
    
    /**
     * Получение всех предупреждений валидации
     */
    public java.util.List<String> getValidationWarnings() {
        return templateConfig.getValidationWarnings();
    }
}
