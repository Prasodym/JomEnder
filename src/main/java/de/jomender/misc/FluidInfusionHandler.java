package de.jomender.misc;

import de.jomender.ModParticles;
import de.jomender.ModRecipes;
import de.jomender.recipe.FluidInfusionInput;
import de.jomender.recipe.FluidInfusionRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Flüssigkeits-Infusion: Liegen die Items eines Rezepts zusammen im selben Flüssigkeitsblock,
 * startet eine Animation (Funkenspirale, Klänge, Items werden zur Mitte gezogen).
 * Danach werden die Zutaten verbraucht und das Ergebnis springt aus der Flüssigkeit.
 */
public final class FluidInfusionHandler {
    private FluidInfusionHandler() {}

    /** Wie oft geprüft wird (Ticks). */
    private static final int CHECK_INTERVAL = 5;

    private static final class Progress {
        final ResourceKey<Recipe<?>> recipe;
        int ticks;
        long lastTick;

        Progress(ResourceKey<Recipe<?>> recipe, long now) {
            this.recipe = recipe;
            this.lastTick = now;
        }
    }

    /** Laufende Infusionen pro Dimension und Block. */
    private static final Map<ResourceKey<Level>, Map<BlockPos, Progress>> RUNNING = new HashMap<>();

    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ItemEntity entity)
                || !(entity.level() instanceof ServerLevel level)
                || entity.tickCount % CHECK_INTERVAL != 0) {
            return;
        }

        BlockPos pos = entity.blockPosition();
        FluidState fluid = level.getFluidState(pos);
        if (fluid.isEmpty()) return;

        long now = level.getGameTime();
        Map<BlockPos, Progress> running = RUNNING.computeIfAbsent(level.dimension(), k -> new HashMap<>());
        cleanUp(running, now);

        Progress progress = running.get(pos);
        if (progress != null && progress.lastTick == now) return; // diesen Block hat schon ein anderes Item bearbeitet

        List<ItemEntity> entities = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos), ItemEntity::isAlive);
        List<ItemStack> stacks = entities.stream().map(ItemEntity::getItem).toList();
        Optional<RecipeHolder<FluidInfusionRecipe>> match = level.recipeAccess()
                .getRecipeFor(ModRecipes.FLUID_INFUSION_TYPE.get(), new FluidInfusionInput(fluid.getType(), stacks), level);

        if (match.isEmpty()) {
            running.remove(pos);
            return;
        }

        RecipeHolder<FluidInfusionRecipe> holder = match.get();
        if (progress == null || !progress.recipe.equals(holder.id()) || now - progress.lastTick > CHECK_INTERVAL * 3) {
            progress = new Progress(holder.id(), now);
            running.put(pos, progress);
        } else {
            progress.ticks += (int) (now - progress.lastTick);
            progress.lastTick = now;
        }

        FluidInfusionRecipe recipe = holder.value();
        float done = Math.min(1F, progress.ticks / (float) recipe.time());
        animate(level, pos, entities, progress.ticks, done);

        if (progress.ticks >= recipe.time()) {
            running.remove(pos);
            finish(level, pos, entities, recipe);
        }
    }

    // =========================
    // ANIMATION
    // =========================

    private static void animate(ServerLevel level, BlockPos pos, List<ItemEntity> entities, int ticks, float done) {
        Vec3 center = Vec3.atCenterOf(pos);

        // Items sanft zur Mitte ziehen, damit sie zusammen "verschmelzen"
        for (ItemEntity entity : entities) {
            Vec3 pull = center.subtract(entity.position()).multiply(0.08, 0.0, 0.08);
            entity.setDeltaMovement(entity.getDeltaMovement().scale(0.6).add(pull));
            entity.setPickUpDelay(20); // während der Infusion nicht aufsammelbar
        }

        // Drei Funkenarme, die sich drehen und mit dem Fortschritt nach oben wandern
        double radius = 0.65 - done * 0.4;
        double y = pos.getY() + 0.2 + done * 0.7;
        for (int arm = 0; arm < 3; arm++) {
            double angle = ticks * 0.35 + arm * (Math.PI * 2 / 3);
            level.sendParticles(ParticleTypes.END_ROD,
                    center.x + Math.cos(angle) * radius, y, center.z + Math.sin(angle) * radius,
                    1, 0, 0, 0, 0);
        }
        level.sendParticles(ParticleTypes.ENCHANT, center.x, pos.getY() + 1.2, center.z, 4, 0.3, 0.2, 0.3, 0.6);

        // Steigender Ton
        if (ticks % 15 == 0) {
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.8F, 0.6F + done);
        }
    }

    // =========================
    // FERTIG
    // =========================

    private static void finish(ServerLevel level, BlockPos pos, List<ItemEntity> entities, FluidInfusionRecipe recipe) {
        // Zutaten verbrauchen
        for (FluidInfusionRecipe.Entry entry : recipe.ingredients()) {
            int remaining = entry.count();
            for (ItemEntity entity : entities) {
                if (remaining <= 0) break;
                ItemStack stack = entity.getItem();
                if (!entity.isAlive() || stack.isEmpty() || !entry.item().test(stack)) continue;
                int take = Math.min(remaining, stack.getCount());
                ItemStack rest = stack.copy();
                rest.shrink(take);
                remaining -= take;
                if (rest.isEmpty()) entity.discard();
                else entity.setItem(rest);
            }
        }

        // Ergebnis springt aus der Flüssigkeit
        Vec3 center = Vec3.atCenterOf(pos);
        ItemEntity result = new ItemEntity(level, center.x, pos.getY() + 0.8, center.z, recipe.resultStack());
        result.setDeltaMovement(0, 0.35, 0);
        result.setPickUpDelay(10);
        level.addFreshEntity(result);

        // Flüssigkeit verbrauchen (nur wenn im Rezept gewünscht)
        if (recipe.consumeFluid()) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof LiquidBlock) {
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            } else if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
                level.setBlockAndUpdate(pos, state.setValue(BlockStateProperties.WATERLOGGED, false));
            }
        }

        // Effekt-Explosion
        level.sendParticles(ParticleTypes.END_ROD, center.x, pos.getY() + 0.8, center.z, 40, 0.1, 0.1, 0.1, 0.18);
        level.sendParticles(ModParticles.ENDARIUM_LIGHTNING.get(), center.x, pos.getY() + 0.9, center.z, 12, 0.35, 0.35, 0.35, 0.05);
        level.sendParticles(ParticleTypes.ENCHANT, center.x, pos.getY() + 1.0, center.z, 60, 0.4, 0.4, 0.4, 1.0);
        level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.2F);
        level.playSound(null, pos, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.BLOCKS, 1.0F, 0.8F);
    }

    /** Alte Einträge entfernen (Items wurden herausgeholt). */
    private static void cleanUp(Map<BlockPos, Progress> running, long now) {
        if (now % 100 == 0) {
            running.values().removeIf(p -> now - p.lastTick > 100);
        }
    }
}
