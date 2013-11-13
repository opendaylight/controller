package org.opendaylight.controller.sal.binding.dom.serializer.impl

import javassist.ClassPool
import org.opendaylight.yangtools.sal.binding.model.api.GeneratedType
import org.opendaylight.yangtools.yang.model.api.SchemaNode
import org.opendaylight.controller.sal.binding.codegen.util.JavassistUtils
import javassist.CtClass
import java.util.Map
import org.opendaylight.yangtools.yang.common.QName
import javassist.CtField
import static javassist.Modifier.*
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode
import org.opendaylight.yangtools.sal.binding.model.api.MethodSignature
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer
import org.opendaylight.yangtools.sal.binding.model.api.Type
import org.opendaylight.yangtools.sal.binding.model.api.type.builder.GeneratedTypeBuilder
import org.opendaylight.yangtools.binding.generator.util.Types
import org.opendaylight.yangtools.sal.binding.model.api.ParameterizedType
import java.util.HashMap
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode
import org.opendaylight.yangtools.binding.generator.util.BindingGeneratorUtil
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode
import java.util.WeakHashMap
import java.util.List
import java.util.TreeSet
import com.google.common.base.Joiner
import org.opendaylight.yangtools.sal.binding.model.api.GeneratedTransferObject
import org.opendaylight.yangtools.sal.binding.model.api.Enumeration
import org.opendaylight.yangtools.yang.model.api.ChoiceCaseNode
import static org.opendaylight.controller.sal.binding.impl.util.ClassLoaderUtils.*;
import org.opendaylight.yangtools.yang.binding.BindingDeserializer
import org.opendaylight.yangtools.yang.binding.BindingSerializer
import org.opendaylight.yangtools.yang.binding.BindingCodec
import org.slf4j.LoggerFactory

class TransformerGenerator {

    private static val log = LoggerFactory.getLogger(TransformerGenerator)

    public static val STRING = Types.typeForClass(String);
    public static val BOOLEAN = Types.typeForClass(Boolean);
    public static val INTEGER = Types.typeForClass(Integer);

    //public static val DECIMAL = Types.typeForClass(Decimal);
    public static val LONG = Types.typeForClass(Long);

    val ClassPool classPool
    val extension JavassistUtils utils;

    CtClass ctTransformator

    CtClass ctQName

    @Property
    var Map<Type, Type> typeDefinitions;

    @Property
    var Map<Type, GeneratedTypeBuilder> typeToDefinition

    @Property
    var Map<Type, SchemaNode> typeToSchemaNode

    val Map<Class<?>, Class<?>> generatedClasses = new WeakHashMap();

    public new(ClassPool pool) {
        classPool = pool;
        utils = new JavassistUtils(pool)

        ctTransformator = BindingCodec.asCtClass;
        ctQName = QName.asCtClass
    }

    def Class<? extends BindingCodec<Map<QName, Object>, Object>> transformerFor(Class<?> inputType) {
        return withClassLoader(inputType.classLoader) [ |
            val ret = generatedClasses.get(inputType);
            if (ret !== null) {
                return ret as Class<? extends BindingCodec<Map<QName,Object>, Object>>;
            }
            val ref = Types.typeForClass(inputType)
            val node = typeToSchemaNode.get(ref)
            val typeSpecBuilder = typeToDefinition.get(ref)
            val typeSpec = typeSpecBuilder.toInstance();
            val newret = generateTransformerFor(inputType, typeSpec, node)
            generatedClasses.put(inputType, newret);
            return newret as Class<? extends BindingCodec<Map<QName,Object>, Object>>;
        ]
    }

    def Class<?> keyTransformerFor(Class<?> inputType, GeneratedType type, ListSchemaNode schema) {
        return withClassLoader(inputType.classLoader) [ |
            val transformer = generatedClasses.get(inputType);
            if (transformer != null) {
                return transformer;
            }
            val newret = generateKeyTransformerFor(inputType, type, schema);
            generatedClasses.put(inputType, newret);
            return newret as Class<? extends BindingCodec<Map<QName,Object>, Object>>;
        ]
    }

    def Class<?> keyTransformer(GeneratedType type, ListSchemaNode node) {
        val cls = loadClassWithTCCL(type.resolvedName + "Key");
        keyTransformerFor(cls, type, node);
    }

    private def serializer(Type type) {
        val cls = loadClassWithTCCL(type.resolvedName);
        transformerFor(cls);

    }

    def Class<?> getValueSerializer(GeneratedTransferObject type) {
        val cls = loadClassWithTCCL(type.resolvedName);
        val transformer = generatedClasses.get(cls);
        if (transformer !== null) {
            return transformer;
        }
        val valueTransformer = generateValueTransformer(cls, type);
        generatedClasses.put(cls, valueTransformer);
        return valueTransformer;
    }

    private def generateKeyTransformerFor(Class<? extends Object> inputType, GeneratedType typeSpec, ListSchemaNode node) {
        try {
            log.info("Generating DOM Codec for {} with {}",inputType,inputType.classLoader)
            val properties = typeSpec.allProperties;
            val ctCls = createClass(inputType.transformatorFqn) [
                //staticField(Map,"AUGMENTATION_SERIALIZERS");
                staticQNameField(node.QName);
                implementsType(ctTransformator)
                method(Object, "toDomStatic", QName, Object) [
                    modifiers = PUBLIC + FINAL + STATIC
                    body = '''
                        {
                        
                            return null;
                        }
                    '''
                ]
                method(Object, "fromDomStatic", QName, Object) [
                    modifiers = PUBLIC + FINAL + STATIC
                    body = '''
                        {
                            if($2 == null){
                                return  null;
                            }
                            «QName.name» _localQName = $1;
                            java.util.Map _compositeNode = (java.util.Map) $2;
                            «FOR key : node.keyDefinition»
                                «val propertyName = key.getterName»
                                «val keyDef = node.getDataChildByName(key)»
                                «val property = properties.get(propertyName)»
                                «deserializeProperty(keyDef, property.returnType, property)»;
                            «ENDFOR»
                            «inputType.name» _value = new «inputType.name»(«node.keyDefinition.keyConstructorList»);
                            return _value;
                        }
                    '''
                ]
                method(Object, "serialize", Object) [
                    body = '''
                        return toDomStatic(QNAME,$1);
                    '''
                ]
                method(Object, "deserialize", Object) [
                    body = '''
                        return fromDomStatic(QNAME,$1);
                    '''
                ]
            ]
            val ret = ctCls.toClass(inputType.classLoader, inputType.protectionDomain)
            log.info("DOM Codec for {} was generated {}",inputType,ret)
            return ret as Class<? extends BindingCodec<Map<QName,Object>, ?>>;
        } catch (Exception e) {
            log.error("Cannot compile DOM Codec for {}. Exception {}",inputType,e);
            val exception = new IllegalStateException("Cannot compile Transformator for " + inputType);
            exception.addSuppressed(e);
            throw exception;
        }
    }

    private def <D> Class<? extends BindingCodec<Map<QName, Object>, D>> generateTransformerFor(Class<D> inputType,
        GeneratedType typeSpec, SchemaNode node) {
        try {
            log.info("Generating DOM Codec for {} with {}",inputType,inputType.classLoader)
            val ctCls = createClass(typeSpec.transformatorFqn) [
                //staticField(Map,"AUGMENTATION_SERIALIZERS");
                staticQNameField(inputType);
                implementsType(ctTransformator)
                method(Object, "toDomStatic", QName, Object) [
                    modifiers = PUBLIC + FINAL + STATIC
                    body = serializeBodyFacade(typeSpec, node)
                ]
                method(Object, "serialize", Object) [
                    body = '''
                        return toDomStatic(QNAME,$1);
                    '''
                ]
                method(Object, "fromDomStatic", QName, Object) [
                    modifiers = PUBLIC + FINAL + STATIC
                    body = deserializeBody(typeSpec, node)
                ]
                method(Object, "deserialize", Object) [
                    body = '''
                        return fromDomStatic(QNAME,$1);
                    '''
                ]
            ]

            val ret = ctCls.toClass(inputType.classLoader, inputType.protectionDomain)
            return ret as Class<? extends BindingCodec<Map<QName,Object>, D>>;
        } catch (Exception e) {
            log.error("Cannot compile DOM Codec for {}. Exception {}",inputType,e);
            val exception = new IllegalStateException("Cannot compile Transformator for " + inputType);
            exception.addSuppressed(e);
            throw exception;
        }
    }

    private def keyConstructorList(List<QName> qnames) {
        val names = new TreeSet<String>()
        for (name : qnames) {
            val fieldName = name.getterName;
            names.add(fieldName);
        }
        return Joiner.on(",").join(names);
    }

    private def serializeBodyFacade(GeneratedType type, SchemaNode node) {
        val ret = serializeBody(type, node);
        return ret;
    }

    private def String deserializeBody(GeneratedType type, SchemaNode node) {
        val ret = deserializeBodyImpl(type, node);
        return ret;
    }

    private def deserializeKey(GeneratedType type, ListSchemaNode node) {
        if (node.keyDefinition != null && !node.keyDefinition.empty) {
            return '''
                «type.resolvedName»Key getKey = («type.resolvedName»Key) «keyTransformer(type, node).canonicalName».fromDomStatic(_localQName,_compositeNode);
                _builder.setKey(getKey);
            ''';
        }
    }

    private def dispatch String deserializeBodyImpl(GeneratedType type, SchemaNode node) '''
        {
            «QName.name» _localQName = «QName.name».create($1,QNAME.getLocalName());
            
            if($2 == null) {
                return null;
            }
            java.util.Map _compositeNode = (java.util.Map) $2;
            «type.builderName» _builder = new «type.builderName»();
            
            return _builder.build();
        }
    '''

    private def dispatch String deserializeBodyImpl(GeneratedType type, ListSchemaNode node) '''
        {
            «QName.name» _localQName = «QName.name».create($1,QNAME.getLocalName());
            if($2 == null) {
                return null;
            }
            java.util.Map _compositeNode = (java.util.Map) $2;
            «type.builderName» _builder = new «type.builderName»();
            «deserializeKey(type, node)»
            «deserializeDataNodeContainerBody(type, node)»
            return _builder.build();
        }
    '''

    private def dispatch String deserializeBodyImpl(GeneratedType type, ContainerSchemaNode node) '''
        {
            «QName.name» _localQName = «QName.name».create($1,QNAME.getLocalName());
            if($2 == null) {
                return null;
            }
            java.util.Map _compositeNode = (java.util.Map) $2;
            «type.builderName» _builder = new «type.builderName»();
            «deserializeDataNodeContainerBody(type, node)»
            return _builder.build();
        }
    '''

    private def dispatch String deserializeBodyImpl(GeneratedType type, ChoiceCaseNode node) '''
        {
            «QName.name» _localQName = «QName.name».create($1,QNAME.getLocalName());
            if($2 == null) {
                return null;
            }
            java.util.Map _compositeNode = (java.util.Map) $2;
            «type.builderName» _builder = new «type.builderName»();
            «deserializeDataNodeContainerBody(type, node)»
            return _builder.build();
        }
    '''

    private def deserializeDataNodeContainerBody(GeneratedType type, DataNodeContainer node) {
        deserializeNodeContainerBodyImpl(type, type.allProperties, node);
    }

    private def deserializeNodeContainerBodyImpl(GeneratedType type, HashMap<String, MethodSignature> properties,
        DataNodeContainer node) {
        val ret = '''
            «FOR child : node.childNodes.filter[!augmenting]»
                «val signature = properties.get(child.getterName)»
                «deserializeProperty(child, signature.returnType, signature)»
                _builder.«signature.name.toSetter»(«signature.name»);
            «ENDFOR»
        '''
        return ret;
    }

    private def dispatch CharSequence deserializeProperty(ListSchemaNode schema, ParameterizedType type,
        MethodSignature property) '''
        java.util.List _dom_«property.name» = _compositeNode.get(«QName.name».create(_localQName,"«schema.QName.
            localName»"));
        //System.out.println("«property.name»#deCode"+_dom_«property.name»);
        java.util.List «property.name» = new java.util.ArrayList();
        if(_dom_«property.name» != null) {
            java.util.List _serialized = new java.util.ArrayList();
            java.util.Iterator _iterator = _dom_«property.name».iterator();
            boolean _hasNext = _iterator.hasNext();
            while(_hasNext) {
                Object _listItem = _iterator.next();
                //System.out.println("  item" + _listItem);
                Object _value = «type.actualTypeArguments.get(0).serializer.name».fromDomStatic(_localQName,_listItem);
                //System.out.println("  value" + _value);
                «property.name».add(_value);
                _hasNext = _iterator.hasNext();
            }
        }
        
        //System.out.println(" list" + «property.name»);
    '''

    private def dispatch CharSequence deserializeProperty(LeafListSchemaNode schema, ParameterizedType type,
        MethodSignature property) '''
        java.util.List _dom_«property.name» = _compositeNode.get(«QName.name».create(_localQName,"«schema.QName.
            localName»"));
        java.util.List «property.name» = new java.util.ArrayList();
        if(_dom_«property.name» != null) {
            java.util.List _serialized = new java.util.ArrayList();
            java.util.Iterator _iterator = _dom_«property.name».iterator();
            boolean _hasNext = _iterator.hasNext();
            while(_hasNext) {
                Object _listItem = _iterator.next();
                if(_listItem instanceof java.util.Map.Entry) {
                    Object _innerValue = ((java.util.Map.Entry) _listItem).getValue();
                    Object _value = «deserializeValue(type.actualTypeArguments.get(0), "_innerValue")»;
                    «property.name».add(_value);
                }
                _hasNext = _iterator.hasNext();
            }
        }
    '''

    private def dispatch CharSequence deserializeProperty(LeafSchemaNode schema, Type type, MethodSignature property) '''
        java.util.List _dom_«property.name»_list = 
            _compositeNode.get(«QName.name».create(_localQName,"«schema.QName.localName»"));
        «type.resolvedName» «property.name» = null;
        if(_dom_«property.name»_list != null && _dom_«property.name»_list.size() > 0) {
            java.util.Map.Entry _dom_«property.name» = (java.util.Map.Entry) _dom_«property.name»_list.get(0);
            Object _inner_value = _dom_«property.name».getValue();
            «property.name» = «deserializeValue(type, "_inner_value")»;
        }
    '''

    private def dispatch CharSequence deserializeProperty(ContainerSchemaNode schema, Type type,
        MethodSignature property) '''
        java.util.List _dom_«property.name»_list = 
            _compositeNode.get(«QName.name».create(_localQName,"«schema.QName.localName»"));
        «type.resolvedName» «property.name» = null;
        if(_dom_«property.name»_list != null && _dom_«property.name»_list.size() > 0) {
            
            java.util.Map _dom_«property.name» = (java.util.Map) _dom_«property.name»_list.get(0);
            «type.resolvedName» «property.name» =  «type.serializer.name».fromDomStatic(_localQName,_dom_«property.name»);
        }
    '''

    private def dispatch String deserializeValue(GeneratedTransferObject type, String domParameter) '''
        («type.resolvedName») «type.valueSerializer.name».fromDomValue(«domParameter»);
    '''

    private def dispatch Class<? extends BindingCodec<Map<QName, Object>, Object>> generateValueTransformer(
        Class<?> inputType, GeneratedTransferObject typeSpec) {
        try {

            val returnType = typeSpec.valueReturnType;
            if (returnType == null) {

                val ctCls = createDummyImplementation(inputType, typeSpec);
                val ret = ctCls.toClass(inputType.classLoader, inputType.protectionDomain)
                return ret as Class<? extends BindingCodec<Map<QName,Object>, Object>>;
            }
            val ctCls = createClass(typeSpec.transformatorFqn) [
                //staticField(Map,"AUGMENTATION_SERIALIZERS");
                implementsType(ctTransformator)
                implementsType(BindingDeserializer.asCtClass)
                method(Object, "toDomValue", Object) [
                    modifiers = PUBLIC + FINAL + STATIC
                    body = '''
                        {
                            ////System.out.println("«inputType.simpleName»#toDomValue: "+$1);
                            
                            if($1 == null) {
                                return null;
                            }
                            «typeSpec.resolvedName» _encapsulatedValue = («typeSpec.resolvedName») $1;
                            //System.out.println("«inputType.simpleName»#toDomValue:Enc: "+_encapsulatedValue);
                            «returnType.resolvedName» _value =  _encapsulatedValue.getValue();
                            //System.out.println("«inputType.simpleName»#toDomValue:DeEnc: "+_value);
                            return _value;
                        }
                    '''
                ]
                method(Object, "serialize", Object) [
                    body = '''
                        {
                            return toDomValue($1);
                        }
                    '''
                ]
                method(Object, "fromDomValue", Object) [
                    modifiers = PUBLIC + FINAL + STATIC
                    body = '''
                        {
                            //System.out.println("«inputType.simpleName»#fromDomValue: "+$1);
                            
                            if($1 == null) {
                                return null;
                            }
                            «returnType.name» _simpleValue = «deserializeValue(returnType, "$1")»;
                            «typeSpec.resolvedName» _value = new «typeSpec.resolvedName»(_simpleValue);
                            return _value;
                        }
                    '''
                ]
                method(Object, "deserialize", Object) [
                    body = '''{
                            return fromDomValue($1);
                    }
                    '''
                ]
            ]

            val ret = ctCls.toClass(inputType.classLoader, inputType.protectionDomain)
            log.info("DOM Codec for {} was generated {}",inputType,ret)
            return ret as Class<? extends BindingCodec<Map<QName,Object>, Object>>;
        } catch (Exception e) {
            log.error("Cannot compile DOM Codec for {}. Exception {}",inputType,e);
            val exception = new IllegalStateException("Cannot compile Transformator for " + inputType);
            exception.addSuppressed(e);
            throw exception;
        }

    }

    private def createDummyImplementation(Class<?> object, GeneratedTransferObject typeSpec) {
        log.info("Generating Dummy DOM Codec for {} with {}",object,object.classLoader)
        return createClass(typeSpec.transformatorFqn) [
            //staticField(Map,"AUGMENTATION_SERIALIZERS");
            implementsType(ctTransformator)
            implementsType(BindingDeserializer.asCtClass)
            method(Object, "toDomValue", Object) [
                modifiers = PUBLIC + FINAL + STATIC
                body = '''return null;'''
            ]
            method(Object, "serialize", Object) [
                body = '''
                    {
                        return toDomValue($1);
                    }
                '''
            ]
            method(Object, "fromDomValue", Object) [
                modifiers = PUBLIC + FINAL + STATIC
                body = '''return null;'''
            ]
            method(Object, "deserialize", Object) [
                body = '''{
                        return fromDomValue($1);
                    }
                    '''
            ]
        ]
    }

    def Type getValueReturnType(GeneratedTransferObject object) {
        for (prop : object.properties) {
            if (prop.name == "value") {
                return prop.returnType;
            }
        }
        if (object.superType != null) {
            return getValueReturnType(object.superType);
        }
        return null;
    }

    private def dispatch Class<? extends BindingCodec<Map<QName, Object>, Object>> generateValueTransformer(
        Class<?> inputType, Enumeration typeSpec) {
        try {
            log.info("Generating DOM Codec for {} with {}",inputType,inputType.classLoader)
            val ctCls = createClass(typeSpec.transformatorFqn) [
                //staticField(Map,"AUGMENTATION_SERIALIZERS");
                implementsType(ctTransformator)
                method(Object, "toDomValue", Object) [
                    modifiers = PUBLIC + FINAL + STATIC
                    body = '''
                        if($1 == null) {
                            return null;
                        }
                        «typeSpec.resolvedName» _value = («typeSpec.resolvedName») $1;
                        return _value.getValue();
                    '''
                ]
                method(Object, "serialize", Object) [
                    body = '''
                        return toDomValue($1);
                    '''
                ]
                method(Object, "fromDomValue", Object) [
                    modifiers = PUBLIC + FINAL + STATIC
                    body = '''
                        if($1 == null) {
                            return null;
                        }
                        _simpleValue = null;
                        «typeSpec.resolvedName» _value = new «typeSpec.resolvedName»(null);
                        return _value;
                    '''
                ]
                method(Object, "deserialize", Object) [
                    body = '''
                        return fromDomValue($1);
                    '''
                ]
            ]

            val ret = ctCls.toClass(inputType.classLoader, inputType.protectionDomain)
            log.info("DOM Codec for {} was generated {}",inputType,ret)
            return ret as Class<? extends BindingCodec<Map<QName,Object>, Object>>;
        } catch (Exception e) {
            log.error("Cannot compile DOM Codec for {}. Exception {}",inputType,e);
            val exception = new IllegalStateException("Cannot compile Transformator for " + inputType);
            exception.addSuppressed(e);
            throw exception;
        }

    }

    private def dispatch String deserializeValue(Type type, String domParameter) '''(«type.resolvedName») «domParameter»'''

    /** 
     * Default catch all
     * 
     **/
    private def dispatch CharSequence deserializeProperty(DataSchemaNode container, Type type, MethodSignature property) '''
        «type.resolvedName» «property.name» = null;
    '''

    private def dispatch CharSequence deserializeProperty(DataSchemaNode container, GeneratedTypeBuilder type,
        MethodSignature property) {
        _deserializeProperty(container, type.toInstance, property)
    }

    public static def toSetter(String it) {

        if (startsWith("is")) {
            return "set" + substring(2);
        } else if (startsWith("get")) {
            return "set" + substring(3);
        }
        return "set" + it;
    }

    /* 
    private def dispatch CharSequence deserializeProperty(DataSchemaNode container,GeneratedType type, MethodSignature property) '''
        «property.returnType.resolvedName» «property.name» = value.«property.name»();
        if(«property.name» != null) {
            Object domValue = «type.serializer».toDomStatic(QNAME,«property.name»);
            childNodes.add(domValue);
        }
    '''
    */
    private def getBuilderName(GeneratedType type) '''«type.resolvedName»Builder'''

    private def staticQNameField(CtClass it, Class node) {
        val field = new CtField(ctQName, "QNAME", it);
        field.modifiers = PUBLIC + FINAL + STATIC;
        addField(field, '''«node.name».QNAME''')
    }
    
    private def staticQNameField(CtClass it, QName node) {
        val field = new CtField(ctQName, "QNAME", it);
        field.modifiers = PUBLIC + FINAL + STATIC;
        addField(field, '''«QName.asCtClass.name».create("«node.namespace»","«node.formattedRevision»","«node.localName»")''')
    }

    private def dispatch String serializeBody(GeneratedType type, ListSchemaNode node) '''
        {
            «QName.name» resultName = «QName.name».create($1,QNAME.getLocalName());
            java.util.List childNodes = new java.util.ArrayList();
            «type.resolvedName» value = («type.resolvedName») $2;
            «transformDataContainerBody(type.allProperties, node)»
            return ($r) java.util.Collections.singletonMap(resultName,childNodes);
        }
    '''

    private def dispatch String serializeBody(GeneratedType type, ContainerSchemaNode node) '''
        {
            «QName.name» resultName = «QName.name».create($1,QNAME.getLocalName());
            java.util.List childNodes = new java.util.ArrayList();
            «type.resolvedName» value = («type.resolvedName») $2;
            «transformDataContainerBody(type.allProperties, node)»
            return ($r) java.util.Collections.singletonMap(resultName,childNodes);
        }
    '''

    private def transformDataContainerBody(Map<String, MethodSignature> properties, DataNodeContainer node) {
        val ret = '''
            «FOR child : node.childNodes.filter[!augmenting]»
                «val signature = properties.get(child.getterName)»
                «serializeProperty(child, signature.returnType, signature)»
            «ENDFOR»
        '''
        return ret;
    }

    private static def String getGetterName(DataSchemaNode node) {
        return "get" + BindingGeneratorUtil.parseToClassName(node.QName.localName);
    }

    private static def String getGetterName(QName node) {
        return "get" + BindingGeneratorUtil.parseToClassName(node.localName);
    }

    private def dispatch CharSequence serializeProperty(ListSchemaNode schema, ParameterizedType type,
        MethodSignature property) '''
        «property.returnType.resolvedName» «property.name» = value.«property.name»();
        if(«property.name» != null) {
            java.util.Iterator _iterator = «property.name».iterator();
            boolean _hasNext = _iterator.hasNext();
            while(_hasNext) {
                Object _listItem = _iterator.next();
                Object _domValue = «type.actualTypeArguments.get(0).serializer.name».toDomStatic(QNAME,_listItem);
                childNodes.add(_domValue);
                _hasNext = _iterator.hasNext();
            }
        }
    '''

    private def dispatch CharSequence serializeProperty(LeafSchemaNode schema, Type type, MethodSignature property) '''
        «property.returnType.resolvedName» «property.name» = value.«property.name»();
        
        if(«property.name» != null) {
            «QName.name» _qname = «QName.name».create(resultName,"«schema.QName.localName»");
            Object _propValue = «serializeValue(type, property.name)»;
            if(_propValue != null) {
                Object _domValue = java.util.Collections.singletonMap(_qname,_propValue);
                childNodes.add(_domValue);
            }
        }
    '''

    private def dispatch serializeValue(GeneratedTransferObject type, String parameter) '''«type.valueSerializer.name».toDomValue(«parameter»)'''

    private def dispatch serializeValue(Type signature, String property) '''«property»'''

    private def dispatch CharSequence serializeProperty(LeafListSchemaNode schema, Type type, MethodSignature property) '''
        «property.returnType.resolvedName» «property.name» = value.«property.name»();
        if(«property.name» != null) {
            «QName.name» _qname = «QName.name».create(resultName,"«schema.QName.localName»");
            java.util.Iterator _iterator = «property.name».iterator();
            boolean _hasNext = _iterator.hasNext();
            while(_hasNext) {
                Object _listItem = _iterator.next();
                Object _propValue = «property.name»;
                Object _domValue = java.util.Collections.singletonMap(_qname,_propValue);
                childNodes.add(_domValue);
                _hasNext = _iterator.hasNext();
            }
        }
    '''

    /** 
     * Default catch all
     * 
     **/
    private def dispatch CharSequence serializeProperty(DataSchemaNode container, Type type, MethodSignature property) '''
        «property.returnType.resolvedName» «property.name» = value.«property.name»();
        if(«property.name» != null) {
            Object domValue = «property.name»;
            childNodes.add(domValue);
        }
    '''

    private def dispatch CharSequence serializeProperty(DataSchemaNode container, GeneratedTypeBuilder type,
        MethodSignature property) {
        serializeProperty(container, type.toInstance, property)
    }

    private def dispatch CharSequence serializeProperty(DataSchemaNode container, GeneratedType type,
        MethodSignature property) '''
        «property.returnType.resolvedName» «property.name» = value.«property.name»();
        if(«property.name» != null) {
            Object domValue = «type.serializer».toDomStatic(QNAME,«property.name»);
            childNodes.add(domValue);
        }
    '''

    private def dispatch String serializeBody(GeneratedType type, SchemaNode node) '''
        {
            return ($r) java.util.Collections.singletonMap(this.QNAME,null);
        }
    '''

    private def transformatorFqn(GeneratedType typeSpec) {
        return '''«typeSpec.resolvedName»$Broker$Codec$DOM'''
    }

    private def transformatorFqn(Class typeSpec) {
        return '''«typeSpec.name»$Broker$Codec$DOM'''
    }

    private def HashMap<String, MethodSignature> getAllProperties(GeneratedType type) {
        val ret = new HashMap<String, MethodSignature>();
        type.collectAllProperties(ret);
        return ret;
    }

    private def dispatch void collectAllProperties(GeneratedType type, Map<String, MethodSignature> set) {
        for (definition : type.methodDefinitions) {
            set.put(definition.name, definition);
        }

        for (parent : type.implements) {
            parent.collectAllProperties(set);
        }
    }

    private def dispatch void collectAllProperties(Type type, Map<String, MethodSignature> set) {
        // NOOP for generic type.
    }

    def String getResolvedName(Type type) {
        return type.asCtClass.name;
    }

    def CtClass asCtClass(Type type) {
        val name = type.fullyQualifiedName
        val cls = loadClassWithTCCL(type.fullyQualifiedName)
        return cls.asCtClass;
    }

}

@Data
class PropertyPair {

    String getterName;

    Type type;

    @Property
    MethodSignature signature;
    @Property
    SchemaNode schemaNode;
}
