package org.opendaylight.persisted;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendaylight.persisted.codec.EncodeDataContainer;
import org.opendaylight.persisted.codec.EncodeUtils;

public class DataPersister {
    private File file = null;
    private RandomAccessFile randomAccessFile = null;
    private Map<MD5Identifier, DataLocation> locations = new HashMap<MD5Identifier, DataLocation>();
    private Map<Integer, List<Integer>> parentIndexToRecordIndex = new HashMap<Integer, List<Integer>>();
    private List<Integer> indexToParentIndex = new ArrayList<Integer>();
    private List<DataLocation> locationsByIndex = new ArrayList<DataLocation>();
    private String dataFileLocation = null;
    private String dataFileDefLocation = null;
    private Class<?> type = null;
    private long fileSize = 0;
    private long lastLocation = 0;

    public void save() {
        try {
            EncodeDataContainer ba = new EncodeDataContainer(1024);
            EncodeUtils.encodeInt32(locations.size(), ba);
            for (Map.Entry<MD5Identifier, DataLocation> entry : locations.entrySet()) {
                EncodeUtils.encodeInt64(entry.getKey().getA(), ba);
                EncodeUtils.encodeInt64(entry.getKey().getB(), ba);
                entry.getValue().encode(ba);
            }
            EncodeUtils.encodeInt32(locationsByIndex.size(), ba);
            for (DataLocation loc:locationsByIndex) {
                loc.encode(ba);
            }
            EncodeUtils.encodeInt32(parentIndexToRecordIndex.size(), ba);
            for (Map.Entry<Integer, List<Integer>> entry : parentIndexToRecordIndex.entrySet()) {
                EncodeUtils.encodeInt32(entry.getKey(), ba);
                EncodeUtils.encodeInt32(entry.getValue().size(), ba);
                for (int index : entry.getValue()) {
                    EncodeUtils.encodeInt32(index, ba);
                }
            }
            EncodeUtils.encodeInt32(indexToParentIndex.size(), ba);
            for(Integer i:indexToParentIndex){
                EncodeUtils.encodeInt32(i, ba);
            }
            FileOutputStream out = new FileOutputStream(dataFileDefLocation);
            out.write(ba.getData());
            out.close();
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    public void load() {
        File f = new File(dataFileDefLocation);
        if (f.exists()) {
            fileSize = f.length();
            try {
                byte data[] = new byte[(int) f.length()];
                FileInputStream in = new FileInputStream(f);
                in.read(data);
                in.close();
                EncodeDataContainer ba = new EncodeDataContainer(data);
                int size = EncodeUtils.decodeInt32(ba);
                for (int i = 0; i < size; i++) {
                    long a = EncodeUtils.decodeInt64(ba);
                    long b = EncodeUtils.decodeInt64(ba);
                    MD5Identifier md5 = MD5Identifier.createX(a, b);
                    locations.put(md5, DataLocation.decode(ba, 0));
                }
                size = EncodeUtils.decodeInt32(ba);
                for (int i = 0; i < size; i++) {
                    locationsByIndex.add(DataLocation.decode(ba, 0));
                }
                size = EncodeUtils.decodeInt32(ba);
                for (int i = 0; i < size; i++) {
                    int key = EncodeUtils.decodeInt32(ba);
                    int listSize = EncodeUtils.decodeInt32(ba);
                    List<Integer> list = new ArrayList<Integer>(listSize);
                    for (int j = 0; j < listSize; j++) {
                        int recIndex = EncodeUtils.decodeInt32(ba);
                        list.add(recIndex);
                    }
                    this.parentIndexToRecordIndex.put(key, list);
                }
                size = EncodeUtils.decodeInt32(ba);
                for(int i=0;i<size;i++){
                    int parIndex = EncodeUtils.decodeInt32(ba);
                    this.indexToParentIndex.add(parIndex);
                }
            } catch (Exception err) {
                err.printStackTrace();
            }
        }
    }

    public DataPersister(String location, Class<?> _type) {
        this.dataFileLocation = location + "/" + _type.getName() + ".dat";
        this.dataFileDefLocation = location + "/" + _type.getName() + ".loc";
        file = new File(this.dataFileLocation);
        this.type = _type;
    }

    public Class<?> getType() {
        return this.type;
    }

    public int getObjectCount() {
        return this.locations.size();
    }

    public boolean contain(MD5Identifier x) {
        return this.locations.containsKey(x);
    }

    public int writeData(int parentRecordIndex,int recordIndex, MD5Identifier x, byte data[]) {
        synchronized (this.locations) {
            if (randomAccessFile == null) {
                try {
                    randomAccessFile = new RandomAccessFile(file, "rw");
                    randomAccessFile.seek(file.length());
                } catch (Exception err) {
                    err.printStackTrace();
                }
            }

            DataLocation l = null;
            if(recordIndex!=-1){
                l = locationsByIndex.get(recordIndex);
            }else
            if(x!=null){
                l = locations.get(x);
            }
            if (l != null) {
                if (l.getLength() < data.length+8) {
                    byte wipe[] = new String("DDDDD").getBytes();
                    try {
                        randomAccessFile.seek(l.getStartPosition()+8);
                        randomAccessFile.write(wipe);
                        return writeNew(data, x, parentRecordIndex);
                    } catch (Exception err) {
                        err.printStackTrace();
                    }
                } else {
                    try {
                        randomAccessFile.seek(l.getStartPosition()+4);
                        byte[] parentIndex = new byte[4];
                        EncodeUtils.encodeInt32(parentRecordIndex,parentIndex,0);
                        randomAccessFile.write(parentIndex);
                        randomAccessFile.write(data);
                        return recordIndex;
                    } catch (Exception err) {
                        err.printStackTrace();
                    }
                }
            } else {
                return writeNew(data, x, parentRecordIndex);
            }
        }
        return locationsByIndex.size() - 1;
    }

    private int writeNew(byte data[],MD5Identifier x,int parentRecordIndex){
        DataLocation newL = new DataLocation((int) fileSize, data.length+8,this.locationsByIndex.size());
        if (x != null) {
            locations.put(x, newL);
        }
        if (parentRecordIndex != -1) {
            List<Integer> list = this.parentIndexToRecordIndex.get(parentRecordIndex);
            if (list == null) {
                list = new ArrayList<Integer>(5);
                this.parentIndexToRecordIndex.put(parentRecordIndex,list);
            }
            list.add(this.locationsByIndex.size());
        }

        this.locationsByIndex.add(newL);
        this.indexToParentIndex.add(parentRecordIndex);
        try {
            if (lastLocation != fileSize) {
                randomAccessFile.seek(fileSize);
            }
            byte[] dataSize = new byte[4];
            byte[] parentIndex = new byte[4];
            EncodeUtils.encodeInt32(data.length,dataSize,0);
            EncodeUtils.encodeInt32(parentRecordIndex,parentIndex,0);
            randomAccessFile.write(dataSize);
            randomAccessFile.write(parentIndex);
            randomAccessFile.write(data);
        } catch (Exception err) {
            err.printStackTrace();
        }
        fileSize += data.length + 8;
        lastLocation = fileSize;
        return locationsByIndex.size()-1;
    }

    public byte[] readAll() throws Exception {
        File f = new File(file.getAbsolutePath());
        FileInputStream in = new FileInputStream(f);
        byte data[] = new byte[(int) f.length()];
        in.read(data);
        in.close();
        return data;
    }

    public byte[][] delete(MD5Identifier x) {
        synchronized (locations) {
            List<DataLocation> locationList = null;
            if (locationList != null) {
                try {
                    if (randomAccessFile == null) {
                        randomAccessFile = new RandomAccessFile(file, "rw");
                    }
                    byte data[][] = new byte[locationList.size()][];
                    int index = 0;
                    for (DataLocation l : locationList) {
                        randomAccessFile.seek(l.getStartPosition());
                        data[index] = new byte[l.getLength()];
                        randomAccessFile.read(data[index]);
                        randomAccessFile.seek(l.getStartPosition());
                        randomAccessFile.write(new String("DDDDD").getBytes());
                        index++;
                    }
                    return data;
                } catch (Exception err) {
                    err.printStackTrace();
                }
            }
        }
        return null;
    }

    public byte[] read(MD5Identifier md5ID) {
        synchronized (locations) {
            DataLocation loc = locations.get(md5ID);
            if (loc != null) {
                try {
                    if (randomAccessFile == null) {
                        randomAccessFile = new RandomAccessFile(file, "rw");
                    }
                    randomAccessFile.seek(loc.getStartPosition()+8);
                    byte data[] = new byte[loc.getLength()-8];
                    randomAccessFile.read(data);
                    lastLocation = loc.getStartPosition() + loc.getLength();
                    return data;
                } catch (Exception err) {
                    err.printStackTrace();
                }
            }
        }
        return null;
    }

    public byte[] read(int index) {
        if (index >= locationsByIndex.size())
            return null;

        synchronized (locations) {
            DataLocation l = locationsByIndex.get(index);
            if (l != null) {
                try {
                    if (randomAccessFile == null) {
                        randomAccessFile = new RandomAccessFile(file, "rw");
                    }
                    randomAccessFile.seek(l.getStartPosition()+8);
                    byte data[] = new byte[l.getLength()-8];
                    randomAccessFile.read(data);
                    lastLocation = l.getStartPosition() + l.getLength();
                    return data;
                } catch (Exception err) {
                    err.printStackTrace();
                }
            }
        }
        return null;
    }

    public Integer getParentIndex(int index){
        return this.indexToParentIndex.get(index);
    }

    public Integer getIndexByMD5(MD5Identifier md5ID){
        return this.locations.get(md5ID).getRecordIndex();
    }

    public List<Integer> getRecordIndexes(int parentRecordIndex) {
        return this.parentIndexToRecordIndex.get(parentRecordIndex);
    }

    public byte[][] read(List<Integer> records) {
        synchronized (locations) {
            if (records != null) {
                try {
                    if (randomAccessFile == null) {
                        randomAccessFile = new RandomAccessFile(file, "rw");
                    }
                    int index = 0;
                    byte data[][] = new byte[records.size()][];
                    for (int recIndex : records) {
                        DataLocation l = locationsByIndex.get(recIndex);
                        randomAccessFile.seek(l.getStartPosition()+8);
                        data[index] = new byte[l.getLength()-8];
                        randomAccessFile.read(data[index]);
                        lastLocation = l.getStartPosition() + l.getLength();
                        index++;
                    }
                    return data;
                } catch (Exception err) {
                    err.printStackTrace();
                }
            }
        }
        return null;
    }

    public void close() {
        synchronized (locations) {
            try {
                randomAccessFile.close();
                randomAccessFile = null;
            } catch (IOException err) {
                err.printStackTrace();
            }
            locations.clear();
        }
    }
}