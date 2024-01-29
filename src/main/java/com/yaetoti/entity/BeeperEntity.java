/*
 * Decompiled with CFR 0.2.1 (FabricMC 53fa44c9).
 */
package com.yaetoti.entity;

import com.yaetoti.entity.ai.control.BeeperFlightMoveControl;
import com.yaetoti.entity.ai.goals.BeeperWanderAroundGoal;
import com.yaetoti.entity.ai.goals.RageAttackGoal;
import com.yaetoti.holders.ModEntities;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.entity.feature.SkinOverlayOwner;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
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
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

import java.util.Collection;

public class BeeperEntity extends HostileEntity implements Flutterer, SkinOverlayOwner {
    private static final TrackedData<Boolean> CHARGED = DataTracker.registerData(CreeperEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private int ticksInsideWater;
    private float explosionRadius = 2.0f;

    public BeeperEntity(EntityType<? extends BeeperEntity> entityType, World world) {
        super(entityType, world);

        this.moveControl = new BeeperFlightMoveControl(this, 20, true);
        this.setPathfindingPenalty(PathNodeType.DANGER_FIRE, -1.0f);
        this.setPathfindingPenalty(PathNodeType.WATER, -1.0f);
        this.setPathfindingPenalty(PathNodeType.WATER_BORDER, 16.0f);
        this.setPathfindingPenalty(PathNodeType.COCOA, -1.0f);
        this.setPathfindingPenalty(PathNodeType.FENCE, -1.0f);
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

    // Goals

    @Override
    protected void initGoals() {
        super.initGoals();

        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(1, new RageAttackGoal(this, 12.0, 16.0));
        goalSelector.add(2, new MeleeAttackGoal(this, 6.0, false));
        goalSelector.add(3, new BeeperWanderAroundGoal(this, 3.0));
        goalSelector.add(4, new LookAroundGoal(this));
        targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, false));
    }

    public void setTargetingEnabled(boolean enabled) {
        targetSelector.setControlEnabled(Goal.Control.TARGET, enabled);
    }

    // Data

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        dataTracker.startTracking(CHARGED, false);
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

    public void setCharged(boolean charged) {
        dataTracker.set(CHARGED, charged);
    }

    public boolean isCharged() {
        return dataTracker.get(CHARGED);
    }

    // Behaviour

    @Override
    public void onStruckByLightning(ServerWorld world, LightningEntity lightning) {
        super.onStruckByLightning(world, lightning);
        setCharged(true);
    }

    @Override
    public boolean shouldRenderOverlay() {
        return isCharged();
    }

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
    protected float getActiveEyeHeight(EntityPose pose, EntityDimensions dimensions) {
        return dimensions.height * 0.5f;
    }

    @Override
    protected void fall(double heightDifference, boolean onGround, BlockState state, BlockPos landedPosition) {
    }

    @Override
    public boolean isFlappingWings() {
        return this.isInAir();
    }

    @Override
    public EntityGroup getGroup() {
        return EntityGroup.ARTHROPOD;
    }

    @Override
    protected void mobTick() {
        this.ticksInsideWater = this.isInsideWaterOrBubbleColumn() ? ++this.ticksInsideWater : 0;
        if (this.ticksInsideWater > 20) {
            this.damage(this.getDamageSources().drown(), 1.0f);
        }
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

    public void explode() {
        if (!this.getWorld().isClient) {
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
            var areaEffectCloudEntity = new AreaEffectCloudEntity(this.getWorld(), this.getX(), this.getY(), this.getZ());
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

