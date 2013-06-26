/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.parser.impl;

import static org.junit.Assert.*;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.ContainerSchemaNode;
import org.opendaylight.controller.yang.model.api.DataSchemaNode;
import org.opendaylight.controller.yang.model.api.GroupingDefinition;
import org.opendaylight.controller.yang.model.api.IdentitySchemaNode;
import org.opendaylight.controller.yang.model.api.LeafSchemaNode;
import org.opendaylight.controller.yang.model.api.ListSchemaNode;
import org.opendaylight.controller.yang.model.api.Module;
import org.opendaylight.controller.yang.model.api.MustDefinition;
import org.opendaylight.controller.yang.model.api.SchemaContext;
import org.opendaylight.controller.yang.model.api.SchemaNode;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.TypeDefinition;
import org.opendaylight.controller.yang.model.api.UnknownSchemaNode;
import org.opendaylight.controller.yang.model.api.UsesNode;
import org.opendaylight.controller.yang.model.api.type.RangeConstraint;
import org.opendaylight.controller.yang.model.util.ExtendedType;

import com.google.common.collect.Lists;

public class YangParserWithContextTest {
    private final DateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final YangParserImpl parser = new YangParserImpl();

    @Test
    public void testTypeFromContext() throws Exception {
        SchemaContext context = null;
        String resource = "/types/ietf-inet-types@2010-09-24.yang";
        InputStream stream = new FileInputStream(getClass().getResource(resource).getPath());
        context = parser.resolveSchemaContext(TestUtils.loadModules(Lists.newArrayList(stream)));
        stream.close();

        Module module = null;
        resource = "/context-test/test1.yang";
        InputStream stream2 = new FileInputStream(getClass().getResource(resource).getPath());
        module = TestUtils.loadModuleWithContext(stream2, context);
        stream2.close();
        assertNotNull(module);

        LeafSchemaNode leaf = (LeafSchemaNode) module.getDataChildByName("id");

        ExtendedType leafType = (ExtendedType) leaf.getType();
        QName qname = leafType.getQName();
        assertEquals(URI.create("urn:simple.demo.test1"), qname.getNamespace());
        assertEquals(simpleDateFormat.parse("2013-06-18"), qname.getRevision());
        assertEquals("t1", qname.getPrefix());
        assertEquals("port-number", qname.getLocalName());

        ExtendedType leafBaseType = (ExtendedType) leafType.getBaseType();
        qname = leafBaseType.getQName();
        assertEquals(URI.create("urn:ietf:params:xml:ns:yang:ietf-inet-types"), qname.getNamespace());
        assertEquals(simpleDateFormat.parse("2010-09-24"), qname.getRevision());
        assertEquals("inet", qname.getPrefix());
        assertEquals("port-number", qname.getLocalName());

        ExtendedType dscpExt = (ExtendedType)TestUtils.findTypedef(module.getTypeDefinitions(), "dscp-ext");
        List<RangeConstraint> ranges = dscpExt.getRanges();
        assertEquals(1, ranges.size());
        RangeConstraint range = ranges.get(0);
        assertEquals(0L, range.getMin());
        assertEquals(63L, range.getMax());

    }

    @Test
    public void testUsesGroupingFromContext() throws Exception {
        SchemaContext context = null;
        try (InputStream stream = new FileInputStream(getClass().getResource("/model/testfile2.yang").getPath())) {
            context = parser.resolveSchemaContext(TestUtils.loadModules(Lists.newArrayList(stream)));
        }
        Module module = null;
        try (InputStream stream = new FileInputStream(getClass().getResource("/context-test/test2.yang").getPath())) {
            module = TestUtils.loadModuleWithContext(stream, context);
        }
        assertNotNull(module);

        ContainerSchemaNode peer = (ContainerSchemaNode) module.getDataChildByName("peer");
        ContainerSchemaNode destination = (ContainerSchemaNode) peer.getDataChildByName("destination");
        Set<UsesNode> usesNodes = destination.getUses();
        assertEquals(1, usesNodes.size());
        UsesNode usesNode = usesNodes.iterator().next();

        // test grouping path
        List<QName> path = new ArrayList<QName>();
        QName qname = new QName(URI.create("urn:simple.types.data.demo"), simpleDateFormat.parse("2013-02-27"), "t2",
                "target");
        path.add(qname);
        SchemaPath expectedPath = new SchemaPath(path, true);
        assertEquals(expectedPath, usesNode.getGroupingPath());

        // test refine
        Map<SchemaPath, SchemaNode> refines = usesNode.getRefines();
        assertEquals(5, refines.size());

        LeafSchemaNode refineLeaf = null;
        ContainerSchemaNode refineContainer = null;
        ListSchemaNode refineList = null;
        GroupingDefinition refineGrouping = null;
        TypeDefinition<?> typedef = null;
        for (Map.Entry<SchemaPath, SchemaNode> entry : refines.entrySet()) {
            SchemaNode value = entry.getValue();
            if (value instanceof LeafSchemaNode) {
                refineLeaf = (LeafSchemaNode) value;
            } else if (value instanceof ContainerSchemaNode) {
                refineContainer = (ContainerSchemaNode) value;
            } else if (value instanceof ListSchemaNode) {
                refineList = (ListSchemaNode) value;
            } else if (value instanceof GroupingDefinition) {
                refineGrouping = (GroupingDefinition) value;
            } else if (value instanceof TypeDefinition<?>) {
                typedef = (TypeDefinition<?>) value;
            }
        }

        // leaf address
        assertNotNull(refineLeaf);
        assertEquals("address", refineLeaf.getQName().getLocalName());
        assertEquals("description of address defined by refine", refineLeaf.getDescription());
        assertEquals("address reference added by refine", refineLeaf.getReference());
        assertFalse(refineLeaf.isConfiguration());
        assertTrue(refineLeaf.getConstraints().isMandatory());
        Set<MustDefinition> leafMustConstraints = refineLeaf.getConstraints().getMustConstraints();
        assertEquals(1, leafMustConstraints.size());
        MustDefinition leafMust = leafMustConstraints.iterator().next();
        assertEquals("\"ifType != 'ethernet' or (ifType = 'ethernet' and ifMTU = 1500)\"", leafMust.toString());

        // container port
        assertNotNull(refineContainer);
        Set<MustDefinition> mustConstraints = refineContainer.getConstraints().getMustConstraints();
        assertTrue(mustConstraints.isEmpty());
        assertEquals("description of port defined by refine", refineContainer.getDescription());
        assertEquals("port reference added by refine", refineContainer.getReference());
        assertFalse(refineContainer.isConfiguration());
        assertTrue(refineContainer.isPresenceContainer());

        // list addresses
        assertNotNull(refineList);
        assertEquals("description of addresses defined by refine", refineList.getDescription());
        assertEquals("addresses reference added by refine", refineList.getReference());
        assertFalse(refineList.isConfiguration());
        assertEquals(2, (int) refineList.getConstraints().getMinElements());
        assertEquals(12, (int) refineList.getConstraints().getMaxElements());

        // grouping target-inner
        assertNotNull(refineGrouping);
        Set<DataSchemaNode> refineGroupingChildren = refineGrouping.getChildNodes();
        assertEquals(1, refineGroupingChildren.size());
        LeafSchemaNode refineGroupingLeaf = (LeafSchemaNode) refineGroupingChildren.iterator().next();
        assertEquals("inner-grouping-id", refineGroupingLeaf.getQName().getLocalName());
        assertEquals("new target-inner grouping description", refineGrouping.getDescription());

        // typedef group-type
        assertNotNull(typedef);
        assertEquals("new group-type description", typedef.getDescription());
        assertEquals("new group-type reference", typedef.getReference());
        assertTrue(typedef.getBaseType() instanceof ExtendedType);
    }

    @Test
    public void testIdentity() throws Exception {
        SchemaContext context = null;
        try (InputStream stream = new FileInputStream(getClass().getResource("/types/custom-types-test@2012-4-4.yang")
                .getPath())) {
            context = parser.resolveSchemaContext(TestUtils.loadModules(Lists.newArrayList(stream)));
        }
        Module module = null;
        try (InputStream stream = new FileInputStream(getClass().getResource("/context-test/test3.yang").getPath())) {
            module = TestUtils.loadModuleWithContext(stream, context);
        }
        assertNotNull(module);

        Set<IdentitySchemaNode> identities = module.getIdentities();
        assertEquals(1, identities.size());

        IdentitySchemaNode identity = identities.iterator().next();
        QName idQName = identity.getQName();
        assertEquals(URI.create("urn:simple.demo.test3"), idQName.getNamespace());
        assertEquals(simpleDateFormat.parse("2013-06-18"), idQName.getRevision());
        assertEquals("t3", idQName.getPrefix());
        assertEquals("pt", idQName.getLocalName());

        IdentitySchemaNode baseIdentity = identity.getBaseIdentity();
        QName idBaseQName = baseIdentity.getQName();
        assertEquals(URI.create("urn:simple.container.demo"), idBaseQName.getNamespace());
        assertEquals(simpleDateFormat.parse("2012-04-16"), idBaseQName.getRevision());
        assertEquals("iit", idBaseQName.getPrefix());
        assertEquals("service-type", idBaseQName.getLocalName());
    }

    @Test
    public void testUnknownNodes() throws Exception {
        SchemaContext context = null;
        try (InputStream stream = new FileInputStream(getClass().getResource("/types/custom-types-test@2012-4-4.yang").getPath())) {
            context = parser.resolveSchemaContext(TestUtils.loadModules(Lists.newArrayList(stream)));
        }

        Module module = null;
        try (InputStream stream = new FileInputStream(getClass().getResource("/context-test/test3.yang").getPath())) {
            module = TestUtils.loadModuleWithContext(stream, context);
        }

        ContainerSchemaNode network = (ContainerSchemaNode) module.getDataChildByName("network");
        List<UnknownSchemaNode> unknownNodes = network.getUnknownSchemaNodes();
        assertEquals(1, unknownNodes.size());

        UnknownSchemaNode un = unknownNodes.iterator().next();
        QName unType = un.getNodeType();
        assertEquals(URI.create("urn:simple.container.demo"), unType.getNamespace());
        assertEquals(simpleDateFormat.parse("2012-04-16"), unType.getRevision());
        assertEquals("custom", unType.getPrefix());
        assertEquals("mountpoint", unType.getLocalName());
        assertEquals("point", un.getNodeParameter());
    }

    @Test
    public void testAugment() throws Exception {
        // load first module
        SchemaContext context = null;
        String resource = "/context-augment-test/test4.yang";

        try (InputStream stream = new FileInputStream(getClass().getResource(resource).getPath())) {
            context = parser.resolveSchemaContext(TestUtils.loadModules(Lists.newArrayList(stream)));
        }

        Set<Module> contextModules = context.getModules();
        Module t3 = TestUtils.findModule(contextModules, "test4");
        ContainerSchemaNode interfaces = (ContainerSchemaNode) t3.getDataChildByName("interfaces");
        ListSchemaNode ifEntry = (ListSchemaNode) interfaces.getDataChildByName("ifEntry");

        // load another modules and parse them against already existing context
        Set<Module> modules = null;
        try (InputStream stream1 = new FileInputStream(getClass().getResource("/context-augment-test/test1.yang")
                .getPath());
                InputStream stream2 = new FileInputStream(getClass().getResource("/context-augment-test/test2.yang")
                        .getPath());
                InputStream stream3 = new FileInputStream(getClass().getResource("/context-augment-test/test3.yang")
                        .getPath())) {
            List<InputStream> input = Lists.newArrayList(stream1, stream2, stream3);
            modules = TestUtils.loadModulesWithContext(input, context);
        }
        assertNotNull(modules);

        // test augmentation process
        ContainerSchemaNode augmentHolder = (ContainerSchemaNode) ifEntry.getDataChildByName("augment-holder");
        assertNotNull(augmentHolder);
        DataSchemaNode ds0 = augmentHolder.getDataChildByName("ds0ChannelNumber");
        assertNotNull(ds0);
        DataSchemaNode interfaceId = augmentHolder.getDataChildByName("interface-id");
        assertNotNull(interfaceId);
        DataSchemaNode higherLayerIf = augmentHolder.getDataChildByName("higher-layer-if");
        assertNotNull(higherLayerIf);
        ContainerSchemaNode schemas = (ContainerSchemaNode) augmentHolder.getDataChildByName("schemas");
        assertNotNull(schemas);
        assertNotNull(schemas.getDataChildByName("id"));

        // test augment target after augmentation: check if it is same instance
        ListSchemaNode ifEntryAfterAugment = (ListSchemaNode) interfaces.getDataChildByName("ifEntry");
        assertTrue(ifEntry == ifEntryAfterAugment);
    }

}
