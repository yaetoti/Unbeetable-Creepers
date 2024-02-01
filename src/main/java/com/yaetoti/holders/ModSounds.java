package com.yaetoti.holders;

import com.yaetoti.ModEntry;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class ModSounds {
    public static final SoundEvent WOO = register(ModEntry.IdOf("woo"));
    public static final SoundEvent AAA = register(ModEntry.IdOf("aaa"));

    public static void register() {
    }

    private static SoundEvent register(Identifier id) {
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }
}
