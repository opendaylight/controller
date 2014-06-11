/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli.reader.impl;

import static org.opendaylight.controller.netconf.cli.io.IOUtil.isSkipInput;

import com.google.common.base.Optional;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jline.console.completer.AggregateCompleter;
import jline.console.completer.Completer;
import jline.console.completer.StringsCompleter;
import org.opendaylight.controller.netconf.cli.io.ConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.controller.netconf.cli.io.IOUtil;
import org.opendaylight.controller.netconf.cli.reader.ReadingException;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.UnionTypeDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class UnionTypeReader {
    private static final Logger LOG = LoggerFactory.getLogger(UnionTypeReader.class);

    private final ConsoleIO console;

    public UnionTypeReader(final ConsoleIO console) {
        this.console = console;
    }

    public Optional<TypeDefinition<?>> read(final TypeDefinition<?> unionTypeDefinition) throws IOException,
            ReadingException {
        final ConsoleContext context = getContext(unionTypeDefinition);
        console.enterContext(context);
        try {
            final Map<String, TypeDefinition<?>> mapping = ((UnionConsoleContext) context).getMenuItemMapping();
            console.formatLn("The element is of type union. Choose concrete type from: %s", mapping.keySet());

            final String rawValue = console.read();
            if (isSkipInput(rawValue)) {
                return Optional.absent();
            }
            final TypeDefinition<?> value = mapping.get(rawValue);
            if (value != null) {
                return Optional.<TypeDefinition<?>> of(value);
            } else {
                final String message = String.format("Incorrect type (%s) was specified for union type definition", rawValue);
                LOG.error(message);
                throw new ReadingException(message);
            }
        } finally {
            console.leaveContext();
        }
    }

    private UnionConsoleContext getContext(final TypeDefinition<?> typeDefinition) {
        return new UnionConsoleContext(typeDefinition);
    }

    private class UnionConsoleContext implements ConsoleContext {

        private final TypeDefinition<?> typeDef;
        private final Map<String, TypeDefinition<?>> menuItemsToTypeDefinitions = new HashMap<>();

        public UnionConsoleContext(final TypeDefinition<?> typeDef) {
            this.typeDef = typeDef;
        }

        @Override
        public Optional<String> getPrompt() {
            return Optional.of("type[" + typeDef.getQName().getLocalName()  + "]");
        }

        @Override
        public Completer getCompleter() {
            List<TypeDefinition<?>> subtypesForMenu = resolveSubtypesFrom(typeDef);
            if (subtypesForMenu.isEmpty()) {
                subtypesForMenu = Collections.<TypeDefinition<?>> singletonList(typeDef);
            }
            final Collection<String> menuItems = toMenuItem(subtypesForMenu);
            return new AggregateCompleter(new StringsCompleter(menuItems), new StringsCompleter(IOUtil.SKIP));
        }

        public Map<String, TypeDefinition<?>> getMenuItemMapping() {
            return menuItemsToTypeDefinitions;
        }

        private Collection<String> toMenuItem(final List<TypeDefinition<?>> allTypesBehindUnion) {
            final List<String> result = new ArrayList<String>();
            for (final TypeDefinition<?> type : allTypesBehindUnion) {
                final String menuItem = type.getQName().getLocalName();
                menuItemsToTypeDefinitions.put(menuItem, type);
                result.add(menuItem);
            }
            return result;
        }

        /**
         *
         * If union type is found in potentialEndTypeCandidate as subtype then
         * it these subtypes become candidates.
         *
         * @param potentialEndTypeCandidate
         *            candidate to node which has no union subtype
         */
        private List<TypeDefinition<?>> resolveSubtypesFrom(final TypeDefinition<?> potentialEndTypeCandidate) {
            if (potentialEndTypeCandidate instanceof UnionTypeDefinition) {
                return ((UnionTypeDefinition) potentialEndTypeCandidate).getTypes();
            }
            if (potentialEndTypeCandidate.getBaseType() == null) {
                return Collections.emptyList();
            }
            return resolveSubtypesFrom(potentialEndTypeCandidate.getBaseType());
        }
    }
}
