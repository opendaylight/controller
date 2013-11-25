package org.opendaylight.controller.sal.binding.dom.serializer.impl;

import java.util.Map;

import org.opendaylight.yangtools.yang.binding.BindingCodec;
import org.opendaylight.yangtools.yang.common.QName;

public interface GeneratorListener {

    
    
    void onClassProcessed(Class<?> cl);
    
    void onCodecCreated(Class<?> codec);
    void onValueCodecCreated(Class<?> valueClass,Class<?> valueCodec);
    void onChoiceCodecCreated(Class<?> choiceClass,Class<? extends BindingCodec<Map<QName, Object>,Object>> choiceCodec);
    void onCaseCodecCreated(Class<?> choiceClass,Class<? extends BindingCodec<Map<QName, Object>,Object>> choiceCodec);
    public abstract void onDataContainerCodecCreated(Class<?> dataClass, Class<?  extends BindingCodec<Map<QName, Object>,Object>> dataCodec);
}
