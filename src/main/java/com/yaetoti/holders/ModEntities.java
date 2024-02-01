package com.yaetoti.holders;

import com.yaetoti.ModEntry;
import com.yaetoti.entity.BeeperEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class ModEntities {
    public static final EntityType<BeeperEntity> BEEPER = Registry.register(
            Registries.ENTITY_TYPE,
            ModEntry.IdOf("beeper"),
            FabricEntityTypeBuilder.<BeeperEntity>create(SpawnGroup.MONSTER, BeeperEntity::new)
                    .dimensions(EntityDimensions.fixed(0.7f, 0.6f))
                    .build());

    public static void register() {
        FabricDefaultAttributeRegistry.register(ModEntities.BEEPER, BeeperEntity.createAttributes());
    }
}
