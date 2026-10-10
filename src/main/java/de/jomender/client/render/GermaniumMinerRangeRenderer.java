package de.jomender.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.jomender.blockentity.GermaniumMinerBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Clientseitige Darstellung des aktivierten Mining-Bereichs als 12 grüne Kanten. */
public class GermaniumMinerRangeRenderer
        implements BlockEntityRenderer<GermaniumMinerBlockEntity, GermaniumMinerRangeRenderer.State> {

    private static final int LINE_COLOR = 0xD936F08C; // ARGB

    public static class State extends BlockEntityRenderState {
        boolean preview;
        AABB relativeBounds;
    }

    public GermaniumMinerRangeRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            GermaniumMinerBlockEntity miner,
            State state,
            float partialTick,
            Vec3 cameraPos,
            ModelFeatureRenderer.CrumblingOverlay breaking
    ) {
        BlockEntityRenderer.super.extractRenderState(miner, state, partialTick, cameraPos, breaking);
        state.preview = miner.isPreviewEnabled();
        state.relativeBounds = state.preview
                ? miner.getPreviewBounds().move(
                        -miner.getBlockPos().getX(),
                        -miner.getBlockPos().getY(),
                        -miner.getBlockPos().getZ())
                : null;
    }

    @Override
    public void submit(
            State state,
            PoseStack pose,
            SubmitNodeCollector collector,
            CameraRenderState camera
    ) {
        if (!state.preview || state.relativeBounds == null) {
            return;
        }

        AABB box = state.relativeBounds;
        collector.submitCustomGeometry(pose, RenderTypes.lines(), (entry, vertices) -> {
            float x0 = (float) box.minX;
            float y0 = (float) box.minY;
            float z0 = (float) box.minZ;
            float x1 = (float) box.maxX;
            float y1 = (float) box.maxY;
            float z1 = (float) box.maxZ;

            // Vier untere Kanten.
            line(entry, vertices, x0, y0, z0, x1, y0, z0, 1, 0, 0);
            line(entry, vertices, x1, y0, z0, x1, y0, z1, 0, 0, 1);
            line(entry, vertices, x1, y0, z1, x0, y0, z1, -1, 0, 0);
            line(entry, vertices, x0, y0, z1, x0, y0, z0, 0, 0, -1);

            // Vier obere Kanten.
            line(entry, vertices, x0, y1, z0, x1, y1, z0, 1, 0, 0);
            line(entry, vertices, x1, y1, z0, x1, y1, z1, 0, 0, 1);
            line(entry, vertices, x1, y1, z1, x0, y1, z1, -1, 0, 0);
            line(entry, vertices, x0, y1, z1, x0, y1, z0, 0, 0, -1);

            // Vier senkrechte Kanten.
            line(entry, vertices, x0, y0, z0, x0, y1, z0, 0, 1, 0);
            line(entry, vertices, x1, y0, z0, x1, y1, z0, 0, 1, 0);
            line(entry, vertices, x1, y0, z1, x1, y1, z1, 0, 1, 0);
            line(entry, vertices, x0, y0, z1, x0, y1, z1, 0, 1, 0);
        });
    }

    private static void line(
            PoseStack.Pose pose,
            VertexConsumer vertices,
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            float nx, float ny, float nz
    ) {
        vertices.addVertex(pose, x0, y0, z0)
                .setColor(LINE_COLOR)
                .setNormal(pose, nx, ny, nz)
                .setLineWidth(2.0F);

        vertices.addVertex(pose, x1, y1, z1)
                .setColor(LINE_COLOR)
                .setNormal(pose, nx, ny, nz)
                .setLineWidth(2.0F);
    }

    @Override
    public AABB getRenderBoundingBox(GermaniumMinerBlockEntity miner) {
        return miner.isPreviewEnabled()
                ? miner.getPreviewBounds()
                : new AABB(miner.getBlockPos());
    }
}
