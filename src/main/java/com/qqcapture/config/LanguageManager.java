package com.qqcapture.config;

import com.qqcapture.QQCapture;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class LanguageManager {
    private final QQCapture plugin;
    private final Map<String, Map<String, String>> messages;
    private String currentLanguage;
    private boolean debugMode;
    
    public LanguageManager(QQCapture plugin) {
        this.plugin = plugin;
        this.messages = new HashMap<>();
        this.debugMode = plugin.getConfigManager().isDebug();
        
        File langDir = new File(plugin.getDataFolder(), "languages");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }
        
        loadLanguages();
    }
    
    private void loadLanguages() {
        loadLanguageFile("ru");
        loadLanguageFile("en");
        loadLanguageFile("zh");
        loadLanguageFile("ko");
        loadLanguageFile("ja");
        
        currentLanguage = plugin.getConfigManager().getLanguage();
        if (!messages.containsKey(currentLanguage)) {
            currentLanguage = "en";
        }
    }
    
    private void loadLanguageFile(String languageCode) {
        try {
            File langFile = new File(plugin.getDataFolder(), "languages/" + languageCode + ".yml");
            FileConfiguration langConfig;
            
            if (langFile.exists()) {
                langConfig = YamlConfiguration.loadConfiguration(langFile);
            } else {
                InputStream inputStream = plugin.getResource("languages/" + languageCode + ".yml");
                if (inputStream == null) {
                    // Только логируем в debug режиме
                    if (debugMode) {
                        plugin.getLogger().info("Language file not found: " + languageCode);
                    }
                    return;
                }
                langConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8)
                );
                langConfig.save(langFile);
            }
            
            Map<String, String> langMessages = new HashMap<>();
            for (String key : langConfig.getKeys(false)) {
                String value = langConfig.getString(key);
                if (value != null) {
                    langMessages.put(key, value);
                }
            }
            
            messages.put(languageCode, langMessages);
            if (debugMode) {
                plugin.getLogger().info("Loaded language: " + languageCode + " (" + langMessages.size() + " messages)");
            }
            
        } catch (Exception e) {
            if (debugMode) {
                plugin.getLogger().warning("Failed to load language: " + languageCode);
                e.printStackTrace();
            }
        }
    }
    
    public String getMessage(String key) {
        Map<String, String> langMessages = messages.get(currentLanguage);
        if (langMessages == null || !langMessages.containsKey(key)) {
            Map<String, String> enMessages = messages.get("en");
            if (enMessages != null && enMessages.containsKey(key)) {
                return enMessages.get(key);
            }
            return "Missing message: " + key;
        }
        return langMessages.get(key);
    }
    
    public String getMessage(String key, Map<String, String> replacements) {
        String message = getMessage(key);
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            message = message.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return message;
    }
    
    public void reloadLanguage() {
        messages.clear();
        this.debugMode = plugin.getConfigManager().isDebug();
        loadLanguages();
    }
    
    public String getCurrentLanguage() {
        return currentLanguage;
    }
    
    public void setCurrentLanguage(String language) {
        if (messages.containsKey(language)) {
            this.currentLanguage = language;
        }
    }
}
