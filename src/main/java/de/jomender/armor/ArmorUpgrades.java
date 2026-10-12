package de.jomender.armor;

import com.mojang.serialization.Codec;
import de.jomender.ModDataComponents;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/** Daten-Komponente auf dem Rüstungsteil: Upgrade-ID -> Stufe. */
public record ArmorUpgrades(Map<String, Integer> levels) {
    public static final ArmorUpgrades EMPTY = new ArmorUpgrades(Map.of());

    public static final Codec<ArmorUpgrades> CODEC =
            Codec.unboundedMap(Codec.STRING, Codec.INT).xmap(ArmorUpgrades::new, ArmorUpgrades::levels);

    public static final StreamCodec<ByteBuf, ArmorUpgrades> STREAM_CODEC =
            ByteBufCodecs.map(HashMap<String, Integer>::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT)
                    .map(ArmorUpgrades::new, upgrades -> new HashMap<>(upgrades.levels()));

    public ArmorUpgrades {
        levels = Map.copyOf(levels);
    }

    public static ArmorUpgrades of(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ARMOR_UPGRADES.get(), EMPTY);
    }

    public int level(ArmorUpgrade upgrade) {
        return levels.getOrDefault(upgrade.id(), 0);
    }

    public boolean isEmpty() {
        return levels.isEmpty();
    }

    public ArmorUpgrades with(ArmorUpgrade upgrade, int level) {
        Map<String, Integer> copy = new HashMap<>(levels);
        copy.put(upgrade.id(), level);
        return new ArmorUpgrades(copy);
    }
}
