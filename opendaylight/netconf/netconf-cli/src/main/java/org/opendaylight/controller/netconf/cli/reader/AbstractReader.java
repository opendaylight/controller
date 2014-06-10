/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli.reader;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import java.io.IOException;
import java.util.List;
import jline.console.completer.Completer;
import jline.console.completer.NullCompleter;
import org.opendaylight.controller.netconf.cli.io.ConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.model.api.ChoiceNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.EmptyTypeDefinition;

public abstract class AbstractReader<T extends DataSchemaNode> implements Reader<T> {

    public static final NullContext NULL_CONTEXT = new NullContext();

    // TODO make console private add protected getter
    protected ConsoleIO console;
    private final SchemaContext context;

    public AbstractReader(final ConsoleIO console, final SchemaContext context) {
        this.console = console;
        this.context = context;
    }

    protected SchemaContext getSchemaContext() {
        return context;
    }

    protected ConsoleIO getConsole() {
        return console;
    }

    @Override
    public List<Node<?>> read(final T schemaNode) throws ReadingException {
        final ConsoleContext ctx = getContext(schemaNode);
        console.enterContext(ctx);
        try {
            return readWithContext(schemaNode);
        } catch (final IOException e) {
            throw new ReadingException("Unable to read data from input for " + schemaNode.getQName(), e);
        } finally {
            console.leaveContext();
        }
    }

    // TODO javadoc

    protected abstract List<Node<?>> readWithContext(T schemaNode) throws IOException, ReadingException;

    protected abstract ConsoleContext getContext(T schemaNode);

    protected Optional<String> getDefaultValue(final T schemaNode) {
        String defaultValue = null;
        if (schemaNode instanceof LeafSchemaNode) {
            defaultValue = ((LeafSchemaNode) schemaNode).getDefault();
        } else if (schemaNode instanceof ChoiceNode) {
            defaultValue = ((ChoiceNode) schemaNode).getDefaultCase();
        }

        return Optional.fromNullable(defaultValue);
    }

    protected boolean isEmptyInput(final String rawValue) {
        return Strings.isNullOrEmpty(rawValue);
    }

    protected static boolean isEmptyType(final TypeDefinition<?> type) {
        return type instanceof EmptyTypeDefinition;
    }

    private static class NullContext implements ConsoleContext {
        @Override
        public Completer getCompleter() {
            return new NullCompleter();
        }

        @Override
        public Optional<String> getPrompt() {
            return Optional.absent();
        }
    }

}
