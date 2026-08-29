package sfiomn.legendarycreatures.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import sfiomn.legendarycreatures.LegendaryCreatures;
import sfiomn.legendarycreatures.entities.goals.BaseMeleeAttackGoal;
import sfiomn.legendarycreatures.entities.goals.FrogmanLeapGoal;
import sfiomn.legendarycreatures.entities.goals.FrogmanSleepGoal;
import sfiomn.legendarycreatures.registry.ParticleTypeRegistry;
import sfiomn.legendarycreatures.registry.SoundRegistry;
import sfiomn.legendarycreatures.sounds.FrogmanSounds;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import javax.annotation.Nullable;

public class FrogmanEntity extends AnimatedCreatureEntity implements Enemy {

    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation RUN_ANIM = RawAnimation.begin().thenLoop("run");
    private static final RawAnimation JUMP_ANIM = RawAnimation.begin().thenPlayAndHold("jump");
    private static final RawAnimation ATTACK_ANIM = RawAnimation.begin().thenPlay("attack");
    private static final RawAnimation HURT_ANIM = RawAnimation.begin().thenPlay("hurt");
    private static final RawAnimation SLEEP_ANIM = RawAnimation.begin().thenLoop("sleep");
    private static final RawAnimation WAKE_UP_ANIM = RawAnimation.begin().thenPlayAndHold("wake_up");

    private static final EntityDataAccessor<Boolean> LEAPING =
            SynchedEntityData.defineId(FrogmanEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SLEEPING =
            SynchedEntityData.defineId(FrogmanEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> WAKING_UP =
            SynchedEntityData.defineId(FrogmanEntity.class, EntityDataSerializers.BOOLEAN);

    private static final int ATTACK_DURATION = 25;
    private static final int ATTACK_ACTION_POINT = 15;
    private static final int ATTACK_COOLDOWN = 10;
    private static final float ATTACK_TRIGGER_REACH = 3.0F;
    private static final float ATTACK_HIT_REACH = 4.5F;
    private static final int POISON_DURATION = 100;
    private static final int POISON_AMPLIFIER = 2;
    private static final int SHIELD_DISABLE_TICKS = 100;
    private static final long SLEEP_START_TIME = 18000L;
    private static final int SLEEP_COOLDOWN = 100;
    private static final int SLEEP_PARTICLE_INTERVAL = 25;
    private static final int WAKE_UP_ANIM_TICKS = 15;
    private static final int ANIM_TRANSITION_TICKS = 6;
    private static final int WAKE_UP_DURATION = WAKE_UP_ANIM_TICKS + ANIM_TRANSITION_TICKS;
    private static final double SLEEP_WAKE_RADIUS = 8.0D;

    private static final byte EVENT_IDLE = 100;
    private static final byte EVENT_HURT = 101;
    private static final byte EVENT_STEP = 102;
    private static final byte EVENT_ATTACK = 103;
    private static final byte EVENT_LEAP = 104;

    private boolean leapWanted;
    private int leapCooldown;
    private int sleepCooldown;
    private int wakeUpTicks;

    public FrogmanEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 10;
        this.moveControl = new FrogmanMoveControl(this);
        if (this.getNavigation() instanceof GroundPathNavigation navigation) {
            navigation.setCanFloat(true);
        }
    }

    public static AttributeSupplier.Builder setCustomAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ARMOR, 0)
                .add(Attributes.ATTACK_DAMAGE, 6)
                .add(Attributes.FOLLOW_RANGE, 24)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0);
    }

    @Override protected double getLevel2HealthBonus() { return 10; }
    @Override protected double getLevel3HealthBonus() { return 20; }
    @Override protected double getLevel2AttackBonus() { return 2; }
    @Override protected double getLevel3AttackBonus() { return 4; }
    @Override public int getLevel2SpawnChance() { return 20; }
    @Override public int getLevel3SpawnChance() { return 4; }

    @Override
    public Component getName() {
        String descriptionId = "entity." + LegendaryCreatures.MOD_ID + ".frogman";
        if (isLevel2())
            descriptionId = "entity." + LegendaryCreatures.MOD_ID + ".frogman2";
        else if (isLevel3())
            descriptionId = "entity." + LegendaryCreatures.MOD_ID + ".frogman3";
        return Component.translatable(descriptionId);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(LEAPING, Boolean.FALSE);
        this.entityData.define(SLEEPING, Boolean.FALSE);
        this.entityData.define(WAKING_UP, Boolean.FALSE);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();

        this.goalSelector.addGoal(0, new FrogmanSleepGoal(this));
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new FrogmanLeapGoal(this));
        this.goalSelector.addGoal(3, new BaseMeleeAttackGoal(this, ATTACK_DURATION, ATTACK_ACTION_POINT, ATTACK_COOLDOWN, 1.0, true) {
            @Override
            protected double getAttackReachSqr(LivingEntity entity) {
                return ATTACK_TRIGGER_REACH * ATTACK_TRIGGER_REACH + entity.getBbWidth();
            }

            @Override
            public boolean canUse() {
                return !isSleeping() && !isWakingUp() && !isLeaping() && !isLeapWanted() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                if (isSleeping() || isWakingUp())
                    return false;
                if (isLeapWanted() && !isAttacking())
                    return false;
                return super.canContinueToUse();
            }

            @Override
            protected void startAttack() {
                super.startAttack();
                playSound(SoundRegistry.FROGMAN_ATTACK.get(), 1.0F, 1.0F);
            }

            @Override
            protected boolean executeAttack(LivingEntity target) {
                setLeapWanted(true);

                double hitReachSqr = ATTACK_HIT_REACH * ATTACK_HIT_REACH + target.getBbWidth();
                if (hitReachSqr < FrogmanEntity.this.distanceToSqr(target))
                    return false;

                if (!FrogmanEntity.this.getSensing().hasLineOfSight(target))
                    return false;

                Player player = target instanceof Player ? (Player) target : null;
                boolean blocking = player != null && player.isBlocking();

                boolean hit = super.executeAttack(target);

                if (hasShieldBash() && blocking)
                    bashShield(player);

                if (hit && isLevel3())
                    target.addEffect(new MobEffectInstance(MobEffects.POISON, POISON_DURATION, POISON_AMPLIFIER, false, true));

                return hit;
            }
        });
        this.goalSelector.addGoal(6, new RandomStrollGoal(this, 0.45, 60));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public boolean hasShieldBash() {
        return isLevel2() || isLevel3();
    }

    private void bashShield(Player player) {
        player.getCooldowns().addCooldown(Items.SHIELD, SHIELD_DISABLE_TICKS);
        player.stopUsingItem();
        player.level().broadcastEntityEvent(player, (byte) 30);
    }

    public boolean isSleeping() {
        return this.entityData.get(SLEEPING);
    }

    public void setSleeping(boolean sleeping) {
        this.entityData.set(SLEEPING, sleeping);
    }

    public boolean isWakingUp() {
        return this.entityData.get(WAKING_UP);
    }

    public void setWakingUp(boolean wakingUp) {
        this.entityData.set(WAKING_UP, wakingUp);
    }

    public void wakeUp() {
        if (!isSleeping())
            return;
        setSleeping(false);
        setWakingUp(true);
        playSound(SoundRegistry.FROGMAN_IDLE.get(), 1.0F, 1.0F);
        this.wakeUpTicks = WAKE_UP_DURATION;
        this.sleepCooldown = SLEEP_COOLDOWN;
    }

    private boolean nearbyPlayerWakesUp() {
        Player player = this.level().getNearestPlayer(this.getX(), this.getY(), this.getZ(), SLEEP_WAKE_RADIUS,
                entity -> entity instanceof Player candidate
                        && !candidate.isCrouching()
                        && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(candidate));
        return player != null;
    }

    private boolean isSleepTime() {
        return this.level().getDayTime() % 24000L >= SLEEP_START_TIME;
    }

    private void tickSleep() {
        if (isWakingUp()) {
            if (--this.wakeUpTicks <= 0)
                setWakingUp(false);
            return;
        }

        if (isSleeping()) {
            if (!isSleepTime() || isInWater() || getLastHurtByMob() != null || nearbyPlayerWakesUp())
                wakeUp();

            return;
        }

        if (this.sleepCooldown > 0) {
            this.sleepCooldown--;
            return;
        }

        if (!isSleepTime() || getTarget() != null || !onGround() || isInWater() || isLeaping() || nearbyPlayerWakesUp())
            return;

        setSleeping(true);
        getNavigation().stop();
    }

    public boolean isMovementLocked() {
        return isAttackLocked() || isSleeping() || isWakingUp();
    }

    public boolean isAttackLocked() {
        return getAttackAnimation() == BASE_ATTACK;
    }

    public boolean isLeaping() {
        return this.entityData.get(LEAPING);
    }

    public void setLeaping(boolean leaping) {
        this.entityData.set(LEAPING, leaping);
    }

    @Override
    public boolean useEasedTurning() {
        return !isLeaping();
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide && isSleeping()) {
            FrogmanSounds.startSleepSound(this);

            if (this.tickCount % SLEEP_PARTICLE_INTERVAL == 0)
                spawnSleepingParticle();
        }
    }

    private void spawnSleepingParticle() {
        float yRotRad = this.yBodyRot * ((float) Math.PI / 180F);
        double forward = -Mth.sin(yRotRad) * 0.15D;
        double side = Mth.cos(yRotRad) * 0.15D;

        double x = this.getX() + forward + ((this.random.nextDouble() - 0.5D) * 0.1D);
        double y = this.getY() + (this.getBbHeight() * 0.9D) + (this.random.nextDouble() * 0.1D);
        double z = this.getZ() + side + ((this.random.nextDouble() - 0.5D) * 0.1D);

        this.level().addParticle(ParticleTypeRegistry.SLEEPING_PARTICLE.get(), x, y, z, 0.0D, 0.0D, 0.0D);
    }

    public boolean isLeapWanted() {
        return this.leapWanted;
    }

    public void setLeapWanted(boolean leapWanted) {
        this.leapWanted = leapWanted;
    }

    public int getLeapCooldown() {
        return this.leapCooldown;
    }

    public void setLeapCooldown(int leapCooldown) {
        this.leapCooldown = leapCooldown;
    }

    public boolean hasLineOfSightTo(Vec3 pos) {
        Vec3 from = this.position();
        if (pos.distanceTo(from) > Math.max(50.0D, this.getAttributeValue(Attributes.FOLLOW_RANGE)))
            return false;
        return this.level().clip(new ClipContext(from, pos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this))
                .getType() == HitResult.Type.MISS;
    }

    public Vec3 randomPointBehindTarget(LivingEntity target) {
        float angle = target.yHeadRot + 180.0F + (float) (this.random.nextGaussian() * 90.0F / 2.0F);
        float distance = Mth.lerp(this.random.nextFloat(), 5.0F, 8.0F);
        Vec3 offset = Vec3.directionFromRotation(0.0F, angle).scale(distance);
        return target.position().add(offset);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putBoolean("sleeping", isSleeping());
        nbt.putInt("sleepCooldown", this.sleepCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        setSleeping(nbt.getBoolean("sleeping"));
        this.sleepCooldown = nbt.getInt("sleepCooldown");
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (this.isEffectiveAi() && this.isInWater() && !this.onGround()) {
            this.moveRelative(this.getSpeed(), travelVector);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.6D));
            if (!this.isNoGravity())
                this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.02D, 0.0D));
        } else {
            super.travel(travelVector);
        }
    }

    @Override
    protected void customServerAiStep() {
        tickSleep();

        if (this.leapCooldown > 0)
            this.leapCooldown--;

        if (isMovementLocked()) {
            this.getNavigation().stop();
            this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
        }

        if (this.getTarget() == null && this.leapWanted)
            this.leapWanted = false;

        super.customServerAiStep();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        super.registerControllers(controllerRegistrar);
        controllerRegistrar.add(new AnimationController<>(this, "Hurt", 0, state -> PlayState.STOP)
                .triggerableAnim("hurt", HURT_ANIM));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean wasAsleep = isSleeping();
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide) {
            wakeUp();
            if (!wasAsleep && !this.isDeadOrDying())
                triggerAnim("Hurt", "hurt");
        }
        return hurt;
    }

    @Override
    public <E extends GeoAnimatable> PlayState movementPredicate(AnimationState<E> state) {
        if (isSleeping())
            return state.setAndContinue(SLEEP_ANIM);
        if (isWakingUp())
            return state.setAndContinue(WAKE_UP_ANIM);
        if (isLeaping())
            return state.setAndContinue(JUMP_ANIM);
        return super.movementPredicate(state);
    }

    @Override
    public <E extends GeoAnimatable> PlayState attackingPredicate(AnimationState<E> state) {
        if (getAttackAnimation() == BASE_ATTACK)
            return state.setAndContinue(ATTACK_ANIM);

        state.getController().forceAnimationReset();
        return PlayState.STOP;
    }

    @Override
    public RawAnimation getIdleAnimation() {
        return IDLE_ANIM;
    }

    @Override
    public RawAnimation getWalkAnimation() {
        return WALK_ANIM;
    }

    @Override
    public RawAnimation getSprintAnimation() {
        return RUN_ANIM;
    }

    private byte followingEventFor(SoundEvent sound) {
        if (sound == SoundRegistry.FROGMAN_IDLE.get())
            return EVENT_IDLE;
        if (sound == SoundRegistry.FROGMAN_HURT.get())
            return EVENT_HURT;
        if (sound == SoundRegistry.FROGMAN_STEP.get())
            return EVENT_STEP;
        if (sound == SoundRegistry.FROGMAN_ATTACK.get())
            return EVENT_ATTACK;
        if (sound == SoundRegistry.FROGMAN_LEAP.get())
            return EVENT_LEAP;
        return 0;
    }

    @Override
    public void playSound(SoundEvent sound, float volume, float pitch) {
        if (this.isSilent())
            return;

        if (!this.level().isClientSide) {
            byte event = followingEventFor(sound);
            if (event != 0) {
                this.level().broadcastEntityEvent(this, event);
                return;
            }
        }

        super.playSound(sound, volume, pitch);
    }

    @Override
    public void handleEntityEvent(byte id) {
        switch (id) {
            case EVENT_IDLE -> FrogmanSounds.playFollowing(SoundRegistry.FROGMAN_IDLE.get(), this, 1.0F, 0.95F, 1.05F);
            case EVENT_HURT -> FrogmanSounds.playFollowing(SoundRegistry.FROGMAN_HURT.get(), this, 1.0F, 0.95F, 1.05F);
            case EVENT_STEP -> FrogmanSounds.playFollowing(SoundRegistry.FROGMAN_STEP.get(), this, 1.0F, 1.05F, 1.15F);
            case EVENT_ATTACK -> FrogmanSounds.playFollowing(SoundRegistry.FROGMAN_ATTACK.get(), this, 1.0F, 1.05F, 1.15F);
            case EVENT_LEAP -> FrogmanSounds.playFollowing(SoundRegistry.FROGMAN_LEAP.get(), this, 1.0F, 1.15F, 1.3F);
            default -> super.handleEntityEvent(id);
        }
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return isSleeping() ? null : SoundRegistry.FROGMAN_IDLE.get();
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return this.isDeadOrDying() ? null : SoundRegistry.FROGMAN_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundRegistry.FROGMAN_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundRegistry.FROGMAN_STEP.get(), 1.0F, 1.0F);
    }

    class FrogmanMoveControl extends MoveControl {
        public FrogmanMoveControl(Mob mob) {
            super(mob);
        }

        @Override
        public void tick() {
            if (FrogmanEntity.this.isMovementLocked()) {
                this.operation = Operation.WAIT;
                FrogmanEntity.this.setSpeed(0.0F);
                FrogmanEntity.this.xxa = 0.0F;
                FrogmanEntity.this.yya = 0.0F;
                FrogmanEntity.this.zza = 0.0F;
                return;
            }
            super.tick();
        }
    }
}
