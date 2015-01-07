package org.opendaylight.persisted.mdsal;

import java.util.Iterator;
import java.util.Map;

import org.opendaylight.datasand.codec.EncodeDataContainer;
import org.opendaylight.datasand.codec.EncodeUtils;
import org.opendaylight.datasand.codec.TypeDescriptor;
import org.opendaylight.datasand.codec.observers.IAugmetationObserver;
import org.opendaylight.yangtools.yang.binding.DataObject;

public class MDSalAugmentationObserver implements IAugmetationObserver{

    @Override
    public void encodeAugmentations(Object value, EncodeDataContainer ba) {
        if (value == null) {
            EncodeUtils.encodeNULL(ba);
            return;
        }
        TypeDescriptor ctype = ba.getTypeDescriptorContainer().getTypeDescriptorByObject(value);
        if (ctype.getAugmentationField(value) != null) {
            try {
                Map<?, ?> augmentations = (Map<?, ?>) ctype.getAugmentationField(value).get(value);
                if (augmentations == null) {
                    EncodeUtils.encodeNULL(ba);
                } else {
                    EncodeUtils.encodeInt16(augmentations.size(), ba);
                    for (Iterator<?> iter = augmentations.entrySet().iterator(); iter.hasNext();) {
                        Map.Entry<?, ?> entry = (Map.Entry<?, ?>) iter.next();
                        Class<?> augClass = (Class<?>) entry.getKey();
                        ctype.addToKnownAugmentingClass(augClass);
                        EncodeUtils.encodeObject(entry.getValue(), ba, augClass);
                    }
                }
            } catch (Exception err) {
                err.printStackTrace();
            }
        }else{
            EncodeUtils.encodeNULL(ba);
        }
    }

    @Override
    public void decodeAugmentations(Object builder, EncodeDataContainer ba,Class<?> augmentedClass) {
        if (EncodeUtils.isNULL(ba)) {
            return;
        } else {
            TypeDescriptor ctype = ba.getTypeDescriptorContainer().getTypeDescriptorByClass(augmentedClass);
            if (!ctype.isAugmentationFieldBuilderInitialized()) {
                ctype.setAugmentationFieldBuilderInitialized(true);
                try {
                    ctype.setAugmentationFieldBuilder(TypeDescriptor.findField(builder.getClass(), "augmentation"));
                } catch (Exception err) {
                    err.printStackTrace();
                }
            }
            if (ctype.getAugmentationFieldBuilder() != null) {
                try {
                    Map<Object,Object> augMap = (Map) ctype.getAugmentationFieldBuilder().get(builder);
                    int size = EncodeUtils.decodeInt16(ba);
                    for (int i = 0; i < size; i++) {
                        DataObject dobj = (DataObject) EncodeUtils.decodeObject(ba);
                        augMap.put(dobj.getImplementedInterface(), dobj);
                    }
                } catch (Exception err) {
                    err.printStackTrace();
                }
            }
        }
    }

}
