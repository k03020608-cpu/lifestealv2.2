package pl.lifesteal;

import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class LifestealPlugin extends JavaPlugin {

    private HeartManager hearts;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        hearts = new HeartManager(this);

        getServer().getPluginManager().registerEvents(new LifestealListener(this, hearts), this);

        LifestealCommand adminCommand = new LifestealCommand(hearts);
        PluginCommand lifesteal = getCommand("lifesteal");
        if (lifesteal != null) {
            lifesteal.setExecutor(adminCommand);
            lifesteal.setTabCompleter(adminCommand);
        }

        PluginCommand wyplac = getCommand("wyplac");
        if (wyplac != null) {
            wyplac.setExecutor(new WithdrawCommand(hearts));
        }

        // Na wypadek przeładowania pluginu, gdy gracze są już online
        for (Player player : getServer().getOnlinePlayers()) {
            hearts.apply(player);
        }

        getLogger().info("Lifesteal jest " + (hearts.isEnabled() ? "WŁĄCZONY" : "WYŁĄCZONY") + ".");
    }
}
