/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli.writer.impl;

import java.io.IOException;
import java.util.List;
import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.controller.netconf.cli.writer.WriteException;
import org.opendaylight.controller.netconf.cli.writer.Writer;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;

public abstract class AbstractWriter<T extends DataSchemaNode> implements Writer<T> {

    protected ConsoleIO console;

    public AbstractWriter(final ConsoleIO console) {
        this.console = console;
    }

    @Override
    public void write(final T dataSchemaNode, final List<Node<?>> dataNodes) throws WriteException {
        try {
            writeInner(dataSchemaNode, dataNodes);
        } catch (final IOException e) {
            throw new WriteException("Unable to write data to output for " + dataSchemaNode.getQName(), e);
        }
    }

    protected abstract void writeInner(final T dataSchemaNode, final List<Node<?>> dataNodes) throws IOException,
            WriteException;
}
