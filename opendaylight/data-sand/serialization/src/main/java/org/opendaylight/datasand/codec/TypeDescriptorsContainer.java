package org.opendaylight.datasand.codec;

import java.io.File;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.opendaylight.datasand.codec.bytearray.ByteArrayEncodeDataContainer;
import org.opendaylight.datasand.codec.observers.DefaultMethodFilterObserver;
import org.opendaylight.datasand.codec.observers.DefaultPOJOClassExtractor;
import org.opendaylight.datasand.codec.observers.DefaultPOJOTypeAttribute;
import org.opendaylight.datasand.codec.observers.IAugmetationObserver;
import org.opendaylight.datasand.codec.observers.IChildAttributeObserver;
import org.opendaylight.datasand.codec.observers.IClassExtractorObserver;
import org.opendaylight.datasand.codec.observers.IMethodFilterObserver;
import org.opendaylight.datasand.codec.observers.ITypeAttributeObserver;

public class TypeDescriptorsContainer {

    private Map<String, TypeDescriptor> types = new ConcurrentHashMap<String, TypeDescriptor>();
    private Map<String, TypeDescriptor> shortNameToType = new ConcurrentHashMap<String, TypeDescriptor>();
    private String repositoryFileName = null;

    private Map<Integer, TypeDescriptor> codeToTypeDescriptor = new ConcurrentHashMap<Integer, TypeDescriptor>();
    private Map<Class<?>, TypeDescriptor> classToTypeDescriptor = new ConcurrentHashMap<Class<?>, TypeDescriptor>();
    private int maxCodeInRepository = 1000;
    private static final ThreadLocal<RandomAccessFile> locked = new ThreadLocal<RandomAccessFile>();

    private List<IChildAttributeObserver> modelChildIdentifierObservers = new ArrayList<IChildAttributeObserver>();
    private List<ITypeAttributeObserver> modelTypeIdentifierObservers = new ArrayList<ITypeAttributeObserver>();
    private List<IMethodFilterObserver> methodFilterObservers = new ArrayList<IMethodFilterObserver>();
    private IClassExtractorObserver classExtractor = new DefaultPOJOClassExtractor();
    private IAugmetationObserver augmentationObserver = null;
    private Set<Class<?>> pendingCreation = new HashSet<Class<?>>();

    public TypeDescriptorsContainer(String path) {
        File f = new File(path);
        if(!f.exists())
            f.mkdirs();
        repositoryFileName = path+"/type-descriptors-container.data";
        addMethodFilterObserver(new DefaultMethodFilterObserver());
        addTypeAttributeObserver(new DefaultPOJOTypeAttribute());
        load();
    }

    public void setClusterMap(Map<String,TypeDescriptor> clusterMap){
        for(Map.Entry<String, TypeDescriptor> entry:types.entrySet()){
            clusterMap.put(entry.getKey(), entry.getValue());
        }
        this.types = clusterMap;
    }

    public void lock() {
        if (locked.get() != null) {
            return;
        }
        while (locked.get() == null) {
            try {
                locked.set(new RandomAccessFile(repositoryFileName, "rw"));
            } catch (Exception err) {
                err.printStackTrace();
            }
            if (locked.get() == null)
                try {
                    Thread.sleep(500);
                } catch (Exception err) {
                }
        }
    }

    public void unlock() {
        try {
            locked.get().close();
            locked.set(null);
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    public void save() {
        lock();
        try {
            byte data[] = getMDSALTableRepositoryData();
            locked.get().seek(0);
            locked.get().write(data);
        } catch (Exception err) {
            err.printStackTrace();
        } finally {
            unlock();
        }
    }

    public byte[] getMDSALTableRepositoryData() {
        ByteArrayEncodeDataContainer ba = new ByteArrayEncodeDataContainer(1024,this);
        ba.getEncoder().encodeInt16(types.size(), ba);
        for (TypeDescriptor type : types.values()) {
            type.encode(ba);
        }
        return ba.getData();
    }

    public void load() {
        lock();
        try {
            File f = new File(repositoryFileName);
            if (f.exists() && f.length() > 0) {
                try {
                    byte[] data = new byte[(int) f.length()];
                    locked.get().seek(0);
                    locked.get().read(data);
                    ByteArrayEncodeDataContainer ba = new ByteArrayEncodeDataContainer(data,this);
                    int size = ba.getEncoder().decodeInt16(ba);
                    List<TypeDescriptor> augmented = new LinkedList<TypeDescriptor>();
                    for (int i = 0; i < size; i++) {
                        TypeDescriptor type = TypeDescriptor.decode(ba);
                        types.put(type.getTypeClassName(), type);
                        if (type.getClassCode() >= maxCodeInRepository)
                            maxCodeInRepository = type.getClassCode() + 1;
                        int index = type.getTypeClassName().lastIndexOf(".");
                        shortNameToType.put(
                                type.getTypeClassName().substring(index + 1),
                                type);
                        if (type.getKnownAugmentingClasses().size() > 0)
                            augmented.add(type);
                    }
                    for (TypeDescriptor td : augmented) {
                        Class<?> augClass = td.getKnownAugmentingClasses().keySet().iterator().next();
                        TypeDescriptor aTD = getTypeDescriptorByClass(augClass);
                        for (AttributeDescriptor ad : aTD.getAttributes()) {
                            ad.setAugmentedTableName(td.getTypeClassShortName());
                        }
                    }
                } catch (Exception err) {
                    err.printStackTrace();
                }
            }
        } finally {
            unlock();
        }
    }

    private void registerClass(TypeDescriptor type) {
        if(!types.containsKey(type.getTypeClassName())){
            types.put(type.getTypeClassName(), type);
        }
        int index = type.getTypeClassName().lastIndexOf(".");
        shortNameToType.put(type.getTypeClassName().substring(index + 1), type);
        classToTypeDescriptor.put(type.getTypeClass(), type);
        codeToTypeDescriptor.put(type.getClassCode(), type);
    }

    public synchronized int newCode() {
        maxCodeInRepository++;
        return maxCodeInRepository;
    }

    public TypeDescriptor getTypeDescriptorByCode(int code) {
        TypeDescriptor type = codeToTypeDescriptor.get(code);
        if (type != null)
            return type;
        if (type == null) {
            for (TypeDescriptor t : types.values()) {
                codeToTypeDescriptor.put(t.getClassCode(), t);
            }
        }
        type = codeToTypeDescriptor.get(code);
        return type;
    }

    public boolean hasTypeDescriptor(Object object) {
        Class<?> cls = this.getElementClass(object);
        return this.types.get(cls.getName()) != null;
    }

    public TypeDescriptor checkTypeDescriptorByObject(Object object) {
        if (object == null)
            return null;
        return checkTypeDescriptorByClass(this.getElementClass(object));
    }

    public TypeDescriptor getTypeDescriptorByObject(Object object) {
        if (object == null)
            return null;
        return getTypeDescriptorByClass(this.getElementClass(object));
    }

    public TypeDescriptor getTypeDescriptorByClass(Class<?> cls) {
        return getTypeDescriptorByClass(cls, 0);
    }

    public TypeDescriptor checkTypeDescriptorByClass(Class<?> cls) {
        return checkTypeDescriptorByClass(cls, 0);
    }

    public TypeDescriptor checkTypeDescriptorByClass(Class<?> cls, int hierarchy) {
        TypeDescriptor type = classToTypeDescriptor.get(cls);
        if(type==null){
            type = types.get(cls.getName());
            if(type!=null){
                type.init();
                registerClass(type);
            }
        }
        return type;
    }

    public TypeDescriptor getTypeDescriptorByClass(Class<?> cls, int hierarchy) {
        TypeDescriptor type = classToTypeDescriptor.get(cls);
        if (type == null) {
            synchronized (this) {
                type = classToTypeDescriptor.get(cls);
                if (type == null) {
                    type = newTypeDescriptor(cls, hierarchy);
                }
            }
        }
        return type;
    }

    protected synchronized TypeDescriptor newTypeDescriptor(Class<?> cls,int hierarchy) {
        if(this.pendingCreation.contains(cls))
            return null;
        pendingCreation.add(cls);

        TypeDescriptor td = classToTypeDescriptor.get(cls);
        if (td == null) {
            td = types.get(cls.getName());
            if (td != null) {
                td.init();
                registerClass(td);
            }
        }
        if (td != null) {
            return td;
        } else {
            td = new TypeDescriptor(cls, null, hierarchy,this);
            registerClass(td);
            save();
            return td;
        }
    }

    public TypeDescriptor getTypeDescriptorByClassName(String clsName) {
        return types.get(clsName);
    }

    public TypeDescriptor getTypeDescriptorByShortClassName(String clsName) {
        return shortNameToType.get(clsName);
    }

    public void deleteRepository() {
        for (TypeDescriptor table : this.types.values()) {
            String serName = table.getSerializerClassName();
            if (serName != null) {
                serName = "./src/main/yang-gen-sal/"
                        + TypeDescriptor.replaceAll(serName, ".", "/")
                        + ".java";
                File f = new File(serName);
                if (f.exists())
                    f.delete();
            }
        }
        File f = new File(repositoryFileName);
        f.delete();
    }

    public void setAugmentationObserver(IAugmetationObserver _ao){
        this.augmentationObserver = _ao;
    }

    public IAugmetationObserver getAugmentationObserver(){
        return this.augmentationObserver;
    }

    public void setClassExtractor(IClassExtractorObserver ce){
        this.classExtractor = ce;
    }

    public void addChildAttributeObserver(IChildAttributeObserver rule){
        this.modelChildIdentifierObservers.add(rule);
    }

    public void addMethodFilterObserver(IMethodFilterObserver rule){
        this.methodFilterObservers.add(rule);
    }

    public void addTypeAttributeObserver(ITypeAttributeObserver rule){
        this.modelTypeIdentifierObservers.add(rule);
    }

    public boolean isChildAttribute(AttributeDescriptor ad){
        for(IChildAttributeObserver rule:this.modelChildIdentifierObservers){
            if(rule.isChildAttribute(ad))
                return true;
        }
        return false;
    }

    public boolean isValidModelMethod(Method m){
        for(IMethodFilterObserver rule:this.methodFilterObservers){
            if(!rule.isValidModelMethod(m))
                return false;
        }
        return true;
    }

    public boolean isValidModelAttribute(AttributeDescriptor ad){
        for(IMethodFilterObserver rule:this.methodFilterObservers){
            if(!rule.isValidAttribute(ad))
                return false;
        }
        return true;
    }

    public boolean isChildAttribute(TypeDescriptor td){
        for(IChildAttributeObserver rule:this.modelChildIdentifierObservers){
            if(rule.isChildAttribute(td))
                return true;
        }
        return false;
    }

    public boolean supportAugmentations(TypeDescriptor td){
        for(IChildAttributeObserver rule:this.modelChildIdentifierObservers){
            if(rule.supportAugmentation(td))
                return true;
        }
        return false;
    }

    public boolean isTypeAttribute(AttributeDescriptor ad){
        for(ITypeAttributeObserver rule:this.modelTypeIdentifierObservers){
            if(rule.isTypeAttribute(ad))
                return true;
        }
        return false;
    }

    public Class<?> getElementClass(Object element) {
        return this.classExtractor.getObjectClass(element);
    }

    public IClassExtractorObserver getClassExtractor(){
        return this.classExtractor;
    }
}
