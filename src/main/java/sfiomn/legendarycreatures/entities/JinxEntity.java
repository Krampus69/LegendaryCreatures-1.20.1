package sfiomn.legendarycreatures.entities;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import sfiomn.legendarycreatures.entities.goals.JinxDartAttackGoal;
import sfiomn.legendarycreatures.registry.SoundRegistry;
import sfiomn.legendarycreatures.sounds.JinxSounds;
import sfiomn.legendarycreatures.entities.goals.JinxSeekShoreGoal;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

public class JinxEntity extends AnimatedCreatureEntity implements Enemy, RangedAttackMob {

    // How long the startle animation lasts (1.5s = 30 ticks). No shooting until it finishes.
    private static final int STARTLE_TICKS = 30;
    // Stays alert for 60s after being startled; idles with idle_alert during that window.
    private static final int ALERT_TICKS = 1200;
    // How long the laugh animation lasts (1.5s = 30 ticks). Held still and facing the target throughout.
    private static final int LAUGH_TICKS = 30;
    // The dance is twice as long (3s = 60 ticks).
    private static final int DANCE_TICKS = 60;

    // Locomotion
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation IDLE_ALERT = RawAnimation.begin().thenLoop("idle_alert");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation RUN  = RawAnimation.begin().thenLoop("run");
    private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("swim");
    private static final RawAnimation APPROACH = RawAnimation.begin().thenLoop("approach");
    // One-shot actions (triggered from code, synced to clients by GeckoLib)
    private static final RawAnimation STARTLE = RawAnimation.begin().thenPlay("startle");
    private static final RawAnimation SHOOT   = RawAnimation.begin().thenPlay("shoot");
    private static final RawAnimation LAUGH   = RawAnimation.begin().thenPlay("laugh");
    // Played instead of the laugh when two darts land in a row (3s of self-imposed downtime,
    // giving the player breathing room against levitation chains).
    private static final RawAnimation DANCE   = RawAnimation.begin().thenPlay("dance");
    // Airborne after the leap. Holds its last frame if the flight outlasts the 10s clip.
    private static final RawAnimation ESCAPE  = RawAnimation.begin().thenPlayAndHold("escape");

    // Fleeing must be known on the client so the run animation plays; getTarget() is server-only.
    private static final EntityDataAccessor<Boolean> FLEEING =
            SynchedEntityData.defineId(JinxEntity.class, EntityDataSerializers.BOOLEAN);

    // Airborne from the leap. Synced so the client can play the escape animation.
    private static final EntityDataAccessor<Boolean> FLYING =
            SynchedEntityData.defineId(JinxEntity.class, EntityDataSerializers.BOOLEAN);

    // On edge after a startle. Synced so the client knows which idle animation to play.
    private static final EntityDataAccessor<Boolean> ALERT =
            SynchedEntityData.defineId(JinxEntity.class, EntityDataSerializers.BOOLEAN);

    // Closing in on its target (out of dart range). Synced so the client plays the run animation
    // — deliberately NOT the FLEEING flag, so the flee sound doesn't fire on approach.
    private static final EntityDataAccessor<Boolean> APPROACHING =
            SynchedEntityData.defineId(JinxEntity.class, EntityDataSerializers.BOOLEAN);

    // The dart leaves 0.2s (4 ticks) after the shoot animation/sound start.
    private static final int SHOOT_WINDUP_TICKS = 4;
    private int shootWindup;
    private LivingEntity pendingShootTarget;

    private boolean playingFleeSound;      // client only
    private boolean deathSoundCut;         // client only
    private boolean playingUmbrellaSound;  // client only
    private int startleTicks;
    private int alertTicks;
    private int laughTicks;
    private int consecutiveDartHits;
    private boolean hadTarget;

    public JinxEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 10; // twice a creeper (5)
        // Otherwise the ground navigator treats water as impassable and it can't path to shore.
        if (this.getNavigation() instanceof GroundPathNavigation navigation) {
            navigation.setCanFloat(true);
        }
    }

    public static AttributeSupplier.Builder setCustomAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10)
                .add(Attributes.MOVEMENT_SPEED, 0.23)   // regular zombie walking speed
                .add(Attributes.FOLLOW_RANGE, 24)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(FLEEING, false);
        this.entityData.define(FLYING, false);
        this.entityData.define(ALERT, false);
        this.entityData.define(APPROACHING, false);
    }

    public boolean isFleeing() { return this.entityData.get(FLEEING); }
    public void setFleeing(boolean fleeing) { this.entityData.set(FLEEING, fleeing); }

    public boolean isAlert() { return this.entityData.get(ALERT); }

    public boolean isApproaching() { return this.entityData.get(APPROACHING); }
    public void setApproaching(boolean approaching) {
        if (approaching != this.isApproaching()) this.entityData.set(APPROACHING, approaching);
    }

    public boolean isFlying() { return this.entityData.get(FLYING); }
    public void setFlying(boolean flying) { this.entityData.set(FLYING, flying); }

    public int getStartleTicks() { return this.startleTicks; }

    public int getLaughTicks() { return this.laughTicks; }

    /** True while startling or laughing: hold position and stare at the target. */
    public boolean isPosturing() { return this.startleTicks > 0 || this.laughTicks > 0; }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new FloatGoal(this));
        // Can't shoot from water -> getting back on land beats both fleeing and attacking.
        this.goalSelector.addGoal(2, new JinxSeekShoreGoal(this, 1.2D));
        // Player within 6 blocks -> flee to re-open the gap (run animation). Higher priority than attacking.
        this.goalSelector.addGoal(3, new FleeFromPlayerGoal(this));
        // Dart attack at skeleton range (15). Won't fire until the startle animation is over.
        this.goalSelector.addGoal(4, new JinxDartAttackGoal(this, 1.2D, 30, 50, 15.0F));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        LivingEntity target = this.getTarget();
        boolean hasTarget = target != null && target.isAlive();

        if (hasTarget && !this.hadTarget && !this.isFlying() && !this.isAlert()) {   // first detection while calm
            this.startleTicks = STARTLE_TICKS;
            this.alertTicks = ALERT_TICKS;      // startled -> alert for the next 60s
            this.triggerAnim("startle", "startle");
            this.playSound(SoundRegistry.JINX_STARTLE.get(), 2.0F, 1.0F);
        }
        if (!hasTarget) {
            this.startleTicks = 0;
            this.laughTicks = 0;
            this.consecutiveDartHits = 0;
        } else {
            if (this.startleTicks > 0) this.startleTicks--;
            if (this.laughTicks > 0) this.laughTicks--;
            this.alertTicks = ALERT_TICKS;      // still in combat -> stay alert; 60s counts from last contact
        }

        if (this.alertTicks > 0) this.alertTicks--;
        boolean alert = this.alertTicks > 0;
        if (alert != this.isAlert()) {
            this.entityData.set(ALERT, alert);
        }

        this.hadTarget = hasTarget;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("AlertTicks", this.alertTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.alertTicks = tag.getInt("AlertTicks");
        this.entityData.set(ALERT, this.alertTicks > 0);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        // Leap away when caught by a melee hit under open sky (must survive the hit to move).
        if (hurt && this.isAlive() && !this.level().isClientSide
                && isMeleeHit(source)
                && this.wantsToLeap(source)
                && this.isNearGround()
                && this.level().canSeeSky(this.blockPosition())) {
            this.parachuteJump();
        }
        return hurt;
    }

    private boolean wantsToLeap(DamageSource source) {
        // Either the flee goal is actively running, or a player caught it in melee within flee range.
        return this.isFleeing()
                || (source.getEntity() instanceof Player player && this.distanceTo(player) <= 10.0F);
    }

    /** True if solid ground is right beneath the feet (tolerates grass, carpets, slabs, edge-clipping). */
    private boolean isNearGround() {
        return this.onGround()
                || !this.level().noCollision(this, this.getBoundingBox().move(0.0D, -0.15D, 0.0D));
    }

    private static boolean isMeleeHit(DamageSource source) {
        return source.getDirectEntity() instanceof LivingEntity && !source.is(DamageTypeTags.IS_PROJECTILE);
    }

    @Override
    public void tick() {
        super.tick();

        // The flee sound has to follow the Jinx: it is running away while the clip is still playing,
        // and Entity.playSound() would leave the sound stranded where it started.
        if (this.level().isClientSide) {
            // The moment death starts, silence everything else so only the death cry plays.
            if (this.isDeadOrDying() && !this.deathSoundCut) {
                this.deathSoundCut = true;
                JinxSounds.stopAllOnDeath();
            }

            if (this.isFleeing() && !this.playingFleeSound) {
                this.playingFleeSound = true;
                JinxSounds.playFollowing(SoundRegistry.JINX_FLEE.get(), this, 1.0F, 1.0F);
            } else if (!this.isFleeing() && this.playingFleeSound) {
                this.playingFleeSound = false;
            }

            // Umbrella deploy: FLYING flips on at the leap; the sound follows the Jinx up.
            // The fly loop runs for the whole flight and cuts the moment FLYING clears
            // (which happens on landing or on touching water).
            if (this.isFlying() && !this.playingUmbrellaSound) {
                this.playingUmbrellaSound = true;
                JinxSounds.playFollowing(SoundRegistry.JINX_UMBRELLA.get(), this, 2.0F, 1.0F);
                JinxSounds.playFlyLoop(this);
            } else if (!this.isFlying() && this.playingUmbrellaSound) {
                this.playingUmbrellaSound = false;
            }
        }

        if (!this.level().isClientSide && this.shootWindup > 0 && --this.shootWindup == 0) {
            this.fireDart();
        }


        if (!this.level().isClientSide && this.isFlying()) {
            // Landing in water ends the flight immediately: the swim animation takes over.
            boolean landed = this.getDeltaMovement().y <= 0.0D && this.isNearGround();
            if (landed || this.isInWater()) {
                this.setFlying(false);
            }
        }
    }

    /** Same upward pop as the Legendary Additions parachute (Y = 1.55), plus slow falling. */
    private void parachuteJump() {
        Vec3 v = this.getDeltaMovement();
        this.setDeltaMovement(v.x, 1.55D, v.z);
        this.hurtMarked = true; // sync the new velocity to clients
        this.fallDistance = 0.0F;
        // ambient=false, visible=false -> no potion swirl particles on the Jinx.
        this.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0, false, false));
        this.setFlying(true);
        this.spawnBoostParticles();
    }

    /** Same burst as the Legendary Additions parachute boost (ParachuteFlightEvents.spawnBoostParticles). */
    private void spawnBoostParticles() {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    this.getX(), this.getY() + 0.1, this.getZ(),
                    20, 0.4, 0.1, 0.4, 0.02);
        }
    }

    /**
     * Starts the wind-up: the shoot animation and sound play immediately, and the dart is actually
     * released SHOOT_WINDUP_TICKS later (see fireDart), so the telegraph reads before the shot.
     */
    @Override
    public void performRangedAttack(LivingEntity target, float velocity) {
        if (this.level().isClientSide) return;
        if (this.isInWater()) return;   // no shooting while in water

        this.pendingShootTarget = target;
        this.shootWindup = SHOOT_WINDUP_TICKS;

        this.triggerAnim("shoot", "shoot");
        this.playSound(SoundRegistry.JINX_SHOOT.get(), 1.0F, 1.0F);
    }

    /** Releases the dart once the wind-up has elapsed. Aim is recomputed here so it tracks a moving target. */
    private void fireDart() {
        LivingEntity target = this.pendingShootTarget;
        this.pendingShootTarget = null;

        if (target == null || !target.isAlive() || this.isInWater() || !this.isAlive()) return;

        JinxDartEntity dart = new JinxDartEntity(this.level(), this);
        double dx = target.getX() - this.getX();
        double dy = target.getY(0.3333D) - dart.getY();
        double dz = target.getZ() - this.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        // 3.2 = twice the old 1.6 speed. Flight time halves, so the arc lift is halved too (0.2 -> 0.1).
        dart.shoot(dx, dy + horizontal * 0.1D, dz, 3.2F, 6.0F);

        this.level().addFreshEntity(dart);
    }

    /** Called by the dart when it misses (block hit, or a blocked/failed entity hit): the dance
     *  requires two clean touches in a row, so any miss restarts the count. */
    public void resetDartStreak() {
        this.consecutiveDartHits = 0;
    }

    /** Called by the dart when it actually damages a target (not shield-blocked). */
    public void triggerLaugh() {
        // No laughing mid-flight: the escape animation owns the pose, and the laugh's freeze
        // would stall the Jinx while it is drifting down.
        if (this.isFlying()) return;

        this.consecutiveDartHits++;
        if (this.consecutiveDartHits >= 2) {
            // Second hit in a row: gloat with the longer dance (3s frozen) instead of the laugh.
            this.consecutiveDartHits = 0;
            this.laughTicks = DANCE_TICKS;
            this.triggerAnim("laugh", "dance");
        } else {
            this.laughTicks = LAUGH_TICKS;
            this.triggerAnim("laugh", "laugh");
        }
        this.playSound(SoundRegistry.JINX_LAUGH.get(), 2.0F, 1.0F);
    }

    private PlayState handleLocomotion(AnimationState<JinxEntity> state) {
        // On the first ticks after (re)appearing client-side, snap straight into the animation
        // instead of blending from the model's bind pose — otherwise the umbrella (open in the
        // raw model) is briefly visible easing shut at spawn.
        state.getController().setTransitionLength(this.tickCount < 2 ? 0 : 4);

        if (this.isInWater())   return state.setAndContinue(SWIM);
        if (this.isFleeing())     return state.setAndContinue(RUN);
        if (this.isApproaching()) return state.setAndContinue(APPROACH);
        if (state.isMoving())   return state.setAndContinue(WALK);
        return state.setAndContinue(this.isAlert() ? IDLE_ALERT : IDLE);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundRegistry.JINX_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        // On the killing blow both getHurtSound and getDeathSound can fire; keep only the death cry.
        return this.isDeadOrDying() ? null : SoundRegistry.JINX_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundRegistry.JINX_DEATH.get();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 4, this::handleLocomotion));
        // Each triggered animation gets its own controller so they can blend at different speeds.
        // Startle: quick but not instant.
        controllers.add(new AnimationController<>(this, "startle", 2, state -> PlayState.STOP)
                .triggerableAnim("startle", STARTLE));
        // Shoot: snappy — a dart firing should feel sharp.
        controllers.add(new AnimationController<>(this, "shoot", 1, state -> PlayState.STOP)
                .triggerableAnim("shoot", SHOOT));
        // Laugh: eases back out to the idle pose instead of snapping.
        controllers.add(new AnimationController<>(this, "laugh", 5, state -> PlayState.STOP)
                .triggerableAnim("laugh", LAUGH)
                .triggerableAnim("dance", DANCE));
        // Registered LAST -> its bone transforms are applied after the others, so while the Jinx
        // is airborne "escape" overrides idle/walk/run and any triggered startle/shoot/laugh.
        controllers.add(new AnimationController<>(this, "escape", 2, state ->
                (this.isFlying() && !this.isInWater()) ? state.setAndContinue(ESCAPE) : PlayState.STOP));
    }

    /** Runs away when a player closes inside 6 blocks; flags fleeing for the run animation. */
    private static class FleeFromPlayerGoal extends AvoidEntityGoal<Player> {
        private final JinxEntity jinx;

        // Only panics when a player gets within 6 blocks. Beyond that it holds ground and shoots.
        private static final float FLEE_DISTANCE = 6.0F;
        // Always sprints away at 1.8x (both AvoidEntityGoal gears use the same speed).
        private static final double FLEE_SPEED = 1.8D;

        FleeFromPlayerGoal(JinxEntity mob) {
            super(mob, Player.class, FLEE_DISTANCE, FLEE_SPEED, FLEE_SPEED);
            this.jinx = mob;
        }

        @Override
        public boolean canUse() {
            // Don't flee while startling/laughing (hold still), nor while airborne — fleeing mid-air does
            // nothing but steal the MOVE/LOOK flags from the dart goal, which stops it shooting.
            return !this.jinx.isPosturing() && !this.jinx.isFlying() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            // Interrupt an in-progress flee if a startle/laugh begins, or once it leaps into the air.
            return !this.jinx.isPosturing() && !this.jinx.isFlying() && super.canContinueToUse();
        }

        @Override
        public void start() {
            super.start();
            this.jinx.setFleeing(true);
        }

        @Override
        public void stop() {
            super.stop();
            this.jinx.setFleeing(false);
        }
    }
}
