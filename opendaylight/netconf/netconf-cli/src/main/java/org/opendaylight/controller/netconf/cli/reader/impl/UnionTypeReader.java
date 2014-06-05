package org.opendaylight.controller.netconf.cli.reader.impl;

import static org.opendaylight.controller.netconf.cli.io.IOUtil.isSkipInput;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jline.console.completer.Completer;
import jline.console.completer.StringsCompleter;

import org.opendaylight.controller.netconf.cli.io.ConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.controller.netconf.cli.io.IOUtil;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.UnionTypeDefinition;

import com.google.common.base.Optional;

class UnionTypeReader {

    private final ConsoleIO console;

    public UnionTypeReader(ConsoleIO console) {
        this.console = console;
    }

    public Optional<TypeDefinition<?>> read(final TypeDefinition<?> unionTypeDefinition) throws IOException {
        ConsoleContext context = getContext(unionTypeDefinition);
        console.enterContext(context);
        try {
            console.writeLn("The element is of type union. Choose value via TAB:");

            while (true) {
                String rawValue = console.read();
                if (isSkipInput(rawValue)) {
                    return Optional.absent();
                }
                Map<String, TypeDefinition<?>> mapping = ((UnionConsoleContext) context).getMenuItemMapping();
                TypeDefinition<?> value = mapping.get(rawValue);
                if (value != null) {
                    return Optional.<TypeDefinition<?>> of(value);
                }
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
            return Optional.of("*type specification*");
        }

        @Override
        public Completer getCompleter() {
            List<TypeDefinition<?>> subtypesForMenu = resolveSubtypesFrom(typeDef);
            if (subtypesForMenu.isEmpty()) {
                subtypesForMenu = Collections.<TypeDefinition<?>> singletonList(typeDef);
            }
            Collection<String> menuItems = toMenuItem(subtypesForMenu);
            menuItems.add(IOUtil.SKIP);
            return new StringsCompleter(menuItems);
        }

        public Map<String, TypeDefinition<?>> getMenuItemMapping() {
            return menuItemsToTypeDefinitions;
        }

        private Collection<String> toMenuItem(List<TypeDefinition<?>> allTypesBehindUnion) {
            List<String> result = new ArrayList<String>();
            for (TypeDefinition<?> type : allTypesBehindUnion) {
                String menuItem = type.getQName().getLocalName();
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
        private List<TypeDefinition<?>> resolveSubtypesFrom(TypeDefinition<?> potentialEndTypeCandidate) {
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
