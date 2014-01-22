package org.opendaylight.yangtools.sal.binding.generator.impl;

public interface BindingClassListener {

    void onBindingClassCaptured(Class<?> cls);

    void onBindingClassProcessed(Class<?> cls);
}
