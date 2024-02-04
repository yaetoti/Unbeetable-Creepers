package com.yaetoti.entity.ai.goals;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.ai.pathing.PathNode;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.function.Predicate;

public class BeeperTargetGoal<T extends LivingEntity> extends TrackTargetGoal {
    protected final Class<T> targetClass;
    /**
     * The reciprocal of chance to actually search for a target on every tick
     * when this goal is not started. This is also the average number of ticks
     * between each search (as in a poisson distribution).
     */
    protected final int reciprocalChance;
    @Nullable
    protected LivingEntity targetEntity;
    protected TargetPredicate targetPredicate;

    public BeeperTargetGoal(MobEntity mob, Class<T> targetClass, boolean checkVisibility) {
        this(mob, targetClass, 10, checkVisibility, false, null);
    }

    public BeeperTargetGoal(MobEntity mob, Class<T> targetClass, boolean checkVisibility, Predicate<LivingEntity> targetPredicate) {
        this(mob, targetClass, 10, checkVisibility, false, targetPredicate);
    }

    public BeeperTargetGoal(MobEntity mob, Class<T> targetClass, boolean checkVisibility, boolean checkCanNavigate) {
        this(mob, targetClass, 10, checkVisibility, checkCanNavigate, null);
    }

    public BeeperTargetGoal(MobEntity mob, Class<T> targetClass, int reciprocalChance, boolean checkVisibility, boolean checkCanNavigate, @Nullable Predicate<LivingEntity> targetPredicate) {
        super(mob, checkVisibility, checkCanNavigate);
        this.targetClass = targetClass;
        this.reciprocalChance = ActiveTargetGoal.toGoalTicks(reciprocalChance);
        this.targetPredicate = TargetPredicate.createAttackable().setBaseMaxDistance(getFollowRange()).setPredicate(targetPredicate);
        setControls(EnumSet.of(Goal.Control.TARGET));
    }

    @Override
    public boolean canStart() {
        if (reciprocalChance > 0 && mob.getRandom().nextInt(reciprocalChance) != 0) {
            return false;
        }
        findClosestTarget();
        return targetEntity != null;
    }

    @Override
    public void start() {
        mob.setTarget(targetEntity);
        super.start();
    }

    protected Box getSearchBox(double distance) {
        return mob.getBoundingBox().expand(distance, 4.0, distance);
    }

    protected void findClosestTarget() {
        targetEntity = targetClass == PlayerEntity.class || targetClass == ServerPlayerEntity.class
                ? mob.getWorld().getClosestPlayer(targetPredicate, mob, mob.getX(), mob.getEyeY(), mob.getZ())
                : mob.getWorld().getClosestEntity(mob.getWorld().getEntitiesByClass(targetClass, getSearchBox(getFollowRange()), livingEntity -> true), targetPredicate, mob, mob.getX(), mob.getEyeY(), mob.getZ());
    }

    public void setTargetEntity(@Nullable LivingEntity targetEntity) {
        this.targetEntity = targetEntity;
    }
}

abstract class TrackTargetGoal extends Goal {
    protected final MobEntity mob;
    protected final boolean checkVisibility;
    private final boolean checkCanNavigate;
    private int canNavigateFlag;
    private int checkCanNavigateCooldown;
    private int timeWithoutVisibility;
    @Nullable
    protected LivingEntity target;
    protected int maxTimeWithoutVisibility = 60;

    public TrackTargetGoal(MobEntity mob, boolean checkVisibility) {
        this(mob, checkVisibility, false);
    }

    public TrackTargetGoal(MobEntity mob, boolean checkVisibility, boolean checkNavigable) {
        this.mob = mob;
        this.checkVisibility = checkVisibility;
        this.checkCanNavigate = checkNavigable;
    }

    @Override
    public boolean shouldContinue() {
        LivingEntity targetEntity = mob.getTarget();
        if (targetEntity == null) {
            targetEntity = target;
            if (targetEntity == null) {
                return false;
            }
        }

        if (!mob.canTarget(targetEntity)) {
            return false;
        }

        Team mobTeam = mob.getScoreboardTeam();
        Team targetTeam = targetEntity.getScoreboardTeam();
        if (mobTeam != null && targetTeam == mobTeam) {
            return false;
        }

        double followRange = getFollowRange();
        if (mob.squaredDistanceTo(targetEntity) > followRange * followRange) {
            return false;
        }

        if (checkVisibility) {
            if (mob.getVisibilityCache().canSee(targetEntity)) {
                timeWithoutVisibility = 0;
            } else if (++timeWithoutVisibility > toGoalTicks(maxTimeWithoutVisibility)) {
                return false;
            }
        }

        mob.setTarget(targetEntity);
        return true;
    }

    @Override
    public void start() {
        canNavigateFlag = 0;
        checkCanNavigateCooldown = 0;
        timeWithoutVisibility = 0;
    }

    @Override
    public void stop() {
        mob.setTarget(null);
        target = null;
    }

    protected double getFollowRange() {
        return mob.getAttributeValue(EntityAttributes.GENERIC_FOLLOW_RANGE);
    }

    protected boolean canTrack(@Nullable LivingEntity target, TargetPredicate targetPredicate) {
        if (target == null || !targetPredicate.test(mob, target)) {
            return false;
        }

        if (!mob.isInWalkTargetRange(target.getBlockPos())) {
            return false;
        }

        if (checkCanNavigate) {
            if (--checkCanNavigateCooldown <= 0) {
                canNavigateFlag = 0;
            }
            if (canNavigateFlag == 0) {
                canNavigateFlag = canNavigateToEntity(target) ? 1 : 2;
            }
            if (canNavigateFlag == 2) {
                return false;
            }
        }
        return true;
    }

    private boolean canNavigateToEntity(LivingEntity entity) {
        checkCanNavigateCooldown = toGoalTicks(10 + mob.getRandom().nextInt(5));
        Path path = mob.getNavigation().findPathTo(entity, 0);
        if (path == null) {
            return false;
        }

        PathNode pathNode = path.getEnd();
        if (pathNode == null) {
            return false;
        }

        int dX = pathNode.x - entity.getBlockX();
        int dZ = pathNode.z - entity.getBlockZ();
        return dX * dX + dZ * dZ <= 2.25;
    }
}
