package org.opendaylight.persisted.autoagents;

import java.util.List;

import org.opendaylight.persisted.codec.TypeDescriptor;
import org.opendaylight.persisted.net.NetworkID;
import org.opendaylight.persisted.net.Packet;


public enum ClassCodes {
    Long(7,Long.class.getName()),
    Integer(8,Integer.class.getName()),
    Class(9,Class.class.getName()),
    String(10,String.class.getName()),
    NetworkID(11,NetworkID.class.getName()),
    Packet(12,Packet.class.getName()),
    CCommand(13,CCommand.class.getName()),
    TypeDescriptor(14,TypeDescriptor.class.getName()),
    ChangeID(15,ChangeID.class.getName()),
    List(16,List.class.getName());

    private int code=-1;
    private String className = null;

    private ClassCodes(int _code,String _clsName){
        this.code = _code;
        this.className = _clsName;
    }
    public int getCode(){
        return this.code;
    }
}
