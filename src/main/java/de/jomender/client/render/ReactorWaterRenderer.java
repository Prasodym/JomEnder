package de.jomender.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.jomender.reactor.ReactorControllerBlockEntity;
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
import net.minecraft.core.BlockPos;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Zeichnet das Kühlwasser im Innenraum des Reaktors. Die Wasserfläche steigt mit
 * dem Tankstand - sichtbar durch das Reaktor-Glas.
 */
public class ReactorWaterRenderer
        implements BlockEntityRenderer<ReactorControllerBlockEntity, ReactorWaterRenderer.State> {

    private static final Identifier WATER_STILL = Identifier.withDefaultNamespace("block/water_still");
    /** Wasserfarbe wie in normalen Biomen, leicht durchsichtig. */
    private static final int WATER_COLOR = 0xC03F76E4;
    /** Etwas Abstand zu den Wänden, damit nichts flackert. */
    private static final float INSET = 0.002F;

    public static class State extends BlockEntityRenderState {
        boolean visible;
        // Wasser-Quader relativ zum Controller (in Blöcken)
        float x0, y0, z0, x1, y1, z1;
        int light;
    }

    public ReactorWaterRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ReactorControllerBlockEntity reactor, State state, float partialTick,
                                   Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(reactor, state, partialTick, cameraPos, breaking);

        BlockPos min = reactor.getRenderMin();
        BlockPos max = reactor.getRenderMax();
        float fill = reactor.getRenderFill();
        state.visible = min != null && max != null && fill > 0F;
        if (!state.visible) return;

        BlockPos origin = reactor.getBlockPos();
        // Innenraum = Box ohne die Hülle
        state.x0 = min.getX() + 1 - origin.getX() + INSET;
        state.y0 = min.getY() + 1 - origin.getY() + INSET;
        state.z0 = min.getZ() + 1 - origin.getZ() + INSET;
        state.x1 = max.getX() - origin.getX() - INSET;
        state.z1 = max.getZ() - origin.getZ() - INSET;
        float innerHeight = max.getY() - min.getY() - 1;
        state.y1 = state.y0 + Math.max(0.05F, innerHeight * fill - 2 * INSET);

        // Licht aus dem Innenraum (der Controller selbst ist ein voller Block = dunkel)
        BlockPos center = new BlockPos((min.getX() + max.getX()) / 2, min.getY() + 1, (min.getZ() + max.getZ()) / 2);
        state.light = reactor.getLevel() != null ? LevelRenderer.getLightCoords(reactor.getLevel(), center) : 0xF000F0;
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.visible) return;
        TextureAtlas blocks = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS);
        TextureAtlasSprite sprite = blocks.getSprite(WATER_STILL);

        collector.submitCustomGeometry(pose, RenderTypes.entityTranslucent(blocks.location()),
                (p, buffer) -> drawBox(p, buffer, sprite, state));
    }

    /** Quader aus 1x1-Kacheln, damit die Wassertextur nicht gestreckt wird. */
    private static void drawBox(PoseStack.Pose p, VertexConsumer b, TextureAtlasSprite s, State st) {
        int light = st.light;
        // Oben (Wasseroberfläche)
        for (float x = st.x0; x < st.x1; x += 1F) {
            for (float z = st.z0; z < st.z1; z += 1F) {
                float xe = Math.min(x + 1F, st.x1), ze = Math.min(z + 1F, st.z1);
                quad(p, b, s, light, 0, 1, 0,
                        x, st.y1, z, x, st.y1, ze, xe, st.y1, ze, xe, st.y1, z, xe - x, ze - z);
            }
        }
        // Seiten: Norden/Süden (z fest) und Westen/Osten (x fest)
        for (float x = st.x0; x < st.x1; x += 1F) {
            float xe = Math.min(x + 1F, st.x1);
            for (float y = st.y0; y < st.y1; y += 1F) {
                float ye = Math.min(y + 1F, st.y1);
                quad(p, b, s, light, 0, 0, -1, xe, y, st.z0, x, y, st.z0, x, ye, st.z0, xe, ye, st.z0, xe - x, ye - y);
                quad(p, b, s, light, 0, 0, 1, x, y, st.z1, xe, y, st.z1, xe, ye, st.z1, x, ye, st.z1, xe - x, ye - y);
            }
        }
        for (float z = st.z0; z < st.z1; z += 1F) {
            float ze = Math.min(z + 1F, st.z1);
            for (float y = st.y0; y < st.y1; y += 1F) {
                float ye = Math.min(y + 1F, st.y1);
                quad(p, b, s, light, -1, 0, 0, st.x0, y, z, st.x0, y, ze, st.x0, ye, ze, st.x0, ye, z, ze - z, ye - y);
                quad(p, b, s, light, 1, 0, 0, st.x1, y, ze, st.x1, y, z, st.x1, ye, z, st.x1, ye, ze, ze - z, ye - y);
            }
        }
    }

    /** Ein Viereck mit 4 Ecken, UV-Ausschnitt passend zur Kachelgröße (Anteil 0..1 des Sprites). */
    private static void quad(PoseStack.Pose p, VertexConsumer b, TextureAtlasSprite s, int light,
                             float nx, float ny, float nz,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float uSize, float vSize) {
        float u0 = s.getU0(), v0 = s.getV0();
        float u1 = s.getU(uSize), v1 = s.getV(vSize);
        vertex(p, b, ax, ay, az, u0, v1, light, nx, ny, nz);
        vertex(p, b, bx, by, bz, u1, v1, light, nx, ny, nz);
        vertex(p, b, cx, cy, cz, u1, v0, light, nx, ny, nz);
        vertex(p, b, dx, dy, dz, u0, v0, light, nx, ny, nz);
    }

    private static void vertex(PoseStack.Pose p, VertexConsumer b, float x, float y, float z,
                               float u, float v, int light, float nx, float ny, float nz) {
        b.addVertex(p, x, y, z)
                .setColor(WATER_COLOR)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(p, nx, ny, nz);
    }

    /** Der Innenraum ist größer als der Controller-Block - sonst würde das Wasser zu früh ausgeblendet. */
    @Override
    public AABB getRenderBoundingBox(ReactorControllerBlockEntity reactor) {
        BlockPos min = reactor.getRenderMin();
        BlockPos max = reactor.getRenderMax();
        if (min == null || max == null) return new AABB(reactor.getBlockPos());
        return new AABB(min.getX(), min.getY(), min.getZ(), max.getX() + 1, max.getY() + 1, max.getZ() + 1);
    }
}
