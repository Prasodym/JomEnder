package de.jomender.misc;

import de.jomender.ModEffects;
import de.jomender.ModItems;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Easter Egg: Wer Uran in der Hand hält, wird verstrahlt.
 * Der Effekt wird alle 10 Ticks auf 5 Sekunden aufgefrischt -
 * legt man das Uran weg, ist er spätestens 5 Sekunden später vorbei.
 */
public final class RadiationHandler {
    private RadiationHandler() {}

    private static final int DURATION = 100;       // 5 Sekunden
    private static final int REFRESH_INTERVAL = 10;

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player.tickCount % REFRESH_INTERVAL != 0) {
            return;
        }
        if (player.isCreative() || player.isSpectator()) {
            return;
        }

        boolean block = holds(player, true);
        boolean ingot = holds(player, false);

        // ambient=false, Partikel sichtbar, Icon sichtbar
        if (block) {
            // Block: stärkere Strahlung, ersetzt die schwache
            player.removeEffect(ModEffects.RADIOACTIVE);
            player.addEffect(new MobEffectInstance(ModEffects.HIGHLY_RADIOACTIVE, DURATION, 0, false, true, true), null);
        } else if (ingot && !player.hasEffect(ModEffects.HIGHLY_RADIOACTIVE)) {
            player.addEffect(new MobEffectInstance(ModEffects.RADIOACTIVE, DURATION, 0, false, true, true), null);
        }
    }

    /** Hält der Spieler Uran-Block (block=true) bzw. Uran-Barren in einer Hand? */
    private static boolean holds(Player player, boolean block) {
        return isUranium(player.getMainHandItem(), block) || isUranium(player.getOffhandItem(), block);
    }

    private static boolean isUranium(ItemStack stack, boolean block) {
        // Atommüll strahlt so stark wie ein Uranblock
        return block
                ? stack.is(ModItems.URANIUM_BLOCK.get()) || stack.is(ModItems.NUCLEAR_WASTE.get())
                : stack.is(ModItems.Uranium_Ingot.get());
    }
}
