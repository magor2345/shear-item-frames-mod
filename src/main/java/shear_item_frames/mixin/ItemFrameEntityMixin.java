package shear_item_frames.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;
import net.minecraft.world.event.GameEvent;

@Mixin(ItemFrameEntity.class)
public class ItemFrameEntityMixin {
	private boolean waxed = false;

	// Inject at head
	@Inject(method = "damage", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/decoration/ItemFrameEntity;dropHeldStack(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/Entity;Z)V"), cancellable = true)
	private void onDamage(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		ItemFrameEntity t = ((ItemFrameEntity) (Object) this);

		if (t.isInvisible()) {
			t.setInvisible(false);
		}

		if (waxed) {
			waxed = false;
		}
	}

	@Inject(method = "interact", at = @At("HEAD"), cancellable = true)
	private void onInteract(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
		ItemStack itemStack = player.getStackInHand(hand);
		ItemFrameEntity t = ((ItemFrameEntity) (Object) this);
		boolean itemFrameEmpty = ((ItemFrameEntity) (Object) this).getHeldItemStack().isEmpty();

		if (!itemFrameEmpty) {
			if (waxed) {
				cir.setReturnValue(ActionResult.SUCCESS);
				return;
			}

			if (!waxed && itemStack.isOf(Items.HONEYCOMB)) {
				waxed = true;
				itemStack.decrement(1);
				t.playSound(SoundEvents.ITEM_HONEYCOMB_WAX_ON, 1.0f, 1.0f);
				t.getEntityWorld().syncWorldEvent((Entity) null, WorldEvents.BLOCK_WAXED, t.getBlockPos(), 0);
				cir.setReturnValue(ActionResult.SUCCESS);
				return;
			}

			if (!t.isInvisible() && itemStack.isOf(Items.SHEARS)) {
				t.setInvisible(true);
				t.playSound(SoundEvents.ITEM_SHEARS_SNIP, 1.0f, 1.0f);
				t.emitGameEvent(GameEvent.SHEAR, player);
				itemStack.damage(1, player, hand);
				cir.setReturnValue(ActionResult.SUCCESS);
				return;
			}

		}
	}
}