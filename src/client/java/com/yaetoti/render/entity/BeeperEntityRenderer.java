/*
 * Decompiled with CFR 0.2.1 (FabricMC 53fa44c9).
 */
package com.yaetoti.render.entity;

import com.yaetoti.ModEntry;
import com.yaetoti.entity.BeeperEntity;
import com.yaetoti.render.entity.feature.BeeperChargeFeatureRenderer;
import com.yaetoti.render.entity.feature.BeeperEyesFeatureRenderer;
import com.yaetoti.render.entity.model.BeeperEntityModel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

@Environment(value=EnvType.CLIENT)
public class BeeperEntityRenderer extends MobEntityRenderer<BeeperEntity, BeeperEntityModel<BeeperEntity>> {
    private static final Identifier TEXTURE = new Identifier(ModEntry.MODID, "textures/entity/beeper/beeper.png");

    public BeeperEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new BeeperEntityModel<>(context.getPart(EntityModelLayers.BEE)), 0.4f);
        this.addFeature(new BeeperChargeFeatureRenderer(this, context.getModelLoader()));
        this.addFeature(new BeeperEyesFeatureRenderer(this));
    }

    @Override
    public Identifier getTexture(BeeperEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(BeeperEntity entity, MatrixStack matrices, float amount) {
        float g = entity.getClientFuseTime(amount);
        float h = 1.0F + MathHelper.sin(g * 100.0F) * g * 0.01F;
        g = MathHelper.clamp(g, 0.0F, 1.0F);
        g *= g;
        g *= g;
        float i = (1.0F + g * 0.4F) * h;
        float j = (1.0F + g * 0.1F) / h;
        matrices.scale(i, j, i);
    }

    @Override
    protected float getAnimationCounter(BeeperEntity entity, float tickDelta) {
        float fuseTime = entity.getClientFuseTime(tickDelta);
        return (int)(fuseTime * 10.0F) % 2 == 0 ? 0.0F : MathHelper.clamp(fuseTime, 0.5F, 1.0F);
    }
}

