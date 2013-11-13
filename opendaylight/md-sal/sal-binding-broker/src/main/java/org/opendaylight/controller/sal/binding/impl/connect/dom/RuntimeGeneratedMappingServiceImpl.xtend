package org.opendaylight.controller.sal.binding.impl.connect.dom

import org.opendaylight.controller.sal.binding.dom.serializer.impl.TransformerGenerator
import javassist.ClassPool
import org.opendaylight.yangtools.yang.model.api.SchemaContext
import org.opendaylight.controller.sal.core.api.model.SchemaServiceListener
import org.opendaylight.yangtools.sal.binding.generator.impl.BindingGeneratorImpl
import java.util.Map
import org.opendaylight.yangtools.sal.binding.model.api.Type
import org.opendaylight.yangtools.sal.binding.model.api.type.builder.GeneratedTypeBuilder
import org.opendaylight.yangtools.yang.model.api.SchemaNode
import java.util.HashMap
import java.util.concurrent.ConcurrentHashMap
import org.opendaylight.yangtools.yang.data.api.CompositeNode
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import java.util.Map.Entry
import java.util.AbstractMap.SimpleEntry
import org.opendaylight.yangtools.yang.model.api.SchemaPath
import org.opendaylight.yangtools.yang.model.util.SchemaContextUtil
import java.util.ArrayList
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.IdentifiableItem
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode
import org.opendaylight.yangtools.binding.generator.util.Types
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifierWithPredicates
import org.opendaylight.yangtools.yang.common.QName
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.Item
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifier
import org.opendaylight.yangtools.yang.binding.DataContainer
import static com.google.common.base.Preconditions.*;
import java.util.List
import org.opendaylight.yangtools.yang.data.api.Node
import org.opendaylight.yangtools.yang.data.impl.CompositeNodeTOImpl
import org.opendaylight.yangtools.yang.data.impl.SimpleNodeTOImpl
import org.opendaylight.yangtools.concepts.Delegator
import java.util.concurrent.ConcurrentMap
import org.opendaylight.yangtools.sal.binding.model.api.GeneratedType
import org.opendaylight.yangtools.yang.binding.BindingCodec

class RuntimeGeneratedMappingServiceImpl implements BindingIndependentMappingService, SchemaServiceListener {

    ClassPool pool;

    @Property
    extension TransformerGenerator binding;
    
    val ConcurrentMap<Type, Type> typeDefinitions = new ConcurrentHashMap();

    val ConcurrentMap<Class<? extends DataContainer>, TransformerWrapper> domSerializers = new ConcurrentHashMap();

    @Property
    val ConcurrentMap<Type, GeneratedTypeBuilder> typeToDefinition = new ConcurrentHashMap();

    @Property
    val ConcurrentMap<Type, SchemaNode> typeToSchemaNode = new ConcurrentHashMap();

    override onGlobalContextUpdated(SchemaContext arg0) {
        recreateBindingContext(arg0);
    }

    def recreateBindingContext(SchemaContext schemaContext) {
        val newBinding = new BindingGeneratorImpl();
        newBinding.generateTypes(schemaContext);

        for (entry : newBinding.moduleContexts.entrySet) {

            //val module = entry.key;
            val context = entry.value;
            updateBindingFor(context.childNodes, schemaContext);
            
            val typedefs = context.typedefs;
            for(typedef : typedefs.values) {
                binding.typeDefinitions.put(typedef,typedef as GeneratedType);
            }
        }
    }

    override CompositeNode toDataDom(DataObject data) {
        toCompositeNodeImpl(data);
    }

    override Entry<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> toDataDom(
        Entry<InstanceIdentifier<? extends DataObject>, DataObject> entry) {
        val key = toDataDomImpl(entry.key);
        val data = toCompositeNodeImpl(entry.value);
        return new SimpleEntry(key, data);
    }

    private def CompositeNode toCompositeNodeImpl(DataObject object) {
        val cls = object.implementedInterface;
        val transformator = resolveTransformator(cls);
        val ret = transformator.transform(object);
        return ret;
    }

    private def resolveTransformator(Class<? extends DataContainer> cls) {
        val serializer = domSerializers.get(cls);
        if (serializer !== null) {
            return serializer;
        }
        val transformerClass = binding.transformerFor(cls).newInstance;
        val wrapper = new TransformerWrapper(transformerClass);
        domSerializers.putIfAbsent(cls, wrapper);
        return wrapper;
    }

    private def org.opendaylight.yangtools.yang.data.api.InstanceIdentifier toDataDomImpl(
        InstanceIdentifier<? extends DataObject> object) {
        val pathArguments = object.path;
        var Class<? extends DataObject> parent;
        val dataDomArgs = new ArrayList<PathArgument>();
        for (pathArgument : pathArguments) {
            dataDomArgs.add(pathArgument.toDataDomPathArgument(parent));
            parent = pathArgument.type;
        }

        return new org.opendaylight.yangtools.yang.data.api.InstanceIdentifier(dataDomArgs);
    }

    override org.opendaylight.yangtools.yang.data.api.InstanceIdentifier toDataDom(
        InstanceIdentifier<? extends DataObject> path) {
        return toDataDomImpl(path);
    }

    override dataObjectFromDataDom(InstanceIdentifier<? extends DataObject> path, CompositeNode result) {
        return dataObjectFromDataDomImpl(path, result);
    }

    def DataObject dataObjectFromDataDomImpl(InstanceIdentifier<? extends DataObject> identifier, CompositeNode node) {
        val targetType = identifier.targetType
        val transformer = resolveTransformator(targetType);
        val ret = transformer.deserialize(node) as DataObject;
        return ret;
    }

    def void updateBindingFor(Map<SchemaPath, GeneratedTypeBuilder> map, SchemaContext module) {
        for (entry : map.entrySet) {
            val schemaNode = SchemaContextUtil.findDataSchemaNode(module, entry.key);
            typeToDefinition.put(entry.value, entry.value);
            typeToSchemaNode.put(entry.value, schemaNode)
        }
    }

    private def dispatch PathArgument toDataDomPathArgument(IdentifiableItem argument,
        Class<? extends DataObject> parent) {
        val Class<?> rawType = argument.type;
        val ref = Types.typeForClass(rawType);
        val schemaType = typeToSchemaNode.get(ref);
        val qname = schemaType.QName

        val Object key = argument.key;
        val predicates = key.toPredicates(schemaType as ListSchemaNode);

        return new NodeIdentifierWithPredicates(qname, predicates);
    }

    private def Map<QName, Object> toPredicates(Object identifier, ListSchemaNode node) {
        val keyDefinitions = node.keyDefinition;
        val map = new HashMap<QName, Object>();
        for (keydef : keyDefinitions) {
            val keyNode = node.getDataChildByName(keydef) as LeafSchemaNode;
            val value = BindingMapping.getSimpleValue(identifier, keydef, keyNode.type);
            map.put(keydef, value.value);
        }
        return map;
    }

    private def dispatch PathArgument toDataDomPathArgument(Item<?> argument, Class<? extends DataObject> parent) {
        val ref = Types.typeForClass(argument.type);
        val qname = typeToSchemaNode.get(ref).QName
        return new NodeIdentifier(qname);
    }

    public def void start() {
        pool = new ClassPool()
        binding = new TransformerGenerator(pool);

        binding.typeToDefinition = typeToDefinition
        binding.typeToSchemaNode = typeToSchemaNode
        binding.typeDefinitions = typeDefinitions

    }
}

class TransformerWrapper implements // //
Delegator<BindingCodec<Map<QName, Object>, Object>> {

    @Property
    val BindingCodec<Map<QName, Object>, Object> delegate;

    new(BindingCodec<Map<QName, Object>, Object> delegate) {
        _delegate = delegate;
    }

    def CompositeNode transform(DataObject input) {
        val ret = delegate.serialize(input);
        val node = toNode(ret)
        return node as CompositeNode;
    }

    def deserialize(CompositeNode node) {
        if (node === null) {
            return null;
        }
        val Map mapCapture = node
        return delegate.deserialize(mapCapture as Map<QName,Object>);
    }

    static def Node<?> toNode(Map map) {
        val nodeMap = map as Map<QName,Object>;
        checkArgument(map.size == 1);
        val elem = nodeMap.entrySet.iterator.next;
        val qname = elem.key;
        val value = elem.value;
        toNodeImpl(qname, value);
    }

    static def dispatch Node<?> toNodeImpl(QName name, List objects) {
        val values = new ArrayList<Node<?>>(objects.size);
        for (obj : objects) {
            values.add(toNode(obj as Map));
        }
        return new CompositeNodeTOImpl(name, null, values);
    }

    static def dispatch Node<?> toNodeImpl(QName name, Map<QName, Object> object) {
        throw new UnsupportedOperationException("Unsupported node hierarchy.");
    }

    static def dispatch Node<?> toNodeImpl(QName name, Object object) {
        return new SimpleNodeTOImpl(name, null, object);
    }
}
