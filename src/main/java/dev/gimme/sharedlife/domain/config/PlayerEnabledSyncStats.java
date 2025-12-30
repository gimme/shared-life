package dev.gimme.sharedlife.domain.config;

/**
 * Represents which stats are synced for a player.
 */
public record PlayerEnabledSyncStats(
        boolean health,
        boolean food,
        boolean saturation,
        boolean thirst,
        boolean quenched,
        boolean experience
) {
}
