package org.opendaylight.persisted;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.persisted.codec.BytesArray;
import org.opendaylight.persisted.codec.MDSALColumn;
import org.opendaylight.persisted.codec.MDSALEncoder;
import org.opendaylight.persisted.codec.MDSALTable;
import org.opendaylight.persisted.codec.MDSALTableRepository;
import org.opendaylight.yangtools.yang.binding.DataObject;

public class MDSALBlockDataLocation {
    private int xVector = -1;
    private int yVector = -1;
    private int zVector = -1;
    private String blockFilesLocation = null;
    private String blockKey = null;
    private Map<Class<?>, MDSALDataFile> dataFiles = new HashMap<Class<?>, MDSALDataFile>();

    public MDSALBlockDataLocation(int _x, int _y, int _z, String _blockKey) {
        this.xVector = _x;
        this.yVector = _y;
        this.zVector = _z;
        this.blockKey = _blockKey;
        blockFilesLocation = MDSALDatabase.DB_LOCATION + "/X-" + xVector
                + "/Y-" + yVector + "/Z-" + zVector + "/" + this.blockKey;
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
        for (MDSALDataFile f : dataFiles.values()) {
            f.save();
        }
    }

    public void close() {
        for (MDSALDataFile f : dataFiles.values()) {
            f.close();
        }
    }

    public void load() {
        File dir = new File(blockFilesLocation);
        File files[] = dir.listFiles();
        for (File f : files) {
            if (f.getName().endsWith(".loc")) {
                try {
                    Class<?> cls = Class.forName(f.getName().substring(0,
                            f.getName().lastIndexOf(".")));
                    MDSALDataFile df = new MDSALDataFile(blockFilesLocation,
                            cls);
                    this.dataFiles.put(cls, df);
                    df.load();
                } catch (Exception err) {
                    err.printStackTrace();
                }
            }
        }
    }

    public void writeObject(Object e, MD5ID eMD5, int parentRecordIndex) {
        Class<?> clazz = null;
        if (e instanceof DataObject) {
            clazz = ((DataObject) e).getImplementedInterface();
        } else
            clazz = e.getClass();
        BytesArray ba = new BytesArray(1024, "");
        MDSALEncoder.encodeObject(e, ba, clazz);
        MDSALDataFile df = getDataFile(clazz);
        int recordIndex = df.writeData(parentRecordIndex, eMD5, ba.getData());
        writeSubObjects(ba, recordIndex);
    }

    private void writeSubObjects(BytesArray ba, int parentRecordIndex) {
        Map<Integer, List<BytesArray>> subElementsData = ba
                .getSubElementsData();
        for (Map.Entry<Integer, List<BytesArray>> entry : subElementsData
                .entrySet()) {
            MDSALTable table = MDSALTableRepository.getInstance()
                    .getCTypeByCode(entry.getKey());
            MDSALDataFile df = getDataFile(table.getMyClass());
            for (BytesArray subBA : entry.getValue()) {
                int recordIndex = df.writeData(parentRecordIndex, null,
                        subBA.getData());
                writeSubObjects(subBA, recordIndex);
            }
        }
    }

    public Object readObject(Class<?> clazz, int recordIndex) {
        MDSALDataFile dataFile = this.dataFiles.get(clazz);
        byte data[] = dataFile.read(recordIndex);
        if(data==null){
            data = new byte[2];
            MDSALEncoder.encodeNULL(data, 0);
        }
        BytesArray ba = new BytesArray(data);
        MDSALTable table = MDSALTableRepository.getInstance().getCtypeByClass(
                clazz);
        readSubObjects(ba, table, recordIndex);
        return MDSALEncoder.decodeObject(ba);
    }

    public Object readAllObjectFromNode(Class<?> clazz, int recordIndex) {
        MDSALDataFile dataFile = this.dataFiles.get(clazz);
        Class currentClass = clazz;
        int currentIndex = recordIndex;
        while(dataFile.getParentIndex(currentIndex)!=-1){
            MDSALTable table = MDSALTableRepository.getInstance().getCtypeByClass(currentClass);
            MDSALTable parentTable = table.getParent();
            currentIndex = dataFile.getParentIndex(currentIndex);
            currentClass = parentTable.getMyClass();
            dataFile = this.dataFiles.get(currentClass);
        }
        return readObject(currentClass, currentIndex);
    }

    public void readSubObjects(BytesArray ba, MDSALTable parentTable,int parentRecordIndex) {
        for (MDSALColumn col : parentTable.getChildren().keySet()) {
            MDSALTable subTable = MDSALTableRepository.getInstance()
                    .getCtypeByClass(col.getReturnType());
            MDSALDataFile dataFile = this.dataFiles.get(subTable.getMyClass());
            if(dataFile==null){
                BytesArray subBA = new BytesArray(2);
                MDSALEncoder.encodeNULL(subBA);
                ba.addSubElementData(subTable.getClassCode(), subBA);
                continue;
            }
            List<Integer> recordIndexes = dataFile.getRecordIndexes(parentRecordIndex);
            byte[][] data = dataFile.read(recordIndexes);
            if (data != null) {
                for (int i = 0; i < data.length; i++) {
                    BytesArray subBA = new BytesArray(data[i]);
                    ba.addSubElementData(subTable.getClassCode(), subBA);
                    readSubObjects(subBA, subTable, recordIndexes.get(i));
                }
            }
        }
        Set<Class<?>> knownAugmentations = parentTable
                .getKnownAugmentingClasses();
        for (Class<?> augClass : knownAugmentations) {
            MDSALTable augTable = MDSALTableRepository.getInstance()
                    .getCtypeByClass(augClass);
            readSubObjects(ba, augTable, parentRecordIndex);
        }
    }

    private MDSALDataFile getDataFile(Class<?> c) {
        MDSALDataFile df = dataFiles.get(c);
        if (df == null) {
            df = new MDSALDataFile(blockFilesLocation, c);
            dataFiles.put(c, df);
        }
        return df;
    }
}