package dev.gimme.sharedlife.neoforge.gametest;

import dev.gimme.sharedlife.domain.util.Constants;
import dev.gimme.sharedlife.gametest.SharedLifeGameTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * NeoForge test wiring: {@link GameTestHolder} auto-registers every {@link GameTest} method under the
 * mod's namespace, one delegate per shared test. {@link PrefixGameTestTemplate}(false) keeps template
 * names unprefixed so they resolve to the {@code sharedlife:empty} structure in this source set.
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

    // The one real-tick test: its own batch, so nothing else runs while its players await a real tick.
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

    @GameTest(template = EMPTY)
    public void savedStateRestoresAfterReload(GameTestHelper helper) {
        SharedLifeGameTests.savedStateRestoresAfterReload(helper);
    }
}
