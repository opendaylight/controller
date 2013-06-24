/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.parser.util;

import static org.opendaylight.controller.yang.parser.util.ParserUtils.*;

import java.lang.reflect.Method;
import java.util.List;

import org.opendaylight.controller.yang.model.api.AnyXmlSchemaNode;
import org.opendaylight.controller.yang.model.api.ChoiceNode;
import org.opendaylight.controller.yang.model.api.ContainerSchemaNode;
import org.opendaylight.controller.yang.model.api.GroupingDefinition;
import org.opendaylight.controller.yang.model.api.LeafListSchemaNode;
import org.opendaylight.controller.yang.model.api.LeafSchemaNode;
import org.opendaylight.controller.yang.model.api.ListSchemaNode;
import org.opendaylight.controller.yang.model.api.MustDefinition;
import org.opendaylight.controller.yang.model.api.TypeDefinition;
import org.opendaylight.controller.yang.model.util.ExtendedType;
import org.opendaylight.controller.yang.parser.builder.api.Builder;
import org.opendaylight.controller.yang.parser.builder.api.GroupingBuilder;
import org.opendaylight.controller.yang.parser.builder.api.SchemaNodeBuilder;
import org.opendaylight.controller.yang.parser.builder.api.TypeDefinitionBuilder;
import org.opendaylight.controller.yang.parser.builder.api.UsesNodeBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.AnyXmlBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.ChoiceBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.ContainerSchemaNodeBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.LeafListSchemaNodeBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.LeafSchemaNodeBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.ListSchemaNodeBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.TypeDefinitionBuilderImpl;
import org.opendaylight.controller.yang.parser.builder.impl.UnknownSchemaNodeBuilder;

/**
 * Utility class with helper methods to perform operations tied to refine
 * process.
 */
public class RefineUtils {

    private RefineUtils() {
    }

    /**
     * Find original builder of node to refine and return copy of this builder.
     * <p>
     * We must create and use a copy of builder to preserve original builder
     * state, because this object will be refined (modified) and later added to
     * {@link UsesNodeBuilder}.
     * </p>
     *
     * @param targetGrouping
     *            builder of grouping which should contains node to refine
     * @param refine
     *            refine object containing informations about refine
     * @param moduleName
     *            current module name
     * @return
     */
    public static SchemaNodeBuilder getRefineNodeFromGroupingBuilder(final GroupingBuilder targetGrouping,
            final RefineHolder refine, final String moduleName) {
        Builder result = null;
        final Builder lookedUpBuilder = findRefineTargetBuilder(targetGrouping, refine.getName());
        if (lookedUpBuilder instanceof LeafSchemaNodeBuilder) {
            result = copyLeafBuilder((LeafSchemaNodeBuilder) lookedUpBuilder);
        } else if (lookedUpBuilder instanceof ContainerSchemaNodeBuilder) {
            result = copyContainerBuilder((ContainerSchemaNodeBuilder) lookedUpBuilder);
        } else if (lookedUpBuilder instanceof ListSchemaNodeBuilder) {
            result = copyListBuilder((ListSchemaNodeBuilder) lookedUpBuilder);
        } else if (lookedUpBuilder instanceof LeafListSchemaNodeBuilder) {
            result = copyLeafListBuilder((LeafListSchemaNodeBuilder) lookedUpBuilder);
        } else if (lookedUpBuilder instanceof ChoiceBuilder) {
            result = copyChoiceBuilder((ChoiceBuilder) lookedUpBuilder);
        } else if (lookedUpBuilder instanceof AnyXmlBuilder) {
            result = copyAnyXmlBuilder((AnyXmlBuilder) lookedUpBuilder);
        } else if (lookedUpBuilder instanceof GroupingBuilder) {
            result = copyGroupingBuilder((GroupingBuilder) lookedUpBuilder);
        } else if (lookedUpBuilder instanceof TypeDefinitionBuilder) {
            result = copyTypedefBuilder((TypeDefinitionBuilderImpl) lookedUpBuilder);
        } else {
            throw new YangParseException(moduleName, refine.getLine(), "Target '" + refine.getName()
                    + "' can not be refined");
        }
        return (SchemaNodeBuilder) result;
    }

    /**
     * Create builder object from refine target node.
     *
     * @param grouping
     *            grouping which should contains node to refine
     * @param refine
     *            refine object containing informations about refine
     * @param moduleName
     *            current module name
     * @return
     */
    public static SchemaNodeBuilder getRefineNodeFromGroupingDefinition(final GroupingDefinition grouping,
            final RefineHolder refine, final String moduleName) {
        SchemaNodeBuilder result = null;
        final int line = refine.getLine();
        final Object lookedUpNode = findRefineTargetNode(grouping, refine.getName());
        if (lookedUpNode instanceof LeafSchemaNode) {
            result = createLeafBuilder((LeafSchemaNode) lookedUpNode, line);
        } else if (lookedUpNode instanceof ContainerSchemaNode) {
            result = createContainer((ContainerSchemaNode) lookedUpNode, line);
        } else if (lookedUpNode instanceof ListSchemaNode) {
            result = createList((ListSchemaNode) lookedUpNode, line);
        } else if (lookedUpNode instanceof LeafListSchemaNode) {
            result = createLeafList((LeafListSchemaNode) lookedUpNode, line);
        } else if (lookedUpNode instanceof ChoiceNode) {
            result = createChoice((ChoiceNode) lookedUpNode, line);
        } else if (lookedUpNode instanceof AnyXmlSchemaNode) {
            result = createAnyXml((AnyXmlSchemaNode) lookedUpNode, line);
        } else if (lookedUpNode instanceof GroupingDefinition) {
            result = createGrouping((GroupingDefinition) lookedUpNode, line);
        } else if (lookedUpNode instanceof TypeDefinition) {
            result = createTypedef((ExtendedType) lookedUpNode, line);
        } else {
            throw new YangParseException(moduleName, line, "Target '" + refine.getName() + "' can not be refined");
        }
        return result;
    }

    public static void refineLeaf(LeafSchemaNodeBuilder leaf, RefineHolder refine, int line) {
        String defaultStr = refine.getDefaultStr();
        Boolean mandatory = refine.isMandatory();
        MustDefinition must = refine.getMust();
        List<UnknownSchemaNodeBuilder> unknownNodes = refine.getUnknownNodes();

        if (defaultStr != null && !("".equals(defaultStr))) {
            leaf.setDefaultStr(defaultStr);
        }
        if (mandatory != null) {
            leaf.getConstraints().setMandatory(mandatory);
        }
        if (must != null) {
            leaf.getConstraints().addMustDefinition(must);
        }
        if (unknownNodes != null) {
            for (UnknownSchemaNodeBuilder unknown : unknownNodes) {
                leaf.addUnknownSchemaNode(unknown);
            }
        }
    }

    public static void refineContainer(ContainerSchemaNodeBuilder container, RefineHolder refine, int line) {
        Boolean presence = refine.isPresence();
        MustDefinition must = refine.getMust();
        List<UnknownSchemaNodeBuilder> unknownNodes = refine.getUnknownNodes();

        if (presence != null) {
            container.setPresence(presence);
        }
        if (must != null) {
            container.getConstraints().addMustDefinition(must);
        }
        if (unknownNodes != null) {
            for (UnknownSchemaNodeBuilder unknown : unknownNodes) {
                container.addUnknownSchemaNode(unknown);
            }
        }
    }

    public static void refineList(ListSchemaNodeBuilder list, RefineHolder refine, int line) {
        MustDefinition must = refine.getMust();
        Integer min = refine.getMinElements();
        Integer max = refine.getMaxElements();
        List<UnknownSchemaNodeBuilder> unknownNodes = refine.getUnknownNodes();

        if (must != null) {
            list.getConstraints().addMustDefinition(must);
        }
        if (min != null) {
            list.getConstraints().setMinElements(min);
        }
        if (max != null) {
            list.getConstraints().setMaxElements(max);
        }
        if (unknownNodes != null) {
            for (UnknownSchemaNodeBuilder unknown : unknownNodes) {
                list.addUnknownSchemaNode(unknown);
            }
        }
    }

    public static void refineLeafList(LeafListSchemaNodeBuilder leafList, RefineHolder refine, int line) {
        MustDefinition must = refine.getMust();
        Integer min = refine.getMinElements();
        Integer max = refine.getMaxElements();
        List<UnknownSchemaNodeBuilder> unknownNodes = refine.getUnknownNodes();

        if (must != null) {
            leafList.getConstraints().addMustDefinition(must);
        }
        if (min != null) {
            leafList.getConstraints().setMinElements(min);
        }
        if (max != null) {
            leafList.getConstraints().setMaxElements(max);
        }
        if (unknownNodes != null) {
            for (UnknownSchemaNodeBuilder unknown : unknownNodes) {
                leafList.addUnknownSchemaNode(unknown);
            }
        }
    }

    public static void refineChoice(ChoiceBuilder choice, RefineHolder refine, int line) {
        String defaultStr = refine.getDefaultStr();
        Boolean mandatory = refine.isMandatory();
        List<UnknownSchemaNodeBuilder> unknownNodes = refine.getUnknownNodes();

        if (defaultStr != null) {
            choice.setDefaultCase(defaultStr);
        }
        if (mandatory != null) {
            choice.getConstraints().setMandatory(mandatory);
        }
        if (unknownNodes != null) {
            for (UnknownSchemaNodeBuilder unknown : unknownNodes) {
                choice.addUnknownSchemaNode(unknown);
            }
        }
    }

    public static void refineAnyxml(AnyXmlBuilder anyXml, RefineHolder refine, int line) {
        Boolean mandatory = refine.isMandatory();
        MustDefinition must = refine.getMust();
        List<UnknownSchemaNodeBuilder> unknownNodes = refine.getUnknownNodes();

        if (mandatory != null) {
            anyXml.getConstraints().setMandatory(mandatory);
        }
        if (must != null) {
            anyXml.getConstraints().addMustDefinition(must);
        }
        if (unknownNodes != null) {
            for (UnknownSchemaNodeBuilder unknown : unknownNodes) {
                anyXml.addUnknownSchemaNode(unknown);
            }
        }
    }

    /**
     * Check if refine can be performed on given node.
     *
     * @param node
     *            node to refine
     * @param refine
     *            refine object containing information about refine process
     */
    public static void checkRefine(SchemaNodeBuilder node, RefineHolder refine) {
        String name = node.getQName().getLocalName();
        int line = refine.getLine();

        String defaultStr = refine.getDefaultStr();
        Boolean mandatory = refine.isMandatory();
        Boolean presence = refine.isPresence();
        MustDefinition must = refine.getMust();
        Integer min = refine.getMinElements();
        Integer max = refine.getMaxElements();

        if (node instanceof AnyXmlBuilder) {
            checkRefineDefault(node, defaultStr, line);
            checkRefinePresence(node, presence, line);
            checkRefineMinMax(name, line, min, max);
        } else if (node instanceof ChoiceBuilder) {
            checkRefinePresence(node, presence, line);
            checkRefineMust(node, must, line);
            checkRefineMinMax(name, line, min, max);
        } else if (node instanceof ContainerSchemaNodeBuilder) {
            checkRefineDefault(node, defaultStr, line);
            checkRefineMandatory(node, mandatory, line);
            checkRefineMust(node, must, line);
            checkRefineMinMax(name, line, min, max);
        } else if (node instanceof LeafSchemaNodeBuilder) {
            checkRefinePresence(node, presence, line);
            checkRefineMinMax(name, line, min, max);
        } else if (node instanceof LeafListSchemaNodeBuilder || node instanceof ListSchemaNodeBuilder) {
            checkRefineDefault(node, defaultStr, line);
            checkRefinePresence(node, presence, line);
            checkRefineMandatory(node, mandatory, line);
        } else if (node instanceof GroupingBuilder || node instanceof TypeDefinitionBuilder
                || node instanceof UsesNodeBuilder) {
            checkRefineDefault(node, defaultStr, line);
            checkRefinePresence(node, presence, line);
            checkRefineMandatory(node, mandatory, line);
            checkRefineMust(node, must, line);
            checkRefineMinMax(name, line, min, max);
        }
    }

    private static void checkRefineDefault(SchemaNodeBuilder node, String defaultStr, int line) {
        if (defaultStr != null) {
            throw new YangParseException(line, "Can not refine 'default' for '" + node.getQName().getLocalName() + "'.");
        }
    }

    private static void checkRefineMandatory(SchemaNodeBuilder node, Boolean mandatory, int line) {
        if (mandatory != null) {
            throw new YangParseException(line, "Can not refine 'mandatory' for '" + node.getQName().getLocalName()
                    + "'.");
        }
    }

    private static void checkRefinePresence(SchemaNodeBuilder node, Boolean presence, int line) {
        if (presence != null) {
            throw new YangParseException(line, "Can not refine 'presence' for '" + node.getQName().getLocalName()
                    + "'.");
        }
    }

    private static void checkRefineMust(SchemaNodeBuilder node, MustDefinition must, int line) {
        if (must != null) {
            throw new YangParseException(line, "Can not refine 'must' for '" + node.getQName().getLocalName() + "'.");
        }
    }

    private static void checkRefineMinMax(String refineTargetName, int refineLine, Integer min, Integer max) {
        if (min != null || max != null) {
            throw new YangParseException(refineLine, "Can not refine 'min-elements' or 'max-elements' for '"
                    + refineTargetName + "'.");
        }
    }

    /**
     * Perform refine operation of following parameters:
     * <ul>
     * <li>description</li>
     * <li>reference</li>
     * <li>config</li>
     * </ul>
     *
     * These parameters may be refined for any node.
     *
     * @param node
     *            node to refine
     * @param refine
     *            refine object containing information about refine process
     * @param line
     *            current line in yang model
     */
    public static void refineDefault(final Builder node, final RefineHolder refine, final int line) {
        Class<? extends Builder> cls = node.getClass();

        String description = refine.getDescription();
        if (description != null) {
            try {
                Method method = cls.getDeclaredMethod("setDescription", String.class);
                method.invoke(node, description);
            } catch (Exception e) {
                throw new YangParseException(line, "Cannot refine description in " + cls.getName(), e);
            }
        }

        String reference = refine.getReference();
        if (reference != null) {
            try {
                Method method = cls.getDeclaredMethod("setReference", String.class);
                method.invoke(node, reference);
            } catch (Exception e) {
                throw new YangParseException(line, "Cannot refine reference in " + cls.getName(), e);
            }
        }

        Boolean config = refine.isConfig();
        if (config != null) {
            try {
                Method method = cls.getDeclaredMethod("setConfiguration", Boolean.TYPE);
                method.invoke(node, config);
            } catch (Exception e) {
                throw new YangParseException(line, "Cannot refine config in " + cls.getName(), e);
            }
        }
    }

    /**
     * Perform refine operation on given node.
     *
     * @param nodeToRefine
     *            builder of node to refine
     * @param refine
     *            refine object containing information about refine process
     * @param line
     *            current line in yang model
     */
    public static void performRefine(SchemaNodeBuilder nodeToRefine, RefineHolder refine, int line) {
        checkRefine(nodeToRefine, refine);
        refineDefault(nodeToRefine, refine, line);
        if (nodeToRefine instanceof LeafSchemaNodeBuilder) {
            refineLeaf((LeafSchemaNodeBuilder) nodeToRefine, refine, line);
        } else if (nodeToRefine instanceof ContainerSchemaNodeBuilder) {
            refineContainer((ContainerSchemaNodeBuilder) nodeToRefine, refine, line);
        } else if (nodeToRefine instanceof ListSchemaNodeBuilder) {
            refineList((ListSchemaNodeBuilder) nodeToRefine, refine, line);
        } else if (nodeToRefine instanceof LeafListSchemaNodeBuilder) {
            refineLeafList((LeafListSchemaNodeBuilder) nodeToRefine, refine, line);
        } else if (nodeToRefine instanceof ChoiceBuilder) {
            refineChoice((ChoiceBuilder) nodeToRefine, refine, line);
        } else if (nodeToRefine instanceof AnyXmlBuilder) {
            refineAnyxml((AnyXmlBuilder) nodeToRefine, refine, line);
        }
    }

}
