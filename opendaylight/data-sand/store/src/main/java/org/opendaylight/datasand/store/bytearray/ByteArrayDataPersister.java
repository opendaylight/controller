package org.opendaylight.datasand.store.bytearray;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendaylight.datasand.codec.EncodeDataContainer;
import org.opendaylight.datasand.codec.MD5Identifier;
import org.opendaylight.datasand.codec.TypeDescriptorsContainer;
import org.opendaylight.datasand.codec.bytearray.ByteArrayEncodeDataContainer;
import org.opendaylight.datasand.codec.bytearray.ByteEncoder;
import org.opendaylight.datasand.store.DataPersister;
import org.opendaylight.datasand.store.Shard;

public class ByteArrayDataPersister extends DataPersister{

    private File file = null;
    private RandomAccessFile randomAccessFile = null;
    private Map<MD5Identifier, ByteArrayDataLocation> locations = new HashMap<MD5Identifier, ByteArrayDataLocation>();
    private Map<Integer, int[]> parentIndexToRecordIndex = new HashMap<Integer, int[]>();
    private List<ByteArrayDataLocation> locationsByIndex = new ArrayList<ByteArrayDataLocation>();
    private String dataFileLocation = null;
    private String dataFileDefLocation = null;
    private long fileSize = 0;
    private long lastLocation = 0;
    private int changeNumber = 0;

    private static final int HEADER_SIZE = 12;

    private ByteArrayEncodeDataContainer writeBuffer = null;
    private static final long MAX_WRITE_BUFFER_SIZE = 1024*1024*10;
    public static boolean USE_WRITE_BUFFERING = true;

    private static final int READ_BUFFER_SIZE = 1024*1024*10;
    private byte[] readBuffer = new byte[READ_BUFFER_SIZE];
    public static boolean USE_READ_BUFFERING = true;
    public long readBufferLocation = 0;
    public boolean isReadBufferInitialize = false;

    public void save() {
        try {
            ByteArrayEncodeDataContainer ba = new ByteArrayEncodeDataContainer(1024,container);
            ba.getEncoder().encodeInt32(changeNumber, ba);
            ba.getEncoder().encodeInt32(locations.size(), ba);
            for (Map.Entry<MD5Identifier, ByteArrayDataLocation> entry : locations.entrySet()) {
                ba.getEncoder().encodeInt64(entry.getKey().getA(), ba);
                ba.getEncoder().encodeInt64(entry.getKey().getB(), ba);
                entry.getValue().encode(ba);
            }
            ba.getEncoder().encodeInt32(locationsByIndex.size(), ba);
            for (ByteArrayDataLocation loc:locationsByIndex) {
                loc.encode(ba);
            }

            ba.getEncoder().encodeInt32(parentIndexToRecordIndex.size(), ba);
            for (Map.Entry<Integer, int[]> entry : parentIndexToRecordIndex.entrySet()) {
                ba.getEncoder().encodeInt32(entry.getKey(), ba);
                ba.getEncoder().encodeInt32(entry.getValue().length, ba);
                for (int index : entry.getValue()) {
                    ba.getEncoder().encodeInt32(index, ba);
                }
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
            fileSize = this.file.length();
            try {
                byte data[] = new byte[(int) f.length()];
                FileInputStream in = new FileInputStream(f);
                in.read(data);
                in.close();
                ByteArrayEncodeDataContainer ba = new ByteArrayEncodeDataContainer(data,container);
                changeNumber = ba.getEncoder().decodeInt32(ba);
                int size = ba.getEncoder().decodeInt32(ba);
                for (int i = 0; i < size; i++) {
                    long a = ba.getEncoder().decodeInt64(ba);
                    long b = ba.getEncoder().decodeInt64(ba);
                    MD5Identifier md5 = MD5Identifier.createX(a, b);
                    locations.put(md5, ByteArrayDataLocation.decode(ba, 0));
                }
                size = ba.getEncoder().decodeInt32(ba);
                for (int i = 0; i < size; i++) {
                    locationsByIndex.add(ByteArrayDataLocation.decode(ba, 0));
                }
                size = ba.getEncoder().decodeInt32(ba);
                for (int i = 0; i < size; i++) {
                    int key = ba.getEncoder().decodeInt32(ba);
                    int listSize = ba.getEncoder().decodeInt32(ba);
                    int arr[] = new int[listSize];
                    for (int j = 0; j < listSize; j++) {
                        int recIndex = ba.getEncoder().decodeInt32(ba);
                        arr[j]=recIndex;
                    }
                    this.parentIndexToRecordIndex.put(key, arr);
                }
            } catch (Exception err) {
                err.printStackTrace();
            }
        }
    }

    public ByteArrayDataPersister(Shard _shard, Class<?> _type,TypeDescriptorsContainer _container) {
        super(_shard,_type,_container);
        this.dataFileLocation = this.shard.getBlockLocation() + "/" + _type.getName() + ".dat";
        this.dataFileDefLocation = this.shard.getBlockLocation() + "/" + _type.getName() + ".loc";
        this.writeBuffer = new ByteArrayEncodeDataContainer(1024,this.container);
        file = new File(this.dataFileLocation);
    }

    public int getObjectCount() {
        return this.locations.size();
    }

    public boolean contain(Object x) {
        return this.locations.containsKey(x);
    }

    private void wipe(ByteArrayDataLocation l) throws IOException{
        byte wipe[] = new String("Deleted!").getBytes();
        randomAccessFile.seek(l.getStartPosition()+HEADER_SIZE);
        randomAccessFile.write(wipe);
        lastLocation = l.getStartPosition()+HEADER_SIZE;
    }

    public boolean isDeleted(Object data){
        byte b[] = (byte[])data;
        if(b.length>=8 && b[0]=='D' && b[1]=='e' && b[2]=='l' && b[3]=='e' && b[4]=='t' && b[5]=='e' && b[6]=='d' && b[7]=='!'){
            return true;
        }
        return false;
    }

    public int write(int parentRecordIndex,int recordIndex,EncodeDataContainer ba) {
        byte[] data = ((ByteArrayEncodeDataContainer)ba).getData();
        synchronized (this.locations) {
            if (randomAccessFile == null) {
                try {
                    randomAccessFile = new RandomAccessFile(file, "rw");
                    randomAccessFile.seek(file.length());
                } catch (Exception err) {
                    err.printStackTrace();
                }
            }

            ByteArrayDataLocation l = null;
            if(recordIndex!=-1){
                l = locationsByIndex.get(recordIndex);
            }else
            if(ba.getMD5ID()!=null){
                l = locations.get(ba.getMD5ID());
                if(l!=null){
                    recordIndex = l.getRecordIndex();
                    parentRecordIndex = l.getParentIndex();
                }
            }
            if (l != null) {
                flushWriteBuffer();
                if (l.getLength() < data.length+HEADER_SIZE) {
                    try {
                        wipe(l);
                        return updateNew(data,l);
                    } catch (Exception err) {
                        err.printStackTrace();
                    }
                } else {
                    try {
                        randomAccessFile.seek(l.getStartPosition());
                        randomAccessFile.write(getHeader(data.length, parentRecordIndex));
                        randomAccessFile.write(data);
                        lastLocation = l.getStartPosition()+HEADER_SIZE+data.length;
                        return recordIndex;
                    } catch (Exception err) {
                        err.printStackTrace();
                    }
                }
            } else {
                return writeNew(data, ba.getMD5ID(), parentRecordIndex);
            }
        }
        return locationsByIndex.size() - 1;
    }

    private void readToBuffer(ByteArrayDataLocation l) throws IOException{
        flushWriteBuffer();
        int bufferStartPos = (int)(l.getStartPosition()-readBufferLocation)+HEADER_SIZE;
        if(USE_READ_BUFFERING && (!isReadBufferInitialize || bufferStartPos<0 || bufferStartPos+l.getLength()>=READ_BUFFER_SIZE)){
            isReadBufferInitialize = true;
            randomAccessFile.seek(l.getStartPosition());
            readBufferLocation = l.getStartPosition();
            if(fileSize>l.getStartPosition()+READ_BUFFER_SIZE){
                randomAccessFile.read(readBuffer);
            }else{
                byte data[] = new byte[(int)(fileSize-l.getStartPosition())];
                randomAccessFile.read(data);
                System.arraycopy(data, 0, readBuffer, 0,data.length);
            }
        }
    }

    private void writeToBuffer(byte data[],int parentRecordIndex) throws IOException{
        writeBuffer.adjustSize(HEADER_SIZE+data.length);
        System.arraycopy(getHeader(data.length,parentRecordIndex),0,writeBuffer.getBytes(),writeBuffer.getLocation(),HEADER_SIZE);
        writeBuffer.advance(HEADER_SIZE);
        System.arraycopy(data,0, writeBuffer.getBytes(), writeBuffer.getLocation(), data.length);
        writeBuffer.advance(data.length);
    }

    private void flushWriteBuffer(){
        if(!USE_WRITE_BUFFERING) return;
        try{
            if(writeBuffer.getLocation()>0){
                if(readBufferLocation+readBuffer.length>=file.length()){
                    isReadBufferInitialize = false;
                }
                byte data[] = writeBuffer.getData();
                if(file.length()!=fileSize || lastLocation!=fileSize){
                    randomAccessFile.seek(file.length());
                }
                randomAccessFile.write(data);
                writeBuffer.resetLocation();
                lastLocation = file.length();
            }
        }catch(IOException err){
            err.printStackTrace();
        }
    }

    private int[] addIndexToArray(int index,int[] currentArray){
        if(currentArray==null){
            return new int[]{index};
        }else{
            int[] temp = new int[currentArray.length+1];
            System.arraycopy(currentArray, 0, temp, 0, currentArray.length);
            temp[temp.length-1] = index;
            return temp;
        }
    }

    private int writeNew(byte data[],MD5Identifier x,int parentRecordIndex){
        ByteArrayDataLocation newL = new ByteArrayDataLocation((int) fileSize, data.length+HEADER_SIZE,this.locationsByIndex.size(),parentRecordIndex);
        if (x != null) {
            locations.put(x, newL);
        }

        if (parentRecordIndex != -1) {
            int[] arr = addIndexToArray(this.locationsByIndex.size(), this.parentIndexToRecordIndex.get(parentRecordIndex));
            this.parentIndexToRecordIndex.put(parentRecordIndex, arr);
        }

        this.locationsByIndex.add(newL);
        try {
            if (lastLocation != fileSize && !USE_WRITE_BUFFERING) {
                randomAccessFile.seek(fileSize);
            }
            if(!USE_WRITE_BUFFERING){
                randomAccessFile.write(getHeader(data.length,parentRecordIndex));
                randomAccessFile.write(data);
            }else{
                writeToBuffer(data, parentRecordIndex);
            }
        } catch (Exception err) {
            err.printStackTrace();
        }
        fileSize += (data.length + HEADER_SIZE);
        lastLocation = fileSize;
        if(USE_WRITE_BUFFERING && writeBuffer.getLocation()>=MAX_WRITE_BUFFER_SIZE){
            flushWriteBuffer();
        }
        return locationsByIndex.size()-1;
    }

    private int updateNew(byte data[],ByteArrayDataLocation dl){
        dl.updateLocationInfo((int)fileSize,HEADER_SIZE+data.length);
        try {
            if (lastLocation != fileSize) {
                randomAccessFile.seek(fileSize);
            }
            randomAccessFile.write(getHeader(data.length,dl.getParentIndex()));
            randomAccessFile.write(data);
        } catch (Exception err) {
            err.printStackTrace();
        }
        fileSize += data.length + HEADER_SIZE;
        lastLocation = fileSize;
        return dl.getRecordIndex();
    }

    private byte[] getHeader(int dataSize,int parentRecordIndex){
        byte header[] = new byte[12];
        ByteEncoder.encodeInt32(changeNumber, header,0);
        changeNumber++;
        ByteEncoder.encodeInt32(dataSize,header,4);
        ByteEncoder.encodeInt32(parentRecordIndex,header,8);
        return header;
    }

    public byte[] readAll() throws Exception {
        File f = new File(file.getAbsolutePath());
        FileInputStream in = new FileInputStream(f);
        byte data[] = new byte[(int) f.length()];
        in.read(data);
        in.close();
        return data;
    }

    public byte[] delete(Object key) {
        synchronized (locations) {
            ByteArrayDataLocation loc = locations.get(key);
            return doDelete(loc);
        }
    }

    public byte[] read(Object key) {
        synchronized (locations) {
            ByteArrayDataLocation loc = locations.get(key);
            return doRead(loc);
        }
    }

    private byte[] doDelete(ByteArrayDataLocation l){
        if (l != null) {
            try {
                if (randomAccessFile == null) {
                    randomAccessFile = new RandomAccessFile(file, "rw");
                }
                flushWriteBuffer();
                isReadBufferInitialize = false;
                randomAccessFile.seek(l.getStartPosition()+HEADER_SIZE);
                byte data[] = new byte[l.getLength()-HEADER_SIZE];
                randomAccessFile.read(data);
                wipe(l);
                return data;
            } catch (Exception err) {
                err.printStackTrace();
            }
        }
        return null;
    }

    private byte[] doRead(ByteArrayDataLocation l){
        if (l != null) {
            try {
                if (randomAccessFile == null) {
                    randomAccessFile = new RandomAccessFile(file, "rw");
                }
                readToBuffer(l);
                byte data[] = new byte[l.getLength()-HEADER_SIZE];
                if(!USE_READ_BUFFERING){
                    randomAccessFile.seek(l.getStartPosition()+HEADER_SIZE);
                    randomAccessFile.read(data);
                }else{
                    int bufferStartPos = (int)(l.getStartPosition()-readBufferLocation)+HEADER_SIZE;
                    try{
                        System.arraycopy(readBuffer,bufferStartPos, data, 0, data.length);
                    }catch(Exception err){
                        err.printStackTrace();
                    }
                }
                lastLocation = l.getStartPosition() + l.getLength();
                return data;
            } catch (Exception err) {
                err.printStackTrace();
            }
        }
        return null;
    }

    public byte[] read(int index) {
        if (index >= locationsByIndex.size())
            return null;

        synchronized (locations) {
            ByteArrayDataLocation l = locationsByIndex.get(index);
            return doRead(l);
        }
    }

    public byte[] delete(int index) {
        if (index >= locationsByIndex.size())
            return null;
        synchronized (locations) {
            ByteArrayDataLocation l = locationsByIndex.get(index);
            return doDelete(l);
        }
    }

    public Integer getParentIndex(int index){
        return this.locationsByIndex.get(index).getParentIndex();
    }

    public Integer getIndexByKey(Object key){
        return this.locations.get(key).getRecordIndex();
    }

    public Integer getParentIndexByKey(Object key){
        return this.locations.get(key).getParentIndex();
    }

    public int[] getRecordIndexesByParentIndex(int parentRecordIndex) {
        return this.parentIndexToRecordIndex.get(parentRecordIndex);
    }

    public byte[][] read(int recordIndexs[]) {
        synchronized (locations) {
            if (recordIndexs != null) {
                try {
                    if (randomAccessFile == null) {
                        randomAccessFile = new RandomAccessFile(file, "rw");
                    }
                    int index = 0;
                    byte data[][] = new byte[recordIndexs.length][];
                    for (int recIndex : recordIndexs) {
                        ByteArrayDataLocation l = locationsByIndex.get(recIndex);
                        data[index] = doRead(l);
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

    public byte[][] delete(int recordIndexs[]) {
        synchronized (locations) {
            if (recordIndexs != null) {
                try {
                    if (randomAccessFile == null) {
                        randomAccessFile = new RandomAccessFile(file, "rw");
                    }
                    int index = 0;
                    byte data[][] = new byte[recordIndexs.length][];
                    for (int recIndex : recordIndexs) {
                        ByteArrayDataLocation l = locationsByIndex.get(recIndex);
                        data[index] = doDelete(l);
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
                flushWriteBuffer();
                if(randomAccessFile!=null)
                    randomAccessFile.close();
                randomAccessFile = null;
            } catch (IOException err) {
                err.printStackTrace();
            }
            locations.clear();
        }
    }
}