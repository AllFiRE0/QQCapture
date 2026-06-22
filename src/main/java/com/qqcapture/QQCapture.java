package com.qqcapture;

import com.qqcapture.commands.QQCaptureCommand;
import com.qqcapture.config.ConfigManager;
import com.qqcapture.config.LanguageManager;
import com.qqcapture.managers.*;
import com.qqcapture.integration.PlaceholderAPIHook;
import com.qqcapture.integration.VaultIntegration;
import com.qqcapture.integration.WorldGuardIntegration;
import com.qqcapture.listeners.PlayerListener;
import org.bukkit.plugin.java.JavaPlugin;

public class QQCapture extends JavaPlugin {
    private static QQCapture instance;
    
    private ConfigManager configManager;
    private LanguageManager languageManager;
    private SessionManager sessionManager;
    private BossBarManager bossBarManager;
    private CommandManager commandManager;
    private PlaceholderManager placeholderManager;
    private ConditionManager conditionManager;
    private RegionManager regionManager;
    private TopStorageManager topStorageManager;
    
    // Integrations
    private VaultIntegration vaultIntegration;
    private WorldGuardIntegration worldGuardIntegration;
    private PlaceholderAPIHook placeholderAPIHook;
    
    @Override
    public void onEnable() {
        instance = this;
        
        saveDefaultConfig();
        
        initializeManagers();
        
        getCommand("qqcapture").setExecutor(new QQCaptureCommand());
        
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        
        checkDependencies();
        
        getLogger().info("QQCapture v" + getDescription().getVersion() + " enabled!");
        getLogger().info("Debug mode: " + configManager.isDebug());
    }
    
    private void initializeManagers() {
        this.configManager = new ConfigManager(this);
        this.languageManager = new LanguageManager(this);
        this.sessionManager = new SessionManager(this);
        this.bossBarManager = new BossBarManager(this);
        this.commandManager = new CommandManager(this);
        this.placeholderManager = new PlaceholderManager(this);
        this.conditionManager = new ConditionManager(this);
        this.regionManager = new RegionManager(this);
        this.topStorageManager = new TopStorageManager(this);
    }
    
    private void checkDependencies() {
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            this.vaultIntegration = new VaultIntegration(this);
            getLogger().info("Vault integration enabled!");
        } else {
            getLogger().warning("Vault not found! Economy features will be disabled.");
        }
        
        if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            this.worldGuardIntegration = new WorldGuardIntegration(this);
            getLogger().info("WorldGuard integration enabled!");
        } else {
            getLogger().warning("WorldGuard not found! Region protection features will be disabled.");
        }
        
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            this.placeholderAPIHook = new PlaceholderAPIHook(this);
            getLogger().info("PlaceholderAPI integration enabled!");
        } else {
            getLogger().warning("PlaceholderAPI not found! Custom placeholders will be limited.");
        }
    }
    
    @Override
    public void onDisable() {
        if (sessionManager != null) {
            sessionManager.stopAllSessions();
        }
        
        if (bossBarManager != null) {
            bossBarManager.clearAllBossBars();
        }
        
        getLogger().info("QQCapture disabled!");
    }
    
    public static QQCapture getInstance() {
        return instance;
    }
    
    // Getters
    public ConfigManager getConfigManager() { return configManager; }
    public LanguageManager getLanguageManager() { return languageManager; }
    public SessionManager getSessionManager() { return sessionManager; }
    public BossBarManager getBossBarManager() { return bossBarManager; }
    public CommandManager getCommandManager() { return commandManager; }
    public PlaceholderManager getPlaceholderManager() { return placeholderManager; }
    public ConditionManager getConditionManager() { return conditionManager; }
    public RegionManager getRegionManager() { return regionManager; }
    public TopStorageManager getTopStorageManager() { return topStorageManager; }
    public VaultIntegration getVaultIntegration() { return vaultIntegration; }
    public WorldGuardIntegration getWorldGuardIntegration() { return worldGuardIntegration; }
    public PlaceholderAPIHook getPlaceholderAPIHook() { return placeholderAPIHook; }
}
