package org.opendaylight.controller.md.sal.binding.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.ClassToInstanceMap;
import org.opendaylight.controller.md.sal.binding.api.BindingService;
import org.opendaylight.controller.md.sal.binding.spi.AdapterBuilder;
import org.opendaylight.controller.md.sal.dom.api.DOMService;

abstract class BindingDOMAdapterBuilder<T extends BindingService> extends AdapterBuilder<T, DOMService> {

    interface Factory<T extends BindingService> {

        BindingDOMAdapterBuilder<T> newBuilder();

    }

    private BindingToNormalizedNodeCodec codec;

    public void setCodec(BindingToNormalizedNodeCodec codec) {
        this.codec = codec;
    }

    @Override
    protected final T createInstance(ClassToInstanceMap<DOMService> delegates) {
        Preconditions.checkState(codec != null);
        return createInstance(codec,delegates);
    }

    protected abstract T createInstance(BindingToNormalizedNodeCodec codec2, ClassToInstanceMap<DOMService> delegates);

}
