package org.opendaylight.controller.config.yang.config.inventory_manager;
import org.opendaylight.controller.md.inventory.manager.FlowCapableInventoryProvider;
public class FlowCapableInventoryProviderModule extends org.opendaylight.controller.config.yang.config.inventory_manager.AbstractFlowCapableInventoryProviderModule {
    public FlowCapableInventoryProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public FlowCapableInventoryProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.inventory_manager.FlowCapableInventoryProviderModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        return new FlowCapableInventoryProvider(getDataBrokerDependency(), getNotificationServiceDependency());
    }

}
