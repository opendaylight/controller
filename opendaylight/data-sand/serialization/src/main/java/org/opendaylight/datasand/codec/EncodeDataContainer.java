/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.datasand.codec;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.opendaylight.datasand.codec.bytearray.ByteEncoder;
import org.opendaylight.datasand.codec.json.JsonEncoder;
import org.opendaylight.datasand.codec.xml.XMLEncoder;

/**
 * @author - Sharon Aicler (saichler@cisco.com)
 */
public abstract class EncodeDataContainer {
    private Object codingOption = null;
    private LeftOvers leftOvers = null;
    private Map<Integer, List<EncodeDataContainer>> subElementsData = new HashMap<Integer, List<EncodeDataContainer>>();
    private MD5Identifier md5ID = null;
    private TypeDescriptor typeDescriptor = null;
    private int encoderType = -1;
    private String currentAttributeName = null;

    private static final Map<Integer,AbstractEncoder> encoders = new ConcurrentHashMap<Integer,AbstractEncoder>();
    public static final int ENCODER_TYPE_BYTE_ARRAY = 5;
    public static final int ENCODER_TYPE_SQLITE     = 6;
    public static final int ENCODER_TYPE_JSON       = 7;
    public static final int ENCODER_TYPE_XML        = 8;
    static{
        encoders.put(ENCODER_TYPE_BYTE_ARRAY, new ByteEncoder());
        encoders.put(ENCODER_TYPE_JSON, new JsonEncoder());
        encoders.put(ENCODER_TYPE_XML, new XMLEncoder());
    }

    public static final void registerEncoder(int type,AbstractEncoder encoder){
        encoders.put(type, encoder);
    }

    public EncodeDataContainer(TypeDescriptor _typeDescriptor,int _encoderType) {
        this(null,_typeDescriptor,_encoderType);
    }

    public EncodeDataContainer(Object _codingOption,TypeDescriptor _typeDescriptor,int _encoderType) {
        this.codingOption = _codingOption;
        this.typeDescriptor = _typeDescriptor;
        this.encoderType = _encoderType;
    }

    public AbstractEncoder getEncoder(){
        return encoders.get(this.encoderType);
    }

    public TypeDescriptorsContainer getTypeDescriptorContainer(){
        return this.typeDescriptor.getTypeDescriptorsContainer();
    }

    public TypeDescriptor getTypeDescriptor(){
        return this.typeDescriptor;
    }

    public void addSubElementData(int classType, EncodeDataContainer subBytesArray,Object _element) {
        List<EncodeDataContainer> subElementList = (List<EncodeDataContainer>) subElementsData.get(classType);
        if (subElementList == null) {
            subElementList = new LinkedList<EncodeDataContainer>();
            subElementsData.put(classType, subElementList);
        }
        if(_element!=null){
            subBytesArray.setMD5ID(this.typeDescriptor.getTypeDescriptorsContainer().getTypeDescriptorByObject(_element).getMD5IDForObject(_element));
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

    public void setCurrentAttributeName(String attName){
        this.currentAttributeName = attName;
    }

    public String getCurrentAttributeName(){
        return this.currentAttributeName;
    }
}
