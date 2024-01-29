package com.yaetoti;

import com.yaetoti.holders.ModEntities;
import com.yaetoti.holders.ModSounds;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyHomeIsMyCastle implements ModInitializer {
	public static final String MODID = "my-home-is-my-castle";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

	@Override
	public void onInitialize() {
		ModEntities.register();
		ModSounds.register();
	}

	public static Identifier IdOf(String path) {
		return Identifier.of(MODID, path);
	}
}