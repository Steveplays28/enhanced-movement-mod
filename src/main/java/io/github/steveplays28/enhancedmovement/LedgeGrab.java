package io.github.steveplays28.enhancedmovement;

import io.github.steveplays28.enhancedmovement.config.EnhancedMovementConfigLoader;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
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
			if (jumpKeyPressed) {
				player.setVelocity(player.getVelocity().x, EnhancedMovementConfigLoader.CONFIG.ledgeGrabHeightPerBlock * getLedgeHeight(player.getBlockPos()), player.getVelocity().z);
				player.addExhaustion(3000000.0F);
			} else if (sneakKeyPressed) {
				player.setVelocity(player.getVelocity().x, 0, player.getVelocity().z);
			}
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

//		for (var playerBlockPos : playerBlockPositions) {
//			if (playerBlockPos)
//		}

		return hasValidLedge && isEmpty(blockPos.add(0, -1, 0));
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
