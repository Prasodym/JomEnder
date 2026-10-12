package de.jomender.recipe;

import com.mojang.serialization.MapCodec;
import de.jomender.ModDataComponents;
import de.jomender.ModItems;
import de.jomender.ModRecipes;
import de.jomender.armor.ArmorUpgrade;
import de.jomender.armor.ArmorUpgrades;
import de.jomender.armor.EndariumArmorItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Optional;

/**
 * Schmiedetisch-Rezept für alle Rüstungs-Upgrades ("type": "jomender:armor_upgrade").
 * Ohne Vorlage: leerer Vorlagen-Slot + Endarium-Rüstungsteil + Upgrade-Item(s).
 * Welche Upgrades es gibt, steht in {@link ArmorUpgrade}.
 */
public class ArmorUpgradeRecipe implements SmithingRecipe {
    public static final ArmorUpgradeRecipe INSTANCE = new ArmorUpgradeRecipe();
    public static final MapCodec<ArmorUpgradeRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, ArmorUpgradeRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<ArmorUpgradeRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private @Nullable Ingredient base;
    private @Nullable Ingredient addition;

    private ArmorUpgradeRecipe() {}

    /** Das passende Upgrade für die aktuelle Belegung, oder null. */
    private static @Nullable ArmorUpgrade upgradeFor(SmithingRecipeInput input) {
        ItemStack armor = input.base();
        if (!input.template().isEmpty() || !(armor.getItem() instanceof EndariumArmorItem)) {
            return null;
        }
        ArmorUpgrade upgrade = ArmorUpgrade.find(EndariumArmorItem.slotOf(armor), input.addition());
        if (upgrade == null
                || input.addition().getCount() < upgrade.cost()
                || ArmorUpgrades.of(armor).level(upgrade) >= upgrade.maxLevel()) {
            return null;
        }
        return upgrade;
    }

    @Override
    public boolean matches(SmithingRecipeInput input, Level level) {
        return upgradeFor(input) != null;
    }

    @Override
    public ItemStack assemble(SmithingRecipeInput input) {
        ArmorUpgrade upgrade = upgradeFor(input);
        if (upgrade == null) return ItemStack.EMPTY;
        ItemStack result = input.base().copyWithCount(1);
        ArmorUpgrades upgrades = ArmorUpgrades.of(result);
        result.set(ModDataComponents.ARMOR_UPGRADES.get(), upgrades.with(upgrade, upgrades.level(upgrade) + 1));
        return result;
    }

    // Diese drei bestimmen, welche Items der Schmiedetisch in welchen Slot lässt
    @Override
    public Optional<Ingredient> templateIngredient() {
        return Optional.empty();
    }

    @Override
    public Ingredient baseIngredient() {
        if (base == null) {
            base = Ingredient.of(ModItems.ENDARIUM_HELMET.get(), ModItems.ENDARIUM_CHESTPLATE.get(),
                    ModItems.ENDARIUM_LEGGINGS.get(), ModItems.ENDARIUM_BOOTS.get());
        }
        return base;
    }

    @Override
    public Optional<Ingredient> additionIngredient() {
        if (addition == null) {
            addition = Ingredient.of(Arrays.stream(ArmorUpgrade.values()).map(ArmorUpgrade::item).distinct());
        }
        return Optional.of(addition);
    }

    @Override
    public boolean isSpecial() { return true; }

    @Override
    public boolean showNotification() { return false; }

    @Override
    public String group() { return ""; }

    @Override
    public RecipeSerializer<ArmorUpgradeRecipe> getSerializer() { return ModRecipes.ARMOR_UPGRADE_SERIALIZER.get(); }

    @Override
    public PlacementInfo placementInfo() { return PlacementInfo.NOT_PLACEABLE; }
}
