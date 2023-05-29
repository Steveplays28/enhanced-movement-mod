package io.github.steveplays28.enhancedmovement.mixin;

import com.mojang.authlib.GameProfile;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import io.github.steveplays28.enhancedmovement.animation.IAnimatedPlayer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractClientPlayerEntity.class)
public class AbstractClientPlayerEntityMixin implements IAnimatedPlayer {
	@Unique
	private final ModifierLayer<IAnimation> animationContainer = new ModifierLayer<>();

	@Override
	public ModifierLayer<IAnimation> enhancedMovement_getModAnimation() {
		return animationContainer;
	}

	@Inject(method = "<init>", at = @At(value = "RETURN"))
	private void init(ClientWorld world, GameProfile profile, CallbackInfo ci) {
		PlayerAnimationAccess.getPlayerAnimLayer((AbstractClientPlayerEntity) (Object) this).addAnimLayer(1000, animationContainer);
	}
}
