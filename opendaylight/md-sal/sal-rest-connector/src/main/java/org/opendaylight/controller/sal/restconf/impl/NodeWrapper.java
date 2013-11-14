package org.opendaylight.controller.sal.restconf.impl;

import java.net.URI;

import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;

public interface NodeWrapper<T extends Node<?>> {

    T unwrap(CompositeNode parent);
    
    URI getNamespace();

    void setNamespace(URI namespace);
    
    String getLocalName();
}