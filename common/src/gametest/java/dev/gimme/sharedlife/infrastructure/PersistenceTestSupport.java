package dev.gimme.sharedlife.infrastructure;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

/**
 * Test-only handles to {@link SharedLifePersistence}. Lives in the gametest source set's
 * {@code infrastructure} package so it can reach the package-private storage name and restore hook.
 */
public final class PersistenceTestSupport {

    private PersistenceTestSupport() {
    }

    /**
     * The persistence attached to the server's world storage at startup, or null if that wiring is gone.
     * The dummy factory never resurrects state: it is only consulted on a cache miss, which is itself the
     * failure this lookup exists to catch.
     */
    public static @Nullable SharedLifePersistence find(MinecraftServer server) {
        return server.overworld().getDataStorage().get(
                new SavedData.Factory<SharedLifePersistence>(
                        () -> null, (tag, registries) -> null, SharedLifePersistence.DATA_FIX_TYPE),
                SharedLifePersistence.DATA_NAME);
    }

    /** Saves the live shared state the way a world save does. */
    public static CompoundTag save(SharedLifePersistence persistence, MinecraftServer server) {
        return persistence.save(new CompoundTag(), server.registryAccess());
    }

    /** Restores a saved state the way a server start does. */
    public static void restore(SharedLifePersistence persistence, CompoundTag tag) {
        persistence.restore(tag);
    }
}
