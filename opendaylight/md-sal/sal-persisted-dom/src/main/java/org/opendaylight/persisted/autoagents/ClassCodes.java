package org.opendaylight.persisted.autoagents;

import org.opendaylight.persisted.autoagents.ClusterMap.CCommand;
import org.opendaylight.persisted.codec.TypeDescriptor;
import org.opendaylight.persisted.net.NetworkID;
import org.opendaylight.persisted.net.Packet;


public enum ClassCodes {
    Class(9,Class.class.getName()),
    String(10,String.class.getName()),
    NetworkID(11,NetworkID.class.getName()),
    Packet(12,Packet.class.getName()),
    CCommand(13,CCommand.class.getName()),
    TypeDescriptor(14,TypeDescriptor.class.getName());

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
