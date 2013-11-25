package org.opendaylight.controller.sal.binding.dom.serializer.impl;

import java.lang.reflect.Field;
import java.util.Map;

import org.opendaylight.controller.sal.binding.dom.serializer.api.InstanceIdentifierCodec;
import org.opendaylight.yangtools.yang.binding.BindingCodec;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;

public class CodecMapping {

    public static final String INSTANCE_IDENTIFIER_CODEC = "INSTANCE_IDENTIFIER_CODEC";
    public static final String CLASS_TO_CASE_MAP = "CLASS_TO_CASE";
    public static final String COMPOSITE_TO_CASE = "COMPOSITE_TO_CASE";
    public static final String AUGMENTATION_CODEC = "AUGMENTATION_CODEC";
    
    public static void setIdentifierCodec(Class obj,InstanceIdentifierCodec codec) {
        Field instanceIdField;
        try {
            instanceIdField = obj.getField(INSTANCE_IDENTIFIER_CODEC);
            instanceIdField.set(null, codec);
        } catch (NoSuchFieldException e) {
           // NOOP
        } catch (SecurityException e) {
            // NOOP
        } catch (IllegalAccessException e) {
            // NOOp
        }
    }

    public static void setClassToCaseMap(Class<? extends BindingCodec<?,?>> codec,
            Map<Class<?>,BindingCodec<?,?>> classToCaseRawCodec) {
        Field instanceIdField;
        try {
            instanceIdField = codec.getField(CLASS_TO_CASE_MAP);
            instanceIdField.set(null, classToCaseRawCodec);
        } catch (NoSuchFieldException e) {
           // NOOP
        } catch (SecurityException e) {
            // NOOP
        } catch (IllegalAccessException e) {
            // NOOp
        }
        
        
    }

    public static void setCompositeNodeToCaseMap(Class<? extends BindingCodec<?,?>> codec,
            Map<CompositeNode,BindingCodec<?,?>> compositeToCase) {
        Field instanceIdField;
        try {
            instanceIdField = codec.getField(COMPOSITE_TO_CASE);
            instanceIdField.set(null, compositeToCase);
        } catch (NoSuchFieldException e) {
           // NOOP
        } catch (SecurityException e) {
            // NOOP
        } catch (IllegalAccessException e) {
            // NOOp
        }
    }

    public static void setAugmentationCodec(Class<? extends BindingCodec<Map<QName, Object>, Object>> dataCodec,
            BindingCodec<?,?> augmentableCodec) {
            Field instanceIdField;
            try {
                instanceIdField = dataCodec.getField(AUGMENTATION_CODEC);
                instanceIdField.set(null, augmentableCodec);
            } catch (NoSuchFieldException e) {
               // NOOP
            } catch (SecurityException e) {
                // NOOP
            } catch (IllegalAccessException e) {
                // NOOp
            }
    }
}
