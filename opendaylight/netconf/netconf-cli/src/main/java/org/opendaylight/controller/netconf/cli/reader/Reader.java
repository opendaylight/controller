package org.opendaylight.controller.netconf.cli.reader;

import java.util.List;

import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;

public interface Reader<T extends DataSchemaNode> {

    // FIXME add final everywhere

    List<Node<?>> read(T schemaNode) throws ReadingException;

}
