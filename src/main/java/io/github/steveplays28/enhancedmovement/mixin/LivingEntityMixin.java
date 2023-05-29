package io.github.steveplays28.enhancedmovement.mixin;

import io.github.steveplays28.enhancedmovement.EnhancedMovement;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
	public LivingEntityMixin(EntityType<?> type, World world) {
		super(type, world);
	}

	@Inject(method = "fall", at = @At("HEAD"), cancellable = true)
	private void onFall(double heightDifference, boolean onGround, BlockState block, BlockPos pos, CallbackInfo ci) {
		LivingEntity entity = (LivingEntity) (Object) this;

		if (entity instanceof PlayerEntity player) {
			var minecraftClient = MinecraftClient.getInstance();
			var instance = EnhancedMovement.getInstance();

			if (player.equals(minecraftClient.player) && instance.hasPerformedMidAirJump()) {
				ci.cancel();
				this.fallDistance = 0.0f;
			}
		}
	}
}
