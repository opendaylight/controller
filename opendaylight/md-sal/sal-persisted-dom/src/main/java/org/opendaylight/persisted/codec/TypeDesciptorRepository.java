package org.opendaylight.persisted.codec;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TypeDesciptorRepository {

    private Map<String, TypeDescriptor> types = new ConcurrentHashMap<String, TypeDescriptor>();
    private Map<String, TypeDescriptor> shortNameToType = new ConcurrentHashMap<String, TypeDescriptor>();
    public static final String CLASS_REPOSITORY_FILENAME = "./class.repository";
    private static final TypeDesciptorRepository instance = new TypeDesciptorRepository();

    private RandomAccessFile ra = null;
    private Map<Integer, TypeDescriptor> codeToTypeDescriptor = new ConcurrentHashMap<Integer, TypeDescriptor>();
    private Map<Class<?>, TypeDescriptor> classToTypeDescriptor = new ConcurrentHashMap<Class<?>, TypeDescriptor>();

    private boolean locked = false;
    private int maxCodeInRepository = 1000;

    private TypeDesciptorRepository() {
        load();
    }

    public static TypeDesciptorRepository getInstance() {
        return instance;
    }

    public synchronized void lock() {
        if (locked)
            try {
                this.wait();
            } catch (Exception err) {
            }
        locked = true;
        while (ra == null) {
            try {
                ra = new RandomAccessFile(CLASS_REPOSITORY_FILENAME, "rw");
            } catch (Exception err) {
                err.printStackTrace();
            }
            if (ra == null)
                try {
                    Thread.sleep(500);
                } catch (Exception err) {
                }
        }
    }

    public synchronized void unlock() {
        try {
            ra.close();
            ra = null;
            locked = false;
            this.notifyAll();
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    public void save() {
        lock();
        try {
            byte data[] = getMDSALTableRepositoryData();
            ra.seek(0);
            ra.write(data);
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
            File f = new File(CLASS_REPOSITORY_FILENAME);
            if (f.exists() && f.length() > 0) {
                try {
                    byte[] data = new byte[(int) f.length()];
                    ra.seek(0);
                    ra.read(data);
                    EncodeDataContainer ba = new EncodeDataContainer(data);
                    int size = EncodeUtils.decodeInt16(ba);
                    for (int i = 0; i < size; i++) {
                        TypeDescriptor type = TypeDescriptor.decode(ba);
                        types.put(type.getTypeClassName(), type);
                        int index = type.getTypeClassName().lastIndexOf(".");
                        shortNameToType.put(
                                type.getTypeClassName().substring(index + 1),
                                type);
                    }
                } catch (Exception err) {
                    err.printStackTrace();
                }
            }
        } finally {
            unlock();
        }
    }

    public void registerClass(TypeDescriptor type) {
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

    public TypeDescriptor getTypeDescriptorByObject(Object object) {
        if(object==null)
            return null;
        return getTypeDescriptorByClass(TypeDescriptor.getElementClass(object));
    }

    public TypeDescriptor getTypeDescriptorByClass(Class<?> cls) {
        TypeDescriptor type = classToTypeDescriptor.get(cls);
        if (type == null) {
            type = types.get(cls.getName());
            if (type != null) {
                type.init();
                registerClass(type);
            } else {
                type = new TypeDescriptor(cls, null, 0);
                registerClass(type);
            }
        }
        return type;
    }

    public TypeDescriptor getTypeDescriptorClass(Class<?> cls,int hierarchyLevel) {
        TypeDescriptor type = classToTypeDescriptor.get(cls);
        if (type == null) {
            type = types.get(cls.getName());
            if (type != null) {
                type.init();
                registerClass(type);
            } else {
                type = new TypeDescriptor(cls, null, hierarchyLevel);
                registerClass(type);
            }
        }
        return type;
    }

    public TypeDescriptor getTypeDescriptorByClassName(String clsName) {
        return types.get(clsName);
    }

    public TypeDescriptor getTypeDescriptorByShortClassName(String clsName) {
        return shortNameToType.get(clsName);
    }

    public void deleteRepository(){
        //if(true) return;
        for(TypeDescriptor table:this.types.values()){
            String serName = table.getSerializerClassName();
            if(serName!=null){
                serName = "./src/main/yang-gen-sal/"+TypeDescriptor.replaceAll(serName, ".", "/")+".java";
                File f = new File(serName);
                if(f.exists())
                    f.delete();
            }
        }
        File f = new File(CLASS_REPOSITORY_FILENAME);
        f.delete();
    }
}
