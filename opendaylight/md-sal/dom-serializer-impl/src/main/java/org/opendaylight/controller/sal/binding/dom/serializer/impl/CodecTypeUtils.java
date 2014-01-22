package org.opendaylight.yangtools.sal.binding.generator.impl;

import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.IdentifiableItem;

public class CodecTypeUtils {

    @SuppressWarnings({"unchecked","rawtypes"})
    public static IdentifiableItem<?, ?> newIdentifiableItem(Class<?> type, Object key) {
        Class<? extends Identifiable<?>> identifiableType = (Class<? extends Identifiable<?>>) type;
        Identifier<? extends Identifiable<?>> identifier = (Identifier<? extends Identifiable<?>>) key;
        return new IdentifiableItem(identifiableType,identifier);
    }
}
