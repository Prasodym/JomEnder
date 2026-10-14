package de.jomender.entity;

import de.jomender.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Galaxius - ein Zombie-Magier aus dem All.
 * <ul>
 *   <li>100 Lebenspunkte, komplette Endarium-Rüstung und Endarium-Schwert (wird nicht gedroppt)</li>
 *   <li>Ignoriert Licht: brennt nicht in der Sonne und spawnt (z.B. aus dem Spawner) bei jeder Helligkeit</li>
 *   <li>Zauber: Sternenschlag, Schwerkraft-Sog, Sprung durch den Raum, Sternen-Nova</li>
 *   <li>Lila Boss-Leiste, Drops über loot_table/entities/galaxius.json (Galaxis-Quarz)</li>
 * </ul>
 */
public class GalaxiusEntity extends Zombie {

    // Abklingzeiten in Ticks (20 Ticks = 1 Sekunde)
    private static final int STAR_STRIKE_COOLDOWN = 100;
    private static final int STAR_STRIKE_DELAY = 20;       // Vorwarnung, bevor der Blitz einschlägt
    private static final int GRAVITY_COOLDOWN = 160;
    private static final int BLINK_COOLDOWN = 120;
    private static final int NOVA_COOLDOWN = 80;

    private static final float STAR_STRIKE_DAMAGE = 8F;
    private static final float NOVA_DAMAGE = 5F;

    private final ServerBossEvent bossBar = new ServerBossEvent(java.util.UUID.randomUUID(),
            Component.translatable("entity.jomender.galaxius"),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);

    private int strikeCooldown = 60;
    private int gravityCooldown = 100;
    private int blinkCooldown = 80;
    private int novaCooldown = 40;
    private @Nullable Vec3 pendingStrike;
    private int pendingStrikeTicks;

    public GalaxiusEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        this.xpReward = 50;
        setCanPickUpLoot(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 100.0)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0.0);
    }

    // =========================
    // AUSRÜSTUNG UND SPAWN
    // =========================

    @Override
    protected void populateDefaultEquipmentSlots(net.minecraft.util.RandomSource random, DifficultyInstance difficulty) {
        equipGalaxius();
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        EntitySpawnReason reason, @Nullable SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data);
        setBaby(false);
        equipGalaxius();
        setHealth(getMaxHealth());
        return result;
    }

    private void equipGalaxius() {
        setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.ENDARIUM_HELMET.get()));
        setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.ENDARIUM_CHESTPLATE.get()));
        setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.ENDARIUM_LEGGINGS.get()));
        setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.ENDARIUM_BOOTS.get()));
        // Schwert nur zur Zierde: ohne seine 50 Schaden, sonst wäre jeder Treffer tödlich.
        // Den Nahkampfschaden regelt ATTACK_DAMAGE in createAttributes().
        ItemStack sword = new ItemStack(ModItems.ENDARIUM_SHWORD.get());
        sword.set(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        setItemSlot(EquipmentSlot.MAINHAND, sword);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            setDropChance(slot, 0F);   // Endarium-Ausrüstung wird nie gedroppt
        }
    }

    /** Kein Baby-Galaxius. */
    @Override
    public void setBaby(boolean baby) {
        super.setBaby(false);
    }

    /** Licht egal: spawnt im Spawner auch bei Helligkeit. */
    @Override
    public boolean checkSpawnRules(LevelAccessor level, EntitySpawnReason reason) {
        return true;
    }

    /** Brennt nicht in der Sonne. */
    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    /** Wird unter Wasser nicht zum Ertrunkenen. */
    @Override
    protected boolean convertsInWater() {
        return false;
    }

    // =========================
    // BOSS-LEISTE
    // =========================

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossBar.removePlayer(player);
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        bossBar.setName(getDisplayName());
    }

    // =========================
    // ZAUBER
    // =========================

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        bossBar.setProgress(getHealth() / getMaxHealth());

        if (strikeCooldown > 0) strikeCooldown--;
        if (gravityCooldown > 0) gravityCooldown--;
        if (blinkCooldown > 0) blinkCooldown--;
        if (novaCooldown > 0) novaCooldown--;

        tickPendingStrike(level);

        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) return;
        double dist = distanceTo(target);

        if (novaCooldown == 0 && dist < 3.5) {
            castNova(level);
            novaCooldown = NOVA_COOLDOWN;
        } else if (strikeCooldown == 0 && dist < 24 && hasLineOfSight(target)) {
            castStarStrike(level, target);
            strikeCooldown = STAR_STRIKE_COOLDOWN;
        } else if (gravityCooldown == 0 && dist < 12 && hasLineOfSight(target)) {
            castGravity(level, target);
            gravityCooldown = GRAVITY_COOLDOWN;
        } else if (blinkCooldown == 0 && dist > 8) {
            if (blinkTo(level, target)) blinkCooldown = BLINK_COOLDOWN;
        }
    }

    /** Sternenschlag: markiert die Stelle des Ziels, eine Sekunde später schlägt ein Sternenblitz ein. */
    private void castStarStrike(ServerLevel level, LivingEntity target) {
        pendingStrike = target.position();
        pendingStrikeTicks = STAR_STRIKE_DELAY;
        playCastSound(level);
    }

    private void tickPendingStrike(ServerLevel level) {
        if (pendingStrike == null) return;
        Vec3 p = pendingStrike;
        if (pendingStrikeTicks-- > 0) {
            // Vorwarnung: Lichtsäule und Ring aus Sternen
            level.sendParticles(ParticleTypes.END_ROD, p.x, p.y + 0.2, p.z, 3, 0.6, 0.05, 0.6, 0.0);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, p.x, p.y + 1.0, p.z, 4, 0.2, 1.0, 0.2, 0.0);
            return;
        }
        pendingStrike = null;
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
        if (bolt != null) {
            bolt.setVisualOnly(true);
            bolt.setPos(p.x, p.y, p.z);
            level.addFreshEntity(bolt);
        }
        level.sendParticles(ParticleTypes.END_ROD, p.x, p.y + 0.5, p.z, 40, 0.8, 0.8, 0.8, 0.15);
        for (LivingEntity hit : level.getEntitiesOfClass(LivingEntity.class,
                new net.minecraft.world.phys.AABB(p, p).inflate(1.5, 2.0, 1.5), e -> e != this && e.isAlive())) {
            hit.hurtServer(level, damageSources().indirectMagic(this, this), STAR_STRIKE_DAMAGE);
            hit.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1), this);
        }
    }

    /** Schwerkraft-Sog: das Ziel schwebt kurz und wird geschwächt. */
    private void castGravity(ServerLevel level, LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 30, 1), this);
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0), this);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, target.getX(), target.getY() + 1, target.getZ(),
                60, 0.5, 1.0, 0.5, 0.05);
        playCastSound(level);
    }

    /** Sprung durch den Raum: teleportiert sich neben das Ziel. */
    private boolean blinkTo(ServerLevel level, LivingEntity target) {
        Vec3 from = position();
        for (int i = 0; i < 8; i++) {
            double angle = getRandom().nextDouble() * Math.PI * 2;
            double x = target.getX() + Math.cos(angle) * 2.5;
            double z = target.getZ() + Math.sin(angle) * 2.5;
            double y = target.getY();
            if (randomTeleport(x, y, z, true)) {
                level.sendParticles(ParticleTypes.PORTAL, from.x, from.y + 1, from.z, 40, 0.4, 0.8, 0.4, 0.2);
                level.playSound(null, from.x, from.y, from.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1F, 0.8F);
                playSound(SoundEvents.ENDERMAN_TELEPORT, 1F, 0.8F);
                return true;
            }
        }
        return false;
    }

    /** Sternen-Nova: stößt alles in der Nähe weg und verletzt es. */
    private void castNova(ServerLevel level) {
        level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1, getZ(), 60, 0.2, 0.2, 0.2, 0.35);
        playCastSound(level);
        for (LivingEntity hit : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(4.0),
                e -> e != this && e.isAlive() && (e instanceof Player || e == getTarget()))) {
            hit.hurtServer(level, damageSources().indirectMagic(this, this), NOVA_DAMAGE);
            Vec3 push = hit.position().subtract(position()).normalize().scale(1.4);
            hit.push(push.x, 0.45, push.z);
            hit.hurtMarked = true;
        }
    }

    private void playCastSound(ServerLevel level) {
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE,
                1.2F, Mth.nextFloat(getRandom(), 0.7F, 0.9F));
    }
}
