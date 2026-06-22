package com.qqcapture.commands;

import com.qqcapture.QQCapture;
import com.qqcapture.models.CaptureSession;
import com.qqcapture.models.Template;
import com.qqcapture.utils.ColorUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QQCaptureCommand implements CommandExecutor, TabCompleter {
    private final QQCapture plugin;

    public QQCaptureCommand() {
        this.plugin = QQCapture.getInstance();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                return handleReload(sender);
            case "start":
                return handleStart(sender, args);
            case "stop":
                return handleStop(sender, args);
            case "list":
                return handleList(sender);
            case "info":
                return handleInfo(sender, args);
            case "top":
                return handleTop(sender, args);
            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("qqcapture.admin.reload")) {
            sender.sendMessage(ColorUtils.colorize(
                plugin.getLanguageManager().getMessage("command-no-permission")
            ));
            return true;
        }

        try {
            plugin.getConfigManager().reloadConfig();
            plugin.getLanguageManager().reloadLanguage();

            sender.sendMessage(ColorUtils.colorize(
                plugin.getLanguageManager().getMessage("reload-success")
            ));

            plugin.getLogger().info("Configuration reloaded by " + sender.getName());

        } catch (Exception e) {
            sender.sendMessage(ColorUtils.colorize(
                plugin.getLanguageManager().getMessage("reload-failed")
            ));
            plugin.getLogger().severe("Failed to reload configuration!");
            e.printStackTrace();
        }

        return true;
    }

    private boolean handleStart(CommandSender sender, String[] args) {
        if (!sender.hasPermission("qqcapture.admin.start")) {
            sender.sendMessage(ColorUtils.colorize(
                plugin.getLanguageManager().getMessage("command-no-permission")
            ));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ColorUtils.colorize("&cUsage: /qqcapture start <template> [-p:points] [-d:duration] [-r:region] [-m:multiplier] [-mt:teamMultiplier] [-t:tickCapture] [-s]"));
            return true;
        }

        String templateName = args[1];

        Map<String, String> params = parseParams(args);

        boolean silent = params.containsKey("s");
        String pointsStr = params.get("p");
        String durationStr = params.get("d");
        String regionName = params.get("r");
        String multiplierStr = params.get("m");
        String teamMultiplierStr = params.get("mt");
        String tickCaptureStr = params.get("t");

        if (!plugin.getConfigManager().templateExists(templateName)) {
            sender.sendMessage(ColorUtils.colorize(
                plugin.getLanguageManager().getMessage("template-not-found")
                    .replace("%template%", templateName)
            ));
            return true;
        }

        Template template = plugin.getConfigManager().getTemplate(templateName);
        int points = template.getNeedAmount();
        int duration = template.getMaxDuration();
        String finalRegionName = template.getRegionName();
        Double multiplier = null;
        Double teamMultiplier = null;
        Integer tickCapture = null;

        if (pointsStr != null) {
            try {
                points = Integer.parseInt(pointsStr);
                if (points <= 0) {
                    sender.sendMessage(ColorUtils.colorize("&cPoints must be greater than 0!"));
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(ColorUtils.colorize("&cInvalid points number: " + pointsStr));
                return true;
            }
        }

        if (durationStr != null) {
            try {
                duration = Integer.parseInt(durationStr);
                if (duration < 0) {
                    sender.sendMessage(ColorUtils.colorize("&cDuration cannot be negative!"));
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(ColorUtils.colorize("&cInvalid duration: " + durationStr));
                return true;
            }
        }

        if (regionName != null && !regionName.isEmpty()) {
            finalRegionName = regionName;
        }

        if (multiplierStr != null) {
            try {
                multiplier = Double.parseDouble(multiplierStr);
                if (multiplier < 0) {
                    sender.sendMessage(ColorUtils.colorize("&cMultiplier cannot be negative!"));
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(ColorUtils.colorize("&cInvalid multiplier: " + multiplierStr));
                return true;
            }
        }

        if (teamMultiplierStr != null) {
            try {
                teamMultiplier = Double.parseDouble(teamMultiplierStr);
                if (teamMultiplier < 0) {
                    sender.sendMessage(ColorUtils.colorize("&cTeam multiplier cannot be negative!"));
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(ColorUtils.colorize("&cInvalid team multiplier: " + teamMultiplierStr));
                return true;
            }
        }

        if (tickCaptureStr != null) {
            try {
                tickCapture = Integer.parseInt(tickCaptureStr);
                if (tickCapture < 1) {
                    sender.sendMessage(ColorUtils.colorize("&cTick capture must be greater than 0!"));
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(ColorUtils.colorize("&cInvalid tick capture: " + tickCaptureStr));
                return true;
            }
        }

        Template finalTemplate = createOverriddenTemplate(template, points, duration, finalRegionName,
                multiplier, teamMultiplier, tickCapture);

        Player starter = sender instanceof Player ? (Player) sender : null;
        boolean started = plugin.getSessionManager().startSession(finalTemplate, silent, starter);

        if (started) {
            if (!silent) {
                sender.sendMessage(ColorUtils.colorize(
                    plugin.getLanguageManager().getMessage("session-started")
                        .replace("%template%", templateName)
                ));
            }
            plugin.getLogger().info("Session started: " + templateName + " with " + points + " points, duration: " +
                    (duration > 0 ? duration + "s" : "∞") +
                    (multiplier != null ? ", multiplier: " + multiplier : "") +
                    (teamMultiplier != null ? ", team multiplier: " + teamMultiplier : "") +
                    (tickCapture != null ? ", tick capture: " + tickCapture : "") +
                    " by " + sender.getName());
        } else {
            sender.sendMessage(ColorUtils.colorize("&cFailed to start session! Check console for errors."));
        }

        return true;
    }

    private Map<String, String> parseParams(String[] args) {
        Map<String, String> params = new HashMap<>();

        for (int i = 2; i < args.length; i++) {
            String arg = args[i];

            if (arg.equalsIgnoreCase("-s")) {
                params.put("s", "true");
            } else if (arg.startsWith("-p:")) {
                params.put("p", arg.substring(3));
            } else if (arg.startsWith("-d:")) {
                params.put("d", arg.substring(3));
            } else if (arg.startsWith("-r:")) {
                params.put("r", arg.substring(3));
            } else if (arg.startsWith("-m:")) {
                params.put("m", arg.substring(3));
            } else if (arg.startsWith("-mt:")) {
                params.put("mt", arg.substring(4));
            } else if (arg.startsWith("-t:")) {
                params.put("t", arg.substring(3));
            }
        }

        return params;
    }

    private Template createOverriddenTemplate(Template original, int points, int duration, String regionName,
                                              Double multiplier, Double teamMultiplier, Integer tickCapture) {
        Template.Builder builder = new Template.Builder(original.getName() + "_override");

        builder.bossBarEnabled(original.isBossBarEnabled())
                .bossBarColor(original.getBossBarColor())
                .bossBarUpdateTicks(original.getBossBarUpdateTicks())
                .startText(original.getStartText())
                .progressText(original.getProgressText())
                .endText(original.getEndText())
                .segments(original.getSegments())
                .updateTicks(original.getUpdateTicks())
                .startDelay(original.getStartDelay())
                .endDelay(original.getEndDelay())
                .timerFormat(original.getTimerFormat())
                .sendOnRejoin(original.isSendOnRejoin())
                .bossBarText(original.getBossBarText())
                .condition(original.getCondition())
                .allPlayersCondition(original.getAllPlayersCondition())
                .rules(original.getRules())
                .permission(original.getPermission())
                .minPlayers(original.getMinPlayers())
                .maxPlayers(original.getMaxPlayers())
                .needAmount(points)
                .tickCapture(tickCapture != null ? tickCapture : original.getTickCapture())
                .multiplier(multiplier != null ? multiplier : original.getMultiplier())
                .teamMultiplierType(original.getTeamMultiplierType())
                .teamMultiplier(teamMultiplier != null ? teamMultiplier : original.getTeamMultiplier())
                .zones(original.getZones())
                .regionName(regionName)
                .regionFlags(original.getRegionFlags())
                .maxDuration(duration)
                .topStorageEnabled(original.isTopStorageEnabled())
                .topStorageDuration(original.getTopStorageDuration())
                .topAutoClearOnStart(original.isTopAutoClearOnStart())
                .startCommands(original.getStartCommands())
                .tickCommands(original.getTickCommands())
                .endCommands(original.getEndCommands());

        return builder.build();
    }

    private boolean handleStop(CommandSender sender, String[] args) {
        if (!sender.hasPermission("qqcapture.admin.stop")) {
            sender.sendMessage(ColorUtils.colorize(
                plugin.getLanguageManager().getMessage("command-no-permission")
            ));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ColorUtils.colorize("&cUsage: /qqcapture stop <template>"));
            return true;
        }

        String templateName = args[1];

        CaptureSession session = null;
        for (CaptureSession s : plugin.getSessionManager().getActiveSessions()) {
            if (s.getTemplate().getName().equalsIgnoreCase(templateName) ||
                    s.getTemplate().getName().equalsIgnoreCase(templateName + "_override")) {
                session = s;
                break;
            }
        }

        if (session == null) {
            sender.sendMessage(ColorUtils.colorize("&cNo active session found for template: " + templateName));
            return true;
        }

        plugin.getSessionManager().stopSession(session.getSessionId());
        sender.sendMessage(ColorUtils.colorize("&aSession for template '" + templateName + "' stopped!"));
        plugin.getLogger().info("Session " + session.getSessionId() + " stopped by " + sender.getName());

        return true;
    }

    private boolean handleList(CommandSender sender) {
        if (!sender.hasPermission("qqcapture.admin.list")) {
            sender.sendMessage(ColorUtils.colorize(
                plugin.getLanguageManager().getMessage("command-no-permission")
            ));
            return true;
        }

        var sessions = plugin.getSessionManager().getActiveSessions();

        if (sessions.isEmpty()) {
            sender.sendMessage(ColorUtils.colorize("&eNo active sessions."));
            return true;
        }

        sender.sendMessage(ColorUtils.colorize("&6=== Active Sessions (" + sessions.size() + ") ==="));
        for (var session : sessions) {
            String progress = String.format("%.1f",
                    (double) session.getCurrentPoints() / session.getTargetPoints() * 100);
            String duration = session.getTemplate().getMaxDuration() > 0 ?
                    session.getTemplate().getMaxDuration() + "s" : "∞";
            sender.sendMessage(ColorUtils.colorize(
                    "&e" + session.getSessionId() +
                            " &7- &f" + session.getTemplate().getName() +
                            " &7- &f" + session.getPlayers().size() + " players" +
                            " &7- &f" + progress + "%" +
                            " &7- &f" + duration
            ));
        }

        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("qqcapture.admin.info")) {
            sender.sendMessage(ColorUtils.colorize(
                plugin.getLanguageManager().getMessage("command-no-permission")
            ));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ColorUtils.colorize("&cUsage: /qqcapture info <template>"));
            return true;
        }

        String templateName = args[1];
        Template template = plugin.getConfigManager().getTemplate(templateName);

        if (template == null) {
            sender.sendMessage(ColorUtils.colorize(
                    plugin.getLanguageManager().getMessage("template-not-found")
                            .replace("%template%", templateName)
            ));
            return true;
        }

        sender.sendMessage(ColorUtils.colorize("&6=== Template: " + template.getName() + " ==="));
        sender.sendMessage(ColorUtils.colorize("&eNeed amount: &f" + template.getNeedAmount()));
        sender.sendMessage(ColorUtils.colorize("&eMin players: &f" + template.getMinPlayers()));
        sender.sendMessage(ColorUtils.colorize("&eMax players: &f" + (template.getMaxPlayers() > 0 ? template.getMaxPlayers() : "∞")));
        sender.sendMessage(ColorUtils.colorize("&eMultiplier: &f" + template.getMultiplier()));
        sender.sendMessage(ColorUtils.colorize("&eTeam multiplier: &f" + template.getTeamMultiplier()));
        sender.sendMessage(ColorUtils.colorize("&eRegion: &f" + (template.isRegionEnabled() ? template.getRegionName() : "None")));
        sender.sendMessage(ColorUtils.colorize("&eMax duration: &f" + (template.getMaxDuration() > 0 ? template.getMaxDuration() + "s" : "∞")));
        sender.sendMessage(ColorUtils.colorize("&eStart commands: &f" + template.getStartCommands().size()));
        sender.sendMessage(ColorUtils.colorize("&eTick commands: &f" + template.getTickCommands().size()));
        sender.sendMessage(ColorUtils.colorize("&eEnd commands: &f" + template.getEndCommands().size()));
        sender.sendMessage(ColorUtils.colorize("&eTop storage: &f" + (template.isTopStorageEnabled() ? "Enabled (" + template.getTopStorageDuration() + "s)" : "Disabled")));

        return true;
    }

    private boolean handleTop(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sendTopHelp(sender);
            return true;
        }

        boolean silent = args.length > 0 && args[args.length - 1].equalsIgnoreCase("-s");
        String action = args[1].toLowerCase();
        String templateName = args[2];

        if (!action.equals("reset")) {
            if (!silent) {
                sender.sendMessage(ColorUtils.colorize("&cUnknown top action: " + action));
            }
            return true;
        }

        if (!sender.hasPermission("qqcapture.admin.top")) {
            if (!silent) {
                sender.sendMessage(ColorUtils.colorize(
                        plugin.getLanguageManager().getMessage("command-no-permission")
                ));
            }
            return true;
        }

        if (args.length < 4) {
            if (!silent) {
                sendTopHelp(sender);
            }
            return true;
        }

        String target = args[3];

        if (target.equalsIgnoreCase("clear")) {
            plugin.getTopStorageManager().clearTop(templateName);
            if (!silent) {
                sender.sendMessage(ColorUtils.colorize(
                        plugin.getLanguageManager().getMessage("top-cleared")
                                .replace("%template%", templateName)
                ));
            }
            plugin.getLogger().info("Top data for template '" + templateName + "' cleared by " + sender.getName());
            return true;
        } else {
            if (args.length < 5 || !args[4].equalsIgnoreCase("reset")) {
                if (!silent) {
                    sendTopHelp(sender);
                }
                return true;
            }

            boolean removed = plugin.getTopStorageManager().removePlayerFromTop(templateName, target);
            if (!silent) {
                if (removed) {
                    sender.sendMessage(ColorUtils.colorize(
                            plugin.getLanguageManager().getMessage("top-player-removed")
                                    .replace("%template%", templateName)
                                    .replace("%player%", target)
                    ));
                } else {
                    sender.sendMessage(ColorUtils.colorize(
                            plugin.getLanguageManager().getMessage("top-player-not-found")
                                    .replace("%template%", templateName)
                                    .replace("%player%", target)
                    ));
                }
            }
            plugin.getLogger().info("Player '" + target + "' removed from top data for template '" + templateName + "' by " + sender.getName());
            return true;
        }
    }

    private void sendTopHelp(CommandSender sender) {
        sender.sendMessage(ColorUtils.colorize("&cUsage: /qqcapture top reset <template> clear [-s]"));
        sender.sendMessage(ColorUtils.colorize("&cUsage: /qqcapture top reset <template> <player> reset [-s]"));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ColorUtils.colorize("&6=== QQCapture Commands ==="));
        sender.sendMessage(ColorUtils.colorize("&e/qqcapture reload &7- Reload config and languages"));
        sender.sendMessage(ColorUtils.colorize("&e/qqcapture start <template> [-p:points] [-d:duration] [-r:region] [-m:multiplier] [-mt:teamMultiplier] [-t:tickCapture] [-s] &7- Start an event"));
        sender.sendMessage(ColorUtils.colorize("&e/qqcapture stop <template> &7- Stop a session by template name"));
        sender.sendMessage(ColorUtils.colorize("&e/qqcapture list &7- List active sessions"));
        sender.sendMessage(ColorUtils.colorize("&e/qqcapture info <template> &7- Show template info"));
        sender.sendMessage(ColorUtils.colorize("&e/qqcapture top reset <template> clear [-s] &7- Clear top data"));
        sender.sendMessage(ColorUtils.colorize("&e/qqcapture top reset <template> <player> reset [-s] &7- Remove player from top"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>(Arrays.asList("reload", "start", "stop", "list", "info", "top"));
            for (String sub : subCommands) {
                if (sub.startsWith(args[0].toLowerCase())) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("top")) {
                completions.add("reset");
            } else if (args[0].equalsIgnoreCase("start") || args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("stop")) {
                for (String templateName : plugin.getConfigManager().getTemplateNames()) {
                    if (templateName.toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(templateName);
                    }
                }
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("start")) {
                for (String templateName : plugin.getConfigManager().getTemplateNames()) {
                    if (templateName.toLowerCase().startsWith(args[2].toLowerCase())) {
                        completions.add(templateName);
                    }
                }
            } else if (args[0].equalsIgnoreCase("top") && args[1].equalsIgnoreCase("reset")) {
                completions.add("clear");
                for (String templateName : plugin.getConfigManager().getTemplateNames()) {
                    if (templateName.toLowerCase().startsWith(args[2].toLowerCase())) {
                        completions.add(templateName);
                    }
                }
            }
        } else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("top") && args[1].equalsIgnoreCase("reset")) {
                completions.add("clear");
                completions.add("-s");
            }
        } else if (args.length >= 4 && args[0].equalsIgnoreCase("start")) {
            String lastArg = args[args.length - 1];
            String[] options = {"-p:", "-d:", "-r:", "-m:", "-mt:", "-t:", "-s"};
            for (String option : options) {
                if (option.startsWith(lastArg.toLowerCase())) {
                    completions.add(option);
                }
            }
            if (lastArg.startsWith("-p:")) {
                completions.add("-p:1000");
                completions.add("-p:10000");
                completions.add("-p:100000");
                completions.add("-p:1000000");
            }
            if (lastArg.startsWith("-d:")) {
                completions.add("-d:60");
                completions.add("-d:120");
                completions.add("-d:300");
            }
            if (lastArg.startsWith("-t:")) {
                completions.add("-t:10");
                completions.add("-t:20");
                completions.add("-t:40");
            }
        } else if (args.length >= 5 && args[0].equalsIgnoreCase("top") && args[1].equalsIgnoreCase("reset")) {
            String lastArg = args[args.length - 1];
            if ("-s".startsWith(lastArg.toLowerCase())) {
                completions.add("-s");
            }
        }

        return completions;
    }
}
