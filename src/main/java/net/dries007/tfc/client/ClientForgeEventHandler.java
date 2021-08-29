/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.client;

import java.lang.reflect.Field;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintCache;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fmllegacy.network.PacketDistributor;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.dries007.tfc.TerraFirmaCraft;
import net.dries007.tfc.client.screen.button.PlayerInventoryTabButton;
import net.dries007.tfc.common.capabilities.food.FoodCapability;
import net.dries007.tfc.common.capabilities.heat.HeatCapability;
import net.dries007.tfc.common.capabilities.size.ItemSizeManager;
import net.dries007.tfc.config.TFCConfig;
import net.dries007.tfc.mixin.client.accessor.ClientLevelAccessor;
import net.dries007.tfc.network.PacketHandler;
import net.dries007.tfc.network.PlaceBlockSpecialPacket;
import net.dries007.tfc.network.SwitchInventoryTabPacket;
import net.dries007.tfc.util.Climate;
import net.dries007.tfc.util.Fuel;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.MetalItem;
import net.dries007.tfc.util.calendar.Calendars;
import net.dries007.tfc.util.calendar.ICalendar;
import net.dries007.tfc.world.chunkdata.ChunkData;

import static net.minecraft.ChatFormatting.*;

public class ClientForgeEventHandler
{
    private static final Field CAP_NBT_FIELD = Helpers.findUnobfField(ItemStack.class, "capNBT");

    public static void init()
    {
        final IEventBus bus = MinecraftForge.EVENT_BUS;

        bus.addListener(ClientForgeEventHandler::onRenderGameOverlayText);
        bus.addListener(ClientForgeEventHandler::onItemTooltip);
        bus.addListener(ClientForgeEventHandler::onInitGuiPost);
        bus.addListener(ClientForgeEventHandler::onClientWorldLoad);
        bus.addListener(ClientForgeEventHandler::onClientTick);
        bus.addListener(ClientForgeEventHandler::onKeyEvent);
        // bus.addListener(ClientForgeEventHandler::onHighlightBlockEvent);
    }

    public static void onRenderGameOverlayText(RenderGameOverlayEvent.Text event)
    {
        Minecraft mc = Minecraft.getInstance();
        List<String> list = event.getRight();
        if (mc.level != null && mc.options.renderDebug && TFCConfig.CLIENT.enableTFCF3Overlays.get())
        {
            //noinspection ConstantConditions
            BlockPos pos = new BlockPos(mc.getCameraEntity().getX(), mc.getCameraEntity().getBoundingBox().minY, mc.getCameraEntity().getZ());
            if (mc.level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4))
            {
                list.add("");
                list.add(AQUA + TerraFirmaCraft.MOD_NAME);

                // Always add calendar info
                list.add(I18n.get("tfc.tooltip.calendar_date") + Calendars.CLIENT.getCalendarTimeAndDate().getString());

                if (TFCConfig.CLIENT.enableDebug.get())
                {
                    list.add(String.format("Ticks = %d, Calendar = %d, Daytime = %d", Calendars.CLIENT.getTicks(), Calendars.CLIENT.getCalendarTicks(), mc.getCameraEntity().level.getDayTime() % ICalendar.TICKS_IN_DAY));
                }

                ChunkData data = ChunkData.get(mc.level, pos);
                if (data.getStatus() == ChunkData.Status.CLIENT)
                {
                    list.add(GRAY + I18n.get("tfc.tooltip.f3_average_temperature", WHITE + String.format("%.1f", data.getAverageTemp(pos))));
                    list.add(GRAY + I18n.get("tfc.tooltip.f3_temperature", WHITE + String.format("%.1f", Climate.calculateTemperature(pos, data.getAverageTemp(pos), Calendars.CLIENT))));
                    list.add(GRAY + I18n.get("tfc.tooltip.f3_rainfall", WHITE + String.format("%.1f", data.getRainfall(pos))));
                    list.add(GRAY + I18n.get("tfc.tooltip.f3_forest_type") + WHITE + I18n.get(Helpers.getEnumTranslationKey(data.getForestType())));
                    list.add(GRAY + I18n.get("tfc.tooltip.f3_forest_properties",
                        WHITE + String.format("%.1f%%", 100 * data.getForestDensity()) + GRAY,
                        WHITE + String.format("%.1f%%", 100 * data.getForestWeirdness()) + GRAY));
                }
                else
                {
                    list.add(GRAY + I18n.get("tfc.tooltip.f3_invalid_chunk_data"));
                }
            }
        }
    }

    public static void onItemTooltip(ItemTooltipEvent event)
    {
        final ItemStack stack = event.getItemStack();
        final List<Component> text = event.getToolTip();
        if (!stack.isEmpty())
        {
            ItemSizeManager.addTooltipInfo(stack, text);
            stack.getCapability(FoodCapability.CAPABILITY).ifPresent(cap -> cap.addTooltipInfo(stack, text));
            stack.getCapability(HeatCapability.CAPABILITY).ifPresent(cap -> cap.addTooltipInfo(stack, text));

            if (event.getFlags().isAdvanced())
            {
                MetalItem.addTooltipInfo(stack, text);
                Fuel.addTooltipInfo(stack, text);
            }

            if (TFCConfig.CLIENT.enableDebug.get())
            {
                final CompoundTag stackTag = stack.getTag();
                if (stackTag != null)
                {
                    text.add(new TextComponent("[Debug] NBT: " + stackTag));
                }

                final CompoundTag capTag = Helpers.uncheck(() -> CAP_NBT_FIELD.get(stack));
                if (capTag != null)
                {
                    text.add(new TextComponent("[Debug] Capability NBT: " + capTag));
                }
            }
        }
    }

    public static void onInitGuiPost(GuiScreenEvent.InitGuiEvent.Post event)
    {
        Player player = Minecraft.getInstance().player;
        if (event.getGui() instanceof InventoryScreen screen && player != null && !player.isCreative())
        {
            int guiLeft = ((InventoryScreen) event.getGui()).getGuiLeft();
            int guiTop = ((InventoryScreen) event.getGui()).getGuiTop();

            event.addWidget(new PlayerInventoryTabButton(guiLeft, guiTop, 176 - 3, 4, 20 + 3, 22, 128 + 20, 0, 1, 3, 0, 0, button -> {}).setRecipeBookCallback(screen));
            event.addWidget(new PlayerInventoryTabButton(guiLeft, guiTop, 176, 27, 20, 22, 128, 0, 1, 3, 32, 0, SwitchInventoryTabPacket.Type.CALENDAR).setRecipeBookCallback(screen));
            event.addWidget(new PlayerInventoryTabButton(guiLeft, guiTop, 176, 50, 20, 22, 128, 0, 1, 3, 64, 0, SwitchInventoryTabPacket.Type.NUTRITION).setRecipeBookCallback(screen));
            event.addWidget(new PlayerInventoryTabButton(guiLeft, guiTop, 176, 73, 20, 22, 128, 0, 1, 3, 96, 0, SwitchInventoryTabPacket.Type.CLIMATE).setRecipeBookCallback(screen));
        }
    }

    public static void onClientWorldLoad(WorldEvent.Load event)
    {
        if (event.getWorld() instanceof final ClientLevel world)
        {
            // Add our custom tints to the color resolver caches
            final Object2ObjectArrayMap<ColorResolver, BlockTintCache> colorCaches = ((ClientLevelAccessor) world).accessor$getTintCaches();

            colorCaches.putIfAbsent(TFCColors.FRESH_WATER, new BlockTintCache());
            colorCaches.putIfAbsent(TFCColors.SALT_WATER, new BlockTintCache());

        }
    }

    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        Level world = Minecraft.getInstance().level;
        if (event.phase == TickEvent.Phase.END && world != null && !Minecraft.getInstance().isPaused())
        {
            Calendars.CLIENT.onClientTick();
            ClimateRenderCache.INSTANCE.onClientTick();
        }
    }

    public static void onKeyEvent(InputEvent.KeyInputEvent event)
    {
        if (TFCKeyBindings.PLACE_BLOCK.isDown())
        {
            PacketHandler.send(PacketDistributor.SERVER.noArg(), new PlaceBlockSpecialPacket());
        }
    }

    /**
     * Handles custom bounding boxes drawing
     * eg: Chisel, Quern handle
     * todo: where?
     */
    public static void onHighlightBlockEvent(){}/*DrawHighlightEvent.HighlightBlock event)
    {
        final ActiveRenderInfo info = event.getInfo();
        final MatrixStack mStack = event.getMatrix();
        final Entity entity = info.getEntity();
        final World world = entity.level;
        final BlockRayTraceResult traceResult = event.getTarget();
        final BlockPos lookingAt = new BlockPos(traceResult.getLocation());

        //noinspection ConstantConditions
        if (lookingAt != null && entity instanceof PlayerEntity)
        {
            PlayerEntity player = (PlayerEntity) entity;
            Block blockAt = world.getBlockState(lookingAt).getBlock();
            //todo: chisel
            if (blockAt instanceof IHighlightHandler) //todo: java 16
            {
                // Pass on to custom implementations
                IHighlightHandler handler = (IHighlightHandler) blockAt;
                if (handler.drawHighlight(world, lookingAt, player, traceResult, mStack, event.getBuffers(), info.getPosition()))
                {
                    // Cancel drawing this block's bounding box
                    event.setCanceled(true);
                }
            }
        }
    }*/
}