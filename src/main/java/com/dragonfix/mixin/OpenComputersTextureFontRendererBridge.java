package com.dragonfix.mixin;

/**
 * Exposes a colored OpenComputers texture-font draw path used by manual code blocks.
 *
 * <p>
 * Adapted from GTNewHorizons/OpenComputers PR #184 by PinkYuDeer.
 *
 * @see <a href="https://github.com/GTNewHorizons/OpenComputers/pull/184">OpenComputers PR #184</a>
 * @see <a href=
 *      "https://github.com/GTNewHorizons/OpenComputers/commit/e7fa0a1aec316bd99720cd8a0eb1c8f55449cef2">OpenComputers
 *      commit e7fa0a1</a>
 */
public interface OpenComputersTextureFontRendererBridge {

    void dragonfix$drawString(String text, int x, int y, int color);
}
