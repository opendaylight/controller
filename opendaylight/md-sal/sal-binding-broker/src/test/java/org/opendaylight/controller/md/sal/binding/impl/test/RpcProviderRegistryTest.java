package org.opendaylight.controller.md.sal.binding.impl.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.TOP_BAR_KEY;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.TOP_FOO_KEY;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.path;

import org.opendaylight.controller.md.sal.binding.compat.HeliumRpcProviderRegistry;

import com.google.common.base.Throwables;
import java.util.Arrays;
import javassist.ClassPool;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.impl.BindingDOMRpcProviderServiceAdapter;
import org.opendaylight.controller.md.sal.binding.impl.BindingDOMRpcServiceAdapter;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.controller.md.sal.binding.test.AbstractSchemaAwareTest;
import org.opendaylight.controller.md.sal.dom.broker.impl.DOMRpcRouter;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.api.RpcConsumerRegistry;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.sal.binding.api.rpc.RpcContextIdentifier;
import org.opendaylight.controller.sal.binding.codegen.RpcIsNotRoutedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.bi.ba.rpcservice.rev140701.OpendaylightTestRpcServiceService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.rpc.routing.rev140701.OpendaylightTestRoutedRpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.rpc.routing.rev140701.TestContext;
import org.opendaylight.yangtools.binding.data.codec.gen.impl.DataObjectSerializerGenerator;
import org.opendaylight.yangtools.binding.data.codec.gen.impl.StreamWriterGenerator;
import org.opendaylight.yangtools.binding.data.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.yangtools.sal.binding.generator.impl.GeneratedClassLoadingStrategy;
import org.opendaylight.yangtools.sal.binding.generator.util.JavassistUtils;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;


public class RpcProviderRegistryTest  extends AbstractSchemaAwareTest {

    private static InstanceIdentifier<TopLevelList> FOO_PATH = path(TOP_FOO_KEY);
    private static InstanceIdentifier<TopLevelList> BAR_PATH = path(TOP_BAR_KEY);
    private static RpcContextIdentifier ROUTING_CONTEXT = RpcContextIdentifier.contextFor(OpendaylightTestRoutedRpcService.class, TestContext.class);

    private RpcProviderRegistry rpcRegistry;


    @Override
    protected Iterable<YangModuleInfo> getModuleInfos() {
        try {
            return Arrays.asList(
                    BindingReflections.getModuleInfo(TopLevelList.class),
                    BindingReflections.getModuleInfo(OpendaylightTestRoutedRpcService.class),
                    BindingReflections.getModuleInfo(OpendaylightTestRpcServiceService.class));
        } catch (final Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    protected void setupWithSchema(final SchemaContext context) {
        final DataObjectSerializerGenerator generator = StreamWriterGenerator.create(JavassistUtils.forClassPool(ClassPool.getDefault()));
        final BindingNormalizedNodeCodecRegistry codecRegistry = new BindingNormalizedNodeCodecRegistry(generator);
        final GeneratedClassLoadingStrategy classLoadingStrategy = GeneratedClassLoadingStrategy.getTCCLClassLoadingStrategy();
        final BindingToNormalizedNodeCodec codec = new BindingToNormalizedNodeCodec(classLoadingStrategy, null, codecRegistry);
        final DOMRpcRouter domRpcRegistry = new DOMRpcRouter();
        domRpcRegistry.onGlobalContextUpdated(context);
        codec.onGlobalContextUpdated(context);
        final RpcConsumerRegistry consumer = new BindingDOMRpcServiceAdapter(domRpcRegistry, codec);
        final BindingDOMRpcProviderServiceAdapter provider = new BindingDOMRpcProviderServiceAdapter( domRpcRegistry,codec);
        rpcRegistry = new HeliumRpcProviderRegistry(consumer,provider);
    }

    @Test
    public void testGlobalRpcRegistrations() throws Exception {
        final OpendaylightTestRpcServiceService one = Mockito.mock(OpendaylightTestRpcServiceService.class);
        final OpendaylightTestRpcServiceService two = Mockito.mock(OpendaylightTestRpcServiceService.class);

        final RpcRegistration<OpendaylightTestRpcServiceService> regOne = rpcRegistry.addRpcImplementation(OpendaylightTestRpcServiceService.class, one);
        assertNotNull(regOne);
        rpcRegistry.addRpcImplementation(OpendaylightTestRpcServiceService.class, two);
        regOne.close();
        final RpcRegistration<OpendaylightTestRpcServiceService> regTwo = rpcRegistry.addRpcImplementation(OpendaylightTestRpcServiceService.class, two);
        assertNotNull(regTwo);
    }


    @Test
    @Ignore
    public void nonRoutedRegisteredAsRouted() {
        final OpendaylightTestRpcServiceService one = Mockito.mock(OpendaylightTestRpcServiceService.class);
        try {
            final RoutedRpcRegistration<OpendaylightTestRpcServiceService> reg = rpcRegistry.addRoutedRpcImplementation(OpendaylightTestRpcServiceService.class, one);
            reg.registerPath(null, BAR_PATH);
            fail("RpcIsNotRoutedException should be thrown");
        } catch (final RpcIsNotRoutedException e) {
            assertNotNull(e.getMessage());
        } catch (final Exception e) {
            fail("RpcIsNotRoutedException should be thrown");
        }

    }

}
