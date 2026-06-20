package dev.gimme.sharedlife.fabric.gametest;

import dev.gimme.sharedlife.gametest.SharedLifeGameTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric test wiring, scanned via the {@code fabric-gametest} entrypoint. One {@link GameTest}
 * delegate per shared test; the structure defaults to Fabric API's built-in empty 8x8x8.
 */
public final class FabricGameTests {

    @GameTest
    public void smoke(GameTestHelper helper) {
        SharedLifeGameTests.smoke(helper);
    }

    @GameTest
    public void healthSyncsToJoiningPlayer(GameTestHelper helper) {
        SharedLifeGameTests.healthSyncsToJoiningPlayer(helper);
    }

    @GameTest
    public void damageReducesSharedHealth(GameTestHelper helper) {
        SharedLifeGameTests.damageReducesSharedHealth(helper);
    }

    @GameTest
    public void healingRaisesSharedHealth(GameTestHelper helper) {
        SharedLifeGameTests.healingRaisesSharedHealth(helper);
    }

    @GameTest
    public void hungerSyncsToJoiningPlayer(GameTestHelper helper) {
        SharedLifeGameTests.hungerSyncsToJoiningPlayer(helper);
    }

    @GameTest
    public void totemRevivesDeadSharedLife(GameTestHelper helper) {
        SharedLifeGameTests.totemRevivesDeadSharedLife(helper);
    }

    @GameTest
    public void deathReseedsFromNextJoiner(GameTestHelper helper) {
        SharedLifeGameTests.deathReseedsFromNextJoiner(helper);
    }

    @GameTest
    public void etherealPlayersDoNotJoin(GameTestHelper helper) {
        SharedLifeGameTests.etherealPlayersDoNotJoin(helper);
    }

    @GameTest
    public void experienceNotSharedByDefault(GameTestHelper helper) {
        SharedLifeGameTests.experienceNotSharedByDefault(helper);
    }
}
