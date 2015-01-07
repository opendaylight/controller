package org.opendaylight.datasand.store;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.datasand.codec.AttributeDescriptor;
import org.opendaylight.datasand.codec.EncodeDataContainer;
import org.opendaylight.datasand.codec.EncodeDataContainerFactory;
import org.opendaylight.datasand.codec.TypeDescriptor;

public class Shard {
    private int xVector = -1;
    private int yVector = -1;
    private int zVector = -1;
    private String blockFilesLocation = null;
    private String blockKey = null;
    private Map<Class<?>, DataPersister> dataPersisters = new HashMap<Class<?>, DataPersister>();
    private ObjectDataStore db = null;

    public Shard(int _x, int _y, int _z, String _blockKey,ObjectDataStore _db) {
        this.xVector = _x;
        this.yVector = _y;
        this.zVector = _z;
        this.blockKey = _blockKey;
        this.db = _db;
        blockFilesLocation = db.getDataLocation() + "/X-" + xVector + "/Y-" + yVector + "/Z-" + zVector + "/" + this.blockKey;
        File f = new File(blockFilesLocation);
        if (!f.exists()) {
            f.mkdirs();
        }
        saveBlockKey(f.getPath(), _blockKey);
        load();
    }

    public static final void saveBlockKey(String path, String blockKey) {
        try {
            File f = new File(path + "/block.key");
            FileOutputStream out = new FileOutputStream(f);
            out.write(blockKey.toString().getBytes());
            out.close();
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    public static final String readBlockKey(String path) {
        try {
            File f = new File(path + "/block.key");
            FileInputStream in = new FileInputStream(f);
            byte data[] = new byte[(int) f.length()];
            in.read(data);
            in.close();
            return new String(data);
        } catch (Exception err) {
            err.printStackTrace();
        }
        return null;
    }

    public String getBlockKey() {
        return this.blockKey;
    }

    public String getBlockLocation(){
        return this.blockFilesLocation;
    }

    public void save() {
        for (DataPersister f : dataPersisters.values()) {
            f.save();
        }
    }

    public void close() {
        for (DataPersister f : dataPersisters.values()) {
            f.close();
        }
    }

    public void load() {
        File dir = new File(blockFilesLocation);
        File files[] = dir.listFiles();
        for (File f : files) {
            if (f.getName().endsWith(".loc")) {
                try {
                    Class<?> cls = Class.forName(f.getName().substring(0,f.getName().lastIndexOf(".")));
                    DataPersister df = DataPersisterFactory.newDataPersister(db.getEncoderType(),this,cls,db.getTypeDescriptorsContainer());
                    this.dataPersisters.put(cls, df);
                    df.load();
                } catch (Exception err) {
                    err.printStackTrace();
                }
            }
        }
    }

    public void writeObject(Object e, Object key, int parentRecordIndex) {
        Class<?> clazz = db.getTypeDescriptorsContainer().getElementClass(e);
        EncodeDataContainer ba = EncodeDataContainerFactory.newContainer(null,key,db.getEncoderType(),db.getTypeDescriptorsContainer());
        ba.getEncoder().encodeObject(e, ba, clazz);
        DataPersister df = getDataPersister(clazz);
        int recordIndex = df.write(parentRecordIndex,-1, ba);
        writeSubObjects(ba, recordIndex);
    }

    public void updateObject(Object e,int recordIndex, Object key, int parentRecordIndex) {
        Class<?> clazz = db.getTypeDescriptorsContainer().getElementClass(e);
        EncodeDataContainer ba = EncodeDataContainerFactory.newContainer(null,key,db.getEncoderType(),db.getTypeDescriptorsContainer());
        ba.getEncoder().encodeObject(e, ba, clazz);
        DataPersister df = getDataPersister(clazz);
        recordIndex = df.write(parentRecordIndex,recordIndex, ba);
        updateSubObjects(ba, recordIndex);
    }

    private void updateSubObjects(EncodeDataContainer ba, int parentRecordIndex) {
        Map<Integer, List<EncodeDataContainer>> subElementsData = ba.getSubElementsData();
        for (Map.Entry<Integer, List<EncodeDataContainer>> entry : subElementsData.entrySet()) {
            TypeDescriptor table = db.getTypeDescriptorsContainer().getTypeDescriptorByCode(entry.getKey());
            DataPersister df = getDataPersister(table.getTypeClass());
            int[] indexes = df.getRecordIndexesByParentIndex(parentRecordIndex);
            int currentIndex = 0;
            for (EncodeDataContainer subBA : entry.getValue()) {
                if(indexes==null || currentIndex>=indexes.length){
                    int recordIndex = df.write(parentRecordIndex,-1,subBA);
                    updateSubObjects(subBA, recordIndex);
                }else{
                    int recordIndex = df.write(parentRecordIndex,indexes[currentIndex],subBA);
                    updateSubObjects(subBA, recordIndex);
                }
                currentIndex++;
            }
        }
    }

    private void writeSubObjects(EncodeDataContainer ba, int parentRecordIndex) {
        Map<Integer, List<EncodeDataContainer>> subElementsData = ba.getSubElementsData();
        for (Map.Entry<Integer, List<EncodeDataContainer>> entry : subElementsData.entrySet()) {
            TypeDescriptor table = db.getTypeDescriptorsContainer().getTypeDescriptorByCode(entry.getKey());
            DataPersister df = getDataPersister(table.getTypeClass());
            for (EncodeDataContainer subBA : entry.getValue()) {
                int recordIndex = df.write(parentRecordIndex,-1,subBA);
                writeSubObjects(subBA, recordIndex);
            }
        }
    }

    public Object deleteObject(Class<?> clazz,Object key) {
        DataPersister dataPersister = this.dataPersisters.get(clazz);
        Object data = dataPersister.delete(key);
        int recordIndex = dataPersister.getIndexByKey(key);
        EncodeDataContainer ba = EncodeDataContainerFactory.newContainer(data,key,db.getEncoderType(),db.getTypeDescriptorsContainer());
        if(data==null || dataPersister.isDeleted(data)){
            ba.getEncoder().encodeNULL(ba);
            ba.resetLocation();
        }
        TypeDescriptor table = db.getTypeDescriptorsContainer().getTypeDescriptorByClass(clazz);
        deleteSubObjects(ba, table, recordIndex);
        return ba.getEncoder().decodeObject(ba);
    }

    public Object readObject(Class<?> clazz,Object key) {
        DataPersister dataPersister = this.dataPersisters.get(clazz);
        Object data = dataPersister.read(key);
        int recordIndex = dataPersister.getIndexByKey(key);
        EncodeDataContainer ba = EncodeDataContainerFactory.newContainer(data,key,db.getEncoderType(),db.getTypeDescriptorsContainer());
        if(data==null || dataPersister.isDeleted(data)){
            ba.getEncoder().encodeNULL(ba);
            ba.resetLocation();
        }
        TypeDescriptor table = db.getTypeDescriptorsContainer().getTypeDescriptorByClass(clazz);
        readSubObjects(ba, table, recordIndex);
        return ba.getEncoder().decodeObject(ba);
    }

    public Object readObject(Class<?> clazz, int recordIndex) {
        DataPersister dataPersister = this.dataPersisters.get(clazz);
        Object data = dataPersister.read(recordIndex);
        EncodeDataContainer ba = EncodeDataContainerFactory.newContainer(data,null,db.getEncoderType(),db.getTypeDescriptorsContainer());
        if(data==null || dataPersister.isDeleted(data)){
            ba.getEncoder().encodeNULL(ba);
            ba.resetLocation();
        }
        TypeDescriptor table = db.getTypeDescriptorsContainer().getTypeDescriptorByClass(clazz);
        readSubObjects(ba, table, recordIndex);
        return ba.getEncoder().decodeObject(ba);
    }

    public Object deleteObject(Class<?> clazz, int recordIndex) {
        DataPersister dataPersister = this.dataPersisters.get(clazz);
        Object data = dataPersister.delete(recordIndex);
        EncodeDataContainer ba = EncodeDataContainerFactory.newContainer(data,null,db.getEncoderType(),db.getTypeDescriptorsContainer());
        if(data==null || dataPersister.isDeleted(data)){
            ba.getEncoder().encodeNULL(ba);
            ba.resetLocation();
        }
        TypeDescriptor table = db.getTypeDescriptorsContainer().getTypeDescriptorByClass(clazz);
        readSubObjects(ba, table, recordIndex);
        return ba.getEncoder().decodeObject(ba);
    }

    public Object readObjectNoChildren(TypeDescriptor table, int recordIndex) {
        DataPersister dataPersister = this.dataPersisters.get(table.getTypeClass());
        Object data = dataPersister.read(recordIndex);
        EncodeDataContainer ba = EncodeDataContainerFactory.newContainer(data,null,db.getEncoderType(),db.getTypeDescriptorsContainer());
        if(data==null || dataPersister.isDeleted(data)){
            ba.getEncoder().encodeNULL(ba);
            ba.resetLocation();
        }
        return ba.getEncoder().decodeObject(ba);
    }

    public Object deleteObjectNoChildren(TypeDescriptor table, int recordIndex) {
        DataPersister dataPersister = this.dataPersisters.get(table.getTypeClass());
        Object data = dataPersister.delete(recordIndex);
        EncodeDataContainer ba = EncodeDataContainerFactory.newContainer(data, null,db.getEncoderType(),db.getTypeDescriptorsContainer());
        if(data==null || dataPersister.isDeleted(data)){
            ba.getEncoder().encodeNULL(ba);
            ba.resetLocation();
        }
        return ba.getEncoder().decodeObject(ba);
    }

    public Object readAllObjectFromNode(Class<?> clazz, int recordIndex) {
        DataPersister dataPersister = this.dataPersisters.get(clazz);
        Class<?> currentClass = clazz;
        int currentIndex = recordIndex;
        while(dataPersister.getParentIndex(currentIndex)!=-1){
            TypeDescriptor table = db.getTypeDescriptorsContainer().getTypeDescriptorByClass(currentClass);
            TypeDescriptor parentTable = table.getParent();
            currentIndex = dataPersister.getParentIndex(currentIndex);
            currentClass = parentTable.getTypeClass();
            dataPersister = this.dataPersisters.get(currentClass);
        }
        return readObject(currentClass, currentIndex);
    }

    public void readSubObjects(EncodeDataContainer ba, TypeDescriptor parentTable,int parentRecordIndex) {
        for (AttributeDescriptor col : parentTable.getChildren().keySet()) {
            TypeDescriptor subTable = db.getTypeDescriptorsContainer().getTypeDescriptorByClass(col.getReturnType());
            DataPersister dataPersister = this.dataPersisters.get(subTable.getTypeClass());
            if(dataPersister==null){
                EncodeDataContainer subBA = EncodeDataContainerFactory.newContainer(null, null, db.getEncoderType(),db.getTypeDescriptorsContainer());
                ba.getEncoder().encodeNULL(subBA);
                ba.addSubElementData(subTable.getClassCode(), subBA,null);
                continue;
            }
            int[] recordIndexes = dataPersister.getRecordIndexesByParentIndex(parentRecordIndex);
            Object data[] = dataPersister.read(recordIndexes);
            if (data != null) {
                for (int i = 0; i < data.length; i++) {
                    if(!dataPersister.isDeleted(data[i])){
                        EncodeDataContainer subBA = EncodeDataContainerFactory.newContainer(data[i],null,db.getEncoderType(),db.getTypeDescriptorsContainer());
                        ba.addSubElementData(subTable.getClassCode(), subBA,null);
                        readSubObjects(subBA, subTable, recordIndexes[i]);
                    }
                }
            }
        }
        Set<Class<?>> knownAugmentations = parentTable.getKnownAugmentingClasses().keySet();
        for (Class<?> augClass : knownAugmentations) {
            TypeDescriptor augTable = db.getTypeDescriptorsContainer().getTypeDescriptorByClass(augClass);
            readSubObjects(ba, augTable, parentRecordIndex);
        }
    }

    public void deleteSubObjects(EncodeDataContainer ba, TypeDescriptor parentTable,int parentRecordIndex) {
        for (AttributeDescriptor col : parentTable.getChildren().keySet()) {
            TypeDescriptor subTable = db.getTypeDescriptorsContainer().getTypeDescriptorByClass(col.getReturnType());
            DataPersister dataPersister = this.dataPersisters.get(subTable.getTypeClass());
            if(dataPersister==null){
                EncodeDataContainer subBA = EncodeDataContainerFactory.newContainer(null,null,db.getEncoderType(),db.getTypeDescriptorsContainer());
                ba.getEncoder().encodeNULL(subBA);
                ba.addSubElementData(subTable.getClassCode(), subBA,null);
                continue;
            }
            int[] recordIndexes = dataPersister.getRecordIndexesByParentIndex(parentRecordIndex);
            Object[] data = dataPersister.delete(recordIndexes);
            if (data != null) {
                for (int i = 0; i < data.length; i++) {
                    if(!dataPersister.isDeleted(data[i])){
                        EncodeDataContainer subBA = EncodeDataContainerFactory.newContainer(data[i],null,db.getEncoderType(),db.getTypeDescriptorsContainer());
                        ba.addSubElementData(subTable.getClassCode(), subBA,null);
                        deleteSubObjects(subBA, subTable, recordIndexes[i]);
                    }
                }
            }
        }
        Set<Class<?>> knownAugmentations = parentTable.getKnownAugmentingClasses().keySet();
        for (Class<?> augClass : knownAugmentations) {
            TypeDescriptor augTable = db.getTypeDescriptorsContainer().getTypeDescriptorByClass(augClass);
            deleteSubObjects(ba, augTable, parentRecordIndex);
        }
    }

    public DataPersister getDataPersister(Class<?> c) {
        synchronized(this){
            DataPersister df = dataPersisters.get(c);
            if (df == null) {
                df = DataPersisterFactory.newDataPersister(db.getEncoderType(), this, c,db.getTypeDescriptorsContainer());
                dataPersisters.put(c, df);
            }
            return df;
        }
    }
}