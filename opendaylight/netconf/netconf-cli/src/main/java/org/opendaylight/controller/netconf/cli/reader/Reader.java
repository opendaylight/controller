package org.opendaylight.controller.netconf.cli.reader;

import java.util.List;

import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;

public interface Reader<T extends DataSchemaNode> {

    List<Node<?>> read(T schemaNode) throws ReadingException;

}
