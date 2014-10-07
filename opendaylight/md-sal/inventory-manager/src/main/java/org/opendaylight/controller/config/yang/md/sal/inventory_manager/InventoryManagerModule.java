package org.opendaylight.controller.config.yang.md.sal.inventory_manager;

import org.opendaylight.controller.md.inventory.manager.FlowCapableInventoryProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InventoryManagerModule extends org.opendaylight.controller.config.yang.md.sal.inventory_manager.AbstractInventoryManagerModule {
    private final static Logger LOG = LoggerFactory.getLogger(InventoryManagerModule.class);

    private FlowCapableInventoryProvider inventoryManager;

    public InventoryManagerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public InventoryManagerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, InventoryManagerModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {

    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        LOG.info("Inventory Manager module initialization.");
        inventoryManager = new FlowCapableInventoryProvider(getDataBrokerDependency(), getNotificationServiceDependency());
        inventoryManager.start();
        LOG.info("Inventory Manager module started successfully.");

        return new AutoCloseable() {
            @Override
            public void close() throws Exception {
                try {
                    inventoryManager.close();
                }
                catch (final Exception e) {
                    LOG.error("Unexpected error by stopping Inventory Manager", e);
                }
                LOG.info("Inventory Manager module stoped.");
            }
        };
    }
}
