package org.opendaylight.controller.sal.binding.dom.serializer.api;

import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.Identifier;

import java.util.List;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.controller.sal.binding.dom.serializer.api.IdentifierCodec;


public interface CodecRegistry {

    InstanceIdentifierCodec getInstanceIdentifierCodec();

    <T extends DataContainer> DataContainerCodec<T> getCodecForDataObject(Class<T> object);

    <T extends Identifiable<?>> IdentifierCodec<?> getIdentifierCodecForIdentifiable(Class<T> object);

    <T extends Identifier<?>> IdentifierCodec<T> getCodecForIdentifier(Class<T> object);

    <T extends Augmentation<?>> AugmentationCodec<T> getCodecForAugmentation(Class<T> object);

    Class<?> getClassForPath(List<QName> names);

    IdentifierCodec<?> getKeyCodecForPath(List<QName> names);
    
    
    void bindingClassEncountered(Class<?> cls);
}
