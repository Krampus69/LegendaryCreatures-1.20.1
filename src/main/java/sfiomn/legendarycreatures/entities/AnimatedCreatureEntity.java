package sfiomn.legendarycreatures.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.constant.DefaultAnimations;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import sfiomn.legendarycreatures.LegendaryCreatures;

import java.util.UUID;

public abstract class AnimatedCreatureEntity extends PathfinderMob implements GeoEntity {

    /**
     * Fragile creatures: any entity type in this tag dies when a player touches it
     * (data/legendarycreatures/tags/entity_types/insects.json).
     */
    private static final TagKey<EntityType<?>> INSECTS =
            TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(sfiomn.legendarycreatures.LegendaryCreatures.MOD_ID, "insects"));

    @Override
    public void playerTouch(Player player) {
        super.playerTouch(player);
        // Squish: insects die on player contact, as a normal attributed death (drops, XP, animation).
        // playerTouch fires from ~1 block away (vanilla scans an inflated box around the player).
        // A squish should need actual contact: only proceed if the hitboxes genuinely overlap.
        if (!this.level().isClientSide && this.isAlive() && this.getType().is(INSECTS)
                && this.getBoundingBox().intersects(player.getBoundingBox())) {
            // Level.playSound ignores the entity's silent flag; this.playSound would not.
            // Pitch varies +/-10% per squish so repeated stomps don't sound identical.
            float squishPitch = 0.9F + this.getRandom().nextFloat() * 0.2F;
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    sfiomn.legendarycreatures.registry.SoundRegistry.INSECT_SQUISH.get(),
                    this.getSoundSource(), 1.0F, squishPitch);
            // Mute the mob so its own hurt/death cries don't stack on the squish.
            this.setSilent(true);
            // genericKill: goes through the normal death pipeline (death event/animation) but has
            // NO attacker entity — so damage-reflection mods (thorns affixes, reflect trinkets)
            // have no one to reflect at, and the player can never be stung for stepping on a bug.
            // Player attribution sacrificed knowingly: all bug loot tables are empty, so nothing
            // (drops, killed_by_player conditions) depended on it.
            this.hurt(this.damageSources().genericKill(), Float.MAX_VALUE);
        }
    }

    protected static final UUID CREATURE_L2_HEALTH_UUID = UUID.fromString("4c9caba4-e7c6-4499-9dae-809aa3c260d8");
    protected static final UUID CREATURE_L3_HEALTH_UUID = UUID.fromString("40c34aa2-9867-448a-a3e5-63ff63750c03");
    protected static final UUID CREATURE_L2_ATTACK_UUID = UUID.fromString("fe62b151-7ae5-4d90-9f66-fe0e5efc49de");
    protected static final UUID CREATURE_L3_ATTACK_UUID = UUID.fromString("447d8a0d-440e-4f60-b8fc-642bea9fa467");

    private final AnimatableInstanceCache instanceCache = GeckoLibUtil.createInstanceCache(this);

    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(AnimatedCreatureEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ATTACK_ANIMATION = SynchedEntityData.defineId(AnimatedCreatureEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> SPAWN_EFFECT = SynchedEntityData.defineId(AnimatedCreatureEntity.class, EntityDataSerializers.BOOLEAN);

    private final RawAnimation WALK_ANIM = RawAnimation.begin().thenPlay("walk");
    private final RawAnimation IDLE_ANIM = RawAnimation.begin().thenPlay("idle");

    public static final int NO_ANIMATION = 0;
    public static final int BASE_ATTACK = 1;
    public static final int CHARGE_ATTACK = 2;
    public static final int CHARGING = 3;
    public static final int ROOT_ATTACK = 4;
    public static final int EFFECT_ATTACK = 5;
    public static final int DELAY_ATTACK = 6;
    public static final int DISTANCE_ATTACK = 7;

    protected AnimatedCreatureEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setMaxUpStep(1.05F);
    }

    @Override
    public double getFluidJumpThreshold() {
        return 1.0D;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();

        this.entityData.define(VARIANT, 0);
        this.entityData.define(ATTACK_ANIMATION, NO_ANIMATION);
        this.entityData.define(SPAWN_EFFECT, Boolean.FALSE);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficultyInstance,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData,
                                        @Nullable CompoundTag tag) {
        if (spawnType.equals(MobSpawnType.SPAWN_EGG))
            setSpawnEffect(true);

        rollRarityVariant();
        applyRarityModifiers();

        return super.finalizeSpawn(level, difficultyInstance, spawnType, spawnGroupData, tag);
    }

    public int getAttackAnimation() {
        return this.entityData.get(ATTACK_ANIMATION);
    }

    public void setAttackAnimation(int animation) {
        this.entityData.set(ATTACK_ANIMATION, animation);
    }

    public int getVariant() {
        // Return range [1 - 10]
        return entityData.get(VARIANT);
    }

    public void setVariant(int variant) {
        entityData.set(VARIANT, variant);
    }

    // ----- Rarity tier system -----

    protected double getLevel2HealthBonus() { return 0; }
    protected double getLevel3HealthBonus() { return 0; }
    protected double getLevel2AttackBonus() { return 0; }
    protected double getLevel3AttackBonus() { return 0; }

    public int getLevel2SpawnChance() { return 0; }
    public int getLevel3SpawnChance() { return 0; }

    public  int getLevel2Variant() { return 7; }
    public  int getLevel3Variant() { return 9; }

    public boolean isLevel2() {
        return getVariant() >= getLevel2Variant() && getVariant() < getLevel3Variant();
    }
    public boolean isLevel3() {
        return getVariant() >= getLevel3Variant();
    }

    protected void rollRarityVariant() {
        if (this.level().isClientSide) return;
        int roll = getRandom().nextInt(100);
        int l3Threshold = 100 - getLevel3SpawnChance();
        int l2Threshold = l3Threshold - getLevel2SpawnChance();
        if (roll >= l3Threshold)      setVariant(getLevel3Variant());
        else if (roll >= l2Threshold) setVariant(getLevel2Variant());
    }

    public  void applyRarityModifiers() {
        AttributeInstance hp  = this.getAttribute(Attributes.MAX_HEALTH);
        AttributeInstance atk = this.getAttribute(Attributes.ATTACK_DAMAGE);

        if (isLevel3()) {
            applyMod(hp,  CREATURE_L3_HEALTH_UUID, "rarity_l3_hp",  getLevel3HealthBonus());
            applyMod(atk, CREATURE_L3_ATTACK_UUID, "rarity_l3_atk", getLevel3AttackBonus());
        } else if (isLevel2()) {
            applyMod(hp,  CREATURE_L2_HEALTH_UUID, "rarity_l2_hp",  getLevel2HealthBonus());
            applyMod(atk, CREATURE_L2_ATTACK_UUID, "rarity_l2_atk", getLevel2AttackBonus());
        }

        if (hp != null) this.setHealth(this.getMaxHealth());
    }

    private void applyMod(AttributeInstance attr, UUID uuid, String nameSuffix, double amount) {
        if (attr == null || amount == 0) return;
        attr.removeModifier(uuid);   // no-op if not present, removes the previous modifier if it is
        attr.addPermanentModifier(new AttributeModifier(
                uuid,
                LegendaryCreatures.MOD_ID + ":" + nameSuffix,
                amount,
                AttributeModifier.Operation.ADDITION));
    }

    public void setSpawnEffect(boolean spawnEffect) {
        entityData.set(SPAWN_EFFECT, spawnEffect);
    }

    public boolean hasSpawnEffect() {
        return entityData.get(SPAWN_EFFECT);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("variant", getVariant());
        nbt.putBoolean("spawn_effect", hasSpawnEffect());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        setVariant(nbt.getInt("variant"));
        setSpawnEffect(nbt.getBoolean("spawn_effect"));
    }

    // While > 0, this creature CAN take fall damage — set when a blowgun/jinx dart touches it,
    // so levitated victims can be dropped to their death. Everywhere else, fall immunity holds.
    private int dartFallVulnerableTicks;

    /** Called by JinxDartEntity on a damaging hit. Window covers the levitation rise plus the drop. */
    public void markDartFallVulnerable(int ticks) {
        this.dartFallVulnerableTicks = Math.max(this.dartFallVulnerableTicks, ticks);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypes.FALL))
            return this.dartFallVulnerableTicks > 0 && super.hurt(source, amount);
        else if (source.is(DamageTypes.DROWN))
            return false;
        else if (source.is(DamageTypes.CACTUS))
            return false;
        else if (source.is(DamageTypes.IN_WALL))
            return false;
        else if (source.is(DamageTypes.SWEET_BERRY_BUSH))
            return false;
        else if (source.is(DamageTypes.FALLING_ANVIL))
            return false;
        else if (source.is(DamageTypes.DRAGON_BREATH))
            return false;
        return super.hurt(source, amount);
    }

    public <E extends GeoAnimatable> PlayState movementPredicate(AnimationState<E> state) {
        if (hasSpawnEffect() && this.tickCount < getSpawnAnimationTicks())
            return PlayState.CONTINUE;

        if (getDeathAnimation() != null && this.isDeadOrDying()) {
            return state.setAndContinue(getDeathAnimation());
        } else if (state.isMoving() || this.xOld != this.getX() || this.zOld != this.getZ()) {
            if (this.isInWaterOrBubble()) {
                if (getSwimAnimation() != null) {
                    return state.setAndContinue(getSwimAnimation());
                } else if (getWalkAnimation() != null) {
                    return state.setAndContinue(getWalkAnimation());
                }
            } else if (getSprintAnimation() != null && this.isSprinting()) {
                return state.setAndContinue(getSprintAnimation());
            } else if (getWalkAnimation() != null) {
                return state.setAndContinue(getWalkAnimation());
            }
        }
        return state.setAndContinue(getIdleAnimation());
    }

    public <E extends GeoAnimatable> PlayState attackingPredicate(AnimationState<E> state) {
        return PlayState.STOP;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        if (getSpawnAnimationTicks() > 0 && hasSpawnEffect())
            controllerRegistrar.add(DefaultAnimations.getSpawnController(this, (animationState) -> this, hasSpawnEffect() ? getSpawnAnimationTicks(): 0));
        controllerRegistrar.add(new AnimationController<>(this, "Movement", 6, this::movementPredicate));
        controllerRegistrar.add(new AnimationController<>(this, "Attack", 6, this::attackingPredicate));
    }

    @Override
    public void tick() {
        if (hasSpawnEffect() && this.tickCount < this.getSpawnAnimationTicks() * 0.6) {
            RandomSource random = this.getRandom();
            for(int i = 0; i < 6; ++i) {
                double x = this.getX() + 0.5 + ((random.nextFloat() * 0.5F) - 1.0);
                double y = this.getY() + 0.1;
                double z = this.getZ() + 0.5 + ((random.nextFloat() * 0.5F) - 1.0);
                BlockState blockstate = this.level().getBlockState(new BlockPos((int) x, (int) y, (int) z).below());
                if (blockstate.getRenderShape() != RenderShape.INVISIBLE && this.level().isClientSide) {
                    this.level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, blockstate), x, y, z, 0.0D, 0.0D, 0.0D);
                }
            }
        }

        if (hasSpawnEffect() && this.tickCount > this.getSpawnAnimationTicks() + 5)
            setSpawnEffect(false);

        float yRotBefore = this.getYRot();
        float yBodyRotBefore = this.yBodyRot;
        boolean ease = !this.level().isClientSide && useEasedTurning() && !this.isDeadOrDying();

        super.tick();

        if (ease && !this.skipTurnEase) {
            float acceleration = getTurnAcceleration();
            float maxSpeed = getMaxTurnSpeed();
            float easeOut = getTurnEaseOut();

            this.setYRot(this.yRotEase.step(yRotBefore, this.getYRot(), acceleration, maxSpeed, easeOut));
            this.yBodyRot = this.yBodyRotEase.step(yBodyRotBefore, this.yBodyRot, acceleration, maxSpeed, easeOut);

            float headOffset = Mth.wrapDegrees(this.yHeadRot - this.yBodyRot);
            float headLimit = this.getMaxHeadYRot();
            if (headOffset > headLimit)
                this.yHeadRot = this.yBodyRot + headLimit;
            else if (headOffset < -headLimit)
                this.yHeadRot = this.yBodyRot - headLimit;
        } else {
            this.yRotEase.reset();
            this.yBodyRotEase.reset();
        }

        this.skipTurnEase = false;
    }

    private final TurnEase yRotEase = new TurnEase();
    private final TurnEase yBodyRotEase = new TurnEase();
    private boolean skipTurnEase;

    public boolean useEasedTurning() {
        return true;
    }

    public float getTurnAcceleration() {
        return 5.0F;
    }

    public float getMaxTurnSpeed() {
        return 25.0F;
    }

    public float getTurnEaseOut() {
        return 0.4F;
    }

    public void skipTurnEasing() {
        this.skipTurnEase = true;
    }

    protected static class TurnEase {
        private float speed;

        public void reset() {
            this.speed = 0.0F;
        }

        public float step(float from, float to, float acceleration, float maxSpeed, float easeOut) {
            float delta = Mth.wrapDegrees(to - from);
            float distance = Math.abs(delta);
            if (distance < 0.05F) {
                this.speed = 0.0F;
                return to;
            }
            float desired = Math.min(maxSpeed, distance * easeOut);
            this.speed = desired > this.speed ? Math.min(desired, this.speed + acceleration) : desired;
            return from + Math.copySign(Math.min(distance, this.speed), delta);
        }
    }

    public static boolean checkHostileCreatureOnSurfaceSpawnRules(EntityType<? extends Mob> entityType, LevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        return level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, pos.getX(), pos.getZ()) >= pos.getY() && level.getDifficulty() != Difficulty.PEACEFUL;
    }

    public static boolean checkHostileCreatureNoSpawnRules(EntityType<? extends Mob> entityType, LevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        return level.getDifficulty() != Difficulty.PEACEFUL;
    }

    public static boolean checkPeacefulCreatureOnSurfaceSpawnRules(EntityType<? extends Mob> entityType, LevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        return level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, pos.getX(), pos.getZ()) >= pos.getY();
    }

    @Override
    protected SoundEvent getHurtSound(@NotNull DamageSource source) {
        return SoundEvents.HOSTILE_HURT;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.instanceCache;
    }

    public int getSpawnAnimationTicks() {
        return 0;
    }
    public RawAnimation getWalkAnimation() { return WALK_ANIM;}
    public RawAnimation getIdleAnimation() { return IDLE_ANIM;}
    public RawAnimation getDeathAnimation() {
        return null;
    }
    public RawAnimation getSwimAnimation() {
        return null;
    }
    public RawAnimation getSprintAnimation() {
        return null;
    }

    @Override
    protected void customServerAiStep() {
        if (this.dartFallVulnerableTicks > 0) this.dartFallVulnerableTicks--;

        if (this.isSprinting() != this.isAggressive())
            this.setSprinting(this.isAggressive());

        super.customServerAiStep();
    }
}
