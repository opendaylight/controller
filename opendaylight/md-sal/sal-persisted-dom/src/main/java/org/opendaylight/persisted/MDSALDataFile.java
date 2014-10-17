package org.opendaylight.persisted;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.opendaylight.persisted.codec.BytesArray;
import org.opendaylight.persisted.codec.MDSALEncoder;

public class MDSALDataFile {
    private File file = null;
    private RandomAccessFile randomAccessFile = null;
    private Map<MD5ID, List<MDSALDataLocation>> locations = new HashMap<MD5ID, List<MDSALDataLocation>>();
    private Map<Integer, List<Integer>> parentIndexToRecordIndex = new HashMap<Integer, List<Integer>>();
    private List<Integer> indexToParentIndex = new ArrayList<Integer>();
    private List<MDSALDataLocation> locationsByIndex = new ArrayList<MDSALDataLocation>();
    private String dataFileLocation = null;
    private String dataFileDefLocation = null;
    // private static final long MAX_FILE_SIZE = 1024*1024*128;
    private Class<?> type = null;
    private static byte OPEN_CHAR = "{".getBytes()[0];
    private static byte CLOSE_CHAR = "}".getBytes()[0];
    private long fileSize = 0;
    private long lastLocation = 0;

    public void save() {
        try {
            BytesArray ba = new BytesArray(1024);
            MDSALEncoder.encodeInt32(locations.size(), ba);
            for (Map.Entry<MD5ID, List<MDSALDataLocation>> entry : locations
                    .entrySet()) {
                MDSALEncoder.encodeInt64(entry.getKey().getA(), ba);
                MDSALEncoder.encodeInt64(entry.getKey().getB(), ba);
                MDSALEncoder.encodeInt32(entry.getValue().size(), ba);
                for (MDSALDataLocation loc : entry.getValue()) {
                    MDSALEncoder.encodeInt32(loc.getStart(), ba);
                    MDSALEncoder.encodeInt32(loc.getSize(), ba);
                }
            }
            MDSALEncoder.encodeInt32(locationsByIndex.size(), ba);
            for (int j = 0; j < locationsByIndex.size(); j++) {
                MDSALEncoder
                        .encodeInt32(locationsByIndex.get(j).getStart(), ba);
                MDSALEncoder.encodeInt32(locationsByIndex.get(j).getSize(), ba);
            }
            MDSALEncoder.encodeInt32(parentIndexToRecordIndex.size(), ba);
            for (Map.Entry<Integer, List<Integer>> entry : parentIndexToRecordIndex
                    .entrySet()) {
                MDSALEncoder.encodeInt32(entry.getKey(), ba);
                MDSALEncoder.encodeInt32(entry.getValue().size(), ba);
                for (int index : entry.getValue()) {
                    MDSALEncoder.encodeInt32(index, ba);
                }
            }
            MDSALEncoder.encodeInt32(indexToParentIndex.size(), ba);
            for(Integer i:indexToParentIndex){
                MDSALEncoder.encodeInt32(i, ba);
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
                BytesArray ba = new BytesArray(data);
                int size = MDSALEncoder.decodeInt32(ba);
                for (int i = 0; i < size; i++) {
                    long a = MDSALEncoder.decodeInt64(ba);
                    long b = MDSALEncoder.decodeInt64(ba);
                    MD5ID md5 = MD5ID.createX(a, b);
                    int listSize = MDSALEncoder.decodeInt32(ba);
                    List<MDSALDataLocation> list = new ArrayList<MDSALDataLocation>(
                            listSize);
                    for (int j = 0; j < listSize; j++) {
                        int c = MDSALEncoder.decodeInt32(ba);
                        int d = MDSALEncoder.decodeInt32(ba);
                        MDSALDataLocation loc = new MDSALDataLocation(c, d);
                        list.add(loc);
                    }
                    locations.put(md5, list);
                }
                size = MDSALEncoder.decodeInt32(ba);
                for (int i = 0; i < size; i++) {
                    int c = MDSALEncoder.decodeInt32(ba);
                    int d = MDSALEncoder.decodeInt32(ba);
                    MDSALDataLocation loc = new MDSALDataLocation(c, d);
                    locationsByIndex.add(loc);
                }
                size = MDSALEncoder.decodeInt32(ba);
                for (int i = 0; i < size; i++) {
                    int key = MDSALEncoder.decodeInt32(ba);
                    int listSize = MDSALEncoder.decodeInt32(ba);
                    List<Integer> list = new ArrayList<Integer>(listSize);
                    for (int j = 0; j < listSize; j++) {
                        int recIndex = MDSALEncoder.decodeInt32(ba);
                        list.add(recIndex);
                    }
                    this.parentIndexToRecordIndex.put(key, list);
                }
                size = MDSALEncoder.decodeInt32(ba);
                for(int i=0;i<size;i++){
                    int parIndex = MDSALEncoder.decodeInt32(ba);
                    this.indexToParentIndex.add(parIndex);
                }
            } catch (Exception err) {
                err.printStackTrace();
            }
        }
    }

    public MDSALDataFile(String location, Class<?> _type) {
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

    public boolean contain(MD5ID x) {
        return this.locations.containsKey(x);
    }

    public int writeData(int parentRecordIndex, MD5ID x, byte data[]) {
        synchronized (this.locations) {
            if (randomAccessFile == null) {
                try {
                    randomAccessFile = new RandomAccessFile(file, "rw");
                    if (x == null)
                        randomAccessFile.seek(file.length());
                } catch (Exception err) {
                    err.printStackTrace();
                }
            }

            List<MDSALDataLocation> locationList = null;
            if (x != null) {
                locationList = this.locations.get(x);
                if (locationList == null) {
                    locationList = new LinkedList<MDSALDataLocation>();
                    this.locations.put(x, locationList);
                }
            }
            MDSALDataLocation l = null;

            if (l != null) { // no update for now
                if (l.getSize() < data.length) {
                    byte wipe[] = new String("DDDDD").getBytes();
                    try {
                        randomAccessFile.seek(l.getStart());
                        randomAccessFile.write(wipe);
                        MDSALDataLocation newL = new MDSALDataLocation(
                                (int) file.length() + 1, data.length);
                        // this.locations.put(x, newL);
                        randomAccessFile.seek(file.length());
                        randomAccessFile.write(OPEN_CHAR);
                        randomAccessFile.write(data);
                        randomAccessFile.write(CLOSE_CHAR);
                    } catch (Exception err) {
                        err.printStackTrace();
                    }
                } else {
                    try {
                        randomAccessFile.seek(l.getStart());
                        randomAccessFile.write(data);
                    } catch (Exception err) {
                        err.printStackTrace();
                    }
                }
            } else {
                /*
                 * if(fileSize+data.length>MAX_FILE_SIZE){ return false; }
                 */
                MDSALDataLocation newL = new MDSALDataLocation(
                        (int) fileSize + 1, data.length);
                if (locationList != null) {
                    locationList.add(newL);
                }
                if (parentRecordIndex != -1) {
                    List<Integer> list = this.parentIndexToRecordIndex
                            .get(parentRecordIndex);
                    if (list == null) {
                        list = new ArrayList<Integer>();
                        this.parentIndexToRecordIndex.put(parentRecordIndex,
                                list);
                    }
                    list.add(this.locationsByIndex.size());
                }
                this.locationsByIndex.add(newL);
                this.indexToParentIndex.add(parentRecordIndex);
                try {
                    if (lastLocation != fileSize) {
                        randomAccessFile.seek(fileSize);
                    }
                    randomAccessFile.write(OPEN_CHAR);
                    randomAccessFile.write(data);
                    randomAccessFile.write(CLOSE_CHAR);
                } catch (Exception err) {
                    err.printStackTrace();
                }
                fileSize += data.length + 2;
                lastLocation = fileSize;
            }
        }
        return locationsByIndex.size() - 1;
    }

    public byte[] readAll() throws Exception {
        File f = new File(file.getAbsolutePath());
        FileInputStream in = new FileInputStream(f);
        byte data[] = new byte[(int) f.length()];
        in.read(data);
        in.close();
        return data;
    }

    public byte[][] delete(MD5ID x) {
        synchronized (locations) {
            List<MDSALDataLocation> locationList = locations.remove(x);
            if (locationList != null) {
                try {
                    if (randomAccessFile == null) {
                        randomAccessFile = new RandomAccessFile(file, "rw");
                    }
                    byte data[][] = new byte[locationList.size()][];
                    int index = 0;
                    for (MDSALDataLocation l : locationList) {
                        randomAccessFile.seek(l.getStart());
                        data[index] = new byte[l.getSize()];
                        randomAccessFile.read(data[index]);
                        randomAccessFile.seek(l.getStart());
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

    public byte[] read(int index) {
        if (index >= locationsByIndex.size())
            return null;

        synchronized (locations) {
            MDSALDataLocation l = locationsByIndex.get(index);
            if (l != null) {
                try {
                    if (randomAccessFile == null) {
                        randomAccessFile = new RandomAccessFile(file, "rw");
                    }
                    randomAccessFile.seek(l.getStart());
                    byte data[] = new byte[l.getSize()];
                    randomAccessFile.read(data);
                    lastLocation = l.getStart() + l.getSize();
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
                        MDSALDataLocation l = locationsByIndex.get(recIndex);
                        randomAccessFile.seek(l.getStart());
                        data[index] = new byte[l.getSize()];
                        randomAccessFile.read(data[index]);
                        lastLocation = l.getStart() + l.getSize();
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

    public byte[][] read(MD5ID x) {
        synchronized (locations) {
            List<MDSALDataLocation> locationList = null;
            if (x != null)
                locationList = locations.get(x);

            if (locationList != null) {
                try {
                    if (randomAccessFile == null) {
                        randomAccessFile = new RandomAccessFile(file, "rw");
                    }
                    int index = 0;
                    byte data[][] = new byte[locationList.size()][];
                    for (MDSALDataLocation l : locationList) {
                        randomAccessFile.seek(l.getStart());
                        data[index] = new byte[l.getSize()];
                        randomAccessFile.read(data[index]);
                        lastLocation = l.getStart() + l.getSize();
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