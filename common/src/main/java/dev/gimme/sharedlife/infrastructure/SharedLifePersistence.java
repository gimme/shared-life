package dev.gimme.sharedlife.infrastructure;

import dev.gimme.sharedlife.domain.SharedLife;
import dev.gimme.sharedlife.domain.util.Constants;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

/**
 * Persists the {@link SharedLife} state as part of the world save ({@code data/sharedlife.dat} in the
 * overworld), so a server restart resumes the shared life where it left off instead of re-seeding it
 * from the first player to join.
 */
public class SharedLifePersistence extends SavedData {

    static final String DATA_NAME = Constants.MOD_ID;

    /**
     * No shared-life datafixers exist; vanilla just requires some type here, and this one carries no
     * structural fixes that could misapply to our data.
     */
    static final DataFixTypes DATA_FIX_TYPE = DataFixTypes.SAVED_DATA_RANDOM_SEQUENCES;

    private final SharedLife sharedLife;

    SharedLifePersistence(@NotNull SharedLife sharedLife) {
        this.sharedLife = sharedLife;
    }

    /**
     * Hooks the given shared life into the server's world storage: restores the previously saved state,
     * if any, and registers the live state to be written out with every world save.
     */
    public static void attach(@NotNull SharedLife sharedLife, @NotNull MinecraftServer server) {
        var factory = new SavedData.Factory<>(
                () -> new SharedLifePersistence(sharedLife),
                (tag, registries) -> {
                    sharedLife.load(tag);
                    return new SharedLifePersistence(sharedLife);
                },
                DATA_FIX_TYPE);
        server.overworld().getDataStorage().computeIfAbsent(factory, DATA_NAME);
    }

    /** Restores a previously saved state, the way vanilla does when loading the world. */
    void restore(CompoundTag tag) {
        sharedLife.load(tag);
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        return sharedLife.save(tag);
    }

    /** The live state changes every tick, so it is written out with every world save. */
    @Override
    public boolean isDirty() {
        return true;
    }
}
