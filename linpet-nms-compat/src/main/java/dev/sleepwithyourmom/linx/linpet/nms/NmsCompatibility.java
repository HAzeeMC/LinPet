package dev.sleepwithyourmom.linx.linpet.nms;

/**
 * Declares that Lin'Pet does not require NMS behavior for the current zero-entity implementation.
 */
public final class NmsCompatibility {
    private NmsCompatibility() {
    }

    /**
     * Returns whether the runtime requires an NMS compatibility bridge.
     *
     * @return false for the current implementation
     */
    public static boolean required() {
        return false;
    }
}
