package com.qqcapture.integration;

import com.qqcapture.QQCapture;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class WorldGuardIntegration {
    private final QQCapture plugin;
    private WorldGuardPlugin worldGuard;
    private boolean enabled;
    
    public WorldGuardIntegration(QQCapture plugin) {
        this.plugin = plugin;
        this.enabled = false;
        setupWorldGuard();
    }
    
    private void setupWorldGuard() {
        if (plugin.getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            this.worldGuard = (WorldGuardPlugin) plugin.getServer().getPluginManager().getPlugin("WorldGuard");
            this.enabled = true;
            plugin.getLogger().info("WorldGuard integration ready!");
        } else {
            plugin.getLogger().warning("WorldGuard not found!");
        }
    }
    
    public boolean isPlayerInRegion(Player player, String regionName) {
        if (!enabled || player == null || regionName == null || regionName.isEmpty()) {
            return false;
        }
        
        try {
            World world = player.getWorld();
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager manager = container.get(com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(world));
            
            if (manager == null) {
                return false;
            }
            
            ProtectedRegion region = manager.getRegion(regionName);
            if (region == null) {
                return false;
            }
            
            com.sk89q.worldedit.math.BlockVector3 pos = com.sk89q.worldedit.math.BlockVector3.at(
                player.getLocation().getBlockX(),
                player.getLocation().getBlockY(),
                player.getLocation().getBlockZ()
            );
            
            return region.contains(pos);
            
        } catch (Exception e) {
            return false;
        }
    }
    
    public boolean createRegion(String regionName, Location pos1, Location pos2) {
        if (!enabled) {
            return false;
        }
        
        try {
            World world = pos1.getWorld();
            if (world == null) {
                return false;
            }
            
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager manager = container.get(com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(world));
            
            if (manager == null) {
                return false;
            }
            
            ProtectedRegion region = manager.getRegion(regionName);
            if (region == null) {
                com.sk89q.worldedit.math.BlockVector3 min = com.sk89q.worldedit.math.BlockVector3.at(
                    Math.min(pos1.getBlockX(), pos2.getBlockX()),
                    Math.min(pos1.getBlockY(), pos2.getBlockY()),
                    Math.min(pos1.getBlockZ(), pos2.getBlockZ())
                );
                com.sk89q.worldedit.math.BlockVector3 max = com.sk89q.worldedit.math.BlockVector3.at(
                    Math.max(pos1.getBlockX(), pos2.getBlockX()),
                    Math.max(pos1.getBlockY(), pos2.getBlockY()),
                    Math.max(pos1.getBlockZ(), pos2.getBlockZ())
                );
                
                region = new com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion(regionName, min, max);
                manager.addRegion(region);
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to create region: " + regionName);
            return false;
        }
    }
    
    public boolean deleteRegion(String regionName, World world) {
        if (!enabled || world == null) {
            return false;
        }
        
        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager manager = container.get(com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(world));
            
            if (manager == null) {
                return false;
            }
            
            ProtectedRegion region = manager.getRegion(regionName);
            if (region != null) {
                manager.removeRegion(regionName);
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to delete region: " + regionName);
            return false;
        }
    }
    
    public boolean isEnabled() {
        return enabled;
    }
}
