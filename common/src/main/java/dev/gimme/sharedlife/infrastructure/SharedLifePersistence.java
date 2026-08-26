package dev.gimme.sharedlife.infrastructure;

import dev.gimme.sharedlife.domain.SharedLife;
import dev.gimme.sharedlife.domain.util.Constants;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import org.jetbrains.annotations.NotNull;

/**
 * Persists the {@link SharedLife} state as part of the world save
 * ({@code data/sharedlife/shared_life.dat} in the overworld), so a server restart resumes the shared
 * life where it left off instead of re-seeding it from the first player to join.
 */
public class SharedLifePersistence extends SavedData {

    static final Identifier ID = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "shared_life");

    /**
     * No shared-life datafixers exist; vanilla just requires some type here, and this one carries no
     * structural fixes that could misapply to our data.
     */
    static final DataFixTypes DATA_FIX_TYPE = DataFixTypes.SAVED_DATA_RANDOM_SEQUENCES;

    private final SharedLife sharedLife;
    private final HolderLookup.Provider registries;

    SharedLifePersistence(@NotNull SharedLife sharedLife, @NotNull HolderLookup.Provider registries) {
        this.sharedLife = sharedLife;
        this.registries = registries;
    }

    /**
     * Hooks the given shared life into the server's world storage: restores the previously saved state,
     * if any, and registers the live state to be written out with every world save.
     */
    public static void attach(@NotNull SharedLife sharedLife, @NotNull MinecraftServer server) {
        server.overworld().getDataStorage().computeIfAbsent(type(sharedLife, server.registryAccess()));
    }

    static SavedDataType<SharedLifePersistence> type(SharedLife sharedLife, HolderLookup.Provider registries) {
        return new SavedDataType<>(
                ID,
                () -> new SharedLifePersistence(sharedLife, registries),
                CompoundTag.CODEC.xmap(
                        tag -> {
                            var persistence = new SharedLifePersistence(sharedLife, registries);
                            persistence.restore(tag);
                            return persistence;
                        },
                        SharedLifePersistence::saveToTag),
                DATA_FIX_TYPE);
    }

    /** Writes the live shared state to a tag, the way a world save does. */
    CompoundTag saveToTag() {
        var output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        sharedLife.save(output);
        return output.buildResult();
    }

    /** Restores a previously saved state, the way vanilla does when loading the world. */
    void restore(CompoundTag tag) {
        sharedLife.load(TagValueInput.create(ProblemReporter.DISCARDING, registries, tag));
    }

    /** The live state changes every tick, so it is written out with every world save. */
    @Override
    public boolean isDirty() {
        return true;
    }
}
