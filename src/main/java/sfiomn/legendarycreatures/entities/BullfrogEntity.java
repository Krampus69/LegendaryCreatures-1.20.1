package sfiomn.legendarycreatures.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;
import sfiomn.legendarycreatures.LegendaryCreatures;
import sfiomn.legendarycreatures.entities.goals.BaseMeleeAttackGoal;
import sfiomn.legendarycreatures.entities.goals.RangedMeleeAttackGoal;
import sfiomn.legendarycreatures.registry.SoundRegistry;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import javax.annotation.Nullable;


public class BullfrogEntity extends AnimatedCreatureEntity implements Enemy {
    private final int baseAttackDuration = 10;
    private final int baseAttackActionPoint = 5;
    private final int baseAttackCoolDown = 5;
    private final int baseDistanceAttackDuration = 10;
    private final int baseDistanceAttackActionPoint = 5;
    public final int SHORT_RANGED_ATTACK = 7;
    public final int shortRangedAttackDistance = 6;
    public final int MIDDLE_RANGED_ATTACK = 8;
    public final int middleRangedAttackDistance = 8;
    public final int LONG_RANGED_ATTACK = 9;
    public final int longRangedAttackDistance = 10;

    public final RawAnimation ATTACK_ANIM = RawAnimation.begin().thenPlay("attack");
    public final RawAnimation TONGUE_ANIM = RawAnimation.begin().thenPlay("tongue");
    public final RawAnimation TONGUE2_ANIM = RawAnimation.begin().thenPlay("tongue2");
    public final RawAnimation TONGUE3_ANIM = RawAnimation.begin().thenPlay("tongue3");
    public final RawAnimation RUN_ANIM = RawAnimation.begin().thenPlay("run");
    public final RawAnimation HURT_ANIM = RawAnimation.begin().thenPlay("hurt");

    private static final float WATER_ACCELERATION = 0.09F;

    public BullfrogEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
        this.xpReward = 12;
        if (isLevel3())
            this.xpReward = 20;
        else if (isLevel2())
            this.xpReward = 10;
    }

    public static AttributeSupplier.Builder setCustomAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 25)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ARMOR, 0)
                .add(Attributes.ATTACK_DAMAGE, 5)
                .add(Attributes.FOLLOW_RANGE, 20)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
    }

    @Override protected double getLevel2HealthBonus() { return 15; }
    @Override protected double getLevel3HealthBonus() { return 35; }
    @Override protected double getLevel2AttackBonus() { return 5; }
    @Override protected double getLevel3AttackBonus() { return 10; }
    @Override public int getLevel2SpawnChance() { return 20; }
    @Override public int getLevel3SpawnChance() { return 4; }

    public static boolean checkBullfrogSpawnRules(EntityType<? extends Mob> entityType, LevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        if (level.getDifficulty() == Difficulty.PEACEFUL)
            return false;

        if (level.getLevelData().getDayTime() % 24000L >= 12000L)
            return false;

        if (level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, pos.getX(), pos.getZ()) < pos.getY())
            return false;

        if (level.getFluidState(pos).is(FluidTags.WATER))
            return true;

        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new AmphibiousPathNavigation(this, level);
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (this.isEffectiveAi() && this.isInWater()) {
            this.moveRelative(WATER_ACCELERATION, travelVector);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9D));
            if (this.getTarget() == null)
                this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.005D, 0.0D));
        } else {
            super.travel(travelVector);
        }
    }

    @Nullable

    @Override
    protected BodyRotationControl createBodyControl() {
        return new BullfrogEntity.BodyHelperController(this);
    }

    @Override
    public Component getName() {
        String descriptionId = "entity." + LegendaryCreatures.MOD_ID + ".bullfrog";
        if (isLevel2())
            descriptionId = "entity." + LegendaryCreatures.MOD_ID + ".bullfrog2";
        else if (isLevel3())
            descriptionId = "entity." + LegendaryCreatures.MOD_ID + ".bullfrog3";
        return Component.translatable(descriptionId);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();

        RangedMeleeAttackGoal rangedMeleeAttackGoal = new RangedMeleeAttackGoal(this, baseDistanceAttackDuration, baseDistanceAttackActionPoint, 100, shortRangedAttackDistance, longRangedAttackDistance + 1.0f, 1.0, true) {
            @Override
            protected void startAttack(LivingEntity target) {
                double distToTargetSqr = this.mob.distanceToSqr(target);
                if (distToTargetSqr < (middleRangedAttackDistance - 1.0f) * (middleRangedAttackDistance - 1.0f) + target.getBbWidth()) {
                    this.mob.setAttackAnimation(SHORT_RANGED_ATTACK);
                } else if (distToTargetSqr < (longRangedAttackDistance - 1.0f) * (longRangedAttackDistance - 1.0f) + target.getBbWidth()) {
                    this.mob.setAttackAnimation(MIDDLE_RANGED_ATTACK);
                } else {
                    this.mob.setAttackAnimation(LONG_RANGED_ATTACK);
                }
                this.attackAnimationTick = this.attackDuration;
                this.mob.playSound(SoundRegistry.BULLFROG_TONGUE_ATTACK.get(), 1.0f, 1.0f);
            }

            @Override
            protected boolean executeAttack(LivingEntity target) {
                boolean hit = super.executeAttack(target);
                if (hit) {
                    if (isLevel3())
                        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 2, false, true));

                    Vec3 targetToMobVector = this.mob.position().subtract(target.position());
                    Vec3 vector3d = target.onGround() ? targetToMobVector.scale(0.18) : targetToMobVector.scale(0.13);
                    target.setDeltaMovement(vector3d.x, 0.5D, vector3d.z);
                    target.hasImpulse = true;
                    if (target instanceof ServerPlayer player)
                        player.connection.send(new ClientboundSetEntityMotionPacket(target));
                }
                return hit;
            }

            @Override
            public boolean isAttacking() {
                return this.mob.getAttackAnimation() == SHORT_RANGED_ATTACK ||
                        this.mob.getAttackAnimation() == MIDDLE_RANGED_ATTACK ||
                        this.mob.getAttackAnimation() == LONG_RANGED_ATTACK;
            }
        };

        this.targetSelector.addGoal(4, new HurtByTargetGoal(this));
        this.goalSelector.addGoal(7, rangedMeleeAttackGoal);
        this.goalSelector.addGoal(8, new BaseMeleeAttackGoal(this, baseAttackDuration, baseAttackActionPoint, baseAttackCoolDown, 1.0, true) {
            @Override
            protected double getAttackReachSqr(LivingEntity entity) {
                return (double) (getMobLength() * 2.0F * getMobLength() * 2.0F + entity.getBbWidth());
            }

            @Override
            protected boolean executeAttack(LivingEntity target) {
                this.mob.playSound(SoundRegistry.BULLFROG_ATTACK.get(), 1.0f, 1.0f);
                boolean hit = super.executeAttack(target);
                if (hit && (isLevel2() || isLevel3())) {
                    target.addEffect(new MobEffectInstance(MobEffects.POISON, 150, 1, false, true));
                }
                return hit;
            }

            @Override
            public boolean canContinueToUse() {
                boolean canContinueToUse = super.canContinueToUse();
                if (canContinueToUse)
                    return !rangedMeleeAttackGoal.canUse();
                return false;
            }
        });
        this.targetSelector.addGoal(9, new NearestAttackableTargetGoal<>(this, Player.class, false));
        this.goalSelector.addGoal(7, new RandomStrollGoal(this, 0.9, 40));
    }

    private float getMobLength() {
        return 1.4f;
    }

    @Override
    public <E extends GeoAnimatable> PlayState attackingPredicate(AnimationState<E> state) {
        if (hasSpawnEffect() && this.tickCount < getSpawnAnimationTicks())
            return PlayState.CONTINUE;

        if (getAttackAnimation() == BASE_ATTACK) {
            return state.setAndContinue(ATTACK_ANIM);
        } else if (getAttackAnimation() == SHORT_RANGED_ATTACK) {
            return state.setAndContinue(TONGUE_ANIM);
        } else if (getAttackAnimation() == MIDDLE_RANGED_ATTACK) {
            return state.setAndContinue(TONGUE2_ANIM);
        } else if (getAttackAnimation() == LONG_RANGED_ATTACK) {
            return state.setAndContinue(TONGUE3_ANIM);
        }

        state.getController().forceAnimationReset();
        return PlayState.STOP;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundRegistry.BULLFROG_DEATH.get();
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundRegistry.BULLFROG_IDLE.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundRegistry.BULLFROG_STEP.get(), 1.0F, 1.0F);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        super.registerControllers(controllerRegistrar);
        controllerRegistrar.add(new AnimationController<>(this, "Hurt", 0, state -> PlayState.STOP)
                .triggerableAnim("hurt", HURT_ANIM));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide && !this.isDeadOrDying())
            triggerAnim("Hurt", "hurt");
        return hurt;
    }

    @Override
    public RawAnimation getSprintAnimation() {
        return RUN_ANIM;
    }

    // Similar to Phantom entity
    class BodyHelperController extends BodyRotationControl {
        public BodyHelperController(Mob p_i49925_2_) {
            super(p_i49925_2_);
        }

        public void clientTick() {
            BullfrogEntity.this.yHeadRot = BullfrogEntity.this.yBodyRot;
            BullfrogEntity.this.yBodyRot = BullfrogEntity.this.getYRot();
        }
    }
}
