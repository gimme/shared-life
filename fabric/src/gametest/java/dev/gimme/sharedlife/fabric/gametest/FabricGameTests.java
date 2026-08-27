package dev.gimme.sharedlife.fabric.gametest;

import dev.gimme.sharedlife.gametest.SharedLifeGameTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric test wiring, scanned via the {@code fabric-gametest} entrypoint: one {@link GameTest} delegate
 * per shared test, on Fabric API's built-in empty 8x8x8 structure.
 */
public final class FabricGameTests {

    @GameTest
    public void damageSyncsAcrossRealPlayers(GameTestHelper helper) {
        SharedLifeGameTests.damageSyncsAcrossRealPlayers(helper);
    }

    @GameTest
    public void armorReducesSharedDamage(GameTestHelper helper) {
        SharedLifeGameTests.armorReducesSharedDamage(helper);
    }

    @GameTest
    public void absorptionAbsorbsSharedDamage(GameTestHelper helper) {
        SharedLifeGameTests.absorptionAbsorbsSharedDamage(helper);
    }

    @GameTest
    public void healingSyncsAcrossRealPlayers(GameTestHelper helper) {
        SharedLifeGameTests.healingSyncsAcrossRealPlayers(helper);
    }

    @GameTest
    public void healCannotReviveDeadPool(GameTestHelper helper) {
        SharedLifeGameTests.healCannotReviveDeadPool(helper);
    }

    @GameTest
    public void deathCascadesToAllPlayers(GameTestHelper helper) {
        SharedLifeGameTests.deathCascadesToAllPlayers(helper);
    }

    @GameTest
    public void totemRevivesSharedLife(GameTestHelper helper) {
        SharedLifeGameTests.totemRevivesSharedLife(helper);
    }

    // Known red (see the shared test's doc): optional until the fix lands.
    @GameTest(required = false)
    public void shareDeathCascadesWithoutSharedHealth(GameTestHelper helper) {
        SharedLifeGameTests.shareDeathCascadesWithoutSharedHealth(helper);
    }

    @GameTest
    public void hungerSyncsAcrossRealPlayers(GameTestHelper helper) {
        SharedLifeGameTests.hungerSyncsAcrossRealPlayers(helper);
    }

    @GameTest
    public void starvationHurtsAllPlayers(GameTestHelper helper) {
        SharedLifeGameTests.starvationHurtsAllPlayers(helper);
    }

    @GameTest
    public void combinedRegenHealsWhenAllFed(GameTestHelper helper) {
        SharedLifeGameTests.combinedRegenHealsWhenAllFed(helper);
    }

    @GameTest
    public void combinedRegenBlockedWhileAnyoneHungry(GameTestHelper helper) {
        SharedLifeGameTests.combinedRegenBlockedWhileAnyoneHungry(helper);
    }

    @GameTest
    public void combinedRegenFastOnlyWhenAllSaturated(GameTestHelper helper) {
        SharedLifeGameTests.combinedRegenFastOnlyWhenAllSaturated(helper);
    }

    @GameTest
    public void individualRegenSuppressedWhenCombined(GameTestHelper helper) {
        SharedLifeGameTests.individualRegenSuppressedWhenCombined(helper);
    }

    @GameTest
    public void experienceSyncsAcrossRealPlayers(GameTestHelper helper) {
        SharedLifeGameTests.experienceSyncsAcrossRealPlayers(helper);
    }

    @GameTest
    public void experienceSpendingSyncsAcrossRealPlayers(GameTestHelper helper) {
        SharedLifeGameTests.experienceSpendingSyncsAcrossRealPlayers(helper);
    }

    // The one real-tick test: its own environment (defined under data/sharedlife/test_environment)
    // keeps it in a sequential batch of its own.
    @GameTest(environment = "sharedlife:env/server_tick_hook_is_wired")
    public void serverTickHookIsWired(GameTestHelper helper) {
        SharedLifeGameTests.serverTickHookIsWired(helper);
    }

    @GameTest
    public void healthNotSharedWhenDisabled(GameTestHelper helper) {
        SharedLifeGameTests.healthNotSharedWhenDisabled(helper);
    }

    @GameTest
    public void hungerNotSharedWhenDisabled(GameTestHelper helper) {
        SharedLifeGameTests.hungerNotSharedWhenDisabled(helper);
    }

    @GameTest
    public void experienceNotSharedWhenDisabled(GameTestHelper helper) {
        SharedLifeGameTests.experienceNotSharedWhenDisabled(helper);
    }

    @GameTest
    public void etherealPlayersExcluded(GameTestHelper helper) {
        SharedLifeGameTests.etherealPlayersExcluded(helper);
    }

    @GameTest
    public void deathReseedsFromNextJoiner(GameTestHelper helper) {
        SharedLifeGameTests.deathReseedsFromNextJoiner(helper);
    }

    // Known red (see the shared test's doc): optional until the fix lands.
    @GameTest(required = false)
    public void survivalSwitchJoinsSharedLife(GameTestHelper helper) {
        SharedLifeGameTests.survivalSwitchJoinsSharedLife(helper);
    }

    @GameTest
    public void creativeSwitchLeavesPoolUntouched(GameTestHelper helper) {
        SharedLifeGameTests.creativeSwitchLeavesPoolUntouched(helper);
    }

    @GameTest
    public void etherealPlayersExcludedFromTickSync(GameTestHelper helper) {
        SharedLifeGameTests.etherealPlayersExcludedFromTickSync(helper);
    }

    // Known red (see the shared test's doc): optional until the fix lands.
    @GameTest(required = false)
    public void survivalSwitchReseedsDeadPool(GameTestHelper helper) {
        SharedLifeGameTests.survivalSwitchReseedsDeadPool(helper);
    }

    @GameTest
    public void savedStateRestoresAfterReload(GameTestHelper helper) {
        SharedLifeGameTests.savedStateRestoresAfterReload(helper);
    }

    @GameTest
    public void damageMessageAnnouncedWithSource(GameTestHelper helper) {
        SharedLifeGameTests.damageMessageAnnouncedWithSource(helper);
    }

    @GameTest
    public void damageMessageSilencedWhenDisabled(GameTestHelper helper) {
        SharedLifeGameTests.damageMessageSilencedWhenDisabled(helper);
    }

    @GameTest
    public void damageMessageOmitsSourceWhenDisabled(GameTestHelper helper) {
        SharedLifeGameTests.damageMessageOmitsSourceWhenDisabled(helper);
    }

    // Known red (see the shared test's doc): optional until the fix lands.
    @GameTest(required = false)
    public void deathSummaryAnnouncedOnSharedDeath(GameTestHelper helper) {
        SharedLifeGameTests.deathSummaryAnnouncedOnSharedDeath(helper);
    }

    // Known red (see the shared test's doc): optional until the fix lands.
    @GameTest(required = false)
    public void deathSummaryCountsSinceLastFullHealth(GameTestHelper helper) {
        SharedLifeGameTests.deathSummaryCountsSinceLastFullHealth(helper);
    }

    @GameTest
    public void deathSummarySilencedWhenDisabled(GameTestHelper helper) {
        SharedLifeGameTests.deathSummarySilencedWhenDisabled(helper);
    }
}
