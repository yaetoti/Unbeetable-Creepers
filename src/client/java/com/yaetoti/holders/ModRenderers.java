package com.yaetoti.holders;

import com.yaetoti.render.entity.BeeperEntityRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class ModRenderers {
    public static void register() {
        EntityRendererRegistry.register(ModEntities.BEEPER, BeeperEntityRenderer::new);
    }
}
