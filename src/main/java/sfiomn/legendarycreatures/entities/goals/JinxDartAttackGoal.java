package sfiomn.legendarycreatures.entities.goals;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.goal.Goal;
import sfiomn.legendarycreatures.entities.JinxEntity;

import java.util.EnumSet;

/**
 * Ranged dart attack, modelled on vanilla RangedAttackGoal but:
 *  - never shoots while the Jinx is still playing its startle animation
 *  - keeps moving during startle (so the Jinx isn't frozen in place)
 *  - leaves the "too close" case to the flee goal
 */
public class JinxDartAttackGoal extends Goal {

    private final JinxEntity jinx;
    private final double speedModifier;
    private final int attackIntervalMin;
    private final int attackIntervalMax;
    private final float attackRadius;
    private final float attackRadiusSqr;

    private LivingEntity target;
    private int attackTime = -1;
    private int seeTime;

    public JinxDartAttackGoal(JinxEntity mob, double speedModifier, int attackIntervalMin, int attackIntervalMax, float attackRadius) {
        this.jinx = mob;
        this.speedModifier = speedModifier;
        this.attackIntervalMin = attackIntervalMin;
        this.attackIntervalMax = attackIntervalMax;
        this.attackRadius = attackRadius;
        this.attackRadiusSqr = attackRadius * attackRadius;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity t = this.jinx.getTarget();
        if (t != null && t.isAlive()) {
            this.target = t;
            return true;
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse() || (this.target != null && this.target.isAlive() && !this.jinx.getNavigation().isDone());
    }

    @Override
    public void stop() {
        this.target = null;
        this.seeTime = 0;
        this.attackTime = -1;
        this.jinx.setApproaching(false);
        this.jinx.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        double distSqr = this.jinx.distanceToSqr(this.target.getX(), this.target.getY(), this.target.getZ());
        boolean canSee = this.jinx.getSensing().hasLineOfSight(this.target);
        if (canSee) this.seeTime++; else this.seeTime = 0;

        // Airborne: keep facing the player through the whole escape animation (but keep shooting).
        if (this.jinx.isFlying()) {
            this.jinx.lookAt(EntityAnchorArgument.Anchor.EYES, this.target.position());
        }

        // Startle and laugh hold the Jinx still, facing its target. It CAN shoot while airborne:
        // the escape animation already holds the sarbacane to its mouth, so it stays the visible pose.
        boolean holding = this.jinx.isPosturing();

        // Peacock-spider style: stand still and face the player for the whole animation.
        if (holding) {
            this.jinx.setApproaching(false);
            this.jinx.getNavigation().stop();
            this.jinx.lookAt(EntityAnchorArgument.Anchor.EYES, this.target.position());
            this.jinx.getLookControl().setLookAt(this.target, 30.0F, 30.0F);

            // Kill leftover momentum so it is completely still (but never mid-leap).
            if (this.jinx.isPosturing() && !this.jinx.isFlying() && this.jinx.onGround()) {
                Vec3 v = this.jinx.getDeltaMovement();
                this.jinx.setDeltaMovement(0.0D, v.y, 0.0D);
            }

            this.attackTime = 10; // shoot shortly after the animation finishes
            return;
        }

        // Approach if out of range or blind; otherwise hold position and shoot.
        if (distSqr > this.attackRadiusSqr || this.seeTime < 5) {
            this.jinx.getNavigation().moveTo(this.target, this.speedModifier);
            this.jinx.setApproaching(true);   // run animation, no flee sound
        } else {
            this.jinx.getNavigation().stop();
            this.jinx.setApproaching(false);
        }

        this.jinx.getLookControl().setLookAt(this.target, 30.0F, 30.0F);

        if (--this.attackTime == 0) {
            if (canSee && distSqr <= this.attackRadiusSqr && !holding && !this.jinx.isInWater()) {
                this.jinx.performRangedAttack(this.target, 1.0F);
                this.attackTime = this.attackIntervalMin + this.jinx.getRandom().nextInt(Math.max(1, this.attackIntervalMax - this.attackIntervalMin));
            } else {
                this.attackTime = 5; // not ready yet, re-check shortly
            }
        } else if (this.attackTime < 0) {
            this.attackTime = this.attackIntervalMin;
        }
    }
}
