package pl.lifesteal;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class LifestealCommand implements TabExecutor {

    private static final List<String> SUBCOMMANDS = List.of("on", "off", "status", "oznacz", "ustawitem");

    private final HeartManager hearts;

    public LifestealCommand(HeartManager hearts) {
        this.hearts = hearts;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            usage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "on" -> setEnabled(sender, true);
            case "off" -> setEnabled(sender, false);
            case "status" -> status(sender);
            case "oznacz" -> mark(sender);
            case "ustawitem" -> setItem(sender);
            default -> usage(sender);
        }
        return true;
    }

    private void usage(CommandSender sender) {
        sender.sendMessage(Component.text("Użycie: /lifesteal <on|off|status|oznacz|ustawitem>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("  on / off    - włącza / wyłącza Lifesteal", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  status      - pokazuje aktualne ustawienia", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  oznacz      - przedmiot z ręki zyskuje właściwość: PPM = +1 serce", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  ustawitem   - przedmiot z ręki będzie wypłacany przez /wyplac", NamedTextColor.GRAY));
    }

    private void setEnabled(CommandSender sender, boolean enabled) {
        hearts.setEnabled(enabled);
        for (Player player : Bukkit.getOnlinePlayers()) {
            hearts.apply(player);
        }
        if (enabled) {
            sender.sendMessage(Component.text("Lifesteal został WŁĄCZONY.", NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text(
                    "Lifesteal został WYŁĄCZONY. Wszyscy mają zwykłe 20 HP, a zapisane serca wrócą po włączeniu.",
                    NamedTextColor.RED));
        }
    }

    private void status(CommandSender sender) {
        ItemStack base = hearts.getWithdrawItemBase();
        String withdrawItem = base == null ? "domyślny (NETHER_STAR)" : base.getType().name();

        sender.sendMessage(Component.text("Lifesteal: " + (hearts.isEnabled() ? "WŁĄCZONY" : "WYŁĄCZONY"),
                hearts.isEnabled() ? NamedTextColor.GREEN : NamedTextColor.RED));
        sender.sendMessage(Component.text("Serca na start: " + hearts.startingHearts()
                + ", maksimum: " + hearts.maxHearts(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Przedmiot wypłacany: " + withdrawItem, NamedTextColor.GRAY));
    }

    private void mark(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Ta komenda jest tylko dla graczy.", NamedTextColor.RED));
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            player.sendMessage(Component.text("Musisz trzymać przedmiot w ręce.", NamedTextColor.RED));
            return;
        }
        if (hearts.isHeartItem(item)) {
            player.sendMessage(Component.text("Ten przedmiot już jest sercem.", NamedTextColor.YELLOW));
            return;
        }

        ItemStack marked = item.clone();
        hearts.markAsHeart(marked);
        player.getInventory().setItemInMainHand(marked);

        player.sendMessage(Component.text(
                "Przedmiot (cały stack) zyskał właściwość: PPM = +1 serce.", NamedTextColor.GREEN));
    }

    private void setItem(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Ta komenda jest tylko dla graczy.", NamedTextColor.RED));
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            player.sendMessage(Component.text("Musisz trzymać przedmiot w ręce.", NamedTextColor.RED));
            return;
        }

        hearts.setWithdrawItem(item);
        player.sendMessage(Component.text(
                "Od teraz /wyplac wypłaca ten przedmiot (" + item.getType().name() + ").", NamedTextColor.GREEN));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            return SUBCOMMANDS.stream().filter(s -> s.startsWith(prefix)).toList();
        }
        return List.of();
    }
}
