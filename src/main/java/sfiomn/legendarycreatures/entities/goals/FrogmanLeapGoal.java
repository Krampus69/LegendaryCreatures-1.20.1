package sfiomn.legendarycreatures.entities.goals;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import sfiomn.legendarycreatures.entities.FrogmanEntity;
import sfiomn.legendarycreatures.registry.SoundRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

public class FrogmanLeapGoal extends Goal {
    private static final int WINDUP_TICKS = 3;
    private static final int MAX_DURATION_TICKS = 100;
    private static final int LANDING_COOLDOWN_TICKS = 20;
    private static final int[] ALLOWED_ANGLES = {40, 55, 60, 75, 80};

    private final FrogmanEntity frogman;
    private BlockPos leapTarget;
    private int ticks;
    private boolean leaped;
    private boolean landed;

    public FrogmanLeapGoal(FrogmanEntity frogman) {
        this.frogman = frogman;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (!this.frogman.isLeapWanted())
            return false;
        if (this.frogman.getLeapCooldown() > 0)
            return false;
        if (!this.frogman.onGround() && !this.frogman.isInWater())
            return false;

        LivingEntity target = this.frogman.getTarget();
        if (target == null) {
            this.frogman.setLeapWanted(false);
            return false;
        }
        if (!this.frogman.level().noCollision(this.frogman, this.frogman.getBoundingBox().move(0.0D, 2.0D, 0.0D))) {
            this.frogman.setLeapWanted(false);
            return false;
        }

        BlockPos candidate = this.snapToSurface(this.frogman.randomPointBehindTarget(target));
        if (candidate == null || !this.frogman.level().getBlockState(candidate.below()).isSolid()) {
            this.frogman.setLeapWanted(false);
            return false;
        }
        if (!this.frogman.hasLineOfSightTo(Vec3.atCenterOf(candidate))
                && !this.frogman.hasLineOfSightTo(Vec3.atCenterOf(candidate.above(4)))) {
            this.frogman.setLeapWanted(false);
            return false;
        }

        this.leapTarget = candidate;
        return true;
    }

    private BlockPos snapToSurface(Vec3 point) {
        BlockHitResult down = this.frogman.level().clip(new ClipContext(point,
                point.add(0.0D, -10.0D, 0.0D), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.frogman));
        if (down.getType() == HitResult.Type.BLOCK)
            return BlockPos.containing(down.getLocation()).above();

        BlockHitResult up = this.frogman.level().clip(new ClipContext(point,
                point.add(0.0D, 10.0D, 0.0D), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.frogman));
        if (up.getType() == HitResult.Type.BLOCK)
            return BlockPos.containing(up.getLocation());

        return null;
    }

    @Override
    public void start() {
        this.ticks = 0;
        this.leaped = false;
        this.landed = false;
        this.frogman.getNavigation().stop();
        this.frogman.setLeaping(true);
    }

    @Override
    public boolean canContinueToUse() {
        return !this.landed && this.ticks < MAX_DURATION_TICKS && this.leapTarget != null;
    }

    @Override
    public void tick() {
        this.ticks++;

        if (!this.leaped && this.ticks >= WINDUP_TICKS) {
            Vec3 vector = this.calculateOptimalLeapVector(Vec3.atCenterOf(this.leapTarget));
            if (vector == null) {
                this.landed = true;
                return;
            }
            this.leaped = true;
            this.frogman.playSound(SoundRegistry.FROGMAN_LEAP.get(), 1.0F, 1.0F);
            this.frogman.skipTurnEasing();
            this.frogman.setYRot(this.frogman.yBodyRot);
            this.frogman.setDiscardFriction(true);
            this.frogman.setDeltaMovement(vector);
        } else if (this.leaped && this.ticks > WINDUP_TICKS + 4 && (this.frogman.onGround() || this.frogman.isInWater())) {
            this.landed = true;
        }
    }

    private Vec3 calculateOptimalLeapVector(Vec3 target) {
        List<Integer> angles = new ArrayList<>();
        for (int angle : ALLOWED_ANGLES)
            angles.add(angle);
        Collections.shuffle(angles, new java.util.Random(this.frogman.getRandom().nextLong()));

        float maxSpeed = 0.058333334F * (float) this.frogman.getAttributeValue(Attributes.FOLLOW_RANGE);
        double dx = target.x - this.frogman.getX();
        double dz = target.z - this.frogman.getZ();
        double dy = target.y - this.frogman.getY();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (horizontal < 1.0E-4D)
            return null;

        double gravity = 0.08D;
        for (int angleDeg : angles) {
            double angle = Math.toRadians(angleDeg);
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            double denominator = 2.0D * cos * cos * (horizontal * Math.tan(angle) - dy);
            if (denominator <= 0.0D)
                continue;
            double speedSqr = gravity * horizontal * horizontal / denominator;
            if (speedSqr <= 0.0D)
                continue;
            double speed = Math.sqrt(speedSqr);
            if (speed > maxSpeed)
                continue;
            double horizontalSpeed = speed * cos / horizontal;
            return new Vec3(dx * horizontalSpeed, speed * sin, dz * horizontalSpeed);
        }

        double fallback = Math.toRadians(55.0D);
        double speed = Math.min(maxSpeed, Math.sqrt(gravity * horizontal / Math.sin(2.0D * fallback)));
        double horizontalSpeed = speed * Math.cos(fallback) / horizontal;
        return new Vec3(dx * horizontalSpeed, speed * Math.sin(fallback), dz * horizontalSpeed);
    }

    @Override
    public void stop() {
        this.frogman.setDiscardFriction(false);
        this.frogman.setLeaping(false);
        this.frogman.setLeapWanted(false);
        if (this.leaped)
            this.frogman.setLeapCooldown(LANDING_COOLDOWN_TICKS);
        this.leapTarget = null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}
