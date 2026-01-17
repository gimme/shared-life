package dev.gimme.sharedlife.domain.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class ExtractingValueOutput implements ValueOutput {

    private float exhaustion;

    public float getExhaustion() {
        return exhaustion;
    }

    @Override
    public <T> void store(String key, Codec<T> codec, T value) {}

    @Override
    public <T> void storeNullable(String key, Codec<T> codec, @Nullable T value) {}

    @Override
    public <T> void store(MapCodec<T> codec, T value) {}

    @Override
    public void putBoolean(String key, boolean value) {}

    @Override
    public void putByte(String key, byte value) {}

    @Override
    public void putShort(String key, short value) {}

    @Override
    public void putInt(String key, int value) {}

    @Override
    public void putLong(String key, long value) {}

    @Override
    public void putFloat(String key, float value) {
        if (key.equals("foodExhaustionLevel")) {
            this.exhaustion = value;
        }
    }

    @Override
    public void putDouble(String key, double value) {}

    @Override
    public void putString(String key, String value) {}

    @Override
    public void putIntArray(String key, int[] value) {}

    @Override
    public ValueOutput child(String key) {
        return null;
    }

    @Override
    public ValueOutputList childrenList(String key) {
        return null;
    }

    @Override
    public <T> TypedOutputList<T> list(String key, Codec<T> elementCodec) {
        return null;
    }

    @Override
    public void discard(String key) {}

    @Override
    public boolean isEmpty() {
        return false;
    }
}
