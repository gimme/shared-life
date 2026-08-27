package dev.gimme.sharedlife.neoforge.gametest;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

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
    public void absorptionAbsorbsSharedDamage(GameTestHelper helper) {
        SharedLifeGameTests.absorptionAbsorbsSharedDamage(helper);
    }

    @GameTest(template = EMPTY)
    public void healingSyncsAcrossRealPlayers(GameTestHelper helper) {
        SharedLifeGameTests.healingSyncsAcrossRealPlayers(helper);
    }

    @GameTest(template = EMPTY)
    public void healCannotReviveDeadPool(GameTestHelper helper) {
        SharedLifeGameTests.healCannotReviveDeadPool(helper);
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
    public void totemDoesNotSaveCascadedPlayers(GameTestHelper helper) {
        SharedLifeGameTests.totemDoesNotSaveCascadedPlayers(helper);
    }

    @GameTest(template = EMPTY)
    public void shareDeathCascadesWithoutSharedHealth(GameTestHelper helper) {
        SharedLifeGameTests.shareDeathCascadesWithoutSharedHealth(helper);
    }

    @GameTest(template = EMPTY)
    public void deathNotSharedWhenDisabled(GameTestHelper helper) {
        SharedLifeGameTests.deathNotSharedWhenDisabled(helper);
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

    @GameTest(template = EMPTY)
    public void experienceSpendingSyncsAcrossRealPlayers(GameTestHelper helper) {
        SharedLifeGameTests.experienceSpendingSyncsAcrossRealPlayers(helper);
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
    public void survivalSwitchJoinsSharedLife(GameTestHelper helper) {
        SharedLifeGameTests.survivalSwitchJoinsSharedLife(helper);
    }

    @GameTest(template = EMPTY)
    public void creativeSwitchLeavesPoolUntouched(GameTestHelper helper) {
        SharedLifeGameTests.creativeSwitchLeavesPoolUntouched(helper);
    }

    @GameTest(template = EMPTY)
    public void etherealPlayersExcludedFromTickSync(GameTestHelper helper) {
        SharedLifeGameTests.etherealPlayersExcludedFromTickSync(helper);
    }

    @GameTest(template = EMPTY)
    public void survivalSwitchReseedsDeadPool(GameTestHelper helper) {
        SharedLifeGameTests.survivalSwitchReseedsDeadPool(helper);
    }

    @GameTest(template = EMPTY)
    public void savedStateRestoresAfterReload(GameTestHelper helper) {
        SharedLifeGameTests.savedStateRestoresAfterReload(helper);
    }

    @GameTest(template = EMPTY)
    public void savedStateRestoresHungerAndExperience(GameTestHelper helper) {
        SharedLifeGameTests.savedStateRestoresHungerAndExperience(helper);
    }

    @GameTest(template = EMPTY)
    public void damageMessageAnnouncedWithSource(GameTestHelper helper) {
        SharedLifeGameTests.damageMessageAnnouncedWithSource(helper);
    }

    @GameTest(template = EMPTY)
    public void damageMessageSilencedWhenDisabled(GameTestHelper helper) {
        SharedLifeGameTests.damageMessageSilencedWhenDisabled(helper);
    }

    @GameTest(template = EMPTY)
    public void damageMessageOmitsSourceWhenDisabled(GameTestHelper helper) {
        SharedLifeGameTests.damageMessageOmitsSourceWhenDisabled(helper);
    }

    @GameTest(template = EMPTY)
    public void deathSummaryAnnouncedOnSharedDeath(GameTestHelper helper) {
        SharedLifeGameTests.deathSummaryAnnouncedOnSharedDeath(helper);
    }

    @GameTest(template = EMPTY)
    public void deathSummaryCountsSinceLastFullHealth(GameTestHelper helper) {
        SharedLifeGameTests.deathSummaryCountsSinceLastFullHealth(helper);
    }

    @GameTest(template = EMPTY)
    public void deathSummarySilencedWhenDisabled(GameTestHelper helper) {
        SharedLifeGameTests.deathSummarySilencedWhenDisabled(helper);
    }

    /** Wiring guard: every shared test body must have a delegate in this class. */
    @GameTest(template = EMPTY)
    public void allSharedTestsRegistered(GameTestHelper helper) {
        SharedLifeGameTests.allSharedTestsRegistered(helper,
                Arrays.stream(NeoForgeGameTests.class.getDeclaredMethods())
                        .filter(method -> method.isAnnotationPresent(GameTest.class))
                        .map(Method::getName)
                        .collect(Collectors.toSet()));
    }
}
