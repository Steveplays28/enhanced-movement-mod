package io.github.steveplays28.enhancedmovement.mixin;

import io.github.steveplays28.enhancedmovement.EnhancedMovement;
import io.github.steveplays28.enhancedmovement.LedgeGrab;
import io.github.steveplays28.enhancedmovement.config.EnhancedMovementConfigLoader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
	@Shadow
	public abstract boolean isSneaking();

	@Shadow
	public abstract boolean isOnGround();

	@Shadow
	public abstract BlockPos getBlockPos();

	@Shadow
	public float speed;
	@Shadow
	public float horizontalSpeed;

	@Shadow
	public abstract Box getBoundingBox();

	@Shadow
	public abstract Vec3d getPos();

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	@Shadow
	public abstract boolean isPlayer();

	@Shadow
	private EntityDimensions dimensions;
	@Shadow
	private float standingEyeHeight;

	@Shadow
	protected abstract float getEyeHeight(EntityPose pose, EntityDimensions dimensions);

	@Shadow private Vec3d pos;

	@Shadow public abstract void setPosition(Vec3d pos);

	public boolean wasSneakingLastTick;
	public boolean isSliding;

	@Inject(method = "tick", at = @At("HEAD"))
	public void tickInject(CallbackInfo ci) {
		if (!isPlayer()) return;

		// Sliding
		if (isSneaking() != wasSneakingLastTick && isSneaking() && !LedgeGrab.isNearLedge(getBlockPos()) && (speed > 0.1f || horizontalSpeed > 0.1f)) {
			onSlideStart();
		}
		if (isSneaking() != wasSneakingLastTick && !isSneaking() && isSliding) {
			onSlideStop();
		}

		wasSneakingLastTick = isSneaking();
	}

	@Inject(method = "calculateBoundingBox()Lnet/minecraft/util/math/Box;", at = @At("HEAD"), cancellable = true)
	public void calculateBoundingBoxInject(CallbackInfoReturnable<Box> cir) {
		if (!isPlayer()) return;

		if (isSliding) {
			cir.setReturnValue(dimensions.getBoxAt(getPos()).contract(0f, EnhancedMovementConfigLoader.CONFIG.slideHitBoxContractY, 0f).offset(0f, EnhancedMovementConfigLoader.CONFIG.slideHitBoxOffsetY, 0f));
			standingEyeHeight = EnhancedMovementConfigLoader.CONFIG.slideEyeHeight;
		}
	}

	public void onSlideStart() {
		// Move camera down
//		var minecraftClient = MinecraftClient.getInstance();
//		var camera = minecraftClient.getCameraEntity();
//		if (camera == null) return;
//		camera.setPosition(camera.getPos().add(0f, -4f, 0f));

		isSliding = true;

		EnhancedMovement.LOGGER.info("server slide start, bounding box = {}", getBoundingBox());
	}

	public void onSlideStop() {
		// Move camera back up
//		var minecraftClient = MinecraftClient.getInstance();
//		var camera = camera();
//		if (camera == null) return;
//		camera.setPosition(camera.getPos().add(0f, 4f, 0f));

		isSliding = false;
		setPosition(getPos().add(0f, 0.5f, 0f));

		EnhancedMovement.LOGGER.info("server slide stop, bounding box = {}", getBoundingBox());
	}
}
