package com.qqcapture.managers;

import com.qqcapture.QQCapture;
import com.qqcapture.models.CaptureSession;
import com.qqcapture.models.Template;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Map;

public class RegionManager {
    private final QQCapture plugin;
    private WorldGuardPlugin worldGuard;
    private boolean enabled;
    
    public RegionManager(QQCapture plugin) {
        this.plugin = plugin;
        this.enabled = false;
        setupWorldGuard();
    }
    
    private void setupWorldGuard() {
        if (plugin.getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            this.worldGuard = (WorldGuardPlugin) plugin.getServer().getPluginManager().getPlugin("WorldGuard");
            this.enabled = true;
            plugin.getLogger().info("WorldGuard integration enabled!");
        } else {
            plugin.getLogger().warning("WorldGuard not found! Region features disabled.");
        }
    }
    
    // ===== СОЗДАНИЕ РЕГИОНОВ ДЛЯ ВСЕХ ЗОН =====
    public void setupRegions(CaptureSession session) {
        if (!enabled) {
            return;
        }
    
        String baseRegionName = session.getTemplate().getRegionName();
        if (baseRegionName == null || baseRegionName.isEmpty()) {
            return;
        }
    
        try {
            int zoneId = 0;
            for (Map.Entry<Integer, Template.Zone> entry : session.getTemplate().getZones().entrySet()) {
                zoneId++;
                Template.Zone zone = entry.getValue();
                
                World world = zone.getPos1().getWorld();
                if (world == null) {
                    world = plugin.getServer().getWorlds().get(0);
                }
    
                RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
                com.sk89q.worldguard.protection.managers.RegionManager manager = container.get(
                    com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(world)
                );
    
                if (manager == null) {
                    plugin.getLogger().warning("Could not get RegionManager for world: " + world.getName());
                    continue;
                }
    
                // ===== УНИКАЛЬНОЕ ИМЯ ДЛЯ КАЖДОЙ ЗОНЫ =====
                String zoneRegionName = baseRegionName + "_zone_" + zoneId;
                
                ProtectedRegion region = manager.getRegion(zoneRegionName);
                if (region == null) {
                    com.sk89q.worldedit.math.BlockVector3 min = com.sk89q.worldedit.math.BlockVector3.at(
                        Math.min(zone.getPos1().getBlockX(), zone.getPos2().getBlockX()),
                        Math.min(zone.getPos1().getBlockY(), zone.getPos2().getBlockY()),
                        Math.min(zone.getPos1().getBlockZ(), zone.getPos2().getBlockZ())
                    );
                    com.sk89q.worldedit.math.BlockVector3 max = com.sk89q.worldedit.math.BlockVector3.at(
                        Math.max(zone.getPos1().getBlockX(), zone.getPos2().getBlockX()),
                        Math.max(zone.getPos1().getBlockY(), zone.getPos2().getBlockY()),
                        Math.max(zone.getPos1().getBlockZ(), zone.getPos2().getBlockZ())
                    );
    
                    region = new ProtectedCuboidRegion(zoneRegionName, min, max);
                    manager.addRegion(region);
                }
    
                // ===== ПРИМЕНЯЕМ ФЛАГИ =====
                applyRegionFlags(region, session.getTemplate().getRegionFlags());
    
                if (plugin.getConfigManager().isDebug()) {
                    plugin.getLogger().info("Region created: " + zoneRegionName + " for zone " + zoneId);
                }
            }
    
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to setup regions for template: " + session.getTemplate().getName());
            e.printStackTrace();
        }
    }
    
    // ===== УДАЛЕНИЕ РЕГИОНОВ =====
    public void deleteRegions(String baseRegionName, World world) {
        if (!enabled || world == null || baseRegionName == null || baseRegionName.isEmpty()) {
            return;
        }
    
        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            com.sk89q.worldguard.protection.managers.RegionManager manager = container.get(
                com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(world)
            );
    
            if (manager == null) {
                return;
            }
    
            // ===== УДАЛЯЕМ ВСЕ РЕГИОНЫ С ЭТИМ ИМЕНЕМ =====
            int deleted = 0;
            for (String regionName : manager.getRegions().keySet()) {
                if (regionName.startsWith(baseRegionName + "_zone_")) {
                    manager.removeRegion(regionName);
                    deleted++;
                    if (plugin.getConfigManager().isDebug()) {
                        plugin.getLogger().info("Region deleted: " + regionName);
                    }
                }
            }
    
            // ===== УДАЛЯЕМ ОСНОВНОЙ РЕГИОН (ЕСЛИ ЕСТЬ) =====
            ProtectedRegion mainRegion = manager.getRegion(baseRegionName);
            if (mainRegion != null) {
                manager.removeRegion(baseRegionName);
                deleted++;
                if (plugin.getConfigManager().isDebug()) {
                    plugin.getLogger().info("Region deleted: " + baseRegionName);
                }
            }
    
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().info("Deleted " + deleted + " regions for template: " + baseRegionName);
            }
    
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to delete regions: " + baseRegionName);
            e.printStackTrace();
        }
    }
    
    // ===== ПРИМЕНЕНИЕ ФЛАГОВ =====
    private void applyRegionFlags(ProtectedRegion region, String flagsString) {
        if (flagsString == null || flagsString.isEmpty()) {
            return;
        }
        
        try {
            String cleanFlags = flagsString.replaceAll("[{}]", "");
            String[] flagEntries = cleanFlags.split(",");
            
            for (String entry : flagEntries) {
                String[] parts = entry.trim().split(":", 2);
                if (parts.length == 2) {
                    String flagName = parts[0].trim();
                    String flagValue = parts[1].trim();
                    applyFlag(region, flagName, flagValue);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to parse region flags: " + flagsString);
        }
    }
    
    private void applyFlag(ProtectedRegion region, String flagName, String flagValue) {
        try {
            if (flagName.equalsIgnoreCase("pvp")) {
                region.setFlag(com.sk89q.worldguard.protection.flags.Flags.PVP, parseStateFlag(flagValue));
            } else if (flagName.equalsIgnoreCase("allow-spawning") || flagName.equalsIgnoreCase("mob-spawning")) {
                region.setFlag(com.sk89q.worldguard.protection.flags.Flags.MOB_SPAWNING, parseStateFlag(flagValue));
            } else if (flagName.equalsIgnoreCase("mob-damage")) {
                region.setFlag(com.sk89q.worldguard.protection.flags.Flags.MOB_DAMAGE, parseStateFlag(flagValue));
            } else if (flagName.equalsIgnoreCase("fall-damage")) {
                region.setFlag(com.sk89q.worldguard.protection.flags.Flags.FALL_DAMAGE, parseStateFlag(flagValue));
            } else if (flagName.equalsIgnoreCase("block-break")) {
                region.setFlag(com.sk89q.worldguard.protection.flags.Flags.BLOCK_BREAK, parseStateFlag(flagValue));
            } else if (flagName.equalsIgnoreCase("block-place")) {
                region.setFlag(com.sk89q.worldguard.protection.flags.Flags.BLOCK_PLACE, parseStateFlag(flagValue));
            } else if (flagName.equalsIgnoreCase("use")) {
                region.setFlag(com.sk89q.worldguard.protection.flags.Flags.USE, parseStateFlag(flagValue));
            } else if (flagName.equalsIgnoreCase("entry")) {
                region.setFlag(com.sk89q.worldguard.protection.flags.Flags.ENTRY, parseStateFlag(flagValue));
            } else if (flagName.equalsIgnoreCase("exit")) {
                region.setFlag(com.sk89q.worldguard.protection.flags.Flags.EXIT, parseStateFlag(flagValue));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to apply flag: " + flagName + " = " + flagValue);
        }
    }
    
    private StateFlag.State parseStateFlag(String value) {
        if (value.equalsIgnoreCase("allow")) {
            return StateFlag.State.ALLOW;
        } else if (value.equalsIgnoreCase("deny")) {
            return StateFlag.State.DENY;
        }
        return StateFlag.State.ALLOW;
    }
    
    public boolean isPlayerInRegion(Location location, String regionName) {
        if (!enabled || regionName == null || regionName.isEmpty()) {
            return false;
        }
        
        try {
            World world = location.getWorld();
            if (world == null) {
                return false;
            }
            
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            com.sk89q.worldguard.protection.managers.RegionManager manager = container.get(
                com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(world)
            );
            
            if (manager == null) {
                return false;
            }
            
            ProtectedRegion region = manager.getRegion(regionName);
            if (region == null) {
                return false;
            }
            
            com.sk89q.worldedit.math.BlockVector3 pos = com.sk89q.worldedit.math.BlockVector3.at(
                location.getBlockX(), location.getBlockY(), location.getBlockZ()
            );
            
            return region.contains(pos);
            
        } catch (Exception e) {
            return false;
        }
    }
    
    public boolean isEnabled() {
        return enabled;
    }
}