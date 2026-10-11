package de.jomender.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import de.jomender.block.ElectricSmelterBlock;
import de.jomender.blockentity.ElectricSmelterBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.AbstractEndPortalRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * Zeichnet den Kern des Electric Smelters mit dem echten End-Portal-Shader
 * (animierter Sternenhimmel), genau wie beim Vanilla-Endportal.
 */
public class ElectricSmelterCoreRenderer
        implements BlockEntityRenderer<ElectricSmelterBlockEntity, ElectricSmelterCoreRenderer.State> {

    // Kern-Box in Modell-Pixeln (0-16), für facing=north. Entspricht dem früheren
    // Element [5,5,12] -> [11,11,13] aus models/block/electric_smelter.json.
    private static final float MIN_X = 5, MIN_Y = 5, MIN_Z = 12;
    private static final float MAX_X = 11, MAX_Y = 11, MAX_Z = 13;

    public static class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
    }

    public ElectricSmelterCoreRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            ElectricSmelterBlockEntity smelter,
            State state,
            float partialTick,
            Vec3 cameraPos,
            ModelFeatureRenderer.CrumblingOverlay breaking
    ) {
        BlockEntityRenderer.super.extractRenderState(smelter, state, partialTick, cameraPos, breaking);
        var blockState = smelter.getBlockState();
        state.facing = blockState.hasProperty(ElectricSmelterBlock.FACING)
                ? blockState.getValue(ElectricSmelterBlock.FACING)
                : Direction.NORTH;
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();

        // Gleiche Drehung wie im Blockstate (north=0, east=90, south=180, west=270; im Uhrzeigersinn).
        pose.translate(0.5F, 0.0F, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(-yRotation(state.facing)));
        pose.translate(-0.5F, 0.0F, -0.5F);

        // Einheitswürfel auf die Kern-Box skalieren. Der Portal-Shader projiziert
        // im Bildschirmraum, das Muster wird dadurch also nicht verzerrt.
        pose.translate(MIN_X / 16F, MIN_Y / 16F, MIN_Z / 16F);
        pose.scale((MAX_X - MIN_X) / 16F, (MAX_Y - MIN_Y) / 16F, (MAX_Z - MIN_Z) / 16F);

        AbstractEndPortalRenderer.submitSpecial(RenderTypes.endPortal(), pose, collector);

        pose.popPose();
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
