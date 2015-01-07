/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.datasand.agents;

import org.opendaylight.datasand.codec.EncodeDataContainer;
import org.opendaylight.datasand.codec.ISerializer;
/**
 * @author - Sharon Aicler (saichler@cisco.com)
 *
 * Message is the vessel which is used to transfer data from one node to another.
 */
public class Message implements ISerializer{

    private long messageID = -1;
    private int messageType = -1;
    private Object messageData = null;
    private static long nextMessageID = 1000;

    public Message(){
    }

    public Message(long _messageID,int _messageType,Object _messageData){
        this.messageID = _messageID;
        this.messageType = _messageType;
        this.messageData = _messageData;
    }

    public Message(int _messageType,Object _messageData){
        synchronized(Message.class){
            this.messageID = nextMessageID;
            nextMessageID++;
        }
        this.messageType = _messageType;
        this.messageData = _messageData;
    }

    public long getMessageID() {
        return messageID;
    }

    public int getMessageType() {
        return messageType;
    }

    public Object getMessageData() {
        return messageData;
    }

    @Override
    public void encode(Object value, byte[] byteArray, int location) {

    }

    @Override
    public void encode(Object value, EncodeDataContainer edc) {
        Message m = (Message)value;
        edc.getEncoder().encodeInt64(m.messageID, edc);
        edc.getEncoder().encodeInt32(m.getMessageType(), edc);
        edc.getEncoder().encodeObject(m.messageData, edc);
    }

    @Override
    public Object decode(byte[] byteArray, int location, int length) {
        return null;
    }

    @Override
    public Object decode(EncodeDataContainer edc, int length) {
        Message m = new Message();
        m.messageID = edc.getEncoder().decodeInt64(edc);
        m.messageType = edc.getEncoder().decodeInt32(edc);
        m.messageData = edc.getEncoder().decodeObject(edc);
        return m;
    }

    @Override
    public String getShardName(Object obj) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object getRecordKey(Object obj) {
        // TODO Auto-generated method stub
        return null;
    }
}
