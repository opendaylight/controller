package org.opendaylight.controller.blacklist



import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareProvider
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext
import org.osgi.framework.BundleContext

class BlackListManager extends AbstractBindingAwareProvider {

var DisconnectSwitchImpl disconnectSwitchImpl;
    override onSessionInitiated(ProviderContext session) {
    disconnectSwitchImpl.onSessionInitiated(session);
    }

       override startImpl(BundleContext ctx) {
           super.startImpl(ctx);
           disconnectSwitchImpl = new DisconnectSwitchImpl(ctx);
           }
}
