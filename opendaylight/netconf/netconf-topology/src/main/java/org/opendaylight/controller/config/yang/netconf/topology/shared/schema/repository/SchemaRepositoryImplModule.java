package org.opendaylight.controller.config.yang.netconf.topology.shared.schema.repository;

import org.opendaylight.controller.netconf.topology.SchemaRepositoryProvider;
import org.opendaylight.yangtools.yang.parser.repo.SharedSchemaRepository;

public class SchemaRepositoryImplModule extends org.opendaylight.controller.config.yang.netconf.topology.shared.schema.repository.AbstractSchemaRepositoryImplModule {

    public SchemaRepositoryImplModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public SchemaRepositoryImplModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, final org.opendaylight.controller.config.yang.netconf.topology.shared.schema.repository.SchemaRepositoryImplModule oldModule, final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        return new SchemaRepositoryProviderAutoCloseAble(this);
    }

    private static class SchemaRepositoryProviderAutoCloseAble implements SchemaRepositoryProvider, AutoCloseable {

        private final SharedSchemaRepository schemaRepository;

        public SchemaRepositoryProviderAutoCloseAble(final SchemaRepositoryImplModule module) {
            schemaRepository = new SharedSchemaRepository(module.getIdentifier().getInstanceName());
        }

        @Override
        public void close() throws Exception {
            //NOOP
        }

        @Override
        public SharedSchemaRepository getSharedSchemaRepository() {
            return schemaRepository;
        }
    }

}
