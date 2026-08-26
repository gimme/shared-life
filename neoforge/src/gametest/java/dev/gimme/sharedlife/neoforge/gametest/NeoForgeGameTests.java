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
    public void healthSyncsToJoiningPlayer(GameTestHelper helper) {
        SharedLifeGameTests.healthSyncsToJoiningPlayer(helper);
    }

    @GameTest(template = EMPTY)
    public void healthNotSharedWhenDisabled(GameTestHelper helper) {
        SharedLifeGameTests.healthNotSharedWhenDisabled(helper);
    }

    @GameTest(template = EMPTY)
    public void damageReducesSharedHealth(GameTestHelper helper) {
        SharedLifeGameTests.damageReducesSharedHealth(helper);
    }

    @GameTest(template = EMPTY)
    public void armorReducesSharedDamage(GameTestHelper helper) {
        SharedLifeGameTests.armorReducesSharedDamage(helper);
    }

    @GameTest(template = EMPTY)
    public void healingRaisesSharedHealth(GameTestHelper helper) {
        SharedLifeGameTests.healingRaisesSharedHealth(helper);
    }

    @GameTest(template = EMPTY)
    public void hungerSyncsToJoiningPlayer(GameTestHelper helper) {
        SharedLifeGameTests.hungerSyncsToJoiningPlayer(helper);
    }

    @GameTest(template = EMPTY)
    public void hungerNotSharedWhenDisabled(GameTestHelper helper) {
        SharedLifeGameTests.hungerNotSharedWhenDisabled(helper);
    }

    @GameTest(template = EMPTY)
    public void experienceSharedWhenEnabled(GameTestHelper helper) {
        SharedLifeGameTests.experienceSharedWhenEnabled(helper);
    }

    @GameTest(template = EMPTY)
    public void experienceNotSharedWhenDisabled(GameTestHelper helper) {
        SharedLifeGameTests.experienceNotSharedWhenDisabled(helper);
    }

    @GameTest(template = EMPTY)
    public void totemRevivesDeadSharedLife(GameTestHelper helper) {
        SharedLifeGameTests.totemRevivesDeadSharedLife(helper);
    }

    @GameTest(template = EMPTY)
    public void deathReseedsFromNextJoiner(GameTestHelper helper) {
        SharedLifeGameTests.deathReseedsFromNextJoiner(helper);
    }

    @GameTest(template = EMPTY)
    public void etherealPlayersDoNotJoin(GameTestHelper helper) {
        SharedLifeGameTests.etherealPlayersDoNotJoin(helper);
    }
}
