package com.yaetoti.entity.ai.control;

import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.MathHelper;

public class BeeperFlightMoveControl extends MoveControl {
    private final int maxPitchChange;
    private final boolean noGravity;

    public BeeperFlightMoveControl(MobEntity entity, int maxPitchChange, boolean noGravity) {
        super(entity);
        this.maxPitchChange = maxPitchChange;
        this.noGravity = noGravity;
    }

    @Override
    public void tick() {
        if (this.state == MoveControl.State.MOVE_TO) {
            this.state = MoveControl.State.WAIT;
            this.entity.setNoGravity(true);
            double dX = this.targetX - this.entity.getX();
            double dY = this.targetY - this.entity.getY();
            double dZ = this.targetZ - this.entity.getZ();
            double dist = dX * dX + dY * dY + dZ * dZ;
            if (dist < 2.500000277905201E-7) {
                this.entity.setUpwardSpeed(0.0f);
                this.entity.setForwardSpeed(0.0f);
                return;
            }

            float h = (float)(MathHelper.atan2(dZ, dX) * 57.2957763671875) - 90.0f;
            this.entity.setYaw(this.wrapDegrees(this.entity.getYaw(), h, 90.0f));

            float speed = this.entity.isOnGround()
                    ? (float)(this.speed * this.entity.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED))
                    : (float)(this.speed * this.entity.getAttributeValue(EntityAttributes.GENERIC_FLYING_SPEED));
            this.entity.setMovementSpeed(speed);

            double distXZ = Math.sqrt(dX * dX + dZ * dZ);
            if (Math.abs(dY) > (double)1.0E-5f || Math.abs(distXZ) > (double)1.0E-5f) {
                float k = (float)(-(MathHelper.atan2(dY, distXZ) * 57.2957763671875));
                this.entity.setPitch(this.wrapDegrees(this.entity.getPitch(), k, this.maxPitchChange));
                // Original
                // float upwardSpeed = dY > 0.0 ? speed : -speed;
                // More straight trajectory
                float upwardSpeed;
                if (Math.abs(distXZ) > (double)1.0E-5f) {
                    upwardSpeed = (float)(dY / (distXZ / speed));
                } else {
                    upwardSpeed = (float)Math.abs(dY) <= speed ? (float)dY : speed;
                }
                this.entity.setUpwardSpeed(upwardSpeed);
            }
        } else {
            if (!this.noGravity) {
                this.entity.setNoGravity(false);
            }
            this.entity.setUpwardSpeed(0.0f);
            this.entity.setForwardSpeed(0.0f);
        }
    }
}
