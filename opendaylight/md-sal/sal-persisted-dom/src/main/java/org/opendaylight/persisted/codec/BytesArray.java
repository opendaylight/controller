package org.opendaylight.persisted.codec;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.opendaylight.persisted.MD5ID;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.persistedtest.rev141020.sal.persisted.dom.test.ListWithKey;

/**
 * @author root
 *
 */
public class BytesArray {
    private int location = 0;
    private byte[] bytes = null;
    public int enlargeTimes = 0;
    private Object codingOption = null;
    private LeftOvers leftOvers = null;
    private Map<Integer, List<BytesArray>> subElementsData = new HashMap<Integer, List<BytesArray>>();
    private MD5ID md5ID = null;

    public BytesArray(byte[] data) {
        this(data, null);
    }

    public BytesArray(byte[] data, Object _codingOption) {
        this.bytes = data;
        this.codingOption = _codingOption;
    }

    public BytesArray(int size) {
        this(size, null);
    }

    public BytesArray(int size, Object _codingOption) {
        this.bytes = new byte[size];
        this.codingOption = _codingOption;
    }

    public void addSubElementData(int classType, BytesArray subBytesArray,Object _element) {
        if(_element instanceof ListWithKey){
            int i=0;
        }
        List<BytesArray> subElementList = (List<BytesArray>) subElementsData.get(classType);
        if (subElementList == null) {
            subElementList = new LinkedList<BytesArray>();
            subElementsData.put(classType, subElementList);
        }
        if(_element!=null){
            subBytesArray.setMD5ID(MDSALTableRepository.getInstance().getCTypeByObject(_element).getMD5IDForObject(_element));
        }
        subElementList.add(subBytesArray);
    }

    public Map<Integer, List<BytesArray>> getSubElementsData() {
        return this.subElementsData;
    }

    public byte[] getBytes() {
        return this.bytes;
    }

    public int getLocation() {
        return this.location;
    }

    public void advance(int size) {
        this.location += size;
    }

    public byte[] getData() {
        byte data[] = new byte[location];
        System.arraycopy(bytes, 0, data, 0, location);
        return data;
    }

    public void adjustSize(int goingToAdd) {
        if (location + goingToAdd < bytes.length)
            return;
        enlargeTimes++;
        int newSize = (int) (bytes.length * 1.3);
        if (newSize < location + goingToAdd) {
            newSize = location + goingToAdd;
        }
        byte temp[] = new byte[newSize];
        System.arraycopy(bytes, 0, temp, 0, location);
        bytes = temp;
    }

    public void resetLocation() {
        this.location = 0;
        for (List<BytesArray> list : this.subElementsData.values()) {
            for (BytesArray subBA : list) {
                subBA.resetLocation();
            }
        }
    }

    public Object getCodingOption() {
        return codingOption;
    }

    public LeftOvers getLeftOvers() {
        return leftOvers;
    }

    public void addLeftOver(Object o) {
        if (this.leftOvers == null) {
            leftOvers = new LeftOvers();
        }
        leftOvers.addLeftOver(o);
    }

    public byte[] getMarked(int mark) {
        byte[] result = new byte[location - mark];
        System.arraycopy(bytes, mark, result, 0, result.length);
        return result;
    }

    public void setMD5ID(MD5ID _md5ID){
        this.md5ID = _md5ID;
    }

    public MD5ID getMD5ID(){
        return this.md5ID;
    }

    public static byte[] toSingleByteArray(BytesArray source){
        BytesArray enc = new BytesArray(1024);
        byte data[] = source.getData();
        MDSALEncoder.encodeByteArray(data, enc);
        MDSALEncoder.encodeInt16(source.subElementsData.size(), enc);
        for(Map.Entry<Integer,List<BytesArray>> subBAs:source.subElementsData.entrySet()){
            MDSALEncoder.encodeInt16(subBAs.getKey(), enc);
            MDSALEncoder.encodeInt32(subBAs.getValue().size(), enc);
            for(BytesArray subBA:subBAs.getValue()){
                MDSALEncoder.encodeByteArray(toSingleByteArray(subBA),enc);
            }
        }
        return enc.getData();
    }

    public static BytesArray fromSingleByteArray(BytesArray source){
        BytesArray result = new BytesArray(1024);
        result.bytes = MDSALEncoder.decodeByteArray(source);
        int subMapSize = MDSALEncoder.decodeInt16(source);
        for(int i=0;i<subMapSize;i++){
            int code = MDSALEncoder.decodeInt16(source);
            int size = MDSALEncoder.decodeInt32(source);
            for(int j=0;j<size;j++){
                byte subData[] = MDSALEncoder.decodeByteArray(source);
                BytesArray subBASource = new BytesArray(subData);
                BytesArray subBA = fromSingleByteArray(subBASource);
                result.addSubElementData(code, subBA,null);
            }
        }
        return result;
    }
}
