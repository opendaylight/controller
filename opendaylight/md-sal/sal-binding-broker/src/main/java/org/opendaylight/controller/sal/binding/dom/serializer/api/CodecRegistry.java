package org.opendaylight.controller.sal.binding.dom.serializer.api;

import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.BaseIdentity;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.Identifier;

import java.util.List;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.controller.sal.binding.dom.serializer.api.IdentifierCodec;
import org.opendaylight.yangtools.yang.binding.DataObject;


public interface CodecRegistry {

    InstanceIdentifierCodec getInstanceIdentifierCodec();

    IdentitityCodec<?> getIdentityCodec();

    <T extends DataContainer> DataContainerCodec<T> getCodecForDataObject(Class<T> object);

    <T extends Identifiable<?>> IdentifierCodec<?> getIdentifierCodecForIdentifiable(Class<T> object);

    <T extends Identifier<?>> IdentifierCodec<T> getCodecForIdentifier(Class<T> object);

    <T extends Augmentation<?>> AugmentationCodec<T> getCodecForAugmentation(Class<T> object);

    <T extends BaseIdentity> IdentitityCodec<T> getCodecForIdentity(Class<T> codec);

    Class<?> getClassForPath(List<QName> names);

    IdentifierCodec<?> getKeyCodecForPath(List<QName> names);


    void bindingClassEncountered(Class<?> cls);

    void putPathToClass(List<QName> names, Class<?> cls);

    public abstract QName getQNameForAugmentation(Class<?> cls);
}
