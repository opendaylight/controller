package org.opendaylight.controller.config.yang.config.distributed_entity_ownership_service;
public class DistributedEntityOwnershipServiceProviderModule extends org.opendaylight.controller.config.yang.config.distributed_entity_ownership_service.AbstractDistributedEntityOwnershipServiceProviderModule {
    public DistributedEntityOwnershipServiceProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public DistributedEntityOwnershipServiceProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.distributed_entity_ownership_service.DistributedEntityOwnershipServiceProviderModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        // TODO:implement
        throw new java.lang.UnsupportedOperationException();
    }

}
