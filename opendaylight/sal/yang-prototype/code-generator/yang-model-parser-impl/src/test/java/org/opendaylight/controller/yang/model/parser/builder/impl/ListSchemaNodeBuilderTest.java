/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.parser.builder.impl;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.AugmentationSchema;
import org.opendaylight.controller.yang.model.api.GroupingDefinition;
import org.opendaylight.controller.yang.model.api.ListSchemaNode;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.Status;
import org.opendaylight.controller.yang.model.api.TypeDefinition;
import org.opendaylight.controller.yang.model.api.UsesNode;
import org.opendaylight.controller.yang.model.parser.builder.api.GroupingBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.UsesNodeBuilder;

public class ListSchemaNodeBuilderTest {

    private ListSchemaNodeBuilder tested;

    private static final String NAME = "test-list";

    private final URI namespace = URI.create("test:list.name");
    private final Date revision = new Date();
    private final String prefix = "x";

    private SchemaPath schemaPath;
    private final String description = "description of list";
    private final String reference = "reference";

    private QName typedefQName;
    private TypedefBuilder typeBuilder;
    @Mock
    private AugmentationSchema augment;
    @Mock
    private UsesNodeBuilder usesBuilder;
    @Mock
    private UsesNode uses;
    @Mock
    private GroupingBuilder groupingBuilder;
    @Mock
    private GroupingDefinition grouping;
    private List<QName> keyDefinition;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        when(usesBuilder.build()).thenReturn(uses);
        when(groupingBuilder.build()).thenReturn(grouping);

        schemaPath = TestUtils.createSchemaPath(true, namespace, "main", NAME);
        typedefQName = new QName(namespace, "test-type");
        typeBuilder = new TypedefBuilder(typedefQName);

        keyDefinition = new ArrayList<QName>();
        keyDefinition.add(new QName(namespace, "name"));

        QName qname = new QName(namespace, revision, prefix, NAME);
        tested = new ListSchemaNodeBuilder(qname);
    }

    @Test
    public void test() {
        tested.addTypedef(typeBuilder);
        tested.setPath(schemaPath);
        tested.setDescription(description);
        tested.setReference(reference);
        tested.setStatus(Status.OBSOLETE);
        tested.setConfiguration(false);
        tested.addUsesNode(usesBuilder);
        tested.addAugmentation(augment);
        tested.setUserOrdered(true);
        tested.setKeyDefinition(keyDefinition);

        ListSchemaNode result = tested.build();

        Set<TypeDefinition<?>> expectedTypedefs = result.getTypeDefinitions();
        assertEquals(1, expectedTypedefs.size());
        assertEquals(typedefQName, expectedTypedefs.iterator().next()
                .getQName());

        Set<AugmentationSchema> expectedAugments = new HashSet<AugmentationSchema>();
        expectedAugments.add(augment);
        assertEquals(expectedAugments, result.getAvailableAugmentations());

        assertEquals(schemaPath, result.getPath());
        assertEquals(description, result.getDescription());
        assertEquals(reference, result.getReference());
        assertEquals(Status.OBSOLETE, result.getStatus());
        assertFalse(result.isConfiguration());

        Set<UsesNode> expectedUses = new HashSet<UsesNode>();
        expectedUses.add(uses);
        assertEquals(expectedUses, result.getUses());

        assertTrue(result.isUserOrdered());
        assertEquals(keyDefinition, result.getKeyDefinition());
    }

}
