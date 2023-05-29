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
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class LedgeGrab {
	public static final String CLIMB_FRONT_ANIMATION = "climb_front";
	public static final String HANG_ANIMATION = "hang";

	public boolean jumpKeyPressedLastTick;
	public boolean sneakKeyPressedLastTick;
	public boolean wasNearLedgeLastTick;

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
				player.setVelocity(player.getVelocity().x, EnhancedMovementConfigLoader.CONFIG.ledgeGrabHeightPerBlock * getLedgeHeight(player.getBlockPos()), player.getVelocity().z);
				player.addExhaustion(3000000.0F);
			} else if (sneakKeyPressed) {
				player.setVelocity(player.getVelocity().x, 0, player.getVelocity().z);
			}
		}

		jumpKeyPressedLastTick = jumpKeyPressed;
		sneakKeyPressedLastTick = sneakKeyPressed;
		wasNearLedgeLastTick = isNearLedge;
	}

	public boolean isNearLedge(@NotNull BlockPos blockPos) {
		var ledgeGrabRange = EnhancedMovementConfigLoader.CONFIG.ledgeGrabRange;

		// List of blocks which surrounds the player
		List<BlockPos> playerBlockPositions = List.of(
				// Around player block position (diagonals don't count)
				blockPos.add(0, 0, 0), blockPos.add(ledgeGrabRange, 0, 0), blockPos.add(-ledgeGrabRange, 0, 0), blockPos.add(0, 0, ledgeGrabRange), blockPos.add(0, 0, -ledgeGrabRange),

				// Around player block position + 1 up (diagonals don't count)
				blockPos.add(0, 1, 0), blockPos.add(ledgeGrabRange, 1, 0), blockPos.add(-ledgeGrabRange, 1, 0), blockPos.add(0, 1, -ledgeGrabRange), blockPos.add(0, 1, ledgeGrabRange));

		// Check if any of the surroundings is a valid ledge
		boolean hasValidLedge = playerBlockPositions.stream().anyMatch(this::isValidLedge);
		hasValidLedge = hasValidLedge && isEmpty(blockPos.add(0, -1, 0));

//		for (var playerBlockPos : playerBlockPositions) {
//			if (playerBlockPos == blockPos.add(ledgeGrabRange, 0, 0)) {
//				// Wall is on the left
//			} else if (playerBlockPos == blockPos.add(-ledgeGrabRange, 0, 0)) {
//				// Wall is on the right
//			} else if (playerBlockPos == blockPos.add(0, 0, ledgeGrabRange)) {
//				// Wall is in front
//				var animationContainer = EnhancedMovement.getInstance().enhancedMovement_getModAnimation();
//
//				KeyframeAnimation anim = PlayerAnimationRegistry.getAnimation(new Identifier(EnhancedMovement.MOD_ID, "climb_front"));
//				if (anim == null) return hasValidLedge;
//				animationContainer.setAnimation(new KeyframeAnimationPlayer(anim));
//			}
//		}

		return hasValidLedge;
	}

	public boolean isValidLedge(BlockPos blockPos) {
		// Check if the ledge block isn't empty so it doesn't try to grab onto air
		return !isEmpty(blockPos);
	}

	public boolean isEmpty(BlockPos blockPos) {
		var minecraftClient = MinecraftClient.getInstance();
		var player = minecraftClient.player;
		if (player == null) return true;
		var world = player.getWorld();

		BlockState blockState = player.world.getBlockState(blockPos);
		VoxelShape voxelShape = blockState.getCollisionShape(world, blockPos, ShapeContext.of(player));
		return voxelShape.isEmpty();
	}

	public int getLedgeHeight(BlockPos blockPos) {
		if (isEmpty(blockPos.add(0, 1, 0))) {
			return 1;
		} else if (isEmpty(blockPos.add(0, 2, 0))) {
			return 1;
		}

		return 0;
	}

	public void onLedgeClimb(ClientPlayerEntity player) {
		var animationContainer = ((IAnimatedPlayer) player).enhancedMovement_getModAnimation();
		var anim = PlayerAnimationRegistry.getAnimation(new Identifier(EnhancedMovement.MOD_ID_FOLDERS, CLIMB_FRONT_ANIMATION));
		if (anim == null) {
			EnhancedMovement.LOGGER.error("Animation {} doesn't exist.", CLIMB_FRONT_ANIMATION);
			return;
		}

//		var builder = anim.mutableCopy();
//		builder.isLooped = true;
//		anim = builder.build();

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

	public void stopAnimations(ClientPlayerEntity player) {
		var animationContainer = ((IAnimatedPlayer) player).enhancedMovement_getModAnimation();
		animationContainer.replaceAnimationWithFade(AbstractFadeModifier.standardFadeIn(20, Ease.LINEAR), null);

		EnhancedMovement.LOGGER.info("stopping all animations");
	}
}
