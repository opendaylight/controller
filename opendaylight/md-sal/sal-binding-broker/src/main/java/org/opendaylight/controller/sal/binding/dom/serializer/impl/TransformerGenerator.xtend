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
import static org.opendaylight.controller.sal.binding.dom.serializer.impl.CodecMapping.*
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode
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
import java.util.List
import java.util.TreeSet
import com.google.common.base.Joiner
import org.opendaylight.yangtools.sal.binding.model.api.GeneratedTransferObject
import org.opendaylight.yangtools.sal.binding.model.api.Enumeration
import org.opendaylight.yangtools.yang.model.api.ChoiceCaseNode
import static org.opendaylight.controller.sal.binding.impl.util.ClassLoaderUtils.*;
import org.opendaylight.yangtools.yang.binding.BindingDeserializer
import org.opendaylight.yangtools.yang.binding.BindingCodec
import org.slf4j.LoggerFactory
import org.opendaylight.controller.sal.binding.codegen.CodeGenerationException
import org.opendaylight.yangtools.yang.model.api.ChoiceNode
import java.security.ProtectionDomain
import java.io.File
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yangtools.sal.binding.model.api.GeneratedProperty
import java.util.Map.Entry
import java.util.AbstractMap.SimpleEntry
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.Augmentation
import java.util.Iterator
import org.opendaylight.yangtools.yang.model.api.AugmentationSchema
import java.util.concurrent.ConcurrentHashMap
import static extension org.opendaylight.controller.sal.binding.impl.util.YangSchemaUtils.*;
import org.opendaylight.yangtools.binding.generator.util.ReferencedTypeImpl
import org.opendaylight.yangtools.yang.model.util.ExtendedType
import org.opendaylight.yangtools.yang.model.util.EnumerationType
import static com.google.common.base.Preconditions.*
import org.opendaylight.yangtools.yang.model.api.SchemaPath
import javassist.CtMethod
import javassist.CannotCompileException
import java.util.concurrent.locks.Lock
import java.util.concurrent.Callable
import org.opendaylight.controller.sal.binding.impl.util.ClassLoaderUtils
import org.opendaylight.yangtools.yang.model.api.TypeDefinition
import org.opendaylight.yangtools.yang.model.api.type.UnionTypeDefinition
import org.opendaylight.yangtools.yang.model.api.type.BitsTypeDefinition
import java.util.HashSet
import java.util.Collections
import org.opendaylight.yangtools.yang.model.api.type.BitsTypeDefinition.Bit
import java.util.Set
import org.opendaylight.controller.sal.binding.codegen.impl.XtendHelper

class TransformerGenerator {

    private static val log = LoggerFactory.getLogger(TransformerGenerator)

    public static val STRING = Types.typeForClass(String);
    public static val BOOLEAN = Types.typeForClass(Boolean);
    public static val INTEGER = Types.typeForClass(Integer);
    public static val INSTANCE_IDENTIFIER = Types.typeForClass(InstanceIdentifier)

    //public static val DECIMAL = Types.typeForClass(Decimal);
    public static val LONG = Types.typeForClass(Long);

    val ClassPool classPool
    val extension JavassistUtils utils;

    CtClass BINDING_CODEC

    CtClass ctQName

    @Property
    var File classFileCapturePath;

    @Property
    var Map<Type, Type> typeDefinitions = new ConcurrentHashMap();

    @Property
    var Map<Type, GeneratedTypeBuilder> typeToDefinition = new ConcurrentHashMap();

    @Property
    var Map<SchemaPath, GeneratedTypeBuilder> pathToType = new ConcurrentHashMap();

    @Property
    var Map<Type, SchemaNode> typeToSchemaNode = new ConcurrentHashMap();

    @Property
    var Map<Type, AugmentationSchema> typeToAugmentation = new ConcurrentHashMap();

    @Property
    var GeneratorListener listener;

    public static val CLASS_TYPE = Types.typeForClass(Class);

    public new(ClassPool pool) {
        classPool = pool;
        utils = new JavassistUtils(pool)

        BINDING_CODEC = BindingCodec.asCtClass;
        ctQName = QName.asCtClass
    }

    def Class<? extends BindingCodec<Map<QName, Object>, Object>> transformerFor(Class<?> inputType) {
        return withClassLoaderAndLock(inputType.classLoader, lock) [ |
            val ret = getGeneratedClass(inputType)
            if (ret !== null) {
                listener.onClassProcessed(inputType);
                return ret as Class<? extends BindingCodec<Map<QName,Object>, Object>>;
            }
            val ref = Types.typeForClass(inputType)
            val node = typeToSchemaNode.get(ref)
            val typeSpecBuilder = typeToDefinition.get(ref)
            checkState(typeSpecBuilder !== null, "Could not find typedefinition for %s", inputType.name);
            val typeSpec = typeSpecBuilder.toInstance();
            val newret = generateTransformerFor(inputType, typeSpec, node);
            listener.onClassProcessed(inputType);
            return newret as Class<? extends BindingCodec<Map<QName,Object>, Object>>;
        ]
    }

    def Class<? extends BindingCodec<Map<QName, Object>, Object>> transformerFor(Class<?> inputType, DataSchemaNode node) {
        return withClassLoaderAndLock(inputType.classLoader, lock) [ |
            val ret = getGeneratedClass(inputType)
            if (ret !== null) {
                listener.onClassProcessed(inputType);
                return ret as Class<? extends BindingCodec<Map<QName,Object>, Object>>;
            }
            val ref = Types.typeForClass(inputType)
            var typeSpecBuilder = typeToDefinition.get(ref)
            if (typeSpecBuilder == null) {
                typeSpecBuilder = pathToType.get(node.path);
            }
            checkState(typeSpecBuilder !== null, "Could not find TypeDefinition for %s, $s", inputType.name, node);
            val typeSpec = typeSpecBuilder.toInstance();
            val newret = generateTransformerFor(inputType, typeSpec, node);
            listener.onClassProcessed(inputType);
            return newret as Class<? extends BindingCodec<Map<QName,Object>, Object>>;
        ]
    }

    def Class<? extends BindingCodec<Map<QName, Object>, Object>> augmentationTransformerFor(Class<?> inputType) {
        return withClassLoaderAndLock(inputType.classLoader, lock) [ |
            val ret = getGeneratedClass(inputType)
            if (ret !== null) {
                return ret as Class<? extends BindingCodec<Map<QName,Object>, Object>>;
            }
            val ref = Types.typeForClass(inputType)
            val node = typeToAugmentation.get(ref)
            val typeSpecBuilder = typeToDefinition.get(ref)
            val typeSpec = typeSpecBuilder.toInstance();
            val newret = generateAugmentationTransformerFor(inputType, typeSpec, node);
            listener.onClassProcessed(inputType);
            return newret as Class<? extends BindingCodec<Map<QName,Object>, Object>>;
        ]
    }

    def Class<? extends BindingCodec<Object, Object>> caseCodecFor(Class<?> inputType, ChoiceCaseNode node) {
        return withClassLoaderAndLock(inputType.classLoader, lock) [ |
            val ret = getGeneratedClass(inputType)
            if (ret !== null) {
                return ret as Class<? extends BindingCodec<Object, Object>>;
            }
            val ref = Types.typeForClass(inputType)
            val typeSpecBuilder = typeToDefinition.get(ref)
            val typeSpec = typeSpecBuilder.toInstance();
            val newret = generateCaseCodec(inputType, typeSpec, node);
            return newret as Class<? extends BindingCodec<Object, Object>>;
        ]
    }

    def Class<? extends BindingCodec<Map<QName, Object>, Object>> keyTransformerForIdentifiable(Class<?> parentType) {
        return withClassLoaderAndLock(parentType.classLoader, lock) [ |
            val inputName = parentType.name + "Key";
            val inputType = loadClassWithTCCL(inputName);
            val ret = getGeneratedClass(inputType)
            if (ret !== null) {
                return ret as Class<? extends BindingCodec<Map<QName,Object>, Object>>;
            }
            val ref = Types.typeForClass(parentType)
            val node = typeToSchemaNode.get(ref) as ListSchemaNode
            val typeSpecBuilder = typeToDefinition.get(ref)
            val typeSpec = typeSpecBuilder.identifierDefinition;
            val newret = generateKeyTransformerFor(inputType, typeSpec, node);
            return newret as Class<? extends BindingCodec<Map<QName,Object>, Object>>;
        ]
    }

    def getIdentifierDefinition(GeneratedTypeBuilder builder) {
        val inst = builder.toInstance
        val keyMethod = inst.methodDefinitions.findFirst[name == "getKey"]
        return keyMethod.returnType as GeneratedTransferObject
    }

    def Class<? extends BindingCodec<Map<QName, Object>, Object>> keyTransformerForIdentifier(Class<?> inputType) {
        return withClassLoaderAndLock(inputType.classLoader, lock) [ |
            val ret = getGeneratedClass(inputType)
            if (ret !== null) {
                return ret as Class<? extends BindingCodec<Map<QName,Object>, Object>>;
            }
            val ref = Types.typeForClass(inputType)
            val node = typeToSchemaNode.get(ref) as ListSchemaNode
            val typeSpecBuilder = typeToDefinition.get(ref)
            val typeSpec = typeSpecBuilder.toInstance();
            val newret = generateKeyTransformerFor(inputType, typeSpec, node);
            return newret as Class<? extends BindingCodec<Map<QName,Object>, Object>>;
        ]
    }

    private def Class<?> keyTransformerFor(Class<?> inputType, GeneratedType type, ListSchemaNode schema) {
        return withClassLoaderAndLock(inputType.classLoader, lock) [ |
            val transformer = getGeneratedClass(inputType)
            if (transformer != null) {
                return transformer;
            }
            val newret = generateKeyTransformerFor(inputType, type, schema);
            return newret as Class<? extends BindingCodec<Map<QName,Object>, Object>>;
        ]
    }

    private def Class<?> getGeneratedClass(Class<? extends Object> cls) {

        try {
            return loadClassWithTCCL(cls.codecClassName)
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private def Class<?> keyTransformer(GeneratedType type, ListSchemaNode node) {
        val cls = loadClassWithTCCL(type.resolvedName + "Key");
        keyTransformerFor(cls, type, node);
    }

    private def serializer(Type type, DataSchemaNode node) {
        val cls = loadClassWithTCCL(type.resolvedName);
        transformerFor(cls, node);
    }

    private def Class<?> valueSerializer(GeneratedTransferObject type, TypeDefinition<?> typeDefinition) {
        val cls = loadClassWithTCCL(type.resolvedName);
        val transformer = cls.generatedClass;
        if (transformer !== null) {
            return transformer;
        }
        var baseType = typeDefinition;
        while (baseType.baseType != null) {
            baseType = baseType.baseType;
        }
        val finalType = baseType;
        return withClassLoaderAndLock(cls.classLoader, lock) [ |
            val valueTransformer = generateValueTransformer(cls, type, finalType);
            return valueTransformer;
        ]
    }

    private def Class<?> valueSerializer(Enumeration type, TypeDefinition<?> typeDefinition) {
        val cls = loadClassWithTCCL(type.resolvedName);
        val transformer = cls.generatedClass;
        if (transformer !== null) {
            return transformer;
        }

        return withClassLoaderAndLock(cls.classLoader, lock) [ |
            val valueTransformer = generateValueTransformer(cls, type);
            return valueTransformer;
        ]
    }

    private def generateKeyTransformerFor(Class<? extends Object> inputType, GeneratedType typeSpec, ListSchemaNode node) {
        try {

            //log.info("Generating DOM Codec for {} with {}", inputType, inputType.classLoader)
            val properties = typeSpec.allProperties;
            val ctCls = createClass(inputType.codecClassName) [
                //staticField(Map,"AUGMENTATION_SERIALIZERS");
                staticField(it, INSTANCE_IDENTIFIER_CODEC, BindingCodec)
                staticField(it, IDENTITYREF_CODEC, BindingCodec)
                staticQNameField(node.QName);
                implementsType(BINDING_CODEC)
                method(Object, "toDomStatic", QName, Object) [
                    modifiers = PUBLIC + FINAL + STATIC
                    bodyChecked = '''
                        {
                            «QName.name» _resultName;
                            if($1 != null) {
                                _resultName = «QName.name».create($1,QNAME.getLocalName());
                            } else {
                                _resultName = QNAME;
                            }
                            java.util.List _childNodes = new java.util.ArrayList();
                            «inputType.resolvedName» value = («inputType.name») $2;
                            «FOR key : node.keyDefinition»
                                «val propertyName = key.getterName»
                                «val keyDef = node.getDataChildByName(key)»
                                «val property = properties.get(propertyName)»
                                «serializeProperty(keyDef, property, propertyName)»;
                            «ENDFOR»
                            return ($r) java.util.Collections.singletonMap(_resultName,_childNodes);
                        }
                    '''
                ]
                method(Object, "fromDomStatic", QName, Object) [
                    modifiers = PUBLIC + FINAL + STATIC
                    bodyChecked = '''
                        {
                            if($2 == null){
                                return  null;
                            }
                            «QName.name» _localQName = $1;
                            java.util.Map _compositeNode = (java.util.Map) $2;
                            boolean _is_empty = true;
                            «FOR key : node.keyDefinition»
                                «val propertyName = key.getterName»
                                «val keyDef = node.getDataChildByName(key)»
                                «val property = properties.get(propertyName)»
                                «deserializeProperty(keyDef, property, propertyName)»;
                            «ENDFOR»
                            «inputType.resolvedName» _value = new «inputType.name»(«node.keyDefinition.
                            keyConstructorList»);
                            return _value;
                        }
                    '''
                ]
                method(Object, "serialize", Object) [
                    bodyChecked = '''
                        {
                            java.util.Map.Entry _input =  (java.util.Map.Entry) $1;
                            «QName.name» _localQName = («QName.name») _input.getKey();
                            «inputType.name» _keyValue = («inputType.name») _input.getValue();
                            return toDomStatic(_localQName,_keyValue);
                        }
                    '''
                ]
                method(Object, "deserialize", Object) [
                    bodyChecked = '''
                        return fromDomStatic(QNAME,$1);
                    '''
                ]
            ]
            val ret = ctCls.toClassImpl(inputType.classLoader, inputType.protectionDomain)
            log.debug("DOM Codec for {} was generated {}", inputType, ret)
            return ret as Class<? extends BindingCodec<Map<QName,Object>, ?>>;
        } catch (Exception e) {
            processException(inputType, e);
            return null;
        }
    }

    private def Class<? extends BindingCodec<Object, Object>> generateCaseCodec(Class<?> inputType, GeneratedType type,
        ChoiceCaseNode node) {
        try {

            //log.info("Generating DOM Codec for {} with {}, TCCL is: {}", inputType, inputType.classLoader,Thread.currentThread.contextClassLoader)
            val ctCls = createClass(type.codecClassName) [
                //staticField(Map,"AUGMENTATION_SERIALIZERS");
                implementsType(BINDING_CODEC)
                staticQNameField(node.QName);
                staticField(it, INSTANCE_IDENTIFIER_CODEC, BindingCodec)
                staticField(it, AUGMENTATION_CODEC, BindingCodec)
                staticField(it, IDENTITYREF_CODEC, BindingCodec)
                method(Object, "toDomStatic", QName, Object) [
                    modifiers = PUBLIC + FINAL + STATIC
                    bodyChecked = '''
                        {
                            «QName.name» _resultName = «QName.name».create($1,QNAME.getLocalName());
                            java.util.List _childNodes = new java.util.ArrayList();
                            «type.resolvedName» value = («type.resolvedName») $2;
                            «transformDataContainerBody(type, type.allProperties, node)»
                            return ($r) _childNodes;
                        }
                    '''
                ]
                method(Object, "serialize", Object) [
                    bodyChecked = '''
                        {
                            java.util.Map.Entry _input = (java.util.Map.Entry) $1;
                            «QName.name» _localName = QNAME;
                            if(_input.getKey() != null) {
                                _localName = («QName.name») _input.getKey();
                            }
                            return toDomStatic(_localName,_input.getValue());
                        }
                    '''
                ]
                method(Object, "fromDomStatic", QName, Object) [
                    modifiers = PUBLIC + FINAL + STATIC
                    bodyChecked = deserializeBody(type, node)
                ]
                method(Object, "deserialize", Object) [
                    bodyChecked = '''
                        {
                            ////System.out.println("«type.name»#deserialize: " +$1);
                            java.util.Map.Entry _input = (java.util.Map.Entry) $1;
                            return fromDomStatic((«QName.name»)_input.getKey(),_input.getValue());
                        }
                    '''
                ]
            ]

            val ret = ctCls.toClassImpl(inputType.classLoader, inputType.protectionDomain)  as Class<? extends BindingCodec<Object, Object>>
            listener?.onDataContainerCodecCreated(inputType, ret);
            log.debug("DOM Codec for {} was generated {}", inputType, ret)
            return ret;
        } catch (Exception e) {
            processException(inputType, e);
            return null;
        }
    }

    private def dispatch  Class<? extends BindingCodec<Map<QName, Object>, Object>> generateTransformerFor(
        Class<?> inputType, GeneratedType typeSpec, SchemaNode node) {
        try {

            //log.info("Generating DOM Codec for {} with {}", inputType, inputType.classLoader)
            val ctCls = createClass(typeSpec.codecClassName) [
                //staticField(Map,"AUGMENTATION_SERIALIZERS");
                staticQNameField(node.QName);
                staticField(it, INSTANCE_IDENTIFIER_CODEC, BindingCodec)
                staticField(it, IDENTITYREF_CODEC, BindingCodec)
                staticField(it, AUGMENTATION_CODEC, BindingCodec)
                implementsType(BINDING_CODEC)
                method(Object, "toDomStatic", QName, Object) [
                    modifiers = PUBLIC + FINAL + STATIC
                    bodyChecked = serializeBodyFacade(typeSpec, node)
                ]
                method(Object, "serialize", Object) [
                    bodyChecked = '''
                        {
                            java.util.Map.Entry _input = (java.util.Map.Entry) $1;
                            «QName.name» _localName = QNAME;
                            if(_input.getKey() != null) {
                                _localName = («QName.name») _input.getKey();
                            }
                            return toDomStatic(_localName,_input.getValue());
                        }
                    '''
                ]
                method(Object, "fromDomStatic", QName, Object) [
                    modifiers = PUBLIC + FINAL + STATIC
                    bodyChecked = deserializeBody(typeSpec, node)
                ]
                method(Object, "deserialize", Object) [
                    bodyChecked = '''
                        return fromDomStatic(QNAME,$1);
                    '''
                ]
            ]

            val ret = ctCls.toClassImpl(inputType.classLoader, inputType.protectionDomain) as Class<? extends BindingCodec<Map<QName,Object>, Object>>
            listener?.onDataContainerCodecCreated(inputType, ret);
            log.debug("DOM Codec for {} was generated {}", inputType, ret)
            return ret;
        } catch (Exception e) {
            processException(inputType, e);
            return null;
        }
    }

    private def Class<? extends BindingCodec<Map<QName, Object>, Object>> generateAugmentationTransformerFor(
        Class<?> inputType, GeneratedType type, AugmentationSchema node) {
        try {

            //log.info("Generating DOM Codec for {} with {}", inputType, inputType.classLoader)
            val properties = type.allProperties
            val ctCls = createClass(type.codecClassName) [
                //staticField(Map,"AUGMENTATION_SERIALIZERS");
                staticQNameField(node.augmentationQName);
                staticField(it, INSTANCE_IDENTIFIER_CODEC, BindingCodec)
                staticField(it, AUGMENTATION_CODEC, BindingCodec)
                staticField(it, IDENTITYREF_CODEC, BindingCodec)
                implementsType(BINDING_CODEC)
                method(Object, "toDomStatic", QName, Object) [
                    modifiers = PUBLIC + FINAL + STATIC
                    bodyChecked = '''
                        {
                            ////System.out.println("Qname " + $1);
                            ////System.out.println("Value " + $2);
                            «QName.name» _resultName = «QName.name».create(QNAME,QNAME.getLocalName());
                            java.util.List _childNodes = new java.util.ArrayList();
                            «type.resolvedName» value = («type.resolvedName») $2;
                            «FOR child : node.childNodes»
                                «var signature = properties.getFor(child)»
                                ////System.out.println("«signature.key»" + value.«signature.key»());
                                «serializeProperty(child, signature.value, signature.key)»
                            «ENDFOR»
                            return ($r) _childNodes;
                        }
                    '''
                ]
                method(Object, "serialize", Object) [
                    bodyChecked = '''
                        {
                        java.util.Map.Entry _input = (java.util.Map.Entry) $1;
                        «QName.name» _localName = QNAME;
                        if(_input.getKey() != null) {
                            _localName = («QName.name») _input.getKey();
                        }
                        return toDomStatic(_localName,_input.getValue());
                        }
                    '''
                ]
                method(Object, "fromDomStatic", QName, Object) [
                    modifiers = PUBLIC + FINAL + STATIC
                    bodyChecked = '''
                        {
                            «QName.name» _localQName = QNAME;
                            
                            if($2 == null) {
                            return null;
                            }
                            java.util.Map _compositeNode = (java.util.Map) $2;
                            ////System.out.println(_localQName + " " + _compositeNode);
                            «type.builderName» _builder = new «type.builderName»();
                            boolean _is_empty = true;
                            «FOR child : node.childNodes»
                                «val signature = properties.getFor(child)»
                                «deserializeProperty(child, signature.value, signature.key)»
                                _builder.«signature.key.toSetter»(«signature.key»);
                            «ENDFOR»
                            if(_is_empty) {
                                return null;
                            }
                            return _builder.build();
                        }
                    '''
                ]
                method(Object, "deserialize", Object) [
                    bodyChecked = '''
                        return fromDomStatic(QNAME,$1);
                    '''
                ]
            ]

            val ret = ctCls.toClassImpl(inputType.classLoader, inputType.protectionDomain) as Class<? extends BindingCodec<Map<QName,Object>, Object>>
            listener?.onDataContainerCodecCreated(inputType, ret);
            return ret;
        } catch (Exception e) {
            processException(inputType, e);
            return null;
        }
    }

    private def dispatch  Class<? extends BindingCodec<Map<QName, Object>, Object>> generateTransformerFor(
        Class<?> inputType, GeneratedType typeSpec, ChoiceNode node) {
        try {

            //log.info("Generating DOM Codec for {} with {}", inputType, inputType.classLoader)
            val ctCls = createClass(typeSpec.codecClassName) [
                //staticField(Map,"AUGMENTATION_SERIALIZERS");
                //staticQNameField(inputType);
                staticField(it, INSTANCE_IDENTIFIER_CODEC, BindingCodec)
                staticField(it, IDENTITYREF_CODEC, BindingCodec)
                staticField(it, CLASS_TO_CASE_MAP, Map)
                staticField(it, COMPOSITE_TO_CASE, Map)
                //staticField(it,QNAME_TO_CASE_MAP,BindingCodec)
                implementsType(BINDING_CODEC)
                method(List, "toDomStatic", QName, Object) [
                    modifiers = PUBLIC + FINAL + STATIC
                    bodyChecked = '''
                        {
                            if($2 == null) {
                                return null;
                            }
                            «DataObject.name» _baValue = («DataObject.name») $2;
                            Class _baClass = _baValue.getImplementedInterface();
                            «BINDING_CODEC.name» _codec =  «CLASS_TO_CASE_MAP».get(_baClass);
                            if(_codec == null) {
                                return null;
                            }
                            java.util.Map.Entry _input = new «SimpleEntry.name»($1,_baValue);
                            Object _ret =  _codec.serialize(_input);
                            ////System.out.println("«typeSpec.name»#toDomStatic: " + _ret);
                            return («List.name») _ret;
                        }
                    '''
                ]
                method(Object, "serialize", Object) [
                    bodyChecked = '''
                        throw new «UnsupportedOperationException.name»("Direct invocation not supported.");
                    '''
                ]
                method(Object, "fromDomStatic", QName, Map) [
                    modifiers = PUBLIC + FINAL + STATIC
                    bodyChecked = '''
                        {
                            «BINDING_CODEC.name» _codec = («BINDING_CODEC.name») «COMPOSITE_TO_CASE».get($2);
                            if(_codec != null) {
                                return _codec.deserialize(new «SimpleEntry.name»($1,$2));
                            }
                            return null;
                        }
                    '''
                ]
                method(Object, "deserialize", Object) [
                    bodyChecked = '''
                        throw new «UnsupportedOperationException.name»("Direct invocation not supported.");
                    '''
                ]
            ]

            val rawRet = ctCls.toClassImpl(inputType.classLoader, inputType.protectionDomain)
            val ret = rawRet as Class<? extends BindingCodec<Map<QName,Object>, Object>>;
            listener?.onChoiceCodecCreated(inputType, ret, node);
            log.debug("DOM Codec for {} was generated {}", inputType, ret)
            return ret;
        } catch (Exception e) {
            processException(inputType, e);
            return null;
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
            «deserializeAugmentations»
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
            «deserializeAugmentations»
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
            ////System.out.println(_localQName + " " + _compositeNode);
            «type.builderName» _builder = new «type.builderName»();
            «deserializeDataNodeContainerBody(type, node)»
            «deserializeAugmentations»
            return _builder.build();
        }
    '''

    private def deserializeDataNodeContainerBody(GeneratedType type, DataNodeContainer node) {
        deserializeNodeContainerBodyImpl(type, type.allProperties, node);
    }

    private def deserializeNodeContainerBodyImpl(GeneratedType type, HashMap<String, Type> properties,
        DataNodeContainer node) {
        val ret = '''
            boolean _is_empty = true;
            «FOR child : node.childNodes»
                «val signature = properties.getFor(child)»
                «IF signature !== null»
                    «deserializeProperty(child, signature.value, signature.key)»
                    _builder.«signature.key.toSetter»(«signature.key»);
                «ENDIF»
            «ENDFOR»
        '''
        return ret;
    }

    def deserializeAugmentations() '''
        java.util.Map _augmentation = (java.util.Map) «AUGMENTATION_CODEC».deserialize(_compositeNode);
        if(_augmentation != null) {
            «Iterator.name» _entries = _augmentation.entrySet().iterator();
            while(_entries.hasNext()) {
                java.util.Map.Entry _entry = (java.util.Map.Entry) _entries.next();
                ////System.out.println("Aug. key:" + _entry.getKey());
                Class _type = (Class) _entry.getKey();
                «Augmentation.resolvedName» _value = («Augmentation.name») _entry.getValue();
                if(_value != null) {
                    _builder.addAugmentation(_type,_value);
                }
            }
        }
    '''

    private def dispatch CharSequence deserializeProperty(ListSchemaNode schema, ParameterizedType type,
        String propertyName) '''
        java.util.List _dom_«propertyName» = _compositeNode.get(«QName.name».create(_localQName,"«schema.QName.
            localName»"));
        ////System.out.println("«propertyName»#deCode"+_dom_«propertyName»);
        java.util.List «propertyName» = new java.util.ArrayList();
        if(_dom_«propertyName» != null) {
            java.util.List _serialized = new java.util.ArrayList();
            java.util.Iterator _iterator = _dom_«propertyName».iterator();
            boolean _hasNext = _iterator.hasNext();
            while(_hasNext) {
                Object _listItem = _iterator.next();
                _is_empty = false;
                ////System.out.println("  item" + _listItem);
                Object _value = «type.actualTypeArguments.get(0).serializer(schema).resolvedName».fromDomStatic(_localQName,_listItem);
                ////System.out.println("  value" + _value);
                «propertyName».add(_value);
                _hasNext = _iterator.hasNext();
            }
        }
        
        ////System.out.println(" list" + «propertyName»);
    '''

    private def dispatch CharSequence deserializeProperty(LeafListSchemaNode schema, ParameterizedType type,
        String propertyName) '''
        java.util.List _dom_«propertyName» = _compositeNode.get(«QName.name».create(_localQName,"«schema.QName.
            localName»"));
        java.util.List «propertyName» = new java.util.ArrayList();
        if(_dom_«propertyName» != null) {
            java.util.List _serialized = new java.util.ArrayList();
            java.util.Iterator _iterator = _dom_«propertyName».iterator();
            boolean _hasNext = _iterator.hasNext();
            while(_hasNext) {
                _is_empty = false;
                Object _listItem = _iterator.next();
                if(_listItem instanceof java.util.Map.Entry) {
                    Object _innerValue = ((java.util.Map.Entry) _listItem).getValue();
                    Object _value = «deserializeValue(type.actualTypeArguments.get(0), "_innerValue", schema.type)»;
                    «propertyName».add(_value);
                }
                _hasNext = _iterator.hasNext();
            }
        }
    '''

    private def dispatch CharSequence deserializeProperty(LeafSchemaNode schema, Type type, String propertyName) '''
        java.util.List _dom_«propertyName»_list = 
            _compositeNode.get(«QName.name».create(_localQName,"«schema.QName.localName»"));
        «type.resolvedName» «propertyName» = null;
        if(_dom_«propertyName»_list != null && _dom_«propertyName»_list.size() > 0) {
            _is_empty = false;
            java.util.Map.Entry _dom_«propertyName» = (java.util.Map.Entry) _dom_«propertyName»_list.get(0);
            Object _inner_value = _dom_«propertyName».getValue();
            «propertyName» = «deserializeValue(type, "_inner_value", schema.type)»;
        }
    '''

    private def dispatch CharSequence deserializeProperty(ContainerSchemaNode schema, Type type,
        String propertyName) '''
        java.util.List _dom_«propertyName»_list = 
            _compositeNode.get(«QName.name».create(_localQName,"«schema.QName.localName»"));
        «type.resolvedName» «propertyName» = null;
        if(_dom_«propertyName»_list != null && _dom_«propertyName»_list.size() > 0) {
            _is_empty = false;
            java.util.Map _dom_«propertyName» = (java.util.Map) _dom_«propertyName»_list.get(0);
            «propertyName» =  «type.serializer(schema).resolvedName».fromDomStatic(_localQName,_dom_«propertyName»);
        }
    '''

    private def dispatch CharSequence deserializeProperty(ChoiceNode schema, Type type, String propertyName) '''
        «type.resolvedName» «propertyName» = «type.serializer(schema).resolvedName».fromDomStatic(_localQName,_compositeNode);
        if(«propertyName» != null) {
            _is_empty = false;
        }
    '''

    private def dispatch String deserializeValue(GeneratedTransferObject type, String domParameter,
        TypeDefinition<?> typeDefinition) '''
        («type.resolvedName») «type.valueSerializer(typeDefinition).resolvedName».fromDomValue(«domParameter»)
    '''

    private def dispatch String deserializeValue(Enumeration type, String domParameter, TypeDefinition<?> typeDefinition) '''
        («type.resolvedName») «type.valueSerializer(typeDefinition).resolvedName».fromDomValue(«domParameter»)
    '''

    private def dispatch Class<? extends BindingCodec<Map<QName, Object>, Object>> generateValueTransformer(
        Class<?> inputType, GeneratedTransferObject typeSpec, TypeDefinition<?> typeDef) {
        try {

            val returnType = typeSpec.valueReturnType;
            if (returnType == null) {
                val ctCls = createDummyImplementation(inputType, typeSpec);
                val ret = ctCls.toClassImpl(inputType.classLoader, inputType.protectionDomain)
                return ret as Class<? extends BindingCodec<Map<QName,Object>, Object>>;
            }

            val ctCls = createClass(typeSpec.codecClassName) [
                //staticField(Map,"AUGMENTATION_SERIALIZERS");
                if (inputType.isYangBindingAvailable) {
                    implementsType(BINDING_CODEC)
                    staticField(it, INSTANCE_IDENTIFIER_CODEC, BindingCodec)
                    staticField(it, IDENTITYREF_CODEC, BindingCodec)
                    implementsType(BindingDeserializer.asCtClass)
                }
                method(Object, "toDomValue", Object) [
                    modifiers = PUBLIC + FINAL + STATIC
                    val ctSpec = typeSpec.asCtClass;
                    bodyChecked = '''
                        {
                            ////System.out.println("«inputType.simpleName»#toDomValue: "+$1);
                            
                            if($1 == null) {
                                return null;
                            }
                            «typeSpec.resolvedName» _encapsulatedValue = («typeSpec.resolvedName») $1;
                            ////System.out.println("«inputType.simpleName»#toDomValue:Enc: "+_encapsulatedValue);
                            «returnType.resolvedName» _value =  _encapsulatedValue.getValue();
                            ////System.out.println("«inputType.simpleName»#toDomValue:DeEnc: "+_value);
                            Object _domValue = «serializeValue(returnType, "_value", null)»;
                            return _domValue;
                        }
                    '''
                ]
                method(Object, "serialize", Object) [
                    bodyChecked = '''
                        {
                            return toDomValue($1);
                        }
                    '''
                ]
                method(Object, "fromDomValue", Object) [
                    modifiers = PUBLIC + FINAL + STATIC
                    bodyChecked = '''
                        {
                            ////System.out.println("«inputType.simpleName»#fromDomValue: "+$1);
                            
                            if($1 == null) {
                                return null;
                            }
                            «returnType.resolvedName» _simpleValue = «deserializeValue(returnType, "$1", null)»;
                            «typeSpec.resolvedName» _value = new «typeSpec.resolvedName»(_simpleValue);
                            return _value;
                        }
                    '''
                ]
                method(Object, "deserialize", Object) [
                    bodyChecked = '''{
                            return fromDomValue($1);
                    }
                    '''
                ]
            ]

            val ret = ctCls.toClassImpl(inputType.classLoader, inputType.protectionDomain)
            log.debug("DOM Codec for {} was generated {}", inputType, ret)
            return ret as Class<? extends BindingCodec<Map<QName,Object>, Object>>;
        } catch (Exception e) {
            log.error("Cannot compile DOM Codec for {}", inputType, e);
            val exception = new CodeGenerationException("Cannot compile Transformator for " + inputType);
            exception.addSuppressed(e);
            throw exception;
        }
    }

    private def dispatch Class<? extends BindingCodec<Map<QName, Object>, Object>> generateValueTransformer(
        Class<?> inputType, GeneratedTransferObject typeSpec, UnionTypeDefinition typeDef) {
        try {
            val ctCls = createClass(typeSpec.codecClassName) [
                val properties = typeSpec.allProperties;
                val getterToTypeDefinition = XtendHelper.getTypes(typeDef).toMap[type | type.QName.getterName];
                //staticField(Map,"AUGMENTATION_SERIALIZERS");
                if (inputType.isYangBindingAvailable) {
                    implementsType(BINDING_CODEC)
                    staticField(it, INSTANCE_IDENTIFIER_CODEC, BindingCodec)
                    staticField(it, IDENTITYREF_CODEC, BindingCodec)
                    implementsType(BindingDeserializer.asCtClass)
                }
                method(Object, "toDomValue", Object) [
                    modifiers = PUBLIC + FINAL + STATIC
                    val ctSpec = inputType.asCtClass;
                    
                    bodyChecked = '''
                        {
                            ////System.out.println("«inputType.simpleName»#toDomValue: "+$1);
                            
                            if($1 == null) {
                                return null;
                            }
                            «typeSpec.resolvedName» _value = («typeSpec.resolvedName») $1;
                            «FOR property : properties.entrySet»
                                «IF property.key != "getValue"»
                                    «property.value.resolvedName» «property.key» = («property.value.resolvedName») _value.«property.
                            key»();
                                    if(«property.key» != null) { 
                                        return «serializeValue(property.value, property.key, getterToTypeDefinition.get(property.key))»;
                                    }
                                «ENDIF»
                            «ENDFOR»
                            
                            return null;
                        }
                    '''
                ]
                method(Object, "serialize", Object) [
                    bodyChecked = '''
                        {
                            return toDomValue($1);
                        }
                    '''
                ]
                method(Object, "fromDomValue", Object) [
                    modifiers = PUBLIC + FINAL + STATIC
                    bodyChecked = '''
                        {
                            ////System.out.println("«inputType.simpleName»#fromDomValue: "+$1);
                            
                            if($1 == null) {
                                return null;
                            }
                            if($1 instanceof String) {
                                String _simpleValue = (String) $1;
                                return new «typeSpec.resolvedName»(_simpleValue.toCharArray());
                            }
                            return null;
                        }
                    '''
                ]
                method(Object, "deserialize", Object) [
                    bodyChecked = '''{
                            return fromDomValue($1);
                    }
                    '''
                ]
            ]

            val ret = ctCls.toClassImpl(inputType.classLoader, inputType.protectionDomain)
            log.debug("DOM Codec for {} was generated {}", inputType, ret)
            return ret as Class<? extends BindingCodec<Map<QName,Object>, Object>>;
        } catch (Exception e) {
            log.error("Cannot compile DOM Codec for {}", inputType, e);
            val exception = new CodeGenerationException("Cannot compile Transformator for " + inputType);
            exception.addSuppressed(e);
            throw exception;
        }
    }


    private def dispatch Class<? extends BindingCodec<Map<QName, Object>, Object>> generateValueTransformer(
        Class<?> inputType, GeneratedTransferObject typeSpec, BitsTypeDefinition typeDef) {
        try {
            val ctCls = createClass(typeSpec.codecClassName) [
                //staticField(Map,"AUGMENTATION_SERIALIZERS");
                if (inputType.isYangBindingAvailable) {
                    implementsType(BINDING_CODEC)
                    staticField(it, INSTANCE_IDENTIFIER_CODEC, BindingCodec)
                    staticField(it, IDENTITYREF_CODEC, BindingCodec)
                    implementsType(BindingDeserializer.asCtClass)
                }
                method(Object, "toDomValue", Object) [
                    modifiers = PUBLIC + FINAL + STATIC
                    val ctSpec = typeSpec.asCtClass;
                    bodyChecked = '''
                        {
                            ////System.out.println("«inputType.simpleName»#toDomValue: "+$1);
                            
                            if($1 == null) {
                                return null;
                            }
                            «typeSpec.resolvedName» _encapsulatedValue = («typeSpec.resolvedName») $1;
                            «HashSet.resolvedName» _value = new «HashSet.resolvedName»();
                            //System.out.println("«inputType.simpleName»#toDomValue:Enc: "+_encapsulatedValue);
                            
                            «FOR bit : typeDef.bits»
                                «val getter = bit.getterName()»
                                if(Boolean.TRUE.equals(_encapsulatedValue.«getter»())) {
                                    _value.add("«bit.name»");
                                }
                            «ENDFOR»
                            «Set.resolvedName» _domValue =  «Collections.resolvedName».unmodifiableSet(_value);
                            //System.out.println("«inputType.simpleName»#toDomValue:DeEnc: "+_domValue);
                            
                            return _domValue;
                        }
                    '''
                ]
                method(Object, "serialize", Object) [
                    bodyChecked = '''
                        {
                            return toDomValue($1);
                        }
                    '''
                ]
                method(Object, "fromDomValue", Object) [
                    modifiers = PUBLIC + FINAL + STATIC
                    val sortedBits = typeDef.bits.sort[o1, o2|o1.propertyName.compareTo(o2.propertyName)]
                    bodyChecked = '''
                        {
                            //System.out.println("«inputType.simpleName»#fromDomValue: "+$1);
                            
                            if($1 == null) {
                                return null;
                            }
                            «Set.resolvedName» _domValue = («Set.resolvedName») $1;
                            «FOR bit : sortedBits»
                                Boolean «bit.propertyName» = Boolean.valueOf(_domValue.contains("«bit.name»"));
                            «ENDFOR»
                            
                            return new «inputType.resolvedName»(«FOR bit : sortedBits SEPARATOR ","»«bit.propertyName»«ENDFOR»);
                        }
                    '''
                ]
                method(Object, "deserialize", Object) [
                    bodyChecked = '''{
                            return fromDomValue($1);
                    }
                    '''
                ]
            ]

            val ret = ctCls.toClassImpl(inputType.classLoader, inputType.protectionDomain)
            log.debug("DOM Codec for {} was generated {}", inputType, ret)
            return ret as Class<? extends BindingCodec<Map<QName,Object>, Object>>;
        } catch (Exception e) {
            log.error("Cannot compile DOM Codec for {}", inputType, e);
            val exception = new CodeGenerationException("Cannot compile Transformator for " + inputType);
            exception.addSuppressed(e);
            throw exception;
        }
    }

    def String getPropertyName(Bit bit) {
        '''_«BindingGeneratorUtil.parseToValidParamName(bit.name)»'''
    }

    def String getterName(Bit bit) {

        val paramName = BindingGeneratorUtil.parseToValidParamName(bit.name);
        return '''is«paramName.toFirstUpper»''';
    }

    def boolean isYangBindingAvailable(Class<?> class1) {
        try {
            val bindingCodecClass = class1.classLoader.loadClass(BINDING_CODEC.name);
            return bindingCodecClass !== null;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private def createDummyImplementation(Class<?> object, GeneratedTransferObject typeSpec) {
        log.info("Generating Dummy DOM Codec for {} with {}", object, object.classLoader)
        return createClass(typeSpec.codecClassName) [
            if (object.isYangBindingAvailable) {
                implementsType(BINDING_CODEC)
                staticField(it, INSTANCE_IDENTIFIER_CODEC, BindingCodec)
                staticField(it, IDENTITYREF_CODEC, BindingCodec)
                implementsType(BindingDeserializer.asCtClass)
            }
            //implementsType(BindingDeserializer.asCtClass)
            method(Object, "toDomValue", Object) [
                modifiers = PUBLIC + FINAL + STATIC
                bodyChecked = '''{
                    if($1 == null) {
                        return null;
                    }
                    return $1.toString();
                    
                    }'''
            ]
            method(Object, "serialize", Object) [
                bodyChecked = '''
                    {
                        return toDomValue($1);
                    }
                '''
            ]
            method(Object, "fromDomValue", Object) [
                modifiers = PUBLIC + FINAL + STATIC
                bodyChecked = '''return null;'''
            ]
            method(Object, "deserialize", Object) [
                bodyChecked = '''{
                        return fromDomValue($1);
                    }
                    '''
            ]
        ]
    }

    private def Type getValueReturnType(GeneratedTransferObject object) {
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

    private def Class<?> generateValueTransformer(Class<?> inputType, Enumeration typeSpec) {
        try {
            val typeRef = new ReferencedTypeImpl(typeSpec.packageName, typeSpec.name);
            val schema = typeToSchemaNode.get(typeRef) as ExtendedType;
            val enumSchema = schema.baseType as EnumerationType;

            //log.info("Generating DOM Codec for {} with {}", inputType, inputType.classLoader)
            val ctCls = createClass(typeSpec.codecClassName) [
                //staticField(Map,"AUGMENTATION_SERIALIZERS");
                //implementsType(BINDING_CODEC)
                method(Object, "toDomValue", Object) [
                    modifiers = PUBLIC + FINAL + STATIC
                    bodyChecked = '''{
                            if($1 == null) {
                                return null;
                            }
                            «typeSpec.resolvedName» _value = («typeSpec.resolvedName») $1;
                            «FOR en : enumSchema.values»
                            if(«typeSpec.resolvedName».«BindingGeneratorUtil.parseToClassName(en.name)».equals(_value)) {
                                return "«en.name»";
                            }
                            «ENDFOR»
                            return null;
                        }
                    '''
                ]
                method(Object, "serialize", Object) [
                    bodyChecked = '''
                        return toDomValue($1);
                    '''
                ]
                method(Object, "fromDomValue", Object) [
                    modifiers = PUBLIC + FINAL + STATIC
                    bodyChecked = '''
                        {
                            if($1 == null) {
                                return null;
                            }
                            String _value = (String) $1;
                            «FOR en : enumSchema.values»
                                if("«en.name»".equals(_value)) {
                                    return «typeSpec.resolvedName».«BindingGeneratorUtil.parseToClassName(en.name)»;
                                }
                            «ENDFOR»
                            return null;
                        }
                    '''
                ]
                method(Object, "deserialize", Object) [
                    bodyChecked = '''
                        return fromDomValue($1);
                    '''
                ]
            ]

            val ret = ctCls.toClassImpl(inputType.classLoader, inputType.protectionDomain)
            log.debug("DOM Codec for {} was generated {}", inputType, ret)
            return ret;
        } catch (CodeGenerationException e) {
            throw new CodeGenerationException("Cannot compile Transformator for " + inputType, e);
        } catch (Exception e) {
            log.error("Cannot compile DOM Codec for {}", inputType, e);
            val exception = new CodeGenerationException("Cannot compile Transformator for " + inputType);
            exception.addSuppressed(e);
            throw exception;
        }

    }

    def Class<?> toClassImpl(CtClass newClass, ClassLoader loader, ProtectionDomain domain) {
        val cls = newClass.toClass(loader, domain);
        if (classFileCapturePath !== null) {
            newClass.writeFile(classFileCapturePath.absolutePath);
        }
        listener?.onCodecCreated(cls);
        return cls;
    }

    def debugWriteClass(CtClass class1) {
        val path = class1.name.replace(".", "/") + ".class"

        val captureFile = new File(classFileCapturePath, path);
        captureFile.createNewFile

    }

    private def dispatch String deserializeValue(Type type, String domParameter, TypeDefinition<?> typeDef) {
        if (INSTANCE_IDENTIFIER.equals(type)) {
            return '''(«InstanceIdentifier.name») «INSTANCE_IDENTIFIER_CODEC».deserialize(«domParameter»)'''
        } else if (CLASS_TYPE.equals(type)) {
            return '''(«Class.name») «IDENTITYREF_CODEC».deserialize(«domParameter»)'''
        }
        return '''(«type.resolvedName») «domParameter»'''

    }

    /** 
     * Default catch all
     * 
     **/
    private def dispatch CharSequence deserializeProperty(DataSchemaNode container, Type type, String propertyName) '''
        «type.resolvedName» «propertyName» = null;
    '''

    private def dispatch CharSequence deserializeProperty(DataSchemaNode container, GeneratedTypeBuilder type,
        String propertyName) {
        _deserializeProperty(container, type.toInstance, propertyName)
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
    private def dispatch CharSequence deserializeProperty(DataSchemaNode container,GeneratedType type, String propertyName) '''
        «type.resolvedName» «propertyName» = value.«propertyName»();
        if(«propertyName» != null) {
            Object domValue = «type.serializer».toDomStatic(QNAME,«propertyName»);
            _childNodes.add(domValue);
        }
    '''
    */
    private def getBuilderName(GeneratedType type) '''«type.resolvedName»Builder'''

    private def staticQNameField(CtClass it, QName node) {
        val field = new CtField(ctQName, "QNAME", it);
        field.modifiers = PUBLIC + FINAL + STATIC;
        addField(field,
            '''«QName.asCtClass.name».create("«node.namespace»","«node.formattedRevision»","«node.localName»")''')
    }

    private def dispatch String serializeBody(GeneratedType type, ListSchemaNode node) '''
        {
            «QName.name» _resultName = «QName.name».create($1,QNAME.getLocalName());
            java.util.List _childNodes = new java.util.ArrayList();
            «type.resolvedName» value = («type.resolvedName») $2;
            «transformDataContainerBody(type, type.allProperties, node)»
            «serializeAugmentations»
            return ($r) java.util.Collections.singletonMap(_resultName,_childNodes);
        }
    '''

    private def dispatch String serializeBody(GeneratedType type, ContainerSchemaNode node) '''
        {
            «QName.name» _resultName = «QName.name».create($1,QNAME.getLocalName());
            java.util.List _childNodes = new java.util.ArrayList();
            «type.resolvedName» value = («type.resolvedName») $2;
            «transformDataContainerBody(type, type.allProperties, node)»
            «serializeAugmentations»
            return ($r) java.util.Collections.singletonMap(_resultName,_childNodes);
        }
    '''

    private def dispatch String serializeBody(GeneratedType type, ChoiceCaseNode node) '''
        {
        «QName.name» _resultName = «QName.name».create($1,QNAME.getLocalName());
            java.util.List _childNodes = new java.util.ArrayList();
            «type.resolvedName» value = («type.resolvedName») $2;
            «transformDataContainerBody(type, type.allProperties, node)»
            «serializeAugmentations»
            return ($r) java.util.Collections.singletonMap(_resultName,_childNodes);
        }
    '''

    private def dispatch String serializeBody(GeneratedType type, SchemaNode node) '''
        {
        «QName.name» _resultName = «QName.name».create($1,QNAME.getLocalName());
            java.util.List _childNodes = new java.util.ArrayList();
            «type.resolvedName» value = («type.resolvedName») $2;
            return ($r) java.util.Collections.singletonMap(_resultName,_childNodes);
        }
    '''

    private def transformDataContainerBody(Type type, Map<String, Type> properties, DataNodeContainer node) {
        val ret = '''
            «FOR child : node.childNodes»
                «val signature = properties.getFor(child)»
                «IF signature !== null»
                    ////System.out.println("«type.name»#«signature.key»" + value.«signature.key»());
                    «serializeProperty(child, signature.value, signature.key)»
                «ENDIF»
            «ENDFOR»
        '''
        return ret;
    }

    private def serializeAugmentations() '''
        java.util.List _augmentations = (java.util.List) «AUGMENTATION_CODEC».serialize(value);
        if(_augmentations != null) {
            _childNodes.addAll(_augmentations);
        }
    '''

    def Entry<String, Type> getFor(Map<String, Type> map, DataSchemaNode node) {
        var sig = map.get(node.getterName);
        if (sig != null) {
            return new SimpleEntry(node.getterName, sig);
        }
        sig = map.get(node.booleanGetterName);
        if (sig != null) {
            return new SimpleEntry(node.booleanGetterName, map.get(node.booleanGetterName));
        }
        return null;
    }

    private static def String getBooleanGetterName(DataSchemaNode node) {
        return "is" + BindingGeneratorUtil.parseToClassName(node.QName.localName);
    }

    private static def String getGetterName(DataSchemaNode node) {
        return "get" + BindingGeneratorUtil.parseToClassName(node.QName.localName);
    }

    private static def String getGetterName(QName node) {
        return "get" + BindingGeneratorUtil.parseToClassName(node.localName);
    }

    private def dispatch CharSequence serializeProperty(ListSchemaNode schema, ParameterizedType type,
        String propertyName) '''
        «type.resolvedName» «propertyName» = value.«propertyName»();
        ////System.out.println("«propertyName»:" + «propertyName»);
        if(«propertyName» != null) {
            java.util.Iterator _iterator = «propertyName».iterator();
            boolean _hasNext = _iterator.hasNext();
            while(_hasNext) {
                Object _listItem = _iterator.next();
                Object _domValue = «type.actualTypeArguments.get(0).serializer(schema).resolvedName».toDomStatic(_resultName,_listItem);
                _childNodes.add(_domValue);
                _hasNext = _iterator.hasNext();
            }
        }
    '''

    private def dispatch CharSequence serializeProperty(LeafSchemaNode schema, Type type, String propertyName) '''
        «type.resolvedName» «propertyName» = value.«propertyName»();
        
        if(«propertyName» != null) {
            «QName.name» _qname = «QName.name».create(_resultName,"«schema.QName.localName»");
            Object _propValue = «serializeValue(type, propertyName, schema.type)»;
            if(_propValue != null) {
                Object _domValue = java.util.Collections.singletonMap(_qname,_propValue);
                _childNodes.add(_domValue);
            }
        }
    '''

    private def dispatch serializeValue(GeneratedTransferObject type, String parameter, TypeDefinition<?> typeDefinition) {
        '''«type.valueSerializer(typeDefinition).resolvedName».toDomValue(«parameter»)'''
    }

    private def dispatch serializeValue(Enumeration type, String parameter, TypeDefinition<?> typeDefinition) {
        '''«type.valueSerializer(typeDefinition).resolvedName».toDomValue(«parameter»)'''
    }

    private def dispatch serializeValue(Type signature, String property, TypeDefinition<?> typeDefinition) {
        if (INSTANCE_IDENTIFIER == signature) {
            return '''«INSTANCE_IDENTIFIER_CODEC».serialize(«property»)'''
        } else if (CLASS_TYPE.equals(signature)) {
            return '''(«QName.resolvedName») «IDENTITYREF_CODEC».serialize(«property»)'''
        }
        if ("char[]" == signature.name) {
            return '''new String(«property»)''';
        }
        return '''«property»''';
    }

    private def dispatch CharSequence serializeProperty(LeafListSchemaNode schema, ParameterizedType type,
        String propertyName) '''
        «type.resolvedName» «propertyName» = value.«propertyName»();
        if(«propertyName» != null) {
            «QName.name» _qname = «QName.name».create(_resultName,"«schema.QName.localName»");
            java.util.Iterator _iterator = «propertyName».iterator();
            boolean _hasNext = _iterator.hasNext();
            while(_hasNext) {
                Object _listItem = _iterator.next();
                Object _propValue = «serializeValue(type.actualTypeArguments.get(0), "_listItem", schema.type)»;
                Object _domValue = java.util.Collections.singletonMap(_qname,_propValue);
                _childNodes.add(_domValue);
                _hasNext = _iterator.hasNext();
            }
        }
    '''

    private def dispatch CharSequence serializeProperty(ChoiceNode container, GeneratedType type,
        String propertyName) '''
        «type.resolvedName» «propertyName» = value.«propertyName»();
        if(«propertyName» != null) {
            java.util.List domValue = «type.serializer(container).resolvedName».toDomStatic(_resultName,«propertyName»);
            _childNodes.addAll(domValue);
        }
    '''

    /** 
     * Default catch all
     * 
     **/
    private def dispatch CharSequence serializeProperty(DataSchemaNode container, Type type, String propertyName) '''
        «type.resolvedName» «propertyName» = value.«propertyName»();
        if(«propertyName» != null) {
            Object domValue = «propertyName»;
            _childNodes.add(domValue);
        }
    '''

    private def dispatch CharSequence serializeProperty(DataSchemaNode container, GeneratedTypeBuilder type,
        String propertyName) {
        serializeProperty(container, type.toInstance, propertyName)
    }

    private def dispatch CharSequence serializeProperty(DataSchemaNode container, GeneratedType type,
        String propertyName) '''
        «type.resolvedName» «propertyName» = value.«propertyName»();
        if(«propertyName» != null) {
            Object domValue = «type.serializer(container).resolvedName».toDomStatic(_resultName,«propertyName»);
            _childNodes.add(domValue);
        }
    '''

    private def codecClassName(GeneratedType typeSpec) {
        return '''«typeSpec.resolvedName»$Broker$Codec$DOM'''
    }

    private def codecClassName(Class<?> typeSpec) {
        return '''«typeSpec.name»$Broker$Codec$DOM'''
    }

    private def HashMap<String, Type> getAllProperties(GeneratedType type) {
        val ret = new HashMap<String, Type>();
        type.collectAllProperties(ret);
        return ret;
    }

    private def dispatch void collectAllProperties(GeneratedType type, Map<String, Type> set) {
        for (definition : type.methodDefinitions) {
            set.put(definition.name, definition.returnType);
        }
        for (property : type.properties) {
            set.put(property.getterName, property.returnType);
        }
        for (parent : type.implements) {
            parent.collectAllProperties(set);
        }
    }

    def String getGetterName(GeneratedProperty property) {
        return "get" + property.name.toFirstUpper
    }

    private def dispatch void collectAllProperties(Type type, Map<String, Type> set) {
        // NOOP for generic type.
    }

    def String getResolvedName(Type type) {
        return type.asCtClass.name;
    }

    def String getResolvedName(Class<?> type) {
        return type.asCtClass.name;
    }

    def CtClass asCtClass(Type type) {
        val cls = loadClassWithTCCL(type.fullyQualifiedName)
        return cls.asCtClass;
    }

    private def dispatch processException(Class<?> inputType, CodeGenerationException e) {
        log.error("Cannot compile DOM Codec for {}. One of it's prerequisites was not generated.", inputType);
        throw e;
    }

    private def dispatch processException(Class<?> inputType, Exception e) {
        log.error("Cannot compile DOM Codec for {}", inputType, e);
        val exception = new CodeGenerationException("Cannot compile Transformator for " + inputType, e);
        throw exception;
    }

    private def setBodyChecked(CtMethod method, String body) {
        try {
            method.setBody(body);
        } catch (CannotCompileException e) {
            log.error("Cannot compile method: {}#{} {}, Reason: {} Body: {}", method.declaringClass, method.name,
                method.signature, e.message, body)
            throw e;
        }
    }

    private def <V> V withClassLoaderAndLock(ClassLoader cls, Lock lock, Callable<V> function) throws Exception {
        appendClassLoaderIfMissing(cls);
        ClassLoaderUtils.withClassLoaderAndLock(cls, lock, function);
    }

}

@Data
class PropertyPair {

    String getterName;

    Type type;

    @Property
    Type returnType;
    @Property
    SchemaNode schemaNode;
}
