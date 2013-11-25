package org.opendaylight.controller.sal.binding.dom.serializer.impl

import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.controller.sal.binding.dom.serializer.api.CodecRegistry
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.IdentifiableItem
import org.opendaylight.yangtools.yang.common.QName
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.Item
import java.util.Map
import java.util.WeakHashMap
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifier
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifierWithPredicates
import java.util.ArrayList
import org.opendaylight.controller.sal.binding.dom.serializer.api.InstanceIdentifierCodec
import org.opendaylight.controller.sal.binding.dom.serializer.api.ValueWithQName
import java.util.HashMap
import org.slf4j.LoggerFactory
import java.util.List
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.controller.sal.binding.dom.serializer.api.IdentifierCodec
import org.opendaylight.yangtools.yang.data.impl.CompositeNodeTOImpl
import org.opendaylight.yangtools.yang.data.api.Node
import org.opendaylight.yangtools.yang.data.impl.SimpleNodeTOImpl
import org.opendaylight.yangtools.yang.data.api.CompositeNode

class InstanceIdentifierCodecImpl implements InstanceIdentifierCodec {
    
    private static val LOG = LoggerFactory.getLogger(InstanceIdentifierCodecImpl);
    val CodecRegistry codecRegistry;
    
    val Map<Class<?>,QName> classToQName = new WeakHashMap;
    
    
    public new(CodecRegistry registry) {
        codecRegistry = registry;
    }
    
    
    override deserialize(org.opendaylight.yangtools.yang.data.api.InstanceIdentifier input) {
        var Class<?> baType = null
        val biArgs = input.path
        val scannedPath = new ArrayList<QName>(biArgs.size);
        val baArgs = new ArrayList<org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument>(biArgs.size)
        for(biArg : biArgs) {
            scannedPath.add(biArg.nodeType);
            val baArg = deserializePathArgument(biArg,scannedPath)
            baArgs.add(baArg)
            baType = baArg?.type
        }
        val ret = new InstanceIdentifier(baArgs,baType as Class<? extends DataObject>);
        return ret;
    }
    
    private def dispatch org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument deserializePathArgument(NodeIdentifier argument,List<QName> processedPath) {
        val Class cls = codecRegistry.getClassForPath(processedPath);
        return new Item(cls);
    }
    
    
    private def dispatch org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument deserializePathArgument(NodeIdentifierWithPredicates argument,List<QName> processedPath) {
        val Class type = codecRegistry.getClassForPath(processedPath);
        val IdentifierCodec codec = codecRegistry.getIdentifierCodecForIdentifiable(type);
        val value = codec.deserialize(argument.toCompositeNode())?.value;
        return CodecTypeUtils.newIdentifiableItem(type,value);
    }
    
    def CompositeNode toCompositeNode(NodeIdentifierWithPredicates predicates) {
        val keyValues = predicates.keyValues.entrySet;
        val values = new ArrayList<Node<?>>(keyValues.size)
        for(keyValue : keyValues) {
            values.add(new SimpleNodeTOImpl(keyValue.key,null,keyValue.value))
        }
        return new CompositeNodeTOImpl(predicates.nodeType,null,values);
    }
    
    override serialize(InstanceIdentifier input) {
        val pathArgs = input.path as List<org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument>
        var QName previousQName = null;
        val components = new ArrayList<PathArgument>(pathArgs.size);
        for(baArg : pathArgs) { 
            codecRegistry.bindingClassEncountered(baArg.type);
            val biArg = serializePathArgument(baArg,previousQName);
            previousQName = biArg.nodeType;
            components.add(biArg);
        }
        return new org.opendaylight.yangtools.yang.data.api.InstanceIdentifier(components);
    }
    
    private def dispatch PathArgument serializePathArgument(Item argument, QName previousQname) {
        val type = argument.type;
        val qname = resolveQname(type);
        if(previousQname == null) {
            return new NodeIdentifier(qname);
        }
        return new NodeIdentifier(QName.create(previousQname,qname.localName));
    }
    
    @SuppressWarnings("rawtypes")
    private def dispatch PathArgument serializePathArgument(IdentifiableItem argument, QName previousQname) {
        val Map<QName,Object> predicates = new HashMap();
        val type = argument.type;
        val keyCodec = codecRegistry.getIdentifierCodecForIdentifiable(type);
        val qname = resolveQname(type);
        val combinedInput =  new ValueWithQName(previousQname,argument.key)
        val compositeOutput = keyCodec.serialize(combinedInput as ValueWithQName);
        for(outputValue :compositeOutput.value) {
            predicates.put(outputValue.nodeType,outputValue.value);
        }
        if(previousQname == null) {
            return new NodeIdentifierWithPredicates(qname,predicates);
        }
        return new NodeIdentifierWithPredicates(QName.create(previousQname,qname.localName),predicates);
    }
    
    def resolveQname(Class<?> class1) {
        val qname = classToQName.get(class1);
        if(qname !== null) {
            return qname;
        }
        val qnameField = class1.getField("QNAME");
        val qnameValue = qnameField.get(null) as QName;
        classToQName.put(class1,qnameValue);
        return qnameValue;
    }
}