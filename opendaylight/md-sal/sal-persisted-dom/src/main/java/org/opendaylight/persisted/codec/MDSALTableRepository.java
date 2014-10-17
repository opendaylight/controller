package org.opendaylight.persisted.codec;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.opendaylight.yangtools.yang.binding.DataObject;

public class MDSALTableRepository {

    private Map<String, MDSALTable> types = new ConcurrentHashMap<String, MDSALTable>();
    private Map<String, MDSALTable> shortNameToType = new ConcurrentHashMap<String, MDSALTable>();
    public static final String CLASS_REPOSITORY_FILENAME = "./class.repository";
    private static final MDSALTableRepository instance = new MDSALTableRepository();

    private RandomAccessFile ra = null;
    private Map<Integer, MDSALTable> codeToCType = new ConcurrentHashMap<Integer, MDSALTable>();
    private Map<Class<?>, MDSALTable> classToCType = new ConcurrentHashMap<Class<?>, MDSALTable>();

    private boolean locked = false;
    private int maxCodeInRepository = 1000;

    private MDSALTableRepository() {
        load();
    }

    public static MDSALTableRepository getInstance() {
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
        BytesArray ba = new BytesArray(1024);
        MDSALEncoder.encodeInt16(types.size(), ba);
        for (MDSALTable type : types.values()) {
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
                    BytesArray ba = new BytesArray(data);
                    int size = MDSALEncoder.decodeInt16(ba);
                    for (int i = 0; i < size; i++) {
                        MDSALTable type = MDSALTable.decode(ba);
                        types.put(type.getMyClassName(), type);
                        int index = type.getMyClassName().lastIndexOf(".");
                        shortNameToType.put(
                                type.getMyClassName().substring(index + 1),
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

    public void registerClass(MDSALTable type) {
        types.put(type.getMyClassName(), type);
        int index = type.getMyClassName().lastIndexOf(".");
        shortNameToType.put(type.getMyClassName().substring(index + 1), type);
        classToCType.put(type.getMyClass(), type);
        codeToCType.put(type.getClassCode(), type);
    }

    public synchronized int newCode() {
        maxCodeInRepository++;
        return maxCodeInRepository;
    }

    public MDSALTable getCTypeByCode(int code) {
        MDSALTable type = codeToCType.get(code);

        if (type != null)
            return type;

        if (type == null) {
            for (MDSALTable t : types.values()) {
                codeToCType.put(t.getClassCode(), t);
            }
        }
        type = codeToCType.get(code);
        return type;
    }

    public MDSALTable getCTypeByObject(Object object) {
        if(object==null)
            return null;
        if (object instanceof DataObject)
            return getCtypeByClass(((DataObject) object)
                    .getImplementedInterface());
        else
            return getCtypeByClass(object.getClass());
    }

    public MDSALTable getCtypeByClass(Class<?> cls) {
        MDSALTable type = classToCType.get(cls);
        if (type == null) {
            type = types.get(cls.getName());
            if (type != null) {
                type.init();
                registerClass(type);
            } else {
                type = new MDSALTable(cls, null, 0);
                registerClass(type);
            }
        }
        return type;
    }

    public MDSALTable getCtypeByClass(Class<?> cls,int hierarchyLevel) {
        MDSALTable type = classToCType.get(cls);
        if (type == null) {
            type = types.get(cls.getName());
            if (type != null) {
                type.init();
                registerClass(type);
            } else {
                type = new MDSALTable(cls, null, hierarchyLevel);
                registerClass(type);
            }
        }
        return type;
    }

    public MDSALTable getCTypeByClassName(String clsName) {
        return types.get(clsName);
    }

    public MDSALTable getCTypeByShortClassName(String clsName) {
        return shortNameToType.get(clsName);
    }

    public void deleteRepository(){
        //if(true) return;
        for(MDSALTable table:this.types.values()){
            String serName = table.getSerializerClassName();
            serName = "./src/main/yang-gen-sal/"+MDSALTable.replaceAll(serName, ".", "/")+".java";
            File f = new File(serName);
            if(f.exists())
                f.delete();
        }
        File f = new File(CLASS_REPOSITORY_FILENAME);
        f.delete();
    }
}
