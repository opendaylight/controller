package org.opendaylight.persisted.mdsal;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.datasand.codec.EncodeDataContainer;
import org.datasand.codec.TypeDescriptor;
import org.datasand.codec.bytearray.ByteArrayEncodeDataContainer;
import org.datasand.codec.observers.IAugmetationObserver;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.DataObject;
/**
 * @author - Sharon Aicler (saichler@cisco.com)
 */
public class MDSalAugmentationObserver implements IAugmetationObserver{

    private Map<Class<?>,Method> addAugMethods = new HashMap<Class<?>,Method>();

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
                Method m = addAugMethods.get(ctype.getTypeClass());
                if(m==null){
                    try{
                        m = builder.getClass().getMethod("addAugmentation",new Class[]{Class.class,Augmentation.class});
                        addAugMethods.put(ctype.getTypeClass(), m);
                    }catch(Exception err){
                        err.printStackTrace();
                    }
                }
                try {
                    int size = ba.getEncoder().decodeInt16(ba);
                    for (int i = 0; i < size; i++) {
                        DataObject dobj = (DataObject) ba.getEncoder().decodeObject(ba);
                        m.invoke(builder, new Object[]{dobj.getImplementedInterface(),dobj});
                    }
                } catch (Exception err) {
                    err.printStackTrace();
                }
            }
        }
    }

}
