/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.model.parser.builder;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.model.parser.api.GroupingBuilder;
import org.opendaylight.controller.model.parser.api.UsesNodeBuilder;
import org.opendaylight.controller.model.parser.builder.ContainerSchemaNodeBuilder;
import org.opendaylight.controller.model.parser.builder.MustDefinitionBuilder;
import org.opendaylight.controller.model.parser.builder.TypedefBuilder;
import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.AugmentationSchema;
import org.opendaylight.controller.yang.model.api.ConstraintDefinition;
import org.opendaylight.controller.yang.model.api.ContainerSchemaNode;
import org.opendaylight.controller.yang.model.api.GroupingDefinition;
import org.opendaylight.controller.yang.model.api.MustDefinition;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.Status;
import org.opendaylight.controller.yang.model.api.TypeDefinition;
import org.opendaylight.controller.yang.model.api.UsesNode;


public class ContainerSchemaNodeBuilderTest {

	private ContainerSchemaNodeBuilder tested;

	private final String NAME = "test-container";

	private final URI namespace = URI.create("test:container.name");
	private final Date revision = new Date();
	private final String prefix = "x";

	private SchemaPath schemaPath;
	private final String description = "description of container";
	private final String reference = "reference";

	private QName typedefQName;
	private TypedefBuilder typeBuilder;
	@Mock private AugmentationSchema augment;
	@Mock private ConstraintDefinition constraint;
	@Mock private UsesNodeBuilder usesBuilder;
	@Mock private UsesNode uses;
	@Mock private MustDefinitionBuilder mustBuilder;
	@Mock private MustDefinition must;
	@Mock private GroupingBuilder groupingBuilder;
	@Mock private GroupingDefinition grouping;

	@Before
	public void init() {
		MockitoAnnotations.initMocks(this);
		when(usesBuilder.build()).thenReturn(uses);
		when(mustBuilder.build()).thenReturn(must);
		when(groupingBuilder.build()).thenReturn(grouping);

		schemaPath = TestUtils.createSchemaPath(true, namespace, "main", "interface");
		typedefQName = new QName(namespace, "test-type");
		typeBuilder = new TypedefBuilder(typedefQName);

		QName qname = new QName(namespace, revision, prefix, NAME);
		tested = new ContainerSchemaNodeBuilder(qname);
	}

	@Test
	public void test() {
		tested.addTypedef(typeBuilder);
		tested.setPath(schemaPath);
		tested.setDescription(description);
		tested.setReference(reference);
		tested.setStatus(Status.OBSOLOTE);
		tested.setConfiguration(false);
		tested.setConstraints(constraint);
		tested.addUsesNode(usesBuilder);
		tested.addAugmentation(augment);
		tested.setMustDefinitionBuilder(mustBuilder);
		tested.setPresenceContainer(true);

		ContainerSchemaNode result = tested.build();

		Set<TypeDefinition<?>> expectedTypedefs = result.getTypeDefinitions();
		assertEquals(1, expectedTypedefs.size());
		assertEquals(typedefQName, expectedTypedefs.iterator().next().getQName());

		Set<AugmentationSchema> expectedAugments = new HashSet<AugmentationSchema>();
		expectedAugments.add(augment);
		assertEquals(expectedAugments, result.getAvailableAugmentations());

		assertEquals(schemaPath, result.getPath());
		assertEquals(description, result.getDescription());
		assertEquals(reference, result.getReference());
		assertEquals(Status.OBSOLOTE, result.getStatus());
		assertFalse(result.isConfiguration());
		assertEquals(constraint, result.getConstraints());

		Set<UsesNode> expectedUses = new HashSet<UsesNode>();
		expectedUses.add(uses);
		assertEquals(expectedUses, result.getUses());

		assertTrue(result.isPresenceContainer());
		assertEquals(must, result.getMustDefinition());
	}

}
