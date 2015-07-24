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
import jline.console.completer.StringsCompleter;
import org.opendaylight.controller.netconf.cli.io.BaseConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;

public class LeafReader extends BasicDataHolderReader<LeafSchemaNode> {

    public LeafReader(final ConsoleIO console, final SchemaContext schemaContext) {
        super(console, schemaContext);
    }

    public LeafReader(final ConsoleIO console, final SchemaContext schemaContext, final boolean readConfigNode) {
        super(console, schemaContext, readConfigNode);
    }

    @Override
    protected TypeDefinition<?> getType(final LeafSchemaNode schemaNode) {
        return schemaNode.getType();
    }

    @Override
    protected ConsoleContext getContext(final LeafSchemaNode schemaNode) {
        return new BaseConsoleContext<LeafSchemaNode>(schemaNode) {
            @Override
            public List<Completer> getAdditionalCompleters() {
                final List<Completer> completers = Lists.<Completer> newArrayList(getBaseCompleter(schemaNode));
                final Optional<String> defaultValue = getDefaultValue(schemaNode);
                if (defaultValue.isPresent()) {
                    completers.add(new StringsCompleter(defaultValue.get()));
                }
                return completers;
            }
        };
    }
}
