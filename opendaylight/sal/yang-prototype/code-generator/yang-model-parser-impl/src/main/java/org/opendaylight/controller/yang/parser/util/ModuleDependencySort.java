/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.parser.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.yang.model.api.Module;
import org.opendaylight.controller.yang.model.api.ModuleImport;
import org.opendaylight.controller.yang.model.api.SchemaContext;
import org.opendaylight.controller.yang.parser.builder.impl.ModuleBuilder;
import org.opendaylight.controller.yang.parser.impl.YangParserListenerImpl;
import org.opendaylight.controller.yang.parser.util.TopologicalSort.Node;
import org.opendaylight.controller.yang.parser.util.TopologicalSort.NodeImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Creates a module dependency graph from provided {@link ModuleBuilder}s and
 * provides a {@link #sort()} method. It is topological sort and returns modules
 * in order in which they should be processed (e.g. if A imports B, sort returns
 * {B, A}).
 */
public final class ModuleDependencySort {

    private static final Date DEFAULT_REVISION = new Date(0);
    private static final Logger logger = LoggerFactory
            .getLogger(ModuleDependencySort.class);

    /**
     * Topological sort of module builder dependency graph.
     *
     * @return Sorted list of Module builders. Modules can be further processed
     *         in returned order.
     */
    public static List<ModuleBuilder> sort(ModuleBuilder... builders) {
        List<Node> sorted = sortInternal(Arrays.asList(builders));
        // Cast to ModuleBuilder from Node and return
        return Lists.transform(sorted, new Function<Node, ModuleBuilder>() {

            @Override
            public ModuleBuilder apply(Node input) {
                return (ModuleBuilder) ((ModuleNodeImpl) input).getReference();
            }
        });
    }

    public static List<ModuleBuilder> sortWithContext(SchemaContext context, ModuleBuilder... builders) {
        List<Object> modules = new ArrayList<Object>();
        Collections.addAll(modules, builders);
        modules.addAll(context.getModules());

        List<Node> sorted = sortInternal(modules);
        // Cast to ModuleBuilder from Node if possible and return
        return Lists.transform(sorted, new Function<Node, ModuleBuilder>() {

            @Override
            public ModuleBuilder apply(Node input) {
                if(((ModuleNodeImpl) input).getReference() instanceof ModuleBuilder) {
                    return (ModuleBuilder) ((ModuleNodeImpl) input).getReference();
                } else {
                    return null;
                }
            }
        });
    }

    /**
     * Topological sort of module dependency graph.
     *
     * @return Sorted list of Modules. Modules can be further processed in
     *         returned order.
     */
    public static List<Module> sort(Module... modules) {
        List<Node> sorted = sortInternal(Arrays.asList(modules));
        // Cast to Module from Node and return
        return Lists.transform(sorted, new Function<Node, Module>() {

            @Override
            public Module apply(Node input) {
                return (Module) ((ModuleNodeImpl) input).getReference();
            }
        });
    }

    private static List<Node> sortInternal(List<?> modules) {
        Map<String, Map<Date, ModuleNodeImpl>> moduleGraph = createModuleGraph(modules);

        Set<Node> nodes = Sets.newHashSet();
        for (Map<Date, ModuleNodeImpl> map : moduleGraph.values()) {
            for (ModuleNodeImpl node : map.values()) {
                nodes.add(node);
            }
        }

        return TopologicalSort.sort(nodes);
    }

    @VisibleForTesting
    static Map<String, Map<Date, ModuleNodeImpl>> createModuleGraph(
            List<?> builders) {
        Map<String, Map<Date, ModuleNodeImpl>> moduleGraph = Maps.newHashMap();

        processModules(moduleGraph, builders);
        processDependencies(moduleGraph, builders);

        return moduleGraph;
    }

    /**
     * Extract module:revision from module builders
     */
    private static void processDependencies(
            Map<String, Map<Date, ModuleNodeImpl>> moduleGraph, List<?> builders) {
        Map<String, Date> imported = Maps.newHashMap();

        // Create edges in graph
        for (Object mb : builders) {

            String fromName = null;
            Date fromRevision = null;
            Set<ModuleImport> imports = null;

            if (mb instanceof Module) {
                fromName = ((Module) mb).getName();
                fromRevision = ((Module) mb).getRevision();
                imports = ((Module) mb).getImports();
            } else if (mb instanceof ModuleBuilder) {
                fromName = ((ModuleBuilder) mb).getName();
                fromRevision = ((ModuleBuilder) mb).getRevision();
                imports = ((ModuleBuilder) mb).getModuleImports();
            }
            // no need to check if other Type of object, check is performed in
            // process modules

            if (fromRevision == null)
                fromRevision = DEFAULT_REVISION;

            for (ModuleImport imprt : imports) {
                String toName = imprt.getModuleName();
                Date toRevision = imprt.getRevision() == null ? DEFAULT_REVISION
                        : imprt.getRevision();

                ModuleNodeImpl from = moduleGraph.get(fromName).get(
                        fromRevision);

                ModuleNodeImpl to = getModuleByNameAndRevision(moduleGraph,
                        fromName, fromRevision, toName, toRevision);

                /*
                 * Check imports: If module is imported twice with different
                 * revisions then throw exception
                 */
                if (imported.get(toName) != null
                        && !imported.get(toName).equals(toRevision))
                    ex(String
                            .format("Module:%s imported twice with different revisions:%s, %s",
                                    toName,
                                    formatRevDate(imported.get(toName)),
                                    formatRevDate(toRevision)));
                imported.put(toName, toRevision);

                from.addEdge(to);
            }
        }
    }

    /**
     * Get imported module by its name and revision from moduleGraph
     */
    private static ModuleNodeImpl getModuleByNameAndRevision(
            Map<String, Map<Date, ModuleNodeImpl>> moduleGraph,
            String fromName, Date fromRevision, String toName, Date toRevision) {
        ModuleNodeImpl to = null;

        if (moduleGraph.get(toName) == null
                || !moduleGraph.get(toName).containsKey(toRevision)) {
            // If revision is not specified in import, but module exists
            // with different revisions, take first
            if (moduleGraph.get(toName) != null
                    && !moduleGraph.get(toName).isEmpty()
                    && toRevision.equals(DEFAULT_REVISION)) {
                to = moduleGraph.get(toName).values().iterator().next();
                logger.warn(String
                        .format("Import:%s:%s by module:%s:%s does not specify revision, using:%s:%s for module dependency sort",
                                toName, formatRevDate(toRevision), fromName,
                                formatRevDate(fromRevision), to.getName(),
                                formatRevDate(to.getRevision())));
            } else
                ex(String.format("Not existing module imported:%s:%s by:%s:%s",
                        toName, formatRevDate(toRevision), fromName,
                        formatRevDate(fromRevision)));
        } else {
            to = moduleGraph.get(toName).get(toRevision);
        }
        return to;
    }

    private static void ex(String message) {
        throw new YangValidationException(message);
    }

    /**
     * Extract dependencies from module builders or modules to fill dependency
     * graph
     */
    private static void processModules(
            Map<String, Map<Date, ModuleNodeImpl>> moduleGraph, List<?> builders) {

        // Process nodes
        for (Object mb : builders) {

            String name = null;
            Date rev = null;

            if (mb instanceof Module) {
                name = ((Module) mb).getName();
                rev = ((Module) mb).getRevision();
            } else if (mb instanceof ModuleBuilder) {
                name = ((ModuleBuilder) mb).getName();
                rev = ((ModuleBuilder) mb).getRevision();
            } else {
                throw new IllegalStateException(
                        String.format(
                                "Unexpected type of node for sort, expected only:%s, %s, got:%s",
                                Module.class, ModuleBuilder.class,
                                mb.getClass()));
            }

            if (rev == null)
                rev = DEFAULT_REVISION;

            if (moduleGraph.get(name) == null)
                moduleGraph.put(name, Maps.<Date, ModuleNodeImpl> newHashMap());

            if (moduleGraph.get(name).get(rev) != null)
                ex(String.format("Module:%s with revision:%s declared twice",
                        name, formatRevDate(rev)));

            moduleGraph.get(name).put(rev, new ModuleNodeImpl(name, rev, mb));
        }
    }

    private static String formatRevDate(Date rev) {
        return rev == DEFAULT_REVISION ? "default"
                : YangParserListenerImpl.simpleDateFormat.format(rev);
    }

    @VisibleForTesting
    static class ModuleNodeImpl extends NodeImpl {
        private final String name;
        private final Date revision;
        private final Object originalObject;

        public ModuleNodeImpl(String name, Date revision, Object builder) {
            this.name = name;
            this.revision = revision;
            this.originalObject = builder;
        }

        public String getName() {
            return name;
        }

        public Date getRevision() {
            return revision;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            result = prime * result
                    + ((revision == null) ? 0 : revision.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ModuleNodeImpl other = (ModuleNodeImpl) obj;
            if (name == null) {
                if (other.name != null)
                    return false;
            } else if (!name.equals(other.name))
                return false;
            if (revision == null) {
                if (other.revision != null)
                    return false;
            } else if (!revision.equals(other.revision))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "Module [name=" + name + ", revision="
                    + formatRevDate(revision) + "]";
        }

        public Object getReference() {
            return originalObject;
        }

    }

}
