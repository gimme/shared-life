package dev.gimme.sharedlife.fabric.gametest;

import dev.gimme.sharedlife.gametest.SharedLifeGameTests;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric test wiring, scanned via the {@code fabric-gametest} entrypoint: one {@link GameTest} delegate
 * per shared test, run on the API's built-in {@link FabricGameTest#EMPTY_STRUCTURE}. Instantiated
 * reflectively, so the implicit public no-arg constructor must stay.
 */
public final class FabricGameTests implements FabricGameTest {

    @GameTest(template = EMPTY_STRUCTURE)
    public void damageSyncsAcrossRealPlayers(GameTestHelper helper) {
        SharedLifeGameTests.damageSyncsAcrossRealPlayers(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void armorReducesSharedDamage(GameTestHelper helper) {
        SharedLifeGameTests.armorReducesSharedDamage(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void absorptionAbsorbsSharedDamage(GameTestHelper helper) {
        SharedLifeGameTests.absorptionAbsorbsSharedDamage(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void healingSyncsAcrossRealPlayers(GameTestHelper helper) {
        SharedLifeGameTests.healingSyncsAcrossRealPlayers(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void healCannotReviveDeadPool(GameTestHelper helper) {
        SharedLifeGameTests.healCannotReviveDeadPool(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void deathCascadesToAllPlayers(GameTestHelper helper) {
        SharedLifeGameTests.deathCascadesToAllPlayers(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void totemRevivesSharedLife(GameTestHelper helper) {
        SharedLifeGameTests.totemRevivesSharedLife(helper);
    }

    // Known red (see the shared test's doc): optional until the fix lands.
    @GameTest(template = EMPTY_STRUCTURE, required = false)
    public void shareDeathCascadesWithoutSharedHealth(GameTestHelper helper) {
        SharedLifeGameTests.shareDeathCascadesWithoutSharedHealth(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void hungerSyncsAcrossRealPlayers(GameTestHelper helper) {
        SharedLifeGameTests.hungerSyncsAcrossRealPlayers(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void starvationHurtsAllPlayers(GameTestHelper helper) {
        SharedLifeGameTests.starvationHurtsAllPlayers(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void combinedRegenHealsWhenAllFed(GameTestHelper helper) {
        SharedLifeGameTests.combinedRegenHealsWhenAllFed(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void combinedRegenBlockedWhileAnyoneHungry(GameTestHelper helper) {
        SharedLifeGameTests.combinedRegenBlockedWhileAnyoneHungry(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void combinedRegenFastOnlyWhenAllSaturated(GameTestHelper helper) {
        SharedLifeGameTests.combinedRegenFastOnlyWhenAllSaturated(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void individualRegenSuppressedWhenCombined(GameTestHelper helper) {
        SharedLifeGameTests.individualRegenSuppressedWhenCombined(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void experienceSyncsAcrossRealPlayers(GameTestHelper helper) {
        SharedLifeGameTests.experienceSyncsAcrossRealPlayers(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void experienceSpendingSyncsAcrossRealPlayers(GameTestHelper helper) {
        SharedLifeGameTests.experienceSpendingSyncsAcrossRealPlayers(helper);
    }

    // The one real-tick test: its own batch, so nothing else runs while its players await a real tick.
    @GameTest(template = EMPTY_STRUCTURE, batch = "serverTickHook")
    public void serverTickHookIsWired(GameTestHelper helper) {
        SharedLifeGameTests.serverTickHookIsWired(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void healthNotSharedWhenDisabled(GameTestHelper helper) {
        SharedLifeGameTests.healthNotSharedWhenDisabled(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void hungerNotSharedWhenDisabled(GameTestHelper helper) {
        SharedLifeGameTests.hungerNotSharedWhenDisabled(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void experienceNotSharedWhenDisabled(GameTestHelper helper) {
        SharedLifeGameTests.experienceNotSharedWhenDisabled(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void etherealPlayersExcluded(GameTestHelper helper) {
        SharedLifeGameTests.etherealPlayersExcluded(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void deathReseedsFromNextJoiner(GameTestHelper helper) {
        SharedLifeGameTests.deathReseedsFromNextJoiner(helper);
    }

    // Known red (see the shared test's doc): optional until the fix lands.
    @GameTest(template = EMPTY_STRUCTURE, required = false)
    public void survivalSwitchJoinsSharedLife(GameTestHelper helper) {
        SharedLifeGameTests.survivalSwitchJoinsSharedLife(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void creativeSwitchLeavesPoolUntouched(GameTestHelper helper) {
        SharedLifeGameTests.creativeSwitchLeavesPoolUntouched(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void etherealPlayersExcludedFromTickSync(GameTestHelper helper) {
        SharedLifeGameTests.etherealPlayersExcludedFromTickSync(helper);
    }

    // Known red (see the shared test's doc): optional until the fix lands.
    @GameTest(template = EMPTY_STRUCTURE, required = false)
    public void survivalSwitchReseedsDeadPool(GameTestHelper helper) {
        SharedLifeGameTests.survivalSwitchReseedsDeadPool(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void savedStateRestoresAfterReload(GameTestHelper helper) {
        SharedLifeGameTests.savedStateRestoresAfterReload(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void damageMessageAnnouncedWithSource(GameTestHelper helper) {
        SharedLifeGameTests.damageMessageAnnouncedWithSource(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void damageMessageSilencedWhenDisabled(GameTestHelper helper) {
        SharedLifeGameTests.damageMessageSilencedWhenDisabled(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void damageMessageOmitsSourceWhenDisabled(GameTestHelper helper) {
        SharedLifeGameTests.damageMessageOmitsSourceWhenDisabled(helper);
    }

    // Known red (see the shared test's doc): optional until the fix lands.
    @GameTest(template = EMPTY_STRUCTURE, required = false)
    public void deathSummaryAnnouncedOnSharedDeath(GameTestHelper helper) {
        SharedLifeGameTests.deathSummaryAnnouncedOnSharedDeath(helper);
    }

    // Known red (see the shared test's doc): optional until the fix lands.
    @GameTest(template = EMPTY_STRUCTURE, required = false)
    public void deathSummaryCountsSinceLastFullHealth(GameTestHelper helper) {
        SharedLifeGameTests.deathSummaryCountsSinceLastFullHealth(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void deathSummarySilencedWhenDisabled(GameTestHelper helper) {
        SharedLifeGameTests.deathSummarySilencedWhenDisabled(helper);
    }
}
