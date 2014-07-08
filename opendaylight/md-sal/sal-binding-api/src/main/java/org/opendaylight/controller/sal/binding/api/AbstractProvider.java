package org.opendaylight.controller.sal.binding.api;

import java.util.Collection;
import java.util.Collections;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.yangtools.yang.binding.RpcService;

/*
 * Simple AbstractProvider that provides implementations of deprectated methods.
 *
 * It is intended to be extended by Provider classes so they only need to do their
 * onSessionInitiated.
 */
public abstract class AbstractProvider implements BindingAwareProvider {

    @Override
    public Collection<? extends ProviderFunctionality> getFunctionality() {
        return Collections.emptySet();
    }

    @Override
    public Collection<? extends RpcService> getImplementations() {
        return Collections.emptySet();
    }

    /**
     * Initialization of consumer context.
     *
     * {@link ProviderContext} is replacement of {@link ConsumerContext}
     * so this method is not needed in case of Provider.
     *
     */
    @Deprecated
    @Override
    public final void onSessionInitialized(ConsumerContext session) {
        // NOOP
    }
}
