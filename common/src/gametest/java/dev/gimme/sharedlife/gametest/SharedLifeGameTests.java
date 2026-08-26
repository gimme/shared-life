package dev.gimme.sharedlife.gametest;

import com.mojang.authlib.GameProfile;
import dev.gimme.sharedlife.Main;
import dev.gimme.sharedlife.domain.SharedLife;
import dev.gimme.sharedlife.domain.util.FakePlayer;
import dev.gimme.sharedlife.infrastructure.ConfigTestSupport;
import dev.gimme.sharedlife.infrastructure.ConfigTestSupport.Scope;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

import java.util.UUID;

/**
 * Loader-agnostic, end-to-end game test bodies. Each {@code static void(GameTestHelper)} method is one
 * test; a test passes by calling {@link GameTestHelper#succeed()} and fails by throwing.
 *
 * <p>To add a test: write the method here, then wire it into {@code FabricGameTests} and
 * {@code NeoForgeGameTests}.
 *
 * <h2>What "end-to-end" means here</h2>
 * Every test drives the production {@link Main#INSTANCE} singleton that the loader wired up at server
 * start — never a hand-built {@link SharedLife}. The behaviour under test is triggered through real
 * gameplay where one exists: real damage and healing route through {@link ServerPlayer#hurt} /
 * {@link ServerPlayer#heal} and the loader's mixin/event hook (Fabric's {@code MixinPlayer*} /
 * NeoForge's {@code Living*Event}); starvation runs the shared heart's real {@code FoodData.tick};
 * totems go through vanilla {@code checkTotemDeathProtection}; and every cross-player effect is observed
 * by running the real per-tick sync via {@link Main#getServerHandler()}.
 *
 * <h2>Two flavours of player</h2>
 * <ul>
 *   <li><b>Cross-player / per-tick tests</b> (damage, healing, hunger, starvation, totem, death cascade,
 *       experience) use {@link #spawnRealPlayer} so the players genuinely live in
 *       {@code server.getPlayerList()} — the same collection the per-tick sync loop iterates — and are
 *       removed again in a {@code finally}.
 *   <li><b>Join-time tests</b> (the {@code *NotSharedWhenDisabled} toggles, ethereal exclusion, death
 *       re-seed) only exercise {@link SharedLife#includePotentialNewPlayer} via
 *       {@code PlayerHandler.onPlayerJoinLevel}, which reads the joining player directly and never
 *       touches the player list, so they use lightweight detached {@link FakePlayer}s.
 * </ul>
 *
 * <h2>Hand-driven ticks, and the one exception</h2>
 * The per-tick tests advance the sync by hand with {@code Main.getServerHandler().onServerTick()} — fast,
 * atomic (the whole test runs inside a single server tick), and so insensitive to the order the game-test
 * framework happens to run tests in. That covers the <em>behaviour</em>, but not whether the loader
 * actually registered its per-tick hook. The single exception is {@link #serverTickHookIsWired}: it calls
 * no {@code onServerTick} and instead lets a real server tick fire the loader's registered
 * {@code END_SERVER_TICK} hook, so it fails if that wiring is ever dropped. Because it is the only test
 * whose players live in the shared player list <em>across</em> a tick boundary — where a concurrently
 * running test re-seeding the global pool would corrupt it — it runs in its own sequential batch; see the
 * loader wiring.
 *
 * <h2>Shared singleton, deterministic seeding</h2>
 * The singleton's pool is shared across every test in a run, so real-player tests first call
 * {@link #resetPool} <em>before spawning</em> — clearing whatever an earlier test left behind. With the pool
 * dead, the spawns themselves establish the starting state through the mod's real join-sync: the first
 * {@link #spawnRealPlayer} seeds the pool from its fresh, full state and each later spawn syncs to it, so
 * tests that want that default need no further setup. A test that needs a <em>non-default</em> starting pool
 * (a partial health, an emptied food bar) sets it on a player and pins it with {@link #seedPoolFrom} (kill
 * the pool, then re-seed it from that known player) before exercising the behaviour under test. The share-*
 * toggles are read globally from the live config, so each test pins the ones it cares about with
 * {@link ConfigTestSupport#override} (restored on scope close) rather than leaning on the shipped defaults.
 */
public final class SharedLifeGameTests {

    private SharedLifeGameTests() {
    }

    // ---- health: damage, healing, death cascade ----

    /**
     * Real damage on one player crosses the live shared pool to another on the next tick.
     *
     * <p>The hit goes through {@link ServerPlayer#hurt} → {@code Player.actuallyHurt} → the loader's
     * damage hook, lowering the shared pool; the per-tick broadcast over {@code getPlayerList()} then
     * pulls the second player down to match.
     */
    public static void damageSyncsAcrossRealPlayers(GameTestHelper helper) {
        try (var ignored = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true);
             var ignored2 = ConfigTestSupport.override(ConfigTestSupport.SHARE_HUNGER, false);
             var ignored3 = ConfigTestSupport.override(ConfigTestSupport.SHARE_EXPERIENCE, false)) {

            resetPool(helper); // isolate from earlier tests so the spawns below start clean
            ServerPlayer a = spawnRealPlayer(helper);
            ServerPlayer b = spawnRealPlayer(helper);
            try {
                // The spawns above already seeded the pool at 20 from A and synced B to it.
                // Real damage on A routes through actuallyHurt -> the loader's damage hook -> pool 20 -> 14.
                a.invulnerableTime = 0;
                a.hurt(helper.getLevel().damageSources().generic(), 6f);

                Main.INSTANCE.getServerHandler().onServerTick(); // broadcasts the pool over getPlayerList()

                assertApproxHealth(helper, a, 14f); // took the blow directly
                assertApproxHealth(helper, b, 14f); // synced down across the shared pool
            } finally {
                removeRealPlayers(helper, a, b);
            }
        }
        helper.succeed();
    }

    /**
     * Armor on the hurt player reduces the damage that reaches the shared pool.
     *
     * <p>Vanilla applies the victim's armor inside {@code Player.actuallyHurt}, before the loader's damage
     * hook reports the loss — so the pool must drop by the reduced amount, not the raw swing. Full iron
     * armor (15 points, no toughness) against a 9.0 hit (a stone axe swing) reduces it by
     * {@code clamp(15 - 9/2, 3, 20) / 25 = 42%} to 5.22, taking the pool from 20 to 14.78.
     */
    public static void armorReducesSharedDamage(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        boolean previousPvp = server.isPvpAllowed();
        try (var ignored = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true);
             var ignored2 = ConfigTestSupport.override(ConfigTestSupport.SHARE_HUNGER, false);
             var ignored3 = ConfigTestSupport.override(ConfigTestSupport.SHARE_EXPERIENCE, false)) {

            server.setPvpAllowed(true); // the game-test server ships with pvp off, silently blocking the hit

            resetPool(helper); // isolate from earlier tests so the spawns below start clean
            ServerPlayer a = spawnRealPlayer(helper);
            ServerPlayer b = spawnRealPlayer(helper);
            try {
                // The spawns above already seeded the pool at 20 from A and synced B to it.
                a.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                a.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
                a.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
                a.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
                a.doTick(); // equipment attribute modifiers (the armor points) only apply on the entity's tick
                helper.assertTrue(a.getArmorValue() == 15, "full iron armor should give 15 armor points, but A had " + a.getArmorValue());

                a.invulnerableTime = 0;
                a.hurt(helper.getLevel().damageSources().playerAttack(b), 9f);

                Main.INSTANCE.getServerHandler().onServerTick(); // broadcasts the pool over getPlayerList()

                assertApproxHealth(helper, a, 14.78f); // took the armor-reduced 5.22, not the raw 9
                assertApproxHealth(helper, b, 14.78f); // synced down by the reduced amount too
            } finally {
                removeRealPlayers(helper, a, b);
            }
        } finally {
            server.setPvpAllowed(previousPvp);
        }
        helper.succeed();
    }

    /**
     * Real healing on one player raises the shared pool and lifts another on the next tick.
     *
     * <p>{@link ServerPlayer#heal} fires the loader's heal hook, so the shared pool climbs back up and the
     * per-tick sync raises everyone else to match.
     */
    public static void healingSyncsAcrossRealPlayers(GameTestHelper helper) {
        try (var ignored = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true);
             var ignored2 = ConfigTestSupport.override(ConfigTestSupport.SHARE_HUNGER, false);
             var ignored3 = ConfigTestSupport.override(ConfigTestSupport.SHARE_EXPERIENCE, false)) {

            resetPool(helper); // isolate from earlier tests so the spawns below start clean
            ServerPlayer a = spawnRealPlayer(helper);
            ServerPlayer b = spawnRealPlayer(helper);
            a.setHealth(10);
            b.setHealth(10);
            try {
                seedPoolFrom(a);                                       // pool seeded at 10 from A
                Main.INSTANCE.getPlayerHandler().onPlayerJoinLevel(b); // B synced to 10

                // Real heal on A fires the loader's heal hook -> pool 10 -> 14.
                a.heal(4f);

                Main.INSTANCE.getServerHandler().onServerTick();

                assertApproxHealth(helper, a, 14f); // healed directly
                assertApproxHealth(helper, b, 14f); // synced up across the shared pool
            } finally {
                removeRealPlayers(helper, a, b);
            }
        }
        helper.succeed();
    }

    /**
     * One player's death ends the shared life, and the next tick kills everyone still alive.
     *
     * <p>A real fatal blow on one player routes through {@link ServerPlayer#hurt} → {@code die()} →
     * the loader's death hook (Fabric's {@code MixinPlayerDie} / NeoForge's {@code LivingDeathEvent}), which
     * ends the shared life. The cascade then runs: {@code syncHealth} sees the dead pool and applies lethal
     * {@code shared_life} damage to every other live player, again via {@link ServerPlayer#hurt}.
     */
    public static void deathCascadesToAllPlayers(GameTestHelper helper) {
        try (var ignored = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true);
             var ignored2 = ConfigTestSupport.override(ConfigTestSupport.SHARE_HUNGER, false);
             var ignored3 = ConfigTestSupport.override(ConfigTestSupport.SHARE_EXPERIENCE, false)) {

            resetPool(helper); // isolate from earlier tests so the spawns below start clean
            ServerPlayer a = spawnRealPlayer(helper);
            ServerPlayer b = spawnRealPlayer(helper);
            try {
                // The spawns above already seeded the pool at 20 from A and synced B to it.
                // A real fatal blow on A goes through hurt -> die() -> the loader's death hook,
                // which ends the shared life.
                a.invulnerableTime = 0;
                a.hurt(helper.getLevel().damageSources().generic(), 1000f);

                Main.INSTANCE.getServerHandler().onServerTick();    // cascade: kills every live player

                helper.assertTrue(a.isDeadOrDying(),
                        "A should have died from the fatal blow, but had health " + a.getHealth());
                helper.assertTrue(b.isDeadOrDying(),
                        "B should have been killed by the death cascade, but had health " + b.getHealth());
            } finally {
                removeRealPlayers(helper, a, b);
            }
        }
        helper.succeed();
    }

    /**
     * A totem of undying revives a pool that has died, restoring it to one health for everyone.
     *
     * <p>A real fatal blow on a totem-holding player triggers vanilla {@code checkTotemDeathProtection},
     * which the loader's totem hook observes and turns into a shared-life revive; the next tick lifts the
     * other player off the floor too.
     */
    public static void totemRevivesSharedLife(GameTestHelper helper) {
        try (var ignored = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true);
             var ignored2 = ConfigTestSupport.override(ConfigTestSupport.SHARE_HUNGER, false);
             var ignored3 = ConfigTestSupport.override(ConfigTestSupport.SHARE_EXPERIENCE, false)) {

            resetPool(helper); // isolate from earlier tests so the spawns below start clean
            ServerPlayer a = spawnRealPlayer(helper);
            ServerPlayer b = spawnRealPlayer(helper);
            try {
                // The spawns above already seeded the pool at 20 from A and synced B to it.
                a.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));

                // A fatal blow consumes the totem (vanilla saves A at 1 health) and fires the totem hook,
                // which revives the shared pool to 1.
                a.invulnerableTime = 0;
                a.hurt(helper.getLevel().damageSources().generic(), 1000f);

                helper.assertTrue(a.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(),
                        "the totem should have been consumed");

                Main.INSTANCE.getServerHandler().onServerTick();

                assertApproxHealth(helper, a, 1f); // saved by the totem
                assertApproxHealth(helper, b, 1f); // revived across the shared pool
            } finally {
                removeRealPlayers(helper, a, b);
            }
        }
        helper.succeed();
    }

    // ---- hunger & starvation ----

    /**
     * A change to one player's food crosses the shared pool to another on the next tick.
     *
     * <p>Vanilla hunger is disabled for shared players and managed by the shared heart, so dropping A's
     * food and running the real per-tick hunger sync pulls B's food down to match.
     */
    public static void hungerSyncsAcrossRealPlayers(GameTestHelper helper) {
        try (var ignored = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true);
             var ignored2 = ConfigTestSupport.override(ConfigTestSupport.SHARE_HUNGER, true);
             var ignored3 = ConfigTestSupport.override(ConfigTestSupport.SHARE_EXPERIENCE, false)) {

            resetPool(helper); // isolate from earlier tests so the spawns below start clean
            ServerPlayer a = spawnRealPlayer(helper);
            ServerPlayer b = spawnRealPlayer(helper);
            try {
                // The spawns above already seeded the pool food at 20 from A and synced B to it.
                a.getFoodData().setFoodLevel(14);
                a.getFoodData().setSaturation(0f);

                Main.INSTANCE.getServerHandler().onServerTick(); // shared hunger sync

                helper.assertTrue(a.getFoodData().getFoodLevel() == 14,
                        "A should have kept food 14, but had " + a.getFoodData().getFoodLevel());
                helper.assertTrue(b.getFoodData().getFoodLevel() == 14,
                        "B should have synced to the shared food 14, but had " + b.getFoodData().getFoodLevel());
            } finally {
                removeRealPlayers(helper, a, b);
            }
        }
        helper.succeed();
    }

    /**
     * When the shared food empties, the shared heart starves and the damage is dealt to every player.
     *
     * <p>This drives the real {@code FoodData.tick} on the shared heart: with food at zero and HARD
     * difficulty it eventually applies starvation, which the mod distributes to every live player as
     * {@code shared_life} damage. Because that damage type bypasses invulnerability, both players take it
     * on the same tick.
     */
    public static void starvationHurtsAllPlayers(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        Difficulty previousDifficulty = helper.getLevel().getDifficulty();
        try (var ignored = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true);
             var ignored2 = ConfigTestSupport.override(ConfigTestSupport.SHARE_HUNGER, true);
             var ignored3 = ConfigTestSupport.override(ConfigTestSupport.SHARE_EXPERIENCE, false)) {

            server.setDifficulty(Difficulty.HARD, true); // EASY/NORMAL won't starve a full-health heart

            resetPool(helper); // isolate from earlier tests so the spawns below start clean
            ServerPlayer a = spawnRealPlayer(helper);
            ServerPlayer b = spawnRealPlayer(helper);
            try {
                a.getFoodData().setFoodLevel(0);
                a.getFoodData().setSaturation(0f);
                seedPoolFrom(a);                                       // pool seeded at health 20, food 0
                Main.INSTANCE.getPlayerHandler().onPlayerJoinLevel(b); // B synced to health 20, food 0

                // Vanilla starvation fires once every 80 ticks of empty food; tick until the heart starves.
                boolean starved = false;
                for (int i = 0; i < 90 && !starved; i++) {
                    Main.INSTANCE.getServerHandler().onServerTick();
                    starved = a.getHealth() < 20f;
                }

                helper.assertTrue(starved, "the shared heart should have starved within 90 ticks");
                assertApproxHealth(helper, a, 19f); // took a point of shared starvation
                assertApproxHealth(helper, b, 19f); // and so did B, across the shared pool
            } finally {
                removeRealPlayers(helper, a, b);
            }
        } finally {
            server.setDifficulty(previousDifficulty, true);
        }
        helper.succeed();
    }

    // ---- experience ----

    /**
     * With experience sharing on, gaining levels on one player raises everyone on the next tick.
     */
    public static void experienceSyncsAcrossRealPlayers(GameTestHelper helper) {
        try (var ignored = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true);
             var ignored2 = ConfigTestSupport.override(ConfigTestSupport.SHARE_HUNGER, false);
             var ignored3 = ConfigTestSupport.override(ConfigTestSupport.SHARE_EXPERIENCE, true)) {

            resetPool(helper); // isolate from earlier tests so the spawns below start clean
            ServerPlayer a = spawnRealPlayer(helper);
            ServerPlayer b = spawnRealPlayer(helper);
            try {
                // The spawns above already seeded the pool experience at 0 from A and synced B to it.
                a.giveExperienceLevels(5);

                Main.INSTANCE.getServerHandler().onServerTick(); // shared experience sync

                helper.assertTrue(a.experienceLevel == 5,
                        "A should have kept 5 levels, but had " + a.experienceLevel);
                helper.assertTrue(b.experienceLevel == 5,
                        "B should have synced to the shared 5 levels, but had " + b.experienceLevel);
            } finally {
                removeRealPlayers(helper, a, b);
            }
        }
        helper.succeed();
    }

    // ---- loader wiring: the real per-tick hook ----

    /**
     * The one test that does <em>not</em> tick by hand: it proves the loader actually registered its
     * per-tick hook. After lowering the shared pool, it lets a real server tick fire the loader's
     * {@code END_SERVER_TICK} hook (Fabric {@code ServerTickEvents.END_SERVER_TICK} / NeoForge
     * {@code ServerTickEvent.Post}) and asserts the second player synced. If that registration were ever
     * dropped, every hand-driven behavioural test above would still pass while this one fails.
     *
     * <p>It must run in its own batch (see the loader wiring): it is the only test whose players stay in
     * the shared player list across a tick boundary, so sharing a batch with another test that re-seeds
     * the global pool mid-flight would corrupt it. Because the assertion runs after the test body
     * returns, the config {@link Scope}s and players are released inside the deferred callback rather than
     * via try-with-resources, which would restore them before the awaited tick.
     */
    public static void serverTickHookIsWired(GameTestHelper helper) {
        Scope shareHealth = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true);
        Scope shareHunger = ConfigTestSupport.override(ConfigTestSupport.SHARE_HUNGER, false);
        Scope shareExperience = ConfigTestSupport.override(ConfigTestSupport.SHARE_EXPERIENCE, false);

        resetPool(helper); // isolate from earlier tests so the spawns below start clean
        ServerPlayer a = spawnRealPlayer(helper);
        ServerPlayer b = spawnRealPlayer(helper);

        // The spawns above already seeded the pool at 20 from A and synced B to it.
        a.invulnerableTime = 0;
        a.hurt(helper.getLevel().damageSources().generic(), 6f); // pool 20 -> 14

        // Deliberately NO onServerTick(): the loader's registered END_SERVER_TICK hook must run the sync.
        succeedAfterTicks(helper, 2,
                () -> {
                    assertApproxHealth(helper, a, 14f); // took the blow directly
                    assertApproxHealth(helper, b, 14f); // only synced if the loader's tick hook fired
                },
                () -> removeRealPlayers(helper, a, b),
                shareHealth, shareHunger, shareExperience);
    }

    // ---- join-time gating: disabled toggles, ethereal players, re-seeding ----

    /** With health sharing off, a joining player keeps their own health. */
    public static void healthNotSharedWhenDisabled(GameTestHelper helper) {
        ServerPlayer seed = spawnFake(helper, 7f);
        seedPoolFrom(seed); // pool seeded at 7

        try (var ignored = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, false)) {
            ServerPlayer joiner = spawnFake(helper, 20f);
            Main.INSTANCE.getPlayerHandler().onPlayerJoinLevel(joiner);
            assertHealth(helper, joiner, 20f); // untouched
        }
        helper.succeed();
    }

    /** With hunger sharing off, a joining player keeps their own food level. */
    public static void hungerNotSharedWhenDisabled(GameTestHelper helper) {
        ServerPlayer seed = spawnFake(helper, 20f);
        seed.getFoodData().setFoodLevel(7);
        seedPoolFrom(seed); // pool seeded at food 7

        try (var ignored = ConfigTestSupport.override(ConfigTestSupport.SHARE_HUNGER, false)) {
            ServerPlayer joiner = spawnFake(helper, 20f);
            Main.INSTANCE.getPlayerHandler().onPlayerJoinLevel(joiner);
            helper.assertTrue(joiner.getFoodData().getFoodLevel() == 20,
                    "hunger sharing is off, so the joining player should keep food level 20, but had "
                            + joiner.getFoodData().getFoodLevel());
        }
        helper.succeed();
    }

    /** Experience sharing ships disabled, so a joining player keeps their own levels. */
    public static void experienceNotSharedWhenDisabled(GameTestHelper helper) {
        ServerPlayer seed = spawnFake(helper, 20f);
        seed.giveExperienceLevels(5);
        seedPoolFrom(seed); // pool seeded at experience 5

        try (var ignored = ConfigTestSupport.override(ConfigTestSupport.SHARE_EXPERIENCE, false)) {
            ServerPlayer joiner = spawnFake(helper, 20f); // no levels
            Main.INSTANCE.getPlayerHandler().onPlayerJoinLevel(joiner);
            helper.assertTrue(joiner.experienceLevel == 0,
                    "experience sharing is off, so the joining player should keep 0 levels, but had "
                            + joiner.experienceLevel);
        }
        helper.succeed();
    }

    /** Creative/spectator players are ethereal and never join or seed the shared pool. */
    public static void etherealPlayersExcluded(GameTestHelper helper) {
        try (var ignored = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true)) {
            ServerPlayer first = spawnFake(helper, 7f);
            Main.INSTANCE.getPlayerHandler().onPlayerDeath(first); // ensure the pool starts dead

            ServerPlayer creative = spawnFake(helper, 20f);
            creative.setGameMode(GameType.CREATIVE);
            Main.INSTANCE.getPlayerHandler().onPlayerJoinLevel(creative); // ignored: ethereal

            Main.INSTANCE.getPlayerHandler().onPlayerJoinLevel(first); // pool still dead -> seeds at 7

            ServerPlayer joiner = spawnFake(helper, 20f);
            Main.INSTANCE.getPlayerHandler().onPlayerJoinLevel(joiner);

            assertHealth(helper, joiner, 7f); // synced to the survival seed, not the ignored creative one
        }
        helper.succeed();
    }

    /** After a death, the next player to join re-seeds the pool from their own state. */
    public static void deathReseedsFromNextJoiner(GameTestHelper helper) {
        try (var ignored = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true)) {
            ServerPlayer seed = spawnFake(helper, 20f);
            seedPoolFrom(seed);                                    // pool alive at 20

            Main.INSTANCE.getPlayerHandler().onPlayerDeath(seed);  // pool dies; the next joiner restarts it

            ServerPlayer reseeder = spawnFake(helper, 6f);
            Main.INSTANCE.getPlayerHandler().onPlayerJoinLevel(reseeder); // pool re-seeded at 6

            ServerPlayer joiner = spawnFake(helper, 20f);
            Main.INSTANCE.getPlayerHandler().onPlayerJoinLevel(joiner);

            assertHealth(helper, joiner, 6f);
        }
        helper.succeed();
    }

    // ---- helpers ----

    /**
     * Lets {@code delayTicks} real server ticks elapse — so the loader's registered
     * {@code END_SERVER_TICK} hook drives the shared-life sync exactly as it does in production — then
     * runs {@code assertion} once and passes the test. The {@code cleanup} and every config {@code scope}
     * are released afterwards whether the assertion passes or throws, so a failing test can't leak its
     * players or config overrides into the next one. Only {@link #serverTickHookIsWired} uses this; the
     * other per-tick tests advance the sync synchronously instead.
     */
    private static void succeedAfterTicks(GameTestHelper helper, int delayTicks, Runnable assertion,
                                          Runnable cleanup, Scope... scopes) {
        helper.runAfterDelay(delayTicks, () -> {
            try {
                assertion.run();
            } finally {
                cleanup.run();
                for (Scope scope : scopes) {
                    scope.close();
                }
            }
            helper.succeed();
        });
    }

    /**
     * Pins a deterministic starting state on the shared singleton: kills the pool, then re-seeds it from
     * {@code seed}'s current health/food/experience.
     *
     * <p>The kill is forced with health sharing temporarily on (so {@code killBy} applies regardless of the
     * test's toggles); the re-seed runs through {@code initializeFrom}, which copies the player's full
     * state unconditionally.
     */
    private static void seedPoolFrom(ServerPlayer seed) {
        killPool(seed);
        Main.INSTANCE.getPlayerHandler().onPlayerJoinLevel(seed); // pool dead -> re-seed from this player
    }

    /**
     * Forces the shared pool dead, with health sharing temporarily on so {@code killBy} applies regardless
     * of the test's toggles. {@code agent} is only read for the death-enabled check and the log line, so it
     * need not be registered in the player list.
     */
    private static void killPool(ServerPlayer agent) {
        try (var ignored = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true)) {
            Main.INSTANCE.getPlayerHandler().onPlayerDeath(agent); // force the pool dead
        }
    }

    /**
     * Test-isolation step, run before any player joins: returns the server-lifetime singleton to a clean
     * (dead) pool so this test never inherits the health an earlier test left behind. With the pool dead,
     * the first {@link #spawnRealPlayer} re-seeds it from a genuinely fresh, full-health player and later
     * joins sync to that — the mod's real join logic, with nothing forced afterwards.
     *
     * <p>Reuses {@link #killPool}; since no player has been spawned yet, it passes a detached
     * {@link FakePlayer} (never placed, never in the player list, its health irrelevant) purely to satisfy
     * the kill's death-enabled gate.
     */
    private static void resetPool(GameTestHelper helper) {
        killPool(spawnFake(helper, 20f));
    }

    /**
     * A real, survival player registered in {@code server.getPlayerList()} — so the per-tick sync loop,
     * which iterates that list, actually sees it.
     *
     * <p>The player is a near-plain {@link ServerPlayer}, not one of the helper's mocks:
     * {@code GameTestHelper.makeMockServerPlayerInLevel()} hardcodes {@code isCreative()} to true, which
     * {@code Players} treats as permanently ethereal, and {@code makeMockPlayer} does not produce a
     * {@link ServerPlayer} at all. Unlike a {@link FakePlayer}, it has no neutered
     * {@code die()}/{@code tick()}: death and per-tick processing run for real, so a fatal blow actually
     * fires the loader's death hook ({@code ServerPlayer.die} → Fabric's {@code MixinPlayerDie} /
     * NeoForge's {@code LivingDeathEvent}) — exactly the wiring these end-to-end tests exist to drive.
     *
     * <p>Registration mirrors {@code makeMockServerPlayerInLevel()}: a fresh {@link Connection} fed through
     * an {@link EmbeddedChannel}, then {@code placeNewPlayer}. Placed players start in the game-test
     * server's creative mode, so we force survival and clear the ability flags creative left set, and lift
     * vanilla's 60-tick post-spawn invulnerability so hits land immediately.
     */
    private static ServerPlayer spawnRealPlayer(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();

        GameProfile profile = new GameProfile(UUID.randomUUID(), "e2e-mock");
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
        ServerPlayer player = new ServerPlayer(server, level, cookie.gameProfile(), cookie.clientInformation()) {
            // The loader's join hook fires inside placeNewPlayer, while the game-test server's default
            // creative mode is still applied — which Players would treat as ethereal, silently skipping
            // the join-time pool sync the spawns rely on. Pin the ethereal check to survival semantics
            // from construction, the way the helper's own mock pins its overrides.
            @Override
            public boolean isCreative() {
                return false;
            }

            @Override
            public boolean isSpectator() {
                return false;
            }
        };
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection); // wires connection.channel so placeNewPlayer can send packets
        server.getPlayerList().placeNewPlayer(connection, player, cookie);
        // Mock players aren't wired to a real client connection, so the server never runs their per-tick update.
        helper.onEachTick(player::doTick);

        player.setGameMode(GameType.SURVIVAL);
        Abilities abilities = player.getAbilities();
        abilities.invulnerable = false;
        abilities.instabuild = false;
        abilities.flying = false;
        player.onUpdateAbilities();
        clearSpawnInvulnerability(player);
        player.invulnerableTime = 0;
        return player;
    }

    /** Removes players registered via {@link #spawnRealPlayer} from the live player list. */
    private static void removeRealPlayers(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer player : players) {
            helper.getLevel().getServer().getPlayerList().remove(player);
        }
    }

    /**
     * A detached survival {@link FakePlayer} that is <em>not</em> registered in the player list — enough for
     * join-time tests, which only pass the player to {@code onPlayerJoinLevel} and read its state back.
     */
    private static ServerPlayer spawnFake(GameTestHelper helper, float health) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "TestPlayer"));
        // A detached player starts in DEFAULT_MODE (survival), so the shared-life gating applies with
        // no forced game mode — deliberately: setGameMode fires the loaders' change-game-mode hook,
        // which would include this half-initialized fake in the production pool and corrupt the test.
        player.setHealth(health);
        return player;
    }

    /**
     * Clears vanilla's 60-tick post-spawn invulnerability ({@code ServerPlayer#spawnInvulnerableTime}),
     * which only counts down as the player ticks — without this, {@code ServerPlayer#hurt} swallows every
     * hit landed in the spawn tick. Private with no setter, hence reflection; game tests only run in dev,
     * where both loaders use Mojang names.
     */
    private static void clearSpawnInvulnerability(ServerPlayer player) {
        try {
            var field = ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
            field.setAccessible(true);
            field.setInt(player, 0);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("could not clear spawn invulnerability", e);
        }
    }

    private static void assertApproxHealth(GameTestHelper helper, ServerPlayer player, float expected) {
        helper.assertTrue(Math.abs(player.getHealth() - expected) < 0.01f,
                "expected shared health ~" + expected + " but player had " + player.getHealth());
    }

    private static void assertHealth(GameTestHelper helper, ServerPlayer player, float expected) {
        helper.assertTrue(player.getHealth() == expected,
                "expected shared health " + expected + " but player had " + player.getHealth());
    }
}
