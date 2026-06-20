package dev.gimme.sharedlife.gametest;

import com.mojang.authlib.GameProfile;
import dev.gimme.sharedlife.domain.SharedLife;
import dev.gimme.sharedlife.domain.config.ServerConfig;
import dev.gimme.sharedlife.domain.util.FakePlayer;
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
 * {@link SharedLife#includePotentialNewPlayer}. They rely on the shipped config defaults
 * (health/death/hunger shared, experience not), which the live server supplies during the run.
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
        SharedLife sharedLife = newSharedLife(helper);

        ServerPlayer first = spawnFake(helper, 7f);
        sharedLife.includePotentialNewPlayer(first); // seeds the pool at 7

        ServerPlayer joiner = spawnFake(helper, 20f);
        sharedLife.includePotentialNewPlayer(joiner); // pulled down to the shared 7

        assertHealth(helper, joiner, 7f);
        helper.succeed();
    }

    /** Damaging the pool through one player lowers the shared health for everyone. */
    public static void damageReducesSharedHealth(GameTestHelper helper) {
        SharedLife sharedLife = newSharedLife(helper);

        ServerPlayer first = spawnFake(helper, 20f);
        sharedLife.includePotentialNewPlayer(first);

        DamageSource generic = helper.getLevel().damageSources().generic();
        sharedLife.hurtByPlayer(first, generic, 6f); // pool: 20 -> 14

        ServerPlayer joiner = spawnFake(helper, 20f);
        sharedLife.includePotentialNewPlayer(joiner);

        assertHealth(helper, joiner, 14f);
        helper.succeed();
    }

    /** Healing raises the shared pool back up. */
    public static void healingRaisesSharedHealth(GameTestHelper helper) {
        SharedLife sharedLife = newSharedLife(helper);

        ServerPlayer first = spawnFake(helper, 20f);
        sharedLife.includePotentialNewPlayer(first);

        DamageSource generic = helper.getLevel().damageSources().generic();
        sharedLife.hurtByPlayer(first, generic, 10f); // pool: 20 -> 10
        sharedLife.healByPlayer(first, 4f);            // pool: 10 -> 14

        ServerPlayer joiner = spawnFake(helper, 20f);
        sharedLife.includePotentialNewPlayer(joiner);

        assertHealth(helper, joiner, 14f);
        helper.succeed();
    }

    /** Food level is part of the shared pool and is applied to a joining player. */
    public static void hungerSyncsToJoiningPlayer(GameTestHelper helper) {
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
        helper.succeed();
    }

    /** A totem of undying revives a pool that has died, restoring it to one health. */
    public static void totemRevivesDeadSharedLife(GameTestHelper helper) {
        SharedLife sharedLife = newSharedLife(helper);

        ServerPlayer first = spawnFake(helper, 20f);
        sharedLife.includePotentialNewPlayer(first);
        sharedLife.killBy(first);         // shared life dies
        sharedLife.protectByTotem(first); // ...and is revived to 1 health

        ServerPlayer joiner = spawnFake(helper, 20f);
        sharedLife.includePotentialNewPlayer(joiner);

        assertHealth(helper, joiner, 1f);
        helper.succeed();
    }

    /** After a death, the next player to join re-seeds the pool from their own state. */
    public static void deathReseedsFromNextJoiner(GameTestHelper helper) {
        SharedLife sharedLife = newSharedLife(helper);

        ServerPlayer first = spawnFake(helper, 20f);
        sharedLife.includePotentialNewPlayer(first);
        sharedLife.killBy(first); // pool dies; the next joiner restarts it

        ServerPlayer reseeder = spawnFake(helper, 6f);
        sharedLife.includePotentialNewPlayer(reseeder); // pool re-seeded at 6

        ServerPlayer joiner = spawnFake(helper, 20f);
        sharedLife.includePotentialNewPlayer(joiner);

        assertHealth(helper, joiner, 6f);
        helper.succeed();
    }

    /** Creative/spectator players are ethereal and never join or seed the shared pool. */
    public static void etherealPlayersDoNotJoin(GameTestHelper helper) {
        SharedLife sharedLife = newSharedLife(helper);

        ServerPlayer creative = spawnFake(helper, 20f);
        creative.setGameMode(GameType.CREATIVE);
        sharedLife.includePotentialNewPlayer(creative); // ignored: ethereal

        ServerPlayer first = spawnFake(helper, 7f);
        sharedLife.includePotentialNewPlayer(first); // pool still dead -> seeds at 7

        ServerPlayer joiner = spawnFake(helper, 20f);
        sharedLife.includePotentialNewPlayer(joiner);

        assertHealth(helper, joiner, 7f);
        helper.succeed();
    }

    /** Experience sharing ships disabled, so a joining player keeps their own levels. */
    public static void experienceNotSharedByDefault(GameTestHelper helper) {
        SharedLife sharedLife = newSharedLife(helper);

        ServerPlayer first = spawnFake(helper, 20f);
        first.experienceLevel = 5;
        sharedLife.includePotentialNewPlayer(first);

        ServerPlayer joiner = spawnFake(helper, 20f); // no levels
        sharedLife.includePotentialNewPlayer(joiner);

        helper.assertTrue(joiner.experienceLevel == 0,
                "experience is disabled by default, so the joining player should keep 0 levels, but had "
                        + joiner.experienceLevel);
        helper.succeed();
    }

    // ---- helpers ----

    private static SharedLife newSharedLife(GameTestHelper helper) {
        return new SharedLife(helper.getLevel().getServer(), TEST_CONFIG);
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

    /** Mirrors the shipped defaults; only the announce flags are read during construction. */
    private static final ServerConfig TEST_CONFIG = new ServerConfig() {
        @Override public boolean shareHealth() { return true; }
        @Override public boolean shareDeath() { return true; }
        @Override public boolean shareHunger() { return true; }
        @Override public boolean shareExperience() { return false; }
        @Override public boolean announceDamage() { return false; }
        @Override public boolean includeDamageSource() { return false; }
    };
}
