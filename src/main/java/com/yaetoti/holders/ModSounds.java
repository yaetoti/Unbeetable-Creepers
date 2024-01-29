package com.yaetoti.holders;

import com.yaetoti.MyHomeIsMyCastle;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class ModSounds {
    public static final SoundEvent WOO = register(MyHomeIsMyCastle.IdOf("woo"));
    public static final SoundEvent AAA = register(MyHomeIsMyCastle.IdOf("aaa"));

    public static void register() {
    }

    private static SoundEvent register(Identifier id) {
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }
}
