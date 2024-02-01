package com.yaetoti.entity.ai.goals;

import com.yaetoti.entity.BeeperEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.sound.SoundEvents;

import java.util.EnumSet;

public class BeeperFuseGoal extends Goal {
    private final BeeperEntity mob;
    private final EntityNavigation mobNavigation;
    private int delusionTimeout;
    private int annoyanceTimeout;
    private boolean deludedRecently;

    public BeeperFuseGoal(BeeperEntity creeper) {
        this.mob = creeper;
        this.mobNavigation = mob.getNavigation();
        this.setControls(EnumSet.of(Goal.Control.MOVE));
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

        return mob.getFuseSpeed() > 0 || mob.getTargetDistance() < 3.0f;
    }

    @Override
    public boolean shouldContinue() {
        return super.shouldContinue();
    }

    @Override
    public void start() {
        delusionTimeout = (int)(100 + mob.getRandom().nextFloat() * 100);
        annoyanceTimeout = 0;
        deludedRecently = false;
        mobNavigation.stop();
        mobNavigation.setSpeed(0);
    }

    @Override
    public void stop() {
        mob.setFuseSpeed(-1);
    }

    @Override
    public void tick() {
        LivingEntity targetEntity = mob.getLastTarget();
        if (!mob.getVisibilityCache().canSee(targetEntity)) {
            mob.setFuseSpeed(-1);
        }

        if (delusionTimeout == 0) {
            mob.playSound(SoundEvents.ENTITY_CREEPER_PRIMED, 1.0f, 0.5f);
            delusionTimeout = (int)(100 + mob.getRandom().nextFloat() * 300);
            annoyanceTimeout = 60;
            deludedRecently = true;
        }

        if (delusionTimeout > 0) {
            --delusionTimeout;
        }

        // Played fuse sound without actually fusing
        if (deludedRecently) {
            if (annoyanceTimeout > 0) {
                --annoyanceTimeout;
            }

            // Player didn't react. Otherwise, FleeGoal would be called
            if (annoyanceTimeout == 0) {
                deludedRecently = false;
                mob.increaseAnnoyance(0.1f);
            }
        }

        // this.mob.setFuseSpeed(mob.getAnnoyance() * 2);
    }
}
