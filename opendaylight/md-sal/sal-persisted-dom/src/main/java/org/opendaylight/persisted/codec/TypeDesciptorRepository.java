package org.opendaylight.persisted.codec;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.opendaylight.persisted.autoagents.AutonomousAgentManager;
import org.opendaylight.persisted.autoagents.ClusterMap;

public class TypeDesciptorRepository {

    private Map<String, TypeDescriptor> types = new ConcurrentHashMap<String, TypeDescriptor>();
    private Map<String, TypeDescriptor> shortNameToType = new ConcurrentHashMap<String, TypeDescriptor>();
    private String localhost = "127.0.0.1";
    private String repositoryFileName = "./type-descriptor-" + localhost+ ".repository";
    private static TypeDesciptorRepository instance = new TypeDesciptorRepository();

    private Map<Integer, TypeDescriptor> codeToTypeDescriptor = new ConcurrentHashMap<Integer, TypeDescriptor>();
    private Map<Class<?>, TypeDescriptor> classToTypeDescriptor = new ConcurrentHashMap<Class<?>, TypeDescriptor>();
    private int maxCodeInRepository = 1000;
    private static final ThreadLocal<RandomAccessFile> locked = new ThreadLocal<RandomAccessFile>();
    private boolean wasLoaded = false;

    private TypeDesciptorRepository() {
    }

    public void initClustering(AutonomousAgentManager m){
        Map map = new ClusterMap<Class<?>, TypeDescriptor>(6,m.getNetworkNode().getLocalHost(),m);
        for(Map.Entry<Class<?>, TypeDescriptor> entry:classToTypeDescriptor.entrySet()){
            map.put(entry.getKey(), entry.getValue());
        }
        this.classToTypeDescriptor = map;
    }

    public static TypeDesciptorRepository getInstance() {
        if (!instance.wasLoaded) {
            synchronized (instance) {
                if (!instance.wasLoaded) {
                    instance.wasLoaded = true;
                    instance.load();
                }
            }
        }
        return instance;
    }

    public static void cleanInstance() {
        instance = new TypeDesciptorRepository();
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
        EncodeDataContainer ba = new EncodeDataContainer(1024);
        EncodeUtils.encodeInt16(types.size(), ba);
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
                    EncodeDataContainer ba = new EncodeDataContainer(data);
                    int size = EncodeUtils.decodeInt16(ba);
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
                        Class augClass = td.getKnownAugmentingClasses()
                                .iterator().next();
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
        types.put(type.getTypeClassName(), type);
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
        Class<?> cls = TypeDescriptor.getElementClass(object);
        return this.types.get(cls.getName()) != null;
    }

    public TypeDescriptor checkTypeDescriptorByObject(Object object) {
        if (object == null)
            return null;
        return checkTypeDescriptorByClass(TypeDescriptor.getElementClass(object));
    }

    public TypeDescriptor getTypeDescriptorByObject(Object object) {
        if (object == null)
            return null;
        return getTypeDescriptorByClass(TypeDescriptor.getElementClass(object));
    }

    public TypeDescriptor getTypeDescriptorByClass(Class<?> cls) {
        return getTypeDescriptorByClass(cls, 0);
    }

    public TypeDescriptor checkTypeDescriptorByClass(Class<?> cls) {
        return checkTypeDescriptorByClass(cls, 0);
    }

    public TypeDescriptor checkTypeDescriptorByClass(Class<?> cls, int hierarchy) {
        TypeDescriptor type = classToTypeDescriptor.get(cls);
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

    private synchronized TypeDescriptor newTypeDescriptor(Class<?> cls,
            int hierarchy) {
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
            td = new TypeDescriptor(cls, null, hierarchy);
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
}
