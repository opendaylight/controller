package org.opendaylight.controller.sal.restconf.impl;

import java.net.URI;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.Node;

public interface NodeWrapper<T extends Node<?>> {

    void setQname(QName name);
    
    QName getQname();
    
    T unwrap();
    
    boolean isChangeAllowed();
    
    URI getNamespace();

    void setNamespace(URI namespace);
    
    String getLocalName();
}