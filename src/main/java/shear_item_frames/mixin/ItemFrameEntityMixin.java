package shear_item_frames.mixin;

import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

@Mixin(ItemFrame.class)
public class ItemFrameEntityMixin {
	private static final EntityDataAccessor<Boolean> DATA_WAXED =
			SynchedEntityData.defineId(ItemFrame.class, EntityDataSerializers.BOOLEAN);

	@Inject(method = "defineSynchedData", at = @At("RETURN"))
	private void defineWaxed(SynchedEntityData.Builder builder, CallbackInfo ci) {
		builder.define(DATA_WAXED, false);
	}

	private boolean isWaxed() {
		return ((ItemFrame)(Object)this).getEntityData().get(DATA_WAXED);
	}

	private void setWaxed(boolean value) {
		((ItemFrame)(Object)this).getEntityData().set(DATA_WAXED, value);
	}

	@Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
	private void writeNbtMixin(ValueOutput view, CallbackInfo ci) {
		view.putBoolean("Waxed", this.isWaxed());
	}

	@Inject(method = "readAdditionalSaveData", at = @At("HEAD"))
	private void readNbtMixin(ValueInput view, CallbackInfo ci) {
		this.setWaxed(view.getBooleanOr("Waxed", false));
	}

	@Inject(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/decoration/ItemFrame;dropItem(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;Z)V"), cancellable = true)
	private void hurtServer(ServerLevel world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		ItemFrame self = (ItemFrame)(Object)this;
		this.setWaxed(false);
		self.setInvisible(false);
	}

	@Inject(method = "interact", at = @At("HEAD"), cancellable = true)
	private void onInteract(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
		ItemStack itemStack = player.getItemInHand(hand);
		ItemFrame t = ((ItemFrame) (Object) this);
		boolean itemFrameEmpty = t.getItem().isEmpty();

		if (!itemFrameEmpty) {
			if (itemStack.is(Items.SHEARS) && !t.isInvisible()) {
				t.setInvisible(true);
				t.playSound(SoundEvents.SHEARS_SNIP, 1.0f, 1.0f);
				t.gameEvent(GameEvent.SHEAR, player);
				itemStack.hurtAndBreak(1, player, hand);
				cir.setReturnValue(InteractionResult.SUCCESS);
				return;
			}

			if (this.isWaxed()) {
				cir.setReturnValue(InteractionResult.PASS);
				return;
			}

			if (itemStack.is(Items.HONEYCOMB)) {
				this.setWaxed(true);
				itemStack.shrink(1);
				t.playSound(SoundEvents.HONEYCOMB_WAX_ON, 1.0f, 1.0f);
				RandomSource random = t.level().random; // or new Random()

				AABB box = t.getBoundingBox();

				Direction facing = t.getDirection();
				double offset = 0.09375;

				for (int i = 0; i < 10; i++) {
					double x = box.minX + random.nextDouble() * (box.maxX - box.minX) + facing.getStepX() * offset;
					double y = box.minY + random.nextDouble() * (box.maxY - box.minY) + facing.getStepY() * offset;
					double z = box.minZ + random.nextDouble() * (box.maxZ - box.minZ) + facing.getStepZ() * offset;

					t.level().addParticle(ParticleTypes.WAX_ON, x, y, z, 0, 0.02, 0);
				}

				t.level().gameEvent((Entity) null, GameEvent.BLOCK_CHANGE, t.getPos());
				cir.setReturnValue(InteractionResult.SUCCESS);
			}
		}
	}
}