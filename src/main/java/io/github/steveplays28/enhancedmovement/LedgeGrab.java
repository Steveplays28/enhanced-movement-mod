package io.github.steveplays28.enhancedmovement;

import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import io.github.steveplays28.enhancedmovement.animation.IAnimatedPlayer;
import io.github.steveplays28.enhancedmovement.config.EnhancedMovementConfigLoader;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class LedgeGrab {
	public void tick() {
		var minecraftClient = MinecraftClient.getInstance();
		var player = minecraftClient.player;
		if (player == null) return;
		boolean jumpKeyPressed = minecraftClient.options.jumpKey.isPressed();
		boolean sneakKeyPressed = minecraftClient.options.sneakKey.isPressed();

		if (isNearLedge(player.getBlockPos())) {
			var animationContainer = ((IAnimatedPlayer) player).enhancedMovement_getModAnimation();
			KeyframeAnimation anim = null;

			if (jumpKeyPressed) {
				player.setVelocity(player.getVelocity().x, EnhancedMovementConfigLoader.CONFIG.ledgeGrabHeightPerBlock * getLedgeHeight(player.getBlockPos()), player.getVelocity().z);
				player.addExhaustion(3000000.0F);

				// Climbing animation
				anim = PlayerAnimationRegistry.getAnimation(new Identifier(EnhancedMovement.MOD_ID_FOLDERS, "climb_front"));
			} else if (sneakKeyPressed) {
				player.setVelocity(player.getVelocity().x, 0, player.getVelocity().z);

				// Hanging animation
				anim = PlayerAnimationRegistry.getAnimation(new Identifier(EnhancedMovement.MOD_ID_FOLDERS, "hang"));
			}

			KeyframeAnimationPlayer keyframeAnimationPlayer;
			if (anim == null) {
				keyframeAnimationPlayer = null;
			} else {
				keyframeAnimationPlayer = new KeyframeAnimationPlayer(anim);
			}
			animationContainer.replaceAnimationWithFade(AbstractFadeModifier.standardFadeIn(20, Ease.LINEAR), keyframeAnimationPlayer);
		}
	}

	public boolean isNearLedge(@NotNull BlockPos blockPos) {
		var ledgeGrabRange = EnhancedMovementConfigLoader.CONFIG.ledgeGrabRange;

		// List of blocks which surrounds the player
		List<BlockPos> playerBlockPositions = List.of(
				// Around player block position (diagonals don't count)
				blockPos.add(0, 0, 0),
				blockPos.add(ledgeGrabRange, 0, 0),
				blockPos.add(-ledgeGrabRange, 0, 0),
				blockPos.add(0, 0, ledgeGrabRange),
				blockPos.add(0, 0, -ledgeGrabRange),

				// Around player block position + 1 up (diagonals don't count)
				blockPos.add(0, 1, 0),
				blockPos.add(ledgeGrabRange, 1, 0),
				blockPos.add(-ledgeGrabRange, 1, 0),
				blockPos.add(0, 1, -ledgeGrabRange),
				blockPos.add(0, 1, ledgeGrabRange)
		);

		// Check if any of the surroundings is a valid ledge
		boolean hasValidLedge = playerBlockPositions
				.stream()
				.anyMatch(this::isValidLedge);
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
}
