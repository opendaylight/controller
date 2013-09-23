package org.opendaylight.controller.sal.binding.test;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javassist.ClassPool;

import org.junit.Before;
import org.junit.Test;
import static org.opendaylight.controller.sal.binding.codegen.RuntimeCodeHelper.*;
import org.opendaylight.controller.sal.binding.codegen.impl.RuntimeCodeGenerator;
import org.opendaylight.controller.sal.binding.test.mock.FooService;
import org.opendaylight.controller.sal.binding.test.mock.ReferencableObject;
import org.opendaylight.controller.sal.binding.test.mock.ReferencableObjectKey;
import org.opendaylight.controller.sal.binding.test.mock.SimpleInput;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.BaseIdentity;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.IdentifiableItem;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument;

import static org.mockito.Mockito.*;


public class RuntimeCodeGeneratorTest {

    private RuntimeCodeGenerator codeGenerator;

    
    @Before
    public void initialize() {
        this.codeGenerator = new RuntimeCodeGenerator(ClassPool.getDefault());
    }
    
    @Test
    public void testGenerateDirectProxy() {
        Class<? extends FooService> product = codeGenerator.generateDirectProxy(FooService.class);
        assertNotNull(product);
    }

    @Test
    public void testGenerateRouter() throws Exception {
        Class<? extends FooService> product = codeGenerator.generateRouter(FooService.class);
        assertNotNull(product);
        assertNotNull(product.getSimpleName());
        assertEquals("2 fields should be generated.",2,product.getFields().length);
        
        verifyRouting(product.newInstance());
    }

    private void verifyRouting(FooService product) {
        Map<InstanceIdentifier,FooService> routingTable = new HashMap<>();
        setRoutingTable(product, BaseIdentity.class, routingTable);
        
        assertSame("Returned routing table should be same instance",routingTable,getRoutingTable(product, BaseIdentity.class));
        
        int servicesCount = 2;
        int instancesPerService = 3;
        
        InstanceIdentifier[][] identifiers = identifiers(servicesCount,instancesPerService);
        FooService service[] = new FooService[] {
                mock(FooService.class, "Instance 0"),
                mock(FooService.class,"Instance 1")
        };
        
        for(int i = 0;i<service.length;i++) {
            for (InstanceIdentifier instance : identifiers[i]) {
                routingTable.put(instance, service[i]);
            }
        }
        
        assertEquals("All instances should be registered.", servicesCount*instancesPerService, routingTable.size());
        
        SimpleInput[] instance_0_input = new SimpleInputImpl[] {
            new SimpleInputImpl(identifiers[0][0]),
            new SimpleInputImpl(identifiers[0][1]),
            new SimpleInputImpl(identifiers[0][2])
        };
        
        SimpleInput[] instance_1_input = new SimpleInputImpl[] {
                new SimpleInputImpl(identifiers[1][0]),
                new SimpleInputImpl(identifiers[1][1]),
                new SimpleInputImpl(identifiers[1][2])
        };
        
        // We test sending mock messages
        
        product.simple(instance_0_input[0]);
        verify(service[0]).simple(instance_0_input[0]);
        
        product.simple(instance_0_input[1]);
        product.simple(instance_0_input[2]);
        
        verify(service[0]).simple(instance_0_input[1]);
        verify(service[0]).simple(instance_0_input[2]);
        
        product.simple(instance_1_input[0]);
        verify(service[1]).simple(instance_1_input[0]);
    }

    private InstanceIdentifier[][] identifiers(int serviceSize, int instancesPerService) {
        InstanceIdentifier[][] ret = new InstanceIdentifier[serviceSize][];
        int service = 0;
        for (int i = 0;i<serviceSize;i++) {
            
            InstanceIdentifier[] instanceIdentifiers = new InstanceIdentifier[instancesPerService];
            ret[i] = instanceIdentifiers;
            for(int id = 0;id<instancesPerService;id++) {
                instanceIdentifiers[id] = referencableIdentifier(service*instancesPerService+id);
            }
            service++;
        }
        
        return ret;
    }

    private InstanceIdentifier referencableIdentifier(int i) {
        ReferencableObjectKey key = new ReferencableObjectKey(i);
        IdentifiableItem<ReferencableObject,ReferencableObjectKey> pathArg = new IdentifiableItem<>(ReferencableObject.class,key);
        return new InstanceIdentifier(Arrays.<PathArgument>asList(pathArg), ReferencableObject.class);
    }

    private static class SimpleInputImpl implements SimpleInput {
        private final InstanceIdentifier identifier;

        public SimpleInputImpl(InstanceIdentifier _identifier) {
            this.identifier = _identifier;
        }

        @Override
        public <E extends Augmentation<SimpleInput>> E getAugmentation(Class<E> augmentationType) {
            return null;
        }

        @Override
        public InstanceIdentifier getIdentifier() {
            return this.identifier;
        }
    }
}
