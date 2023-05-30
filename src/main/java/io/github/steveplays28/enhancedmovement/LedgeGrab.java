package io.github.steveplays28.enhancedmovement;

import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import io.github.steveplays28.enhancedmovement.animation.IAnimatedPlayer;
import io.github.steveplays28.enhancedmovement.config.EnhancedMovementConfigLoader;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class LedgeGrab {
	public static final String CLIMB_FRONT_ANIMATION = "climb_front";
	public static final String HANG_ANIMATION = "hang";
	public static final String SLIDE_START_ANIMATION = "slide_start";
//	public static final String SLIDE_STOP_ANIMATION = "slide_stop";
	public static final String SLIDE_ANIMATION = "slide";

	public boolean jumpKeyPressedLastTick;
	public boolean sneakKeyPressedLastTick;
	public boolean wasNearLedgeLastTick;
	public boolean isSliding;

	public void tick() {
		var minecraftClient = MinecraftClient.getInstance();
		var player = minecraftClient.player;
		if (player == null) return;
		boolean jumpKeyPressed = minecraftClient.options.jumpKey.isPressed();
		boolean sneakKeyPressed = minecraftClient.options.sneakKey.isPressed();
		boolean isNearLedge = isNearLedge(player.getBlockPos());

		// Ledge climb start
		if (jumpKeyPressed != jumpKeyPressedLastTick || isNearLedge != wasNearLedgeLastTick) {
			if (jumpKeyPressed && isNearLedge) {
				onLedgeClimb(player);
			} else {
				stopAnimations(player);
			}
		}

		// Ledge hang start
		if ((isNearLedge != wasNearLedgeLastTick || jumpKeyPressed != jumpKeyPressedLastTick) && !jumpKeyPressed) {
			if (isNearLedge) {
				onLedgeHang(player);
			} else {
				stopAnimations(player);
			}
		}

		if (isNearLedge) {
			player.setVelocity(player.getVelocity().x, player.getVelocity().y * 0.8f, player.getVelocity().z);

			if (jumpKeyPressed) {
				player.setVelocity(player.getVelocity().x, EnhancedMovementConfigLoader.CONFIG.ledgeGrabHeightPerBlock, player.getVelocity().z);
				player.addExhaustion(3000000.0F);
			} else if (sneakKeyPressed) {
				player.setVelocity(player.getVelocity().x, 0, player.getVelocity().z);
			}
		}

		// Sliding
		if (sneakKeyPressed != sneakKeyPressedLastTick && sneakKeyPressed && !jumpKeyPressed && player.isOnGround() && !player.isSubmergedInWater() && !player.isSubmergedIn(FluidTags.LAVA) && !isNearLedge && (player.forwardSpeed > 0.1f || player.sidewaysSpeed > 0.1f)) {
			onSlideStart(player);
		}
		if (sneakKeyPressed != sneakKeyPressedLastTick && !sneakKeyPressed && !jumpKeyPressed && !isNearLedge) {
			onSlideStop(player);
		}
		if (isSliding && player.getVelocity().length() < 0.1f) {
			EnhancedMovement.LOGGER.info("stop slide no vel");
			onSlideStop(player);
		}

		jumpKeyPressedLastTick = jumpKeyPressed;
		sneakKeyPressedLastTick = sneakKeyPressed;
		wasNearLedgeLastTick = isNearLedge;
	}

	public static boolean isNearLedge(@NotNull BlockPos blockPos) {
		var ledgeGrabRange = EnhancedMovementConfigLoader.CONFIG.ledgeGrabRange;

		// List of blocks which surrounds the player
		List<BlockPos> playerBlockPositions = List.of(
				// Around player block position (diagonals don't count)
				blockPos.add(0, 0, 0), blockPos.add(ledgeGrabRange, 0, 0), blockPos.add(-ledgeGrabRange, 0, 0), blockPos.add(0, 0, ledgeGrabRange), blockPos.add(0, 0, -ledgeGrabRange),

				// Around player block position + 1 up (diagonals don't count)
				blockPos.add(0, 1, 0), blockPos.add(ledgeGrabRange, 1, 0), blockPos.add(-ledgeGrabRange, 1, 0), blockPos.add(0, 1, -ledgeGrabRange), blockPos.add(0, 1, ledgeGrabRange));

		// Check if any of the surroundings is a valid ledge
		boolean hasValidLedge = playerBlockPositions.stream().anyMatch(LedgeGrab::isValidLedge);
		hasValidLedge = hasValidLedge && isEmpty(blockPos.add(0, -1, 0));

		return hasValidLedge;
	}

	public static boolean isValidLedge(BlockPos blockPos) {
		// Check if the ledge block isn't empty so it doesn't try to grab onto air
		return !isEmpty(blockPos);
	}

	public static boolean isEmpty(BlockPos blockPos) {
		var minecraftClient = MinecraftClient.getInstance();
		var player = minecraftClient.player;
		if (player == null) return true;
		var world = player.getWorld();

		BlockState blockState = player.world.getBlockState(blockPos);
		VoxelShape voxelShape = blockState.getCollisionShape(world, blockPos, ShapeContext.of(player));
		return voxelShape.isEmpty();
	}

	public void stopAnimations(ClientPlayerEntity player) {
		var animationContainer = ((IAnimatedPlayer) player).enhancedMovement_getModAnimation();
		animationContainer.replaceAnimationWithFade(AbstractFadeModifier.standardFadeIn(20, Ease.LINEAR), null);

		EnhancedMovement.LOGGER.info("stopping all animations");
	}

	public void stopAnimations(ClientPlayerEntity player, int fadeLength) {
		var animationContainer = ((IAnimatedPlayer) player).enhancedMovement_getModAnimation();
		animationContainer.replaceAnimationWithFade(AbstractFadeModifier.standardFadeIn(fadeLength, Ease.LINEAR), null);

		EnhancedMovement.LOGGER.info("stopping all animations");
	}

	public void onLedgeClimb(ClientPlayerEntity player) {
		var animationContainer = ((IAnimatedPlayer) player).enhancedMovement_getModAnimation();
		var anim = PlayerAnimationRegistry.getAnimation(new Identifier(EnhancedMovement.MOD_ID_FOLDERS, CLIMB_FRONT_ANIMATION));
		if (anim == null) {
			EnhancedMovement.LOGGER.error("Animation {} doesn't exist.", CLIMB_FRONT_ANIMATION);
			return;
		}

		animationContainer.replaceAnimationWithFade(AbstractFadeModifier.standardFadeIn(20, Ease.LINEAR), new KeyframeAnimationPlayer(anim));
		EnhancedMovement.LOGGER.info("playing ledge climb animation");
	}

	public void onLedgeHang(ClientPlayerEntity player) {
		var animationContainer = ((IAnimatedPlayer) player).enhancedMovement_getModAnimation();
		var anim = PlayerAnimationRegistry.getAnimation(new Identifier(EnhancedMovement.MOD_ID_FOLDERS, HANG_ANIMATION));
		if (anim == null) {
			EnhancedMovement.LOGGER.error("Animation {} doesn't exist.", HANG_ANIMATION);
			return;
		}

		animationContainer.replaceAnimationWithFade(AbstractFadeModifier.standardFadeIn(20, Ease.LINEAR), new KeyframeAnimationPlayer(anim));
		EnhancedMovement.LOGGER.info("playing ledge hang animation");
	}

	public void onSlideStart(ClientPlayerEntity player) {
		EnhancedMovement.LOGGER.info("slide start");

		isSliding = true;

		// Play slide start animation
		var animationContainer = ((IAnimatedPlayer) player).enhancedMovement_getModAnimation();
		var anim = PlayerAnimationRegistry.getAnimation(new Identifier(EnhancedMovement.MOD_ID_FOLDERS, SLIDE_START_ANIMATION));
		if (anim == null) {
			EnhancedMovement.LOGGER.error("Animation {} doesn't exist.", SLIDE_START_ANIMATION);
			return;
		}

		animationContainer.replaceAnimationWithFade(AbstractFadeModifier.standardFadeIn(5, Ease.LINEAR), new KeyframeAnimationPlayer(anim));

		// Play slide animation
		var anim2 = PlayerAnimationRegistry.getAnimation(new Identifier(EnhancedMovement.MOD_ID_FOLDERS, SLIDE_ANIMATION));
		if (anim2 == null) {
			EnhancedMovement.LOGGER.error("Animation {} doesn't exist.", SLIDE_ANIMATION);
			return;
		}

		animationContainer.replaceAnimationWithFade(AbstractFadeModifier.standardFadeIn(5, Ease.LINEAR), new KeyframeAnimationPlayer(anim2));
	}

	public void onSlideStop(ClientPlayerEntity player) {
		EnhancedMovement.LOGGER.info("slide stop");
		isSliding = false;
		stopAnimations(player, 10);
	}
}
