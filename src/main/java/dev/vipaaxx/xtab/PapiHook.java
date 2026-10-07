package dev.vipaaxx.xtab;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

/** Only ever touched when PlaceholderAPI is installed, so the class is never loaded otherwise. */
final class PapiHook {
    private PapiHook() {}

    static String apply(Player player, String text) {
        return PlaceholderAPI.setPlaceholders(player, text);
    }
}
