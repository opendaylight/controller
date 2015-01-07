package org.opendaylight.datasand.codec;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.opendaylight.datasand.codec.bytearray.ByteArrayEncodeDataContainer;

public class TypeDescriptor {

    public static boolean REGENERATE_SERIALIZERS = false;
    private static Map<String, Field> refFieldsCache = new HashMap<String, Field>();

    static{
        AbstractEncoder.registerSerializer(TypeDescriptor.class, new TypeDescriptorSerializer(), FrameworkClassCodes.CODE_TypeDescriptor);
    }

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
    private Map<String,String> knownAugmentingClassNames = new ConcurrentHashMap<String,String>();
    private Map<Class<?>,Class<?>> knownAugmentingClasses = new ConcurrentHashMap<Class<?>,Class<?>>();
    private Class<?> typeClass = null;
    private ISerializer serializer = null;
    private Field augmentationField = null;
    private boolean augmentationFieldInitialized = false;
    private Field augmentationFieldBuilder = null;
    private boolean augmentationFieldBuilderInitialized = false;
    private AttributeDescriptor keyColumn = null;
    private TypeDescriptorsContainer container = null;

    private TypeDescriptor() {
    }

    public TypeDescriptor(String _tablename, String _origTableName,TypeDescriptorsContainer _container) {
        this.typeClassName = _tablename;
        this.typeSerializerClassName = _origTableName;
        this.container = _container;
    }

    public TypeDescriptor(Class<?> cls, ISerializer _serializer, int _hierarchyLevel,TypeDescriptorsContainer _container) {
        this.typeClassName = cls.getName();
        this.serializer = _serializer;
        this.hierarchyLevel = _hierarchyLevel;
        this.container = _container;

        if (this.serializer != null) {
            this.typeSerializerClassName = serializer.getClass().getName();
        } else {
            this.serializer = AbstractEncoder.getRegisteredSerializer(cls);
            if(serializer==null){
                if(!REGENERATE_SERIALIZERS){
                    try {
                        String serializerClassName = cls.getName() + "Serializer";
                        this.serializer = (ISerializer) Class.forName(serializerClassName).newInstance();
                        this.typeSerializerClassName = serializer.getClass().getName();
                    } catch (Exception err) {}
                }
            }
        }
        Integer _classCode = AbstractEncoder.getClassCodeByClass(cls);
        if(_classCode!=null)
            this.classCode = _classCode;
        else
            this.classCode = container.newCode();
        this.typeClass = cls;

        List<AttributeDescriptor> pList = new ArrayList<AttributeDescriptor>();

        Method mth[] = cls.getMethods();
        for (Method m : mth) {
            if(!container.isValidModelMethod(m))
                continue;
            AttributeDescriptor p = new AttributeDescriptor(m, cls);
            if(!container.isValidModelAttribute(p))
                continue;

            if(p.getMethodName().equals("getKey")){
                this.keyColumn = p;
            }

            if (container.isChildAttribute(p)) {
                children.put(p, p);
                TypeDescriptor child = container.getTypeDescriptorByClass(p.getReturnType(),this.hierarchyLevel+1);
                child.parents.put(p, p);
            } else {
                if (container.isTypeAttribute(p)) {
                    if(container.checkTypeDescriptorByClass(p.getReturnType())==null){
                        container.newTypeDescriptor(p.getReturnType(), 0);
                    }
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
            ByteArrayEncodeDataContainer ba = new ByteArrayEncodeDataContainer(128,this.container);
            container.getTypeDescriptorByObject(value).getSerializer().encode(value, ba);
            return MD5Identifier.createX(ba.getData());
        }
        return null;
    }

    public Map<AttributeDescriptor, AttributeDescriptor> getChildren() {
        return this.children;
    }

    public TypeDescriptor getParent(){
        if(!parents.isEmpty()){
            AttributeDescriptor col = parents.keySet().iterator().next();
            return container.getTypeDescriptorByClassName(col.getClassName());
        }
        return null;
    }

    public Field getAugmentationField(Object value) {
        if (!this.augmentationFieldInitialized) {
            synchronized(this.container){
                if(!this.augmentationFieldBuilderInitialized){
                    try {
                        this.setAugmentationField(TypeDescriptor.findField(value.getClass(), "augmentation"));
                    } catch (Exception err) {
                        err.printStackTrace();
                    }
                    this.augmentationFieldInitialized = true;
                }
            }
        }
        return augmentationField;
    }

    public void addToKnownAugmentingClass(Class<?> clazz) {
        if (!this.knownAugmentingClassNames.containsKey(clazz.getName())) {
            synchronized(this.container){
                if(!this.knownAugmentingClassNames.containsKey(clazz.getName())){
                    this.knownAugmentingClassNames.put(clazz.getName(),clazz.getName());
                    this.knownAugmentingClasses.put(clazz,clazz);
                    TypeDescriptor augTable = container.getTypeDescriptorByClass(clazz);
                    for(AttributeDescriptor col:augTable.getAttributes()){
                        col.setAugmentedTableName(this.getTypeClassShortName());
                    }
                }
            }
        }
    }

    public Map<Class<?>,Class<?>> getKnownAugmentingClasses() {
        if (this.knownAugmentingClasses.isEmpty() && !this.knownAugmentingClassNames.isEmpty()) {
            synchronized(this.container){
                if (this.knownAugmentingClasses.isEmpty() && !this.knownAugmentingClassNames.isEmpty()) {
                    try {
                        for (String augClsName : this.knownAugmentingClassNames.keySet()) {
                            Class<?> cls = this.getClass().getClassLoader().loadClass(augClsName);
                            this.knownAugmentingClasses.put(cls,cls);
                        }
                    } catch (Exception err) {
                        err.printStackTrace();
                    }
                }
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

    public TypeDescriptorsContainer getTypeDescriptorsContainer(){
        return this.container;
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
                @SuppressWarnings("deprecation")
                URLClassLoader cl = new URLClassLoader(new URL[] { dir.toURL() }, getClass().getClassLoader());
                this.serializer = (ISerializer) cl.loadClass(this.typeSerializerClassName).newInstance();
                cl.close();
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
        String serializerName = type.getTypeClass().getSimpleName();
        if(serializerName.indexOf("$")!=-1){
            serializerName = serializerName.substring(serializerName.indexOf("$")+1);
        }
        serializerName+="Serializer.";
        file = new File(file.getPath() + "/"+ serializerName+"java");
        try {
            if(!file.exists()){
                FileOutputStream out = new FileOutputStream(file);
                out.write(data.getBytes());
                out.close();
            }
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            compiler.run(null, null, null, file.getPath());
            @SuppressWarnings("deprecation")
            URLClassLoader cl = new URLClassLoader(new URL[] { new File("./src/main/yang-gen-sal/").toURL() },TypeDescriptor.class.getClassLoader());
            Class<?> serializerClass = cl.loadClass(type.getTypeClass().getName()+ "Serializer");
            ISerializer serializer = (ISerializer) serializerClass.newInstance();
            cl.close();
            return serializer;
        } catch (Exception err) {
            err.printStackTrace();
        } finally {
            if(REGENERATE_SERIALIZERS){
                file.delete();
                file = new File(directory + "/" + serializerName+ "class");
                if (file.exists())
                    file.delete();
            }
        }
        return null;
    }

    public static String generateSerializerClass(TypeDescriptor type) {

        TypeDescriptorsContainer container = type.getTypeDescriptorsContainer();
        StringBuffer buff = new StringBuffer();

        append("", 0, buff);
        append("package " + type.getTypeClass().getPackage().getName() + ";", 0,buff);
        append("import org.opendaylight.datasand.codec.EncodeDataContainer;", 0, buff);
        append("import org.opendaylight.datasand.codec.ISerializer;", 0, buff);
        String className = type.getTypeClassName();
        className = replaceAll(className, "$", ".");
        append("import " + className + ";", 0, buff);
        Class<?> builderClass = container.getClassExtractor().getBuilderClass(type);
        if (builderClass!=null && !builderClass.equals(type.getTypeClass()))
            append("import " + builderClass.getName()+";", 0, buff);
        append("", 0, buff);
        String serializerName = type.getTypeClass().getSimpleName();
        if(serializerName.indexOf("$")!=-1){
            serializerName = serializerName.substring(serializerName.indexOf("$")+1);
        }
        append("public class " + serializerName + "Serializer implements ISerializer{", 0, buff);
        append("@Override", 4, buff);
        append("public void encode(Object value, byte[] byteArray, int location) {",4, buff);
        append("}", 4, buff);
        append("", 0, buff);

        append("@Override", 4, buff);
        append("public void encode(Object value, EncodeDataContainer ba) {", 4, buff);
        append(type.getTypeClass().getSimpleName() + " element = ("+ type.getTypeClass().getSimpleName() + ") value;", 8, buff);
        for (AttributeDescriptor p : type.attributes) {
            if (p.getReturnType().equals(short.class) || p.getReturnType().equals(Short.class)) {
                append("ba.getEncoder().encodeShort(element." + p.getMethodName()
                        + "(), ba);", 8, buff);
            } else
            if (p.getReturnType().equals(boolean.class) || p.getReturnType().equals(Boolean.class)) {
                append("ba.getEncoder().encodeBoolean(element." + p.getMethodName()
                        + "(), ba);", 8, buff);
            } else
            if (!p.isCollection() && (p.getReturnType().equals(byte.class)
                    || p.getReturnType().equals(Byte.class))) {
                append("ba.getEncoder().encodeByte(element." + p.getMethodName()
                        + "(), ba);", 8, buff);
            }else
            if (p.isCollection() && (p.getReturnType().equals(byte.class)
                    || p.getReturnType().equals(Byte.class))) {
                append("ba.getEncoder().encodeByteArray(element." + p.getMethodName()
                        + "(), ba);", 8, buff);
            }else
            if (p.getReturnType().equals(BigDecimal.class)) {
                append("ba.getEncoder().encodeBigDecimal(element." + p.getMethodName()
                        + "(), ba);", 8, buff);
            }else
            if (p.getReturnType().equals(String.class)) {
                append("ba.getEncoder().encodeString(element." + p.getMethodName()
                        + "(), ba);", 8, buff);
            } else if (p.getReturnType().equals(int.class)
                    || p.getReturnType().equals(Integer.class)) {
                append("ba.getEncoder().encodeInt32(element." + p.getMethodName()
                        + "(), ba);", 8, buff);
            } else if (p.getReturnType().equals(long.class)
                    || p.getReturnType().equals(Long.class)) {
                append("ba.getEncoder().encodeInt64(element." + p.getMethodName()
                        + "(), ba);", 8, buff);
            } else if (container.isTypeAttribute(p)) {
                append("ba.getEncoder().encodeObject(element." + p.getMethodName()
                        + "(), ba, " + p.getReturnType().getName() + ".class);",
                        8, buff);
            }
        }

        if(container.supportAugmentations(type)){
            append("ba.getEncoder().encodeAugmentations(value, ba);", 8, buff);
        }

        if (container.isChildAttribute(type)) {
            for (AttributeDescriptor child : type.getChildren().keySet()) {
                TypeDescriptor subTable = container.getTypeDescriptorByClass(child.getReturnType());
                if(child.isCollection()){
                    append("ba.getEncoder().encodeAndAddList(element."+child.getMethodName()+"(), ba,"+subTable.getTypeClassName()+".class);",8,buff);
                }else{
                    append("ba.getEncoder().encodeAndAddObject(element."+child.getMethodName()+"(), ba,"+subTable.getTypeClassName()+".class);",8,buff);
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
                    append("builder.set" + p.getColumnName()+ "(ba.getEncoder().decodeShort(ba));", 8, buff);
                }else
                if (p.getReturnType().equals(boolean.class) || p.getReturnType().equals(Boolean.class)) {
                    append("builder.set" + p.getColumnName()
                            + "(ba.getEncoder().decodeBoolean(ba));", 8, buff);
                }else
                if (!p.isCollection() && (p.getReturnType().equals(byte.class)
                    || p.getReturnType().equals(Byte.class))) {
                    append("builder.set" + p.getColumnName()
                        + "(ba.getEncoder().decodeByte(ba));", 8, buff);
                }else
                if (p.isCollection() && (p.getReturnType().equals(byte.class)
                            || p.getReturnType().equals(Byte.class))) {
                    append("builder.set" + p.getColumnName()
                                + "(ba.getEncoder().decodeByteArray(ba));", 8, buff);
                }else
                if (p.getReturnType().equals(BigDecimal.class)) {
                    append("builder.set" + p.getColumnName()
                            + "(ba.getEncoder().decodeBigDecimal(ba));", 8, buff);
                }else
                if (p.getReturnType().equals(String.class)) {
                    append("builder.set" + p.getColumnName()
                            + "(ba.getEncoder().decodeString(ba));", 8, buff);
                } else if (p.getReturnType().equals(int.class)
                        || p.getReturnType().equals(Integer.class)) {
                    append("builder.set" + p.getColumnName()
                            + "(ba.getEncoder().decodeInt32(ba));", 8, buff);
                } else if (p.getReturnType().equals(long.class)
                        || p.getReturnType().equals(Long.class)) {
                    append("builder.set" + p.getColumnName()
                            + "(ba.getEncoder().decodeInt64(ba));", 8, buff);
                } else if (container.isTypeAttribute(p)) {
                    append("builder.set" + p.getColumnName() + "(("
                            + p.getReturnType().getName()
                            + ")ba.getEncoder().decodeObject(ba));", 8, buff);
                }
            }

            if(container.supportAugmentations(type)){
                append("ba.getEncoder().decodeAugmentations(builder, ba,"+ type.getTypeClass().getSimpleName() + ".class);", 8,buff);
            }

            if (container.isChildAttribute(type)) {
                for (AttributeDescriptor child : type.getChildren().keySet()) {
                    TypeDescriptor subTable = container.getTypeDescriptorByClass(child.getReturnType());
                    if(child.isCollection()){
                        append("builder.set"+child.getColumnName()+"(ba.getEncoder().decodeAndList(ba,"+subTable.getTypeClassName()+".class));",8,buff);
                    }else{
                        append("builder.set"+child.getColumnName()+"(("+subTable.getTypeClassName()+")ba.getEncoder().decodeAndObject(ba));",8,buff);
                    }
                }
            }
            if(container.getClassExtractor().getBuilderMethod(type)!=null){
                append("return builder."+container.getClassExtractor().getBuilderMethod(type)+";", 8, buff);
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
                    append("ba.getEncoder().decodeBoolean(ba)", 8, buff);
                }else
                if (p.getReturnType().equals(String.class)) {
                    if(!first)
                        append(",",12,buff);
                    first = false;
                    append("ba.getEncoder().decodeString(ba)", 8, buff);
                } else if (p.getReturnType().equals(int.class) || p.getReturnType().equals(Integer.class)) {
                    if (type.getTypeClass().isEnum()) {
                        append(type.getTypeClass().getSimpleName()
                                + " instance = "
                                + type.getTypeClass().getSimpleName()
                                + ".forValue(ba.getEncoder().decodeInt32(ba));",
                                8, buff);
                    } else{
                        if(!first)
                            append(",",12,buff);
                        first = false;
                        append("ba.getEncoder().decodeInt32(ba)", 8, buff);
                    }
                } else if (p.getReturnType().equals(long.class) || p.getReturnType().equals(Long.class)) {
                    if(!first)
                        append(",",12,buff);
                    first = false;
                    append("ba.getEncoder().decodeInt64(ba)", 8, buff);
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
        ba.getEncoder().encodeInt16(classCode, ba);
        ba.getEncoder().encodeString(typeClassName, ba);
        ba.getEncoder().encodeString(typeSerializerClassName, ba);
        ba.getEncoder().encodeInt16(hierarchyLevel, ba);
        if(this.keyColumn!=null){
            ba.getEncoder().encodeString(this.keyColumn.getClassName(), ba);
            ba.getEncoder().encodeString(this.keyColumn.getMethodName(), ba);
        }else{
            ba.getEncoder().encodeString(null, ba);
            ba.getEncoder().encodeString(null, ba);
        }
        ba.getEncoder().encodeInt16(attributes.length, ba);
        for (AttributeDescriptor p : attributes) {
            p.encode(ba);
        }
        ba.getEncoder().encodeInt16(children.size(), ba);
        for (AttributeDescriptor p : children.keySet()) {
            p.encode(ba);
        }
        ba.getEncoder().encodeInt16(parents.size(), ba);
        for (AttributeDescriptor p : parents.keySet()) {
            p.encode(ba);
        }
        ba.getEncoder().encodeInt16(knownAugmentingClassNames.size(), ba);
        for (String augClass : knownAugmentingClassNames.keySet()) {
            ba.getEncoder().encodeString(augClass, ba);
        }
    }

    public static TypeDescriptor decode(EncodeDataContainer ba) {

        TypeDescriptor t = new TypeDescriptor();
        t.container = ba.getTypeDescriptorContainer();
        t.classCode = ba.getEncoder().decodeInt16(ba);
        t.typeClassName = ba.getEncoder().decodeString(ba);
        t.typeSerializerClassName = ba.getEncoder().decodeString(ba);
        t.hierarchyLevel = ba.getEncoder().decodeInt16(ba);
        String str1 = ba.getEncoder().decodeString(ba);
        String str2 = ba.getEncoder().decodeString(ba);
        if(str1!=null){
            t.keyColumn = new AttributeDescriptor(str2,str1);
        }
        t.attributes = new AttributeDescriptor[ba.getEncoder().decodeInt16(ba)];
        for (int i = 0; i < t.attributes.length; i++) {
            t.attributes[i] = AttributeDescriptor.decode(ba);
        }

        int size = ba.getEncoder().decodeInt16(ba);
        for (int i = 0; i < size; i++) {
            AttributeDescriptor p = AttributeDescriptor.decode(ba);
            t.children.put(p, p);
        }

        size = ba.getEncoder().decodeInt16(ba);
        for (int i = 0; i < size; i++) {
            AttributeDescriptor p = AttributeDescriptor.decode(ba);
            t.parents.put(p, p);
        }

        size = ba.getEncoder().decodeInt16(ba);
        for (int i = 0; i < size; i++) {
            String augClass = ba.getEncoder().decodeString(ba);
            t.knownAugmentingClassNames.put(augClass,augClass);
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
}
