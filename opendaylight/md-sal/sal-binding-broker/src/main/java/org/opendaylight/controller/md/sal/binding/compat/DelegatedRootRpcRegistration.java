package org.opendaylight.controller.md.sal.binding.compat;

import com.google.common.base.Throwables;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.binding.RpcService;

final class DelegatedRootRpcRegistration<T extends RpcService> implements RpcRegistration<T> {

    private final ObjectRegistration<T> delegate;
    private final Class<T> type;

    public DelegatedRootRpcRegistration(Class<T> type,ObjectRegistration<T> impl) {
        this.delegate = impl;
        this.type = type;
    }


    @Override
    public void close() {
        try {
            // FIXME: Should use more specific registration object.
            delegate.close();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public T getInstance() {
        return delegate.getInstance();
    }

    @Override
    public Class<T> getServiceType() {
        return type;
    }

}
