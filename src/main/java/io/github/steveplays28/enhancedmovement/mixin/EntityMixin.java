package io.github.steveplays28.enhancedmovement.mixin;

import io.github.steveplays28.enhancedmovement.EnhancedMovement;
import io.github.steveplays28.enhancedmovement.LedgeGrab;
import io.github.steveplays28.enhancedmovement.config.EnhancedMovementConfigLoader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.registry.tag.TagKey;
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
	private EntityDimensions dimensions;
	@Shadow
	private float standingEyeHeight;

	@Shadow
	public abstract boolean isSneaking();

	@Shadow
	public abstract boolean isOnGround();

	@Shadow
	public abstract BlockPos getBlockPos();

	@Shadow
	public abstract Vec3d getPos();

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	@Shadow
	public abstract boolean isPlayer();

	@Shadow
	public abstract void setPosition(Vec3d pos);

	@Shadow
	public abstract Vec3d getRotationVector();

	@Shadow
	public abstract void addVelocity(Vec3d velocity);

	@Shadow
	public abstract Vec3d getVelocity();

	@Shadow
	public abstract boolean isSubmergedInWater();

	@Shadow
	public abstract boolean isSubmergedIn(TagKey<Fluid> fluidTag);

	@Shadow
	public abstract boolean isSprinting();

	public boolean wasSneakingLastTick;
	public boolean isSliding;

	@Inject(method = "tick", at = @At("HEAD"))
	public void tickInject(CallbackInfo ci) {
		if (!isPlayer()) return;

		// Sliding
		if (isSneaking() != wasSneakingLastTick && isSneaking() && isOnGround() && !isSubmergedInWater() && !isSubmergedIn(FluidTags.LAVA) && !LedgeGrab.isNearLedge(getBlockPos()) && isSprinting()) {
			onSlideStart();
		}
		if (isSneaking() != wasSneakingLastTick && wasSneakingLastTick && !isSneaking() && isSliding) {
			onSlideStop();
		}
		if (isSliding && getVelocity().length() < 0.1f) {
			EnhancedMovement.LOGGER.info("stop slide no vel server");
			onSlideStop();
		}

		EnhancedMovement.LOGGER.debug(getVelocity().length());

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
		EnhancedMovement.LOGGER.info("slide start server");
		isSliding = true;

		var rotationVector = getRotationVector();
		rotationVector = new Vec3d(rotationVector.x, 0f, rotationVector.z);
		addVelocity(rotationVector.multiply(2f));
	}

	public void onSlideStop() {
		EnhancedMovement.LOGGER.info("slide stop server");
		isSliding = false;
		setPosition(getPos().add(0f, 0.5f, 0f));
	}

	@Mixin(PlayerEntity.class)
	public abstract static class PlayerEntityMixin extends EntityMixin {
		@Inject(method = "clipAtLedge", at = @At("HEAD"), cancellable = true)
		public void clipAtLedge(CallbackInfoReturnable<Boolean> cir) {
			var entity = (EntityMixin) this;

			if (entity.isSliding) {
				cir.setReturnValue(false);
			} else {
				cir.setReturnValue(isSneaking());
			}
		}
	}
}
