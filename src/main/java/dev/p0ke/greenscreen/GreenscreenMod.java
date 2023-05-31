package dev.p0ke.greenscreen;

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
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class GreenscreenMod implements ModInitializer, ModMenuApi {
	private static final Component COLOR_COMMAND_FORMAT =
			Component.literal("/greenscreen color toggle | /greenscreen color <r> <g> <b>");
	private static final Component GREENSCREEN_COMMAND_FORMAT =
			Component.literal("/greenscreen <toggle|blocks|particles|entities|stands|color>");

	private static GreenscreenMod instance;
	private static Greenscreen greenscreen;

	@Override
	public void onInitialize() {
		instance = this;
		greenscreen = new Greenscreen();
		ClientCommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess) -> registerCommands(dispatcher)));

		KeyMapping toggleKeybind = KeyBindingHelper.registerKeyBinding(new KeyMapping("Toggle Greenscreen", Type.KEYSYM, GLFW.GLFW_KEY_G, "GreenscreenMod"));
		KeyMapping configKeybind = KeyBindingHelper.registerKeyBinding(new KeyMapping("Open Config", Type.KEYSYM, GLFW.GLFW_KEY_F, "GreenscreenMod"));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (toggleKeybind.consumeClick()) {
				greenscreen().toggleEnabled();
			}

			while (configKeybind.consumeClick()) {
				Minecraft.getInstance().setScreen(this.createConfigScreen(Minecraft.getInstance().screen));
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

		int rgb = greenscreen().getSkyColor().getRGB();
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
		BooleanRenderState state = greenscreen().toggleArmorStandRendering();
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
				.executes(ctx -> {
					ctx.getSource().sendError(GREENSCREEN_COMMAND_FORMAT);
					return 1;
				}));

		dispatcher.register(literal("gs").redirect(greenscreenNode));
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
				.startEnumSelector(Component.literal("Armor Stand Rendering"), BooleanRenderState.class, greenscreen().armorStandRenderState())
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
				.startColorField(Component.literal("Custom Sky Color"), greenscreen().getSkyColor().getRGB() & 0xffffff) // mask out transparency value
				.setSaveConsumer2(newValue -> greenscreen().setSkyColor(newValue.getRed(), newValue.getGreen(), newValue.getBlue()))
				.build());

		return configBuilder.build();
	}

	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return this::createConfigScreen;
	}

}
