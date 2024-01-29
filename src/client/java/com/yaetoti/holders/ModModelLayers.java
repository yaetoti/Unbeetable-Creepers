package com.yaetoti.holders;

import com.yaetoti.MyHomeIsMyCastle;
import com.yaetoti.render.entity.model.BeeperEntityModel;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;

public class ModModelLayers {
    public static final EntityModelLayer BEEPER = new EntityModelLayer(
            new Identifier(MyHomeIsMyCastle.MODID, "beeper"),
            "main");
    public static final EntityModelLayer BEEPER_ARMOR = new EntityModelLayer(
            new Identifier(MyHomeIsMyCastle.MODID, "beeper"),
            "armor");

    public static void register() {
        EntityModelLayerRegistry.registerModelLayer(BEEPER, BeeperEntityModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(BEEPER_ARMOR, BeeperEntityModel::getTexturedModelData);
    }
}
