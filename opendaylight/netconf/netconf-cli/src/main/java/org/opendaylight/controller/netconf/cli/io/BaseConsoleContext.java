/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli.io;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jline.console.completer.AggregateCompleter;
import jline.console.completer.Completer;
import jline.console.completer.StringsCompleter;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;

public class BaseConsoleContext<T extends DataSchemaNode> implements ConsoleContext {

    private static final Completer SKIP_COMPLETER = new StringsCompleter(IOUtil.SKIP);

    private final T dataSchemaNode;

    public BaseConsoleContext(final T dataSchemaNode) {
        Preconditions.checkNotNull(dataSchemaNode);
        this.dataSchemaNode = dataSchemaNode;
    }

    @Override
    public Completer getCompleter() {
        final ArrayList<Completer> completers = Lists.newArrayList(SKIP_COMPLETER);
        completers.addAll(getAdditionalCompleters());
        return new AggregateCompleter(completers);
    }

    protected List<Completer> getAdditionalCompleters() {
        return Collections.emptyList();
    }

    @Override
    public Optional<String> getPrompt() {
        return Optional.of(dataSchemaNode.getQName().getLocalName());
    }

    protected T getDataSchemaNode() {
        return dataSchemaNode;
    }
}
