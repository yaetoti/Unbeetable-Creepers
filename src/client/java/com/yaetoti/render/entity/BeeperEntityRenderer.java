/*
 * Decompiled with CFR 0.2.1 (FabricMC 53fa44c9).
 */
package com.yaetoti.render.entity;

import com.yaetoti.MyHomeIsMyCastle;
import com.yaetoti.entity.BeeperEntity;
import com.yaetoti.render.entity.feature.BeeperChargeFeatureRenderer;
import com.yaetoti.render.entity.feature.BeeperEyesFeatureRenderer;
import com.yaetoti.render.entity.model.BeeperEntityModel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.util.Identifier;

@Environment(value=EnvType.CLIENT)
public class BeeperEntityRenderer extends MobEntityRenderer<BeeperEntity, BeeperEntityModel<BeeperEntity>> {
    private static final Identifier TEXTURE = new Identifier(MyHomeIsMyCastle.MODID, "textures/entity/beeper/beeper.png");

    public BeeperEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new BeeperEntityModel<>(context.getPart(EntityModelLayers.BEE)), 0.4f);
        this.addFeature(new BeeperChargeFeatureRenderer(this, context.getModelLoader()));
        this.addFeature(new BeeperEyesFeatureRenderer(this));
    }

    @Override
    public Identifier getTexture(BeeperEntity entity) {
        return TEXTURE;
    }
}

