package com.qqcapture.managers;

import com.qqcapture.QQCapture;
import com.qqcapture.models.CaptureSession;
import com.qqcapture.models.PlayerData;
import com.qqcapture.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandManager {
    private final QQCapture plugin;
    private final Pattern checkPattern;
    private final Pattern delayPattern;
    private final Pattern randomPattern;
    private final Pattern soundPattern;
    
    public CommandManager(QQCapture plugin) {
        this.plugin = plugin;
        this.checkPattern = Pattern.compile("^check:(.+?)! (.+)$");
        this.delayPattern = Pattern.compile("^delay! (\\d+)$");
        this.randomPattern = Pattern.compile("^random:(\\d+)! (.+)$");
        this.soundPattern = Pattern.compile("^sound! (.+?) (\\d+\\.?\\d*) (\\d+\\.?\\d*)$");
    }
    
    public void executeCommands(CaptureSession session, List<Player> players) {
        List<String> commands = session.getTemplate().getCommands();
        if (commands == null || commands.isEmpty()) {
            return;
        }
        
        // Process commands with delay
        processCommandsWithDelay(session, players, commands, 0);
    }
    
    private void processCommandsWithDelay(CaptureSession session, List<Player> players, List<String> commands, int index) {
        if (index >= commands.size()) {
            return;
        }
        
        String command = commands.get(index);
        
        // Check for delay
        Matcher delayMatcher = delayPattern.matcher(command);
        if (delayMatcher.matches()) {
            int delay = Integer.parseInt(delayMatcher.group(1));
            Bukkit.getScheduler().runTaskLater(plugin, 
                () -> processCommandsWithDelay(session, players, commands, index + 1), 
                delay * 20L);
            return;
        }
        
        // Execute command
        executeCommand(session, players, command);
        
        // Continue to next command
        Bukkit.getScheduler().runTask(plugin, 
            () -> processCommandsWithDelay(session, players, commands, index + 1));
    }
    
    private void executeCommand(CaptureSession session, List<Player> players, String command) {
        // Check for random
        Matcher randomMatcher =
