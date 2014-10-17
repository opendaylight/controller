package org.opendaylight.persisted.codec;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.opendaylight.persisted.MD5Identifier;
import org.opendaylight.persisted.ObjectDataStore;
import org.opendaylight.persisted.codec.observers.DefaultMethodFilterObserver;
import org.opendaylight.persisted.codec.observers.DefaultPOJOClassExtractor;
import org.opendaylight.persisted.codec.observers.DefaultPOJOTypeAttribute;
import org.opendaylight.persisted.codec.observers.IChildAttributeObserver;
import org.opendaylight.persisted.codec.observers.IClassExtractorObserver;
import org.opendaylight.persisted.codec.observers.IMethodFilterObserver;
import org.opendaylight.persisted.codec.observers.ITypeAttributeObserver;

public class TypeDescriptor {

    private int classCode = -1;
    private int hierarchyLevel = -1;
    private String typeClassName = null;
    private String typeSerializerClassName = null;
    private AttributeDescriptor attributes[] = new AttributeDescriptor[0];
    private Map<AttributeDescriptor, AttributeDescriptor> children = new HashMap<AttributeDescriptor, AttributeDescriptor>();
    private Map<AttributeDescriptor, AttributeDescriptor> parents = new HashMap<AttributeDescriptor, AttributeDescriptor>();
    private Map<String, AttributeDescriptor> attributeNameToAttribute = new HashMap<String, AttributeDescriptor>();
    private Map<String, AttributeDescriptor> methodNameToAttribute = new HashMap<String, AttributeDescriptor>();
    private Map<Integer, AttributeDescriptor> locationToAttribute = new HashMap<Integer, AttributeDescriptor>();
    private Map<String, Integer> attributeNameToLocation = new HashMap<String, Integer>();
    private Map<String, Integer> methodNameToLocation = new HashMap<String, Integer>();
    private Set<String> knownAugmentingClassNames = new HashSet<String>();
    private Set<Class<?>> knownAugmentingClasses = new HashSet<Class<?>>();
    private Class<?> typeClass = null;
    private ISerializer serializer = null;
    private Field augmentationField = null;
    private boolean augmentationFieldInitialized = false;
    private Field augmentationFieldBuilder = null;
    private boolean augmentationFieldBuilderInitialized = false;
    private AttributeDescriptor keyColumn = null;
    public static boolean REGENERATE_SERIALIZERS = false;
    public static Map<String, Field> refFieldsCache = new HashMap<String, Field>();
    private static List<IChildAttributeObserver> modelChildIdentifierObservers = new ArrayList<IChildAttributeObserver>();
    private static List<ITypeAttributeObserver> modelTypeIdentifierObservers = new ArrayList<ITypeAttributeObserver>();
    private static List<IMethodFilterObserver> methodFilterObservers = new ArrayList<IMethodFilterObserver>();
    private static IClassExtractorObserver classExtractor = new DefaultPOJOClassExtractor();

    static{
        resetObservers();
    }

    private TypeDescriptor() {
    }

    public TypeDescriptor(String _tablename, String _origTableName) {
        this.typeClassName = _tablename;
        this.typeSerializerClassName = _origTableName;
    }

    public TypeDescriptor(Class<?> cls, ISerializer _serializer, int _hierarchyLevel) {
        this.typeClassName = cls.getName();
        this.serializer = _serializer;
        this.hierarchyLevel = _hierarchyLevel;
        if (this.serializer != null) {
            this.typeSerializerClassName = serializer.getClass().getName();
        } else {
            if(!REGENERATE_SERIALIZERS){
                try {
                    String serializerClassName = cls.getName() + "Serializer";
                    this.serializer = (ISerializer) Class.forName(
                            serializerClassName).newInstance();
                    this.typeSerializerClassName = serializer.getClass().getName();
                } catch (Exception err) {}
            }
        }
        this.classCode = TypeDesciptorRepository.getInstance().newCode();
        this.typeClass = cls;

        List<AttributeDescriptor> pList = new ArrayList<AttributeDescriptor>();

        Method mth[] = cls.getMethods();
        for (Method m : mth) {
            if(!isValidModelMethod(m))
                continue;
            AttributeDescriptor p = new AttributeDescriptor(m, cls);
            if(!isValidModelAttribute(p))
                continue;

            if(p.getMethodName().equals("getKey")){
                this.keyColumn = p;
            }

            if (isChildAttribute(p)) {
                children.put(p, p);
                TypeDescriptor child = TypeDesciptorRepository.getInstance().getTypeDescriptorByClass(p.getReturnType(),this.hierarchyLevel+1);
                child.parents.put(p, p);
            } else {
                if (isTypeAttribute(p)) {
                    TypeDesciptorRepository.getInstance().getTypeDescriptorByClass(p.getReturnType());
                }
                pList.add(p);
            }
        }
        this.attributes = pList.toArray(new AttributeDescriptor[pList.size()]);
        if (this.serializer == null) {
            this.serializer = createSerializer(this);
            try {
                this.typeSerializerClassName = this.serializer.getClass().getName();
            } catch (Exception err) {
            }
        }
        initMaps();
    }

    public AttributeDescriptor getKeyColumn(){
        return this.keyColumn;
    }

    public MD5Identifier getMD5IDForObject(Object object){
        if(this.keyColumn==null){
            return null;
        }
        if(object==null){
            return null;
        }
        Object value = this.keyColumn.get(object, null, this.typeClass);
        if(value!=null){
            TypeDescriptor keyTD = TypeDesciptorRepository.getInstance().getTypeDescriptorByObject(value);
            EncodeDataContainer ba = new EncodeDataContainer(128);
            TypeDesciptorRepository.getInstance().getTypeDescriptorByObject(value).getSerializer().encode(value, ba);
            return MD5Identifier.createX(ba.getData());
        }
        return null;
    }

    public Map<AttributeDescriptor, AttributeDescriptor> getChildren() {
        return this.children;
    }

    public static Class<?> getElementClass(Object element) {
        return classExtractor.getObjectClass(element);
    }

    public TypeDescriptor getParent(){
        if(!parents.isEmpty()){
            AttributeDescriptor col = parents.keySet().iterator().next();
            return TypeDesciptorRepository.getInstance().getTypeDescriptorByClassName(col.getClassName());
        }
        return null;
    }

    public Field getAugmentationField(Object value) {
        if (!this.augmentationFieldInitialized) {
            this.augmentationFieldInitialized = true;
            try {
                this.setAugmentationField(TypeDescriptor.findField(
                        value.getClass(), "augmentation"));
            } catch (Exception err) {
                err.printStackTrace();
            }
        }
        return augmentationField;
    }

    public void addToKnownAugmentingClass(Class<?> clazz) {
        if (!this.knownAugmentingClassNames.contains(clazz.getName())) {
            this.knownAugmentingClassNames.add(clazz.getName());
            this.knownAugmentingClasses.add(clazz);
            TypeDescriptor augTable = TypeDesciptorRepository.getInstance().getTypeDescriptorByClass(clazz);
            for(AttributeDescriptor col:augTable.getAttributes()){
                col.setAugmentedTableName(this.getTypeClassShortName());
            }
        }
    }

    public Set<Class<?>> getKnownAugmentingClasses() {
        if (this.knownAugmentingClasses.isEmpty() && !this.knownAugmentingClassNames.isEmpty()) {
            try {
                for (String augClsName : this.knownAugmentingClassNames) {
                    this.knownAugmentingClasses.add(this.getClass().getClassLoader().loadClass(augClsName));
                }
            } catch (Exception err) {
                err.printStackTrace();
            }
        }
        return this.knownAugmentingClasses;
    }

    public void setAugmentationField(Field augmentationField) {
        this.augmentationField = augmentationField;
    }

    public boolean isAugmentationFieldInitialized() {
        return augmentationFieldInitialized;
    }

    public void setAugmentationFieldInitialized(
            boolean augmentationFieldInitialized) {
        this.augmentationFieldInitialized = augmentationFieldInitialized;
    }

    public Field getAugmentationFieldBuilder() {
        return augmentationFieldBuilder;
    }

    public void setAugmentationFieldBuilder(Field augmentationFieldBuilder) {
        this.augmentationFieldBuilder = augmentationFieldBuilder;
    }

    public boolean isAugmentationFieldBuilderInitialized() {
        return augmentationFieldBuilderInitialized;
    }

    public void setAugmentationFieldBuilderInitialized(
            boolean augmentationFieldBuilderInitialized) {
        this.augmentationFieldBuilderInitialized = augmentationFieldBuilderInitialized;
    }

    public void init() {
        getTypeClass();
        initMethods();
        initMaps();
    }

    private void initMaps() {
        for (int i = 0; i < this.attributes.length; i++) {
            attributeNameToAttribute.put(this.attributes[i].getColumnName(),this.attributes[i]);
            methodNameToAttribute.put(this.attributes[i].getMethodName(),this.attributes[i]);
            locationToAttribute.put(i, this.attributes[i]);
            attributeNameToLocation.put(this.attributes[i].getColumnName(), i);
            methodNameToLocation.put(this.attributes[i].getMethodName(), i);
        }
    }

    public AttributeDescriptor[] getAttributes() {
        return this.attributes;
    }

    public int getHierarchyLevel() {
        return this.hierarchyLevel;
    }

    public boolean hasChildren() {
        return !this.children.isEmpty();
    }

    public boolean hasParents() {
        return !this.parents.isEmpty();
    }

    public int getLocationByAttributeName(String name) {
        Integer result = attributeNameToLocation.get(name);
        if (result != null)
            return result;
        return -1;
    }

    public int getLocationByMethodName(String name) {
        Integer result = methodNameToLocation.get(name);
        if (result != null)
            return result;
        if (name.startsWith("set") || name.startsWith("add")
                || name.startsWith("del")) {
            result = methodNameToLocation.get("get" + name.substring(3));
            if (result != null) {
                methodNameToLocation.put(name, result);
                return result;
            }
        }
        return -1;
    }

    public String getAttributeNameByLocation(int i) {
        return locationToAttribute.get(i).getColumnName();
    }

    public String getMetodNameByLocation(int i) {
        return locationToAttribute.get(i).getMethodName();
    }

    private void initMethods() {
        if (this.typeClass == null) {
            try {
                this.typeClass = this.getClass().getClassLoader()
                        .loadClass(this.typeClassName);
            } catch (Exception err) {
                err.printStackTrace();
            }
        }
        for (AttributeDescriptor p : this.attributes) {
            p.initMethod();
        }
    }

    public Object valueOf(String name, String stringVal) {
        AttributeDescriptor p = attributeNameToAttribute.get(name);
        if (!p.isMethodInitialized()) {
            initMethods();
        }
        if (p.isInt()) {
            return Integer.parseInt(stringVal);
        } else if (p.isBoolean()) {
            return Boolean.parseBoolean(stringVal);
        } else if (p.isLong()) {
            return Long.parseLong(stringVal);
        } else if (String.class.equals(p.getReturnType())) {
            return stringVal;
        }
        System.err.println("Unknown Value Type" + p.getReturnType());
        return null;
    }

    public String getSerializerClassName(){
        return this.typeSerializerClassName;
    }

    public ISerializer getSerializer() {
        if (this.serializer == null) {
            try {
                this.serializer = (ISerializer) getClass().getClassLoader().loadClass(this.typeSerializerClassName).newInstance();
            } catch (Exception err) {
            }
            try {
                String directory = "/src/main/yang-gen-sal/"+ getTypeClass().getPackage().getName();
                directory = replaceAll(directory, ".", "/");
                File dir = new File(directory);
                URLClassLoader cl = new URLClassLoader(
                        new URL[] { dir.toURL() }, getClass().getClassLoader());
                this.serializer = (ISerializer) cl.loadClass(
                        this.typeSerializerClassName).newInstance();
            } catch (Exception err) {
                initMethods();
                this.serializer = createSerializer(this);
                this.typeSerializerClassName = this.serializer.getClass().getName();
            }
        }
        return this.serializer;
    }

    public AttributeDescriptor getAttributeByAttributeName(String name) {
        AttributeDescriptor cprop = attributeNameToAttribute.get(name);
        if (cprop != null && !cprop.isMethodInitialized()) {
            initMethods();
        }
        return cprop;
    }

    public AttributeDescriptor getAttributeByMethodName(String name) {
        AttributeDescriptor cprop = methodNameToAttribute.get(name);
        if (!cprop.isMethodInitialized()) {
            initMethods();
        }
        return cprop;
    }

    public Object[] newData() {
        return new Object[this.attributes.length];
    }

    public Class<?> getTypeClass() {
        if (this.typeClass == null) {
            try {
                this.typeClass = getClass().getClassLoader().loadClass(typeClassName);
            } catch (Exception err) {
                err.printStackTrace();
            }
        }
        return this.typeClass;
    }

    public String getTypeClassName() {
        return this.typeClassName;
    }

    public String getTypeClassShortName() {
        int index = this.typeClassName.lastIndexOf(".");
        return this.typeClassName.substring(index + 1);
    }

    public static ISerializer createSerializer(TypeDescriptor type) {
        String data = TypeDescriptor.generateSerializerClass(type);
        String directory = "/src/main/yang-gen-sal/"+ type.getTypeClass().getPackage().getName();
        directory = "." + replaceAll(directory, ".", "/");

        File file = new File(directory);
        if (!file.exists()) {
            file.mkdirs();
        }
        file = new File(file.getPath() + "/"+ type.getTypeClass().getSimpleName() + "Serializer.java");
        try {
            if(!file.exists()){
                FileOutputStream out = new FileOutputStream(file);
                out.write(data.getBytes());
                out.close();
            }
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            compiler.run(null, null, null, file.getPath());
            URLClassLoader cl = new URLClassLoader(new URL[] { new File(
                    "./src/main/yang-gen-sal/").toURL() },
                    ObjectDataStore.class.getClassLoader());
            Class<?> serializerClass = cl.loadClass(type.getTypeClass().getName()
                    + "Serializer");
            ISerializer serializer = (ISerializer) serializerClass
                    .newInstance();
            return serializer;
        } catch (Exception err) {
            err.printStackTrace();
        } finally {
            file = new File(directory + "/" + type.getTypeClass().getSimpleName()
                    + "Serializer.class");
            if (file.exists())
                file.delete();
        }
        return null;
    }

    public static String generateSerializerClass(TypeDescriptor type) {
        StringBuffer buff = new StringBuffer();

        append("", 0, buff);
        append("package " + type.getTypeClass().getPackage().getName() + ";", 0,buff);
        append("import org.opendaylight.persisted.codec.EncodeDataContainer;", 0, buff);
        append("import org.opendaylight.persisted.codec.EncodeUtils;", 0, buff);
        append("import org.opendaylight.persisted.codec.ISerializer;", 0, buff);
        append("import " + type.getTypeClassName() + ";", 0, buff);
        Class<?> builderClass = TypeDescriptor.classExtractor.getBuilderClass(type);
        if (builderClass!=null && !builderClass.equals(type.getTypeClass()))
            append("import " + builderClass.getName()+";", 0, buff);
        append("", 0, buff);
        append("public class " + type.getTypeClass().getSimpleName()
                + "Serializer implements ISerializer{", 0, buff);
        append("@Override", 4, buff);
        append("public void encode(Object value, byte[] byteArray, int location) {",4, buff);
        append("}", 4, buff);
        append("", 0, buff);

        append("@Override", 4, buff);
        append("public void encode(Object value, EncodeDataContainer ba) {", 4, buff);
        append(type.getTypeClass().getSimpleName() + " element = ("+ type.getTypeClass().getSimpleName() + ") value;", 8, buff);
        for (AttributeDescriptor p : type.attributes) {
            if (p.getReturnType().equals(short.class) || p.getReturnType().equals(Short.class)) {
                append("EncodeUtils.encodeShort(element." + p.getMethodName()
                        + "(), ba);", 8, buff);
            } else
            if (p.getReturnType().equals(boolean.class) || p.getReturnType().equals(Boolean.class)) {
                append("EncodeUtils.encodeBoolean(element." + p.getMethodName()
                        + "(), ba);", 8, buff);
            } else
            if (!p.isCollection() && (p.getReturnType().equals(byte.class)
                    || p.getReturnType().equals(Byte.class))) {
                append("EncodeUtils.encodeByte(element." + p.getMethodName()
                        + "(), ba);", 8, buff);
            }else
            if (p.isCollection() && (p.getReturnType().equals(byte.class)
                    || p.getReturnType().equals(Byte.class))) {
                append("EncodeUtils.encodeByteArray(element." + p.getMethodName()
                        + "(), ba);", 8, buff);
            }else
            if (p.getReturnType().equals(BigDecimal.class)) {
                append("EncodeUtils.encodeBigDecimal(element." + p.getMethodName()
                        + "(), ba);", 8, buff);
            }else
            if (p.getReturnType().equals(String.class)) {
                append("EncodeUtils.encodeString(element." + p.getMethodName()
                        + "(), ba);", 8, buff);
            } else if (p.getReturnType().equals(int.class)
                    || p.getReturnType().equals(Integer.class)) {
                append("EncodeUtils.encodeInt32(element." + p.getMethodName()
                        + "(), ba);", 8, buff);
            } else if (p.getReturnType().equals(long.class)
                    || p.getReturnType().equals(Long.class)) {
                append("EncodeUtils.encodeInt64(element." + p.getMethodName()
                        + "(), ba);", 8, buff);
            } else if (TypeDescriptor.isTypeAttribute(p)) {
                append("EncodeUtils.encodeObject(element." + p.getMethodName()
                        + "(), ba, " + p.getReturnType().getName() + ".class);",
                        8, buff);
            }
        }

        if(supportAugmentations(type)){
            append("EncodeUtils.encodeAugmentations(value, ba);", 8, buff);
        }

        if (isChildAttribute(type)) {
            for (AttributeDescriptor child : type.getChildren().keySet()) {
                TypeDescriptor subTable = TypeDesciptorRepository.getInstance().getTypeDescriptorByClass(child.getReturnType());
                if(child.isCollection()){
                    append("EncodeUtils.encodeAndAddList(element."+child.getMethodName()+"(), ba,"+subTable.getTypeClassName()+".class);",8,buff);
                }else{
                    append("EncodeUtils.encodeAndAddObject(element."+child.getMethodName()+"(), ba,"+subTable.getTypeClassName()+".class);",8,buff);
                }
            }
        }

        append("}", 4, buff);
        append("@Override", 4, buff);
        append("public Object decode(byte[] byteArray, int location, int length) {",4, buff);
        append("return null;", 8, buff);
        append("}", 4, buff);
        append("@Override", 4, buff);
        append("public Object decode(EncodeDataContainer ba, int length) {", 4, buff);
        if (builderClass!=null) {
            append(builderClass.getSimpleName()+" builder = new "+ builderClass.getSimpleName()+"();", 8, buff);
            for (AttributeDescriptor p : type.attributes) {
                if (p.getReturnType().equals(short.class) || p.getReturnType().equals(Short.class)) {
                    append("builder.set" + p.getColumnName()+ "(EncodeUtils.decodeShort(ba));", 8, buff);
                }else
                if (p.getReturnType().equals(boolean.class) || p.getReturnType().equals(Boolean.class)) {
                    append("builder.set" + p.getColumnName()
                            + "(EncodeUtils.decodeBoolean(ba));", 8, buff);
                }else
                if (!p.isCollection() && (p.getReturnType().equals(byte.class)
                    || p.getReturnType().equals(Byte.class))) {
                    append("builder.set" + p.getColumnName()
                        + "(EncodeUtils.decodeByte(ba));", 8, buff);
                }else
                if (p.isCollection() && (p.getReturnType().equals(byte.class)
                            || p.getReturnType().equals(Byte.class))) {
                    append("builder.set" + p.getColumnName()
                                + "(EncodeUtils.decodeByteArray(ba));", 8, buff);
                }else
                if (p.getReturnType().equals(BigDecimal.class)) {
                    append("builder.set" + p.getColumnName()
                            + "(EncodeUtils.decodeBigDecimal(ba));", 8, buff);
                }else
                if (p.getReturnType().equals(String.class)) {
                    append("builder.set" + p.getColumnName()
                            + "(EncodeUtils.decodeString(ba));", 8, buff);
                } else if (p.getReturnType().equals(int.class)
                        || p.getReturnType().equals(Integer.class)) {
                    append("builder.set" + p.getColumnName()
                            + "(EncodeUtils.decodeInt32(ba));", 8, buff);
                } else if (p.getReturnType().equals(long.class)
                        || p.getReturnType().equals(Long.class)) {
                    append("builder.set" + p.getColumnName()
                            + "(EncodeUtils.decodeInt64(ba));", 8, buff);
                } else if (TypeDescriptor.isTypeAttribute(p)) {
                    append("builder.set" + p.getColumnName() + "(("
                            + p.getReturnType().getName()
                            + ")EncodeUtils.decodeObject(ba));", 8, buff);
                }
            }

            if(supportAugmentations(type)){
                append("EncodeUtils.decodeAugmentations(builder, ba,"+ type.getTypeClass().getSimpleName() + ".class);", 8,buff);
            }

            if (isChildAttribute(type)) {
                for (AttributeDescriptor child : type.getChildren().keySet()) {
                    TypeDescriptor subTable = TypeDesciptorRepository.getInstance().getTypeDescriptorByClass(child.getReturnType());
                    String shortName = subTable.getTypeClassShortName().toLowerCase();
                    if(child.isCollection()){
                        append("builder.set"+child.getColumnName()+"(EncodeUtils.decodeAndList(ba,"+subTable.getTypeClassName()+".class));",8,buff);
                    }else{
                        append("builder.set"+child.getColumnName()+"(("+subTable.getTypeClassName()+")EncodeUtils.decodeAndObject(ba));",8,buff);
                    }
                }
            }
            if(classExtractor.getBuilderMethod(type)!=null){
                append("return builder."+classExtractor.getBuilderMethod(type)+";", 8, buff);
            }else{
                append("return builder;", 8, buff);
            }
        } else {
            if(!type.getTypeClass().isEnum()){
                append(type.getTypeClass().getSimpleName()
                        + " instance = new "
                        + type.getTypeClass().getSimpleName()+"(",8,buff);
            }
            boolean first = true;
            for (AttributeDescriptor p : type.attributes) {
                if (p.getReturnType().equals(boolean.class) || p.getReturnType().equals(Boolean.class)) {
                    if(!first)
                        append(",",12,buff);
                    first = false;
                    append("EncodeUtils.decodeBoolean(ba)", 8, buff);
                }else
                if (p.getReturnType().equals(String.class)) {
                    if(!first)
                        append(",",12,buff);
                    first = false;
                    append("EncodeUtils.decodeString(ba)", 8, buff);
                } else if (p.getReturnType().equals(int.class) || p.getReturnType().equals(Integer.class)) {
                    if (type.getTypeClass().isEnum()) {
                        append(type.getTypeClass().getSimpleName()
                                + " instance = "
                                + type.getTypeClass().getSimpleName()
                                + ".forValue(EncodeUtils.decodeInt32(ba));",
                                8, buff);
                    } else{
                        if(!first)
                            append(",",12,buff);
                        first = false;
                        append("EncodeUtils.decodeInt32(ba)", 8, buff);
                    }
                } else if (p.getReturnType().equals(long.class) || p.getReturnType().equals(Long.class)) {
                    if(!first)
                        append(",",12,buff);
                    first = false;
                    append("EncodeUtils.decodeInt64(ba)", 8, buff);
                }
            }
            if(!type.getTypeClass().isEnum()){
                append(");",8,buff);
            }
            append("return instance;", 8, buff);
        }

        append("}", 4, buff);

        append("public String getShardName(Object obj) {", 4, buff);
        append("return \"Default\";", 8, buff);
        append("}", 4, buff);
        append("public String getRecordKey(Object obj) {", 4, buff);
        append("return null;", 8, buff);
        append("}", 4, buff);
        append("}", 0, buff);
        return buff.toString();
    }

    private static void append(String text, int level, StringBuffer buff) {
        for (int i = 0; i < level; i++) {
            buff.append(" ");
        }
        buff.append(text);
        buff.append("\n");
    }

    public void encode(EncodeDataContainer ba) {
        EncodeUtils.encodeInt16(classCode, ba);
        EncodeUtils.encodeString(typeClassName, ba);
        EncodeUtils.encodeString(typeSerializerClassName, ba);
        EncodeUtils.encodeInt16(hierarchyLevel, ba);
        if(this.keyColumn!=null){
            EncodeUtils.encodeString(this.keyColumn.getClassName(), ba);
            EncodeUtils.encodeString(this.keyColumn.getMethodName(), ba);
        }else{
            EncodeUtils.encodeString(null, ba);
            EncodeUtils.encodeString(null, ba);
        }
        EncodeUtils.encodeInt16(attributes.length, ba);
        for (AttributeDescriptor p : attributes) {
            p.encode(ba);
        }
        EncodeUtils.encodeInt16(children.size(), ba);
        for (AttributeDescriptor p : children.keySet()) {
            p.encode(ba);
        }
        EncodeUtils.encodeInt16(parents.size(), ba);
        for (AttributeDescriptor p : parents.keySet()) {
            p.encode(ba);
        }
        EncodeUtils.encodeInt16(knownAugmentingClassNames.size(), ba);
        for (String augClass : knownAugmentingClassNames) {
            EncodeUtils.encodeString(augClass, ba);
        }
    }

    public static TypeDescriptor decode(EncodeDataContainer ba) {

        TypeDescriptor t = new TypeDescriptor();

        t.classCode = EncodeUtils.decodeInt16(ba);
        t.typeClassName = EncodeUtils.decodeString(ba);
        t.typeSerializerClassName = EncodeUtils.decodeString(ba);
        t.hierarchyLevel = EncodeUtils.decodeInt16(ba);
        String str1 = EncodeUtils.decodeString(ba);
        String str2 = EncodeUtils.decodeString(ba);
        if(str1!=null){
            t.keyColumn = new AttributeDescriptor(str2,str1);
        }
        t.attributes = new AttributeDescriptor[EncodeUtils.decodeInt16(ba)];
        for (int i = 0; i < t.attributes.length; i++) {
            t.attributes[i] = AttributeDescriptor.decode(ba);
        }

        int size = EncodeUtils.decodeInt16(ba);
        for (int i = 0; i < size; i++) {
            AttributeDescriptor p = AttributeDescriptor.decode(ba);
            t.children.put(p, p);
        }

        size = EncodeUtils.decodeInt16(ba);
        for (int i = 0; i < size; i++) {
            AttributeDescriptor p = AttributeDescriptor.decode(ba);
            t.parents.put(p, p);
        }

        size = EncodeUtils.decodeInt16(ba);
        for (int i = 0; i < size; i++) {
            String augClass = EncodeUtils.decodeString(ba);
            t.knownAugmentingClassNames.add(augClass);
        }
        return t;
    }

    public int getClassCode() {
        return this.classCode;
    }

    public static String replaceAll(String src, String that, String withThis) {
        StringBuffer buff = new StringBuffer();
        int index0 = 0;
        int index1 = src.indexOf(that);
        if (index1 == -1)
            return src;
        while (index1 != -1) {
            buff.append(src.substring(index0, index1));
            buff.append(withThis);
            index0 = index1 + that.length();
            index1 = src.indexOf(that, index0);
        }
        buff.append(src.substring(index0));
        return buff.toString();
    }

    public static Field findField(Class<?> c, String name) {
        if (c == null) {
            return null;
        }
        String cacheKey = c.getName() + name;
        Field f = refFieldsCache.get(cacheKey);
        if (f != null) {
            return f;
        }

        try {
            f = c.getDeclaredField(name);
            f.setAccessible(true);
            refFieldsCache.put(cacheKey, f);
            return f;
        } catch (Exception err) {
        }

        Class<?> s = c.getSuperclass();
        if (s != null) {
            f = findField(s, name);
            if (f != null) {
                refFieldsCache.put(cacheKey, f);
            }
            return f;
        }
        return null;
    }

    public AttributeDescriptor addAttribute(String _logicalFieldName,String _logicalTableName, String _origFieldName,String _origTableName) {
        return null;
    }

    public static void setClassExtractor(IClassExtractorObserver ce){
        classExtractor = ce;
    }

    public static void addChildAttributeObserver(IChildAttributeObserver rule){
        modelChildIdentifierObservers.add(rule);
    }

    public static void addMethodFilterObserver(IMethodFilterObserver rule){
        methodFilterObservers.add(rule);
    }

    public static void addTypeAttributeObserver(ITypeAttributeObserver rule){
        modelTypeIdentifierObservers.add(rule);
    }

    public static void clearObservers(){
        modelTypeIdentifierObservers.clear();
        methodFilterObservers.clear();
        modelChildIdentifierObservers.clear();
    }

    public static void resetObservers(){
        clearObservers();
        methodFilterObservers.add(new DefaultMethodFilterObserver());
        modelTypeIdentifierObservers.add(new DefaultPOJOTypeAttribute());
        classExtractor = new DefaultPOJOClassExtractor();
    }

    private static boolean isChildAttribute(AttributeDescriptor ad){
        for(IChildAttributeObserver rule:modelChildIdentifierObservers){
            if(rule.isChildAttribute(ad))
                return true;
        }
        return false;
    }

    private static boolean isValidModelMethod(Method m){
        for(IMethodFilterObserver rule:methodFilterObservers){
            if(!rule.isValidModelMethod(m))
                return false;
        }
        return true;
    }

    private static boolean isValidModelAttribute(AttributeDescriptor ad){
        for(IMethodFilterObserver rule:methodFilterObservers){
            if(!rule.isValidAttribute(ad))
                return false;
        }
        return true;
    }

    private static boolean isChildAttribute(TypeDescriptor td){
        for(IChildAttributeObserver rule:modelChildIdentifierObservers){
            if(rule.isChildAttribute(td))
                return true;
        }
        return false;
    }

    private static boolean supportAugmentations(TypeDescriptor td){
        for(IChildAttributeObserver rule:modelChildIdentifierObservers){
            if(rule.supportAugmentation(td))
                return true;
        }
        return false;
    }

    private static boolean isTypeAttribute(AttributeDescriptor ad){
        for(ITypeAttributeObserver rule:modelTypeIdentifierObservers){
            if(rule.isTypeAttribute(ad))
                return true;
        }
        return false;
    }
}
