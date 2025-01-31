package io.github.steveplays28.enhancedmovement.config;

public class EnhancedMovementConfig {
	public boolean enableDoubleJump = true;
	public boolean enableDash = true;
	public boolean enableLedgeGrab = true;
	public boolean enableAirDash = true;
	public boolean sneakDisablesFeatures;
	public int timeDelayDash = 400;
	public int timeCooldownDash = 1300;
	public double minimumVerticalVelocity = 0.4;
	public double fixedJumpBoost = 0.4;
	public float dashSpeed = 1.7f;
	public float inAirDashSpeed = 1.3f;
	public int ledgeGrabRange = 1;
	public float ledgeGrabHeightPerBlock = 0.2f;

	public float slideHitBoxOffsetY = -0.6f;
	public float slideEyeHeight = 0.7f;
	public float slideHitBoxContractY = 0.7f;
}
