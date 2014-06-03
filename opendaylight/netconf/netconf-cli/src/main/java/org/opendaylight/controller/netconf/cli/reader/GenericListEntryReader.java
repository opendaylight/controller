package org.opendaylight.controller.netconf.cli.reader;

import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;

/**
 * marker interface to mark reader which can be used with GenericListReader
 */
public interface GenericListEntryReader<T extends DataSchemaNode> extends Reader<T> {

    // FIXME is this really necessary ?
}
