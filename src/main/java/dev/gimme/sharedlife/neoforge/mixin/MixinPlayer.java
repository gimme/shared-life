package dev.gimme.sharedlife.neoforge.mixin;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Player.class)
public class MixinPlayer {

    @Redirect(method = {"tick"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;tick(Lnet/minecraft/world/entity/player/Player;)V"))
    private void redirectPlayerFoodDataTick(net.minecraft.world.food.FoodData foodData, Player player) {
        // Cancel food tick
    }
}
