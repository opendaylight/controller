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
import org.opendaylight.datasand.codec.EncodeUtils;
import org.opendaylight.datasand.codec.MD5Identifier;
import org.opendaylight.datasand.codec.TypeDescriptor;

public class ShardLocation {
    private int xVector = -1;
    private int yVector = -1;
    private int zVector = -1;
    private String blockFilesLocation = null;
    private String blockKey = null;
    private Map<Class<?>, DataPersister> dataFiles = new HashMap<Class<?>, DataPersister>();
    private ObjectDataStore db = null;

    public ShardLocation(int _x, int _y, int _z, String _blockKey,ObjectDataStore _db) {
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

    public void save() {
        for (DataPersister f : dataFiles.values()) {
            f.save();
        }
    }

    public void close() {
        for (DataPersister f : dataFiles.values()) {
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
                    DataPersister df = new DataPersister(blockFilesLocation,cls,db.getTypeDescriptorsContainer());
                    this.dataFiles.put(cls, df);
                    df.load();
                } catch (Exception err) {
                    err.printStackTrace();
                }
            }
        }
    }

    public void writeObject(Object e, MD5Identifier eMD5, int parentRecordIndex) {
        Class<?> clazz = db.getTypeDescriptorsContainer().getElementClass(e);
        EncodeDataContainer ba = new EncodeDataContainer(1024, "",db.getTypeDescriptorsContainer());
        EncodeUtils.encodeObject(e, ba, clazz);
        DataPersister df = getDataFile(clazz);
        int recordIndex = df.writeData(parentRecordIndex,-1, eMD5, ba.getData());
        writeSubObjects(ba, recordIndex);
    }

    public void updateObject(Object e,int recordIndex, MD5Identifier eMD5, int parentRecordIndex) {
        Class<?> clazz = db.getTypeDescriptorsContainer().getElementClass(e);
        EncodeDataContainer ba = new EncodeDataContainer(1024, "",db.getTypeDescriptorsContainer());
        EncodeUtils.encodeObject(e, ba, clazz);
        DataPersister df = getDataFile(clazz);
        recordIndex = df.writeData(parentRecordIndex,recordIndex, eMD5, ba.getData());
        updateSubObjects(ba, recordIndex);
    }

    private void updateSubObjects(EncodeDataContainer ba, int parentRecordIndex) {
        Map<Integer, List<EncodeDataContainer>> subElementsData = ba.getSubElementsData();
        for (Map.Entry<Integer, List<EncodeDataContainer>> entry : subElementsData.entrySet()) {
            TypeDescriptor table = db.getTypeDescriptorsContainer().getTypeDescriptorByCode(entry.getKey());
            DataPersister df = getDataFile(table.getTypeClass());
            int[] indexes = df.getRecordIndexes(parentRecordIndex);
            int currentIndex = 0;
            for (EncodeDataContainer subBA : entry.getValue()) {
                if(indexes==null || currentIndex>=indexes.length){
                    int recordIndex = df.writeData(parentRecordIndex,-1,subBA.getMD5ID() ,subBA.getData());
                    updateSubObjects(subBA, recordIndex);
                }else{
                    int recordIndex = df.writeData(parentRecordIndex,indexes[currentIndex],subBA.getMD5ID() ,subBA.getData());
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
            DataPersister df = getDataFile(table.getTypeClass());
            for (EncodeDataContainer subBA : entry.getValue()) {
                int recordIndex = df.writeData(parentRecordIndex,-1,subBA.getMD5ID() ,subBA.getData());
                writeSubObjects(subBA, recordIndex);
            }
        }
    }

    public Object deleteObject(Class<?> clazz,MD5Identifier md5ID) {
        DataPersister dataFile = this.dataFiles.get(clazz);
        byte data[] = dataFile.delete(md5ID);
        int recordIndex = dataFile.getIndexByMD5(md5ID);
        if(data==null || DataPersister.isDeleted(data)){
            data = new byte[2];
            EncodeUtils.encodeNULL(data, 0);
        }
        EncodeDataContainer ba = new EncodeDataContainer(data,db.getTypeDescriptorsContainer());
        TypeDescriptor table = db.getTypeDescriptorsContainer().getTypeDescriptorByClass(clazz);
        deleteSubObjects(ba, table, recordIndex);
        return EncodeUtils.decodeObject(ba);
    }

    public Object readObject(Class<?> clazz,MD5Identifier md5ID) {
        DataPersister dataFile = this.dataFiles.get(clazz);
        byte data[] = dataFile.read(md5ID);
        int recordIndex = dataFile.getIndexByMD5(md5ID);
        if(data==null || DataPersister.isDeleted(data)){
            data = new byte[2];
            EncodeUtils.encodeNULL(data, 0);
        }
        EncodeDataContainer ba = new EncodeDataContainer(data,db.getTypeDescriptorsContainer());
        TypeDescriptor table = db.getTypeDescriptorsContainer().getTypeDescriptorByClass(clazz);
        readSubObjects(ba, table, recordIndex);
        return EncodeUtils.decodeObject(ba);
    }

    public Object readObject(Class<?> clazz, int recordIndex) {
        DataPersister dataFile = this.dataFiles.get(clazz);
        byte data[] = dataFile.read(recordIndex);
        if(data==null || DataPersister.isDeleted(data)){
            data = new byte[2];
            EncodeUtils.encodeNULL(data, 0);
        }
        EncodeDataContainer ba = new EncodeDataContainer(data,db.getTypeDescriptorsContainer());
        TypeDescriptor table = db.getTypeDescriptorsContainer().getTypeDescriptorByClass(clazz);
        readSubObjects(ba, table, recordIndex);
        return EncodeUtils.decodeObject(ba);
    }

    public Object deleteObject(Class<?> clazz, int recordIndex) {
        DataPersister dataFile = this.dataFiles.get(clazz);
        byte data[] = dataFile.delete(recordIndex);
        if(data==null || DataPersister.isDeleted(data)){
            data = new byte[2];
            EncodeUtils.encodeNULL(data, 0);
        }
        EncodeDataContainer ba = new EncodeDataContainer(data,db.getTypeDescriptorsContainer());
        TypeDescriptor table = db.getTypeDescriptorsContainer().getTypeDescriptorByClass(clazz);
        readSubObjects(ba, table, recordIndex);
        return EncodeUtils.decodeObject(ba);
    }

    public Object readObjectNoChildren(TypeDescriptor table, int recordIndex) {
        DataPersister dataFile = this.dataFiles.get(table.getTypeClass());
        byte data[] = dataFile.read(recordIndex);
        if(data==null || DataPersister.isDeleted(data)){
            data = new byte[2];
            EncodeUtils.encodeNULL(data, 0);
        }
        EncodeDataContainer ba = new EncodeDataContainer(data,db.getTypeDescriptorsContainer());
        return EncodeUtils.decodeObject(ba);
    }

    public Object deleteObjectNoChildren(TypeDescriptor table, int recordIndex) {
        DataPersister dataFile = this.dataFiles.get(table.getTypeClass());
        byte data[] = dataFile.delete(recordIndex);
        if(data==null || DataPersister.isDeleted(data)){
            data = new byte[2];
            EncodeUtils.encodeNULL(data, 0);
        }
        EncodeDataContainer ba = new EncodeDataContainer(data,db.getTypeDescriptorsContainer());
        return EncodeUtils.decodeObject(ba);
    }

    public Object readAllObjectFromNode(Class<?> clazz, int recordIndex) {
        DataPersister dataFile = this.dataFiles.get(clazz);
        Class<?> currentClass = clazz;
        int currentIndex = recordIndex;
        while(dataFile.getParentIndex(currentIndex)!=-1){
            TypeDescriptor table = db.getTypeDescriptorsContainer().getTypeDescriptorByClass(currentClass);
            TypeDescriptor parentTable = table.getParent();
            currentIndex = dataFile.getParentIndex(currentIndex);
            currentClass = parentTable.getTypeClass();
            dataFile = this.dataFiles.get(currentClass);
        }
        return readObject(currentClass, currentIndex);
    }

    public void readSubObjects(EncodeDataContainer ba, TypeDescriptor parentTable,int parentRecordIndex) {
        for (AttributeDescriptor col : parentTable.getChildren().keySet()) {
            TypeDescriptor subTable = db.getTypeDescriptorsContainer().getTypeDescriptorByClass(col.getReturnType());
            DataPersister dataFile = this.dataFiles.get(subTable.getTypeClass());
            if(dataFile==null){
                EncodeDataContainer subBA = new EncodeDataContainer(2,db.getTypeDescriptorsContainer());
                EncodeUtils.encodeNULL(subBA);
                ba.addSubElementData(subTable.getClassCode(), subBA,null);
                continue;
            }
            int[] recordIndexes = dataFile.getRecordIndexes(parentRecordIndex);
            byte[][] data = dataFile.read(recordIndexes);
            if (data != null) {
                for (int i = 0; i < data.length; i++) {
                    if(!DataPersister.isDeleted(data[i])){
                        EncodeDataContainer subBA = new EncodeDataContainer(data[i],db.getTypeDescriptorsContainer());
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
            DataPersister dataFile = this.dataFiles.get(subTable.getTypeClass());
            if(dataFile==null){
                EncodeDataContainer subBA = new EncodeDataContainer(2,db.getTypeDescriptorsContainer());
                EncodeUtils.encodeNULL(subBA);
                ba.addSubElementData(subTable.getClassCode(), subBA,null);
                continue;
            }
            int[] recordIndexes = dataFile.getRecordIndexes(parentRecordIndex);
            byte[][] data = dataFile.delete(recordIndexes);
            if (data != null) {
                for (int i = 0; i < data.length; i++) {
                    if(!DataPersister.isDeleted(data[i])){
                        EncodeDataContainer subBA = new EncodeDataContainer(data[i],db.getTypeDescriptorsContainer());
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

    public DataPersister getDataFile(Class<?> c) {
        synchronized(this){
            DataPersister df = dataFiles.get(c);
            if (df == null) {
                df = new DataPersister(blockFilesLocation, c,db.getTypeDescriptorsContainer());
                dataFiles.put(c, df);
            }
            return df;
        }
    }
}