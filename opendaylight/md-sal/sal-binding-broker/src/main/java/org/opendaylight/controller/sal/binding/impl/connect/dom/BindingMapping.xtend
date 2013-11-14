package org.opendaylight.controller.sal.binding.impl.connect.dom

import org.opendaylight.yangtools.yang.common.QName
import org.opendaylight.yangtools.sal.binding.generator.impl.ModuleContext
import java.util.List
import org.opendaylight.yangtools.sal.binding.model.api.type.builder.GeneratedTypeBuilder
import org.opendaylight.yangtools.sal.binding.model.api.Type
import org.opendaylight.yangtools.yang.model.api.SchemaNode
import java.util.Map
import org.opendaylight.yangtools.yang.model.api.SchemaPath
import org.opendaylight.yangtools.yang.model.api.SchemaContext
import org.opendaylight.yangtools.yang.model.util.SchemaContextUtil
import org.opendaylight.yangtools.binding.generator.util.Types
import java.util.HashMap
import org.opendaylight.yangtools.yang.data.api.CompositeNode
import org.opendaylight.yangtools.yang.binding.DataContainer
import org.opendaylight.yangtools.yang.data.impl.CompositeNodeTOImpl
import org.opendaylight.yangtools.sal.binding.model.api.GeneratedProperty
import org.opendaylight.yangtools.sal.binding.model.api.GeneratedType
import java.util.Collections
import java.util.ArrayList
import org.opendaylight.yangtools.yang.data.api.Node
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode
import org.opendaylight.yangtools.yang.model.api.ChoiceNode
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode
import org.opendaylight.yangtools.sal.binding.generator.impl.BindingGeneratorImpl
import org.opendaylight.yangtools.yang.data.impl.SimpleNodeTOImpl
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition
import org.opendaylight.yangtools.yang.model.api.TypeDefinition
import org.opendaylight.yangtools.yang.model.api.type.BooleanTypeDefinition
import org.opendaylight.yangtools.yang.model.api.type.StringTypeDefinition
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.Item
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.IdentifiableItem
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifier
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifierWithPredicates
import org.opendaylight.yangtools.yang.model.util.ExtendedType
import org.opendaylight.yangtools.sal.binding.model.api.GeneratedTransferObject
import com.google.common.collect.FluentIterable
import org.opendaylight.yangtools.yang.data.api.SimpleNode
import org.opendaylight.yangtools.binding.generator.util.BindingGeneratorUtil
import org.opendaylight.controller.sal.binding.impl.util.ClassLoaderUtils
import org.opendaylight.yangtools.yang.model.api.type.BinaryTypeDefinition
import com.google.common.collect.HashMultimap
import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import java.util.Collection
import org.opendaylight.yangtools.sal.binding.model.api.MethodSignature

class BindingMapping {

    @Property
    val Map<Type, GeneratedTypeBuilder> typeToDefinition = new HashMap();
    
    @Property
    val Map<Type, SchemaNode> typeToSchemaNode = new HashMap();

    def QName getSchemaNode(Class<?> cls) {
        val ref = Types.typeForClass(cls);
        return typeToSchemaNode.get(ref)?.QName;
    }

    def void updateBinding(SchemaContext schemaContext, ModuleContext moduleBindingContext) {
        updateBindingFor(moduleBindingContext.childNodes, schemaContext);

    }

    def org.opendaylight.yangtools.yang.data.api.InstanceIdentifier toDataDom(
        InstanceIdentifier<? extends DataObject> obj) {
        val pathArguments = obj.path;
        var Class<? extends DataObject> parent;
        val dataDomArgs = new ArrayList<PathArgument>();
        for (pathArgument : pathArguments) {
            dataDomArgs.add(pathArgument.toDataDomPathArgument(parent));
            parent = pathArgument.type;
        }

        return new org.opendaylight.yangtools.yang.data.api.InstanceIdentifier(dataDomArgs);
    }

    

    def DataObject dataObjectFromDataDom(InstanceIdentifier<? extends DataObject> identifier, CompositeNode node) {
        if (node == null) {
            return null;
        }
        val targetClass = identifier.targetType;
        val classLoader = targetClass.classLoader;
        val ref = Types.typeForClass(targetClass);
        val targetType = typeToDefinition.get(ref);
        val targetSchema = typeToSchemaNode.get(ref);
        return node.toDataObject(classLoader, targetType.toInstance, targetSchema);

    }

    private def dispatch PathArgument toDataDomPathArgument(IdentifiableItem argument, Class<? extends DataObject> parent) {
        val Class rawType = argument.type;
        val ref = Types.typeForClass(rawType);
        val schemaType = typeToSchemaNode.get(ref);
        val qname = schemaType.QName

        val Object key = argument.key;
        val predicates = key.toPredicates(schemaType as ListSchemaNode);

        return new NodeIdentifierWithPredicates(qname, predicates);
    }
    
    private def dispatch PathArgument toDataDomPathArgument(Item<?> argument, Class<? extends DataObject> parent) {
        val ref = Types.typeForClass(argument.type);
        val qname = typeToSchemaNode.get(ref).QName
        return new NodeIdentifier(qname);
    }

    private def Map<QName, Object> toPredicates(Object identifier, ListSchemaNode node) {
        val keyDefinitions = node.keyDefinition;
        val map = new HashMap<QName, Object>();
        for (keydef : keyDefinitions) {
            val keyNode = node.getDataChildByName(keydef) as LeafSchemaNode;
            val value = identifier.getSimpleValue(keydef, keyNode.type);
            map.put(keydef, value.value);
        }
        return map;
    }

    def void updateBindingFor(Map<SchemaPath, GeneratedTypeBuilder> map, SchemaContext module) {
        for (entry : map.entrySet) {
            val schemaNode = SchemaContextUtil.findDataSchemaNode(module, entry.key);
            typeToDefinition.put(entry.value, entry.value);
            typeToSchemaNode.put(entry.value, schemaNode)
        }
    }

    def CompositeNode toCompositeNode(DataContainer data) {
        val type = data.implementedInterface;
        val typeRef = Types.typeForClass(type);
        val schemaNode = typeToSchemaNode.get(typeRef);
        val generatedType = typeToDefinition.get(typeRef);

        return data.toDataDom(schemaNode, generatedType);
    }

    private def dispatch CompositeNode toDataDom(DataContainer data, ContainerSchemaNode node,
        GeneratedTypeBuilder builder) {
        val subnodes = data.toDataDomComponents(node);
        return new CompositeNodeTOImpl(node.QName, null, subnodes);
    }

    private def dispatch CompositeNode toDataDom(DataContainer data, NotificationDefinition node,
        GeneratedTypeBuilder builder) {
        val subnodes = data.toDataDomComponents(node);
        return new CompositeNodeTOImpl(node.QName, null, subnodes);
    }

    private def dispatch CompositeNode toDataDom(DataContainer data, ListSchemaNode node,
        GeneratedTypeBuilder builder) {
        val subnodes = data.toDataDomComponents(node);
        return new CompositeNodeTOImpl(node.QName, null, subnodes);
    }

    private def List<Node<?>> toDataDomComponents(DataContainer data, DataNodeContainer node) {
        val subnodes = new ArrayList<Node<?>>();
        for (childNode : node.childNodes) {
            val value = childNode.dataDomFromParent(data);
            if (value !== null) {
                subnodes.addAll(value);
            }
        }
        return subnodes;
    }

    private def List<Node<?>> dataDomFromParent(DataSchemaNode node, DataContainer container) {
        if (node.augmenting) {
            return Collections.emptyList();
        }
        return dataDomFromParentImpl(node, container);
    }

    private def dispatch List<Node<?>> dataDomFromParentImpl(LeafSchemaNode node, DataContainer container) {
        val value = container.getSimpleValue(node.QName, node.type);
        if (value !== null) {
            return Collections.<Node<?>>singletonList(value);
        }
        return Collections.emptyList();
    }

    private def dispatch List<Node<?>> dataDomFromParentImpl(LeafListSchemaNode node, DataContainer container) {
        val values = container.getSimpleValues(node);
        if (values !== null) {
            //val it = new ArrayList<Node<?>>();
            //for (value : values) {
            //}

        }
        return Collections.emptyList();
    }

    private def getSimpleValues(DataContainer container, LeafListSchemaNode node) {
        return Collections.emptyList();
    }

    private def dispatch List<Node<?>> dataDomFromParentImpl(ListSchemaNode node, DataContainer container) {
        val qname = node.QName;
        val values = container.<List>getValue(qname, List) as List<? extends DataContainer>;
        if (values === null) {
            return Collections.emptyList;
        }
        val it = new ArrayList<Node<?>>();
        for (value : values) {
            add(value.toCompositeNode());
        }

        return it;
    }

    private def dispatch List<Node<?>> dataDomFromParentImpl(ChoiceNode node, DataContainer container) {
    }

    private def dispatch List<Node<?>> serializeValueImpl(List<?> list, GeneratedTypeBuilder builder,
        ListSchemaNode node) {
        val it = new ArrayList<Node<?>>();
        for (value : list) {

            val serVal = value.serializeValueImpl(builder, node);
            if (serVal !== null) {
                addAll(serVal);
            }
        }
        return it;
    }

    public static def dispatch Node<?> getSimpleValue(Object container, QName name, ExtendedType type) {
        getSimpleValue(container, name, type.baseType);
    }

    public static def dispatch Node<?> getSimpleValue(Object container, QName name, StringTypeDefinition type) {
        val value = container.getValue(name, String);
        if(value === null) return null;
        return new SimpleNodeTOImpl(name, null, value);
    }

    public static def dispatch Node<?> getSimpleValue(Object container, QName name, TypeDefinition<?> type) {
        val value = container.getValue(name, Object);
        if(value === null) return null;
        return new SimpleNodeTOImpl(name, null, value);
    }

    public static def dispatch Node<?> getSimpleValue(Object container, QName name, BooleanTypeDefinition type) {
        val value = container.getValue(name, Boolean);
        if(value === null) return null;
        return new SimpleNodeTOImpl(name, null, value);
    }

    public static def dispatch Node<?> getSimpleValue(Object container, QName name, BinaryTypeDefinition type) {
        val Object value = container.getValue(name, Object); //Constants.BYTES_CLASS);
        if(value === null) return null;
        return new SimpleNodeTOImpl(name, null, value);
    }

    public static def <T> T getValue(Object object, QName node, Class<T> type) {
        val methodName = BindingGeneratorImpl.getterMethodName(node.localName, Types.typeForClass(type));
        var clz = object.class;
        if (object instanceof DataContainer) {
            clz = (object as DataContainer).implementedInterface;
        }
        val method = clz.getMethod(methodName);
        if (method === null) {
            return null;
        }
        val value = method.invoke(object);
        if (value === null) {
            return null;
        }
        if (type.isAssignableFrom(value.class)) {
            return value  as T;
        }
        return value.getEncapsulatedValue(type);
    }

    public static def <T> T getEncapsulatedValue(Object value, Class<T> type) {
        val method = value.class.getMethod("getValue");
        if (method !== null && type.isAssignableFrom(method.returnType)) {
            return method.invoke(value) as T;
        }
        return null;
    }

    private def dispatch List<Node<?>> serializeValueImpl(DataContainer data, GeneratedTypeBuilder builder,
        SchemaNode node) {
        return Collections.<Node<?>>singletonList(data.toDataDom(node, builder));
    }

    private def dispatch List<Node<?>> serializeValueImpl(Object object, GeneratedTypeBuilder builder,
        SchemaNode node) {
    }

    def DataObject toDataObject(CompositeNode node, ClassLoader loader, GeneratedType type, SchemaNode schema) {

        // Nasty reflection hack (for now)
        val builderClass = loader.loadClass(type.builderFQN);
        val builder = builderClass.newInstance;
        val buildMethod = builderClass.getMethod("build");

        node.fillDataObject(builder, loader, type, schema);

        return buildMethod.invoke(builder) as DataObject;
    }

    private def dispatch void fillDataObject(CompositeNode node, Object builder, ClassLoader loader, GeneratedType type,
        ListSchemaNode schema) {

        if (schema.keyDefinition !== null && !schema.keyDefinition.empty) {

            val value = node.keyToBindingKey(loader, type, schema);
            builder.setProperty("key", value);
        }
        node.fillBuilderFromContainer(builder,loader,type,schema);
    }
    
    

    private def dispatch void fillDataObject(CompositeNode node, Object builder, ClassLoader loader, GeneratedType type,
        ContainerSchemaNode schema) {
        node.fillBuilderFromContainer(builder,loader,type,schema);
    }

    
    private def void fillBuilderFromContainer(CompositeNode node, Object builder, ClassLoader loader, GeneratedType type, DataNodeContainer schema) {
        val Multimap<QName,Node<?>> dataMap = ArrayListMultimap.create();
        for(child :node.children) {
            dataMap.put(child.nodeType,node);
        }
        for(entry : dataMap.asMap.entrySet) {
            val entrySchema = schema.getDataChildByName(entry.key);
            val entryType = type.methodDefinitions.byQName(entry.key);
            entry.value.addValueToBuilder(builder,loader,entryType,entrySchema);
        }
    }
    
    private def Type byQName(List<MethodSignature> signatures, QName name) {
      
    }
    
    private def dispatch addValueToBuilder(Collection<Node<? extends Object>> nodes, Object object, ClassLoader loader, Object object2, LeafSchemaNode container) {
        
    }
    
    
    
    private def dispatch addValueToBuilder(Collection<Node<? extends Object>> nodes, Object object, ClassLoader loader, Object object2, ContainerSchemaNode container) {
        
    }
    
    
    private def dispatch addValueToBuilder(Collection<Node<? extends Object>> nodes, Object object, ClassLoader loader, Object object2, ListSchemaNode container) {
        
    }
    
    private def dispatch addValueToBuilder(Collection<Node<? extends Object>> nodes, Object object, ClassLoader loader, Object object2, LeafListSchemaNode container) {
        
    }
    
    
    
    
    private def Object keyToBindingKey(CompositeNode node, ClassLoader loader, GeneratedType type, ListSchemaNode schema) {
        val keyClass = loader.loadClass(type.keyFQN);
        val constructor = keyClass.constructors.get(0);
        val keyType = type.keyTypeProperties;
        val args = new ArrayList();
        for (key : schema.keyDefinition) {
            var keyProperty = keyType.get(BindingGeneratorUtil.parseToClassName(key.localName));
            if (keyProperty == null) {
                keyProperty = keyType.get(BindingGeneratorUtil.parseToValidParamName(key.localName));
            }
            val domKeyValue = node.getFirstSimpleByName(key);
            val keyValue = domKeyValue.deserializeSimpleValue(loader, keyProperty.returnType,
                schema.getDataChildByName(key));
            args.add(keyValue);
        }
        return ClassLoaderUtils.construct(constructor, args);
    }

    private def dispatch Object deserializeSimpleValue(SimpleNode<? extends Object> node, ClassLoader loader, Type type,
        LeafSchemaNode node2) {
        deserializeSimpleValueImpl(node, loader, type, node2.type);
    }

    private def dispatch Object deserializeSimpleValue(SimpleNode<? extends Object> node, ClassLoader loader, Type type,
        LeafListSchemaNode node2) {
        deserializeSimpleValueImpl(node, loader, type, node2.type);
    }

    private def dispatch Object deserializeSimpleValueImpl(SimpleNode<? extends Object> node, ClassLoader loader, Type type,
        ExtendedType definition) {
        deserializeSimpleValueImpl(node, loader, type, definition.baseType);
    }

    private def dispatch Object deserializeSimpleValueImpl(SimpleNode<? extends Object> node, ClassLoader loader, Type type,
        StringTypeDefinition definition) {
        if (type instanceof GeneratedTransferObject) {
            val cls = loader.getClassForType(type);
            val const = cls.getConstructor(String);
            val str = String.valueOf(node.value);
            return const.newInstance(str);
        }
        return node.value;
    }

    private def Class<?> getClassForType(ClassLoader loader, Type type) {
        loader.loadClass(type.fullyQualifiedName);
    }

    private def dispatch Object deserializeSimpleValueImpl(SimpleNode<? extends Object> node, ClassLoader loader, Type type,
        TypeDefinition definition) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    private def Map<String, GeneratedProperty> getKeyTypeProperties(GeneratedType type) {
        val method = FluentIterable.from(type.methodDefinitions).findFirst[name == "getKey"]
        val key = method.returnType as GeneratedTransferObject;
        val ret = new HashMap<String, GeneratedProperty>();
        for (prop : key.properties) {
            ret.put(prop.name, prop);
        }
        return ret;
    }

    private def void setProperty(Object object, String property, Object value) {
        val cls = object.class;
        val valMethod = cls.getMethod("set" + property.toFirstUpper, value.class);
        if (valMethod != null)
            valMethod.invoke(object, value);
    }

    private def String getBuilderFQN(Type type) '''«type.fullyQualifiedName»Builder'''

    private def String getKeyFQN(Type type) '''«type.fullyQualifiedName»Key'''

}

@Data
class PropertyCapture {

    @Property
    val Type returnType;
    @Property
    val String name;

}
