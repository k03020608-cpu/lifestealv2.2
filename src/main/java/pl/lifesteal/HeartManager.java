package pl.lifesteal;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Cała logika serc: ile serc ma gracz, ustawianie maksymalnego zdrowia
 * oraz tworzenie i rozpoznawanie przedmiotów-serc.
 */
public final class HeartManager {

    private final LifestealPlugin plugin;
    private final NamespacedKey heartsKey;
    private final NamespacedKey itemKey;

    public HeartManager(LifestealPlugin plugin) {
        this.plugin = plugin;
        this.heartsKey = new NamespacedKey(plugin, "hearts");
        this.itemKey = new NamespacedKey(plugin, "heart_item");
    }

    // ---------- Ustawienia ----------

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("enabled", true);
    }

    public void setEnabled(boolean enabled) {
        plugin.getConfig().set("enabled", enabled);
        plugin.saveConfig();
    }

    public int startingHearts() {
        return Math.max(1, plugin.getConfig().getInt("starting-hearts", 10));
    }

    public int maxHearts() {
        return Math.max(1, plugin.getConfig().getInt("max-hearts", 20));
    }

    public int minHeartsAfterWithdraw() {
        return Math.max(1, plugin.getConfig().getInt("min-hearts-after-withdraw", 1));
    }

    public int reviveHearts() {
        return Math.max(1, plugin.getConfig().getInt("revive-hearts", 3));
    }

    // ---------- Serca gracza ----------

    public int getHearts(Player player) {
        return player.getPersistentDataContainer()
                .getOrDefault(heartsKey, PersistentDataType.INTEGER, startingHearts());
    }

    public void setHearts(Player player, int hearts) {
        int clamped = Math.max(0, Math.min(hearts, maxHearts()));
        player.getPersistentDataContainer().set(heartsKey, PersistentDataType.INTEGER, clamped);
        apply(player);
    }

    /**
     * Ustawia maksymalne zdrowie gracza. Gdy Lifesteal jest wyłączony,
     * gracz ma zwykłe 20 HP (zapisane serca wracają po ponownym włączeniu).
     */
    public void apply(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);
        if (attribute == null) {
            return;
        }

        double max = isEnabled() ? Math.max(1, getHearts(player)) * 2.0 : 20.0;
        attribute.setBaseValue(max);

        double effectiveMax = attribute.getValue();
        if (player.getHealth() > effectiveMax) {
            player.setHealth(effectiveMax);
        }
    }

    // ---------- Przedmioty-serca ----------

    public boolean isHeartItem(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(itemKey, PersistentDataType.BYTE);
    }

    /** Nadaje przedmiotowi właściwość: PPM = +1 serce. */
    public void markAsHeart(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return;
        }
        item.editMeta(meta -> {
            meta.getPersistentDataContainer().set(itemKey, PersistentDataType.BYTE, (byte) 1);

            List<Component> existing = meta.lore();
            List<Component> lore = existing == null ? new ArrayList<>() : new ArrayList<>(existing);
            lore.add(Component.text("Kliknij PPM, aby zdobyć serce", NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
        });
    }

    /** Zapamiętuje przedmiot, który będzie wypłacany przez /wyplac. */
    public void setWithdrawItem(ItemStack held) {
        ItemStack copy = held.clone();
        copy.setAmount(1);
        plugin.getConfig().set("withdraw-item", copy);
        plugin.saveConfig();
    }

    public ItemStack getWithdrawItemBase() {
        ItemStack stored = plugin.getConfig().getItemStack("withdraw-item");
        if (stored == null || stored.getType().isAir()) {
            return null;
        }
        return stored;
    }

    /** Tworzy jedną sztukę przedmiotu-serca (ustawiony przedmiot albo domyślna Gwiazda Netheru). */
    public ItemStack createHeartItem() {
        ItemStack base = getWithdrawItemBase();
        ItemStack item;

        if (base == null) {
            item = new ItemStack(Material.NETHER_STAR);
            item.editMeta(meta -> meta.displayName(
                    Component.text("Serce", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)));
        } else {
            item = base.clone();
        }

        item.setAmount(1);
        if (!isHeartItem(item)) {
            markAsHeart(item);
        }
        return item;
    }

    public void giveItem(Player player, ItemStack item) {
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        for (ItemStack rest : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), rest);
        }
    }
}
