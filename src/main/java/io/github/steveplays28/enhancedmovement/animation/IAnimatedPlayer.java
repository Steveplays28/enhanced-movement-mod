package io.github.steveplays28.enhancedmovement.animation;

import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;

public interface IAnimatedPlayer {
	/**
	 * @return Mod animation container
	 */
	ModifierLayer<IAnimation> enhancedMovement_getModAnimation();
}
