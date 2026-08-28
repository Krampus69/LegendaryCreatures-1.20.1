package sfiomn.legendarycreatures.entities.goals;

import net.minecraft.world.entity.ai.goal.Goal;
import sfiomn.legendarycreatures.entities.ShroomlingEntity;

import java.util.EnumSet;

public class ShroomlingSneezeGoal extends Goal {

    private final ShroomlingEntity shroomling;

    public ShroomlingSneezeGoal(ShroomlingEntity shroomling) {
        this.shroomling = shroomling;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return this.shroomling.isSneezing();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        this.shroomling.getNavigation().stop();
    }

    @Override
    public void tick() {
        this.shroomling.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}
