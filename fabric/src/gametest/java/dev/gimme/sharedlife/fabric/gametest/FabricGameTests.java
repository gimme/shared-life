package dev.gimme.sharedlife.fabric.gametest;

import dev.gimme.sharedlife.gametest.SharedLifeGameTests;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric test wiring, scanned via the {@code fabric-gametest} entrypoint. One {@link GameTest}
 * delegate per shared test. Implementing {@link FabricGameTest} gives each test the API's built-in
 * empty 8x8x8 structure ({@link FabricGameTest#EMPTY_STRUCTURE}), so no template resource is needed.
 *
 * <p>Methods are instance methods invoked on a fresh instance, so this class needs a public no-arg
 * constructor (the implicit default suffices).
 */
public final class FabricGameTests implements FabricGameTest {

    @GameTest(template = EMPTY_STRUCTURE)
    public void healthSyncsToJoiningPlayer(GameTestHelper helper) {
        SharedLifeGameTests.healthSyncsToJoiningPlayer(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void healthNotSharedWhenDisabled(GameTestHelper helper) {
        SharedLifeGameTests.healthNotSharedWhenDisabled(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void damageReducesSharedHealth(GameTestHelper helper) {
        SharedLifeGameTests.damageReducesSharedHealth(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void armorReducesSharedDamage(GameTestHelper helper) {
        SharedLifeGameTests.armorReducesSharedDamage(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void healingRaisesSharedHealth(GameTestHelper helper) {
        SharedLifeGameTests.healingRaisesSharedHealth(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void hungerSyncsToJoiningPlayer(GameTestHelper helper) {
        SharedLifeGameTests.hungerSyncsToJoiningPlayer(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void hungerNotSharedWhenDisabled(GameTestHelper helper) {
        SharedLifeGameTests.hungerNotSharedWhenDisabled(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void experienceSharedWhenEnabled(GameTestHelper helper) {
        SharedLifeGameTests.experienceSharedWhenEnabled(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void experienceNotSharedWhenDisabled(GameTestHelper helper) {
        SharedLifeGameTests.experienceNotSharedWhenDisabled(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void totemRevivesDeadSharedLife(GameTestHelper helper) {
        SharedLifeGameTests.totemRevivesDeadSharedLife(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void deathReseedsFromNextJoiner(GameTestHelper helper) {
        SharedLifeGameTests.deathReseedsFromNextJoiner(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void etherealPlayersDoNotJoin(GameTestHelper helper) {
        SharedLifeGameTests.etherealPlayersDoNotJoin(helper);
    }
}
