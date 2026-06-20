package dev.gimme.sharedlife.gametest;

import com.mojang.authlib.GameProfile;
import dev.gimme.sharedlife.Main;
import dev.gimme.sharedlife.domain.SharedLife;
import dev.gimme.sharedlife.domain.util.FakePlayer;
import dev.gimme.sharedlife.infrastructure.ConfigTestSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import java.util.UUID;

/**
 * Loader-agnostic game test bodies. Each {@code static void(GameTestHelper)} method is one test;
 * a test passes by calling {@link GameTestHelper#succeed()} and fails by throwing.
 *
 * <p>To add a test: write the method here, then wire it into {@code FabricGameTests} and
 * {@code NeoForgeGameTests}.
 *
 * <p>The behavioural tests drive a freshly built {@link SharedLife} directly with detached
 * {@link FakePlayer}s, then read the shared pool back through a player that "joins" afterwards via
 * {@link SharedLife#includePotentialNewPlayer}. The share-* toggles are read globally from the live
 * config, so each test pins the ones it cares about with {@link ConfigTestSupport#override} (restored
 * on scope close) rather than leaning on the shipped defaults.
 */
public final class SharedLifeGameTests {

    private SharedLifeGameTests() {
    }

    /** Bootstrap smoke test: the harness boots, places a block, and reads it back. */
    public static void smoke(GameTestHelper helper) {
        BlockPos pos = new BlockPos(0, 1, 0);
        helper.setBlock(pos, Blocks.STONE);
        helper.assertBlockPresent(Blocks.STONE, pos);
        helper.succeed();
    }

    /** The first player seeds the pool; a player who joins afterwards is synced to that health. */
    public static void healthSyncsToJoiningPlayer(GameTestHelper helper) {
        try (var ignored = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true)) {
            SharedLife sharedLife = newSharedLife(helper);

            ServerPlayer first = spawnFake(helper, 7f);
            sharedLife.includePotentialNewPlayer(first); // seeds the pool at 7

            ServerPlayer joiner = spawnFake(helper, 20f);
            sharedLife.includePotentialNewPlayer(joiner); // pulled down to the shared 7

            assertHealth(helper, joiner, 7f);
        }
        helper.succeed();
    }

    /** With health sharing off, a joining player keeps their own health. */
    public static void healthNotSharedWhenDisabled(GameTestHelper helper) {
        try (var ignored = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, false)) {
            SharedLife sharedLife = newSharedLife(helper);

            ServerPlayer first = spawnFake(helper, 7f);
            sharedLife.includePotentialNewPlayer(first);

            ServerPlayer joiner = spawnFake(helper, 20f);
            sharedLife.includePotentialNewPlayer(joiner);

            assertHealth(helper, joiner, 20f); // untouched
        }
        helper.succeed();
    }

    /** Damaging the pool through one player lowers the shared health for everyone. */
    public static void damageReducesSharedHealth(GameTestHelper helper) {
        try (var ignored = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true)) {
            SharedLife sharedLife = newSharedLife(helper);

            ServerPlayer first = spawnFake(helper, 20f);
            sharedLife.includePotentialNewPlayer(first);

            DamageSource generic = helper.getLevel().damageSources().generic();
            sharedLife.hurtByPlayer(first, generic, 6f); // pool: 20 -> 14

            ServerPlayer joiner = spawnFake(helper, 20f);
            sharedLife.includePotentialNewPlayer(joiner);

            assertHealth(helper, joiner, 14f);
        }
        helper.succeed();
    }

    /** Healing raises the shared pool back up. */
    public static void healingRaisesSharedHealth(GameTestHelper helper) {
        try (var ignored = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true)) {
            SharedLife sharedLife = newSharedLife(helper);

            ServerPlayer first = spawnFake(helper, 20f);
            sharedLife.includePotentialNewPlayer(first);

            DamageSource generic = helper.getLevel().damageSources().generic();
            sharedLife.hurtByPlayer(first, generic, 10f); // pool: 20 -> 10
            sharedLife.healByPlayer(first, 4f);            // pool: 10 -> 14

            ServerPlayer joiner = spawnFake(helper, 20f);
            sharedLife.includePotentialNewPlayer(joiner);

            assertHealth(helper, joiner, 14f);
        }
        helper.succeed();
    }

    /** Food level is part of the shared pool and is applied to a joining player. */
    public static void hungerSyncsToJoiningPlayer(GameTestHelper helper) {
        try (var ignored = ConfigTestSupport.override(ConfigTestSupport.SHARE_HUNGER, true)) {
            SharedLife sharedLife = newSharedLife(helper);

            ServerPlayer first = spawnFake(helper, 20f);
            first.getFoodData().setFoodLevel(7);
            first.getFoodData().setSaturation(0f);
            sharedLife.includePotentialNewPlayer(first); // seeds the shared food at 7

            ServerPlayer joiner = spawnFake(helper, 20f); // arrives well-fed (20)
            sharedLife.includePotentialNewPlayer(joiner);

            helper.assertTrue(joiner.getFoodData().getFoodLevel() == 7,
                    "joining player should be synced to the shared food level 7, but had "
                            + joiner.getFoodData().getFoodLevel());
        }
        helper.succeed();
    }

    /** With hunger sharing off, a joining player keeps their own food level. */
    public static void hungerNotSharedWhenDisabled(GameTestHelper helper) {
        try (var ignored = ConfigTestSupport.override(ConfigTestSupport.SHARE_HUNGER, false)) {
            SharedLife sharedLife = newSharedLife(helper);

            ServerPlayer first = spawnFake(helper, 20f);
            first.getFoodData().setFoodLevel(7);
            sharedLife.includePotentialNewPlayer(first);

            ServerPlayer joiner = spawnFake(helper, 20f);
            sharedLife.includePotentialNewPlayer(joiner);

            helper.assertTrue(joiner.getFoodData().getFoodLevel() == 20,
                    "hunger sharing is off, so the joining player should keep food level 20, but had "
                            + joiner.getFoodData().getFoodLevel());
        }
        helper.succeed();
    }

    /** With experience sharing on, a joining player is synced to the shared level. */
    public static void experienceSharedWhenEnabled(GameTestHelper helper) {
        try (var ignored = ConfigTestSupport.override(ConfigTestSupport.SHARE_EXPERIENCE, true)) {
            SharedLife sharedLife = newSharedLife(helper);

            ServerPlayer first = spawnFake(helper, 20f);
            first.experienceLevel = 5;
            sharedLife.includePotentialNewPlayer(first); // seeds the shared experience at 5

            ServerPlayer joiner = spawnFake(helper, 20f); // no levels
            sharedLife.includePotentialNewPlayer(joiner);

            helper.assertTrue(joiner.experienceLevel == 5,
                    "experience sharing is on, so the joining player should be synced to 5 levels, but had "
                            + joiner.experienceLevel);
        }
        helper.succeed();
    }

    /** Experience sharing ships disabled, so a joining player keeps their own levels. */
    public static void experienceNotSharedWhenDisabled(GameTestHelper helper) {
        try (var ignored = ConfigTestSupport.override(ConfigTestSupport.SHARE_EXPERIENCE, false)) {
            SharedLife sharedLife = newSharedLife(helper);

            ServerPlayer first = spawnFake(helper, 20f);
            first.experienceLevel = 5;
            sharedLife.includePotentialNewPlayer(first);

            ServerPlayer joiner = spawnFake(helper, 20f); // no levels
            sharedLife.includePotentialNewPlayer(joiner);

            helper.assertTrue(joiner.experienceLevel == 0,
                    "experience sharing is off, so the joining player should keep 0 levels, but had "
                            + joiner.experienceLevel);
        }
        helper.succeed();
    }

    /** A totem of undying revives a pool that has died, restoring it to one health. */
    public static void totemRevivesDeadSharedLife(GameTestHelper helper) {
        try (var ignored = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true)) {
            SharedLife sharedLife = newSharedLife(helper);

            ServerPlayer first = spawnFake(helper, 20f);
            sharedLife.includePotentialNewPlayer(first);
            sharedLife.killBy(first);         // shared life dies
            sharedLife.protectByTotem(first); // ...and is revived to 1 health

            ServerPlayer joiner = spawnFake(helper, 20f);
            sharedLife.includePotentialNewPlayer(joiner);

            assertHealth(helper, joiner, 1f);
        }
        helper.succeed();
    }

    /** After a death, the next player to join re-seeds the pool from their own state. */
    public static void deathReseedsFromNextJoiner(GameTestHelper helper) {
        try (var ignored = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true)) {
            SharedLife sharedLife = newSharedLife(helper);

            ServerPlayer first = spawnFake(helper, 20f);
            sharedLife.includePotentialNewPlayer(first);
            sharedLife.killBy(first); // pool dies; the next joiner restarts it

            ServerPlayer reseeder = spawnFake(helper, 6f);
            sharedLife.includePotentialNewPlayer(reseeder); // pool re-seeded at 6

            ServerPlayer joiner = spawnFake(helper, 20f);
            sharedLife.includePotentialNewPlayer(joiner);

            assertHealth(helper, joiner, 6f);
        }
        helper.succeed();
    }

    /** Creative/spectator players are ethereal and never join or seed the shared pool. */
    public static void etherealPlayersDoNotJoin(GameTestHelper helper) {
        try (var ignored = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true)) {
            SharedLife sharedLife = newSharedLife(helper);

            ServerPlayer creative = spawnFake(helper, 20f);
            creative.setGameMode(GameType.CREATIVE);
            sharedLife.includePotentialNewPlayer(creative); // ignored: ethereal

            ServerPlayer first = spawnFake(helper, 7f);
            sharedLife.includePotentialNewPlayer(first); // pool still dead -> seeds at 7

            ServerPlayer joiner = spawnFake(helper, 20f);
            sharedLife.includePotentialNewPlayer(joiner);

            assertHealth(helper, joiner, 7f);
        }
        helper.succeed();
    }

    // ---- helpers ----

    private static SharedLife newSharedLife(GameTestHelper helper) {
        return new SharedLife(helper.getLevel().getServer(), Main.INSTANCE.getServerConfig());
    }

    private static ServerPlayer spawnFake(GameTestHelper helper, float health) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "TestPlayer"));
        // The game-test server defaults new players to creative, which Players treats as ethereal
        // and excludes from the shared life. Force survival so the shared-life gating applies.
        player.setGameMode(GameType.SURVIVAL);
        player.setHealth(health);
        return player;
    }

    private static void assertHealth(GameTestHelper helper, ServerPlayer player, float expected) {
        helper.assertTrue(player.getHealth() == expected,
                "expected shared health " + expected + " but player had " + player.getHealth());
    }
}
