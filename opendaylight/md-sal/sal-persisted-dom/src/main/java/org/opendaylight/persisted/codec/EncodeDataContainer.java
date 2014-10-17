package org.opendaylight.persisted.codec;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.opendaylight.persisted.MD5Identifier;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.persistedtest.rev141020.sal.persisted.dom.test.ListWithKey;

/**
 * @author root
 *
 */
public class EncodeDataContainer {
    private int location = 0;
    private byte[] bytes = null;
    public int enlargeTimes = 0;
    private Object codingOption = null;
    private LeftOvers leftOvers = null;
    private Map<Integer, List<EncodeDataContainer>> subElementsData = new HashMap<Integer, List<EncodeDataContainer>>();
    private MD5Identifier md5ID = null;
    private TypeDescriptorsContainer container = null;

    public EncodeDataContainer(byte[] data,TypeDescriptorsContainer _container) {
        this(data, null,_container);
    }

    public EncodeDataContainer(byte[] data, Object _codingOption,TypeDescriptorsContainer _container) {
        this.bytes = data;
        this.codingOption = _codingOption;
        this.container = _container;
    }

    public EncodeDataContainer(int size,TypeDescriptorsContainer _container) {
        this(size, null,_container);
    }

    public EncodeDataContainer(int size, Object _codingOption,TypeDescriptorsContainer _container) {
        this.bytes = new byte[size];
        this.codingOption = _codingOption;
        this.container = _container;
    }

    public TypeDescriptorsContainer getTypeDescriptorContainer(){
        return this.container;
    }

    public void addSubElementData(int classType, EncodeDataContainer subBytesArray,Object _element) {
        if(_element instanceof ListWithKey){
            int i=0;
        }
        List<EncodeDataContainer> subElementList = (List<EncodeDataContainer>) subElementsData.get(classType);
        if (subElementList == null) {
            subElementList = new LinkedList<EncodeDataContainer>();
            subElementsData.put(classType, subElementList);
        }
        if(_element!=null){
            subBytesArray.setMD5ID(this.container.getTypeDescriptorByObject(_element).getMD5IDForObject(_element));
        }
        subElementList.add(subBytesArray);
    }

    public Map<Integer, List<EncodeDataContainer>> getSubElementsData() {
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
        for (List<EncodeDataContainer> list : this.subElementsData.values()) {
            for (EncodeDataContainer subBA : list) {
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

    public void setMD5ID(MD5Identifier _md5ID){
        this.md5ID = _md5ID;
    }

    public MD5Identifier getMD5ID(){
        return this.md5ID;
    }

    public static byte[] toSingleByteArray(EncodeDataContainer source){
        EncodeDataContainer enc = new EncodeDataContainer(1024,source.getTypeDescriptorContainer());
        byte data[] = source.getData();
        EncodeUtils.encodeByteArray(data, enc);
        EncodeUtils.encodeInt16(source.subElementsData.size(), enc);
        for(Map.Entry<Integer,List<EncodeDataContainer>> subBAs:source.subElementsData.entrySet()){
            EncodeUtils.encodeInt16(subBAs.getKey(), enc);
            EncodeUtils.encodeInt32(subBAs.getValue().size(), enc);
            for(EncodeDataContainer subBA:subBAs.getValue()){
                EncodeUtils.encodeByteArray(toSingleByteArray(subBA),enc);
            }
        }
        return enc.getData();
    }

    public static EncodeDataContainer fromSingleByteArray(EncodeDataContainer source){
        EncodeDataContainer result = new EncodeDataContainer(1024,source.getTypeDescriptorContainer());
        result.bytes = EncodeUtils.decodeByteArray(source);
        int subMapSize = EncodeUtils.decodeInt16(source);
        for(int i=0;i<subMapSize;i++){
            int code = EncodeUtils.decodeInt16(source);
            int size = EncodeUtils.decodeInt32(source);
            for(int j=0;j<size;j++){
                byte subData[] = EncodeUtils.decodeByteArray(source);
                EncodeDataContainer subBASource = new EncodeDataContainer(subData,source.getTypeDescriptorContainer());
                EncodeDataContainer subBA = fromSingleByteArray(subBASource);
                result.addSubElementData(code, subBA,null);
            }
        }
        return result;
    }
}
