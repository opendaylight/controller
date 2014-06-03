package org.opendaylight.controller.netconf.cli.writer;

import java.util.List;

import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;

/**
 * Generic handler(writer) of output elements for commands
 */
public interface Writer<T extends DataSchemaNode> {

    void write(T dataSchemaNode, List<Node<?>> dataNodes) throws WriteException;

}
