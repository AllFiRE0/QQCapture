package com.qqcapture.listeners;

import com.qqcapture.QQCapture;
import com.qqcapture.models.CaptureSession;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class RegionListener implements Listener {
    private final QQCapture plugin;
    
    public RegionListener() {
        this.plugin = QQCapture.getInstance();
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // Основная логика уже в PlayerListener
    }
}
