package com.example.comet;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class CometMod implements ClientModInitializer {
    public static boolean triggerBot = true, esp = true, autoTotem = true, velocity = true;
    private final Random random = new Random();
    private final ConcurrentHashMap<BlockPos, String> blocks = new ConcurrentHashMap<>();
    private long lastAtk = 0, lastTotem = 0;

    @Override
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(new KeyMapping("key.comet.esp", GLFW.GLFW_KEY_Y, "Comet"));

        ClientChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
            CompletableFuture.runAsync(() -> {
                chunk.getBlockEntitiesPos().forEach(pos -> {
                    BlockEntity be = chunk.getBlockEntity(pos);
                    if (be instanceof TrialSpawnerBlockEntity) blocks.put(pos, "TRIAL");
                    else if (be instanceof ShulkerBoxBlockEntity) blocks.put(pos, "SHULKER");
                    else if (be instanceof EnderChestBlockEntity) blocks.put(pos, "E-CHEST");
                    else if (be instanceof ChestBlockEntity) blocks.put(pos, "CHEST");
                    else if (be instanceof SpawnerBlockEntity) blocks.put(pos, "SPAWNER");
                });
            });
        });

        WorldRenderEvents.AFTER_ENTITIES.register(this::render);
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(Minecraft mc) {
        if (mc.player == null) return;
        if (triggerBot && mc.hitResult instanceof EntityHitResult e && mc.player.distanceTo(e.getEntity()) <= 2.9) {
            if (System.currentTimeMillis() - lastAtk > 120 + random.nextInt(40)) {
                mc.gameMode.attack(mc.player, e.getEntity());
                mc.player.swing(InteractionHand.MAIN_HAND);
                lastAtk = System.currentTimeMillis();
            }
        }
        if (autoTotem && !mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
            if (System.currentTimeMillis() - lastTotem > 200) {
                for (int i = 0; i < 36; i++) {
                    if (mc.player.getInventory().getItem(i).is(Items.TOTEM_OF_UNDYING)) {
                        int slot = i < 9 ? i + 36 : i;
                        mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, slot, 0, net.minecraft.world.inventory.ClickType.PICKUP, mc.player);
                        mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, 45, 0, net.minecraft.world.inventory.ClickType.PICKUP, mc.player);
                        mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, slot, 0, net.minecraft.world.inventory.ClickType.PICKUP, mc.player);
                        lastTotem = System.currentTimeMillis();
                        break;
                    }
                }
            }
        }
        if (velocity && mc.player.hurtTime > 0) mc.player.setDeltaMovement(mc.player.getDeltaMovement().multiply(0.92, 1.0, 0.92));
    }

    private void render(WorldRenderContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !esp) return;
        mc.level.entitiesForRendering().forEach(e -> { if (e instanceof Player && e != mc.player) e.setGlowingTag(true); });
        ctx.matrixStack().push();
        Vec3 c = ctx.camera().getPosition();
        ctx.matrixStack().translate(-c.x, -c.y, -c.z);
        blocks.forEach((p, t) -> {
            if (mc.player.blockPosition().closerThan(p, 64)) {
                float r = t.equals("CHEST") ? 0 : 1, g = t.equals("CHEST") ? 1 : 0, b = t.equals("E-CHEST") ? 1 : 0;
                LevelRenderer.renderLineBox(ctx.matrixStack(), ctx.consumers().getBuffer(RenderType.lines()), new AABB(p), r, g, b, 0.8f);
            }
        });
        ctx.matrixStack().pop();
    }
}
