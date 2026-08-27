package dev.gimme.sharedlife.gametest;

import com.mojang.authlib.GameProfile;
import dev.gimme.sharedlife.Main;
import dev.gimme.sharedlife.domain.util.ExtractingValueOutput;
import dev.gimme.sharedlife.domain.util.FakePlayer;
import dev.gimme.sharedlife.infrastructure.ConfigTestSupport;
import dev.gimme.sharedlife.infrastructure.ConfigTestSupport.Scope;
import dev.gimme.sharedlife.infrastructure.PersistenceTestSupport;
import dev.gimme.sharedlife.infrastructure.SharedLifePersistence;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Loader-agnostic, end-to-end game test bodies, wired into {@code FabricGameTests} and
 * {@code NeoForgeGameTests}. Every test drives the production {@link Main#INSTANCE} singleton through
 * real gameplay ({@link ServerPlayer#hurtServer}, loader hooks, real {@code FoodData} ticks), so the
 * pool and config are global state: tests reset/seed the pool and pin the config toggles they rely on
 * with {@link ConfigTestSupport#override}.
 *
 * <p>Cross-player tests use {@link #spawnRealPlayer} — registered in the player list the per-tick sync
 * iterates — and after {@link #resetPool} the spawns themselves seed the pool through the mod's real
 * join-sync; join-time tests only need detached {@link FakePlayer}s. Ticks are driven by hand so each
 * test runs atomically within one server tick, except {@link #serverTickHookIsWired}, which awaits a
 * real tick in its own sequential batch.
 */
public final class SharedLifeGameTests {

    /** Mirrors the production formatter (locale-sensitive on both sides) for message assertions. */
    private static final DecimalFormat HEARTS_FORMAT = new DecimalFormat("0.0");

    private SharedLifeGameTests() {
    }

    // ---- health: damage, healing, death cascade ----

    /** Real damage on one player crosses the live shared pool to another on the next tick. */
    public static void damageSyncsAcrossRealPlayers(GameTestHelper helper) {
        try (var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HUNGER, false);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_EXPERIENCE, false)) {

            resetPool(helper);
            ServerPlayer a = spawnRealPlayer(helper);
            ServerPlayer b = spawnRealPlayer(helper);
            try {
                a.invulnerableTime = 0;
                a.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 6f);

                Main.INSTANCE.getServerHandler().onServerTick();

                assertApproxHealth(helper, a, 14f);
                assertApproxHealth(helper, b, 14f);
            } finally {
                removeRealPlayers(helper, a, b);
            }
        }
        helper.succeed();
    }

    /**
     * The pool drops by the armor-reduced damage, not the raw hit: vanilla applies armor before the
     * loader's damage hook, so full iron armor (15 points) reduces a 9.0 hit by 42% to 5.22.
     */
    public static void armorReducesSharedDamage(GameTestHelper helper) {
        try (var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HUNGER, false);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_EXPERIENCE, false)) {

            resetPool(helper);
            ServerPlayer a = spawnRealPlayer(helper);
            ServerPlayer b = spawnRealPlayer(helper);
            try {
                a.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                a.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
                a.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
                a.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
                a.doTick(); // equipment attribute modifiers (the armor points) only apply on the entity's tick
                helper.assertTrue(a.getArmorValue() == 15, "full iron armor should give 15 armor points, but A had " + a.getArmorValue());

                a.invulnerableTime = 0;
                a.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(b), 9f);

                Main.INSTANCE.getServerHandler().onServerTick();

                assertApproxHealth(helper, a, 14.78f);
                assertApproxHealth(helper, b, 14.78f);
            } finally {
                removeRealPlayers(helper, a, b);
            }
        }
        helper.succeed();
    }

    /**
     * Absorption hearts soak damage before it reaches the shared pool: only the health-reducing
     * remainder is shared, and a fully absorbed hit leaves the pool untouched.
     */
    public static void absorptionAbsorbsSharedDamage(GameTestHelper helper) {
        try (var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HUNGER, false);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_EXPERIENCE, false)) {

            resetPool(helper);
            ServerPlayer a = spawnRealPlayer(helper);
            ServerPlayer b = spawnRealPlayer(helper);
            try {
                a.getAttribute(Attributes.MAX_ABSORPTION).setBaseValue(10); // absorption is clamped by this attribute, 0 by default
                a.setAbsorptionAmount(4f);
                a.invulnerableTime = 0;
                a.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 6f);

                Main.INSTANCE.getServerHandler().onServerTick();

                assertApproxHealth(helper, a, 18f); // 4 of the 6 was absorbed, only 2 was shared
                assertApproxHealth(helper, b, 18f);

                a.setAbsorptionAmount(5f);
                a.invulnerableTime = 0;
                a.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 3f);

                Main.INSTANCE.getServerHandler().onServerTick();

                assertApproxHealth(helper, a, 18f); // fully absorbed: nothing reached the pool
                assertApproxHealth(helper, b, 18f);
            } finally {
                removeRealPlayers(helper, a, b);
            }
        }
        helper.succeed();
    }

    /** Real healing on one player raises the shared pool and lifts another on the next tick. */
    public static void healingSyncsAcrossRealPlayers(GameTestHelper helper) {
        try (var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HUNGER, false);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_EXPERIENCE, false)) {

            resetPool(helper);
            ServerPlayer a = spawnRealPlayer(helper);
            ServerPlayer b = spawnRealPlayer(helper);
            try {
                a.setHealth(10);
                b.setHealth(10);
                seedPoolFrom(a);
                Main.INSTANCE.getPlayerHandler().onPlayerJoinLevel(b); // sync B to the re-seeded pool

                a.heal(4f);

                Main.INSTANCE.getServerHandler().onServerTick();

                assertApproxHealth(helper, a, 14f);
                assertApproxHealth(helper, b, 14f);
            } finally {
                removeRealPlayers(helper, a, b);
            }
        }
        helper.succeed();
    }

    /** One player's death ends the shared life, and the next tick kills everyone still alive. */
    public static void deathCascadesToAllPlayers(GameTestHelper helper) {
        try (var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HUNGER, false);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_EXPERIENCE, false)) {

            resetPool(helper);
            ServerPlayer a = spawnRealPlayer(helper);
            ServerPlayer b = spawnRealPlayer(helper);
            try {
                a.invulnerableTime = 0;
                a.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 1000f);

                Main.INSTANCE.getServerHandler().onServerTick();

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

    /** Healing cannot revive a dead pool: a heal landing after the fatal blow must not cancel the cascade. */
    public static void healCannotReviveDeadPool(GameTestHelper helper) {
        try (var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true)) {
            ServerPlayer seed = spawnFake(helper, 20f);
            seedPoolFrom(seed);

            Main.INSTANCE.getPlayerHandler().onPlayerDeath(seed);
            Main.INSTANCE.getPlayerHandler().onPlayerHeal(seed, 4f); // e.g. regeneration firing after the death

            ServerPlayer joiner = spawnFake(helper, 20f);
            Main.INSTANCE.getPlayerHandler().onPlayerJoinLevel(joiner);
            assertHealth(helper, joiner, 20f); // the pool stayed dead, so the joiner re-seeded it instead of syncing to 4
        }
        helper.succeed();
    }

    /** A totem of undying on the fatally hit player revives the shared pool to one health for everyone. */
    public static void totemRevivesSharedLife(GameTestHelper helper) {
        try (var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HUNGER, false);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_EXPERIENCE, false)) {

            resetPool(helper);
            ServerPlayer a = spawnRealPlayer(helper);
            ServerPlayer b = spawnRealPlayer(helper);
            try {
                a.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));

                a.invulnerableTime = 0;
                a.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 1000f);

                helper.assertTrue(a.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(),
                        "the totem should have been consumed");

                Main.INSTANCE.getServerHandler().onServerTick();

                assertApproxHealth(helper, a, 1f);
                assertApproxHealth(helper, b, 1f);
            } finally {
                removeRealPlayers(helper, a, b);
            }
        }
        helper.succeed();
    }

    /**
     * The cascade's {@code shared_life} damage bypasses totems (via the {@code bypasses_invulnerability}
     * tag): a totem held by a <em>bystander</em> must not eat the cascade — only the fatally hit
     * player's own totem can save the shared life.
     */
    public static void totemDoesNotSaveCascadedPlayers(GameTestHelper helper) {
        try (var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HUNGER, false);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_EXPERIENCE, false)) {

            resetPool(helper);
            ServerPlayer a = spawnRealPlayer(helper);
            ServerPlayer b = spawnRealPlayer(helper);
            try {
                b.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));

                a.invulnerableTime = 0;
                a.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 1000f);

                Main.INSTANCE.getServerHandler().onServerTick();

                helper.assertTrue(b.isDeadOrDying(),
                        "B's totem should not block the death cascade, but B had health " + b.getHealth());
            } finally {
                removeRealPlayers(helper, a, b);
            }
        }
        helper.succeed();
    }

    // ---- death sharing without shared health ----

    /**
     * With health sharing off but death sharing on, one player's death must still kill everyone:
     * the shareDeath config promises "all players should die when one player dies".
     */
    public static void shareDeathCascadesWithoutSharedHealth(GameTestHelper helper) {
        try (var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, false);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_DEATH, true);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HUNGER, false);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_EXPERIENCE, false)) {

            resetPool(helper);
            ServerPlayer a = spawnRealPlayer(helper);
            ServerPlayer b = spawnRealPlayer(helper);
            try {
                a.invulnerableTime = 0;
                a.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 1000f);

                Main.INSTANCE.getServerHandler().onServerTick();

                helper.assertTrue(a.isDeadOrDying(),
                        "A should have died from the fatal blow, but had health " + a.getHealth());
                helper.assertTrue(b.isDeadOrDying(),
                        "B should have been killed by the shared death, but had health " + b.getHealth());
            } finally {
                removeRealPlayers(helper, a, b);
            }
        }
        helper.succeed();
    }

    /** With health and death sharing both off, one player's death leaves everyone else alive. */
    public static void deathNotSharedWhenDisabled(GameTestHelper helper) {
        try (var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, false);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_DEATH, false);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HUNGER, false);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_EXPERIENCE, false)) {

            resetPool(helper);
            ServerPlayer a = spawnRealPlayer(helper);
            ServerPlayer b = spawnRealPlayer(helper);
            try {
                a.invulnerableTime = 0;
                a.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 1000f);

                Main.INSTANCE.getServerHandler().onServerTick();

                helper.assertTrue(a.isDeadOrDying(),
                        "A should have died from the fatal blow, but had health " + a.getHealth());
                helper.assertTrue(!b.isDeadOrDying(),
                        "death sharing is off, so B should have survived A's death, but B was dying");
                assertApproxHealth(helper, b, 20f);
            } finally {
                removeRealPlayers(helper, a, b);
            }
        }
        helper.succeed();
    }

    // ---- hunger & starvation ----

    /** A change to one player's food crosses the shared pool to another on the next tick. */
    public static void hungerSyncsAcrossRealPlayers(GameTestHelper helper) {
        try (var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HUNGER, true);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_EXPERIENCE, false)) {

            resetPool(helper);
            ServerPlayer a = spawnRealPlayer(helper);
            ServerPlayer b = spawnRealPlayer(helper);
            try {
                a.getFoodData().setFoodLevel(14);
                a.getFoodData().setSaturation(0f);

                Main.INSTANCE.getServerHandler().onServerTick();

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
     * One player's exertion (sprinting, jumping) pools into the shared hunger: the shared saturation
     * pays for it once, for everyone — and the absorbed exhaustion is not re-counted on later ticks.
     */
    public static void exhaustionPoolsIntoSharedHunger(GameTestHelper helper) {
        try (var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HUNGER, true);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_EXPERIENCE, false)) {

            resetPool(helper);
            ServerPlayer a = spawnRealPlayer(helper);
            ServerPlayer b = spawnRealPlayer(helper);
            try {
                a.getFoodData().addExhaustion(5f); // as from sprinting: over vanilla's 4-point threshold

                Main.INSTANCE.getServerHandler().onServerTick();

                assertFood(helper, a, 20, 4f); // the shared saturation (spawned at 5) paid one point...
                assertFood(helper, b, 20, 4f); // ...for the whole group
                assertExhaustion(helper, a, 0f); // the player's own exhaustion was absorbed into the pool

                Main.INSTANCE.getServerHandler().onServerTick();

                assertFood(helper, a, 20, 4f); // absorbed once, not re-counted as a fresh change
                assertFood(helper, b, 20, 4f);
            } finally {
                removeRealPlayers(helper, a, b);
            }
        }
        helper.succeed();
    }

    /**
     * When the shared food empties, the shared heart starves and every player takes the damage — as
     * {@code shared_life} damage, which bypasses invulnerability, so both take it on the same tick.
     */
    public static void starvationHurtsAllPlayers(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        Difficulty previousDifficulty = helper.getLevel().getDifficulty();
        try (var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HUNGER, true);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_EXPERIENCE, false)) {

            server.setDifficulty(Difficulty.HARD, true); // any difficulty starves a full-health heart; pinned for determinism

            resetPool(helper);
            ServerPlayer a = spawnRealPlayer(helper);
            ServerPlayer b = spawnRealPlayer(helper);
            try {
                a.getFoodData().setFoodLevel(0);
                a.getFoodData().setSaturation(0f);
                seedPoolFrom(a);
                Main.INSTANCE.getPlayerHandler().onPlayerJoinLevel(b); // sync B to the re-seeded pool

                // Vanilla starvation fires once every 80 ticks of empty food; tick until the heart starves.
                boolean starved = false;
                for (int i = 0; i < 90 && !starved; i++) {
                    Main.INSTANCE.getServerHandler().onServerTick();
                    starved = a.getHealth() < 20f;
                }

                helper.assertTrue(starved, "the shared heart should have starved within 90 ticks");
                assertApproxHealth(helper, a, 19f);
                assertApproxHealth(helper, b, 19f);
            } finally {
                removeRealPlayers(helper, a, b);
            }
        } finally {
            server.setDifficulty(previousDifficulty, true);
        }
        helper.succeed();
    }

    // ---- combined natural regeneration ----

    /**
     * With every player fed to vanilla's regeneration threshold, the group heals — one heal, not one
     * per fed player — and every player pays vanilla's exhaustion cost for it.
     */
    public static void combinedRegenHealsWhenAllFed(GameTestHelper helper) {
        try (var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HUNGER, false);
             var _ = ConfigTestSupport.override(ConfigTestSupport.COMBINE_NATURAL_REGENERATION, true);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_EXPERIENCE, false)) {

            resetPool(helper);
            ServerPlayer a = spawnRealPlayer(helper);
            ServerPlayer b = spawnRealPlayer(helper);
            try {
                a.setHealth(10);
                b.setHealth(10);
                seedPoolFrom(a);
                Main.INSTANCE.getPlayerHandler().onPlayerJoinLevel(b); // sync B to the re-seeded pool

                setFood(a, 18, 0f); // fed enough for vanilla's slow regeneration, no saturation
                setFood(b, 18, 0f);

                for (int i = 0; i < 85; i++) {
                    Main.INSTANCE.getServerHandler().onServerTick();
                }

                assertApproxHealth(helper, a, 11f); // exactly one vanilla heal (tick 80), not one per player
                assertApproxHealth(helper, b, 11f);
                assertExhaustion(helper, a, 6f);    // both players paid vanilla's cost for that heal
                assertExhaustion(helper, b, 6f);
            } finally {
                removeRealPlayers(helper, a, b);
            }
        }
        helper.succeed();
    }

    /** One hungry player blocks the whole group's natural regeneration. */
    public static void combinedRegenBlockedWhileAnyoneHungry(GameTestHelper helper) {
        try (var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HUNGER, false);
             var _ = ConfigTestSupport.override(ConfigTestSupport.COMBINE_NATURAL_REGENERATION, true);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_EXPERIENCE, false)) {

            resetPool(helper);
            ServerPlayer a = spawnRealPlayer(helper);
            ServerPlayer b = spawnRealPlayer(helper);
            try {
                a.setHealth(10);
                b.setHealth(10);
                seedPoolFrom(a);
                Main.INSTANCE.getPlayerHandler().onPlayerJoinLevel(b); // sync B to the re-seeded pool

                setFood(a, 20, 5f);
                setFood(b, 17, 0f); // below vanilla's regeneration threshold of 18

                for (int i = 0; i < 85; i++) {
                    Main.INSTANCE.getServerHandler().onServerTick();
                }

                assertApproxHealth(helper, a, 10f);
                assertApproxHealth(helper, b, 10f);
                assertExhaustion(helper, a, 0f);
                assertExhaustion(helper, b, 0f);
            } finally {
                removeRealPlayers(helper, a, b);
            }
        }
        helper.succeed();
    }

    /**
     * Vanilla's fast (saturated) regeneration applies only while <em>everyone</em> is at full food with
     * saturation, limited by the group's lowest saturation.
     */
    public static void combinedRegenFastOnlyWhenAllSaturated(GameTestHelper helper) {
        try (var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HUNGER, false);
             var _ = ConfigTestSupport.override(ConfigTestSupport.COMBINE_NATURAL_REGENERATION, true);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_EXPERIENCE, false)) {

            resetPool(helper);
            ServerPlayer a = spawnRealPlayer(helper);
            ServerPlayer b = spawnRealPlayer(helper);
            try {
                a.setHealth(10);
                b.setHealth(10);
                seedPoolFrom(a);
                Main.INSTANCE.getPlayerHandler().onPlayerJoinLevel(b); // sync B to the re-seeded pool

                setFood(a, 20, 3f); // everyone at full food with saturation:
                setFood(b, 20, 5f); // vanilla's fast branch, limited by the lowest saturation (3)

                for (int i = 0; i < 12; i++) {
                    Main.INSTANCE.getServerHandler().onServerTick();
                }

                assertApproxHealth(helper, a, 10.5f); // one fast heal (tick 10) of min(3, 6) / 6 = 0.5
                assertApproxHealth(helper, b, 10.5f);

                setFood(b, 20, 0f); // B's saturation runs out: the group falls back to the slow branch

                for (int i = 0; i < 15; i++) {
                    Main.INSTANCE.getServerHandler().onServerTick();
                }

                assertApproxHealth(helper, a, 10.5f); // no fast heal without everyone saturated
                assertApproxHealth(helper, b, 10.5f);
            } finally {
                removeRealPlayers(helper, a, b);
            }
        }
        helper.succeed();
    }

    /**
     * A player's own vanilla regeneration is suppressed (via {@code MixinFoodData}) while combined
     * regeneration is active — and only then: with the option off, the same setup regenerates again.
     */
    public static void individualRegenSuppressedWhenCombined(GameTestHelper helper) {
        try (var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HUNGER, false);
             var _ = ConfigTestSupport.override(ConfigTestSupport.COMBINE_NATURAL_REGENERATION, true)) {

            ServerPlayer player = spawnFake(helper, 10f);
            setFood(player, 20, 6f);

            for (int i = 0; i < 15; i++) {
                player.getFoodData().tick(player);
            }

            assertHealth(helper, player, 10f); // vanilla's fast regeneration never fired
            assertExhaustion(helper, player, 0f);
        }

        try (var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HUNGER, false);
             var _ = ConfigTestSupport.override(ConfigTestSupport.COMBINE_NATURAL_REGENERATION, false)) {

            ServerPlayer player = spawnFake(helper, 10f);
            setFood(player, 20, 6f);

            for (int i = 0; i < 15; i++) {
                player.getFoodData().tick(player);
            }

            helper.assertTrue(player.getHealth() > 10f,
                    "combining is off, so the player should have regenerated on their own, but had health "
                            + player.getHealth());
        }
        helper.succeed();
    }

    // ---- experience ----

    /**
     * With experience sharing on, gaining levels on one player raises everyone on the next tick.
     */
    public static void experienceSyncsAcrossRealPlayers(GameTestHelper helper) {
        try (var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HUNGER, false);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_EXPERIENCE, true)) {

            resetPool(helper);
            ServerPlayer a = spawnRealPlayer(helper);
            ServerPlayer b = spawnRealPlayer(helper);
            try {
                a.giveExperienceLevels(5);

                Main.INSTANCE.getServerHandler().onServerTick();

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

    /** Spending levels on one player lowers everyone on the next tick, and the pool bottoms out at zero. */
    public static void experienceSpendingSyncsAcrossRealPlayers(GameTestHelper helper) {
        try (var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HUNGER, false);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_EXPERIENCE, true)) {

            resetPool(helper);
            ServerPlayer a = spawnRealPlayer(helper);
            ServerPlayer b = spawnRealPlayer(helper);
            try {
                a.giveExperienceLevels(5);
                Main.INSTANCE.getServerHandler().onServerTick(); // both at the shared 5 levels

                a.giveExperienceLevels(-3); // spent, as on an enchantment or anvil
                Main.INSTANCE.getServerHandler().onServerTick();

                helper.assertTrue(a.experienceLevel == 2,
                        "A should have kept the 2 levels left after spending, but had " + a.experienceLevel);
                helper.assertTrue(b.experienceLevel == 2,
                        "B should have synced down to the shared 2 levels, but had " + b.experienceLevel);

                a.giveExperienceLevels(-2); // both spend everything on the same tick:
                b.giveExperienceLevels(-2); // the pool clamps at zero instead of going negative
                Main.INSTANCE.getServerHandler().onServerTick();

                helper.assertTrue(a.experienceLevel == 0,
                        "A should have been synced to the emptied pool, but had " + a.experienceLevel);
                helper.assertTrue(b.experienceLevel == 0,
                        "B should have been synced to the emptied pool, but had " + b.experienceLevel);
            } finally {
                removeRealPlayers(helper, a, b);
            }
        }
        helper.succeed();
    }

    // ---- loader wiring: the real per-tick hook ----

    /**
     * The one test that does <em>not</em> tick by hand: a real server tick must fire the loader's
     * registered per-tick hook, so this fails if that wiring is ever dropped while every hand-driven
     * test above still passes. It must run in its own test environment (see the loader wiring), being the
     * only test whose players stay in the player list across a tick boundary; the config {@link Scope}s
     * and players are released inside the deferred callback, since try-with-resources would restore them
     * before the awaited tick.
     */
    public static void serverTickHookIsWired(GameTestHelper helper) {
        Scope shareHealth = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true);
        Scope shareHunger = ConfigTestSupport.override(ConfigTestSupport.SHARE_HUNGER, false);
        Scope shareExperience = ConfigTestSupport.override(ConfigTestSupport.SHARE_EXPERIENCE, false);

        resetPool(helper);
        ServerPlayer a = spawnRealPlayer(helper);
        ServerPlayer b = spawnRealPlayer(helper);

        a.invulnerableTime = 0;
        a.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 6f); // pool 20 -> 14

        // Deliberately NO onServerTick(): the loader's registered END_SERVER_TICK hook must run the sync.
        succeedAfterTicks(helper, 2,
                () -> {
                    assertApproxHealth(helper, a, 14f);
                    assertApproxHealth(helper, b, 14f);
                },
                () -> removeRealPlayers(helper, a, b),
                shareHealth, shareHunger, shareExperience);
    }

    // ---- join-time gating: disabled toggles, ethereal players, re-seeding ----

    /** With health sharing off, a joining player keeps their own health. */
    public static void healthNotSharedWhenDisabled(GameTestHelper helper) {
        ServerPlayer seed = spawnFake(helper, 7f);
        seedPoolFrom(seed);

        try (var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, false)) {
            ServerPlayer joiner = spawnFake(helper, 20f);
            Main.INSTANCE.getPlayerHandler().onPlayerJoinLevel(joiner);
            assertHealth(helper, joiner, 20f);
        }
        helper.succeed();
    }

    /** With hunger sharing off, a joining player keeps their own food level. */
    public static void hungerNotSharedWhenDisabled(GameTestHelper helper) {
        ServerPlayer seed = spawnFake(helper, 20f);
        seed.getFoodData().setFoodLevel(7);
        seedPoolFrom(seed);

        try (var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HUNGER, false)) {
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
        seedPoolFrom(seed);

        try (var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_EXPERIENCE, false)) {
            ServerPlayer joiner = spawnFake(helper, 20f);
            Main.INSTANCE.getPlayerHandler().onPlayerJoinLevel(joiner);
            helper.assertTrue(joiner.experienceLevel == 0,
                    "experience sharing is off, so the joining player should keep 0 levels, but had "
                            + joiner.experienceLevel);
        }
        helper.succeed();
    }

    /** Creative/spectator players are ethereal and never join or seed the shared pool. */
    public static void etherealPlayersExcluded(GameTestHelper helper) {
        try (var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true)) {
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

    // ---- game-mode changes: leaving and rejoining the shared life ----

    /**
     * Switching from creative into survival must sync the player onto the live shared pool, the same
     * way joining the level does — via the mod's post-change hook (see {@code MixinPlayerChangeGameMode}),
     * so the player no longer reads as ethereal.
     */
    public static void survivalSwitchJoinsSharedLife(GameTestHelper helper) {
        try (var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true)) {
            ServerPlayer seed = spawnFake(helper, 14f);
            seedPoolFrom(seed);

            ServerPlayer switcher = spawnFake(helper, 20f);
            switcher.setGameMode(GameType.CREATIVE);

            switcher.setGameMode(GameType.SURVIVAL); // the loader's real change hook must sync them in

            assertHealth(helper, switcher, 14f);
        }
        helper.succeed();
    }

    /**
     * Switching into creative must leave the shared life untouched: the switcher keeps their own
     * state and the pool carries on unchanged for everyone else.
     */
    public static void creativeSwitchLeavesPoolUntouched(GameTestHelper helper) {
        try (var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true)) {
            ServerPlayer seed = spawnFake(helper, 14f);
            seedPoolFrom(seed);

            ServerPlayer switcher = spawnFake(helper, 20f);
            switcher.setGameMode(GameType.CREATIVE); // must not re-seed, kill, or sync anything

            assertHealth(helper, switcher, 20f);

            ServerPlayer joiner = spawnFake(helper, 20f);
            Main.INSTANCE.getPlayerHandler().onPlayerJoinLevel(joiner);
            assertHealth(helper, joiner, 14f); // the pool carried on at the seeded 14
        }
        helper.succeed();
    }

    /**
     * Creative players are ethereal to the per-tick sync as well: the pool follows the survival
     * players and never writes to — or seeds from — the creative one. The creative player is pinned
     * at spawn because {@link GameTestHelper#makeMockServerPlayer} answers its creative/spectator
     * checks from the construction game type, not from later {@code setGameMode} calls.
     */
    public static void etherealPlayersExcludedFromTickSync(GameTestHelper helper) {
        try (var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HUNGER, false);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_EXPERIENCE, false)) {

            resetPool(helper);
            ServerPlayer creative = spawnRealPlayer(helper, GameType.CREATIVE); // never joins or seeds the pool
            ServerPlayer b = spawnRealPlayer(helper);
            try {
                b.invulnerableTime = 0;
                b.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 6f);

                Main.INSTANCE.getServerHandler().onServerTick();

                assertApproxHealth(helper, b, 14f);        // the pool carried on for the survival player
                assertApproxHealth(helper, creative, 20f); // the creative player never followed it
            } finally {
                removeRealPlayers(helper, creative, b);
            }
        }
        helper.succeed();
    }

    /**
     * After a total death, a creative player switching into survival must re-seed the dead pool from
     * their own state, just like the next player to join does.
     */
    public static void survivalSwitchReseedsDeadPool(GameTestHelper helper) {
        try (var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true)) {
            ServerPlayer switcher = spawnFake(helper, 6f);
            switcher.setGameMode(GameType.CREATIVE);

            killPool(helper.getLevel().getServer()); // total death while the switcher sits in creative

            switcher.setGameMode(GameType.SURVIVAL); // must re-seed the pool at their 6 health

            ServerPlayer joiner = spawnFake(helper, 20f);
            Main.INSTANCE.getPlayerHandler().onPlayerJoinLevel(joiner);
            assertHealth(helper, joiner, 6f); // synced to the re-seeded pool, not seeding it themselves
        }
        helper.succeed();
    }

    // ---- persistence: the shared state is saved with the world ----

    /**
     * The shared state is attached to the world's data storage at server start, and a saved state
     * restores over a later one — as across a server restart — with the next tick syncing every player
     * back to it.
     */
    public static void savedStateRestoresAfterReload(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        try (var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HUNGER, false);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_EXPERIENCE, false)) {

            SharedLifePersistence persistence = PersistenceTestSupport.find(server);
            helper.assertTrue(persistence != null,
                    "the shared life should be attached to the world's data storage at server start");

            resetPool(helper);
            ServerPlayer a = spawnRealPlayer(helper);
            ServerPlayer b = spawnRealPlayer(helper);
            try {
                a.setHealth(10);
                seedPoolFrom(a); // pin the pool at 10
                Main.INSTANCE.getPlayerHandler().onPlayerJoinLevel(b);

                CompoundTag saved = PersistenceTestSupport.save(persistence);

                b.setHealth(20);
                seedPoolFrom(b); // a different life takes over, so the restore has something to replace

                PersistenceTestSupport.restore(persistence, saved);

                Main.INSTANCE.getServerHandler().onServerTick();

                assertApproxHealth(helper, a, 10f);
                assertApproxHealth(helper, b, 10f);
            } finally {
                removeRealPlayers(helper, a, b);
            }
        }
        helper.succeed();
    }

    /**
     * The saved state carries the shared hunger and experience too: a restore brings back food,
     * saturation and levels, the rejoin syncs every player onto them — as a restart does — and the
     * next tick's delta syncs hold them steady (pinning {@code load()}'s previous-stats reset)
     * instead of re-counting the restored values as fresh changes.
     */
    public static void savedStateRestoresHungerAndExperience(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        try (var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HUNGER, true);
             var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_EXPERIENCE, true)) {

            SharedLifePersistence persistence = PersistenceTestSupport.find(server);

            resetPool(helper);
            ServerPlayer a = spawnRealPlayer(helper);
            ServerPlayer b = spawnRealPlayer(helper);
            try {
                a.setHealth(10);
                setFood(a, 12, 2f);
                a.giveExperienceLevels(4);
                seedPoolFrom(a); // pool: health 10, food 12, saturation 2.0, 4 levels
                Main.INSTANCE.getPlayerHandler().onPlayerJoinLevel(b);

                CompoundTag saved = PersistenceTestSupport.save(persistence);

                b.setHealth(20);
                setFood(b, 20, 0f);
                b.giveExperienceLevels(-4);
                seedPoolFrom(b); // a different life takes over, so the restore has something to replace

                PersistenceTestSupport.restore(persistence, saved);
                Main.INSTANCE.getPlayerHandler().onPlayerJoinLevel(a); // a restart re-joins every player
                Main.INSTANCE.getPlayerHandler().onPlayerJoinLevel(b);

                Main.INSTANCE.getServerHandler().onServerTick();

                for (ServerPlayer player : List.of(a, b)) {
                    assertApproxHealth(helper, player, 10f);
                    assertFood(helper, player, 12, 2f);
                    helper.assertTrue(player.experienceLevel == 4,
                            "expected the restored 4 shared levels, but the player had " + player.experienceLevel);
                }
            } finally {
                removeRealPlayers(helper, a, b);
            }
        }
        helper.succeed();
    }

    /** After a death, the next player to join re-seeds the pool from their own state. */
    public static void deathReseedsFromNextJoiner(GameTestHelper helper) {
        try (var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true)) {
            ServerPlayer seed = spawnFake(helper, 20f);
            seedPoolFrom(seed);

            Main.INSTANCE.getPlayerHandler().onPlayerDeath(seed);

            ServerPlayer reseeder = spawnFake(helper, 6f);
            Main.INSTANCE.getPlayerHandler().onPlayerJoinLevel(reseeder);

            ServerPlayer joiner = spawnFake(helper, 20f);
            Main.INSTANCE.getPlayerHandler().onPlayerJoinLevel(joiner);

            assertHealth(helper, joiner, 6f);
        }
        helper.succeed();
    }

    // ---- chat announcements: damage messages and the death summary ----

    /** Every hit is announced to the group as hearts of damage, naming the damage source when configured. */
    public static void damageMessageAnnouncedWithSource(GameTestHelper helper) {
        try (var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true);
             var _ = ConfigTestSupport.override(ConfigTestSupport.ANNOUNCE_DAMAGE, true);
             var _ = ConfigTestSupport.override(ConfigTestSupport.INCLUDE_DAMAGE_SOURCE, true)) {

            resetPool(helper);
            RealPlayer a = spawnRealPlayerWithChannel(helper);
            String name = a.player().getName().getString();
            try {
                drainSystemMessages(a.channel()); // discard the join noise

                a.player().invulnerableTime = 0;
                a.player().hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 6f);

                List<String> messages = drainSystemMessages(a.channel());
                String expectedHearts = HEARTS_FORMAT.format(3.0); // 6 damage = 3 hearts
                helper.assertTrue(messages.stream().anyMatch(message ->
                                message.contains(name) && message.contains(expectedHearts) && message.contains("generic")),
                        "expected a damage message naming " + name + ", " + expectedHearts
                                + "❤ and the generic source, but got " + messages);
            } finally {
                removeRealPlayers(helper, a.player());
            }
        }
        helper.succeed();
    }

    /** With announceDamage off, hits stay out of the chat. */
    public static void damageMessageSilencedWhenDisabled(GameTestHelper helper) {
        try (var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true);
             var _ = ConfigTestSupport.override(ConfigTestSupport.ANNOUNCE_DAMAGE, false)) {

            resetPool(helper);
            RealPlayer a = spawnRealPlayerWithChannel(helper);
            try {
                drainSystemMessages(a.channel());

                a.player().invulnerableTime = 0;
                a.player().hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 6f);

                List<String> messages = drainSystemMessages(a.channel());
                helper.assertTrue(messages.stream().noneMatch(message -> message.contains("❤")),
                        "announceDamage is off, so no damage message should be sent, but got " + messages);
            } finally {
                removeRealPlayers(helper, a.player());
            }
        }
        helper.succeed();
    }

    /** With includeDamageSource off, the damage message leaves out where the damage came from. */
    public static void damageMessageOmitsSourceWhenDisabled(GameTestHelper helper) {
        try (var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true);
             var _ = ConfigTestSupport.override(ConfigTestSupport.ANNOUNCE_DAMAGE, true);
             var _ = ConfigTestSupport.override(ConfigTestSupport.INCLUDE_DAMAGE_SOURCE, false)) {

            resetPool(helper);
            RealPlayer a = spawnRealPlayerWithChannel(helper);
            try {
                drainSystemMessages(a.channel());

                a.player().invulnerableTime = 0;
                a.player().hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 6f);

                List<String> messages = drainSystemMessages(a.channel());
                helper.assertTrue(messages.stream().anyMatch(message -> message.contains("❤")),
                        "expected a damage message, but got " + messages);
                helper.assertTrue(messages.stream().noneMatch(message -> message.contains("generic")),
                        "includeDamageSource is off, so the source should be omitted, but got " + messages);
            } finally {
                removeRealPlayers(helper, a.player());
            }
        }
        helper.succeed();
    }

    /**
     * When a death ends the shared life, the group is told how much damage each player took since the
     * shared health was last full — the fall that ended the life. The summary is deferred to the death
     * sync so the chat reads in order: first the death message pointing at who to blame, then the
     * summary.
     */
    public static void deathSummaryAnnouncedOnSharedDeath(GameTestHelper helper) {
        try (var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true);
             var _ = ConfigTestSupport.override(ConfigTestSupport.ANNOUNCE_DAMAGE, false);
             var _ = ConfigTestSupport.override(ConfigTestSupport.ANNOUNCE_DEATH_SUMMARY, true)) {

            resetPool(helper);
            RealPlayer a = spawnRealPlayerWithChannel(helper);
            String name = a.player().getName().getString();
            try {
                drainSystemMessages(a.channel());

                a.player().invulnerableTime = 0;
                a.player().hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 20f);

                Main.INSTANCE.getServerHandler().onServerTick(); // the death sync announces the summary

                List<String> messages = drainSystemMessages(a.channel());
                String expectedHearts = HEARTS_FORMAT.format(10.0); // the full 20-point fall
                int deathIndex = messages.indexOf(name + " died"); // vanilla's own death broadcast
                int summaryIndex = -1;
                for (int i = 0; i < messages.size(); i++) {
                    if (messages.get(i).contains(name) && messages.get(i).contains(expectedHearts)) {
                        summaryIndex = i;
                    }
                }
                helper.assertTrue(summaryIndex >= 0,
                        "expected a death summary crediting " + name + " with " + expectedHearts
                                + "❤, but got " + messages);
                helper.assertTrue(deathIndex >= 0 && deathIndex < summaryIndex,
                        "the death message should arrive before the summary, but got " + messages);
            } finally {
                removeRealPlayers(helper, a.player());
            }
        }
        helper.succeed();
    }

    /**
     * The death summary counts only the fall since the shared health was last full: damage healed
     * back to full is forgotten.
     */
    public static void deathSummaryCountsSinceLastFullHealth(GameTestHelper helper) {
        try (var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true);
             var _ = ConfigTestSupport.override(ConfigTestSupport.ANNOUNCE_DAMAGE, false);
             var _ = ConfigTestSupport.override(ConfigTestSupport.ANNOUNCE_DEATH_SUMMARY, true)) {

            resetPool(helper);
            RealPlayer a = spawnRealPlayerWithChannel(helper);
            String name = a.player().getName().getString();
            try {
                a.player().invulnerableTime = 0;
                a.player().hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 6f);
                a.player().heal(6f); // back to full health: the earlier fall is forgotten

                drainSystemMessages(a.channel());

                a.player().invulnerableTime = 0;
                a.player().hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 20f);

                Main.INSTANCE.getServerHandler().onServerTick(); // the death sync announces the summary

                List<String> messages = drainSystemMessages(a.channel());
                String expectedHearts = HEARTS_FORMAT.format(10.0); // only the final 20-point fall
                String staleHearts = HEARTS_FORMAT.format(13.0);    // 6 + 20, if the healed damage still counted
                helper.assertTrue(messages.stream().anyMatch(message ->
                                message.contains(name) && message.contains(expectedHearts)),
                        "expected a death summary crediting " + name + " with " + expectedHearts
                                + "❤, but got " + messages);
                helper.assertTrue(messages.stream().noneMatch(message -> message.contains(staleHearts)),
                        "the damage healed back to full should be forgotten, but got " + messages);
            } finally {
                removeRealPlayers(helper, a.player());
            }
        }
        helper.succeed();
    }

    /** With announceDeathSummary off, a shared death announces nothing. */
    public static void deathSummarySilencedWhenDisabled(GameTestHelper helper) {
        try (var _ = ConfigTestSupport.override(ConfigTestSupport.SHARE_HEALTH, true);
             var _ = ConfigTestSupport.override(ConfigTestSupport.ANNOUNCE_DAMAGE, false);
             var _ = ConfigTestSupport.override(ConfigTestSupport.ANNOUNCE_DEATH_SUMMARY, false)) {

            resetPool(helper);
            RealPlayer a = spawnRealPlayerWithChannel(helper);
            try {
                drainSystemMessages(a.channel());

                a.player().invulnerableTime = 0;
                a.player().hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 20f);

                Main.INSTANCE.getServerHandler().onServerTick(); // the death sync would announce here

                List<String> messages = drainSystemMessages(a.channel());
                helper.assertTrue(messages.stream().noneMatch(message -> message.contains("❤")),
                        "announceDeathSummary is off, so no summary should be sent, but got " + messages);
            } finally {
                removeRealPlayers(helper, a.player());
            }
        }
        helper.succeed();
    }

    // ---- wiring guard ----

    /**
     * Asserts the loader's wiring registers every public test body in this class, so a body added here
     * but forgotten in one loader's registry fails loudly instead of silently never running. Each
     * loader passes the names its registry wires up; the comparison ignores case and underscores, so
     * snake_case registry names match the camelCase bodies as-is.
     */
    public static void allSharedTestsRegistered(GameTestHelper helper, Set<String> registeredNames) {
        var registered = registeredNames.stream()
                .map(SharedLifeGameTests::normalizeTestName)
                .collect(Collectors.toSet());
        var missing = Arrays.stream(SharedLifeGameTests.class.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()) && Modifier.isStatic(method.getModifiers()))
                .filter(method -> method.getParameterCount() == 1 && method.getParameterTypes()[0] == GameTestHelper.class)
                .map(Method::getName)
                .filter(name -> !registered.contains(normalizeTestName(name)))
                .sorted()
                .toList();
        helper.assertTrue(missing.isEmpty(),
                "shared test bodies missing from this loader's wiring: " + missing);
        helper.succeed();
    }

    private static String normalizeTestName(String name) {
        return name.replace("_", "").toLowerCase(Locale.ROOT);
    }

    // ---- helpers ----

    /**
     * Lets {@code delayTicks} real server ticks elapse, then runs {@code assertion} once and passes the
     * test. The cleanup and config scopes are released even when the assertion throws, so a failing test
     * can't leak players or config overrides into the next one.
     */
    private static void succeedAfterTicks(GameTestHelper helper, int delayTicks, Runnable assertion,
                                          Runnable cleanup, Scope... scopes) {
        helper.runAfterDelay(delayTicks, () -> {
            try {
                assertion.run();
            } finally {
                cleanup.run();
                for (int i = scopes.length - 1; i >= 0; i--) { // reverse order, like try-with-resources
                    scopes[i].close();
                }
            }
            helper.succeed();
        });
    }

    /**
     * Pins a deterministic starting state on the shared singleton: kills the pool, then re-seeds it from
     * {@code seed}'s current health/food/experience.
     */
    private static void seedPoolFrom(ServerPlayer seed) {
        killPool(seed.level().getServer());
        Main.INSTANCE.getPlayerHandler().onPlayerJoinLevel(seed); // pool dead -> re-seed from this player
    }

    /**
     * Forces the shared pool dead by restoring a dead saved state — deliberately not through the death
     * hook: {@code killBy} is a real total death that kills every registered player, so it would wipe
     * out live players this helper is only meant to reset the pool around.
     */
    private static void killPool(MinecraftServer server) {
        var deadState = new CompoundTag();
        deadState.putFloat("health", 0f);
        PersistenceTestSupport.restore(PersistenceTestSupport.find(server), deadState);
    }

    /**
     * Test-isolation step, run before any player joins: returns the server-lifetime singleton to a dead
     * pool so this test never inherits what an earlier test left behind — the first spawn then re-seeds
     * it fresh through the mod's real join logic.
     */
    private static void resetPool(GameTestHelper helper) {
        killPool(helper.getLevel().getServer());
    }

    /** A {@link #spawnRealPlayer real player} together with its embedded channel, holding the packets sent to it. */
    private record RealPlayer(ServerPlayer player, EmbeddedChannel channel) {
    }

    /**
     * A real, survival player registered in {@code server.getPlayerList()}, so the per-tick sync loop
     * sees it and death/tick processing runs for real. {@link GameTestHelper#makeMockServerPlayer(GameType)}
     * pins survival (never ethereal, survival abilities); registration mirrors the "in level" half of the
     * deprecated {@code makeMockServerPlayerInLevel()}.
     */
    private static ServerPlayer spawnRealPlayer(GameTestHelper helper) {
        return spawnRealPlayer(helper, GameType.SURVIVAL);
    }

    /**
     * A {@link #spawnRealPlayer real player} pinned to the given game type: the mock's
     * creative/spectator checks answer from this pin, not from later {@code setGameMode} calls.
     */
    private static ServerPlayer spawnRealPlayer(GameTestHelper helper, GameType gameType) {
        return spawnRealPlayerWithChannel(helper, gameType).player();
    }

    /**
     * Like {@link #spawnRealPlayer}, but keeps the player's {@link EmbeddedChannel} so the test can
     * read the packets — e.g. chat messages — that the mod sends to the player.
     */
    private static RealPlayer spawnRealPlayerWithChannel(GameTestHelper helper) {
        return spawnRealPlayerWithChannel(helper, GameType.SURVIVAL);
    }

    private static RealPlayer spawnRealPlayerWithChannel(GameTestHelper helper, GameType gameType) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(gameType);
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        EmbeddedChannel channel = new EmbeddedChannel(connection);
        helper.getLevel().getServer().getPlayerList()
            .placeNewPlayer(connection, player, CommonListenerCookie.createInitial(player.getGameProfile(), false));
        // Mock players aren't wired to a connection, so the server never runs their per-tick update.
        helper.onEachTick(player::doTick);

        // A freshly placed player is invulnerable until its client reports the level loaded.
        player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        player.invulnerableTime = 0;
        return new RealPlayer(player, channel);
    }

    /**
     * Returns the chat messages sent to the player so far and clears the channel, so a test can
     * discard setup noise and then assert on exactly the messages its own action produced.
     */
    private static List<String> drainSystemMessages(EmbeddedChannel channel) {
        var messages = new ArrayList<String>();
        for (Object packet : channel.outboundMessages()) {
            if (packet instanceof ClientboundSystemChatPacket chat) {
                messages.add(chat.content().getString());
            }
        }
        channel.outboundMessages().clear();
        return messages;
    }

    /** Removes players registered via {@link #spawnRealPlayer} from the live player list. */
    private static void removeRealPlayers(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer player : players) {
            helper.getLevel().getServer().getPlayerList().remove(player);
        }
    }

    /** A detached survival {@link FakePlayer}, not registered in the player list — enough for join-time tests. */
    private static ServerPlayer spawnFake(GameTestHelper helper, float health) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "TestPlayer"));
        // The game-test server defaults new players to creative, which Players treats as ethereal
        // and excludes from the shared life. Pin survival through the internal mode holder:
        // setGameMode would fire the real change hook and join this player into the pool mid-setup.
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        player.setHealth(health);
        return player;
    }

    private static void setFood(ServerPlayer player, int foodLevel, float saturation) {
        player.getFoodData().setFoodLevel(foodLevel);
        player.getFoodData().setSaturation(saturation);
    }

    private static void assertFood(GameTestHelper helper, ServerPlayer player, int foodLevel, float saturation) {
        helper.assertTrue(player.getFoodData().getFoodLevel() == foodLevel,
                "expected shared food " + foodLevel + " but player had " + player.getFoodData().getFoodLevel());
        helper.assertTrue(Math.abs(player.getFoodData().getSaturationLevel() - saturation) < 0.01f,
                "expected shared saturation " + saturation + " but player had " + player.getFoodData().getSaturationLevel());
    }

    /** Reads the player's exhaustion through {@link ExtractingValueOutput}, since vanilla exposes no getter. */
    private static void assertExhaustion(GameTestHelper helper, ServerPlayer player, float expected) {
        var output = new ExtractingValueOutput();
        player.getFoodData().addAdditionalSaveData(output);
        helper.assertTrue(Math.abs(output.getExhaustion() - expected) < 0.01f,
                "expected exhaustion " + expected + " but player had " + output.getExhaustion());
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
