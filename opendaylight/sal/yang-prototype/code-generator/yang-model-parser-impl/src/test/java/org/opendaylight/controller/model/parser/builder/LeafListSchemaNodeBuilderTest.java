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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.model.parser.api.GroupingBuilder;
import org.opendaylight.controller.model.parser.api.UsesNodeBuilder;
import org.opendaylight.controller.model.parser.builder.LeafSchemaNodeBuilder;
import org.opendaylight.controller.model.parser.builder.MustDefinitionBuilder;
import org.opendaylight.controller.model.parser.builder.TypedefBuilder;
import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.ConstraintDefinition;
import org.opendaylight.controller.yang.model.api.GroupingDefinition;
import org.opendaylight.controller.yang.model.api.LeafSchemaNode;
import org.opendaylight.controller.yang.model.api.MustDefinition;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.Status;
import org.opendaylight.controller.yang.model.api.TypeDefinition;
import org.opendaylight.controller.yang.model.api.UsesNode;


public class LeafListSchemaNodeBuilderTest {

	private LeafSchemaNodeBuilder tested;

	private final String NAME = "test-leaf";

	private final URI namespace = URI.create("test:leaf.name");
	private final Date revision = new Date();
	private final String prefix = "x";

	private SchemaPath schemaPath;
	private final String description = "description of container";
	private final String reference = "reference";

	private QName typedefQName;
	private TypeDefinition<?> type;
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
		TypedefBuilder typeBuilder = new TypedefBuilder(typedefQName);
		type = typeBuilder.build();

		QName qname = new QName(namespace, revision, prefix, NAME);
		tested = new LeafSchemaNodeBuilder(qname);
	}

	@Test
	public void test() {
		tested.setType(type);
		tested.setPath(schemaPath);
		tested.setDescription(description);
		tested.setReference(reference);
		tested.setStatus(Status.OBSOLOTE);
		tested.setConfiguration(false);
		tested.setConstraints(constraint);
		tested.setMustDefinitionBuilder(mustBuilder);

		LeafSchemaNode result = tested.build();

		assertEquals(type, result.getType());
		assertEquals(schemaPath, result.getPath());
		assertEquals(description, result.getDescription());
		assertEquals(reference, result.getReference());
		assertEquals(Status.OBSOLOTE, result.getStatus());
		assertFalse(result.isConfiguration());
		assertEquals(constraint, result.getConstraints());
		assertEquals(must, result.getMustDefinition());
	}

}
