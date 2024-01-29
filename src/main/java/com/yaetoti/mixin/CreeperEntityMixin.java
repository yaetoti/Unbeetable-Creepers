package com.yaetoti.mixin;

import com.yaetoti.entity.BeeperEntity;
import com.yaetoti.holders.ModSounds;
import net.minecraft.client.render.entity.feature.SkinOverlayOwner;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.passive.OcelotEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
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

    public boolean isCharged() {
        return dataTracker.get(CHARGED);
    }

    private CreeperEntity Self() {
        return (CreeperEntity)(Object)this;
    }

    @Inject(
            at = @At(value = "HEAD"),
            method = "initGoals()V",
            cancellable = true
    )
    private void initGoals(CallbackInfo info) {
        // TODO soft injection
        goalSelector.add(1, new SwimGoal(Self()));
        goalSelector.add(2, new CreeperIgniteGoal(Self()));
        goalSelector.add(3, new FleeEntityGoal<>(Self(), OcelotEntity.class, 6.0f, 1.0, 1.2));
        goalSelector.add(3, new FleeEntityGoal<>(Self(), CatEntity.class, 6.0f, 1.0, 1.2));
        goalSelector.add(4, new BeeperMeleeAttackGoal(Self(), 1.0, true));
        goalSelector.add(5, new WanderAroundFarGoal(Self(), 0.8));
        goalSelector.add(6, new LookAtEntityGoal(Self(), PlayerEntity.class, 8.0f));
        goalSelector.add(6, new LookAroundGoal(Self()));
        targetSelector.add(1, new ActiveTargetGoal<>(Self(), PlayerEntity.class, true));
        targetSelector.add(2, new RevengeGoal(Self()));
        info.cancel();
    }

    static class BeeperMeleeAttackGoal extends Goal {
        protected final CreeperEntity mob;
        private final double speed;
        private final boolean pauseWhenMobIdle;
        private Path path;
        private double targetX;
        private double targetY;
        private double targetZ;
        private int updateCountdownTicks;

        private final int morphCountdownTime = 60;
        private int morphCountdown; // "You, son of a bitch..." timer
        private boolean morphTriggered;
        private LivingEntity lastTarget;

        private int cooldown;
        private long lastUpdateTime;

        public BeeperMeleeAttackGoal(CreeperEntity mob, double speed, boolean pauseWhenMobIdle) {
            this.mob = mob;
            this.speed = speed;
            this.pauseWhenMobIdle = pauseWhenMobIdle;
            this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
        }

        @Override
        public boolean canStart() {
            long l = this.mob.getWorld().getTime();
            if (l - this.lastUpdateTime < 20L) {
                return false;
            }
            this.lastUpdateTime = l;
            LivingEntity livingEntity = this.mob.getTarget();
            if (livingEntity == null) {
                return false;
            }
            if (!livingEntity.isAlive()) {
                return false;
            }
            this.path = this.mob.getNavigation().findPathTo(livingEntity, 0);
            if (this.path != null) {
                return true;
            }
            return this.mob.isInAttackRange(livingEntity);
        }

        @Override
        public boolean shouldContinue() {
            if (morphTriggered) {
                return true;
            }

            LivingEntity livingEntity = this.mob.getTarget();
            if (livingEntity == null) {
                return false;
            }
            if (!livingEntity.isAlive()) {
                return false;
            }
            if (!this.pauseWhenMobIdle) {
                return !this.mob.getNavigation().isIdle();
            }
            if (!this.mob.isInWalkTargetRange(livingEntity.getBlockPos())) {
                return false;
            }

            return !(livingEntity instanceof PlayerEntity) || !livingEntity.isSpectator() && !((PlayerEntity)livingEntity).isCreative();
        }

        @Override
        public void start() {
            this.mob.getNavigation().startMovingAlong(this.path, this.speed);
            this.mob.setAttacking(true);
            this.updateCountdownTicks = 0;
            this.cooldown = 0;

            morphTriggered = false;
            morphCountdown = 0;
            lastTarget = null;
        }

        @Override
        public void stop() {
            LivingEntity livingEntity = this.mob.getTarget();
            if (!EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR.test(livingEntity)) {
                this.mob.setTarget(null);
            }
            this.mob.setAttacking(false);
            this.mob.getNavigation().stop();
        }

        @Override
        public boolean shouldRunEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            // TODO: Morph to bee when:
            // 1. Not reachable
            // 2. Finished but distance is too large

            if (morphTriggered) {
                if (morphCountdown == 0) {
                    mob.getNavigation().stop();
                    mob.getNavigation().setSpeed(0);
                }

                ++morphCountdown;
                mob.getLookControl().lookAt(lastTarget, 30.0f, 30.0f);
                System.out.println(morphCountdown);
                if (morphCountdown >= morphCountdownTime) {
                    Vec3d mobPos = mob.getEyePos();
                    ServerWorld world = (ServerWorld)mob.getEntityWorld();

                    // Remove creeper
                    mob.discard(); // Cancelled.

                    // Spawn beeper
                    BeeperEntity beeper = new BeeperEntity(world);
                    beeper.refreshPositionAndAngles(mobPos.getX(), mobPos.getY(), mobPos.getZ(), mob.getYaw(), mob.getPitch());
                    beeper.setTarget(lastTarget);
                    beeper.setCharged(((CreeperEntityMixin)(Object)mob).isCharged());
                    for (var effect : mob.getStatusEffects()) {
                        beeper.addStatusEffect(effect);
                    }
                    // TODO: charged
                    world.spawnEntity(beeper);

                    // Spawn effects
                    world.spawnParticles(
                            ParticleTypes.CAMPFIRE_COSY_SMOKE,
                            mobPos.getX(), mobPos.getY(), mobPos.getZ(),
                            24,
                            0.7, 0.7, 0.7,
                            0.05);
                    mob.playSound(ModSounds.WOO, 1.0f, 1.0f);
                }

                return;
            }

            LivingEntity targetEntity = this.mob.getTarget();
            if (targetEntity == null) {
                return;
            }

            double dX = targetEntity.getX() - mob.getX();
            double dY = targetEntity.getY() - mob.getY();
            double dZ = targetEntity.getZ() - mob.getZ();
            double dist = Math.sqrt(dX * dX + dY * dY + dZ * dZ);

            // TODO fix as shouldrun runs before tick. Modify targetselector
            if (!morphTriggered && (dY >= 4.0f || dist >= 12)) {
                morphTriggered = true;
                lastTarget = targetEntity;
            }

            this.mob.getLookControl().lookAt(targetEntity, 30.0f, 30.0f);
            this.updateCountdownTicks = Math.max(this.updateCountdownTicks - 1, 0);
            if ((this.pauseWhenMobIdle || this.mob.getVisibilityCache().canSee(targetEntity)) && this.updateCountdownTicks <= 0 && (this.targetX == 0.0 && this.targetY == 0.0 && this.targetZ == 0.0 || targetEntity.squaredDistanceTo(this.targetX, this.targetY, this.targetZ) >= 1.0 || this.mob.getRandom().nextFloat() < 0.05f)) {
                this.targetX = targetEntity.getX();
                this.targetY = targetEntity.getY();
                this.targetZ = targetEntity.getZ();
                this.updateCountdownTicks = 4 + this.mob.getRandom().nextInt(7);
                double d = this.mob.squaredDistanceTo(targetEntity);
                if (d > 1024.0) {
                    this.updateCountdownTicks += 10;
                } else if (d > 256.0) {
                    this.updateCountdownTicks += 5;
                }
                if (!this.mob.getNavigation().startMovingTo(targetEntity, this.speed)) {
                    this.updateCountdownTicks += 15;
                }
                this.updateCountdownTicks = this.getTickCount(this.updateCountdownTicks);
            }
            this.cooldown = Math.max(this.cooldown - 1, 0);
            this.attack(targetEntity);
        }

        protected void attack(LivingEntity target) {
            if (this.canAttack(target)) {
                this.resetCooldown();
                this.mob.swingHand(Hand.MAIN_HAND);
                this.mob.tryAttack(target);
            }
        }

        protected void resetCooldown() {
            this.cooldown = this.getTickCount(20);
        }

        protected boolean isCooledDown() {
            return this.cooldown <= 0;
        }

        protected boolean canAttack(LivingEntity target) {
            return this.isCooledDown() && this.mob.isInAttackRange(target) && this.mob.getVisibilityCache().canSee(target);
        }

        protected int getCooldown() {
            return this.cooldown;
        }

        protected int getMaxCooldown() {
            return this.getTickCount(20);
        }
    }
}
