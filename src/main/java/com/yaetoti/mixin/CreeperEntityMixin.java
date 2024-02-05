package com.yaetoti.mixin;

import com.yaetoti.entity.BeeperEntity;
import com.yaetoti.utils.Annoyance;
import net.minecraft.client.render.entity.feature.SkinOverlayOwner;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.passive.OcelotEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.EnumSet;

@Mixin(CreeperEntity.class)
public abstract class CreeperEntityMixin extends HostileEntity implements SkinOverlayOwner {
    @Shadow
    @Final
    private static TrackedData<Boolean> CHARGED;

    protected CreeperEntityMixin(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(
            at = @At(value = "HEAD"),
            method = "initGoals()V",
            cancellable = true
    )
    private void injectInitGoals(CallbackInfo info) {
        // TODO: Soft injection
        goalSelector.add(1, new SwimGoal(Self()));
        goalSelector.add(2, new CreeperIgniteGoal(Self()));
        goalSelector.add(3, new FleeEntityGoal<>(Self(), OcelotEntity.class, 6.0f, 1.0, 1.2));
        goalSelector.add(3, new FleeEntityGoal<>(Self(), CatEntity.class, 6.0f, 1.0, 1.2));
        goalSelector.add(4, new CreeperMeleeAttackGoal(Self(), 1.0, true));
        goalSelector.add(5, new WanderAroundFarGoal(Self(), 0.8));
        goalSelector.add(6, new LookAtEntityGoal(Self(), PlayerEntity.class, 8.0f));
        goalSelector.add(6, new LookAroundGoal(Self()));
        targetSelector.add(1, new ActiveTargetGoal<>(Self(), PlayerEntity.class, true));
        targetSelector.add(2, new RevengeGoal(Self()));
        info.cancel();
    }

    @Override
    protected void applyDamage(DamageSource source, float amount) {
        boolean shouldSpawn = false;
        float annoyance = 0.0f;

        if (amount < getHealth()) {
            if (getAttacker() != null && random.nextFloat() < 0.05f) {
                shouldSpawn = true;
                annoyance = random.nextFloat() * Annoyance.SCARE.getRange().upperEndpoint().floatValue();
            }
        } else {
            if (getAttacker() != null) {
                shouldSpawn = random.nextFloat() < 0.15f;
                annoyance = Annoyance.REVENGE.getRange().upperEndpoint().floatValue();
            } else {
                shouldSpawn = true;
                annoyance = random.nextFloat() * Annoyance.KILL.getRange().upperEndpoint().floatValue();
            }
        }

        if (shouldSpawn) {
            BeeperEntity beeper = BeeperEntity.createFromCreeper(Self());
            beeper.setHealth(1);
            beeper.setCharged(isCharged());
            beeper.setTarget(getAttacker());
            beeper.setAnnoyance(getAttacker() instanceof PlayerEntity
                    ? annoyance
                    : Annoyance.REVENGE.getRange().upperEndpoint().floatValue());
            // Cancel creeper
            discard();
            // Spawn
            BeeperEntity.spawnInWorld(beeper);
        } else {
            super.applyDamage(source, amount);
        }
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        return super.damage(source, amount);
    }

    public boolean isCharged() {
        return dataTracker.get(CHARGED);
    }

    private CreeperEntity Self() {
        return (CreeperEntity)(Object)this;
    }

    // Goals. Can't move to a separate file because of the use of accessors
    private static class CreeperMeleeAttackGoal extends Goal {
        protected final CreeperEntity mob;
        private final double speed;
        private final boolean pauseWhenMobIdle;
        private final EntityNavigation mobNavigation;

        @Nullable
        private Path path;
        private double targetX;
        private double targetY;
        private double targetZ;
        private int updateCountdownTicks;

        private int morphCountdown; // "You, son of a beech..." timer
        private boolean morphTriggered;
        private LivingEntity lastTarget;
        private float annoyance;

        private int spottedTimeout;
        private boolean spotted;

        private int cooldown;
        private long lastUpdateTime;

        public CreeperMeleeAttackGoal(CreeperEntity mob, double speed, boolean pauseWhenMobIdle) {
            this.mob = mob;
            this.speed = speed;
            this.pauseWhenMobIdle = pauseWhenMobIdle;
            this.mobNavigation = mob.getNavigation();
            setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
        }

        @Override
        public boolean shouldRunEveryTick() {
            return true;
        }

        @Override
        public boolean canStart() {
            long l = this.mob.getWorld().getTime();
            if (l - this.lastUpdateTime < 20L) {
                return false;
            }
            this.lastUpdateTime = l;

            LivingEntity targetEntity = mob.getTarget();
            if (targetEntity == null || !targetEntity.isAlive() || !EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR.test(targetEntity)) {
                return false;
            }

            path = mobNavigation.findPathTo(targetEntity, 0);
            if (path != null) {
                return true;
            }

            return mob.isInAttackRange(targetEntity);
        }

        @Override
        public boolean shouldContinue() {
            if (morphTriggered) {
                return true;
            }

            LivingEntity targetEntity = mob.getTarget();
            if (targetEntity == null || !targetEntity.isAlive() || !EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR.test(targetEntity)) {
                return false;
            }

            if (!pauseWhenMobIdle) {
                return !mobNavigation.isIdle();
            }

            return mob.isInWalkTargetRange(targetEntity.getBlockPos());
        }

        @Override
        public void start() {
            mobNavigation.startMovingAlong(path, speed);
            mob.setAttacking(true);
            updateCountdownTicks = 0;
            cooldown = 0;

            lastTarget = null;
            morphCountdown = 0;
            morphTriggered = false;

            annoyance = 0.0f;
            spottedTimeout = 0;
            spotted = true;
        }

        @Override
        public void stop() {
            LivingEntity targetEntity = mob.getTarget();
            if (!EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR.test(targetEntity)) {
                mob.setTarget(null);
            }

            mob.setAttacking(false);
            mobNavigation.stop();
        }

        @Override
        public void tick() {
            if (morphTriggered) {
                handleMorph();
                return;
            }

            LivingEntity targetEntity = mob.getTarget();
            if (targetEntity == null) {
                return;
            }

            // Morph handling
            Vec3d targetPos = targetEntity.getEyePos();
            Vec3d mobPos = mob.getEyePos();
            double dist = targetPos.squaredDistanceTo(mobPos);

            handleMorphSensors(targetPos, mobPos, targetEntity, dist);

            // Vanilla code
            mob.getLookControl().lookAt(targetEntity, 30.0f, 30.0f);
            updateCountdownTicks = Math.max(updateCountdownTicks - 1, 0);
            if ((pauseWhenMobIdle || mob.getVisibilityCache().canSee(targetEntity))
                    && updateCountdownTicks <= 0
                    && (targetX == 0.0 && targetY == 0.0 && targetZ == 0.0
                    || targetEntity.squaredDistanceTo(targetX, targetY, targetZ) >= 1.0
                    || mob.getRandom().nextFloat() < 0.05f)) {
                targetX = targetEntity.getX();
                targetY = targetEntity.getY();
                targetZ = targetEntity.getZ();
                updateCountdownTicks = 4 + mob.getRandom().nextInt(7);
                if (dist > 1024.0) {
                    updateCountdownTicks += 10;
                } else if (dist > 256.0) {
                    updateCountdownTicks += 5;
                }
                if (!mobNavigation.startMovingTo(targetEntity, speed)) {
                    updateCountdownTicks += 15;
                }
                updateCountdownTicks = getTickCount(updateCountdownTicks);
            }
            cooldown = Math.max(cooldown - 1, 0);
            attack(targetEntity);
        }

        private void handleMorph() {
            mob.getLookControl().lookAt(lastTarget, 30.0f, 30.0f);

            if (morphCountdown == 0) {
                mobNavigation.stop();
                mobNavigation.setSpeed(0);
            }

            if (morphCountdown >= 60) {
                // Spawn beeper
                BeeperEntity beeper = BeeperEntity.createFromCreeper(mob);
                beeper.setTarget(lastTarget);
                beeper.setCharged(((CreeperEntityMixin)(Object)mob).isCharged());
                beeper.setAnnoyance(annoyance);
                // Cancel creeper
                mob.discard();
                // Spawn
                BeeperEntity.spawnInWorld(beeper);
            }

            ++morphCountdown;
        }

        private void handleMorphSensors(Vec3d targetPos, Vec3d mobPos, LivingEntity targetEntity, double dist) {
            double dY = targetPos.getY() - mobPos.getY();
            handleSpottedTimeout(targetPos, mobPos, targetEntity);

            if (dY > 4.0f) {
                morphTriggered = true;
                lastTarget = targetEntity;
                annoyance = Annoyance.REVENGE.getRange().upperEndpoint().floatValue();
                return;
            }

            if (!spotted && dist <= 144) {
                morphTriggered = true;
                lastTarget = targetEntity;
                annoyance = mob.getRandom().nextFloat() * Annoyance.KILL.getRange().upperEndpoint().floatValue();
            }
        }

        private void handleSpottedTimeout(Vec3d targetPos, Vec3d mobPos, LivingEntity targetEntity) {
            if (spotted) {
                Vec3d lookVector = targetEntity.getRotationVector().normalize();
                Vec3d mobVector = mobPos.subtract(targetPos).normalize();
                double angleCos = lookVector.dotProduct(mobVector);
                if (angleCos < 0.17364817766f) {
                    ++spottedTimeout;
                } else {
                    spottedTimeout = 0;
                }

                if (spottedTimeout >= 40) {
                    spotted = false;
                    spottedTimeout = 0;
                }
            }
        }

        // Attack methods
        protected void attack(LivingEntity target) {
            if (canAttack(target)) {
                resetCooldown();
                mob.swingHand(Hand.MAIN_HAND);
                mob.tryAttack(target);
            }
        }

        protected void resetCooldown() {
            cooldown = getTickCount(20);
        }

        protected boolean isCooledDown() {
            return cooldown <= 0;
        }

        protected boolean canAttack(LivingEntity target) {
            return isCooledDown() && mob.isInAttackRange(target) && mob.getVisibilityCache().canSee(target);
        }

        protected int getCooldown() {
            return cooldown;
        }

        protected int getMaxCooldown() {
            return getTickCount(20);
        }
    }
}
