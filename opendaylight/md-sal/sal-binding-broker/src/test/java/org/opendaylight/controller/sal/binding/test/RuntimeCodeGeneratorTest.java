package org.opendaylight.controller.sal.binding.test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javassist.ClassPool;

import org.junit.Before;
import org.junit.Test;

import static org.opendaylight.controller.sal.binding.codegen.RuntimeCodeHelper.*;

import org.opendaylight.controller.sal.binding.api.NotificationListener;
import org.opendaylight.controller.sal.binding.codegen.impl.RuntimeCodeGenerator;
import org.opendaylight.controller.sal.binding.spi.NotificationInvokerFactory;
import org.opendaylight.controller.sal.binding.spi.NotificationInvokerFactory.NotificationInvoker;
import org.opendaylight.controller.sal.binding.spi.RpcRouter;
import org.opendaylight.controller.sal.binding.spi.RpcRoutingTable;
import org.opendaylight.controller.sal.binding.test.mock.BarListener;
import org.opendaylight.controller.sal.binding.test.mock.BarUpdate;
import org.opendaylight.controller.sal.binding.test.mock.CompositeListener;
import org.opendaylight.controller.sal.binding.test.mock.FlowDelete;
import org.opendaylight.controller.sal.binding.test.mock.FooListener;
import org.opendaylight.controller.sal.binding.test.mock.FooService;
import org.opendaylight.controller.sal.binding.test.mock.FooUpdate;
import org.opendaylight.controller.sal.binding.test.mock.InheritedContextInput;
import org.opendaylight.controller.sal.binding.test.mock.ReferencableObject;
import org.opendaylight.controller.sal.binding.test.mock.ReferencableObjectKey;
import org.opendaylight.controller.sal.binding.test.mock.SimpleInput;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.BaseIdentity;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.IdentifiableItem;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument;

import static org.mockito.Mockito.*;

public class RuntimeCodeGeneratorTest {

    private RuntimeCodeGenerator codeGenerator;
    private NotificationInvokerFactory invokerFactory;

    @Before
    public void initialize() {
        this.codeGenerator = new RuntimeCodeGenerator(ClassPool.getDefault());
        this.invokerFactory = codeGenerator.getInvokerFactory();
    }

    @Test
    public void testGenerateDirectProxy() {
        FooService product = codeGenerator.getDirectProxyFor(FooService.class);
        assertNotNull(product);
    }

    @Test
    public void testGenerateRouter() throws Exception {
        RpcRouter<FooService> product = codeGenerator.getRouterFor(FooService.class);
        assertNotNull(product);
        assertNotNull(product.getInvocationProxy());

        assertNotNull(product.getSupportedInputs());
        assertTrue(product.getSupportedInputs().contains(SimpleInput.class));
        assertTrue(product.getSupportedInputs().contains(InheritedContextInput.class));
        assertEquals("2 fields should be generated.", 2, product.getInvocationProxy().getClass().getFields().length);

        verifyRouting(product);
    }

    @Test
    public void testInvoker() throws Exception {

        FooListenerImpl fooListener = new FooListenerImpl();

        NotificationInvoker invokerFoo = invokerFactory.invokerFor(fooListener);

        
        assertSame(fooListener,invokerFoo.getDelegate());
        assertNotNull(invokerFoo.getSupportedNotifications());
        assertEquals(1, invokerFoo.getSupportedNotifications().size());
        assertNotNull(invokerFoo.getInvocationProxy());

        FooUpdateImpl fooOne = new FooUpdateImpl();
        invokerFoo.getInvocationProxy().onNotification(fooOne);

        assertEquals(1, fooListener.receivedFoos.size());
        assertSame(fooOne, fooListener.receivedFoos.get(0));

        CompositeListenerImpl composite = new CompositeListenerImpl();

        NotificationInvoker invokerComposite = invokerFactory.invokerFor(composite);

        assertNotNull(invokerComposite.getSupportedNotifications());
        assertEquals(3, invokerComposite.getSupportedNotifications().size());
        assertNotNull(invokerComposite.getInvocationProxy());

        invokerComposite.getInvocationProxy().onNotification(fooOne);

        assertEquals(1, composite.receivedFoos.size());
        assertSame(fooOne, composite.receivedFoos.get(0));

        assertEquals(0, composite.receivedBars.size());

        BarUpdateImpl barOne = new BarUpdateImpl();

        invokerComposite.getInvocationProxy().onNotification(barOne);

        assertEquals(1, composite.receivedFoos.size());
        assertEquals(1, composite.receivedBars.size());
        assertSame(barOne, composite.receivedBars.get(0));

    }

    private void verifyRouting(RpcRouter<FooService> product) {
        assertNotNull("Routing table should be initialized", product.getRoutingTable(BaseIdentity.class));

        RpcRoutingTable<BaseIdentity, FooService> routingTable = product.getRoutingTable(BaseIdentity.class);

        int servicesCount = 2;
        int instancesPerService = 3;

        InstanceIdentifier<?>[][] identifiers = identifiers(servicesCount, instancesPerService);
        FooService service[] = new FooService[] { mock(FooService.class, "Instance 0"),
                mock(FooService.class, "Instance 1") };

        for (int i = 0; i < service.length; i++) {
            for (InstanceIdentifier<?> instance : identifiers[i]) {
                routingTable.updateRoute(instance, service[i]);
            }
        }

        assertEquals("All instances should be registered.", servicesCount * instancesPerService, routingTable
                .getRoutes().size());

        SimpleInput[] instance_0_input = new SimpleInputImpl[] { new SimpleInputImpl(identifiers[0][0]),
                new SimpleInputImpl(identifiers[0][1]), new SimpleInputImpl(identifiers[0][2]) };

        SimpleInput[] instance_1_input = new SimpleInputImpl[] { new SimpleInputImpl(identifiers[1][0]),
                new SimpleInputImpl(identifiers[1][1]), new SimpleInputImpl(identifiers[1][2]) };

        // We test sending mock messages

        product.getInvocationProxy().simple(instance_0_input[0]);
        verify(service[0]).simple(instance_0_input[0]);

        product.getInvocationProxy().simple(instance_0_input[1]);
        product.getInvocationProxy().simple(instance_0_input[2]);

        verify(service[0]).simple(instance_0_input[1]);
        verify(service[0]).simple(instance_0_input[2]);

        product.getInvocationProxy().simple(instance_1_input[0]);

        // We should have call to instance 1
        verify(service[1]).simple(instance_1_input[0]);
    }

    private InstanceIdentifier<?>[][] identifiers(int serviceSize, int instancesPerService) {
        InstanceIdentifier<?>[][] ret = new InstanceIdentifier[serviceSize][];
        int service = 0;
        for (int i = 0; i < serviceSize; i++) {

            InstanceIdentifier<?>[] instanceIdentifiers = new InstanceIdentifier[instancesPerService];
            ret[i] = instanceIdentifiers;
            for (int id = 0; id < instancesPerService; id++) {
                instanceIdentifiers[id] = referencableIdentifier(service * instancesPerService + id);
            }
            service++;
        }

        return ret;
    }

    private InstanceIdentifier<?> referencableIdentifier(int i) {
        ReferencableObjectKey key = new ReferencableObjectKey(i);
        IdentifiableItem<ReferencableObject, ReferencableObjectKey> pathArg = new IdentifiableItem<>(
                ReferencableObject.class, key);
        return new InstanceIdentifier<ReferencableObject>(Arrays.<PathArgument> asList(pathArg),
                ReferencableObject.class);
    }

    private static class SimpleInputImpl implements SimpleInput {
        private final InstanceIdentifier<?> identifier;

        public SimpleInputImpl(InstanceIdentifier<?> _identifier) {
            this.identifier = _identifier;
        }

        @Override
        public <E extends Augmentation<SimpleInput>> E getAugmentation(Class<E> augmentationType) {
            return null;
        }

        @Override
        public InstanceIdentifier<?> getIdentifier() {
            return this.identifier;
        }

        @Override
        public Class<? extends DataObject> getImplementedInterface() {
            return SimpleInput.class;
        }
    }

    private static class FooUpdateImpl implements FooUpdate {
        @Override
        public Class<? extends DataContainer> getImplementedInterface() {
            return FooUpdate.class;
        }
    }

    private static class BarUpdateImpl implements BarUpdate {
        @Override
        public Class<? extends DataContainer> getImplementedInterface() {
            return BarUpdate.class;
        }

        @Override
        public InstanceIdentifier<?> getInheritedIdentifier() {
            return null;
        }
    }

    private static class FooListenerImpl implements FooListener {

        List<FooUpdate> receivedFoos = new ArrayList<>();

        @Override
        public void onFooUpdate(FooUpdate notification) {
            receivedFoos.add(notification);
        }

    }

    private static class CompositeListenerImpl extends FooListenerImpl implements BarListener {

        List<BarUpdate> receivedBars = new ArrayList<>();
        List<FlowDelete> receivedDeletes = new ArrayList<>();

        @Override
        public void onBarUpdate(BarUpdate notification) {
            receivedBars.add(notification);
        }

        @Override
        public void onFlowDelete(FlowDelete notification) {
            receivedDeletes.add(notification);
        }

    }
}
