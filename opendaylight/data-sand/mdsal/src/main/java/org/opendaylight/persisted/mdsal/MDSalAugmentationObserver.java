package org.opendaylight.persisted.mdsal;

import java.util.Iterator;
import java.util.Map;

import org.opendaylight.datasand.codec.EncodeDataContainer;
import org.opendaylight.datasand.codec.TypeDescriptor;
import org.opendaylight.datasand.codec.bytearray.ByteArrayEncodeDataContainer;
import org.opendaylight.datasand.codec.observers.IAugmetationObserver;
import org.opendaylight.yangtools.yang.binding.DataObject;
/**
 * @author - Sharon Aicler (saichler@cisco.com)
 */
public class MDSalAugmentationObserver implements IAugmetationObserver{

    @Override
    public void encodeAugmentations(Object value, EncodeDataContainer ba) {
        if (value == null) {
            ba.getEncoder().encodeNULL(ba);
            return;
        }
        TypeDescriptor ctype = ba.getTypeDescriptorContainer().getTypeDescriptorByObject(value);
        if (ctype.getAugmentationField(value) != null) {
            try {
                Map<?, ?> augmentations = (Map<?, ?>) ctype.getAugmentationField(value).get(value);
                if(ba instanceof ByteArrayEncodeDataContainer){
                    if (augmentations == null) {
                        ba.getEncoder().encodeNULL(ba);
                    } else {
                        ba.getEncoder().encodeInt16(augmentations.size(), ba);
                        for (Iterator<?> iter = augmentations.entrySet().iterator(); iter.hasNext();) {
                            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) iter.next();
                            Class<?> augClass = (Class<?>) entry.getKey();
                            ctype.addToKnownAugmentingClass(augClass);
                            ba.getEncoder().encodeObject(entry.getValue(), ba, augClass);
                        }
                    }
                }else{
                    if (augmentations == null) {

                    }else{
                        for (Iterator<?> iter = augmentations.entrySet().iterator(); iter.hasNext();) {
                            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) iter.next();
                            Class<?> augClass = (Class<?>) entry.getKey();
                            ctype.addToKnownAugmentingClass(augClass);
                            TypeDescriptor augCType = ba.getTypeDescriptorContainer().getTypeDescriptorByClass(augClass);
                            augCType.getSerializer().encode(entry.getValue(), ba);
                        }
                    }
                }
            } catch (Exception err) {
                err.printStackTrace();
            }
        }else{
            ba.getEncoder().encodeNULL(ba);
        }
    }

    @Override
    public void decodeAugmentations(Object builder, EncodeDataContainer ba,Class<?> augmentedClass) {
        if (ba.getEncoder().isNULL(ba)) {
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
                    int size = ba.getEncoder().decodeInt16(ba);
                    for (int i = 0; i < size; i++) {
                        DataObject dobj = (DataObject) ba.getEncoder().decodeObject(ba);
                        augMap.put(dobj.getImplementedInterface(), dobj);
                    }
                } catch (Exception err) {
                    err.printStackTrace();
                }
            }
        }
    }

}
