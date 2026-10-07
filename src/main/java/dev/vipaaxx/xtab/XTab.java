package dev.vipaaxx.xtab;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;


@SuppressWarnings("deprecation")
public final class XTab extends JavaPlugin implements Listener {

    private BukkitTask task;
    private boolean papi;
    private String headerRaw = "";
    private String footerRaw = "";
    private String staticHeader; // non-null when no placeholders are used
    private String staticFooter;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        load();
    }

    @Override
    public void onDisable() {
        if (task != null) task.cancel();
        for (Player p : Bukkit.getOnlinePlayers()) p.setPlayerListHeaderFooter("", "");
    }

    private void load() {
        reloadConfig();
        if (task != null) task.cancel();

        papi = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        headerRaw = String.join("\n", getConfig().getStringList("header"));
        footerRaw = String.join("\n", getConfig().getStringList("footer"));
        staticHeader = (papi && headerRaw.contains("%")) ? null : TextFormatter.format(headerRaw);
        staticFooter = (papi && footerRaw.contains("%")) ? null : TextFormatter.format(footerRaw);

        // Config value is in milliseconds; a server tick is 50ms, so the minimum is 1 tick.
        long ms = Math.max(1, getConfig().getLong("refresh", 50));
        long ticks = Math.max(1L, Math.round(ms / 50.0));
        task = Bukkit.getScheduler().runTaskTimer(this, this::updateAll, 0L, ticks);
    }

    private void updateAll() {
        for (Player p : Bukkit.getOnlinePlayers()) update(p);
    }

    private void update(Player p) {
        String header = staticHeader != null ? staticHeader : TextFormatter.format(PapiHook.apply(p, headerRaw));
        String footer = staticFooter != null ? staticFooter : TextFormatter.format(PapiHook.apply(p, footerRaw));
        p.setPlayerListHeaderFooter(header, footer);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        update(e.getPlayer());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("xtab.reload")) {
                sender.sendMessage("\u00a7cYou don't have permission.");
                return true;
            }
            load();
            sender.sendMessage("\u00a7axTab reloaded.");
            return true;
        }
        sender.sendMessage("\u00a77Usage: \u00a7f/xtab reload");
        return true;
    }
}
