package pl.lifesteal;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class AdminWithdrawCommand implements CommandExecutor, TabCompleter {

    private final HeartManager hearts;

    public AdminWithdrawCommand(HeartManager hearts) {
        this.hearts = hearts;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player admin)) {
            sender.sendMessage(Component.text(
                    "Tej komendy może użyć tylko gracz (przedmioty trafiają do ręki).", NamedTextColor.RED));
            return true;
        }

        if (!hearts.isEnabled()) {
            admin.sendMessage(Component.text("Lifesteal jest wyłączony.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            admin.sendMessage(Component.text("Użycie: /wyplacgraczowi <gracz> <ilość>", NamedTextColor.YELLOW));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            admin.sendMessage(Component.text(
                    "Nie znaleziono gracza \"" + args[0] + "\" (musi być online).", NamedTextColor.RED));
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            admin.sendMessage(Component.text("Ilość musi być liczbą całkowitą.", NamedTextColor.RED));
            return true;
        }

        if (amount < 1) {
            admin.sendMessage(Component.text("Ilość musi być większa od zera.", NamedTextColor.RED));
            return true;
        }

        int current = hearts.getHearts(target);
        if (current < 1) {
            admin.sendMessage(Component.text(
                    target.getName() + " nie ma żadnych serc do wypłacenia.", NamedTextColor.RED));
            return true;
        }

        int toTake = Math.min(amount, current);

        hearts.setHearts(target, current - toTake);
        for (int i = 0; i < toTake; i++) {
            hearts.giveItem(admin, hearts.createHeartItem());
        }

        admin.sendMessage(Component.text(
                "Zabrano " + toTake + " serc(a) graczowi " + target.getName()
                        + " i dodano je do Twojego ekwipunku.", NamedTextColor.GREEN));

        if (toTake < amount) {
            admin.sendMessage(Component.text(
                    target.getName() + " miał tylko " + current + " serc, więcej nie można było zabrać.",
                    NamedTextColor.YELLOW));
        }

        target.sendMessage(Component.text(
                "Admin " + admin.getName() + " zabrał Ci " + toTake + " serc(a). Zostało Ci: "
                        + (current - toTake), NamedTextColor.RED));

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(prefix))
                    .toList();
        }
        if (args.length == 2) {
            return List.of("1", "5", "10");
        }
        return List.of();
    }
}
