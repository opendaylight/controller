package org.opendaylight.datasand.codec;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.opendaylight.datasand.codec.bytearray.ByteEncoder;

/**
 * @author saichler
 *
 */
public abstract class EncodeDataContainer {
    private Object codingOption = null;
    private LeftOvers leftOvers = null;
    private Map<Integer, List<EncodeDataContainer>> subElementsData = new HashMap<Integer, List<EncodeDataContainer>>();
    private MD5Identifier md5ID = null;
    private TypeDescriptorsContainer container = null;
    private int encoderType = -1;

    private static final Map<Integer,AbstractEncoder> encoders = new ConcurrentHashMap<Integer,AbstractEncoder>();
    public static final int ENCODER_TYPE_BYTE_ARRAY = 5;
    static{
        encoders.put(ENCODER_TYPE_BYTE_ARRAY, new ByteEncoder());
    }
    public EncodeDataContainer(TypeDescriptorsContainer _container,int _encoderType) {
        this(null,_container,_encoderType);
    }

    public EncodeDataContainer(Object _codingOption,TypeDescriptorsContainer _container,int _encoderType) {
        this.codingOption = _codingOption;
        this.container = _container;
        this.encoderType = _encoderType;
    }

    public AbstractEncoder getEncoder(){
        return encoders.get(this.encoderType);
    }

    public TypeDescriptorsContainer getTypeDescriptorContainer(){
        return this.container;
    }

    public void addSubElementData(int classType, EncodeDataContainer subBytesArray,Object _element) {
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

    public void setMD5ID(MD5Identifier _md5ID){
        this.md5ID = _md5ID;
    }

    public MD5Identifier getMD5ID(){
        return this.md5ID;
    }

    public abstract void resetLocation();
}
