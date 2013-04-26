/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.parser.util;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.ModuleImport;
import org.opendaylight.controller.yang.model.api.MustDefinition;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.TypeDefinition;
import org.opendaylight.controller.yang.model.parser.builder.api.AugmentationSchemaBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.ChildNodeBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.DataSchemaNodeBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.GroupingBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.TypeDefinitionBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.UsesNodeBuilder;
import org.opendaylight.controller.yang.model.parser.builder.impl.AnyXmlBuilder;
import org.opendaylight.controller.yang.model.parser.builder.impl.ChoiceBuilder;
import org.opendaylight.controller.yang.model.parser.builder.impl.ChoiceCaseBuilder;
import org.opendaylight.controller.yang.model.parser.builder.impl.ConstraintsBuilder;
import org.opendaylight.controller.yang.model.parser.builder.impl.ContainerSchemaNodeBuilder;
import org.opendaylight.controller.yang.model.parser.builder.impl.LeafListSchemaNodeBuilder;
import org.opendaylight.controller.yang.model.parser.builder.impl.LeafSchemaNodeBuilder;
import org.opendaylight.controller.yang.model.parser.builder.impl.ListSchemaNodeBuilder;
import org.opendaylight.controller.yang.model.parser.builder.impl.ModuleBuilder;
import org.opendaylight.controller.yang.model.parser.builder.impl.UnknownSchemaNodeBuilder;

public final class ParserUtils {

    private ParserUtils() {
    }

    /**
     * Get module import referenced by given prefix.
     *
     * @param builder
     *            module to search
     * @param prefix
     *            prefix associated with import
     * @return ModuleImport based on given prefix
     */
    public static ModuleImport getModuleImport(final ModuleBuilder builder,
            final String prefix) {
        ModuleImport moduleImport = null;
        for (ModuleImport mi : builder.getModuleImports()) {
            if (mi.getPrefix().equals(prefix)) {
                moduleImport = mi;
                break;
            }
        }
        return moduleImport;
    }

    /**
     * Parse uses path.
     *
     * @param usesPath
     *            as String
     * @return SchemaPath from given String
     */
    public static SchemaPath parseUsesPath(final String usesPath) {
        final boolean absolute = usesPath.startsWith("/");
        final String[] splittedPath = usesPath.split("/");
        final List<QName> path = new ArrayList<QName>();
        QName name;
        for (String pathElement : splittedPath) {
            if (pathElement.length() > 0) {
                final String[] splittedElement = pathElement.split(":");
                if (splittedElement.length == 1) {
                    name = new QName(null, null, null, splittedElement[0]);
                } else {
                    name = new QName(null, null, splittedElement[0],
                            splittedElement[1]);
                }
                path.add(name);
            }
        }
        return new SchemaPath(path, absolute);
    }

    /**
     * Add all augment's child nodes to given target.
     *
     * @param augment
     * @param target
     */
    public static void fillAugmentTarget(
            final AugmentationSchemaBuilder augment,
            final ChildNodeBuilder target) {
        for (DataSchemaNodeBuilder builder : augment.getChildNodes()) {
            builder.setAugmenting(true);
            target.addChildNode(builder);
        }
    }

    public static LeafSchemaNodeBuilder copyLeafBuilder(
            final LeafSchemaNodeBuilder old) {
        final LeafSchemaNodeBuilder copy = new LeafSchemaNodeBuilder(
                old.getQName());
        final TypeDefinition<?> type = old.getType();

        if (type == null) {
            copy.setType(old.getTypedef());
        } else {
            copy.setType(type);
        }
        copy.setPath(old.getPath());
        copyConstraints(old, copy);
        for (UnknownSchemaNodeBuilder unknown : old.getUnknownNodes()) {
            copy.addUnknownSchemaNode(unknown);
        }
        copy.setDescription(old.getDescription());
        copy.setReference(old.getReference());
        copy.setStatus(old.getStatus());
        copy.setAugmenting(old.isAugmenting());
        copy.setConfiguration(old.isConfiguration());
        copy.setDefaultStr(old.getDefaultStr());
        copy.setUnits(old.getUnits());
        return copy;
    }

    public static ContainerSchemaNodeBuilder copyContainerBuilder(
            final ContainerSchemaNodeBuilder old) {
        final ContainerSchemaNodeBuilder copy = new ContainerSchemaNodeBuilder(
                old.getQName());
        copy.setPath(old.getPath());
        copyConstraints(old, copy);
        for (UnknownSchemaNodeBuilder unknown : old.getUnknownNodes()) {
            copy.addUnknownSchemaNode(unknown);
        }
        for (DataSchemaNodeBuilder child : old.getChildNodes()) {
            copy.addChildNode(child);
        }
        for (GroupingBuilder grouping : old.getGroupings()) {
            copy.addGrouping(grouping);
        }
        for (TypeDefinitionBuilder typedef : old.getTypedefs()) {
            copy.addTypedef(typedef);
        }
        for (AugmentationSchemaBuilder augment : old.getAugmentations()) {
            copy.addAugmentation(augment);
        }
        for (UsesNodeBuilder use : old.getUsesNodes()) {
            copy.addUsesNode(use);
        }
        for (UnknownSchemaNodeBuilder unknown : old.getUnknownNodes()) {
            copy.addUnknownSchemaNode(unknown);
        }
        copy.setDescription(old.getDescription());
        copy.setReference(old.getReference());
        copy.setStatus(old.getStatus());
        copy.setAugmenting(old.isAugmenting());
        copy.setConfiguration(old.isConfiguration());
        copy.setPresence(old.isPresence());
        return copy;
    }

    public static ListSchemaNodeBuilder copyListBuilder(
            final ListSchemaNodeBuilder old) {
        final ListSchemaNodeBuilder copy = new ListSchemaNodeBuilder(
                old.getQName());
        copy.setPath(old.getPath());
        copyConstraints(old, copy);
        for (UnknownSchemaNodeBuilder unknown : old.getUnknownNodes()) {
            copy.addUnknownSchemaNode(unknown);
        }
        for (DataSchemaNodeBuilder child : old.getChildNodes()) {
            copy.addChildNode(child);
        }
        for (GroupingBuilder grouping : old.getGroupings()) {
            copy.addGrouping(grouping);
        }
        for (TypeDefinitionBuilder typedef : old.getTypedefs()) {
            copy.addTypedef(typedef);
        }
        for (AugmentationSchemaBuilder augment : old.getAugmentations()) {
            copy.addAugmentation(augment);
        }
        for (UsesNodeBuilder use : old.getUsesNodes()) {
            copy.addUsesNode(use);
        }
        for (UnknownSchemaNodeBuilder unknown : old.getUnknownNodes()) {
            copy.addUnknownSchemaNode(unknown);
        }
        copy.setDescription(old.getDescription());
        copy.setReference(old.getReference());
        copy.setStatus(old.getStatus());
        copy.setAugmenting(old.isAugmenting());
        copy.setConfiguration(old.isConfiguration());
        copy.setUserOrdered(old.isUserOrdered());
        return copy;
    }

    public static LeafListSchemaNodeBuilder copyLeafListBuilder(
            final LeafListSchemaNodeBuilder old) {
        final LeafListSchemaNodeBuilder copy = new LeafListSchemaNodeBuilder(
                old.getQName());
        copy.setPath(old.getPath());
        copyConstraints(old, copy);
        final TypeDefinition<?> type = old.getType();
        if (type == null) {
            copy.setType(old.getTypedef());
        } else {
            copy.setType(type);
        }
        for (UnknownSchemaNodeBuilder unknown : old.getUnknownNodes()) {
            copy.addUnknownSchemaNode(unknown);
        }
        for (UnknownSchemaNodeBuilder unknown : old.getUnknownNodes()) {
            copy.addUnknownSchemaNode(unknown);
        }
        copy.setDescription(old.getDescription());
        copy.setReference(old.getReference());
        copy.setStatus(old.getStatus());
        copy.setAugmenting(old.isAugmenting());
        copy.setConfiguration(old.isConfiguration());
        copy.setUserOrdered(old.isUserOrdered());
        return copy;
    }

    public static ChoiceBuilder copyChoiceBuilder(final ChoiceBuilder old) {
        final ChoiceBuilder copy = new ChoiceBuilder(old.getQName());
        copy.setPath(old.getPath());
        copyConstraints(old, copy);
        for (ChoiceCaseBuilder caseBuilder : old.getCases()) {
            copy.addChildNode(caseBuilder);
        }
        for (UnknownSchemaNodeBuilder unknown : old.getUnknownNodes()) {
            copy.addUnknownSchemaNode(unknown);
        }
        for (TypeDefinitionBuilder typedef : old.getTypedefs()) {
            copy.addTypedef(typedef);
        }
        for (UsesNodeBuilder use : old.getUsesNodes()) {
            copy.addUsesNode(use);
        }
        for (UnknownSchemaNodeBuilder unknown : old.getUnknownNodes()) {
            copy.addUnknownSchemaNode(unknown);
        }
        copy.setDefaultCase(old.getDefaultCase());
        copy.setDescription(old.getDescription());
        copy.setReference(old.getReference());
        copy.setStatus(old.getStatus());
        copy.setAugmenting(old.isAugmenting());
        copy.setConfiguration(old.isConfiguration());
        return copy;
    }

    public static AnyXmlBuilder copyAnyXmlBuilder(final AnyXmlBuilder old) {
        final AnyXmlBuilder copy = new AnyXmlBuilder(old.getQName());
        copy.setPath(old.getPath());
        copyConstraints(old, copy);
        for (UnknownSchemaNodeBuilder unknown : old.getUnknownNodes()) {
            copy.addUnknownSchemaNode(unknown);
        }
        copy.setDescription(old.getDescription());
        copy.setReference(old.getReference());
        copy.setStatus(old.getStatus());
        copy.setConfiguration(old.isConfiguration());
        return copy;
    }

    private static void copyConstraints(final DataSchemaNodeBuilder oldBuilder,
            final DataSchemaNodeBuilder newBuilder) {
        final ConstraintsBuilder oldConstraints = oldBuilder.getConstraints();
        final ConstraintsBuilder newConstraints = newBuilder.getConstraints();
        newConstraints.addWhenCondition(oldConstraints.getWhenCondition());
        for (MustDefinition must : oldConstraints.getMustDefinitions()) {
            newConstraints.addMustDefinition(must);
        }
        newConstraints.setMandatory(oldConstraints.isMandatory());
        newConstraints.setMinElements(oldConstraints.getMinElements());
        newConstraints.setMaxElements(oldConstraints.getMaxElements());
    }

}
