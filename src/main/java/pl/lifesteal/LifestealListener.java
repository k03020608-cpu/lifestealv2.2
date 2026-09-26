package pl.lifesteal;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class LifestealListener implements Listener {

    private final LifestealPlugin plugin;
    private final HeartManager hearts;

    public LifestealListener(LifestealPlugin plugin, HeartManager hearts) {
        this.plugin = plugin;
        this.hearts = hearts;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Gracz wraca po banie za utratę wszystkich serc
        if (hearts.isEnabled() && hearts.getHearts(player) <= 0) {
            hearts.setHearts(player, hearts.reviveHearts());
            player.sendMessage(Component.text(
                    "Wracasz do gry z " + hearts.getHearts(player) + " sercami.", NamedTextColor.YELLOW));
            return;
        }

        hearts.apply(player);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> hearts.apply(player));
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (!hearts.isEnabled()) {
            return;
        }

        Player victim = event.getEntity();

        // Gracz w trakcie pojedynku (plugin Pojedynek) nie traci ani nie zyskuje serca za tę śmierć.
        if (victim.hasMetadata("wPojedynku")) {
            return;
        }

        Player killer = victim.getKiller();

        if (killer == null && !plugin.getConfig().getBoolean("lose-heart-on-natural-death", true)) {
            return;
        }

        boolean ban = plugin.getConfig().getBoolean("ban-at-zero-hearts", true);
        int current = hearts.getHearts(victim);

        // Ostatnie serce i bany wyłączone: gracz nic nie traci (i nikt go nie okrada)
        if (current <= 1 && !ban) {
            return;
        }

        int after = current - 1;
        hearts.setHearts(victim, after);

        if (after <= 0) {
            String name = victim.getName();
            String reason = plugin.getConfig().getString("ban-reason", "Straciłeś wszystkie serca!");
            Bukkit.getScheduler().runTask(plugin,
                    () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ban " + name + " " + reason));
        } else {
            victim.sendMessage(Component.text(
                    "Straciłeś serce! Zostało Ci: " + after, NamedTextColor.RED));
        }

        if (killer != null) {
            int killerHearts = hearts.getHearts(killer);
            if (killerHearts < hearts.maxHearts()) {
                hearts.setHearts(killer, killerHearts + 1);
                killer.sendMessage(Component.text(
                        "Zabiłeś gracza " + victim.getName() + " i zyskujesz serce! Masz ich teraz: "
                                + (killerHearts + 1), NamedTextColor.GREEN));
            } else {
                hearts.giveItem(killer, hearts.createHeartItem());
                killer.sendMessage(Component.text(
                        "Masz już maksymalną liczbę serc, więc dostajesz serce jako przedmiot.",
                        NamedTextColor.YELLOW));
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!hearts.isHeartItem(item)) {
            return;
        }

        // Nie pozwól np. postawić bloku, który jest sercem
        event.setCancelled(true);

        if (!hearts.isEnabled()) {
            player.sendMessage(Component.text("Lifesteal jest wyłączony.", NamedTextColor.RED));
            return;
        }

        int current = hearts.getHearts(player);
        if (current >= hearts.maxHearts()) {
            player.sendMessage(Component.text(
                    "Masz już maksymalną liczbę serc (" + hearts.maxHearts() + ").", NamedTextColor.YELLOW));
            return;
        }

        int amount = item.getAmount();
        if (amount <= 1) {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        } else {
            ItemStack rest = item.clone();
            rest.setAmount(amount - 1);
            player.getInventory().setItemInMainHand(rest);
        }

        hearts.setHearts(player, current + 1);
        player.sendMessage(Component.text(
                "Zyskujesz serce! Masz ich teraz: " + (current + 1), NamedTextColor.GREEN));
    }
}
