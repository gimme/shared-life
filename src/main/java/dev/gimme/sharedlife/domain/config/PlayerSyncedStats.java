package dev.gimme.sharedlife.domain.config;

/**
 * Player stats that are synced.
 */
public record PlayerSyncedStats(
        boolean health,
        boolean food,
        boolean saturation,
        boolean thirst,
        boolean quenched
) {
}
