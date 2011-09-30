package org.onehippo.forge.externalresource.api.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * @version $Id$
 */
public enum SynchronizationState {
    SYNCHRONIZED,
    OUT_OF_SYNC,
    BROKEN,
    UNKNOWN,
    BUSY,;

    public static final Map<SynchronizationState, String> TYPE_MAP = new HashMap<SynchronizationState, String>();

    static {
        TYPE_MAP.put(SYNCHRONIZED, "synchronized");
        TYPE_MAP.put(OUT_OF_SYNC, "unsynchronized");
        TYPE_MAP.put(BROKEN, "broken");
        TYPE_MAP.put(UNKNOWN, "unknown");
        TYPE_MAP.put(BUSY, "busy");
    }

    public String getStringValue() {
        String type = TYPE_MAP.get(this);
        if (type != null) {
            return type;
        }
        return BUSY.getStringValue();
    }
}
