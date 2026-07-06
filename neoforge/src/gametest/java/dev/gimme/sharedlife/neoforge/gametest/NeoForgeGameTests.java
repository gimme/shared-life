package dev.gimme.sharedlife.neoforge.gametest;

import dev.gimme.sharedlife.domain.util.Constants;
import dev.gimme.sharedlife.gametest.SharedLifeGameTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * NeoForge test wiring. {@link GameTestHolder} auto-registers every {@link GameTest} method in this
 * class under the mod's namespace (enabled via the {@code neoforge.enabledGameTestNamespaces} run
 * property), so {@code main} never references this dev-only source set. One delegate per shared test.
 *
 * <p>{@link PrefixGameTestTemplate}(false) keeps template names unprefixed, so every test resolves to
 * the single {@code sharedlife:empty} structure shipped under this source set's resources. Methods are
 * invoked on a fresh instance, so this class needs a public no-arg constructor (the implicit default).
 */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NeoForgeGameTests {

    private static final String EMPTY = "empty";

    @GameTest(template = EMPTY)
    public void damageSyncsAcrossRealPlayers(GameTestHelper helper) {
        SharedLifeGameTests.damageSyncsAcrossRealPlayers(helper);
    }

    @GameTest(template = EMPTY)
    public void armorReducesSharedDamage(GameTestHelper helper) {
        SharedLifeGameTests.armorReducesSharedDamage(helper);
    }

    @GameTest(template = EMPTY)
    public void healingSyncsAcrossRealPlayers(GameTestHelper helper) {
        SharedLifeGameTests.healingSyncsAcrossRealPlayers(helper);
    }

    @GameTest(template = EMPTY)
    public void deathCascadesToAllPlayers(GameTestHelper helper) {
        SharedLifeGameTests.deathCascadesToAllPlayers(helper);
    }

    @GameTest(template = EMPTY)
    public void totemRevivesSharedLife(GameTestHelper helper) {
        SharedLifeGameTests.totemRevivesSharedLife(helper);
    }

    @GameTest(template = EMPTY)
    public void hungerSyncsAcrossRealPlayers(GameTestHelper helper) {
        SharedLifeGameTests.hungerSyncsAcrossRealPlayers(helper);
    }

    @GameTest(template = EMPTY)
    public void starvationHurtsAllPlayers(GameTestHelper helper) {
        SharedLifeGameTests.starvationHurtsAllPlayers(helper);
    }

    @GameTest(template = EMPTY)
    public void combinedRegenHealsWhenAllFed(GameTestHelper helper) {
        SharedLifeGameTests.combinedRegenHealsWhenAllFed(helper);
    }

    @GameTest(template = EMPTY)
    public void combinedRegenBlockedWhileAnyoneHungry(GameTestHelper helper) {
        SharedLifeGameTests.combinedRegenBlockedWhileAnyoneHungry(helper);
    }

    @GameTest(template = EMPTY)
    public void combinedRegenFastOnlyWhenAllSaturated(GameTestHelper helper) {
        SharedLifeGameTests.combinedRegenFastOnlyWhenAllSaturated(helper);
    }

    @GameTest(template = EMPTY)
    public void individualRegenSuppressedWhenCombined(GameTestHelper helper) {
        SharedLifeGameTests.individualRegenSuppressedWhenCombined(helper);
    }

    @GameTest(template = EMPTY)
    public void experienceSyncsAcrossRealPlayers(GameTestHelper helper) {
        SharedLifeGameTests.experienceSyncsAcrossRealPlayers(helper);
    }

    // The one real-tick test: isolated in its own batch so it never runs concurrently with a test
    // that re-seeds the global pool while its players sit in the player list across a tick boundary.
    // Batches run strictly one after another, so nothing else executes during its awaited ticks.
    @GameTest(template = EMPTY, batch = "serverTickHook")
    public void serverTickHookIsWired(GameTestHelper helper) {
        SharedLifeGameTests.serverTickHookIsWired(helper);
    }

    @GameTest(template = EMPTY)
    public void healthNotSharedWhenDisabled(GameTestHelper helper) {
        SharedLifeGameTests.healthNotSharedWhenDisabled(helper);
    }

    @GameTest(template = EMPTY)
    public void hungerNotSharedWhenDisabled(GameTestHelper helper) {
        SharedLifeGameTests.hungerNotSharedWhenDisabled(helper);
    }

    @GameTest(template = EMPTY)
    public void experienceNotSharedWhenDisabled(GameTestHelper helper) {
        SharedLifeGameTests.experienceNotSharedWhenDisabled(helper);
    }

    @GameTest(template = EMPTY)
    public void etherealPlayersExcluded(GameTestHelper helper) {
        SharedLifeGameTests.etherealPlayersExcluded(helper);
    }

    @GameTest(template = EMPTY)
    public void deathReseedsFromNextJoiner(GameTestHelper helper) {
        SharedLifeGameTests.deathReseedsFromNextJoiner(helper);
    }
}
