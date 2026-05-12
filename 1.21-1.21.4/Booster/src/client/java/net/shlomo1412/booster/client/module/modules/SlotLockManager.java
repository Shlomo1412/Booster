package net.shlomo1412.booster.client.module.modules;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks soft and hard-locked slots per screen handler sync ID.
 */
public final class SlotLockManager {
    private static final Map<Long, LockType> LOCKED_SLOTS = new HashMap<>();

    public enum LockType {
        SOFT,
        HARD
    }

    private SlotLockManager() {}

    private static long key(int syncId, int slotId) {
        return (((long) syncId) << 32) | (slotId & 0xFFFFFFFFL);
    }

    public static LockType getLockType(int syncId, int slotId) {
        return LOCKED_SLOTS.get(key(syncId, slotId));
    }

    public static boolean isLocked(int syncId, int slotId) {
        return LOCKED_SLOTS.containsKey(key(syncId, slotId));
    }

    public static boolean isHardLocked(int syncId, int slotId) {
        return getLockType(syncId, slotId) == LockType.HARD;
    }

    public static void toggleLock(int syncId, int slotId, boolean hard) {
        LockType newType = hard ? LockType.HARD : LockType.SOFT;
        long key = key(syncId, slotId);
        LockType current = LOCKED_SLOTS.get(key);
        if (current == newType) {
            LOCKED_SLOTS.remove(key);
        } else {
            LOCKED_SLOTS.put(key, newType);
        }
    }

    public static void clearScreen(int syncId) {
        LOCKED_SLOTS.keySet().removeIf(k -> (int) (k >> 32) == syncId);
    }
}
