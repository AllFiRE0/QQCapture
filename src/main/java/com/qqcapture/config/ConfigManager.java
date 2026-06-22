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
    
    public ConfigManager(QQCapture plugin) {
        this.plugin = plugin;
        this.templates = new HashMap<>();
        reloadConfig();
    }
    
    public void reloadConfig() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        
        // Load general settings
        this.debug = config.getBoolean("debug", false);
        this.language = config.getString("language", "ru");
        
        // Load templates
        loadTemplates();
    }
    
    private void loadTemplates() {
        templates.clear();
        ConfigurationSection templatesSection = config.getConfigurationSection("templates");
        
        if (templatesSection == null) {
            plugin.getLogger().warning("No templates found in config!");
            return;
        }
        
        for (String templateName : templatesSection.getKeys(false)) {
            ConfigurationSection templateSection = templatesSection.getConfigurationSection(templateName);
            if (templateSection != null) {
                try {
                    Template template = new Template(templateName, templateSection);
                    templates.put(templateName.toLowerCase(), template);
                    if (debug) {
                        plugin.getLogger().info("Loaded template: " + templateName);
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("Failed to load template: " + templateName);
                    e.printStackTrace();
                }
            }
        }
    }
    
    public Template getTemplate(String name) {
        return templates.get(name.toLowerCase());
    }
    
    public boolean templateExists(String name) {
        return templates.containsKey(name.toLowerCase());
    }
    
    public Map<String, Template> getTemplates() {
        return templates;
    }
    
    public boolean isDebug() {
        return debug;
    }
    
    public String getLanguage() {
        return language;
    }
    
    public FileConfiguration getConfig() {
        return config;
    }
}
