package com.yaetoti.entity.ai.goals;

import com.google.common.collect.Range;
import com.yaetoti.entity.BeeperEntity;
import com.yaetoti.holders.ModSounds;
import com.yaetoti.utils.Annoyance;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class BeeperRageGoal extends Goal {
    protected final BeeperEntity mob;
    private final double speed;
    private final Range<Double> attackRange;
    private final EntityNavigation mobNavigation;
    @Nullable
    private Path path;

    private boolean lostEyeContact;
    private LivingEntity lastTarget;
    private double targetX;
    private double targetY;
    private double targetZ;

    public BeeperRageGoal(BeeperEntity mob, double speed, Range<Double> attackRange) {
        this.mob = mob;
        this.speed = speed;
        this.attackRange = attackRange;
        mobNavigation = mob.getNavigation();
        setControls(EnumSet.of(Control.MOVE, Control.LOOK, Control.TARGET));
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public boolean canStart() {
        Annoyance annoyance = Annoyance.of(mob.getAnnoyance());
        if (annoyance != Annoyance.REVENGE) {
            return false;
        }

        lastTarget = mob.getLastTarget();
        if (lastTarget == null || !lastTarget.isAlive() || !EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR.test(lastTarget)) {
            return false;
        }

        if (!mob.getVisibilityCache().canSee(lastTarget)
                || mob.distanceTo(lastTarget) > attackRange.upperEndpoint()
                || mob.distanceTo(lastTarget) < attackRange.lowerEndpoint()) {
            return false;
        }

        Vec3d targetPos = mob.getTargetPos();
        path = mobNavigation.findPathTo(targetPos.getX(), targetPos.getY(), targetPos.getZ(), 0);
        if (path == null) {
            return false;
        }

        return path.reachesTarget();
    }

    @Override
    public boolean shouldContinue() {
        if (lastTarget == null || !lastTarget.isAlive() || !EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR.test(lastTarget)) {
            return false;
        }

        return true;
    }

    @Override
    public void start() {
        lostEyeContact = false;
        mob.getNavigation().startMovingAlong(path, speed);
        // TODO: Kludge
        // Disable targeting
        mob.setTargetingEnabled(false);
        mob.getWorld().playSoundFromEntity(null, mob, ModSounds.AAA, mob.getSoundCategory(), 1.0f, 1.0f);
    }

    @Override
    public void stop() {
        mobNavigation.stop();
        mobNavigation.setSpeed(0);
        // TODO: Kludge
        // Enable targeting goals
        mob.setTargetingEnabled(true);
    }

    @Override
    public void tick() {
        if (!lostEyeContact && !mob.getVisibilityCache().canSee(lastTarget)) {
            lostEyeContact = true;
        }

        if (!lostEyeContact) {
            Vec3d eyePos = lastTarget.getEyePos();
            targetX = eyePos.getX();
            targetY = eyePos.getY();
            targetZ = eyePos.getZ();
        }

        mob.getNavigation().startMovingTo(targetX, targetY, targetZ, speed);
        mob.getLookControl().lookAt(targetX, targetY, targetZ, 30.0f, 30.0f);
        if (mob.squaredDistanceTo(targetX, targetY, targetZ) <= 3.0) {
            mob.explode();
        }
    }
}

