package dev.gimme.sharedlife.infrastructure;

import com.mojang.serialization.DataResult;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jetbrains.annotations.Nullable;

/**
 * Test-only handles to {@link SharedLifePersistence}. Lives in the gametest source set's
 * {@code infrastructure} package so it can reach the package-private storage type and save/restore
 * hooks.
 */
public final class PersistenceTestSupport {

    /**
     * Looked up by id alone ({@code SavedDataType} equality); the dummy constructor and codec never
     * resurrect state, as they are only consulted on a cache miss — which is itself the failure this
     * lookup exists to catch.
     */
    private static final SavedDataType<SharedLifePersistence> LOOKUP_TYPE = new SavedDataType<>(
            SharedLifePersistence.ID,
            () -> null,
            CompoundTag.CODEC.comapFlatMap(
                    tag -> DataResult.<SharedLifePersistence>error(() -> "the shared life persistence is not attached"),
                    persistence -> new CompoundTag()),
            SharedLifePersistence.DATA_FIX_TYPE);

    private PersistenceTestSupport() {
    }

    /** The persistence attached to the server's world storage at startup, or null if that wiring is gone. */
    public static @Nullable SharedLifePersistence find(MinecraftServer server) {
        return server.overworld().getDataStorage().get(LOOKUP_TYPE);
    }

    /** Saves the live shared state the way a world save does. */
    public static CompoundTag save(SharedLifePersistence persistence) {
        return persistence.saveToTag();
    }

    /** Restores a saved state the way a server start does. */
    public static void restore(SharedLifePersistence persistence, CompoundTag tag) {
        persistence.restore(tag);
    }
}
