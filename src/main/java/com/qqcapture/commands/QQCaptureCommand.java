package com.qqcapture.commands;

import com.qqcapture.QQCapture;
import com.qqcapture.models.Template;
import com.qqcapture.utils.ColorUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class QQCaptureCommand implements CommandExecutor, TabCompleter {
    private final QQCapture plugin;
    
    public QQCaptureCommand() {
        this.plugin = QQCapture.getInstance();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check permission
        if (!sender.hasPermission("qqcapture.admin")) {
            sender.sendMessage(ColorUtils.colorize(
                plugin.getLanguageManager().getMessage("command-no-permission")
            ));
            return true;
        }
        
        // Check arguments
        if (args.length < 2) {
            sender.sendMessage(ColorUtils.colorize(
                plugin.getLanguageManager().getMessage("command-usage")
            ));
            return true;
        }
        
        String templateName = args[0];
        String pointsStr = args[1];
        boolean silent = args.length > 2 && args[2].equalsIgnoreCase("-s");
        
        // Validate template
        if (!plugin.getConfigManager().templateExists(templateName)) {
            sender.sendMessage(ColorUtils.colorize(
                plugin.getLanguageManager().getMessage("template-not-found")
                    .replace("%template%", templateName)
            ));
            return true;
        }
        
        // Validate points
        int points;
        try {
            points = Integer.parseInt(pointsStr);
            if (points <= 0) {
                sender.sendMessage(ColorUtils.colorize("&cPoints must be greater than 0!"));
                return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ColorUtils.colorize("&cInvalid points number!"));
            return true;
        }
        
        // Start session
        Player starter = sender instanceof Player ? (Player) sender : null;
        boolean started = plugin.getSessionManager().startSession(templateName, points, silent, starter);
        
        if (started) {
            if (!silent) {
                sender.sendMessage(ColorUtils.colorize(
                    plugin.getLanguageManager().getMessage("session-started")
                        .replace("%template%", templateName)
                ));
            }
            plugin.getLogger().info("Session started: " + templateName + " with " + points + " points by " + sender.getName());
        } else {
            sender.sendMessage(ColorUtils.colorize("&cFailed to start session! Check console for errors."));
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Template names
            for (String templateName : plugin.getConfigManager().getTemplates().keySet()) {
                if (templateName.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(templateName);
                }
            }
        } else if (args.length == 2) {
            // Points suggestions
            completions.add("100000");
            completions.add("500000");
            completions.add("1000000");
        } else if (args.length == 3) {
            // Silent option
            if ("-s".startsWith(args[2].toLowerCase())) {
                completions.add("-s");
            }
        }
        
        return completions;
    }
}
