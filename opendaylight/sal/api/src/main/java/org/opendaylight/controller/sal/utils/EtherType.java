package org.opendaylight.controller.sal.utils;

public interface EtherType {

    public abstract String getDescription();
    
    public abstract int intValue();

    public abstract short shortValue();

}