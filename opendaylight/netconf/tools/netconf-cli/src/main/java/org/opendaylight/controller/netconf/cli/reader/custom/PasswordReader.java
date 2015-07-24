/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli.reader.custom;

import com.google.common.base.Preconditions;
import java.io.IOException;
import jline.console.completer.Completer;
import jline.console.completer.NullCompleter;
import org.opendaylight.controller.netconf.cli.io.BaseConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.controller.netconf.cli.reader.impl.BasicDataHolderReader;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;

public class PasswordReader extends BasicDataHolderReader<DataSchemaNode> {

    private static final char PASSWORD_MASK = '*';

    public PasswordReader(final ConsoleIO console, final SchemaContext schemaContext) {
        super(console, schemaContext);
    }

    @Override
    protected ConsoleContext getContext(final DataSchemaNode schemaNode) {
        return new BaseConsoleContext<DataSchemaNode>(schemaNode) {
            @Override
            public Completer getCompleter() {
                return new NullCompleter();
            }
        };
    }

    @Override
    protected TypeDefinition<?> getType(final DataSchemaNode schemaNode) {
        Preconditions.checkArgument(schemaNode instanceof LeafSchemaNode);
        return ((LeafSchemaNode)schemaNode).getType();
    }

    @Override
    protected String readValue() throws IOException {
        return console.read(PASSWORD_MASK);
    }
}
