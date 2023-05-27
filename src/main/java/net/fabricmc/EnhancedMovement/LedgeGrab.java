package net.fabricmc.EnhancedMovement;

import net.fabricmc.EnhancedMovement.config.EnhancedMovementConfigLoader;
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

		boolean jumpKeyIsPressed = minecraftClient.options.jumpKey.isPressed();
		if (!jumpKeyIsPressed) return;
		if (!isNearLedge(player.getBlockPos())) return;

		player.setVelocity(player.getVelocity().x, EnhancedMovementConfigLoader.CONFIG.ledgeGrabHeightPerBlock * getLedgeHeight(player.getBlockPos()), player.getVelocity().z);

		// Add hunger
		player.addExhaustion(3000000.0F);
	}

	public boolean isNearLedge(@NotNull BlockPos blockPos) {
		// List of blocks which surrounds the player;
		List<BlockPos> playerBlockPos = List.of(
				blockPos.add(-1, 0, 0),
				blockPos.add(+1, 0, 0),
				blockPos.add(0, 0, 0),
				blockPos.add(0, 0, -1),
				blockPos.add(0, 0, 1)
		);

		// Check if any of the surroundings is a valid ledge.
		boolean hasValidLedge = playerBlockPos
				.stream()
				.anyMatch(this::isValidLedge);

		return hasValidLedge && isEmpty(blockPos.add(0, -1, 0));
	}

	public boolean isValidLedge(BlockPos blockPos) {
		boolean isNotLedgeEmpty = !isEmpty(blockPos);
		boolean isLedgeTopEmpty = isEmpty(blockPos.add(0, 1, 0)) || isEmpty(blockPos.add(0, 2, 0));

		// Check if 'Ledge' not empty so as not to be grabbing on empty blocks.
		// Check if Ledge's  top block is empty.
		return isNotLedgeEmpty && isLedgeTopEmpty;
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
