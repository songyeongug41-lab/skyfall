package com.example.gusboss.entity;

import com.example.gusboss.GusStats;
import com.example.gusboss.entity.ai.GusGrabThrowGoal;
import com.example.gusboss.entity.ai.GusLeapSlamGoal;
import com.example.gusboss.entity.ai.GusMegaSlamGoal;
import com.example.gusboss.entity.ai.GusMeleeGoal;
import com.example.gusboss.entity.ai.GusRoarGoal;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

/**
 * Gus, The Iron Golem.
 * Attack 1: punch combo. Attack 2: leap + ground slam. Phase 2: instant switch at low health (full heal),
 * then attack 3 (grab + throw), attack 4 (area jump slam) and attack 5 (roar).
 */
public class GusEntity extends Monster {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final int LEAP_NONE = GusStates.LEAP_NONE;
    public static final int LEAP_WINDUP = GusStates.LEAP_WINDUP;
    public static final int LEAP_AIR = GusStates.LEAP_AIR;
    public static final int LEAP_SLAM = GusStates.LEAP_SLAM;

    // phase 2 attack animations (DATA_ACTION)
    public static final int ACTION_NONE = GusStates.ACTION_NONE;
    public static final int ACTION_GRAB_REACH = GusStates.ACTION_GRAB_REACH;
    public static final int ACTION_GRAB_HOLD = GusStates.ACTION_GRAB_HOLD;
    public static final int ACTION_GRAB_THROW = GusStates.ACTION_GRAB_THROW;
    public static final int ACTION_RECOVER = GusStates.ACTION_RECOVER;
    public static final int ACTION_ROAR_WINDUP = GusStates.ACTION_ROAR_WINDUP;
    public static final int ACTION_ROAR = GusStates.ACTION_ROAR;
    public static final int ACTION_ROAR_RECOVER = GusStates.ACTION_ROAR_RECOVER;
    public static final int ACTION_WHIFF_RECOVER = GusStates.ACTION_WHIFF_RECOVER;

    private static final UUID PHASE2_SPEED_ID = UUID.fromString("7b2a5c1e-3f0d-4a5b-9d0e-5c1f2a7b9e41");
    private static final UUID PHASE2_DAMAGE_ID = UUID.fromString("c3d94a16-8e72-4b5f-a1c0-2d7e6f08b3a9");

    /** Incremented on every punch. Drives the combo variant and restarts the client animation. */
    private static final EntityDataAccessor<Integer> DATA_PUNCH_COUNT =
            SynchedEntityData.defineId(GusEntity.class, EntityDataSerializers.INT);
    /** Current leap phase (LEAP_*). Drives the leap animation on the client. */
    private static final EntityDataAccessor<Integer> DATA_LEAP_STATE =
            SynchedEntityData.defineId(GusEntity.class, EntityDataSerializers.INT);
    /** Phase 2 attack animation (ACTION_*). */
    private static final EntityDataAccessor<Integer> DATA_ACTION =
            SynchedEntityData.defineId(GusEntity.class, EntityDataSerializers.INT);
    /** Boss phase: 1 or 2. */
    private static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(GusEntity.class, EntityDataSerializers.INT);

    private final ServerBossEvent bossEvent =
            new ServerBossEvent(this.getDisplayName(), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);

    /** Server only (saved): phase 2 has been triggered. Guarantees it can only happen once. */
    private boolean phase2Triggered;

    /** Client only: tickCount at which the current animation started. */
    private int punchAnimStartTick = -1000;
    private int leapStateStartTick = -1000;
    /** tickCount at which the current ACTION_* began (set on both sides). */
    private int actionStartTick = -1000;

    public GusEntity(EntityType<? extends GusEntity> type, Level level) {
        super(type, level);
        this.xpReward = GusStats.XP_REWARD;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, GusStats.MAX_HEALTH)
                .add(Attributes.ARMOR, GusStats.ARMOR)
                .add(Attributes.MOVEMENT_SPEED, GusStats.MOVE_SPEED)
                .add(Attributes.ATTACK_DAMAGE, GusStats.ATTACK_DAMAGE)
                .add(Attributes.ATTACK_KNOCKBACK, GusStats.ATTACK_KNOCKBACK)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, GusStats.FOLLOW_RANGE);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // priority 1 = the special attacks (phase 2 ones only start in phase 2); priority 2 = the punches
        this.goalSelector.addGoal(1, new GusRoarGoal(this));
        this.goalSelector.addGoal(1, new GusGrabThrowGoal(this));
        this.goalSelector.addGoal(1, new GusMegaSlamGoal(this));
        this.goalSelector.addGoal(1, new GusLeapSlamGoal(this));
        this.goalSelector.addGoal(2, new GusMeleeGoal(this, 1.0D));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_PUNCH_COUNT, 0);
        this.entityData.define(DATA_LEAP_STATE, LEAP_NONE);
        this.entityData.define(DATA_ACTION, ACTION_NONE);
        this.entityData.define(DATA_PHASE, 1);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (this.level().isClientSide) {
            if (DATA_PUNCH_COUNT.equals(key) && this.getPunchCount() > 0) {
                this.punchAnimStartTick = this.tickCount;
            } else if (DATA_LEAP_STATE.equals(key)) {
                this.leapStateStartTick = this.tickCount;
            } else if (DATA_ACTION.equals(key)) {
                this.actionStartTick = this.tickCount;
            }
        }
    }

    // ---- phase state --------------------------------------------------------------------------

    public int getPhase() {
        return this.entityData.get(DATA_PHASE);
    }

    public boolean isPhase2() {
        return this.getPhase() >= 2;
    }

    /** Phase 2 attacks come faster. */
    private float tempo() {
        return this.isPhase2() ? (float) GusStats.PHASE2_TEMPO_MULT : 1.0F;
    }

    public int getPunchIntervalTicks() {
        return Math.max(4, Math.round(GusStats.PUNCH_INTERVAL_TICKS * this.tempo()));
    }

    public int getPunchAnimTicks() {
        return Math.max(4, Math.round(GusStats.PUNCH_ANIM_TICKS * this.tempo()));
    }

    public int getPunchHitDelayTicks() {
        return Math.max(2, Math.round(GusStats.PUNCH_HIT_DELAY_TICKS * this.tempo()));
    }

    public int getLeapCooldownTicks() {
        return Math.round(GusStats.LEAP_COOLDOWN_TICKS * this.tempo());
    }

    /** Phase 2 deals +10% damage (the attribute covers punches; this covers the slam). */
    public float getDamageMultiplier() {
        return this.isPhase2() ? 1.0F + (float) GusStats.PHASE2_DAMAGE_BONUS : 1.0F;
    }

    // ---- attack 1: punch combo ----------------------------------------------------------------

    public int getPunchCount() {
        return this.entityData.get(DATA_PUNCH_COUNT);
    }

    /** 0 = right straight, 1 = left straight, 2 = two-fisted slam (repeats). */
    public int getPunchVariant() {
        return Math.floorMod(this.getPunchCount() - 1, 3);
    }

    /** Server: begin a punch. The damage itself is applied a few ticks later by the goal. */
    public void startPunch() {
        this.entityData.set(DATA_PUNCH_COUNT, this.getPunchCount() + 1);
        this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.6F);
    }

    /** Server: the fist connects. */
    public void onPunchImpact() {
        this.playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.0F, 1.0F);
    }

    /** Client: progress 0..1 of the current punch animation, or -1 when not punching. */
    public float getPunchProgress(float ageInTicks) {
        float t = (ageInTicks - (float) this.punchAnimStartTick) / (float) this.getPunchAnimTicks();
        return (t >= 0.0F && t < 1.0F) ? t : -1.0F;
    }

    // ---- attack 2: leap + ground slam ---------------------------------------------------------

    public int getLeapState() {
        return this.entityData.get(DATA_LEAP_STATE);
    }

    public void setLeapState(int state) {
        this.entityData.set(DATA_LEAP_STATE, state);
    }

    /** Client: ticks since the current leap phase began. */
    public float getLeapStateTicks(float ageInTicks) {
        return ageInTicks - (float) this.leapStateStartTick;
    }

    /** Server: attack 2 shockwave at the landing point. */
    public void groundSlam() {
        this.shockwave(GusStats.SLAM_RADIUS, GusStats.SLAM_DAMAGE, 0.9D);
    }

    /** Server: attack 4 shockwave (huge radius). */
    public void megaSlam() {
        this.shockwave(GusStats.MEGA_RADIUS, GusStats.MEGA_DAMAGE, GusStats.MEGA_KNOCKBACK);
    }

    /** Server: damaging shockwave around Gus. Jumping over it avoids the damage. */
    private void shockwave(double radius, float baseDamage, double knockbackStrength) {
        if (this.level().isClientSide) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel) this.level();
        Vec3 c = this.position();

        AABB box = new AABB(c.x - radius, c.y - 1.5D, c.z - radius, c.x + radius, c.y + 3.0D, c.z + radius);
        List<LivingEntity> victims = serverLevel.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != this && e.isAlive() && !(e instanceof Monster)
                        && !(e instanceof Player p && (p.isSpectator() || p.isCreative())));

        for (LivingEntity victim : victims) {
            double dx = victim.getX() - c.x;
            double dz = victim.getZ() - c.z;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > radius || victim.getY() > c.y + 1.2D) {
                continue;   // outside the shockwave, or jumped over it
            }
            float falloff = (float) (1.0D - 0.6D * (dist / radius));
            float damage = baseDamage * this.getDamageMultiplier() * falloff;
            if (victim.hurt(this.damageSources().mobAttack(this), damage)) {
                double kb = 1.0D - victim.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
                if (kb > 0.0D) {
                    double len = Math.max(dist, 0.001D);
                    victim.setDeltaMovement(victim.getDeltaMovement().add(
                            dx / len * knockbackStrength * falloff * kb, 0.55D * kb,
                            dz / len * knockbackStrength * falloff * kb));
                    victim.hurtMarked = true;
                }
            }
        }

        this.spawnImpactEffects(serverLevel, c, radius);
    }

    /** Sound, debris burst and an expanding dust ring. */
    private void spawnImpactEffects(ServerLevel level, Vec3 c, double radius) {
        level.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2.0F, 0.6F);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.IRON_GOLEM_ATTACK, SoundSource.HOSTILE, 2.0F, 0.5F);
        BlockState below = level.getBlockState(this.blockPosition().below());
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, below),
                c.x, c.y + 0.1D, c.z, (int) (30.0D + radius * 6.0D), radius * 0.25D, 0.2D, radius * 0.25D, 0.15D);
        level.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 0.2D, c.z, 3, 1.0D, 0.1D, 1.0D, 0.0D);
        for (int ring = 1; ring <= 4; ring++) {
            double ringRadius = radius * ring / 4.0D;
            int count = Math.max(8, (int) (Math.PI * 2.0D * ringRadius / 1.2D));
            for (int i = 0; i < count; i++) {
                double angle = (Math.PI * 2.0D * i) / count;
                level.sendParticles(ParticleTypes.CLOUD,
                        c.x + Math.cos(angle) * ringRadius, c.y + 0.1D, c.z + Math.sin(angle) * ringRadius,
                        1, 0.0D, 0.05D, 0.0D, 0.02D);
            }
        }
    }

    /** Gus lands from big leaps without hurting himself. */
    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    // ---- phase 2 attacks: shared animation state, carrying, roar ----------------------------

    public int getAction() {
        return this.entityData.get(DATA_ACTION);
    }

    public void setAction(int action) {
        this.actionStartTick = this.tickCount;
        this.entityData.set(DATA_ACTION, action);
    }

    /** Client: ticks since the current action began. */
    public float getActionTicks(float ageInTicks) {
        return ageInTicks - (float) this.actionStartTick;
    }

    /** A carried player rides overhead: the seat height rises while Gus lifts, so the grab looks right on both sides. */
    @Override
    public double getPassengersRidingOffset() {
        if (this.getAction() == ACTION_GRAB_HOLD) {
            double t = Mth.clamp((double) (this.tickCount - this.actionStartTick) / 12.0D, 0.0D, 1.0D);
            double smooth = t * t * (3.0D - 2.0D * t);
            return Mth.lerp(smooth, 2.0D, 3.6D);
        }
        if (this.getAction() == ACTION_GRAB_THROW) {
            return 3.6D;
        }
        return 2.0D;
    }

    @Override
    public void die(DamageSource source) {
        this.ejectPassengers();
        super.die(source);
    }

    /** Server: the roar. Everyone nearby gets Nausea and Wither. */
    public void roarShout() {
        if (this.level().isClientSide) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel) this.level();
        double radius = GusStats.ROAR_RADIUS;
        List<Player> players = serverLevel.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(radius),
                p -> p.isAlive() && !p.isSpectator() && !p.isCreative() && p.distanceToSqr(this) <= radius * radius);
        for (Player player : players) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, GusStats.ROAR_NAUSEA_TICKS, 0));
            player.addEffect(new MobEffectInstance(MobEffects.WITHER, GusStats.ROAR_WITHER_TICKS,
                    GusStats.ROAR_WITHER_AMPLIFIER));
        }
        // visible shock rings at head height
        for (int ring = 1; ring <= 3; ring++) {
            double ringRadius = ring * 2.5D;
            int count = 24;
            for (int i = 0; i < count; i++) {
                double angle = (Math.PI * 2.0D * i) / count;
                serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                        this.getX() + Math.cos(angle) * ringRadius, this.getY() + 2.2D,
                        this.getZ() + Math.sin(angle) * ringRadius, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    // ---- phase 2: instant switch --------------------------------------------------------------

    /**
     * Phase 1 is never fatal: when damage takes Gus to 30% health or below, phase 2 starts on the spot.
     * A lethal hit is rescued the same way (the full heal happens before death is processed).
     */
    @Override
    protected void actuallyHurt(DamageSource source, float amount) {
        super.actuallyHurt(source, amount);
        if (!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            this.checkPhase2Trigger();
        }
    }

    /** Starts phase 2 (once) when health is at or below the trigger line. Also checked every tick. */
    private void checkPhase2Trigger() {
        if (this.level().isClientSide || this.phase2Triggered) {
            return;
        }
        if (this.getHealth() <= this.getMaxHealth() * GusStats.PHASE2_TRIGGER_RATIO) {
            this.startPhase2();
        }
    }

    /** Server: the instant switch - phase flag (swaps the texture), buffs, full heal, piglins, a burst of feedback. */
    private void startPhase2() {
        this.phase2Triggered = true;
        ServerLevel level = (ServerLevel) this.level();
        LOGGER.info("[Gus] phase 2 started (health {} / {})", this.getHealth(), this.getMaxHealth());

        this.entityData.set(DATA_PHASE, 2);
        this.applyPhase2Buffs();
        this.setHealth(this.getMaxHealth());
        this.bossEvent.setColor(BossEvent.BossBarColor.PURPLE);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(this) < 64.0D * 64.0D) {
                player.displayClientMessage(Component.literal("Gus enters phase 2!"), true);
            }
        }
        this.spawnPiglins(level);

        level.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 3.0F, 0.6F);
        this.spawnImpactEffects(level, this.position(), 8.0D);
        level.sendParticles(ParticleTypes.ANGRY_VILLAGER, this.getX(), this.getY() + 2.8D, this.getZ(),
                12, 0.8D, 0.5D, 0.8D, 0.0D);
    }

    /** +50% speed and +10% damage. Safe to call repeatedly (e.g. after loading a save). */
    private void applyPhase2Buffs() {
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && speed.getModifier(PHASE2_SPEED_ID) == null) {
            speed.addPermanentModifier(new AttributeModifier(PHASE2_SPEED_ID, "Gus phase 2 speed",
                    GusStats.PHASE2_SPEED_BONUS, AttributeModifier.Operation.MULTIPLY_BASE));
        }
        AttributeInstance damage = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damage != null && damage.getModifier(PHASE2_DAMAGE_ID) == null) {
            damage.addPermanentModifier(new AttributeModifier(PHASE2_DAMAGE_ID, "Gus phase 2 damage",
                    GusStats.PHASE2_DAMAGE_BONUS, AttributeModifier.Operation.MULTIPLY_BASE));
        }
    }

    private void spawnPiglins(ServerLevel level) {
        LivingEntity target = this.getTarget();
        for (int i = 0; i < GusStats.PIGLIN_COUNT; i++) {
            Piglin piglin = EntityType.PIGLIN.create(level);
            if (piglin == null) {
                continue;
            }
            BlockPos pos = this.findPiglinSpawnPos(level);
            piglin.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, this.random.nextFloat() * 360.0F, 0.0F);
            piglin.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.MOB_SUMMONED, null, null);
            piglin.setBaby(false);
            piglin.setImmuneToZombification(true);   // they would turn into zombified piglins in the Overworld
            piglin.setPersistenceRequired();
            if (target != null) {
                piglin.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, target);
            }
            level.addFreshEntity(piglin);
            level.sendParticles(ParticleTypes.POOF, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    12, 0.3D, 0.5D, 0.3D, 0.05D);
        }
        level.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.PIGLIN_ANGRY, SoundSource.HOSTILE, 2.0F, 0.8F);
    }

    /** A random standable spot 4-10 blocks away (same height band as Gus). */
    private BlockPos findPiglinSpawnPos(ServerLevel level) {
        for (int attempt = 0; attempt < 20; attempt++) {
            double angle = this.random.nextDouble() * Math.PI * 2.0D;
            double dist = 4.0D + this.random.nextDouble() * 6.0D;
            int x = Mth.floor(this.getX() + Math.cos(angle) * dist);
            int z = Mth.floor(this.getZ() + Math.sin(angle) * dist);
            for (int dy = 3; dy >= -3; dy--) {
                BlockPos pos = new BlockPos(x, this.getBlockY() + dy, z);
                BlockPos floor = pos.below();
                if (level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()
                        && level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) {
                    return pos;
                }
            }
        }
        return this.blockPosition();
    }

    // ---- passive: combat regeneration + boss bar ----------------------------------------------

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.checkPhase2Trigger();
        if (this.getTarget() != null
                && this.tickCount % 20 == 0 && this.getHealth() < this.getMaxHealth()) {
            this.heal(GusStats.COMBAT_REGEN_PER_SECOND);
        }
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("GusPhase", this.getPhase());
        tag.putBoolean("GusPhase2Triggered", this.phase2Triggered);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (this.hasCustomName()) {
            this.bossEvent.setName(this.getDisplayName());
        }
        this.phase2Triggered = tag.getBoolean("GusPhase2Triggered");
        this.entityData.set(DATA_PHASE, Math.max(1, tag.getInt("GusPhase")));
        if (this.isPhase2()) {
            this.applyPhase2Buffs();
            this.bossEvent.setColor(BossEvent.BossBarColor.PURPLE);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    // ---- sounds (vanilla iron golem sounds for now) -------------------------------------------

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.IRON_GOLEM_STEP, 1.0F, 1.0F);
    }
}
