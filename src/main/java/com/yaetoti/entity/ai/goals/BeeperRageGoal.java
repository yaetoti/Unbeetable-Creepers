package com.yaetoti.entity.ai.goals;

import com.google.common.collect.Range;
import com.yaetoti.entity.BeeperEntity;
import com.yaetoti.holders.ModSounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.predicate.entity.EntityPredicates;

import java.util.EnumSet;

public class BeeperRageGoal extends Goal {
    protected final BeeperEntity mob;
    private final double speed;
    private final Range<Double> attackRange;
    private Path path;
    private boolean lostEyeContact;
    LivingEntity lastTarget;
    private double targetX;
    private double targetY;
    private double targetZ;
    private long lastUpdateTime;

    public BeeperRageGoal(BeeperEntity mob, double speed, Range<Double> attackRange) {
        this.mob = mob;
        this.speed = speed;
        this.attackRange = attackRange;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK, Control.TARGET));
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public boolean canStart() {
        if (mob.getAnnoyance() < 0.85f) {
            return false;
        }

        // Once a second
        long l = mob.getWorld().getTime();
        if (l - lastUpdateTime < 20L) {
            return false;
        }
        lastUpdateTime = l;

        // Has target
        lastTarget = this.mob.getTarget();
        if (lastTarget == null || !lastTarget.isAlive()) {
            return false;
        }
        // In range + Have eye contact
        if (!mob.getVisibilityCache().canSee(lastTarget)
                || mob.distanceTo(lastTarget) > attackRange.upperEndpoint()
                || mob.distanceTo(lastTarget) < attackRange.lowerEndpoint()) {
            return false;
        }

        // Remember position
        var eyePos = lastTarget.getEyePos();
        targetX = eyePos.getX();
        targetY = eyePos.getY();
        targetZ = eyePos.getZ();
        // There is a path to the target
        path = mob.getNavigation().findPathTo(targetX, targetY, targetZ, 0);
        if (path != null && path.reachesTarget()) {
            return true;
        }

        return false;
    }

    @Override
    public boolean shouldContinue() {
        // Entity disappeared / went to creative
        if (lastTarget == null || !lastTarget.isAlive() || !EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR.test(lastTarget)) {
            mob.setTarget(null);
            return false;
        }

        return true;
    }

    @Override
    public void start() {
        mob.getNavigation().startMovingAlong(this.path, this.speed);
        mob.setAttacking(true);
        lostEyeContact = false;
        // Disable targeting goals
        mob.setTargetingEnabled(false);
        mob.getWorld().playSoundFromEntity(null, mob, ModSounds.AAA, mob.getSoundCategory(), 1.0f, 1.0f);
    }

    @Override
    public void stop() {
        LivingEntity livingEntity = mob.getTarget();
        if (!EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR.test(livingEntity)) {
            mob.setTarget(null);
        }
        mob.setAttacking(false);
        mob.getNavigation().stop();
        // Enable targeting goals
        mob.setTargetingEnabled(true);
    }

    @Override
    public void tick() {
        // Follow entity. If eye contact was lost - target last entity position
        LivingEntity targetEntity = this.mob.getTarget();
        if (!lostEyeContact && (targetEntity == null || !targetEntity.isAlive() || !mob.getVisibilityCache().canSee(targetEntity))) {
            lostEyeContact = true;
        }

        if (!lostEyeContact) {
            var eyePos = targetEntity.getEyePos();
            targetX = eyePos.getX();
            targetY = eyePos.getY();
            targetZ = eyePos.getZ();
        }

        // Update pathfinding
        //if (!mob.getNavigation().isFollowingPath()) {
        mob.getNavigation().startMovingTo(targetX, targetY, targetZ, speed);
        //}

        mob.getLookControl().lookAt(targetX, targetY, targetZ, 30.0f, 30.0f);
        if (mob.squaredDistanceTo(targetX, targetY, targetZ) <= 3.5) {
            mob.explode();
        }
    }
}

