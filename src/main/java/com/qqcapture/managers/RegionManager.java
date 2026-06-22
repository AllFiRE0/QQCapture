package com.qqcapture.managers;

import com.qqcapture.QQCapture;
import com.qqcapture.models.CaptureSession;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
// import com.sk89q.worldguard.protection.managers.RegionManager;
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
    
    public void setupRegion(CaptureSession session) {
        if (!enabled) {
            return;
        }
    
        String regionName = session.getTemplate().getRegionName();
        if (regionName == null || regionName.isEmpty()) {
            return;
        }
    
        try {
            // Берем первую зону для создания региона
            Template.Zone firstZone = session.getTemplate().getZones().values().stream().findFirst().orElse(null);
            if (firstZone == null) {
                plugin.getLogger().warning("No zones found for template: " + session.getTemplate().getName());
                return;
            }
        
            World world = firstZone.getPos1().getWorld();
            if (world == null) {
                world = plugin.getServer().getWorlds().get(0);
            }
        
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            com.sk89q.worldguard.protection.managers.RegionManager manager = container.get(com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(world));
        
            if (manager == null) {
                plugin.getLogger().warning("Could not get RegionManager for world: " + world.getName());
                return;
            }
        
            ProtectedRegion region = manager.getRegion(regionName);
            if (region == null) {
                com.sk89q.worldedit.math.BlockVector3 min = com.sk89q.worldedit.math.BlockVector3.at(
                    Math.min(firstZone.getPos1().getBlockX(), firstZone.getPos2().getBlockX()),
                    Math.min(firstZone.getPos1().getBlockY(), firstZone.getPos2().getBlockY()),
                    Math.min(firstZone.getPos1().getBlockZ(), firstZone.getPos2().getBlockZ())
                );
                com.sk89q.worldedit.math.BlockVector3 max = com.sk89q.worldedit.math.BlockVector3.at(
                    Math.max(firstZone.getPos1().getBlockX(), firstZone.getPos2().getBlockX()),
                    Math.max(firstZone.getPos1().getBlockY(), firstZone.getPos2().getBlockY()),
                    Math.max(firstZone.getPos1().getBlockZ(), firstZone.getPos2().getBlockZ())
                );
            
                region = new ProtectedCuboidRegion(regionName, min, max);
                manager.addRegion(region);
            }
        
            applyRegionFlags(region, session.getTemplate().getRegionFlags());
        
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().info("Region setup: " + regionName + " for session " + session.getSessionId());
            }
        
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to setup region: " + regionName);
            e.printStackTrace();
        }
    }
    
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
                StateFlag pvpFlag = com.sk89q.worldguard.protection.flags.Flags.PVP;
                region.setFlag(pvpFlag, parseStateFlag(flagValue));
            } else if (flagName.equalsIgnoreCase("allow-spawning")) {
                StateFlag spawningFlag = com.sk89q.worldguard.protection.flags.Flags.MOB_SPAWNING;
                region.setFlag(spawningFlag, parseStateFlag(flagValue));
            } else if (flagName.equalsIgnoreCase("mob-damage")) {
                StateFlag mobDamageFlag = com.sk89q.worldguard.protection.flags.Flags.MOB_DAMAGE;
                region.setFlag(mobDamageFlag, parseStateFlag(flagValue));
            } else if (flagName.equalsIgnoreCase("fall-damage")) {
                StateFlag fallDamageFlag = com.sk89q.worldguard.protection.flags.Flags.FALL_DAMAGE;
                region.setFlag(fallDamageFlag, parseStateFlag(flagValue));
            } else if (flagName.equalsIgnoreCase("block-break")) {
                StateFlag blockBreakFlag = com.sk89q.worldguard.protection.flags.Flags.BLOCK_BREAK;
                region.setFlag(blockBreakFlag, parseStateFlag(flagValue));
            } else if (flagName.equalsIgnoreCase("block-place")) {
                StateFlag blockPlaceFlag = com.sk89q.worldguard.protection.flags.Flags.BLOCK_PLACE;
                region.setFlag(blockPlaceFlag, parseStateFlag(flagValue));
            } else if (flagName.equalsIgnoreCase("use")) {
                StateFlag useFlag = com.sk89q.worldguard.protection.flags.Flags.USE;
                region.setFlag(useFlag, parseStateFlag(flagValue));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to apply flag: " + flagName + " = " + flagValue);
        }
    }
    
    private StateFlag.State parseStateFlag(String value) {
        if (value.equalsIgnoreCase("allow") || value.equalsIgnoreCase("allow")) {
            return StateFlag.State.ALLOW;
        } else if (value.equalsIgnoreCase("deny") || value.equalsIgnoreCase("deny")) {
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
            com.sk89q.worldguard.protection.managers.RegionManager manager = container.get(com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(world));
            
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
