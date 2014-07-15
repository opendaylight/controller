package org.opendaylight.controller.md.sal.binding.impl.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.TOP_BAR_KEY;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.TOP_FOO_KEY;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.path;

import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.test.AssertCollections;
import org.opendaylight.controller.md.sal.common.api.routing.RouteChange;
import org.opendaylight.controller.md.sal.common.api.routing.RouteChangeListener;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.api.rpc.RpcContextIdentifier;
import org.opendaylight.controller.sal.binding.api.rpc.RpcRouter;
import org.opendaylight.controller.sal.binding.impl.RpcProviderRegistryImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.bi.ba.rpcservice.rev140701.OpendaylightTestRpcServiceService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.rpc.routing.rev140701.OpendaylightTestRoutedRpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.rpc.routing.rev140701.TestContext;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.util.concurrent.SettableFuture;


public class RpcProviderRegistryTest {

    private static InstanceIdentifier<TopLevelList> FOO_PATH = path(TOP_FOO_KEY);
    private static InstanceIdentifier<TopLevelList> BAR_PATH = path(TOP_BAR_KEY);
    private static RpcContextIdentifier ROUTING_CONTEXT = RpcContextIdentifier.contextFor(OpendaylightTestRoutedRpcService.class, TestContext.class);

    private RpcProviderRegistryImpl rpcRegistry;

    @Before
    public void setup() {
        rpcRegistry = new RpcProviderRegistryImpl("test");
    }

    private static class TestListener implements RouteChangeListener<RpcContextIdentifier, InstanceIdentifier<?>> {

        final SettableFuture<RouteChange<RpcContextIdentifier, InstanceIdentifier<?>>> event = SettableFuture.create();
        @Override
        public void onRouteChange(
                final RouteChange<RpcContextIdentifier, InstanceIdentifier<?>> change) {
            event.set(change);
        }
    }

    @Test
    public void testGlobalRpcRegistrations() throws Exception {
        OpendaylightTestRpcServiceService one = Mockito.mock(OpendaylightTestRpcServiceService.class);
        OpendaylightTestRpcServiceService two = Mockito.mock(OpendaylightTestRpcServiceService.class);

        RpcRegistration<OpendaylightTestRpcServiceService> regOne = rpcRegistry.addRpcImplementation(OpendaylightTestRpcServiceService.class, one);
        assertNotNull(regOne);

        try {
            rpcRegistry.addRpcImplementation(OpendaylightTestRpcServiceService.class, two);
        fail("Second call for registration of same RPC must throw IllegalStateException");
        } catch (IllegalStateException e) {
            assertNotNull(e.getMessage());
        }

        regOne.close();

        RpcRegistration<OpendaylightTestRpcServiceService> regTwo = rpcRegistry.addRpcImplementation(OpendaylightTestRpcServiceService.class, two);
        assertNotNull(regTwo);
    }

    @Test
    public void testRpcRouterInstance() throws Exception  {
        OpendaylightTestRoutedRpcService def = Mockito.mock(OpendaylightTestRoutedRpcService.class);

        RpcRouter<OpendaylightTestRoutedRpcService> router = rpcRegistry.getRpcRouter(OpendaylightTestRoutedRpcService.class);

        assertEquals(OpendaylightTestRoutedRpcService.class, router.getServiceType());
        assertNotNull(router.getInvocationProxy());
        assertNull(router.getDefaultService());

        AssertCollections.assertContains(router.getContexts(), TestContext.class);

        RpcRegistration<OpendaylightTestRoutedRpcService> regDef = router.registerDefaultService(def);
        assertNotNull(regDef);
        assertEquals(OpendaylightTestRoutedRpcService.class,regDef.getServiceType());
        assertEquals(def,regDef.getInstance());
        assertEquals(def, router.getDefaultService());

        regDef.close();
        assertNull("Default instance should be null after closing registration",  router.getDefaultService());
    }

    @Test
    public void testRoutedRpcPathChangeEvents() throws InterruptedException, TimeoutException, ExecutionException {
        OpendaylightTestRoutedRpcService one = Mockito.mock(OpendaylightTestRoutedRpcService.class);
        OpendaylightTestRoutedRpcService two = Mockito.mock(OpendaylightTestRoutedRpcService.class);
        RoutedRpcRegistration<OpendaylightTestRoutedRpcService> regOne = rpcRegistry.addRoutedRpcImplementation(OpendaylightTestRoutedRpcService.class, one);
        RoutedRpcRegistration<OpendaylightTestRoutedRpcService> regTwo = rpcRegistry.addRoutedRpcImplementation(OpendaylightTestRoutedRpcService.class, two);
        assertNotNull(regOne);
        assertNotNull(regTwo);

        final TestListener addListener = new TestListener();
        rpcRegistry.registerRouteChangeListener(addListener);
        regOne.registerPath(TestContext.class, FOO_PATH);

        RouteChange<RpcContextIdentifier, InstanceIdentifier<?>> fooAddEvent = addListener.event.get(500, TimeUnit.MILLISECONDS);
        Set<InstanceIdentifier<?>> announce = fooAddEvent.getAnnouncements().get(ROUTING_CONTEXT);
        assertNotNull(announce);
        AssertCollections.assertContains(announce, FOO_PATH);
        AssertCollections.assertNotContains(announce, BAR_PATH);



        final TestListener removeListener = new TestListener();
        rpcRegistry.registerRouteChangeListener(removeListener);

        regOne.unregisterPath(TestContext.class, FOO_PATH);

        RouteChange<RpcContextIdentifier, InstanceIdentifier<?>> fooRemoveEvent = removeListener.event.get(500, TimeUnit.MILLISECONDS);
        Set<InstanceIdentifier<?>> removal = fooRemoveEvent.getRemovals().get(ROUTING_CONTEXT);
        assertNotNull(removal);
        AssertCollections.assertContains(removal, FOO_PATH);
        AssertCollections.assertNotContains(removal, BAR_PATH);


    }

}
