/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.parser.impl;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.antlrv4.code.gen.YangLexer;
import org.opendaylight.controller.antlrv4.code.gen.YangParser;
import org.opendaylight.controller.model.util.UnknownType;
import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.ContainerSchemaNode;
import org.opendaylight.controller.yang.model.api.DataNodeContainer;
import org.opendaylight.controller.yang.model.api.DataSchemaNode;
import org.opendaylight.controller.yang.model.api.LeafSchemaNode;
import org.opendaylight.controller.yang.model.api.ListSchemaNode;
import org.opendaylight.controller.yang.model.api.Module;
import org.opendaylight.controller.yang.model.api.ModuleImport;
import org.opendaylight.controller.yang.model.api.SchemaNode;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.Status;
import org.opendaylight.controller.yang.model.api.TypeDefinition;
import org.opendaylight.controller.yang.model.parser.builder.impl.ModuleBuilder;

public class YangModelParserListenerTest {

    private final String testFile = "/test-model.yang";
    ModuleBuilder builder;
    Module module;


    @Before
    public void init() throws IOException {
        builder = getBuilder(testFile);
        module = builder.build();
    }

    @Test
    public void testParseModule() throws IOException {
        Set<ModuleImport> imports = module.getImports();
        assertEquals(3, imports.size());

        URI namespace = module.getNamespace();
        URI expectedNS = URI.create("urn:cisco:params:xml:ns:yang:controller:network");
        assertEquals(expectedNS, namespace);

        Date revision = module.getRevision();
        assertNull(revision);

        String prefix = module.getPrefix();
        String expectedPrefix = "topos";
        assertEquals(expectedPrefix, prefix);

        String expectedDescription = "module description";
        assertEquals(expectedDescription, module.getDescription());

        String expectedReference = "module reference";
        assertEquals(expectedReference, module.getReference());

        Set<TypeDefinition<?>> typedefs = module.getTypeDefinitions();
        assertEquals(10, typedefs.size());

        Set<DataSchemaNode> childNodes = module.getChildNodes();
        assertEquals(1, childNodes.size());

        final String containerName = "network";
        final QName containerQName = new QName(namespace, revision, prefix, containerName);
        ContainerSchemaNode tested = (ContainerSchemaNode) module.getChildNodes().iterator().next();
        DataSchemaNode container1 = module.getDataChildByName(containerName);
        DataSchemaNode container2 = module.getDataChildByName(containerQName);

        assertEquals(tested, container1);
        assertEquals(container1, container2);
    }

    @Test
    public void testParseContainer() {
        URI namespace = module.getNamespace();
        Date revision = module.getRevision();
        String prefix = module.getPrefix();
        final QName containerQName = new QName(namespace, revision, prefix, "network");

        ContainerSchemaNode tested = (ContainerSchemaNode)module.getDataChildByName(containerQName);

        Set<DataSchemaNode> containerChildNodes = tested.getChildNodes();
        assertEquals(3, containerChildNodes.size());

        String expectedDescription = "network-description";
        String expectedReference = "network-reference";
        Status expectedStatus = Status.OBSOLETE;
        testDesc_Ref_Status(tested, expectedDescription, expectedReference, expectedStatus);

        List<QName> path = new ArrayList<QName>();
        path.add(new QName(namespace, revision, prefix, "test-model"));
        path.add(containerQName);
        SchemaPath expectedSchemaPath = new SchemaPath(path, true);
        assertEquals(expectedSchemaPath, tested.getPath());

        assertTrue(tested.isConfiguration());
        assertTrue(tested.isPresenceContainer());
    }

    @Test
    public void testParseList() {
        URI namespace = module.getNamespace();
        Date revision = module.getRevision();
        String prefix = module.getPrefix();
        final QName listQName = new QName(namespace, revision, prefix, "topology");

        DataNodeContainer networkContainer = (DataNodeContainer)module.getDataChildByName("network");
        DataNodeContainer topologiesContainer = (DataNodeContainer)networkContainer.getDataChildByName("topologies");
        ListSchemaNode tested = (ListSchemaNode)topologiesContainer.getDataChildByName(listQName);
        assertEquals(listQName, tested.getQName());

        String expectedDescription = "Test description of list 'topology'.";
        String expectedReference = null;
        Status expectedStatus = Status.CURRENT;
        testDesc_Ref_Status(tested, expectedDescription, expectedReference, expectedStatus);

        List<QName> path = new ArrayList<QName>();
        path.add(new QName(namespace, revision, prefix, "test-model"));
        path.add(new QName(namespace, revision, prefix, "network"));
        path.add(new QName(namespace, revision, prefix, "topologies"));
        path.add(listQName);
        SchemaPath expectedSchemaPath = new SchemaPath(path, true);
        assertEquals(expectedSchemaPath, tested.getPath());

        List<QName> expectedKey = new ArrayList<QName>();
        expectedKey.add(new QName(namespace, revision, prefix, "topology-id"));
        assertEquals(expectedKey, tested.getKeyDefinition());

        assertEquals(Collections.EMPTY_SET, tested.getTypeDefinitions());
        assertEquals(Collections.EMPTY_SET, tested.getUses());
        assertEquals(Collections.EMPTY_SET, tested.getGroupings());

        assertTrue(tested.getDataChildByName("topology-id") instanceof LeafSchemaNode);
    }

    @Test
    public void testParseLeaf() {
        URI namespace = module.getNamespace();
        Date revision = module.getRevision();
        String prefix = module.getPrefix();
        final QName leafQName = new QName(namespace, revision, prefix, "topology-id");

        DataNodeContainer networkContainer = (DataNodeContainer)module.getDataChildByName("network");
        DataNodeContainer topologiesContainer = (DataNodeContainer)networkContainer.getDataChildByName("topologies");
        DataNodeContainer topologyList = (DataNodeContainer)topologiesContainer.getDataChildByName("topology");
        LeafSchemaNode tested = (LeafSchemaNode)topologyList.getDataChildByName(leafQName);
        assertEquals(leafQName, tested.getQName());

        String expectedDescription = "Test description of leaf 'topology-id'";
        String expectedReference = null;
        Status expectedStatus = Status.CURRENT;
        testDesc_Ref_Status(tested, expectedDescription, expectedReference, expectedStatus);

        List<QName> path = new ArrayList<QName>();
        path.add(new QName(namespace, revision, prefix, "test-model"));
        path.add(new QName(namespace, revision, prefix, "network"));
        path.add(new QName(namespace, revision, prefix, "topologies"));
        path.add(new QName(namespace, revision, prefix, "topology"));
        path.add(leafQName);
        SchemaPath expectedSchemaPath = new SchemaPath(path, true);
        assertEquals(expectedSchemaPath, tested.getPath());

        UnknownType.Builder typeBuilder = new UnknownType.Builder(new QName(namespace, revision, prefix, "topology-id"), null, null);
        TypeDefinition<?> expectedType = typeBuilder.build();
        assertEquals(expectedType, tested.getType());
    }


    private void testDesc_Ref_Status(SchemaNode tested, String expectedDescription, String expectedReference, Status expectedStatus) {
        assertEquals(expectedDescription, tested.getDescription());
        assertEquals(expectedReference, tested.getReference());
        assertEquals(expectedStatus, tested.getStatus());
    }

    private ModuleBuilder getBuilder(String fileName) throws IOException {
        final InputStream inStream = getClass().getResourceAsStream(fileName);
        ANTLRInputStream input = new ANTLRInputStream(inStream);
        final YangLexer lexer = new YangLexer(input);
        final CommonTokenStream tokens = new CommonTokenStream(lexer);
        final YangParser parser = new YangParser(tokens);

        final ParseTree tree = parser.yang();
        final ParseTreeWalker walker = new ParseTreeWalker();

        final YangModelParserListenerImpl modelParser = new YangModelParserListenerImpl();
        walker.walk(modelParser, tree);
        return modelParser.getModuleBuilder();
    }

}
