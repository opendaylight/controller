package org.opendaylight.datasand.codec.observers;

import org.opendaylight.datasand.codec.EncodeDataContainer;

public interface IAugmetationObserver {
    public void encodeAugmentations(Object value, EncodeDataContainer ba);
    public void decodeAugmentations(Object builder, EncodeDataContainer ba,Class<?> augmentedClass);
}
