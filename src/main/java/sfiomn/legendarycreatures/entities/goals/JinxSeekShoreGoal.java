package sfiomn.legendarycreatures.entities.goals;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import sfiomn.legendarycreatures.entities.JinxEntity;

import java.util.EnumSet;

/**
 * The Jinx can't shoot from water, so when it ends up swimming it makes for the nearest shore.
 * Runs above the attack and flee goals: getting out is the priority.
 */
public class JinxSeekShoreGoal extends Goal {

    private static final int SEARCH_RADIUS = 8;
    private static final int SEARCH_HEIGHT = 3;

    private final JinxEntity jinx;
    private final double speedModifier;
    private BlockPos shore;

    public JinxSeekShoreGoal(JinxEntity jinx, double speedModifier) {
        this.jinx = jinx;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!this.jinx.isInWater()) return false;
        this.shore = findNearestShore();
        return this.shore != null;
    }

    @Override
    public boolean canContinueToUse() {
        // Keep going until it is out of the water (or the path dies).
        return this.jinx.isInWater() && this.shore != null && !this.jinx.getNavigation().isDone();
    }

    @Override
    public void start() {
        this.jinx.getNavigation().moveTo(
                this.shore.getX() + 0.5D, this.shore.getY(), this.shore.getZ() + 0.5D, this.speedModifier);
    }

    @Override
    public void stop() {
        this.shore = null;
        this.jinx.getNavigation().stop();
    }

    /** Closest standable, non-water block with headroom. */
    private BlockPos findNearestShore() {
        Level level = this.jinx.level();
        BlockPos origin = this.jinx.blockPosition();
        BlockPos best = null;
        double bestDistSqr = Double.MAX_VALUE;

        for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
            for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz++) {
                for (int dy = -1; dy <= SEARCH_HEIGHT; dy++) {
                    BlockPos pos = origin.offset(dx, dy, dz);

                    boolean solidFooting = level.getBlockState(pos.below()).isSolid();
                    boolean dryFeet = level.getFluidState(pos).isEmpty()
                            && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
                    boolean headroom = level.getFluidState(pos.above()).isEmpty()
                            && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty();

                    if (solidFooting && dryFeet && headroom) {
                        double distSqr = pos.distSqr(origin);
                        if (distSqr < bestDistSqr) {
                            bestDistSqr = distSqr;
                            best = pos.immutable();
                        }
                    }
                }
            }
        }
        return best;
    }
}
