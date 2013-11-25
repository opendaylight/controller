package org.opendaylight.controller.sal.binding.dom.serializer.impl

import org.opendaylight.controller.sal.binding.dom.serializer.impl.TransformerGenerator
import javassist.ClassPool
import org.opendaylight.yangtools.yang.model.api.SchemaContext
import org.opendaylight.controller.sal.core.api.model.SchemaServiceListener
import org.opendaylight.yangtools.sal.binding.generator.impl.BindingGeneratorImpl
import java.util.Map
import org.opendaylight.yangtools.sal.binding.model.api.Type
import org.opendaylight.yangtools.sal.binding.model.api.type.builder.GeneratedTypeBuilder
import org.opendaylight.yangtools.yang.model.api.SchemaNode
import java.util.concurrent.ConcurrentHashMap
import org.opendaylight.yangtools.yang.data.api.CompositeNode
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import java.util.Map.Entry
import java.util.AbstractMap.SimpleEntry
import org.opendaylight.yangtools.yang.model.api.SchemaPath
import org.opendaylight.yangtools.yang.model.util.SchemaContextUtil
import org.opendaylight.yangtools.yang.binding.DataContainer
import java.util.concurrent.ConcurrentMap
import org.opendaylight.yangtools.sal.binding.model.api.GeneratedType
import com.google.common.collect.HashMultimap
import com.google.common.util.concurrent.SettableFuture
import java.util.concurrent.Future
import org.opendaylight.yangtools.binding.generator.util.ReferencedTypeImpl
import org.opendaylight.controller.sal.binding.dom.serializer.impl.LazyGeneratedCodecRegistry
import org.opendaylight.controller.sal.binding.impl.connect.dom.BindingIndependentMappingService
import org.slf4j.LoggerFactory
import org.opendaylight.controller.sal.binding.dom.serializer.api.ValueWithQName
import org.opendaylight.controller.sal.binding.dom.serializer.api.DataContainerCodec
import org.opendaylight.yangtools.binding.generator.util.Types
import org.osgi.framework.BundleContext
import java.util.Hashtable
import org.osgi.framework.ServiceRegistration

class RuntimeGeneratedMappingServiceImpl implements BindingIndependentMappingService, SchemaServiceListener, AutoCloseable {

    @Property
    ClassPool pool;

    private static val LOG = LoggerFactory.getLogger(RuntimeGeneratedMappingServiceImpl);

    @Property
    extension TransformerGenerator binding;

    @Property
    extension LazyGeneratedCodecRegistry registry;

    @Property
    val ConcurrentMap<Type, Type> typeDefinitions = new ConcurrentHashMap();

    @Property
    val ConcurrentMap<Type, GeneratedTypeBuilder> typeToDefinition = new ConcurrentHashMap();

    @Property
    val ConcurrentMap<Type, SchemaNode> typeToSchemaNode = new ConcurrentHashMap();

    val promisedTypeDefinitions = HashMultimap.<Type, SettableFuture<GeneratedTypeBuilder>>create;

    val promisedSchemas = HashMultimap.<Type, SettableFuture<SchemaNode>>create;
    
    ServiceRegistration<SchemaServiceListener> listenerRegistration

    override onGlobalContextUpdated(SchemaContext arg0) {
        recreateBindingContext(arg0);
        registry.onGlobalContextUpdated(arg0);
    }

    def recreateBindingContext(SchemaContext schemaContext) {
        val newBinding = new BindingGeneratorImpl();
        newBinding.generateTypes(schemaContext);

        for (entry : newBinding.moduleContexts.entrySet) {

            registry.onModuleContextAdded(schemaContext, entry.key, entry.value);

            //val module = entry.key;
            val context = entry.value;
            updateBindingFor(context.childNodes, schemaContext);
            updateBindingFor(context.cases, schemaContext);
            

            val typedefs = context.typedefs;
            for (typedef : typedefs.values) {
                binding.typeDefinitions.put(typedef, typedef as GeneratedType);
            }
            val augmentations = context.augmentations;
            for (augmentation : augmentations) {
                binding.typeToDefinition.put(augmentation, augmentation);
            }
            
            binding.typeToAugmentation.putAll(context.typeToAugmentation);
        }
    }

    override CompositeNode toDataDom(DataObject data) {
        toCompositeNodeImpl(data);
    }

    override Entry<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> toDataDom(
        Entry<InstanceIdentifier<? extends DataObject>, DataObject> entry) {
        val key = toDataDom(entry.key)
        val data = toCompositeNodeImpl(entry.value);
        return new SimpleEntry(key, data);
    }

    private def CompositeNode toCompositeNodeImpl(DataObject object) {
        val cls = object.implementedInterface;
        waitForSchema(cls);
        val codec = registry.getCodecForDataObject(cls) as DataContainerCodec<DataObject>;
        val ret = codec.serialize(new ValueWithQName(null, object));
        return ret as CompositeNode;
    }

    private def waitForSchema(Class<? extends DataContainer> class1) {
        val ref = Types.typeForClass(class1);
        getSchemaWithRetry(ref);
    }

    override org.opendaylight.yangtools.yang.data.api.InstanceIdentifier toDataDom(
        InstanceIdentifier<? extends DataObject> path) {
        for (arg : path.path) {
            waitForSchema(arg.type);
        }
        return registry.instanceIdentifierCodec.serialize(path);
    }

    override dataObjectFromDataDom(InstanceIdentifier<? extends DataObject> path, CompositeNode node) {
        if (node == null) {
            return null;
        }
        val targetType = path.targetType
        val transformer = registry.getCodecForDataObject(targetType);
        val ret = transformer.deserialize(node)?.value as DataObject;
        return ret;
    }
    
    override fromDataDom(org.opendaylight.yangtools.yang.data.api.InstanceIdentifier entry) {
        return registry.instanceIdentifierCodec.deserialize(entry);
    }

    private def void updateBindingFor(Map<SchemaPath, GeneratedTypeBuilder> map, SchemaContext module) {
        for (entry : map.entrySet) {
            val schemaNode = SchemaContextUtil.findDataSchemaNode(module, entry.key);
            //LOG.info("{} : {}",entry.key,entry.value.fullyQualifiedName)
            if (schemaNode != null) {
                typeToSchemaNode.put(entry.value, schemaNode);
                typeToDefinition.put(entry.value, entry.value);
                updatePromisedSchemas(entry.value, schemaNode);
            }
        }
    }

    public def void start(BundleContext ctx) {
        binding = new TransformerGenerator(pool);
        registry = new LazyGeneratedCodecRegistry()
        registry.generator = binding

        //binding.staticFieldsInitializer = registry
        binding.listener = registry
        binding.typeToDefinition = typeToDefinition
        binding.typeToSchemaNode = typeToSchemaNode
        binding.typeDefinitions = typeDefinitions
        if(ctx !== null) {
            listenerRegistration = ctx.registerService(SchemaServiceListener,this,new Hashtable<String,String>());
        }
    }

    private def getTypeDefinition(Type type) {
        val typeDef = typeToDefinition.get(type);
        if (typeDef !== null) {
            return typeDef;
        }
        return type.getTypeDefInFuture.get();
    }

    private def Future<GeneratedTypeBuilder> getTypeDefInFuture(Type type) {
        val future = SettableFuture.<GeneratedTypeBuilder>create()
        promisedTypeDefinitions.put(type, future);
        return future;
    }

    private def void updatePromisedTypeDefinitions(GeneratedTypeBuilder builder) {
        val futures = promisedTypeDefinitions.get(builder);
        if (futures === null || futures.empty) {
            return;
        }
        for (future : futures) {
            future.set(builder);
        }
        promisedTypeDefinitions.removeAll(builder);
    }

    private def getSchemaWithRetry(Type type) {
        val typeDef = typeToSchemaNode.get(type);
        if (typeDef !== null) {
            return typeDef;
        }
        return type.getSchemaInFuture.get();
    }

    private def Future<SchemaNode> getSchemaInFuture(Type type) {
        val future = SettableFuture.<SchemaNode>create()
        promisedSchemas.put(type, future);
        return future;
    }

    private def void updatePromisedSchemas(Type builder, SchemaNode schema) {
        val ref = new ReferencedTypeImpl(builder.packageName, builder.name);
        val futures = promisedSchemas.get(ref);
        if (futures === null || futures.empty) {
            return;
        }
        for (future : futures) {
            future.set(schema);
        }
        promisedSchemas.removeAll(builder);
    }
    
    override close() throws Exception {
        listenerRegistration?.unregister();
    }
    
}
