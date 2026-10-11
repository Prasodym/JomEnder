package de.jomender.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import de.jomender.block.ArcFusionChamberBlock;
import de.jomender.blockentity.ArcFusionChamberBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Zeigt den Inhalt der Glaskammer:
 * - Ruhe: Inputs schweben links/rechts, das fertige Item in der Mitte.
 * - Verschmelzen: Inputs kreisen immer schneller, rücken zusammen, steigen und schrumpfen.
 * Glaskammer im Modell: x/z 3-13, y 5-13 Pixel; Plattform oben bei y=6.
 */
public class ArcFusionChamberRenderer
        implements BlockEntityRenderer<ArcFusionChamberBlockEntity, ArcFusionChamberRenderer.State> {

    private static final float PX = 1F / 16F;

    public static class State extends BlockEntityRenderState {
        final ItemStackRenderState input1 = new ItemStackRenderState();
        final ItemStackRenderState input2 = new ItemStackRenderState();
        final ItemStackRenderState output = new ItemStackRenderState();
        Direction facing = Direction.NORTH;
        boolean active;
        float progress;
        float time;
    }

    private final ItemModelResolver itemModelResolver;

    public ArcFusionChamberRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ArcFusionChamberBlockEntity chamber, State state, float partialTick,
                                   Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(chamber, state, partialTick, cameraPos, breaking);

        var blockState = chamber.getBlockState();
        state.facing = blockState.hasProperty(ArcFusionChamberBlock.FACING)
                ? blockState.getValue(ArcFusionChamberBlock.FACING) : Direction.NORTH;
        state.active = chamber.isActive();
        state.progress = chamber.getProgressFraction(partialTick);
        state.time = chamber.getLevel() == null ? 0 : chamber.getLevel().getGameTime() + partialTick;

        int seed = (int) chamber.getBlockPos().asLong();
        resolve(state.input1, chamber.getItem(ArcFusionChamberBlockEntity.INPUT_START), chamber, seed);
        resolve(state.input2, chamber.getItem(ArcFusionChamberBlockEntity.INPUT_START + 1), chamber, seed + 1);

        // Fertiges Item: erster belegter Output-Slot
        ItemStack out = chamber.getItem(ArcFusionChamberBlockEntity.OUTPUT_START);
        if (out.isEmpty()) {
            out = chamber.getItem(ArcFusionChamberBlockEntity.OUTPUT_START + 1);
        }
        resolve(state.output, out, chamber, seed + 2);
    }

    private void resolve(ItemStackRenderState target, ItemStack stack, ArcFusionChamberBlockEntity chamber, int seed) {
        itemModelResolver.updateForTopItem(target, stack, ItemDisplayContext.FIXED, chamber.getLevel(), null, seed);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        boolean hasIn1 = !state.input1.isEmpty();
        boolean hasIn2 = !state.input2.isEmpty();
        boolean hasOut = !state.output.isEmpty();
        if (!hasIn1 && !hasIn2 && !hasOut) {
            return;
        }

        pose.pushPose();
        // Wie der Blockstate drehen, damit "links/rechts" zur Vorderseite passt.
        pose.translate(0.5F, 0F, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(-yRotation(state.facing)));

        float t = state.time;
        float bob = (float) Math.sin(t * 0.08F) * 0.4F;

        if (state.active) {
            float p = state.progress;
            // Kreisen: Radius schrumpft, Tempo steigt, Items steigen und werden kleiner.
            float radius = 2.6F * (float) Math.pow(1F - p, 0.8F) + 0.2F;
            float angle = t * (0.06F + 0.30F * p * p);
            float y = 8.5F + p * 1.2F + bob * (1F - p);
            float scale = 0.32F * (1F - 0.5F * p);
            float spin = t * (3F + 12F * p);

            if (hasIn1) drawItem(pose, collector, state.input1,
                    (float) Math.cos(angle) * radius, y, (float) Math.sin(angle) * radius, scale, spin);
            if (hasIn2) drawItem(pose, collector, state.input2,
                    (float) Math.cos(angle + Math.PI) * radius, y, (float) Math.sin(angle + Math.PI) * radius, scale, spin);
            // Bereits fertige Items schweben oben.
            if (hasOut) drawItem(pose, collector, state.output, 0, 11.6F, 0, 0.2F, t * 2F);
        } else if (hasOut) {
            if (hasIn1 || hasIn2) {
                // Ergebnis oben in der Mitte, Inputs darunter links/rechts.
                drawItem(pose, collector, state.output, 0, 10.6F + bob, 0, 0.3F, t * 2F);
                if (hasIn1) drawItem(pose, collector, state.input1, -2.7F, 7.4F + bob * 0.5F, 0, 0.24F, t * 1.5F);
                if (hasIn2) drawItem(pose, collector, state.input2, 2.7F, 7.4F + bob * 0.5F, 0, 0.24F, t * 1.5F);
            } else {
                // Nur das Ergebnis: groß in der Mitte präsentieren.
                drawItem(pose, collector, state.output, 0, 9F + bob, 0, 0.36F, t * 2F);
            }
        } else {
            // Nur Inputs: links und rechts schweben.
            if (hasIn1) drawItem(pose, collector, state.input1, -2.4F, 8.5F + bob, 0, 0.3F, t * 1.5F);
            if (hasIn2) drawItem(pose, collector, state.input2, 2.4F, 8.5F - bob, 0, 0.3F, t * 1.5F);
        }

        pose.popPose();
    }

    /** x/y/z in Pixeln relativ zur Kammermitte (x/z) bzw. zum Blockboden (y). */
    private static void drawItem(PoseStack pose, SubmitNodeCollector collector, ItemStackRenderState item,
                                 float x, float y, float z, float scale, float spinDegrees) {
        pose.pushPose();
        pose.translate(x * PX, y * PX, z * PX);
        pose.mulPose(Axis.YP.rotationDegrees(spinDegrees));
        pose.scale(scale, scale, scale);
        // Voll beleuchtet, damit die Items in der dunklen Kammer leuchten.
        item.submit(pose, collector, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
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
