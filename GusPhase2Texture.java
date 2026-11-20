package com.example.gusboss.client;

import com.example.gusboss.GusBossMod;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import java.io.InputStream;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.slf4j.Logger;

/**
 * Builds the phase 2 skin at runtime from the vanilla iron golem texture (no image file needed):
 *  - the green vines (and yellow flowers) turn gray
 *  - the tips of both arms get a darker, slightly heated look
 * All knobs are the constants below.
 */
public final class GusPhase2Texture {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ResourceLocation SOURCE =
            new ResourceLocation("textures/entity/iron_golem/iron_golem.png");
    private static final ResourceLocation PHASE2 =
            new ResourceLocation(GusBossMod.MOD_ID, "dynamic/gus_phase2");

    // ---- tweak here ----
    private static final boolean GRAY_OUT_FLOWERS = true;   // the yellow flower dots turn gray too
    private static final float VINE_GRAY_BRIGHTNESS = 0.92F;
    private static final int TIP_ROWS = 8;                  // how many pixel rows at the end of each arm are affected
    private static final float TIP_STRENGTH_TOP = 0.20F;    // blend toward the tip color at the start of the tip...
    private static final float TIP_STRENGTH_BOTTOM = 0.55F; // ...and at the very end of the arm
    private static final int TIP_R = 170;                   // heated-steel tip color
    private static final int TIP_G = 50;
    private static final int TIP_B = 40;

    private static boolean ready;
    private static boolean failed;

    private GusPhase2Texture() {}

    /** The phase 2 texture, or the normal golem texture if building it failed. */
    public static ResourceLocation get() {
        if (failed) {
            return SOURCE;
        }
        TextureManager textures = Minecraft.getInstance().getTextureManager();
        if (!ready || textures.getTexture(PHASE2, null) == null) {
            ready = build(textures);
            failed = !ready;
        }
        return ready ? PHASE2 : SOURCE;
    }

    private static boolean build(TextureManager textures) {
        try {
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(SOURCE);
            if (resource.isEmpty()) {
                return false;
            }
            try (InputStream in = resource.get().open()) {
                NativeImage image = NativeImage.read(in);
                recolor(image);
                textures.register(PHASE2, new DynamicTexture(image));
            }
            return true;
        } catch (Exception e) {
            LOGGER.warn("[Gus] could not build the phase 2 texture; using the normal one", e);
            return false;
        }
    }

    private static void recolor(NativeImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        // 1) vines and flowers -> gray (keeps the original shading)
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int px = image.getPixelRGBA(x, y);
                int a = (px >>> 24) & 0xFF;
                if (a == 0) {
                    continue;
                }
                int b = (px >>> 16) & 0xFF;
                int g = (px >>> 8) & 0xFF;
                int r = px & 0xFF;
                boolean vine = g > r + 10 && g > b + 10;
                boolean flower = GRAY_OUT_FLOWERS && r > 170 && g > 140 && b < 90;
                if (vine || flower) {
                    float lum = 0.299F * r + 0.587F * g + 0.114F * b;
                    int gray = clamp(Math.round(lum * (flower ? 1.0F : VINE_GRAY_BRIGHTNESS)));
                    image.setPixelRGBA(x, y, pack(a, gray, gray, clamp(gray + 4)));
                }
            }
        }

        // 2) arm tips. The golem arm boxes are 4 x 30 x 6 at texOffs (60,21) and (60,58) (vanilla layout).
        tintArmTip(image, 60, 21, 4, 30, 6);
        tintArmTip(image, 60, 58, 4, 30, 6);
    }

    /** Tints the bottom rows of the four side faces and the whole bottom face of one arm box. */
    private static void tintArmTip(NativeImage image, int u, int v, int w, int h, int d) {
        int sideTop = v + d;                 // first row of the side faces
        int sideBottom = sideTop + h;        // one past the last row
        int tipStart = sideBottom - TIP_ROWS;
        int sideLeft = u;
        int sideRight = u + 2 * d + 2 * w;   // left, front, right and back faces side by side
        for (int y = tipStart; y < sideBottom; y++) {
            float t = (y - tipStart + 0.5F) / TIP_ROWS;
            float strength = TIP_STRENGTH_TOP + (TIP_STRENGTH_BOTTOM - TIP_STRENGTH_TOP) * t;
            for (int x = sideLeft; x < sideRight; x++) {
                tint(image, x, y, strength);
            }
        }
        // bottom face (the very end of the arm)
        for (int y = v; y < v + d; y++) {
            for (int x = u + d + w; x < u + d + 2 * w; x++) {
                tint(image, x, y, TIP_STRENGTH_BOTTOM);
            }
        }
    }

    private static void tint(NativeImage image, int x, int y, float strength) {
        if (x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight()) {
            return;
        }
        int px = image.getPixelRGBA(x, y);
        int a = (px >>> 24) & 0xFF;
        if (a == 0) {
            return;
        }
        int b = (px >>> 16) & 0xFF;
        int g = (px >>> 8) & 0xFF;
        int r = px & 0xFF;
        float lum = 0.299F * r + 0.587F * g + 0.114F * b;
        float shade = Math.max(0.55F, Math.min(1.1F, lum / 190.0F));
        int nr = clamp(Math.round(r + (TIP_R * shade - r) * strength));
        int ng = clamp(Math.round(g + (TIP_G * shade - g) * strength));
        int nb = clamp(Math.round(b + (TIP_B * shade - b) * strength));
        image.setPixelRGBA(x, y, pack(a, nr, ng, nb));
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    /** NativeImage RGBA pixels are stored as ABGR. */
    private static int pack(int a, int r, int g, int b) {
        return (a << 24) | (b << 16) | (g << 8) | r;
    }
}
