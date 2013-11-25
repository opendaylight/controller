package org.opendaylight.controller.sal.binding.dom.serializer.impl;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.Set;
import java.util.WeakHashMap;

import org.opendaylight.controller.sal.binding.dom.serializer.api.AugmentationCodec;
import org.opendaylight.controller.sal.binding.dom.serializer.api.ChoiceCaseCodec;
import org.opendaylight.controller.sal.binding.dom.serializer.api.ChoiceCodec;
import org.opendaylight.controller.sal.binding.dom.serializer.api.CodecRegistry;
import org.opendaylight.controller.sal.binding.dom.serializer.api.DataContainerCodec;
import org.opendaylight.controller.sal.binding.dom.serializer.api.DomCodec;
import org.opendaylight.controller.sal.binding.dom.serializer.api.IdentifierCodec;
import org.opendaylight.controller.sal.binding.dom.serializer.api.InstanceIdentifierCodec;
import org.opendaylight.controller.sal.binding.dom.serializer.api.ValueWithQName;
import org.opendaylight.controller.sal.core.api.model.SchemaServiceListener;
import org.opendaylight.yangtools.binding.generator.util.ReferencedTypeImpl;
import org.opendaylight.yangtools.binding.generator.util.Types;
import org.opendaylight.yangtools.concepts.Delegator;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.binding.Augmentable;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.BindingCodec;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.model.api.ChoiceCaseNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.*;
import static org.opendaylight.controller.sal.binding.dom.serializer.impl.IntermediateMapping.*;

import org.opendaylight.yangtools.sal.binding.generator.impl.ModuleContext;
import org.opendaylight.yangtools.sal.binding.model.api.ConcreteType;
import org.opendaylight.yangtools.sal.binding.model.api.Type;
import org.opendaylight.yangtools.sal.binding.model.api.type.builder.GeneratedTypeBuilder;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.util.SchemaContextUtil;

public class LazyGeneratedCodecRegistry implements //
        CodecRegistry, //
        SchemaServiceListener, //
        GeneratorListener {

    private final static Logger LOG = LoggerFactory.getLogger(LazyGeneratedCodecRegistry.class);
    private final static LateMixinCodec NOT_READY_CODEC = new LateMixinCodec();

    private final InstanceIdentifierCodec instanceIdentifierCodec = new InstanceIdentifierCodecImpl(this);

    private TransformerGenerator generator;

    // Concrete class to codecs
    private Map<Class<?>, DataContainerCodec<?>> containerCodecs = new WeakHashMap<>();
    private Map<Class<?>, IdentifierCodec<?>> identifierCodecs = new WeakHashMap<>();
    private Map<Class<?>, ChoiceCodecImpl<?>> choiceCodecs = new WeakHashMap<>();
    private Map<Class<?>, ChoiceCaseCodecImpl<?>> caseCodecs = new WeakHashMap<>();
    private Map<Class<?>, AugmentableCompositeCodec> augmentableCodecs = new WeakHashMap<>();

    /** Binding type to encountered classes mapping **/
    @SuppressWarnings("rawtypes")
    Map<Type, WeakReference<Class>> typeToClass = new ConcurrentHashMap<>();

    @SuppressWarnings("rawtypes")
    private ConcurrentMap<Type, ChoiceCaseCodecImpl> typeToCaseNodes = new ConcurrentHashMap<>();

    private CaseClassMapFacade classToCaseRawCodec = new CaseClassMapFacade();

    Map<SchemaPath, GeneratedTypeBuilder> pathToType = new ConcurrentHashMap<>();

    private SchemaContext currentSchema;

    public TransformerGenerator getGenerator() {
        return generator;
    }

    public void setGenerator(TransformerGenerator generator) {
        this.generator = generator;
    }

    @Override
    public InstanceIdentifierCodec getInstanceIdentifierCodec() {
        return instanceIdentifierCodec;
    }

    @Override
    public <T extends Augmentation<?>> AugmentationCodec<T> getCodecForAugmentation(Class<T> object) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Class<?> getClassForPath(List<QName> names) {
        DataSchemaNode node = getSchemaNode(names);
        SchemaPath path = node.getPath();
        GeneratedTypeBuilder type = pathToType.get(path);
        ReferencedTypeImpl typeref = new ReferencedTypeImpl(type.getPackageName(), type.getName());
        @SuppressWarnings("rawtypes")
        WeakReference<Class> weakRef = typeToClass.get(typeref);
        if(weakRef == null) {
            LOG.error("Could not find loaded class for path: {} and type: {}",path,typeref.getFullyQualifiedName());
        }
        return weakRef.get();
    }

    @Override
    public IdentifierCodec<?> getKeyCodecForPath(List<QName> names) {
        @SuppressWarnings("unchecked")
        Class<? extends Identifiable<?>> cls = (Class<? extends Identifiable<?>>) getClassForPath(names);
        return getIdentifierCodecForIdentifiable(cls);
    }

    @Override
    public <T extends DataContainer> DataContainerCodec<T> getCodecForDataObject(Class<T> type) {
        @SuppressWarnings("unchecked")
        DataContainerCodec<T> ret = (DataContainerCodec<T>) containerCodecs.get(type);
        if (ret != null) {
            return ret;
        }
        Class<? extends BindingCodec<Map<QName, Object>, Object>> newType = generator.transformerFor(type);
        BindingCodec<Map<QName, Object>, Object> rawCodec = newInstanceOf(newType);
        DataContainerCodecImpl<T> newWrapper = new DataContainerCodecImpl<>(rawCodec);
        containerCodecs.put(type, newWrapper);
        return newWrapper;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void bindingClassEncountered(Class cls) {
        
        ConcreteType typeRef = Types.typeForClass(cls);
        if(typeToClass.containsKey(typeRef)) {
            return;
        }
        LOG.info("Binding Class {} encountered.",cls);
        WeakReference<Class> weakRef = new WeakReference<>(cls);
        typeToClass.put(typeRef, weakRef);
        if(DataObject.class.isAssignableFrom(cls)) {
            @SuppressWarnings({"unchecked","unused"})
            Object cdc = getCodecForDataObject((Class<? extends DataObject>) cls);
        }
    }
    
    @Override
    public void onClassProcessed(Class<?> cls) {
        ConcreteType typeRef = Types.typeForClass(cls);
        if(typeToClass.containsKey(typeRef)) {
            return;
        }
        LOG.info("Binding Class {} encountered.",cls);
        WeakReference<Class> weakRef = new WeakReference<>((Class) cls);
        typeToClass.put(typeRef, weakRef);
    }

    private DataSchemaNode getSchemaNode(List<QName> path) {
        QName firstNode = path.get(0);
        DataNodeContainer previous = currentSchema.findModuleByNamespaceAndRevision(firstNode.getNamespace(),
                firstNode.getRevision());
        Iterator<QName> iterator = path.iterator();
        while (iterator.hasNext()) {
            QName arg = iterator.next();
            DataSchemaNode currentNode = previous.getDataChildByName(arg);
            if (currentNode == null && previous instanceof DataNodeContainer) {
                currentNode = searchInChoices(previous, arg);
            }
            if (currentNode instanceof DataNodeContainer) {
                previous = (DataNodeContainer) currentNode;
            } else if (currentNode instanceof LeafSchemaNode || currentNode instanceof LeafListSchemaNode) {
                checkState(!iterator.hasNext(), "Path tries to nest inside leaf node.");
                return currentNode;
            }
        }
        return (DataSchemaNode) previous;
    }

    private DataSchemaNode searchInChoices(DataNodeContainer node, QName arg) {
        Set<DataSchemaNode> children = node.getChildNodes();
        for (DataSchemaNode child : children) {
            if (child instanceof ChoiceNode) {
                ChoiceNode choiceNode = (ChoiceNode) child;
                DataSchemaNode potential = searchInCases(choiceNode, arg);
                if (potential != null) {
                    return potential;
                }
            }
        }
        return null;
    }

    private DataSchemaNode searchInCases(ChoiceNode choiceNode, QName arg) {
        Set<ChoiceCaseNode> cases = choiceNode.getCases();
        for (ChoiceCaseNode caseNode : cases) {
            DataSchemaNode node = caseNode.getDataChildByName(arg);
            if (node != null) {
                return node;
            }
        }
        return null;
    }

    private <T> T newInstanceOf(Class<?> newType) {
        try {
            @SuppressWarnings("unchecked")
            T ret = (T) newType.newInstance();
            return ret;
        } catch (InstantiationException e) {
            throw new IllegalStateException(e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public <T extends Identifiable<?>> IdentifierCodec<?> getIdentifierCodecForIdentifiable(Class<T> type) {
        IdentifierCodec<?> obj = identifierCodecs.get(type);
        if (obj != null) {
            return obj;
        }
        Class<? extends BindingCodec<Map<QName, Object>, Object>> newCodec = generator
                .keyTransformerForIdentifiable(type);
        BindingCodec<Map<QName, Object>, Object> newInstance;
        newInstance = newInstanceOf(newCodec);
        IdentifierCodecImpl<?> newWrapper = new IdentifierCodecImpl<>(newInstance);
        identifierCodecs.put(type, newWrapper);
        return newWrapper;
    }

    @Override
    public void onCodecCreated(Class<?> cls) {
        CodecMapping.setIdentifierCodec(cls, instanceIdentifierCodec);
    }

    @Override
    public <T extends Identifier<?>> IdentifierCodec<T> getCodecForIdentifier(Class<T> object) {
        @SuppressWarnings("unchecked")
        IdentifierCodec<T> obj = (IdentifierCodec<T>) identifierCodecs.get(object);
        if (obj != null) {
            return obj;
        }
        Class<? extends BindingCodec<Map<QName, Object>, Object>> newCodec = generator
                .keyTransformerForIdentifier(object);
        BindingCodec<Map<QName, Object>, Object> newInstance;
        newInstance = newInstanceOf(newCodec);
        IdentifierCodecImpl<T> newWrapper = new IdentifierCodecImpl<>(newInstance);
        identifierCodecs.put(object, newWrapper);
        return newWrapper;
    }

    @SuppressWarnings("rawtypes")
    public ChoiceCaseCodecImpl getCaseCodecFor(Class caseClass) {
        ChoiceCaseCodecImpl<?> potential = caseCodecs.get(caseClass);
        if (potential != null) {
            return potential;
        }
        ConcreteType typeref = Types.typeForClass(caseClass);
        ChoiceCaseCodecImpl caseCodec = typeToCaseNodes.get(typeref);

        @SuppressWarnings("unchecked")
        Class<? extends BindingCodec> newCodec = generator.caseCodecFor(caseClass, caseCodec.schema);
        BindingCodec newInstance = newInstanceOf(newCodec);
        caseCodec.setDelegate(newInstance);
        caseCodecs.put(caseClass, caseCodec);

        for (Entry<Class<?>, ChoiceCodecImpl<?>> choice : choiceCodecs.entrySet()) {
            if (choice.getKey().isAssignableFrom(caseClass)) {
                choice.getValue().cases.put(caseClass, caseCodec);
            }
        }
        return caseCodec;
    }

    public void onModuleContextAdded(SchemaContext schemaContext, Module module, ModuleContext context) {
        pathToType.putAll(context.getChildNodes());

        captureCases(context.getCases(), schemaContext);
    }

    private void captureCases(Map<SchemaPath, GeneratedTypeBuilder> cases, SchemaContext module) {
        for (Entry<SchemaPath, GeneratedTypeBuilder> caseNode : cases.entrySet()) {
            ReferencedTypeImpl typeref = new ReferencedTypeImpl(caseNode.getValue().getPackageName(), caseNode
                    .getValue().getName());
            ChoiceCaseNode node = (ChoiceCaseNode) SchemaContextUtil.findDataSchemaNode(module, caseNode.getKey());
            if (node == null) {
                LOG.error("YANGTools Bug: SchemaNode for {}, with path {} was not found in context.",
                        typeref.getFullyQualifiedName(), caseNode.getKey());
                continue;
            }

            @SuppressWarnings("rawtypes")
            ChoiceCaseCodecImpl value = new ChoiceCaseCodecImpl(node);
            typeToCaseNodes.putIfAbsent(typeref, value);
        }
    }

    @Override
    public void onGlobalContextUpdated(SchemaContext context) {
        currentSchema = context;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void onChoiceCodecCreated(Class<?> choiceClass,
            Class<? extends BindingCodec<Map<QName, Object>, Object>> choiceCodec) {
        ChoiceCodec<?> oldCodec = choiceCodecs.get(choiceClass);
        checkState(oldCodec == null);
        BindingCodec<Map<QName, Object>, Object> delegate = newInstanceOf(choiceCodec);
        ChoiceCodecImpl<?> newCodec = new ChoiceCodecImpl(delegate);
        choiceCodecs.put(choiceClass, newCodec);
        CodecMapping.setClassToCaseMap(choiceCodec, (Map<Class<?>, BindingCodec<?, ?>>) classToCaseRawCodec);
        CodecMapping.setCompositeNodeToCaseMap(choiceCodec, newCodec.getCompositeToCase());

    }

    @Override
    public void onValueCodecCreated(Class<?> valueClass, Class<?> valueCodec) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onCaseCodecCreated(Class<?> choiceClass,
            Class<? extends BindingCodec<Map<QName, Object>, Object>> choiceCodec) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onDataContainerCodecCreated(Class<?> dataClass,
            Class<? extends BindingCodec<Map<QName, Object>, Object>> dataCodec) {
        if (Augmentable.class.isAssignableFrom(dataClass)) {
            AugmentableCompositeCodec augmentableCodec = getAugmentableCodec(dataClass);
            CodecMapping.setAugmentationCodec(dataCodec, augmentableCodec);
        }

    }

    private AugmentableCompositeCodec getAugmentableCodec(Class<?> dataClass) {
        AugmentableCompositeCodec ret = augmentableCodecs.get(dataClass);
        if (ret != null) {
            return ret;
        }
        ret = new AugmentableCompositeCodec(dataClass);
        augmentableCodecs.put(dataClass, ret);
        return ret;
    }

    private static abstract class IntermediateCodec<T> implements //
            DomCodec<T>, Delegator<BindingCodec<Map<QName, Object>, Object>> {

        private final BindingCodec<Map<QName, Object>, Object> delegate;

        @Override
        public BindingCodec<Map<QName, Object>, Object> getDelegate() {
            return delegate;
        }

        public IntermediateCodec(BindingCodec<Map<QName, Object>, Object> delegate) {
            this.delegate = delegate;
        }

        @Override
        public Node<?> serialize(ValueWithQName<T> input) {
            Map<QName, Object> intermediateOutput = delegate.serialize(input);
            return toNode(intermediateOutput);
        }
    }

    private static class IdentifierCodecImpl<T extends Identifier<?>> //
            extends IntermediateCodec<T> //
            implements IdentifierCodec<T> {

        public IdentifierCodecImpl(BindingCodec<Map<QName, Object>, Object> delegate) {
            super(delegate);
        }

        @Override
        public ValueWithQName<T> deserialize(Node<?> input) {
            QName qname = input.getNodeType();
            @SuppressWarnings("unchecked")
            T value = (T) getDelegate().deserialize((Map<QName, Object>) input);
            return new ValueWithQName<T>(qname, value);
        }

        @Override
        public CompositeNode serialize(ValueWithQName<T> input) {
            return (CompositeNode) super.serialize(input);
        }
    }

    private static class DataContainerCodecImpl<T extends DataContainer> //
            extends IntermediateCodec<T> //
            implements DataContainerCodec<T> {

        public DataContainerCodecImpl(BindingCodec<Map<QName, Object>, Object> delegate) {
            super(delegate);
        }

        @Override
        public ValueWithQName<T> deserialize(Node<?> input) {
            if (input == null) {
                return null;
            }
            QName qname = input.getNodeType();
            @SuppressWarnings("unchecked")
            T value = (T) getDelegate().deserialize((Map<QName, Object>) input);
            return new ValueWithQName<T>(qname, value);
        }

        @Override
        public CompositeNode serialize(ValueWithQName<T> input) {
            return (CompositeNode) super.serialize(input);
        }
    }

    @SuppressWarnings("rawtypes")
    private static class ChoiceCaseCodecImpl<T extends DataContainer> implements ChoiceCaseCodec<T>, //
            Delegator<BindingCodec> {
        private final boolean augmenting;
        private BindingCodec delegate;

        private final Set<String> validNames;
        private final Set<QName> validQNames;
        private ChoiceCaseNode schema;

        public ChoiceCaseCodecImpl(ChoiceCaseNode caseNode) {
            this.delegate = NOT_READY_CODEC;
            this.schema = caseNode;
            validNames = new HashSet<>();
            validQNames = new HashSet<>();
            for (DataSchemaNode node : caseNode.getChildNodes()) {
                QName qname = node.getQName();
                validQNames.add(qname);
                validNames.add(qname.getLocalName());
            }
            augmenting = caseNode.isAugmenting();
        }

        @Override
        public ValueWithQName<T> deserialize(Node<?> input) {
            throw new UnsupportedOperationException("Direct invocation of this codec is not allowed.");
        }

        @Override
        public CompositeNode serialize(ValueWithQName<T> input) {
            throw new UnsupportedOperationException("Direct invocation of this codec is not allowed.");
        }

        public BindingCodec getDelegate() {
            return delegate;
        }

        public void setDelegate(BindingCodec delegate) {
            this.delegate = delegate;
        }

        public ChoiceCaseNode getSchema() {
            return schema;
        }

        @Override
        public boolean isAcceptable(Node<?> input) {
            if (false == (input instanceof CompositeNode)) {
                if (augmenting) {
                    return checkAugmenting((CompositeNode) input);
                } else {
                    return checkLocal((CompositeNode) input);
                }
            }
            return false;
        }

        private boolean checkLocal(CompositeNode input) {
            QName parent = input.getNodeType();
            for (Node<?> childNode : input.getChildren()) {
                QName child = childNode.getNodeType();
                if (false == Objects.equals(parent.getNamespace(), child.getNamespace())) {
                    continue;
                }
                if (false == Objects.equals(parent.getRevision(), child.getRevision())) {
                    continue;
                }
                if (validNames.contains(child.getLocalName())) {
                    return true;
                }
            }
            return false;
        }

        private boolean checkAugmenting(CompositeNode input) {
            for (Node<?> child : input.getChildren()) {
                if (validQNames.contains(child.getNodeType())) {
                    return true;
                }
            }
            return false;
        }
    }

    private static class ChoiceCodecImpl<T> implements ChoiceCodec<T> {

        private final BindingCodec<Map<QName, Object>, Object> delegate;

        @SuppressWarnings("rawtypes")
        private final Map<Class, ChoiceCaseCodecImpl<?>> cases = new WeakHashMap<>();

        private final CaseCompositeNodeMapFacade CompositeToCase;

        public ChoiceCodecImpl(BindingCodec<Map<QName, Object>, Object> delegate) {
            this.delegate = delegate;
            this.CompositeToCase = new CaseCompositeNodeMapFacade(cases);
        }

        @Override
        public ValueWithQName<T> deserialize(Node<?> input) {
            throw new UnsupportedOperationException("Direct invocation of this codec is not allowed.");
        }

        @Override
        public Node<?> serialize(ValueWithQName<T> input) {
            throw new UnsupportedOperationException("Direct invocation of this codec is not allowed.");
        }

        public CaseCompositeNodeMapFacade getCompositeToCase() {
            return CompositeToCase;
        }

        public Map<Class, ChoiceCaseCodecImpl<?>> getCases() {
            return cases;
        }

        public BindingCodec<Map<QName, Object>, Object> getDelegate() {
            return delegate;
        }

    }

    @SuppressWarnings("rawtypes")
    private class CaseClassMapFacade extends MapFacadeBase {

        @Override
        public Set<java.util.Map.Entry<Class, BindingCodec<Object, Object>>> entrySet() {
            return null;
        }

        @Override
        public BindingCodec get(Object key) {
            if (key instanceof Class) {
                Class cls = (Class) key;
                //bindingClassEncountered(cls);
                ChoiceCaseCodecImpl caseCodec = getCaseCodecFor(cls);
                return caseCodec.getDelegate();
            }
            return null;
        }
    }

    @SuppressWarnings("rawtypes")
    private static class CaseCompositeNodeMapFacade extends MapFacadeBase<CompositeNode> {

        final Map<Class, ChoiceCaseCodecImpl<?>> choiceCases;

        public CaseCompositeNodeMapFacade(Map<Class, ChoiceCaseCodecImpl<?>> choiceCases) {
            this.choiceCases = choiceCases;
        }

        @Override
        public BindingCodec get(Object key) {
            if (false == (key instanceof CompositeNode)) {
                return null;
            }
            for (java.util.Map.Entry<Class, ChoiceCaseCodecImpl<?>> entry : choiceCases.entrySet()) {
                ChoiceCaseCodecImpl<?> codec = entry.getValue();
                if (codec.isAcceptable((CompositeNode) key)) {
                    return codec.getDelegate();
                }
            }
            return null;
        }
        
        
    }

    /**
     * This map is used as only facade for {@link BindingCodec} in different
     * classloaders to retrieve codec dynamicly based on provided key.
     * 
     * @param <T>
     *            Key type
     */
    @SuppressWarnings("rawtypes")
    private static abstract class MapFacadeBase<T> implements Map<T, BindingCodec<?, ?>> {

        @Override
        public boolean containsKey(Object key) {
            return get(key) != null;
        }

        @Override
        public void clear() {
            throw notModifiable();
        }

        @Override
        public boolean equals(Object obj) {
            return super.equals(obj);
        }

        @Override
        public BindingCodec remove(Object key) {
            return null;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public Collection<BindingCodec<?, ?>> values() {
            return null;
        }

        private UnsupportedOperationException notModifiable() {
            return new UnsupportedOperationException("Not externally modifiable.");
        }

        @Override
        public BindingCodec<Map<QName, Object>, Object> put(T key, BindingCodec<?,?> value) {
            throw notModifiable();
        }

        @Override
        public void putAll(Map<? extends T, ? extends BindingCodec<?, ?>> m) {
            throw notModifiable();
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public Set<T> keySet() {
            return null;
        }

        @Override
        public Set<java.util.Map.Entry<T, BindingCodec<?, ?>>> entrySet() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public boolean containsValue(Object value) {
            return false;
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private class AugmentableCompositeCodec implements BindingCodec {

        private final Class augmentableType;

        Map<Class, BindingCodec> rawAugmentationCodecs = new WeakHashMap<>();

        public AugmentableCompositeCodec(Class type) {
            checkArgument(Augmentable.class.isAssignableFrom(type));
            augmentableType = type;
        }

        @Override
        public Object serialize(Object input) {
            if (input instanceof Augmentable<?>) {

                Map<Class, Augmentation> augmentations = getAugmentations(input);
                return serializeImpl(augmentations);
            }
            return null;
        }

        private Map<Class, Augmentation> getAugmentations(Object input) {
            Field augmentationField;
            try {
                augmentationField = input.getClass().getDeclaredField("augmentation");
                augmentationField.setAccessible(true);
                Map<Class, Augmentation> augMap = (Map<Class, Augmentation>) augmentationField.get(input);
                return augMap;
            } catch (NoSuchFieldException e) {

            } catch (SecurityException e) {

            } catch (IllegalArgumentException e) {

            } catch (IllegalAccessException e) {

            }
            return Collections.emptyMap();
        }

        private List serializeImpl(Map<Class, Augmentation> input) {
            List ret = new ArrayList<>();
            for (Entry<Class, Augmentation> entry : input.entrySet()) {
                BindingCodec codec = getRawCodecForAugmentation(entry.getKey());
                List output = (List) codec.serialize(new ValueWithQName(null, entry.getValue()));
                ret.addAll(output);
            }
            return ret;
        }

        private BindingCodec getRawCodecForAugmentation(Class key) {
            BindingCodec ret = rawAugmentationCodecs.get(key);
            if (ret != null) {
                return ret;
            }
            try {
                Class<? extends BindingCodec> retClass = generator.augmentationTransformerFor(key);
                ret = retClass.newInstance();
                rawAugmentationCodecs.put(key, ret);
                return ret;
            } catch (InstantiationException e) {

            } catch (IllegalAccessException e) {

            }
            return null;
        }

        @Override
        public Map<Class, Augmentation> deserialize(Object input) {
            Map<Class, Augmentation> ret = new HashMap<>();
            if (input instanceof CompositeNode) {
                for (Entry<Class, BindingCodec> codec : rawAugmentationCodecs.entrySet()) {
                    Augmentation value = (Augmentation) codec.getValue().deserialize(input);
                    if (value != null) {
                        ret.put(codec.getKey(), value);
                    }
                }
            }
            return ret;
        }

        public Map<Class, BindingCodec> getRawAugmentationCodecs() {
            return rawAugmentationCodecs;
        }

        public void setRawAugmentationCodecs(Map<Class, BindingCodec> rawAugmentationCodecs) {
            this.rawAugmentationCodecs = rawAugmentationCodecs;
        }

        public Class getAugmentableType() {
            return augmentableType;
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static class LateMixinCodec implements BindingCodec, Delegator<BindingCodec> {

        private BindingCodec delegate;

        @Override
        public BindingCodec getDelegate() {
            if (delegate == null) {
                throw new IllegalStateException("Codec not initialized yet.");
            }
            return delegate;
        }

        @Override
        public Object deserialize(Object input) {
            return getDelegate().deserialize(input);
        }

        @Override
        public Object serialize(Object input) {
            return getDelegate().serialize(input);
        }
    }
}