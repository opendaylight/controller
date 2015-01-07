/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.datasand.codec.xml;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.opendaylight.datasand.codec.EncodeDataContainer;
import org.opendaylight.datasand.codec.TypeDescriptor;
/**
 * @author - Sharon Aicler (saichler@cisco.com)
 *
 */
public class XMLEncodeDataContainer extends EncodeDataContainer{

    private List<XMLEntry> properties = new LinkedList<XMLEntry>();

    public XMLEncodeDataContainer(TypeDescriptor _td){
        super(_td,EncodeDataContainer.ENCODER_TYPE_XML);
    }

    @Override
    public void resetLocation() {
    }

    public void addEntry(String name,Object obj){
        this.properties.add(new XMLEntry(name,obj));
    }

    private class XMLEntry{
        public String name;
        public String value;
        public XMLEntry(String _name,Object _value){
            this.name = _name;
            if(_value!=null)
                this.value = _value.toString();
            else
                this.value="";
        }
        public String toXML(int level){
            StringBuffer buff = new StringBuffer();
            appendTab(buff, level);
            buff.append("<").append(this.name).append(">").append(this.value).append("</").append(this.name).append(">");
            return buff.toString();
        }
    }

    public static void appendTab(StringBuffer b,int level){
        for(int i=0;i<level;i++){
            b.append("  ");
        }
    }

    public String toXML(int level){
        StringBuffer buff = new StringBuffer();
        appendTab(buff, level);
        buff.append("<").append(this.getTypeDescriptor().getTypeClassShortName()).append(">").append("\n");
        for(XMLEntry entry:properties){
            buff.append(entry.toXML(level+1)).append("\n");
        }
        Map<Integer, List<EncodeDataContainer>> subElements = this.getSubElementsData();
        for(Map.Entry<Integer,List<EncodeDataContainer>> entry:subElements.entrySet()){
            for(EncodeDataContainer edc:entry.getValue()){
                buff.append(((XMLEncodeDataContainer)edc).toXML(level+1));
            }
        }
        appendTab(buff, level);
        buff.append("</").append(this.getTypeDescriptor().getTypeClassShortName()).append(">").append("\n");
        return buff.toString();
    }
}
