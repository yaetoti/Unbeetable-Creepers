package com.yaetoti.entity.ai.goals;

import com.yaetoti.entity.BeeperEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class BeeperFollowGoal extends Goal {
    private final BeeperEntity mob;
    private final double speed;
    private final EntityNavigation mobNavigation;
    @Nullable
    private Path path;

    public BeeperFollowGoal(BeeperEntity mob, double speed) {
        this.mob = mob;
        this.speed = speed;
        this.mobNavigation = mob.getNavigation();
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public boolean canStart() {
        LivingEntity targetEntity = mob.getLastTarget();
        if (targetEntity == null || !targetEntity.isAlive() || !EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR.test(targetEntity)) {
            return false;
        }

        if (mob.isSpotted() && mob.getTargetDistance() < 16.0f) {
            return false;
        }

        Vec3d targetPos = mob.getTargetPos();
        path = mobNavigation.findPathTo(targetPos.getX(), targetPos.getY(), targetPos.getZ(), 0);
        return path != null;
    }

    @Override
    public boolean shouldContinue() {
        LivingEntity targetEntity = mob.getLastTarget();
        if (targetEntity == null || !targetEntity.isAlive() || !EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR.test(targetEntity)) {
            return false;
        }

        if (mob.isSpotted() && mob.getTargetDistance() < 16.0f) {
            return false;
        }

        return true;
    }

    @Override
    public void start() {
        mobNavigation.startMovingAlong(path, speed);
    }

    @Override
    public void stop() {
        if (!EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR.test(mob.getLastTarget())) {
            mob.setTarget(null);
        }

        mobNavigation.stop();
        mobNavigation.setSpeed(0);
    }

    @Override
    public void tick() {
        LivingEntity targetEntity = mob.getLastTarget();
        mob.getLookControl().lookAt(targetEntity, 30.0f, 30.0f);

        if (mob.getTargetDistance() <= 2.5f) {
            mobNavigation.stop();
            mobNavigation.setSpeed(0);
            return;
        } else if (mob.getTargetDistance() <= 5) {
            mobNavigation.setSpeed(speed * 0.2);
        }

        if (!mob.getNavigation().isFollowingPath()) {
            Vec3d targetPos = mob.getTargetPos();
            path = mobNavigation.findPathTo(targetPos.getX(), targetPos.getY(), targetPos.getZ(), 0);
            mobNavigation.startMovingAlong(path, speed);
        }
    }
}
