package com.yaetoti.entity.ai.goals;

import com.yaetoti.entity.BeeperEntity;
import net.minecraft.entity.ai.AboveGroundTargeting;
import net.minecraft.entity.ai.NoPenaltySolidTargeting;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class BeeperWanderAroundGoal extends Goal {
    private final BeeperEntity mob;
    private final double speed;

    public BeeperWanderAroundGoal(BeeperEntity mob, double speed) {
        this.setControls(EnumSet.of(Goal.Control.MOVE));
        this.mob = mob;
        this.speed = speed;
    }

    @Override
    public boolean canStart() {
        return mob.getNavigation().isIdle() && mob.getRandom().nextInt(10) == 0;
    }

    @Override
    public boolean shouldContinue() {
        return mob.getNavigation().isFollowingPath();
    }

    @Override
    public void start() {
        Vec3d vec3d = this.getRandomLocation();
        if (vec3d != null) {
            mob.getNavigation().startMovingAlong(mob.getNavigation().findPathTo(BlockPos.ofFloored(vec3d), 1), speed);
        }
    }

    @Nullable
    private Vec3d getRandomLocation() {
        Vec3d vec3d2 = mob.getRotationVec(0.0f);
        Vec3d vec3d3 = AboveGroundTargeting.find(mob, 8, 7, vec3d2.x, vec3d2.z, 1.5707964f, 3, 1);
        if (vec3d3 != null) {
            return vec3d3;
        }
        return NoPenaltySolidTargeting.find(mob, 8, 4, -2, vec3d2.x, vec3d2.z, 1.5707963705062866);
    }
}
