package org.opendaylight.persisted.codec;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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

import org.opendaylight.persisted.MDSALDatabase;
import org.opendaylight.yangtools.yang.binding.DataObject;

public class MDSALTable {

    private int classCode = -1;
    private int hierarchyLevel = -1;
    private String className = null;
    private String serializerClassName = null;
    private MDSALColumn columns[] = new MDSALColumn[0];
    private Map<MDSALColumn, MDSALColumn> children = new HashMap<MDSALColumn, MDSALColumn>();
    private Map<MDSALColumn, MDSALColumn> parents = new HashMap<MDSALColumn, MDSALColumn>();
    private Map<String, MDSALColumn> columnNameToProperty = new HashMap<String, MDSALColumn>();
    private Map<String, MDSALColumn> methodNameToProperty = new HashMap<String, MDSALColumn>();
    private Map<Integer, MDSALColumn> locationToProperty = new HashMap<Integer, MDSALColumn>();
    private Map<String, Integer> columnNameToLocation = new HashMap<String, Integer>();
    private Map<String, Integer> methodNameToLocation = new HashMap<String, Integer>();
    private Set<String> knownAugmentingClassNames = new HashSet<String>();
    private Set<Class<?>> knownAugmentingClasses = new HashSet<Class<?>>();
    private Class<?> myClass = null;
    private ISerializer serializer = null;
    private Field augmentationField = null;
    private boolean augmentationFieldInitialized = false;
    private Field augmentationFieldBuilder = null;
    private boolean augmentationFieldBuilderInitialized = false;

    public static Map<String, Field> refFieldsCache = new HashMap<String, Field>();

    private MDSALTable() {
    }

    public MDSALTable(String _tablename, String _origTableName) {
        this.className = _tablename;
        this.serializerClassName = _origTableName;
    }

    public MDSALColumn addColumn(String _logicalFieldName,
            String _logicalTableName, String _origFieldName,
            String _origTableName) {
        return null;
    }

    public MDSALTable(Class<?> cls, ISerializer _serializer, int _hierarchyLevel) {
        this.className = cls.getName();
        this.serializer = _serializer;
        this.hierarchyLevel = _hierarchyLevel;
        if (this.serializer != null) {
            this.serializerClassName = serializer.getClass().getName();
        } else {
            try {
                String serializerClassName = cls.getName() + "Serializer";
                this.serializer = (ISerializer) Class.forName(
                        serializerClassName).newInstance();
                this.serializerClassName = serializer.getClass().getName();
            } catch (Exception err) {
            }
        }
        this.classCode = MDSALTableRepository.getInstance().newCode();
        this.myClass = cls;
        MDSALTableRepository.getInstance().registerClass(this);

        List<MDSALColumn> pList = new ArrayList<MDSALColumn>();

        Method mth[] = cls.getMethods();
        for (Method m : mth) {
            if (m.getName().equals("getImplementedInterface"))
                continue;
            if (m.getName().startsWith("get") || m.getName().startsWith("is")) {
                if (m.getParameterTypes().length > 0)
                    continue;
                MDSALColumn p = new MDSALColumn(m, cls);
                if (DataObject.class.isAssignableFrom(p.getReturnType())) {
                    children.put(p, p);
                    System.out.println("Child=" + p.getReturnType());
                    MDSALTable child = MDSALTableRepository.getInstance()
                            .getCtypeByClass(p.getReturnType());
                    if (child == null) {
                        child = new MDSALTable((Class<?>) p.getReturnType(),
                                null, this.hierarchyLevel + 1);
                        MDSALTableRepository.getInstance().registerClass(child);
                    }
                    child.parents.put(p, p);
                } else {
                    if (p.getReturnType().getName().indexOf(".rev") != -1) {
                        MDSALTable typedef = MDSALTableRepository.getInstance()
                                .getCtypeByClass(p.getReturnType());
                        if (typedef == null) {
                            typedef = new MDSALTable(
                                    (Class<?>) p.getReturnType(), null,
                                    this.hierarchyLevel + 1);
                            MDSALTableRepository.getInstance().registerClass(
                                    typedef);
                        }
                    }
                    pList.add(p);
                }
            }
        }
        this.columns = pList.toArray(new MDSALColumn[pList.size()]);
        if (this.serializer == null) {
            this.serializer = createSerializer(this);
            try {
                this.serializerClassName = this.serializer.getClass().getName();
            } catch (Exception err) {
            }
        }
        initMaps();
        MDSALTableRepository.getInstance().save();
    }

    public Map<MDSALColumn, MDSALColumn> getChildren() {
        return this.children;
    }

    public static Class<?> getElementClass(Object element) {
        if (element instanceof DataObject)
            ((DataObject) element).getImplementedInterface();
        return element.getClass();
    }

    public Field getAugmentationField(Object value) {
        if (!this.augmentationFieldInitialized) {
            this.augmentationFieldInitialized = true;
            try {
                this.setAugmentationField(MDSALTable.findField(
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
        }
    }

    public Set<Class<?>> getKnownAugmentingClasses() {
        if (this.knownAugmentingClasses.isEmpty()
                && !this.knownAugmentingClassNames.isEmpty()) {
            try {
                for (String augClsName : this.knownAugmentingClassNames) {
                    this.knownAugmentingClasses.add(this.getClass()
                            .getClassLoader().loadClass(augClsName));
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
        getMyClass();
        initMethods();
        initMaps();
    }

    private void initMaps() {
        for (int i = 0; i < this.columns.length; i++) {
            columnNameToProperty.put(this.columns[i].getColumnName(),
                    this.columns[i]);
            methodNameToProperty.put(this.columns[i].getMethodName(),
                    this.columns[i]);
            locationToProperty.put(i, this.columns[i]);
            columnNameToLocation.put(this.columns[i].getColumnName(), i);
            methodNameToLocation.put(this.columns[i].getMethodName(), i);
        }
    }

    public MDSALColumn[] getColumns() {
        return this.columns;
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

    public int getLocationByColumnName(String name) {
        Integer result = columnNameToLocation.get(name);
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

    public String getColumnNameByLocation(int i) {
        return locationToProperty.get(i).getColumnName();
    }

    public String getMetodNameByLocation(int i) {
        return locationToProperty.get(i).getMethodName();
    }

    private void initMethods() {
        if (this.myClass == null) {
            try {
                this.myClass = this.getClass().getClassLoader()
                        .loadClass(this.className);
            } catch (Exception err) {
                err.printStackTrace();
            }
        }
        for (MDSALColumn p : this.columns) {
            p.initMethod();
        }
    }

    public Object valueOf(String name, String stringVal) {
        MDSALColumn p = columnNameToProperty.get(name);
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

    public ISerializer getSerializer() {
        if (this.serializer == null) {
            try {
                this.serializer = (ISerializer) getClass().getClassLoader()
                        .loadClass(this.serializerClassName).newInstance();
            } catch (Exception err) {
            }
            try {
                String directory = "serializers/"
                        + myClass.getPackage().getName();
                directory = replaceAll(directory, ".", "/");
                File dir = new File(directory);
                URLClassLoader cl = new URLClassLoader(
                        new URL[] { dir.toURL() }, getClass().getClassLoader());
                this.serializer = (ISerializer) cl.loadClass(
                        this.serializerClassName).newInstance();
            } catch (Exception err) {
                initMethods();
                this.serializer = createSerializer(this);
                this.serializerClassName = this.serializer.getClass().getName();
            }
        }
        return this.serializer;
    }

    public MDSALColumn getPropertyByColumnName(String name) {
        MDSALColumn cprop = columnNameToProperty.get(name);
        if (cprop != null && !cprop.isMethodInitialized()) {
            initMethods();
        }
        return cprop;
    }

    public MDSALColumn getPropertyByMethodName(String name) {
        MDSALColumn cprop = methodNameToProperty.get(name);
        if (!cprop.isMethodInitialized()) {
            initMethods();
        }
        return cprop;
    }

    public Object[] newData() {
        return new Object[this.columns.length];
    }

    public Class<?> getMyClass() {
        if (this.myClass == null) {
            try {
                this.myClass = getClass().getClassLoader().loadClass(className);
            } catch (Exception err) {
                err.printStackTrace();
            }
        }
        return this.myClass;
    }

    public String getMyClassName() {
        return this.className;
    }

    public String getMyShortClassName() {
        int index = this.className.lastIndexOf(".");
        return this.className.substring(index + 1);
    }

    public static ISerializer createSerializer(MDSALTable type) {
        String data = MDSALTable.generateSerializerClass(type);
        String directory = "/src/main/yang-gen-sal/"
                + type.getMyClass().getPackage().getName();
        directory = "." + replaceAll(directory, ".", "/");

        File file = new File(directory);
        if (!file.exists()) {
            file.mkdirs();
        }
        file = new File(file.getPath() + "/"
                + type.getMyClass().getSimpleName() + "Serializer.java");
        try {
            FileOutputStream out = new FileOutputStream(file);
            out.write(data.getBytes());
            out.close();
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            compiler.run(null, null, null, file.getPath());
            URLClassLoader cl = new URLClassLoader(new URL[] { new File(
                    "./src/main/yang-gen-sal/").toURL() },
                    MDSALDatabase.class.getClassLoader());
            Class<?> serializerClass = cl.loadClass(type.getMyClass().getName()
                    + "Serializer");
            ISerializer serializer = (ISerializer) serializerClass
                    .newInstance();
            return serializer;
        } catch (Exception err) {
            err.printStackTrace();
        } finally {
            file = new File(directory + "/" + type.getMyClass().getSimpleName()
                    + "Serializer.class");
            if (file.exists())
                file.delete();
        }
        return null;
    }

    public static String generateSerializerClass(MDSALTable type) {
        StringBuffer buff = new StringBuffer();

        append("", 0, buff);
        append("package " + type.getMyClass().getPackage().getName() + ";", 0,
                buff);
        append("import org.opendaylight.persisted.codec.BytesArray;", 0, buff);
        append("import org.opendaylight.persisted.codec.MDSALEncoder;", 0, buff);
        append("import org.opendaylight.persisted.codec.ISerializer;", 0, buff);
        append("import org.opendaylight.persisted.codec.MDSALTable;", 0, buff);
        append("import org.opendaylight.persisted.codec.MDSALTableRepository;",
                0, buff);
        append("import java.util.ArrayList;", 0, buff);
        append("import java.util.List;", 0, buff);

        append("import " + type.getMyClassName() + ";", 0, buff);
        if (type.getMyClass().isInterface())
            append("import " + type.getMyClassName() + "Builder;", 0, buff);
        append("", 0, buff);
        append("public class " + type.getMyClass().getSimpleName()
                + "Serializer implements ISerializer{", 0, buff);
        append("@Override", 4, buff);
        append("public void encode(Object value, byte[] byteArray, int location) {",
                4, buff);
        append("}", 4, buff);
        append("", 0, buff);

        append("@Override", 4, buff);
        append("public void encode(Object value, BytesArray ba) {", 4, buff);
        append(type.getMyClass().getSimpleName() + " element = ("
                + type.getMyClass().getSimpleName() + ") value;", 8, buff);
        for (MDSALColumn p : type.columns) {
            if (p.getReturnType().equals(String.class)) {
                append("MDSALEncoder.encodeString(element." + p.getMethodName()
                        + "(), ba);", 8, buff);
            } else if (p.getReturnType().equals(int.class)
                    || p.getReturnType().equals(Integer.class)) {
                append("MDSALEncoder.encodeInt32(element." + p.getMethodName()
                        + "(), ba);", 8, buff);
            } else if (p.getReturnType().equals(long.class)
                    || p.getReturnType().equals(Long.class)) {
                append("MDSALEncoder.encodeInt64(element." + p.getMethodName()
                        + "(), ba);", 8, buff);
            } else if (p.getReturnType().getPackage().getName().indexOf(".rev") != -1) {
                append("MDSALEncoder.encodeObject(element." + p.getMethodName()
                        + "(), ba, " + p.getReturnType().getName() + ".class);",
                        8, buff);
            }
        }

        if (DataObject.class.isAssignableFrom(type.getMyClass())) {
            append("MDSALEncoder.encodeAugmentations(value, ba);", 8, buff);
            for (MDSALColumn child : type.getChildren().keySet()) {
                MDSALTable subTable = MDSALTableRepository.getInstance()
                        .getCtypeByClass(child.getReturnType());
                append("if(element." + child.getMethodName() + "()!=null){", 8,
                        buff);
                append("for(" + subTable.getMyClassName()
                        + " subElement:element." + child.getMethodName()
                        + "()){", 12, buff);
                append("MDSALTable subTable = MDSALTableRepository.getInstance().getCtypeByClass("
                        + subTable.getMyClassName() + ".class);", 16, buff);
                append("BytesArray subBA = new BytesArray(1024);", 16, buff);
                append("subTable.getSerializer().encode(subElement, subBA);",
                        16, buff);
                append("ba.addSubElementData(subTable.getClassCode(),subBA);",
                        16, buff);
                append("}", 12, buff);
                append("}", 8, buff);

            }
        }

        append("}", 4, buff);
        append("@Override", 4, buff);
        append("public Object decode(byte[] byteArray, int location, int length) {",
                4, buff);
        append("return null;", 8, buff);
        append("}", 4, buff);
        append("@Override", 4, buff);
        append("public Object decode(BytesArray ba, int length) {", 4, buff);
        if (type.getMyClass().isInterface()) {
            append(type.getMyClass().getSimpleName() + "Builder builder = new "
                    + type.getMyClass().getSimpleName() + "Builder();", 8, buff);
            for (MDSALColumn p : type.columns) {
                if (p.getReturnType().equals(String.class)) {
                    append("builder.set" + p.getColumnName()
                            + "(MDSALEncoder.decodeString(ba));", 8, buff);
                } else if (p.getReturnType().equals(int.class)
                        || p.getReturnType().equals(Integer.class)) {
                    append("builder.set" + p.getColumnName()
                            + "(MDSALEncoder.decodeInt32(ba));", 8, buff);
                } else if (p.getReturnType().equals(long.class)
                        || p.getReturnType().equals(Long.class)) {
                    append("builder.set" + p.getColumnName()
                            + "(MDSALEncoder.decodeInt64(ba));", 8, buff);
                } else if (p.getReturnType().getName().indexOf(".rev") != -1) {
                    append("builder.set" + p.getColumnName() + "(("
                            + p.getReturnType().getName()
                            + ")MDSALEncoder.decodeObject(ba));", 8, buff);
                }
            }
            if (DataObject.class.isAssignableFrom(type.getMyClass())) {
                append("MDSALEncoder.decodeAugmentations(builder, ba,"
                        + type.getMyClass().getSimpleName() + ".class);", 8,
                        buff);
                for (MDSALColumn child : type.getChildren().keySet()) {
                    MDSALTable subTable = MDSALTableRepository.getInstance()
                            .getCtypeByClass(child.getReturnType());
                    String shortName = subTable.getMyShortClassName()
                            .toLowerCase();
                    append("MDSALTable "
                            + shortName
                            + "Table = MDSALTableRepository.getInstance().getCtypeByClass("
                            + subTable.getMyClassName() + ".class);", 8, buff);
                    append("List<BytesArray> " + shortName
                            + "List = ba.getSubElementsData().get(" + shortName
                            + "Table.getClassCode());", 8, buff);
                    append("ISerializer " + shortName + "Serializer = "
                            + shortName + "Table.getSerializer();", 8, buff);
                    append("if(" + shortName + "List!=null){", 8, buff);
                    append("List<" + subTable.getMyClassName()
                            + "> list = new ArrayList<"
                            + subTable.getMyClassName() + ">();", 12, buff);
                    append("for(BytesArray subBA:" + shortName + "List){", 12,
                            buff);
                    append("list.add((" + subTable.getMyClassName() + ")"
                            + shortName + "Serializer.decode(subBA, 0));", 16,
                            buff);
                    append("}", 12, buff);
                    append("builder.set" + child.getColumnName() + "(list);",
                            12, buff);
                    append("}", 8, buff);
                }
            }
            append("return builder.build();", 8, buff);
        } else {
            for (MDSALColumn p : type.columns) {
                if (p.getReturnType().equals(String.class)) {
                    append(type.getMyClass().getSimpleName()
                            + " instance = new "
                            + type.getMyClass().getSimpleName()
                            + "(MDSALEncoder.decodeString(ba));", 8, buff);
                } else if (p.getReturnType().equals(int.class)
                        || p.getReturnType().equals(Integer.class)) {
                    if (type.getMyClass().isEnum()) {
                        append(type.getMyClass().getSimpleName()
                                + " instance = "
                                + type.getMyClass().getSimpleName()
                                + ".forValue(MDSALEncoder.decodeInt32(ba));",
                                8, buff);
                    } else
                        append(type.getMyClass().getSimpleName()
                                + " instance = new "
                                + type.getMyClass().getSimpleName()
                                + "(MDSALEncoder.decodeInt32(ba));", 8, buff);
                } else if (p.getReturnType().equals(long.class)
                        || p.getReturnType().equals(Long.class)) {
                    append(type.getMyClass().getSimpleName()
                            + " instance = new "
                            + type.getMyClass().getSimpleName()
                            + "(MDSALEncoder.decodeInt64(ba));", 8, buff);
                }
            }
            append("return instance;", 8, buff);
        }

        append("}", 4, buff);

        append("public String getBlockKey(Object obj) {", 4, buff);
        append("return null;", 8, buff);
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

    public void encode(BytesArray ba) {
        MDSALEncoder.encodeInt16(classCode, ba);
        MDSALEncoder.encodeString(className, ba);
        MDSALEncoder.encodeString(serializerClassName, ba);
        MDSALEncoder.encodeInt16(hierarchyLevel, ba);
        MDSALEncoder.encodeInt16(columns.length, ba);
        for (MDSALColumn p : columns) {
            p.encode(ba);
        }
        MDSALEncoder.encodeInt16(children.size(), ba);
        for (MDSALColumn p : children.keySet()) {
            p.encode(ba);
        }
        MDSALEncoder.encodeInt16(parents.size(), ba);
        for (MDSALColumn p : parents.keySet()) {
            p.encode(ba);
        }
        MDSALEncoder.encodeInt16(knownAugmentingClassNames.size(), ba);
        for (String augClass : knownAugmentingClassNames) {
            MDSALEncoder.encodeString(augClass, ba);
        }
    }

    public static MDSALTable decode(BytesArray ba) {

        MDSALTable t = new MDSALTable();

        t.classCode = MDSALEncoder.decodeInt16(ba);
        t.className = MDSALEncoder.decodeString(ba);
        t.serializerClassName = MDSALEncoder.decodeString(ba);
        t.hierarchyLevel = MDSALEncoder.decodeInt16(ba);
        t.columns = new MDSALColumn[MDSALEncoder.decodeInt16(ba)];
        for (int i = 0; i < t.columns.length; i++) {
            t.columns[i] = MDSALColumn.decode(ba);
        }

        int size = MDSALEncoder.decodeInt16(ba);
        for (int i = 0; i < size; i++) {
            MDSALColumn p = MDSALColumn.decode(ba);
            t.children.put(p, p);
        }

        size = MDSALEncoder.decodeInt16(ba);
        for (int i = 0; i < size; i++) {
            MDSALColumn p = MDSALColumn.decode(ba);
            t.parents.put(p, p);
        }

        size = MDSALEncoder.decodeInt16(ba);
        for (int i = 0; i < size; i++) {
            String augClass = MDSALEncoder.decodeString(ba);
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

}
