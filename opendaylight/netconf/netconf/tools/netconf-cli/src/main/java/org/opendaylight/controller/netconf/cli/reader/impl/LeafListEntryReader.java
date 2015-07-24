
/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli.reader.impl;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import java.util.List;
import jline.console.completer.Completer;
import org.opendaylight.controller.netconf.cli.io.BaseConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.controller.netconf.cli.reader.GenericListEntryReader;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;

class LeafListEntryReader extends BasicDataHolderReader<LeafListSchemaNode> implements
        GenericListEntryReader<LeafListSchemaNode> {

    public LeafListEntryReader(final ConsoleIO console, final SchemaContext schemaContext) {
        super(console, schemaContext);
    }

    public LeafListEntryReader(final ConsoleIO console, final SchemaContext schemaContext, final boolean readConfigNode) {
        super(console, schemaContext, readConfigNode);
    }

    @Override
    protected TypeDefinition<?> getType(final LeafListSchemaNode schemaNode) {
        return schemaNode.getType();
    }

    @Override
    protected ConsoleContext getContext(final LeafListSchemaNode schemaNode) {
        return new BaseConsoleContext<LeafListSchemaNode>(schemaNode) {

            @Override
            public Optional<String> getPrompt() {
                return Optional.of("[entry]");
            }

            @Override
            protected List<Completer> getAdditionalCompleters() {
                return Lists.<Completer> newArrayList(getBaseCompleter(getDataSchemaNode()));
            }
        };
    }
}
