package io.github.steveplays28.enhancedmovement;

import io.github.steveplays28.enhancedmovement.config.EnhancedMovementConfigLoader;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.player.PlayerEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class EnhancedMovement implements ModInitializer {
	public static final String MOD_ID = "enhanced-movement";
	public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

	private static EnhancedMovement instance;
	private final MinecraftClient client = MinecraftClient.getInstance();

	// Jump
	private boolean jumpKeyPressed = false;
	private boolean jumpKeyReleased = false;
	private boolean midAirJumpPerformed = false;
	private boolean isInAir = false;
	private boolean isJumping = false;
	private long jumpStartTime = 0;

	// Movement
	private boolean forwardPressed = false;
	private boolean backPressed = false;
	private boolean leftPressed = false;
	private boolean rightPressed = false;

	private AtomicLong forwardPressTime = new AtomicLong(0);
	private AtomicLong backPressTime = new AtomicLong(0);
	private AtomicLong leftPressTime = new AtomicLong(0);
	private AtomicLong rightPressTime = new AtomicLong(0);

	private AtomicLong globalCooldownTime = new AtomicLong(0);
	private AtomicLong forwardCooldownTime = new AtomicLong(0);
	private AtomicLong backCooldownTime = new AtomicLong(0);
	private AtomicLong leftCooldownTime = new AtomicLong(0);
	private AtomicLong rightCooldownTime = new AtomicLong(0);

	private AtomicBoolean forwardKeyReleased = new AtomicBoolean(false);
	private AtomicBoolean backKeyReleased = new AtomicBoolean(false);
	private AtomicBoolean leftKeyReleased = new AtomicBoolean(false);
	private AtomicBoolean rightKeyReleased = new AtomicBoolean(false);

	private final AtomicBoolean forwardPressHandled = new AtomicBoolean(false);
	private final AtomicBoolean backPressHandled = new AtomicBoolean(false);
	private final AtomicBoolean leftPressHandled = new AtomicBoolean(false);
	private final AtomicBoolean rightPressHandled = new AtomicBoolean(false);
	public static LedgeGrab ledgeGrab = new LedgeGrab();

	@Override
	public void onInitialize() {
		EnhancedMovementConfigLoader.CONFIG = EnhancedMovementConfigLoader.load();
		instance = this;
		NetworkHandler.registerReceivers();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player != null) {
				boolean isSneaking = client.options.sneakKey.isPressed();
				if (isSneaking && EnhancedMovementConfigLoader.CONFIG.sneakDisablesFeatures) {
					return;
				}

				if (EnhancedMovementConfigLoader.CONFIG.enableDash) {
					KeyBinding forwardKey = client.options.forwardKey;
					KeyBinding backKey = client.options.backKey;
					KeyBinding leftKey = client.options.leftKey;
					KeyBinding rightKey = client.options.rightKey;

					// Movement Bools
					forwardPressed = forwardKey.isPressed();
					leftPressed = leftKey.isPressed();
					rightPressed = rightKey.isPressed();
					backPressed = backKey.isPressed();

					handleDash(forwardKey, forwardPressed, forwardPressTime, forwardCooldownTime, forwardKeyReleased, globalCooldownTime, forwardPressHandled);
					handleDash(backKey, backPressed, backPressTime, backCooldownTime, backKeyReleased, globalCooldownTime, backPressHandled);
					handleDash(leftKey, leftPressed, leftPressTime, leftCooldownTime, leftKeyReleased, globalCooldownTime, leftPressHandled);
					handleDash(rightKey, rightPressed, rightPressTime, rightCooldownTime, rightKeyReleased, globalCooldownTime, rightPressHandled);
				}

				if (EnhancedMovementConfigLoader.CONFIG.enableDoubleJump) {
					boolean onGround = client.player.isOnGround();
					jumpKeyPressed = client.options.jumpKey.isPressed();

					// Handle the initial jump
					if (jumpKeyPressed && !isInAir) {
						jumpStartTime = System.currentTimeMillis();
						isInAir = true;
						isJumping = true;
					}

					// Handle mid-air jump
					if (isJumping && isInAir) {
						long timeSinceJumpStart = System.currentTimeMillis() - jumpStartTime;

						if (timeSinceJumpStart >= 250 && jumpKeyPressed && jumpKeyReleased && !midAirJumpPerformed && !onGround) {
							performMidAirJump(client.player);
							NetworkHandler.sendDoubleJumpPacket(client.player);
							midAirJumpPerformed = true;
						}

						if (!jumpKeyPressed) {
							jumpKeyReleased = true;
						}
					}

					// Reset fall distance
					if (isInAir && jumpKeyReleased && midAirJumpPerformed) {
						client.player.fallDistance = 0;
					}

					// Reset all variables when the player is on the ground
					if (onGround) {
						resetJumpState();
					}
				}

				if (EnhancedMovementConfigLoader.CONFIG.enableLedgeGrab) {
					ledgeGrab.tick();
				}
			}
		});
	}

	public void performMidAirJump(PlayerEntity player) {
		double currentVerticalVelocity = player.getVelocity().y;

		currentVerticalVelocity = Math.max(currentVerticalVelocity, EnhancedMovementConfigLoader.CONFIG.minimumVerticalVelocity);
		double newVerticalVelocity = currentVerticalVelocity + EnhancedMovementConfigLoader.CONFIG.fixedJumpBoost;

		// Set the new velocity for the player
		player.setVelocity(player.getVelocity().add(0, newVerticalVelocity, 0));
		player.fallDistance = 0;
	}

	private void handleDash(KeyBinding key, boolean isPressed, AtomicLong pressTime, AtomicLong cooldownTime, AtomicBoolean keyReleased, AtomicLong globalCooldownTime, AtomicBoolean pressHandled) {
		// Check if the player has a UI up
		if (client.currentScreen != null) {
			return;
		}

		long currentTime = System.currentTimeMillis();

		// Check if the player has performed an action that is not in the direction of the dash
		boolean resetState = false;
		if (key == client.options.forwardKey) {
			resetState = !forwardPressed || leftPressed || rightPressed || backPressed;
		} else if (key == client.options.backKey) {
			resetState = !backPressed || leftPressed || rightPressed || forwardPressed;
		} else if (key == client.options.leftKey) {
			resetState = !leftPressed || forwardPressed || backPressed || rightPressed;
		} else if (key == client.options.rightKey) {
			resetState = !rightPressed || forwardPressed || backPressed || leftPressed;
		}

		if (resetState) {
			forwardPressHandled.set(false);
			backPressHandled.set(false);
			leftPressHandled.set(false);
			rightPressHandled.set(false);
			keyReleased.set(false);
		}

		if (currentTime - globalCooldownTime.get() > EnhancedMovementConfigLoader.CONFIG.timeCooldownDash || globalCooldownTime.get() == 0) {
			if (isPressed) {
				if (!pressHandled.get()) {
					if (keyReleased.get()) {
						if (currentTime - pressTime.get() < EnhancedMovementConfigLoader.CONFIG.timeDelayDash) {
							performDash(key);
							cooldownTime.set(currentTime);
							globalCooldownTime.set(currentTime);
							keyReleased.set(false);
						} else if (currentTime - pressTime.get() >= EnhancedMovementConfigLoader.CONFIG.timeDelayDash) {
							pressTime.set(currentTime);
							keyReleased.set(false);
						}
					} else {
						pressTime.set(currentTime);
					}
					pressHandled.set(true);
				}
			} else {
				if (currentTime - pressTime.get() < EnhancedMovementConfigLoader.CONFIG.timeDelayDash) {
					keyReleased.set(true);
				}
				pressHandled.set(false);
			}
		}
	}

	private void performDash(KeyBinding key) {
		if (isInAir && !EnhancedMovementConfigLoader.CONFIG.enableAirDash) {
			return;
		}

		if (client.player != null) {
			float dashSpeed;
			float upwardsLift = 0f;

			if (isInAir) {
				dashSpeed = EnhancedMovementConfigLoader.CONFIG.inAirDashSpeed;
			} else {
				dashSpeed = EnhancedMovementConfigLoader.CONFIG.dashSpeed;
				upwardsLift = 0.3f;
			}

			double playerYaw = Math.toRadians(client.player.getYaw());
			double offsetX = -Math.sin(playerYaw) * dashSpeed;
			double offsetZ = Math.cos(playerYaw) * dashSpeed;

			// TODO: Simplify
			if (key == client.options.forwardKey) {
				client.player.setVelocity(client.player.getVelocity().add(offsetX, upwardsLift, offsetZ));
				NetworkHandler.sendDashPacket(client.player, offsetX, upwardsLift, offsetZ);
			} else if (key == client.options.backKey) {
				client.player.setVelocity(client.player.getVelocity().subtract(offsetX, upwardsLift, offsetZ));
				NetworkHandler.sendDashPacket(client.player, -offsetX, upwardsLift, -offsetZ);
			} else if (key == client.options.leftKey) {
				double leftOffsetX = Math.cos(playerYaw) * EnhancedMovementConfigLoader.CONFIG.dashSpeed;
				double leftOffsetZ = Math.sin(playerYaw) * EnhancedMovementConfigLoader.CONFIG.dashSpeed;
				client.player.setVelocity(client.player.getVelocity().add(leftOffsetX, upwardsLift, leftOffsetZ));
				NetworkHandler.sendDashPacket(client.player, leftOffsetX, upwardsLift, leftOffsetZ);
			} else if (key == client.options.rightKey) {
				double rightOffsetX = -Math.cos(playerYaw) * EnhancedMovementConfigLoader.CONFIG.dashSpeed;
				double rightOffsetZ = -Math.sin(playerYaw) * EnhancedMovementConfigLoader.CONFIG.dashSpeed;
				client.player.setVelocity(client.player.getVelocity().add(rightOffsetX, upwardsLift, rightOffsetZ));
				NetworkHandler.sendDashPacket(client.player, rightOffsetX, upwardsLift, rightOffsetZ);
			}
		}
	}

	public static EnhancedMovement getInstance() {
		return instance;
	}

	public boolean hasPerformedMidAirJump() {
		return midAirJumpPerformed;
	}

	private void resetJumpState() {
		isInAir = false;
		midAirJumpPerformed = false;
		isJumping = false;
		jumpKeyReleased = false;
		jumpKeyPressed = false;
		jumpStartTime = 0;
	}
}
