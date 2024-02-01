/*
 * Decompiled with CFR 0.2.1 (FabricMC 53fa44c9).
 */
package com.yaetoti.entity;

import com.google.common.collect.Range;
import com.yaetoti.entity.ai.control.BeeperFlightMoveControl;
import com.yaetoti.entity.ai.goals.*;
import com.yaetoti.holders.ModEntities;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.entity.feature.SkinOverlayOwner;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.event.GameEvent;

import java.util.Collection;

public class BeeperEntity extends HostileEntity implements Flutterer, SkinOverlayOwner {
    private static final TrackedData<Boolean> CHARGED = DataTracker.registerData(BeeperEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Float> FUSE_SPEED = DataTracker.registerData(BeeperEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private int ticksInsideWater;
    private float explosionRadius = 2.0f;

    // Fuse
    // TODO: Config
    private static final float FUSE_TIME = 30;
    private float lastFuseTime;
    private float currentFuseTime;

    // Memories (Variables commonly used by goals)
    LivingEntity lastTarget;
    private Vec3d targetPos;
    private Vec3d mobPos;
    private double targetDistance;
    private boolean spotted;
    private float annoyance;

    public BeeperEntity(EntityType<? extends BeeperEntity> entityType, World world) {
        super(entityType, world);

        this.moveControl = new BeeperFlightMoveControl(this, 20, true);
        this.setPathfindingPenalty(PathNodeType.DANGER_FIRE, -1.0f);
        this.setPathfindingPenalty(PathNodeType.WATER, -1.0f);
        this.setPathfindingPenalty(PathNodeType.WATER_BORDER, 16.0f);
        this.setPathfindingPenalty(PathNodeType.COCOA, -1.0f);
        this.setPathfindingPenalty(PathNodeType.FENCE, -1.0f);

        // annoyance = getRandom().nextFloat();
        annoyance = 0.75f;
        System.out.println("Annoyance: " + annoyance);
    }

    public BeeperEntity(World world) {
        this(ModEntities.BEEPER, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 10.0)
                .add(EntityAttributes.GENERIC_FLYING_SPEED, 0.6f)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.1f)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48.0)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 0);
    }

    // Behaviour

    @Override
    protected void initGoals() {
        super.initGoals();

        goalSelector.add(0, new SwimGoal(this));

        goalSelector.add(1, new BeeperRageGoal(this, 12.0, Range.open(10.0, 16.0)));
        goalSelector.add(2, new BeeperFleeGoal(this, 8.0f, 12.0));
        goalSelector.add(3, new BeeperFuseGoal(this));
        goalSelector.add(4, new BeeperFollowGoal(this, 6.0));
        goalSelector.add(5, new BeeperWanderGoal(this, 3.0));

        goalSelector.add(6, new LookAroundGoal(this));
        targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, false));
    }

    @Override
    public void tick() {
        // TODO: Kludge. Use brain system instead
        // Update memories
        updateMemories();

        // Handle fuse
        if (isAlive()) {
            lastFuseTime = currentFuseTime;
            float fuseSpeed = getFuseSpeed();
            if (fuseSpeed > 0 && this.currentFuseTime == 0.0f) {
                this.playSound(SoundEvents.ENTITY_CREEPER_PRIMED, 1.0f, 0.5f);
                this.emitGameEvent(GameEvent.PRIME_FUSE);
            }

            this.currentFuseTime += fuseSpeed;
            if (this.currentFuseTime < 0) {
                this.currentFuseTime = 0;
            }

            if (this.currentFuseTime >= FUSE_TIME) {
                this.currentFuseTime = FUSE_TIME;
                this.explode();
            }
        }

        super.tick();
    }

    private void updateMemories() {
        lastTarget = getTarget();
        if (lastTarget == null || !lastTarget.isAlive() || !EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR.test(lastTarget)) {
            return;
        }

        targetPos = lastTarget.getEyePos();
        mobPos = getEyePos();
        targetDistance = getEyePos().distanceTo(targetPos);

        Vec3d lookVector = lastTarget.getRotationVector().normalize();
        Vec3d mobVector = mobPos.subtract(targetPos).normalize();
        double angleCos = lookVector.dotProduct(mobVector);
        spotted = angleCos >= 0.5;
    }

    @Override
    protected void mobTick() {
        this.ticksInsideWater = this.isInsideWaterOrBubbleColumn() ? ++this.ticksInsideWater : 0;
        if (this.ticksInsideWater > 20) {
            this.damage(this.getDamageSources().drown(), 1.0f);
        }
    }

    public void setTargetingEnabled(boolean enabled) {
        // TODO: Kludge. Use brain's memories for detailed behaviour control
        targetSelector.setControlEnabled(Goal.Control.TARGET, enabled);
    }

    // AI misc

    @Override
    public float getPathfindingFavor(BlockPos pos, WorldView world) {
        if (world.getBlockState(pos).isAir()) {
            return 10.0f;
        }
        return 0.0f;
    }

    @Override
    protected EntityNavigation createNavigation(World world) {
        BirdNavigation birdNavigation = new BirdNavigation(this, world) {

            @Override
            public boolean isValidPosition(BlockPos pos) {
                return !this.world.getBlockState(pos.down()).isAir();
            }

            @Override
            public void tick() {
                super.tick();
            }
        };

        birdNavigation.setCanPathThroughDoors(true);
        birdNavigation.setCanSwim(false);
        birdNavigation.setCanEnterOpenDoors(true);
        return birdNavigation;
    }

    @Override
    public void onStruckByLightning(ServerWorld world, LightningEntity lightning) {
        super.onStruckByLightning(world, lightning);
        setCharged(true);
    }

    @Override
    protected void fall(double heightDifference, boolean onGround, BlockState state, BlockPos landedPosition) {
    }

    @Override
    public boolean isFlappingWings() {
        return this.isInAir();
    }

    @Override
    protected void swimUpward(TagKey<Fluid> fluid) {
        this.setVelocity(this.getVelocity().add(0.0, 0.01, 0.0));
    }

    @Override
    public boolean isInAir() {
        return !this.isOnGround();
    }

    @Override
    protected float getOffGroundSpeed() {
        // If you not override this ugly son of a dirty rat then air movement speed will be set to 0.2. Hours wasted: 3
        // See LivingEntity::getOffGroundSpeed
        return getMovementSpeed() * 0.01f;
    }

    @Override
    protected void applyDamage(DamageSource source, float amount) {
        super.applyDamage(source, amount);
        if (source.getAttacker() == getTarget()) {
            if (getHealth() - amount <= 0) {
                setAnnoyance(1.0f);
            } else {
                increaseAnnoyance(0.1f);
            }
        }
    }

    // Data

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        dataTracker.startTracking(CHARGED, false);
        dataTracker.startTracking(FUSE_SPEED, 0.0f);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (dataTracker.get(CHARGED)) {
            nbt.putBoolean("powered", true);
        }
        nbt.putByte("ExplosionRadius", (byte)this.explosionRadius);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.dataTracker.set(CHARGED, nbt.getBoolean("powered"));
        if (nbt.contains("ExplosionRadius", NbtElement.NUMBER_TYPE)) {
            this.explosionRadius = nbt.getByte("ExplosionRadius");
        }
    }

    // Charged

    public void setCharged(boolean charged) {
        dataTracker.set(CHARGED, charged);
    }

    public boolean isCharged() {
        return dataTracker.get(CHARGED);
    }

    // Fuse

    public float getClientFuseTime(float timeDelta) {
        return MathHelper.lerp(timeDelta, lastFuseTime, currentFuseTime) / (FUSE_TIME - 2);
    }

    public float getFuseSpeed() {
        return dataTracker.get(FUSE_SPEED);
    }

    public void setFuseSpeed(float fuseSpeed) {
        dataTracker.set(FUSE_SPEED, fuseSpeed);
    }

    // Memories
    public LivingEntity getLastTarget() {
        return lastTarget;
    }

    public Vec3d getTargetPos() {
        return targetPos;
    }

    public Vec3d getMobPos() {
        return mobPos;
    }

    public double getTargetDistance() {
        return targetDistance;
    }

    public boolean isSpotted() {
        return spotted;
    }

    public float getAnnoyance() {
        return annoyance;
    }

    public void setAnnoyance(float annoyance) {
        this.annoyance = annoyance;
    }

    public void increaseAnnoyance(float annoyance) {
        this.annoyance += annoyance;
        if (this.annoyance > 1.0f) {
            this.annoyance = 1.0f;
        }
        System.out.println("+ANNOYING: " + this.annoyance);
    }

    public void decreaseAnnoyance(float annoyance) {
        this.annoyance -= annoyance;
        if (this.annoyance < 0.0f) {
            this.annoyance = 0.0f;
        }
        System.out.println("-ANNOYING: " + this.annoyance);
    }

    // Behaviour

    @Override
    public boolean shouldRenderOverlay() {
        return isCharged();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_CREEPER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_CREEPER_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 0.4f;
    }

    @Override
    public EntityGroup getGroup() {
        return EntityGroup.ARTHROPOD;
    }

    @Override
    protected float getActiveEyeHeight(EntityPose pose, EntityDimensions dimensions) {
        return dimensions.height * 0.5f;
    }

    public void explode() {
        if (!this.getWorld().isClient) {
            // TODO: Config
            float f = this.isCharged() ? 2.0f : 1.0f;
            this.dead = true;
            this.discard();
            this.getWorld().createExplosion(this, this.getX(), this.getY(), this.getZ(), this.explosionRadius * f, World.ExplosionSourceType.MOB);
            this.spawnEffectsCloud();
        }
    }

    private void spawnEffectsCloud() {
        Collection<StatusEffectInstance> collection = this.getStatusEffects();
        if (!collection.isEmpty()) {
            AreaEffectCloudEntity areaEffectCloudEntity = new AreaEffectCloudEntity(this.getWorld(), this.getX(), this.getY(), this.getZ());
            // TODO: Config
            float f = this.isCharged() ? 1.5f : 1.0f;
            areaEffectCloudEntity.setRadius(2.5f * f);
            areaEffectCloudEntity.setRadiusOnUse(-0.2f);
            areaEffectCloudEntity.setWaitTime(10);
            areaEffectCloudEntity.setDuration(areaEffectCloudEntity.getDuration() / 2);
            areaEffectCloudEntity.setRadiusGrowth(-areaEffectCloudEntity.getRadius() / (float)areaEffectCloudEntity.getDuration());
            for (StatusEffectInstance statusEffectInstance : collection) {
                areaEffectCloudEntity.addEffect(new StatusEffectInstance(statusEffectInstance));
            }
            this.getWorld().spawnEntity(areaEffectCloudEntity);
        }
    }
}

