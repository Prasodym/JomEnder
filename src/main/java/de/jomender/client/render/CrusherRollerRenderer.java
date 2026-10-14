package de.jomender.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import de.jomender.Jomender;
import de.jomender.block.CrusherBlock;
import de.jomender.blockentity.CrusherBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * Die zwei Mahlwalzen des Crushers. Sie drehen sich gegenläufig, solange die Maschine arbeitet
 * (Blockstate "active"), und bleiben beim Anhalten in ihrer letzten Stellung stehen.
 */
public class CrusherRollerRenderer
        implements BlockEntityRenderer<CrusherBlockEntity, CrusherRollerRenderer.State> {

    private static final Identifier ROLLER = Identifier.fromNamespaceAndPath(Jomender.MOD_ID, "block/crusher_roller");
    private static final Identifier ROLLER_END = Identifier.fromNamespaceAndPath(Jomender.MOD_ID, "block/crusher_roller_end");

    // Walzen in Modell-Pixeln (facing=north): entlang X, Mitten bei y=8.75 und z=6.2 / 9.8
    private static final float X0 = 3.5F / 16F, X1 = 12.5F / 16F;
    private static final float CENTER_Y = 8.75F / 16F;
    private static final float[] CENTER_Z = {6.2F / 16F, 9.8F / 16F};
    /** Abstand Mitte -> Fläche (wie die gedrehten Würfel im Item-Modell). */
    private static final float APOTHEM = 1.75F / 16F;
    private static final float RADIUS = APOTHEM / (float) Math.cos(Math.PI / 8);
    private static final int SIDES = 8;
    /** Grad pro Tick. */
    private static final float SPEED = 6F;

    public static class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        float angle;
        int light;
    }

    public CrusherRollerRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(CrusherBlockEntity crusher, State state, float partialTick,
                                   Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(crusher, state, partialTick, cameraPos, breaking);
        var blockState = crusher.getBlockState();
        state.facing = blockState.hasProperty(CrusherBlock.FACING)
                ? blockState.getValue(CrusherBlock.FACING) : Direction.NORTH;
        boolean active = blockState.hasProperty(CrusherBlock.ACTIVE) && blockState.getValue(CrusherBlock.ACTIVE);
        if (active && crusher.getLevel() != null) {
            crusher.clientRollerAngle = ((crusher.getLevel().getGameTime() % 3600) + partialTick) * SPEED % 360F;
        }
        state.angle = crusher.clientRollerAngle;
        state.light = crusher.getLevel() != null
                ? LevelRenderer.getLightCoords(crusher.getLevel(), crusher.getBlockPos()) : 0xF000F0;
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        TextureAtlas blocks = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS);
        TextureAtlasSprite side = blocks.getSprite(ROLLER);
        TextureAtlasSprite end = blocks.getSprite(ROLLER_END);

        pose.pushPose();
        // Gleiche Drehung wie im Blockstate (north=0, east=90, south=180, west=270)
        pose.translate(0.5F, 0.0F, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(-yRotation(state.facing)));
        pose.translate(-0.5F, 0.0F, -0.5F);

        collector.submitCustomGeometry(pose, RenderTypes.entityCutout(blocks.location()), (p, buffer) -> {
            for (int i = 0; i < CENTER_Z.length; i++) {
                // gegenläufig: die Walzen ziehen das Material zwischen sich nach unten
                float angle = (float) Math.toRadians(i == 0 ? state.angle : -state.angle);
                drawRoller(p, buffer, side, end, state.light, CENTER_Z[i], angle);
            }
        });
        pose.popPose();
    }

    /** Achteckiges Prisma entlang X. */
    private static void drawRoller(PoseStack.Pose p, VertexConsumer b, TextureAtlasSprite side, TextureAtlasSprite end,
                                   int light, float cz, float angle) {
        float[] ys = new float[SIDES];
        float[] zs = new float[SIDES];
        // Textur-Koordinaten der Stirnseite drehen mit (ungedrehter Winkel)
        float[] us = new float[SIDES];
        float[] vs = new float[SIDES];
        for (int k = 0; k < SIDES; k++) {
            double base = Math.PI / SIDES + k * 2 * Math.PI / SIDES;
            double a = angle + base;
            ys[k] = CENTER_Y + RADIUS * (float) Math.cos(a);
            zs[k] = cz + RADIUS * (float) Math.sin(a);
            us[k] = 0.5F + 0.5F * (float) Math.sin(base);
            vs[k] = 0.5F - 0.5F * (float) Math.cos(base);
        }
        float vStep = 1F / SIDES;
        for (int k = 0; k < SIDES; k++) {
            int n = (k + 1) % SIDES;
            double mid = angle + (k + 1) * 2 * Math.PI / SIDES;
            float ny = (float) Math.cos(mid), nz = (float) Math.sin(mid);
            float v0 = side.getV(k * vStep), v1 = side.getV((k + 1) * vStep);
            vertex(p, b, X0, ys[k], zs[k], side.getU0(), v0, light, 0, ny, nz);
            vertex(p, b, X0, ys[n], zs[n], side.getU0(), v1, light, 0, ny, nz);
            vertex(p, b, X1, ys[n], zs[n], side.getU1(), v1, light, 0, ny, nz);
            vertex(p, b, X1, ys[k], zs[k], side.getU1(), v0, light, 0, ny, nz);
        }
        // Stirnseiten als Fächer aus 4 Vierecken (Mitte + drei Ecken)
        for (int k = 0; k < SIDES; k += 2) {
            int k1 = (k + 1) % SIDES, k2 = (k + 2) % SIDES;
            // +X
            vertex(p, b, X1, CENTER_Y, cz, end.getU(0.5F), end.getV(0.5F), light, 1, 0, 0);
            vertex(p, b, X1, ys[k], zs[k], end.getU(us[k]), end.getV(vs[k]), light, 1, 0, 0);
            vertex(p, b, X1, ys[k1], zs[k1], end.getU(us[k1]), end.getV(vs[k1]), light, 1, 0, 0);
            vertex(p, b, X1, ys[k2], zs[k2], end.getU(us[k2]), end.getV(vs[k2]), light, 1, 0, 0);
            // -X (umgekehrte Reihenfolge)
            vertex(p, b, X0, CENTER_Y, cz, end.getU(0.5F), end.getV(0.5F), light, -1, 0, 0);
            vertex(p, b, X0, ys[k2], zs[k2], end.getU(us[k2]), end.getV(vs[k2]), light, -1, 0, 0);
            vertex(p, b, X0, ys[k1], zs[k1], end.getU(us[k1]), end.getV(vs[k1]), light, -1, 0, 0);
            vertex(p, b, X0, ys[k], zs[k], end.getU(us[k]), end.getV(vs[k]), light, -1, 0, 0);
        }
    }

    private static void vertex(PoseStack.Pose p, VertexConsumer b, float x, float y, float z,
                               float u, float v, int light, float nx, float ny, float nz) {
        b.addVertex(p, x, y, z)
                .setColor(0xFFFFFFFF)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(p, nx, ny, nz);
    }

    private static float yRotation(Direction facing) {
        return switch (facing) {
            case EAST -> 90F;
            case SOUTH -> 180F;
            case WEST -> 270F;
            default -> 0F;
        };
    }
}
