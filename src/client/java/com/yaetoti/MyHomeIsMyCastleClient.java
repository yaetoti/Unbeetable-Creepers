package com.yaetoti;

import com.yaetoti.holders.ModModelLayers;
import com.yaetoti.holders.ModRenderers;
import net.fabricmc.api.ClientModInitializer;

public class MyHomeIsMyCastleClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModModelLayers.register();
		ModRenderers.register();
	}
}