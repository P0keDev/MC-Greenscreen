package dev.p0ke.greenscreen;

import com.google.gson.Gson;
import com.mojang.blaze3d.platform.InputConstants.Type;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.math.Axis;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.p0ke.greenscreen.Greenscreen.BooleanRenderState;
import dev.p0ke.greenscreen.Greenscreen.EntityRenderState;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class GreenscreenMod implements ModInitializer, ModMenuApi {
	private static final Component WHITELIST_COMMAND_FORMAT =
			Component.literal("/greenscreen [whitelist|blacklist] [add|remove|clear]");
	private static final Component COLOR_COMMAND_FORMAT =
			Component.literal("/greenscreen color toggle | /greenscreen color <r> <g> <b>");
	private static final Component GREENSCREEN_COMMAND_FORMAT =
			Component.literal("/greenscreen [toggle|blocks|particles|entities|stands|color|whitelist|blacklist]");

	private static GreenscreenMod instance;
	private static Greenscreen greenscreen;

	private static Gson gson = new Gson();

	@Override
	public void onInitialize() {
		instance = this;
		loadGreenscreen();
		ClientCommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess) -> registerCommands(dispatcher)));

		KeyMapping toggleKeybind = KeyBindingHelper.registerKeyBinding(new KeyMapping("Toggle Greenscreen", Type.KEYSYM, GLFW.GLFW_KEY_G, "GreenscreenMod"));
		KeyMapping configKeybind = KeyBindingHelper.registerKeyBinding(new KeyMapping("Open Config", Type.KEYSYM, GLFW.GLFW_KEY_F, "GreenscreenMod"));
		KeyMapping entityNameKeybind = KeyBindingHelper.registerKeyBinding(new KeyMapping("Get Entity Name", Type.KEYSYM, GLFW.GLFW_KEY_N, "GreenscreenMod"));
		KeyMapping nameTagKeybind = KeyBindingHelper.registerKeyBinding(new KeyMapping("Get Entity Name Tag", Type.KEYSYM, GLFW.GLFW_KEY_M, "GreenscreenMod"));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (toggleKeybind.consumeClick()) {
				greenscreen().toggleEnabled();
			}

			while (configKeybind.consumeClick()) {
				Minecraft.getInstance().setScreen(this.createConfigScreen(Minecraft.getInstance().screen));
			}

			while (entityNameKeybind.consumeClick()) {
				Entity hovered = getHoveredEntity();
				if (hovered != null) {
					String name = hovered.getScoreboardName();
					Minecraft.getInstance().player.sendSystemMessage(
							Component.literal("Copied name to clipboard: " )
									.append(Component.literal(name)
											.withStyle(ChatFormatting.UNDERLINE)));
					Minecraft.getInstance().keyboardHandler.setClipboard(name);
				}
			}

			while (nameTagKeybind.consumeClick()) {
				Entity hovered = getHoveredEntity();
				if (hovered != null) {
					String name = hovered.getDisplayName().getString();
					Minecraft.getInstance().player.sendSystemMessage(
							Component.literal("Copied name to clipboard: " )
									.append(Component.literal(name)
											.withStyle(ChatFormatting.UNDERLINE)));
					Minecraft.getInstance().keyboardHandler.setClipboard(name);
				}
			}
		});
	}

	public static GreenscreenMod instance() {
		return instance;
	}

	public static Greenscreen greenscreen() {
		return greenscreen;
	}

	public void drawGreenscreenSky(PoseStack poseStack) {
		RenderSystem.enableBlend();
		RenderSystem.depthMask(false);
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder bufferBuilder = tesselator.getBuilder();

		int rgb = greenscreen().getSkyColor();
		for (int i = 0; i < 6; ++i) {
			poseStack.pushPose();
			if (i == 1) {
				poseStack.mulPose(Axis.XP.rotationDegrees(90.0f));
			}
			if (i == 2) {
				poseStack.mulPose(Axis.XP.rotationDegrees(-90.0f));
			}
			if (i == 3) {
				poseStack.mulPose(Axis.XP.rotationDegrees(180.0f));
			}
			if (i == 4) {
				poseStack.mulPose(Axis.ZP.rotationDegrees(90.0f));
			}
			if (i == 5) {
				poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0f));
			}
			Matrix4f matrix4f = poseStack.last().pose();
			bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
			bufferBuilder.vertex(matrix4f, -100.0f, -100.0f, -100.0f).color(rgb).endVertex();
			bufferBuilder.vertex(matrix4f, -100.0f, -100.0f, 100.0f).color(rgb).endVertex();
			bufferBuilder.vertex(matrix4f, 100.0f, -100.0f, 100.0f).color(rgb).endVertex();
			bufferBuilder.vertex(matrix4f, 100.0f, -100.0f, -100.0f).color(rgb).endVertex();
			tesselator.end();
			poseStack.popPose();
		}
		RenderSystem.depthMask(true);
		RenderSystem.disableBlend();
	}

	private int toggleGreenscreen(FabricClientCommandSource src) {
		BooleanRenderState state = greenscreen().toggleEnabled();
		src.sendFeedback(Component.literal("Greenscreen " + state));
		return 0;
	}

	private int toggleBlocks(FabricClientCommandSource src) {
		BooleanRenderState state = greenscreen().toggleBlockRendering();
		src.sendFeedback(Component.literal("Block rendering: " + state));
		return 0;
	}

	private int toggleParticles(FabricClientCommandSource src) {
		BooleanRenderState state = greenscreen().toggleParticleRendering();
		src.sendFeedback(Component.literal("Particle rendering: " + state));
		return 0;
	}

	private int cycleEntities(FabricClientCommandSource src) {
		EntityRenderState state = greenscreen().cycleEntityRendering();
		src.sendFeedback(Component.literal("Entity rendering: " + state));
		return 0;
	}

	private int toggleArmorStands(FabricClientCommandSource src) {
		EntityRenderState state = greenscreen().toggleArmorStandRendering();
		src.sendFeedback(Component.literal("Armor stand rendering: " + state));
		return 0;
	}

	private int toggleNameTags(FabricClientCommandSource src) {
		EntityRenderState state = greenscreen().cycleNameTagRendering();
		src.sendFeedback(Component.literal("Name tag rendering: " + state));
		return 0;
	}

	private int toggleCustomSky(FabricClientCommandSource src) {
		BooleanRenderState state = greenscreen().toggleCustomSkyRendering();
		src.sendFeedback(Component.literal("Custom skybox rendering: " + state));
		return 0;
	}

	private int setCustomSkyColor(FabricClientCommandSource src, int r, int g, int b) {
		boolean result = greenscreen().setSkyColor(r, g, b);
		if (!result) {
			src.sendError(Component.literal("Each RGB value must be within range 0-255"));
			return 1;
		} else {
			src.sendFeedback(Component.literal("Updated color!"));
			return 0;
		}
	}

	private int whitelistAdd(FabricClientCommandSource src, String name) {
		boolean result = greenscreen().whitelistAdd(name);
		if (!result) {
			src.sendError(Component.literal("That entry already exists on the whitelist!"));
			return 1;
		} else {
			src.sendFeedback(Component.literal("Added '" + name + "' to whitelist!"));
			return 0;
		}
	}

	private int whitelistRemove(FabricClientCommandSource src, String name) {
		boolean result = greenscreen().whitelistRemove(name);
		if (!result) {
			src.sendError(Component.literal("No such whitelist entry!"));
			return 1;
		} else {
			src.sendFeedback(Component.literal("Removed '" + name + "' from whitelist!"));
			return 0;
		}
	}

	private int whitelistClear(FabricClientCommandSource src) {
		greenscreen().whitelistClear();
		src.sendFeedback(Component.literal("Cleared whitelist!"));
		return 0;
	}

	private int whitelistList(FabricClientCommandSource src) {
		src.sendFeedback(Component.literal(
				"Current whitelist: " + String.join(", " , greenscreen().getWhitelist())));
		return 1;
	}

	private int blacklistAdd(FabricClientCommandSource src, String name) {
		boolean result = greenscreen().blacklistAdd(name);
		if (!result) {
			src.sendError(Component.literal("That entry already exists on the blacklist!"));
			return 1;
		} else {
			src.sendFeedback(Component.literal("Added '" + name + "' to blacklist!"));
			return 0;
		}
	}

	private int blacklistRemove(FabricClientCommandSource src, String name) {
		boolean result = greenscreen().blacklistRemove(name);
		if (!result) {
			src.sendError(Component.literal("No such blacklist entry!"));
			return 1;
		} else {
			src.sendFeedback(Component.literal("Removed '" + name + "' from blacklist!"));
			return 0;
		}
	}

	private int blacklistClear(FabricClientCommandSource src) {
		greenscreen().blacklistClear();
		src.sendFeedback(Component.literal("Cleared blacklist!"));
		return 0;
	}

	private int blacklistList(FabricClientCommandSource src) {
		src.sendFeedback(Component.literal(
				"Current blacklist: " + String.join(", " , greenscreen().getBlacklist())));
		return 1;
	}

	private void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		LiteralCommandNode<FabricClientCommandSource> skyColorNode = literal("color")
				.then(literal("toggle")
						.executes(ctx -> toggleCustomSky(ctx.getSource())))
				.then(argument("r", integer())
						.then(argument("g", integer())
								.then(argument("b", integer())
										.executes(ctx ->
												setCustomSkyColor(ctx.getSource(),
														getInteger(ctx, "r"),
														getInteger(ctx, "g"),
														getInteger(ctx, "b"))))))
				.executes(ctx -> {
					ctx.getSource().sendError(COLOR_COMMAND_FORMAT);
					return 1;
				}).build();

		LiteralCommandNode<FabricClientCommandSource> whitelistNode = literal("whitelist")
				.then(literal("add")
						.then(argument("name", string())
								.executes(ctx -> whitelistAdd(ctx.getSource(), getString(ctx, "name")))))
				.then(literal("remove")
						.then(argument("name", string())
								.executes(ctx -> whitelistRemove(ctx.getSource(), getString(ctx, "name")))))
				.then(literal("clear")
						.executes(ctx -> whitelistClear(ctx.getSource())))
				.then(literal("list")
						.executes(ctx -> whitelistList(ctx.getSource())))
				.executes(ctx -> {
					ctx.getSource().sendError(WHITELIST_COMMAND_FORMAT);
					return 1;
				}).build();

		LiteralCommandNode<FabricClientCommandSource> blacklistNode = literal("blacklist")
				.then(literal("add")
						.then(argument("name", string())
								.executes(ctx -> blacklistAdd(ctx.getSource(), getString(ctx, "name")))))
				.then(literal("remove")
						.then(argument("name", string())
								.executes(ctx -> blacklistRemove(ctx.getSource(), getString(ctx, "name")))))
				.then(literal("clear")
						.executes(ctx -> blacklistClear(ctx.getSource())))
				.then(literal("list")
						.executes(ctx -> blacklistList(ctx.getSource())))
				.executes(ctx -> {
					ctx.getSource().sendError(WHITELIST_COMMAND_FORMAT);
					return 1;
				}).build();


		LiteralCommandNode<FabricClientCommandSource> greenscreenNode = dispatcher.register(literal("greenscreen")
				.then(literal("toggle")
						.executes(ctx -> toggleGreenscreen(ctx.getSource())))
				.then(literal("blocks")
						.executes(ctx -> toggleBlocks(ctx.getSource())))
				.then(literal("particles")
						.executes(ctx -> toggleParticles(ctx.getSource())))
				.then(literal("entities")
						.executes(ctx -> cycleEntities(ctx.getSource())))
				.then(literal("stands")
						.executes(ctx -> toggleArmorStands(ctx.getSource())))
				.then(literal("nametags")
						.executes(ctx -> toggleNameTags(ctx.getSource())))
				.then(literal("sky")
						.redirect(skyColorNode))
				.then(literal("color")
						.redirect(skyColorNode))
				.then(literal("whitelist")
						.redirect(whitelistNode))
				.then(literal("wl")
						.redirect(whitelistNode))
				.then(literal("blacklist")
						.redirect(blacklistNode))
				.then(literal("bl")
						.redirect(blacklistNode))
				.executes(ctx -> {
					ctx.getSource().sendError(GREENSCREEN_COMMAND_FORMAT);
					return 1;
				}));

		dispatcher.register(literal("gs").redirect(greenscreenNode));
	}

	private void loadGreenscreen() {
		try {
			Path configFile = FabricLoader.getInstance().getConfigDir().resolve("greenscreenconfig.json");
			if (!Files.exists(configFile)) {
				greenscreen = new Greenscreen();
				saveGreenscreen();
				return;
			}

			BufferedReader br = Files.newBufferedReader(configFile);
			greenscreen = gson.fromJson(br, Greenscreen.class);
		} catch (IOException e) {
			greenscreen = new Greenscreen();
			saveGreenscreen();
		}
	}

	private void saveGreenscreen() {
		try {
			Path configFile = FabricLoader.getInstance().getConfigDir().resolve("greenscreenconfig.json");
			String output = gson.toJson(greenscreen);

			BufferedWriter bufferedWriter = Files.newBufferedWriter(configFile);
			bufferedWriter.write(output);
			bufferedWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Screen createConfigScreen(Screen parent) {
		ConfigBuilder configBuilder = ConfigBuilder.create().setParentScreen(parent);
		configBuilder.setTitle(Component.literal("Greenscreen Config"));
		ConfigCategory category = configBuilder.getOrCreateCategory(Component.literal("category"));

		category.addEntry(configBuilder.entryBuilder()
				.startEnumSelector(Component.literal("Greenscreen"), BooleanRenderState.class, greenscreen().state())
				.setSaveConsumer(newValue -> greenscreen().setEnabled(newValue))
				.build());

		category.addEntry(configBuilder.entryBuilder()
				.startEnumSelector(Component.literal("Block Rendering"), BooleanRenderState.class, greenscreen().blockRenderState())
				.setSaveConsumer(newValue -> greenscreen().setBlockRending(newValue))
				.build());

		category.addEntry(configBuilder.entryBuilder()
				.startEnumSelector(Component.literal("Particle Rendering"), BooleanRenderState.class, greenscreen().particleRenderState())
				.setSaveConsumer(newValue -> greenscreen().setParticleRendering(newValue))
				.build());

		category.addEntry(configBuilder.entryBuilder()
				.startEnumSelector(Component.literal("Entity Rendering"), EntityRenderState.class, greenscreen().entityRenderState())
				.setSaveConsumer(newValue -> greenscreen().setEntityRendering(newValue))
				.build());

		category.addEntry(configBuilder.entryBuilder()
				.startEnumSelector(Component.literal("Armor Stand Rendering"), EntityRenderState.class, greenscreen().armorStandRenderState())
				.setSaveConsumer(newValue -> greenscreen().setArmorStandRendering(newValue))
				.build());

		category.addEntry(configBuilder.entryBuilder()
				.startEnumSelector(Component.literal("Name Tag Rendering"), EntityRenderState.class, greenscreen().nameTageRenderState())
				.setSaveConsumer(newValue -> greenscreen().setNameTagRendering(newValue))
				.build());

		category.addEntry(configBuilder.entryBuilder()
				.startEnumSelector(Component.literal("Custom Sky Rendering"), BooleanRenderState.class, greenscreen().customSkyRenderState())
				.setSaveConsumer(newValue -> greenscreen().setCustomSkyRendering(newValue))
				.build());

		category.addEntry(configBuilder.entryBuilder()
				.startColorField(Component.literal("Custom Sky Color"), greenscreen().getSkyColor() & 0xffffff) // mask out transparency value
				.setSaveConsumer2(newValue -> greenscreen().setSkyColor(newValue.getRed(), newValue.getGreen(), newValue.getBlue()))
				.build());

		category.addEntry(configBuilder.entryBuilder()
				.startStrList(Component.literal("Entity Whitelist"), greenscreen().getWhitelist())
				.setSaveConsumer(newValue -> greenscreen().setWhitelist(newValue))
				.setDefaultValue(List.of())
				.build());

		category.addEntry(configBuilder.entryBuilder()
				.startStrList(Component.literal("Entity Blacklist"), greenscreen().getBlacklist())
				.setSaveConsumer(newValue -> greenscreen().setBlacklist(newValue))
				.setDefaultValue(List.of())
				.build());

		category.addEntry(configBuilder.entryBuilder()
				.startStrList(Component.literal("Name Tag Transformers"), greenscreen().getNameTagTransformsList())
				.setSaveConsumer(newValue -> greenscreen().setNameTagTransforms(newValue))
				.setDefaultValue(List.of())
				.build());

		configBuilder.setSavingRunnable(this::saveGreenscreen);

		return configBuilder.build();
	}

	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return this::createConfigScreen;
	}

	public GreenscreenButton createPauseScreenButton(PauseScreen pauseScreen) {
		if (pauseScreen.children().stream().anyMatch(child -> child instanceof GreenscreenButton)) return null;

		GreenscreenButton greenscreenButton = new GreenscreenButton(() -> this.createConfigScreen(pauseScreen),
				pauseScreen.width - 24, pauseScreen.height - 24);
		return greenscreenButton;
	}

	private static final float RAYCAST_RANGE = 5f;

	private static Entity getHoveredEntity() {
		LocalPlayer player = Minecraft.getInstance().player;

		Vec3 start = player.getEyePosition(1f);
		Vec3 look = player.getLookAngle();
		Vec3 direction = start.add(look.x * RAYCAST_RANGE, look.y * RAYCAST_RANGE, look.z * RAYCAST_RANGE);
		AABB bb = player.getBoundingBox()
				.expandTowards(look.x * RAYCAST_RANGE, look.y * RAYCAST_RANGE, look.z * RAYCAST_RANGE)
				.expandTowards(1, 1, 1);

		EntityHitResult hitResult = ProjectileUtil.getEntityHitResult(
				Minecraft.getInstance().level, player, start, direction, bb, (e) -> true);

		return (hitResult == null) ? null : hitResult.getEntity();
	}

	private static class GreenscreenButton extends Button {

		GreenscreenButton(Supplier<Screen> supplier, int x, int y) {
			super(x, y, 20, 20, Component.literal(""),
					button -> Minecraft.getInstance().setScreen(supplier.get()), Button.DEFAULT_NARRATION);
		}

		@Override
		public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
			super.render(graphics, mouseX, mouseY, partialTicks);

			graphics.fill(this.getX() + 3, this.getY() + 3,
					this.getX() + 17, this.getY() + 17, 0xFF00FF00);

		}
	}

}
