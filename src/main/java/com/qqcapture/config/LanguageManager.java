package com.qqcapture.managers;

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
    
    public LanguageManager(QQCapture plugin) {
        this.plugin = plugin;
        this.messages = new HashMap<>();
        
        // Создаем папку languages если её нет
        File langDir = new File(plugin.getDataFolder(), "languages");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }
        
        loadLanguages();
    }
    
    private void loadLanguages() {
        // Load default languages
        loadLanguageFile("ru");
        loadLanguageFile("en");
        loadLanguageFile("zh");
        loadLanguageFile("ko");
        loadLanguageFile("ja");
        
        // Set current language
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
                // Load from resources
                InputStream inputStream = plugin.getResource("languages/" + languageCode + ".yml");
                if (inputStream == null) {
                    plugin.getLogger().warning("Language file not found in resources: " + languageCode);
                    return;
                }
                langConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8)
                );
                
                // Save to disk for editing
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
            plugin.getLogger().info("Loaded language: " + languageCode + " (" + langMessages.size() + " messages)");
            
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load language: " + languageCode);
            e.printStackTrace();
        }
    }
    
    public String getMessage(String key) {
        Map<String, String> langMessages = messages.get(currentLanguage);
        if (langMessages == null || !langMessages.containsKey(key)) {
            // Try English fallback
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
