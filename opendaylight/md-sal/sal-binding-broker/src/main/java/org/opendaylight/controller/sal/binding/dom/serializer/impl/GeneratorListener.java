package org.opendaylight.controller.sal.binding.dom.serializer.impl;

import java.util.Map;

import org.opendaylight.yangtools.yang.binding.BindingCodec;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.ChoiceNode;

public interface GeneratorListener {

    
    
    void onClassProcessed(Class<?> cl);
    
    void onCodecCreated(Class<?> codec);
    void onValueCodecCreated(Class<?> valueClass,Class<?> valueCodec);
    void onCaseCodecCreated(Class<?> choiceClass,Class<? extends BindingCodec<Map<QName, Object>,Object>> choiceCodec);
    void onDataContainerCodecCreated(Class<?> dataClass, Class<?  extends BindingCodec<?,?>> dataCodec);

    void onChoiceCodecCreated(Class<?> choiceClass,
            Class<? extends BindingCodec<Map<QName, Object>, Object>> choiceCodec, ChoiceNode schema);
}
