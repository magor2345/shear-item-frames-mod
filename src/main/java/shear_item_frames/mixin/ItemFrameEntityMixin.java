package shear_item_frames.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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
	@Unique
    public boolean waxed = false;

	@Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
	private void writeNbtMixin(ValueOutput view, CallbackInfo ci) {
		view.putBoolean("Waxed", this.waxed);
	}

	@Inject(method = "readAdditionalSaveData", at = @At("HEAD"))
	private void readNbtMixin(ValueInput view, CallbackInfo ci) {
		this.waxed = view.getBooleanOr("Waxed", false);
	}

	@Inject(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/decoration/ItemFrame;dropItem(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;Z)V"))
	private void hurtServer(ServerLevel world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		ItemFrame self = (ItemFrame)(Object)this;
		this.waxed = false;
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

			if (this.waxed) {
				if (player.isCrouching() && !(player.getMainHandItem().is(Items.AIR) && player.getOffhandItem().is(Items.AIR))) {
					cir.setReturnValue(InteractionResult.FAIL);
					return;
				}

				ServerLevel level = (ServerLevel) t.level();

				BlockPos behind = t.blockPosition()
						.relative(t.getDirection().getOpposite());

				BlockState state = level.getBlockState(behind);

				Vec3 eyePos = player.getEyePosition();
				Vec3 lookVec = player.getLookAngle().scale(5.0);

				BlockHitResult blockHit = level.clip(
						new ClipContext(
								eyePos,
								eyePos.add(lookVec),
								ClipContext.Block.COLLIDER,
								ClipContext.Fluid.NONE,
								player
						)
				);

				if (blockHit.getBlockPos().equals(behind)) {
					player.openMenu(state.getMenuProvider(level, behind));
				}

				cir.setReturnValue(InteractionResult.CONSUME);
				return;
			}

			if (itemStack.is(Items.HONEYCOMB)) {
				this.waxed = true;
				itemStack.shrink(1);
				t.playSound(SoundEvents.HONEYCOMB_WAX_ON, 1.0f, 1.0f);

				Direction facing = t.getDirection();
				double offset = 0.09375;

				double x = t.position().x + facing.getStepX() * offset;
				double y = t.position().y + facing.getStepY() * offset;
				double z = t.position().z + facing.getStepZ() * offset;

				ServerLevel serverLevel = (ServerLevel) t.level();
				AABB box = t.getBoundingBox();

				serverLevel.players().forEach(p -> serverLevel.sendParticles(
                        p,
                        ParticleTypes.WAX_ON,
                        false, false,
                        x, y, z,
                        10,
                        box.getXsize() * 0.4, box.getYsize() * 0.4, box.getZsize() * 0.4,
                        0.02
                ));

				t.level().gameEvent(null, GameEvent.BLOCK_CHANGE, t.getPos());
				cir.setReturnValue(InteractionResult.SUCCESS);
			}
		}
	}
}