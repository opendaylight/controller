package org.opendaylight.controller.md.sal.dom.store.impl;

import com.google.common.primitives.UnsignedLong;

public final class StoreUtils {


    public static final UnsignedLong increase(final UnsignedLong original) {
        return original.plus(UnsignedLong.ONE);
    }




}
