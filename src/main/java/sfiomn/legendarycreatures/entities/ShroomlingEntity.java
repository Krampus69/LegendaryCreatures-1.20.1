package sfiomn.legendarycreatures.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import sfiomn.legendarycreatures.entities.goals.ShroomlingSneezeGoal;
import sfiomn.legendarycreatures.registry.SoundRegistry;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import javax.annotation.Nullable;

public class ShroomlingEntity extends AnimatedCreatureEntity {

    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation ATCHOO_ANIM = RawAnimation.begin().thenPlay("atchoo");

    private static final EntityDataAccessor<Boolean> SNEEZING =
            SynchedEntityData.defineId(ShroomlingEntity.class, EntityDataSerializers.BOOLEAN);

    private static final int SNEEZE_CHANCE = 20;
    private static final int SNEEZE_DURATION = 52;

    private int sneezeTicks;
    private boolean wasMoving;

    public ShroomlingEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 1;
    }

    public static AttributeSupplier.Builder setCustomAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1)
                .add(Attributes.MOVEMENT_SPEED, 0.08)
                .add(Attributes.ARMOR, 0)
                .add(Attributes.ATTACK_DAMAGE, 0)
                .add(Attributes.FOLLOW_RANGE, 12)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SNEEZING, Boolean.FALSE);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();

        this.goalSelector.addGoal(0, new ShroomlingSneezeGoal(this));
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(4, new RandomStrollGoal(this, 1.0, 120) {
            @Override
            protected Vec3 getPosition() {
                return DefaultRandomPos.getPos(this.mob, 8, 4);
            }
        });
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    public boolean isSneezing() {
        return this.entityData.get(SNEEZING);
    }

    public void setSneezing(boolean sneezing) {
        this.entityData.set(SNEEZING, sneezing);
    }

    private void startSneeze() {
        setSneezing(true);
        this.sneezeTicks = SNEEZE_DURATION;
        getNavigation().stop();
        playSound(SoundRegistry.SHROOMLING_ATCHOO.get(), 1.0F, 1.0F);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();

        if (isSneezing()) {
            if (--this.sneezeTicks <= 0)
                setSneezing(false);

            this.wasMoving = false;
            return;
        }

        boolean moving = !getNavigation().isDone();

        if (this.wasMoving && !moving && this.random.nextInt(SNEEZE_CHANCE) == 0)
            startSneeze();

        this.wasMoving = moving;
    }

    @Override
    public <E extends GeoAnimatable> PlayState movementPredicate(AnimationState<E> state) {
        if (isSneezing())
            return state.setAndContinue(ATCHOO_ANIM);
        return super.movementPredicate(state);
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
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("sneezeTicks", this.sneezeTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.sneezeTicks = nbt.getInt("sneezeTicks");
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundRegistry.SHROOMLING_IDLE.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundRegistry.SHROOMLING_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
    }

    public boolean isPushable() {
        return false;
    }

    protected void doPush(@NotNull Entity entity) {
    }

    protected void pushEntities() {
    }
}
