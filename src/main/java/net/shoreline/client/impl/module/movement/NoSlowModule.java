package net.shoreline.client.impl.module.movement;

import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.event.EventStage;
import net.shoreline.client.api.event.listener.EventListener;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.impl.event.block.BlockSlipperinessEvent;
import net.shoreline.client.impl.event.block.SteppedOnSlimeBlockEvent;
import net.shoreline.client.impl.event.entity.VelocityMultiplierEvent;
import net.shoreline.client.impl.event.network.GameJoinEvent;
import net.shoreline.client.impl.event.network.MovementSlowdownEvent;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.impl.event.network.SetCurrentHandEvent;
import net.shoreline.client.init.Managers;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.client.input.Input;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.shoreline.client.util.Globals;
import org.lwjgl.glfw.GLFW;

/**
 *
 *
 * @author linus
 * @since 1.0
 */
public class NoSlowModule extends ToggleModule
{
    //
    Config<Boolean> strictConfig = new BooleanConfig("Strict", "Strict NCP " +
            "bypass for ground slowdowns", false);
    Config<Boolean> airStrictConfig = new BooleanConfig("AirStrict",  "Strict" +
            " NCP bypass for air slowdowns", false);
    Config<Boolean> grimConfig = new BooleanConfig("Grim", "Strict" +
            " Grim bypass for slowdown", false);
    Config<Boolean> inventoryMoveConfig = new BooleanConfig("InventoryMove",
            "Allows the player to move while in inventories or screens", false);
    Config<Boolean> arrowMoveConfig = new BooleanConfig("ArrowMove", "Allows " +
            "the player to look while in inventories or screens by using the " +
            "arrow keys", false);
    Config<Boolean> itemsConfig = new BooleanConfig("Items", "Removes " +
            "the slowdown effect caused by using items", true);
    Config<Boolean> shieldsConfig = new BooleanConfig("Shields", "Removes the" +
            " slowdown effect caused by shields", true);
    Config<Boolean> websConfig = new BooleanConfig("Webs", "Removes the " +
            "slowdown caused when moving through webs", false);
    Config<Float> webSpeedConfig = new NumberConfig<>("WebSpeed", "Speed to " +
            "fall through webs", 0.0f, 3.5f, 20.0f, () -> websConfig.getValue());
    Config<Boolean> soulsandConfig = new BooleanConfig("SoulSand", "Removes " +
            "the slowdown effect caused by walking over SoulSand blocks", false);
    Config<Boolean> honeyblockConfig = new BooleanConfig("HoneyBlock", "Removes " +
            "the slowdown effect caused by walking over Honey blocks", false);
    Config<Boolean> slimeblockConfig = new BooleanConfig("SlimeBlock",  "Removes " +
            "the slowdown effect caused by walking over Slime blocks", false);
    //
    private boolean sneaking;
    //
    private static KeyBinding[] MOVE_KEYBINDS;
    
    /**
     *
     */
    public NoSlowModule()
    {
        super("NoSlow", "Prevents items from slowing down player",
                ModuleCategory.MOVEMENT);
    }

    /**
     *
     *
     */
    @Override
    public void onEnable()
    {
        if (MOVE_KEYBINDS == null)
        {
            MOVE_KEYBINDS = new KeyBinding[]
                    {
                            Globals.mc.options.forwardKey,
                            Globals.mc.options.backKey,
                            Globals.mc.options.rightKey,
                            Globals.mc.options.leftKey
                    };
        }
    }
    
    /**
     *
     *
     */
    @Override
    public void onDisable()
    {
        if (airStrictConfig.getValue() && sneaking)
        {
            Managers.NETWORK.sendPacket(new ClientCommandC2SPacket(Globals.mc.player,
                    ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
        }
        sneaking = false;
        Managers.TICK.setClientTick(1.0f);
    }

    /**
     *
     * @param event
     */
    @EventListener
    public void onGameJoin(GameJoinEvent event)
    {
        onEnable();
    }
    
    /**
     *
     *
     * @param event
     */
    @EventListener
    public void onSetCurrentHand(SetCurrentHandEvent event)
    {
        if (airStrictConfig.getValue() && !sneaking && checkSlowed())
        {
            sneaking = true;
            Managers.NETWORK.sendPacket(new ClientCommandC2SPacket(Globals.mc.player,
                    ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
        }
    }
    
    /**
     *
     *
     * @param event
     */
    @EventListener
    public void onTick(TickEvent event)
    {
        if (event.getStage() == EventStage.PRE)
        {
            if (airStrictConfig.getValue() && sneaking
                    && !Globals.mc.player.isUsingItem())
            {
                sneaking = false;
                Managers.NETWORK.sendPacket(new ClientCommandC2SPacket(Globals.mc.player,
                        ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
            }
            if (checkSlowed())
            {
                // Old NCP
                // Managers.NETWORK.sendSequencedPacket(id ->
                //        new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND,
                //                new BlockHitResult(mc.player.getPos(),
                //                Direction.UP, BlockPos.ORIGIN, false), id));
            }
            if (inventoryMoveConfig.getValue() && checkScreen()
                    && MOVE_KEYBINDS != null)
            {
                final long handle = Globals.mc.getWindow().getHandle();
                for (KeyBinding binding : MOVE_KEYBINDS)
                {
                    binding.setPressed(InputUtil.isKeyPressed(handle,
                            binding.getDefaultKey().getCode()));
                }
                if (arrowMoveConfig.getValue())
                {
                    float yaw = Globals.mc.player.getYaw();
                    float pitch = Globals.mc.player.getPitch();
                    if (InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_UP))
                    {
                        pitch -= 3.0f;
                    }
                    else if (InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_DOWN))
                    {
                       pitch += 3.0f;
                    }
                    else if (InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_LEFT))
                    {
                        yaw -= 3.0f;
                    }
                    else if (InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_RIGHT))
                    {
                        yaw += 3.0f;
                    }
                    Globals.mc.player.setYaw(yaw);
                    Globals.mc.player.setPitch(MathHelper.clamp(pitch, -90.0f, 90.0f));
                }
            }
            final BlockState state = Globals.mc.world.getBlockState(BlockPos.ofFloored(Globals.mc.player.getPos()));
            if (state.getBlock() == Blocks.COBWEB && websConfig.getValue())
            {
                if (Globals.mc.player.isOnGround())
                {
                    Managers.TICK.setClientTick(1.0f);
                }
                else
                {
                    Managers.TICK.setClientTick(webSpeedConfig.getValue() / 2.0f);
                }
            }
        }
    }
    
    /**
     *
     *
     * @param event
     */
    @EventListener
    public void onMovementSlowdown(MovementSlowdownEvent event)
    {
        Input input = event.getInput();
        if (checkSlowed())
        {
            input.movementForward *= 5.0f;
            input.movementSideways *= 5.0f;
        }
    }
    
    /**
     *
     *
     * @param event
     */
    @EventListener
    public void onVelocityMultiplier(VelocityMultiplierEvent event)
    {
        if (event.getBlock() == Blocks.SOUL_SAND && soulsandConfig.getValue()
                || event.getBlock() == Blocks.HONEY_BLOCK && honeyblockConfig.getValue())
        {
            event.cancel();
        }
    }
    
    /**
     *
     *
     * @param event
     */
    @EventListener
    public void onSteppedOnSlimeBlock(SteppedOnSlimeBlockEvent event)
    {
        if (slimeblockConfig.getValue())
        {
            event.cancel();
        }
    }
    
    /**
     *
     *
     * @param event
     */
    @EventListener
    public void onBlockSlipperiness(BlockSlipperinessEvent event)
    {
        if (event.getBlock() == Blocks.SLIME_BLOCK
                && slimeblockConfig.getValue())
        {
            event.cancel();
            event.setSlipperiness(0.6f);
        }
    }
    
    /**
     *
     *
     * @param event
     */
    @EventListener
    public void onPacketOutbound(PacketEvent.Outbound event)
    {
        if (Globals.mc.player == null || Globals.mc.world == null || Globals.mc.isInSingleplayer())
        {
            return;
        }
        if (event.getPacket() instanceof PlayerMoveC2SPacket packet)
        {
            if (strictConfig.getValue() && checkSlowed()
                    && packet.changesPosition())
            {
                Managers.NETWORK.sendPacket(new UpdateSelectedSlotC2SPacket(0));
                // Managers.NETWORK.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.OFF_HAND, id));
                Managers.NETWORK.sendPacket(new UpdateSelectedSlotC2SPacket(Globals.mc.player.getInventory().selectedSlot));
            }
        }
        else if (event.getPacket() instanceof ClickSlotC2SPacket
                && strictConfig.getValue())
        {
            if (Globals.mc.player.isUsingItem())
            {
                Globals.mc.player.stopUsingItem();
            }
            if (sneaking || Managers.POSITION.isSneaking())
            {
                Managers.NETWORK.sendPacket(new ClientCommandC2SPacket(Globals.mc.player,
                        ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
            }
            if (Managers.POSITION.isSprinting())
            {
                Managers.NETWORK.sendPacket(new ClientCommandC2SPacket(Globals.mc.player,
                        ClientCommandC2SPacket.Mode.STOP_SPRINTING));
            }
        }
    }
    
    /**
     *
     *
     * @return
     */
    public boolean checkSlowed()
    {
        return !Globals.mc.player.isRiding() && !Globals.mc.player.isFallFlying()
                && (Globals.mc.player.isUsingItem() && itemsConfig.getValue())
                || (Globals.mc.player.isBlocking() && shieldsConfig.getValue());
    }
    
    /**
     *
     *
     * @return
     */
    public boolean checkScreen()
    {
        return Globals.mc.currentScreen != null
                && !(Globals.mc.currentScreen instanceof ChatScreen
                || Globals.mc.currentScreen instanceof SignEditScreen
                || Globals.mc.currentScreen instanceof DeathScreen);
    }
}