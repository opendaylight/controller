package org.opendaylight.controller.test.sal.binding.it;

import java.util.Collection;
import java.util.Collections;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.yangtools.yang.binding.RpcService;

public abstract class AbstractTestProvider implements BindingAwareProvider {

    @Override
    public Collection<? extends RpcService> getImplementations() {
        return Collections.emptySet();
    }

    @Override
    public Collection<? extends ProviderFunctionality> getFunctionality() {
        return Collections.emptySet();
    }

    @Override
    public void onSessionInitialized(ConsumerContext session) {
        // Noop

    }

}
