package org.opendaylight.controller.sal.binding.api;

import java.util.Collection;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public abstract class AbstractBindingAwareProvider implements BindingAwareProvider, BundleActivator {

    @Override
    public final void start(BundleContext context) throws Exception {
            ServiceReference<BindingAwareBroker> brokerRef = context.getServiceReference(BindingAwareBroker.class);
            BindingAwareBroker broker = context.getService(brokerRef);
            
            ProviderContext ctx = broker.registerProvider(this, context);
            registerRpcImplementations(ctx);
            registerFunctionality(ctx);
            
            startImpl(context);
    }
    
    private void registerFunctionality(ProviderContext ctx) {
        Collection<? extends ProviderFunctionality> functionality = this.getFunctionality();
        if(functionality == null || functionality.isEmpty()) {
            return;
        }
        for (ProviderFunctionality providerFunctionality : functionality) {
            ctx.registerFunctionality(providerFunctionality);
        }
        
    }

    private void registerRpcImplementations(ProviderContext ctx) {
        Collection<? extends RpcService> rpcs = this.getImplementations();
        if(rpcs == null || rpcs.isEmpty()) {
            return;
        }
        for (RpcService rpcService : rpcs) {
            //ctx.addRpcImplementation(type, implementation);
        }
        
    }

    protected void startImpl(BundleContext context) {
        // NOOP
    }
    
    @Override
    public final void stop(BundleContext context) throws Exception {
            
            
    }
    
    @Override
    public Collection<? extends ProviderFunctionality> getFunctionality() {
        return null;
    }
    
    @Override
    public Collection<? extends RpcService> getImplementations() {
        return null;
    }
}
