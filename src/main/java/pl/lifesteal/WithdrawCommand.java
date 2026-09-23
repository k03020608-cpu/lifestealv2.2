package pl.lifesteal;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class WithdrawCommand implements CommandExecutor {

    private final HeartManager hearts;

    public WithdrawCommand(HeartManager hearts) {
        this.hearts = hearts;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Ta komenda jest tylko dla graczy.", NamedTextColor.RED));
            return true;
        }

        if (!hearts.isEnabled()) {
            player.sendMessage(Component.text("Lifesteal jest wyłączony.", NamedTextColor.RED));
            return true;
        }

        int amount = 1;
        if (args.length >= 1) {
            try {
                amount = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text("Użycie: /wyplac [ilość]", NamedTextColor.YELLOW));
                return true;
            }
        }

        if (amount < 1) {
            player.sendMessage(Component.text("Ilość musi być większa od zera.", NamedTextColor.RED));
            return true;
        }

        int current = hearts.getHearts(player);
        int min = hearts.minHeartsAfterWithdraw();
        int maxWithdraw = current - min;

        if (maxWithdraw < 1) {
            player.sendMessage(Component.text(
                    "Nie możesz wypłacić serc, musisz zostawić sobie co najmniej " + min + ".",
                    NamedTextColor.RED));
            return true;
        }
        if (amount > maxWithdraw) {
            player.sendMessage(Component.text(
                    "Możesz wypłacić maksymalnie " + maxWithdraw + " (musisz zostawić sobie co najmniej "
                            + min + ").", NamedTextColor.RED));
            return true;
        }

        hearts.setHearts(player, current - amount);
        for (int i = 0; i < amount; i++) {
            hearts.giveItem(player, hearts.createHeartItem());
        }

        player.sendMessage(Component.text(
                "Wypłacono serca: " + amount + ". Zostało Ci: " + (current - amount), NamedTextColor.GREEN));
        return true;
    }
}
