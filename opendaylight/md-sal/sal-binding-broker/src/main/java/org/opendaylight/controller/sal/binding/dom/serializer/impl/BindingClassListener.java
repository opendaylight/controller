package org.opendaylight.controller.sal.binding.dom.serializer.impl;

public interface BindingClassListener {

    void onBindingClassCaptured(Class<?> cls);
    
    void onBindingClassProcessed(Class<?> cls);
}
