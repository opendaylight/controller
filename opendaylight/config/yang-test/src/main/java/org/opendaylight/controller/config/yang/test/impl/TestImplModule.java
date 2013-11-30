package org.opendaylight.controller.config.yang.test.impl;

/**
*
*/
public final class TestImplModule extends org.opendaylight.controller.config.yang.test.impl.AbstractTestImplModule
 {

    public TestImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public TestImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            TestImplModule oldModule, java.lang.AutoCloseable oldInstance) {

        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    protected void customValidation(){
        // Add custom validation for module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        return new AutoCloseable() {
            @Override
            public void close() throws Exception {
            }
        };
    }
}
