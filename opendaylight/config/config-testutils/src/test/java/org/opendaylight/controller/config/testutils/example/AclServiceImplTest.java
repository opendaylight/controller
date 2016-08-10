/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.testutils.example;

public class AclServiceImplTest /* extends AbstractAclServiceTest */ {
/*
    @Module
    static class TestDependenciesModule extends AbstractBindingAndConfigTestModule {

        @Provides
        @Singleton
        ModuleFactory aclServiceImplModuleFactory(OdlInterfaceRpcService odlInterfaceRpcService) {
            // We must depend on OdlInterfaceRpcService, even if we don't directly use it,
            // because it is actually used, in AclServiceProvider, but through dynamic lookup instead of static.
            return null; // new AclServiceImplModuleFactory();
        }

        @Provides
        @Singleton
        OdlInterfaceRpcService odlInterfaceRpcService(ObjectRegistry.Builder registry) {
            // Using "classical" Mockito here (could also implement this using
            // Mikito; useful if more complex; both are perfectly possible).
            OdlInterfaceRpcService odlInterfaceRpcService = mock(OdlInterfaceRpcService.class, EXCEPTION_ANSWER);
            Future<RpcResult<GetDpidFromInterfaceOutput>> result = RpcResultBuilder
                    .success(new GetDpidFromInterfaceOutputBuilder().setDpid(new BigInteger("123"))).buildFuture();
            doReturn(result).when(odlInterfaceRpcService).getDpidFromInterface(any());
            registry.putInstance(odlInterfaceRpcService, OdlInterfaceRpcService.class);
            return odlInterfaceRpcService;
        }

        @Provides
        @Singleton
        TestIMdsalApiManager fakeMdsalApiManager(ObjectRegistry.Builder registry) {
            TestIMdsalApiManager mdsalApiManager = Mikito.stub(TestIMdsalApiManager.class);
            registry.putInstance(mdsalApiManager, IMdsalApiManager.class);
            return mdsalApiManager;
        }

        @Provides
        @Singleton
        IMdsalApiManager mdsalApiManager(TestIMdsalApiManager fake) {
            return fake;
        }
    }

    @Singleton
    @Component(modules = { TestDependenciesModule.class, DataBrokerTestModule.class })
    interface Configuration extends MembersInjector<AclServiceImplTest> {
        @Override
        void injectMembers(AclServiceImplTest test);
    }

    @Rule public InjectorRule injector = new InjectorRule(DaggerAclServiceImplTest_Configuration.create());
*/
}
