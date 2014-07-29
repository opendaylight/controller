package org.opendaylight.controller.cluster.datastore.node.utils;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class NodeIdentifierGenerator {
    private final String id;
    private final QName qName;

    public NodeIdentifierGenerator(String id){
        this.id = id;
        this.qName = QNameFactory.create(id);
    }

    public YangInstanceIdentifier.PathArgument getArgument(){
        return new YangInstanceIdentifier.NodeIdentifier(qName);
    }
}
