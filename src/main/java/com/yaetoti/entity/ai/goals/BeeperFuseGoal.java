package com.yaetoti.entity.ai.goals;

import com.yaetoti.entity.BeeperEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class BeeperFuseGoal extends Goal {
    private final BeeperEntity mob;
    @Nullable
    private LivingEntity target;

    public BeeperFuseGoal(BeeperEntity creeper) {
        this.mob = creeper;
        this.setControls(EnumSet.of(Goal.Control.MOVE));
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public boolean canStart() {
        if (mob.getAnnoyance() <= 0.95f) {
            return false;
        }

        LivingEntity livingEntity = this.mob.getTarget();
        return mob.getFuseSpeed() > 0 || livingEntity != null && mob.squaredDistanceTo(livingEntity) < 9.0;
    }

    @Override
    public boolean shouldContinue() {
        return super.shouldContinue();
    }

    @Override
    public void start() {
        this.mob.getNavigation().stop();
        this.target = this.mob.getTarget();
    }

    @Override
    public void stop() {
        this.target = null;
    }

    @Override
    public void tick() {
        if (this.target == null) {
            this.mob.setFuseSpeed(-1);
            return;
        }
        if (this.mob.squaredDistanceTo(this.target) > 49.0) {
            this.mob.setFuseSpeed(-1);
            return;
        }
        if (!this.mob.getVisibilityCache().canSee(this.target)) {
            this.mob.setFuseSpeed(-1);
            return;
        }
        this.mob.setFuseSpeed(mob.getAnnoyance() * 2);
    }
}
