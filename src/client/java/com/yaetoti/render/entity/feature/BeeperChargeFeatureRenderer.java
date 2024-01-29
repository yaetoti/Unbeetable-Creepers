package com.yaetoti.render.entity.feature;

import com.yaetoti.entity.BeeperEntity;
import com.yaetoti.holders.ModModelLayers;
import com.yaetoti.render.entity.model.BeeperEntityModel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.feature.EnergySwirlOverlayFeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelLoader;
import net.minecraft.util.Identifier;

@Environment(value= EnvType.CLIENT)
public class BeeperChargeFeatureRenderer extends EnergySwirlOverlayFeatureRenderer<BeeperEntity, BeeperEntityModel<BeeperEntity>> {
    private static final Identifier SKIN = new Identifier("textures/entity/creeper/creeper_armor.png");
    private final BeeperEntityModel<BeeperEntity> model;

    public BeeperChargeFeatureRenderer(FeatureRendererContext<BeeperEntity, BeeperEntityModel<BeeperEntity>> context, EntityModelLoader loader) {
        super(context);
        this.model = new BeeperEntityModel<>(loader.getModelPart(ModModelLayers.BEEPER_ARMOR));
    }

    @Override
    protected float getEnergySwirlX(float partialAge) {
        return partialAge * 0.01f;
    }

    @Override
    protected Identifier getEnergySwirlTexture() {
        return SKIN;
    }

    @Override
    protected EntityModel<BeeperEntity> getEnergySwirlModel() {
        return this.model;
    }
}
