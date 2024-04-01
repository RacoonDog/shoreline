package net.shoreline.client.mixin.entity;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.shoreline.client.Shoreline;
import net.shoreline.client.impl.event.entity.*;
import net.shoreline.client.impl.event.entity.decoration.TeamColorEvent;
import net.shoreline.client.impl.event.entity.player.PushEntityEvent;
import net.shoreline.client.util.Globals;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author linus
 * @see Entity
 * @since 1.0
 */
@Mixin(Entity.class)
public abstract class MixinEntity implements Globals {

    /**
     * @param movementInput
     * @param speed
     * @param yaw
     * @return
     */
    @Shadow
    private static Vec3d movementInputToVelocity(Vec3d movementInput, float speed, float yaw) {
        return null;
    }

    @Shadow
    public abstract Box getBoundingBox();

    @Shadow
    public abstract Vec3d getVelocity();

    @Shadow
    public abstract void setVelocity(Vec3d velocity);

    @Shadow
    public abstract boolean isSprinting();

    @Shadow
    public boolean velocityDirty;

    /**
     *
     * @param tickDelta
     * @param info
     * @author xgraza
     */
    @Inject(method = "getRotationVec", at = @At("RETURN"), cancellable = true)
    public void hookGetCameraPosVec(final float tickDelta, final CallbackInfoReturnable<Vec3d> info) {
        final EntityRotationVectorEvent event = new EntityRotationVectorEvent(
                tickDelta, (Entity) (Object) this, info.getReturnValue());
        Shoreline.EVENT_HANDLER.dispatch(event);
        info.setReturnValue(event.getPosition());
    }

    /**
     * @param movement
     * @param cir
     */
    @Inject(method = "adjustMovementForCollisions(Lnet/minecraft/util/math/Vec3d;)Lnet/minecraft/util/math/Vec3d;", at = @At(value = "HEAD"))
    public void hookMove(Vec3d movement, CallbackInfoReturnable<Vec3d> cir) {
        if ((Object) this != mc.player) {
            return;
        }
        // StepEvent stepEvent = new StepEvent(cir.getReturnValue().y - movement.y);
        // Shoreline.EVENT_HANDLER.dispatch(stepEvent);
    }

    /**
     * @param state
     * @param multiplier
     * @param ci
     */
    @Inject(method = "slowMovement", at = @At(value = "HEAD"), cancellable = true)
    private void hookSlowMovement(BlockState state, Vec3d multiplier, CallbackInfo ci) {
        if ((Object) this != mc.player) {
            return;
        }
        SlowMovementEvent slowMovementEvent = new SlowMovementEvent(state);
        Shoreline.EVENT_HANDLER.dispatch(slowMovementEvent);
        if (slowMovementEvent.isCanceled()) {
            ci.cancel();
        }
    }

    /**
     * @param instance
     * @return
     */
    @Redirect(method = "getVelocityMultiplier", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/block/BlockState;getBlock()" +
                    "Lnet/minecraft/ block/Block;"))
    private Block hookGetVelocityMultiplier(BlockState instance) {
        if ((Object) this != mc.player) {
            return instance.getBlock();
        }
        VelocityMultiplierEvent velocityMultiplierEvent =
                new VelocityMultiplierEvent(instance);
        Shoreline.EVENT_HANDLER.dispatch(velocityMultiplierEvent);
        if (velocityMultiplierEvent.isCanceled()) {
            return Blocks.DIRT;
        }
        return instance.getBlock();
    }

    /**
     * @param speed
     * @param movementInput
     * @param ci
     */
    @Inject(method = "updateVelocity", at = @At(value = "HEAD"), cancellable = true)
    private void hookUpdateVelocity(float speed, Vec3d movementInput, CallbackInfo ci) {
        if ((Object) this == mc.player) {
            UpdateVelocityEvent updateVelocityEvent = new UpdateVelocityEvent(movementInput, speed, mc.player.getYaw(), movementInputToVelocity(movementInput, speed, mc.player.getYaw()));
            Shoreline.EVENT_HANDLER.dispatch(updateVelocityEvent);
            if (updateVelocityEvent.isCanceled()) {
                ci.cancel();
                mc.player.setVelocity(mc.player.getVelocity().add(updateVelocityEvent.getVelocity()));
            }
        }
    }

    /**
     * @param entity
     * @param ci
     */
    @Inject(method = "pushAwayFrom", at = @At(value = "HEAD"), cancellable = true)
    private void hookPushAwayFrom(Entity entity, CallbackInfo ci) {
        PushEntityEvent pushEntityEvent = new PushEntityEvent();
        Shoreline.EVENT_HANDLER.dispatch(pushEntityEvent);
        if (pushEntityEvent.isCanceled()) {
            ci.cancel();
        }
    }

    /**
     * @param cir
     */
    @Inject(method = "getTeamColorValue", at = @At(value = "HEAD"),
            cancellable = true)
    private void hookGetTeamColorValue(CallbackInfoReturnable<Integer> cir) {
        TeamColorEvent teamColorEvent =
                new TeamColorEvent((Entity) (Object) this);
        Shoreline.EVENT_HANDLER.dispatch(teamColorEvent);
        if (teamColorEvent.isCanceled()) {
            cir.setReturnValue(teamColorEvent.getColor());
            cir.cancel();
        }
    }
}
