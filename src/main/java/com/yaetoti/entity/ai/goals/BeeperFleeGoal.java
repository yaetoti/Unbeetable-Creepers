package com.yaetoti.entity.ai.goals;

import com.yaetoti.entity.BeeperEntity;
import com.yaetoti.holders.ModSounds;
import com.yaetoti.utils.Annoyance;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class BeeperFleeGoal extends Goal {
    private final BeeperEntity mob;
    private final double slowSpeed;
    private final double fastSpeed;
    private final EntityNavigation mobNavigation;
    private int safeTicks;
    @Nullable
    protected Path fleePath;

    public BeeperFleeGoal(BeeperEntity mob, double slowSpeed, double fastSpeed) {
        this.mob = mob;
        this.slowSpeed = slowSpeed;
        this.fastSpeed = fastSpeed;
        this.mobNavigation = mob.getNavigation();
        this.setControls(EnumSet.of(Goal.Control.MOVE));
    }

    @Override
    public boolean canStart() {
        LivingEntity targetEntity = mob.getLastTarget();
        if (targetEntity == null || !targetEntity.isAlive() || !EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR.test(targetEntity)) {
            return false;
        }

        Annoyance annoyance = Annoyance.of(mob.getAnnoyance());
        if ((!mob.isSpotted() && mob.getFuseSpeed() <= 0)
                || mob.getTargetDistance() > 12.0f
                || ((annoyance == Annoyance.SCARE) && (mob.getTargetDistance() < 2.0))
                || ((annoyance == Annoyance.KILL) && (mob.getTargetDistance() < 4.0))) {
            return false;
        }

        return updateFleePath(targetEntity);
    }

    @Override
    public boolean shouldContinue() {
        LivingEntity targetEntity = mob.getLastTarget();
        if (targetEntity == null || !targetEntity.isAlive() || !EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR.test(targetEntity)) {
            return false;
        }

        if (!mob.isSpotted() || mob.getTargetDistance() > 16.0f) {
            ++safeTicks;
        } else {
            safeTicks = 0;
        }

        if (safeTicks >= 20) {
            return false;
        }

        return EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR.test(targetEntity);
    }

    @Override
    public void start() {
        safeTicks = 0;
        mobNavigation.startMovingAlong(fleePath, fastSpeed);
        if (mob.getTargetDistance() <= 4.0f) {
            mob.playSound(ModSounds.WOO, 8.0f, 1.0f);
        }
    }

    @Override
    public void stop() {

    }

    @Override
    public void tick() {
        if (!mob.getNavigation().isFollowingPath()) {
            if (updateFleePath(mob.getLastTarget())) {
                return;
            }

            mobNavigation.startMovingAlong(fleePath, fastSpeed);
            mob.decreaseAnnoyance(0.005f);
        }

        if (mob.getTargetDistance() < 7) {
            mobNavigation.setSpeed(fastSpeed);
        } else {
            mobNavigation.setSpeed(slowSpeed);
        }
    }

    private boolean updateFleePath(@NotNull LivingEntity targetEntity) {
        Vec3d fleePoint = NoPenaltyTargeting.findFrom(mob, 16, 5, mob.getTargetPos());
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
