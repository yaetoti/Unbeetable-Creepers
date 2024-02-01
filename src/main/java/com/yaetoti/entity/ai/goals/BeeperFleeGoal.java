package com.yaetoti.entity.ai.goals;

import com.yaetoti.entity.BeeperEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class BeeperFleeGoal extends Goal {
    private final BeeperEntity mob;
    private final double slowSpeed;
    private final double fastSpeed;
    private final float fleeDistance;
    private final EntityNavigation mobNavigation;
    @Nullable
    protected Path fleePath;

    public BeeperFleeGoal(BeeperEntity mob, float distance, double slowSpeed, double fastSpeed) {
        this.mob = mob;
        this.fleeDistance = distance;
        this.slowSpeed = slowSpeed;
        this.fastSpeed = fastSpeed;
        this.mobNavigation = mob.getNavigation();
        this.setControls(EnumSet.of(Goal.Control.MOVE));
    }

    @Override
    public boolean canStart() {
        LivingEntity targetEntity = mob.getTarget();
        if (targetEntity == null || !targetEntity.isAlive()) {
            return false;
        }

        if (!mob.isSpotted() || mob.getTargetDistance() > 16.0f) {
            return false;
        }

        return updateFleePath(targetEntity);
    }

    @Override
    public boolean shouldContinue() {
        LivingEntity targetEntity = mob.getTarget();
        if (targetEntity == null || !targetEntity.isAlive()) {
            return false;
        }

        if (!mob.isSpotted() || mob.getTargetDistance() > 16.0f) {
            return false;
        }

        return true;
    }

    @Override
    public void start() {
        mobNavigation.startMovingAlong(fleePath, fastSpeed);
    }

    @Override
    public void stop() {

    }

    @Override
    public void tick() {
        if (!mob.getNavigation().isFollowingPath()) {
            if (updateFleePath(mob.getTarget())) {
                return;
            }

            this.mobNavigation.startMovingAlong(fleePath, fastSpeed);
        }

        if (mob.getTargetDistance() < 7) {
            mobNavigation.setSpeed(fastSpeed);
        } else {
            mobNavigation.setSpeed(slowSpeed);
        }
    }

    private boolean updateFleePath(@NotNull LivingEntity targetEntity) {
        Vec3d fleePoint = NoPenaltyTargeting.findFrom(mob, 16, 7, mob.getTargetPos());
        if (fleePoint == null) {
            mob.increaseAnnoyance(0.001f);
            return false;
        }
        if (targetEntity.squaredDistanceTo(fleePoint.x, fleePoint.y, fleePoint.z) <= mob.getTargetDistance() * mob.getTargetDistance()) {
            mob.increaseAnnoyance(0.001f);
            return false;
        }

        fleePath = mobNavigation.findPathTo(fleePoint.x, fleePoint.y, fleePoint.z, 0);
        if (fleePath == null) {
            mob.increaseAnnoyance(0.001f);
            return false;
        }

        return true;
    }
}
