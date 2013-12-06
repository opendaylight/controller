package org.opendaylight.controller.sal.binding.dom.serializer.impl;

import java.lang.reflect.Field;
import java.util.Map;

import org.opendaylight.controller.sal.binding.dom.serializer.api.IdentitityCodec;
import org.opendaylight.controller.sal.binding.dom.serializer.api.InstanceIdentifierCodec;
import org.opendaylight.yangtools.yang.binding.BindingCodec;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CodecMapping {

    private static final Logger LOG = LoggerFactory.getLogger(CodecMapping.class);
    
    public static final String INSTANCE_IDENTIFIER_CODEC = "INSTANCE_IDENTIFIER_CODEC";
    public static final String IDENTITYREF_CODEC = "IDENTITYREF_CODEC";
    
    public static final String CLASS_TO_CASE_MAP = "CLASS_TO_CASE";
    public static final String COMPOSITE_TO_CASE = "COMPOSITE_TO_CASE";
    public static final String AUGMENTATION_CODEC = "AUGMENTATION_CODEC";
    
    public static void setIdentifierCodec(Class<?> obj,InstanceIdentifierCodec codec) {
        Field instanceIdField;
        try {
            instanceIdField = obj.getField(INSTANCE_IDENTIFIER_CODEC);
            if(obj != null) {
                instanceIdField.set(null, codec);
            }
        } catch (NoSuchFieldException e) {
           LOG.trace("Instance identifier codec is not needed for {}",obj.getName(),e);
        } catch (SecurityException | IllegalAccessException e) {
            LOG.error("Instance identifier could not be set for {}",obj.getName(),e);
        }
    }


    public static void setIdentityRefCodec(Class<?> obj,IdentitityCodec<?> codec) {
        Field instanceIdField;
        try {
            instanceIdField = obj.getField(IDENTITYREF_CODEC);
            if(obj != null) {
                instanceIdField.set(null, codec);
            }
        } catch (NoSuchFieldException e) {
           LOG.trace("Instance identifier codec is not needed for {}",obj.getName(),e);
        } catch (SecurityException | IllegalAccessException e) {
            LOG.error("Instance identifier could not be set for {}",obj.getName(),e);
        }
    }

    public static void setClassToCaseMap(Class<? extends BindingCodec<?,?>> codec,
            Map<Class<?>,BindingCodec<?,?>> classToCaseRawCodec) {
        Field instanceIdField;
        try {
            instanceIdField = codec.getField(CLASS_TO_CASE_MAP);
            instanceIdField.set(null, classToCaseRawCodec);
        } catch (NoSuchFieldException e) {
            LOG.debug("BUG: Class to case mappping is not needed for {}",codec.getName(),e);
        } catch (SecurityException | IllegalAccessException e) {
            LOG.error("Class to case mappping could not be set for {}",codec.getName(),e);
        }
    }

    public static void setCompositeNodeToCaseMap(Class<? extends BindingCodec<?,?>> codec,
            Map<CompositeNode,BindingCodec<?,?>> compositeToCase) {
        Field instanceIdField;
        try {
            instanceIdField = codec.getField(COMPOSITE_TO_CASE);
            instanceIdField.set(null, compositeToCase);
        } catch (NoSuchFieldException e) {
            LOG.debug("BUG: Class to case mappping is not needed for {}",codec.getName(),e);
        } catch (SecurityException | IllegalAccessException e) {
            LOG.error("Composite node to case mappping could not be set for {}",codec.getName(),e);
        }
    }

    public static void setAugmentationCodec(Class<? extends BindingCodec<?,?>> dataCodec,
            BindingCodec<?,?> augmentableCodec) {
            Field instanceIdField;
            try {
                instanceIdField = dataCodec.getField(AUGMENTATION_CODEC);
                instanceIdField.set(null, augmentableCodec);
            } catch (NoSuchFieldException e) {
                LOG.debug("BUG: Augmentation codec is not needed for {}",dataCodec.getName(),e);
            } catch (SecurityException | IllegalAccessException e) {
                LOG.error("Augmentation codec could not be set for {}",dataCodec.getName(),e);
            }
    }
    
    
    public static BindingCodec<?,?> getAugmentationCodec(Class<? extends BindingCodec<?,?>> dataCodec) {
            Field instanceIdField;
            try {
                instanceIdField = dataCodec.getField(AUGMENTATION_CODEC);
                return (BindingCodec<?,?>) instanceIdField.get(null);
            } catch (NoSuchFieldException e) {
                LOG.debug("BUG: Augmentation codec is not needed for {}",dataCodec.getName(),e);
            } catch (SecurityException | IllegalAccessException e) {
                LOG.error("Augmentation codec could not be set for {}",dataCodec.getName(),e);
            }
            return null;
    }
}
