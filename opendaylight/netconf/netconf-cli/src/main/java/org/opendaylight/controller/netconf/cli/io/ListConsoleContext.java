package org.opendaylight.controller.netconf.cli.io;

import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;


public class ListConsoleContext extends BaseConsoleContext<DataSchemaNode> {
    private int entryCount = 0;

    public ListConsoleContext(final DataSchemaNode schemaNode) {
        super(schemaNode);
    }

    public int getEntryCount() {
        return entryCount;
    }

    public void setEntryCount(final int entryCount) {
        this.entryCount = entryCount;
    }

}
