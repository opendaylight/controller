package org.opendaylight.controller.netconf.cli.reader.impl;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jline.console.completer.Completer;
import jline.console.completer.StringsCompleter;

import org.opendaylight.controller.netconf.cli.io.ConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.UnionTypeDefinition;

import com.google.common.base.Optional;

public class UnionTypeReader {

    private final ConsoleIO console;

    public UnionTypeReader(ConsoleIO console) {
        this.console = console;
    }

    // TODO all united types should not be presented flat to the user, but recursively
    // the recursion should be performed in BasicDataHolderReader line 32, if should be transformed to while

    public TypeDefinition<?> read(final TypeDefinition<?> unionTypeDefinition) throws IOException {
        UnionConsoleContext context = getContext(unionTypeDefinition);
        console.enterContext(context);
        try {
            console.writeLn("The element is of type union. Choose type:");

            while (true) {
                String rawValue = console.read();
                Map<String, TypeDefinition<?>> mapping = context.getMenuItemMapping();
                TypeDefinition<?> value = mapping.get(rawValue);
                if (value != null) {
                    return value;
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
            Map<TypeDefinition<?>, TypeDefinition<?>> allTypesBehindUnion = getAllTypesBehindUnion(typeDef);
            return new StringsCompleter(toMenuItem(allTypesBehindUnion));
        }

        public Map<String, TypeDefinition<?>> getMenuItemMapping() {
            return menuItemsToTypeDefinitions;
        }

        private Collection<String> toMenuItem(Map<TypeDefinition<?>, TypeDefinition<?>> allTypesBehindUnion) {
            List<String> result = new ArrayList<String>();
            for (Entry<TypeDefinition<?>, TypeDefinition<?>> type : allTypesBehindUnion.entrySet()) {
                String menuItem = type.getKey().getQName().getLocalName() + "("
                        + type.getValue().getQName().getLocalName() + ")";
                menuItemsToTypeDefinitions.put(menuItem, type.getKey());
                result.add(menuItem);
            }
            return result;
        }

        private Map<TypeDefinition<?>, TypeDefinition<?>> getAllTypesBehindUnion(TypeDefinition<?> typeDefinition) {
            Deque<TypeDefinition<?>> candidates = new ArrayDeque<>();
            candidates.addLast(typeDefinition);
            Map<TypeDefinition<?>, TypeDefinition<?>> result = new HashMap<>();
            while (!candidates.isEmpty()) {
                TypeDefinition<?> baseType = resolveCandidate(candidates.getFirst(), candidates);
                if (baseType != null) {
                    result.put(candidates.removeFirst(), baseType);
                }
            }
            return result;
        }

        /**
         *
         * If union type is found in potentialEndTypeCandidate as subtype then
         * it can't be candidate. Its child union type are added to
         * potentialEndTypesCandidates.
         *
         * @param potentialEndTypeCandidate
         *            candidate to node which has no union subtype
         */
        private TypeDefinition<?> resolveCandidate(TypeDefinition<?> potentialEndTypeCandidate,
                Deque<TypeDefinition<?>> potentialEndTypesCandidates) {
            if (potentialEndTypeCandidate instanceof UnionTypeDefinition) {
                for (TypeDefinition<?> subtype : ((UnionTypeDefinition) potentialEndTypeCandidate).getTypes()) {
                    potentialEndTypesCandidates.addLast(subtype);
                }
                potentialEndTypesCandidates.removeFirst();
                return null;
            }
            if (potentialEndTypeCandidate.getBaseType() == null) {
                return potentialEndTypeCandidate;
            }
            return resolveCandidate(potentialEndTypeCandidate.getBaseType(), potentialEndTypesCandidates);
        }
    }
}
