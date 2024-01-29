package com.yaetoti.render.entity.feature;

import com.yaetoti.MyHomeIsMyCastle;
import com.yaetoti.entity.BeeperEntity;
import com.yaetoti.render.entity.model.BeeperEntityModel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.feature.EyesFeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;


@Environment(value= EnvType.CLIENT)
public class BeeperEyesFeatureRenderer extends EyesFeatureRenderer<BeeperEntity, BeeperEntityModel<BeeperEntity>> {
    private static final RenderLayer SKIN = RenderLayer.getEyes(MyHomeIsMyCastle.IdOf("textures/entity/beeper/beeper_eyes.png"));

    public BeeperEyesFeatureRenderer(FeatureRendererContext<BeeperEntity, BeeperEntityModel<BeeperEntity>> featureRendererContext) {
        super(featureRendererContext);
    }

    @Override
    public RenderLayer getEyesTexture() {
        return SKIN;
    }
}
