package sfiomn.legendarycreatures.entities.goals;

import net.minecraft.world.entity.ai.goal.Goal;
import sfiomn.legendarycreatures.entities.FrogmanEntity;

import java.util.EnumSet;

public class FrogmanSleepGoal extends Goal {

    private final FrogmanEntity frogman;

    public FrogmanSleepGoal(FrogmanEntity frogman) {
        this.frogman = frogman;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return this.frogman.isSleeping() || this.frogman.isWakingUp();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        this.frogman.getNavigation().stop();
    }

    @Override
    public void tick() {
        this.frogman.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}
