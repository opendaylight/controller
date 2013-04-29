/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.parser.impl;

import static org.junit.Assert.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.AnyXmlSchemaNode;
import org.opendaylight.controller.yang.model.api.AugmentationSchema;
import org.opendaylight.controller.yang.model.api.ChoiceCaseNode;
import org.opendaylight.controller.yang.model.api.ChoiceNode;
import org.opendaylight.controller.yang.model.api.ConstraintDefinition;
import org.opendaylight.controller.yang.model.api.ContainerSchemaNode;
import org.opendaylight.controller.yang.model.api.Deviation;
import org.opendaylight.controller.yang.model.api.Deviation.Deviate;
import org.opendaylight.controller.yang.model.api.ExtensionDefinition;
import org.opendaylight.controller.yang.model.api.FeatureDefinition;
import org.opendaylight.controller.yang.model.api.LeafSchemaNode;
import org.opendaylight.controller.yang.model.api.ListSchemaNode;
import org.opendaylight.controller.yang.model.api.Module;
import org.opendaylight.controller.yang.model.api.ModuleImport;
import org.opendaylight.controller.yang.model.api.MustDefinition;
import org.opendaylight.controller.yang.model.api.NotificationDefinition;
import org.opendaylight.controller.yang.model.api.RpcDefinition;
import org.opendaylight.controller.yang.model.api.SchemaNode;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.Status;
import org.opendaylight.controller.yang.model.api.TypeDefinition;
import org.opendaylight.controller.yang.model.api.UnknownSchemaNode;
import org.opendaylight.controller.yang.model.api.UsesNode;
import org.opendaylight.controller.yang.model.api.type.LengthConstraint;
import org.opendaylight.controller.yang.model.api.type.PatternConstraint;
import org.opendaylight.controller.yang.model.api.type.RangeConstraint;
import org.opendaylight.controller.yang.model.util.Decimal64;
import org.opendaylight.controller.yang.model.util.ExtendedType;
import org.opendaylight.controller.yang.model.util.Int16;
import org.opendaylight.controller.yang.model.util.Int32;
import org.opendaylight.controller.yang.model.util.StringType;
import org.opendaylight.controller.yang.model.util.Uint32;
import org.opendaylight.controller.yang.model.util.UnionType;

public class YangModelParserTest {
    private Set<Module> modules;

    @Before
    public void init() {
        modules = TestUtils.loadModules("src/test/resources/model");
        assertEquals(3, modules.size());
    }

    @Test
    public void testHeaders() {
        Module test = TestUtils.findModule(modules, "types1");

        assertEquals("types1", test.getName());
        assertEquals("1", test.getYangVersion());
        URI expectedNamespace = URI.create("urn:simple.container.demo");
        assertEquals(expectedNamespace, test.getNamespace());
        assertEquals("t1", test.getPrefix());

        Set<ModuleImport> imports = test.getImports();
        assertEquals(2, imports.size());

        ModuleImport import2 = TestUtils.findImport(imports, "data");
        assertEquals("types2", import2.getModuleName());
        assertEquals(TestUtils.createDate("2013-02-27"), import2.getRevision());

        ModuleImport import3 = TestUtils.findImport(imports, "t3");
        assertEquals("types3", import3.getModuleName());
        assertEquals(TestUtils.createDate("2013-02-27"), import3.getRevision());

        assertEquals("opendaylight", test.getOrganization());
        assertEquals("http://www.opendaylight.org/", test.getContact());
        Date expectedRevision = TestUtils.createDate("2013-02-27");
        assertEquals(expectedRevision, test.getRevision());
        assertEquals(" WILL BE DEFINED LATER", test.getReference());
    }

    @Test
    public void testParseContainer() {
        Module test = TestUtils.findModule(modules, "types2");
        URI expectedNamespace = URI.create("urn:simple.types.data.demo");
        String expectedPrefix = "t2";
        Date expectedRevision = TestUtils.createDate("2013-02-27");

        ContainerSchemaNode interfaces = (ContainerSchemaNode) test
                .getDataChildByName("interfaces");
        // test SchemaNode args
        QName expectedQName = new QName(expectedNamespace, expectedRevision,
                expectedPrefix, "interfaces");
        assertEquals(expectedQName, interfaces.getQName());
        SchemaPath expectedPath = TestUtils.createPath(true, expectedNamespace,
                expectedRevision, expectedPrefix, "interfaces");
        assertEquals(expectedPath, interfaces.getPath());
        assertNull(interfaces.getDescription());
        assertNull(interfaces.getReference());
        assertEquals(Status.CURRENT, interfaces.getStatus());
        assertEquals(0, interfaces.getUnknownSchemaNodes().size());
        // test DataSchemaNode args
        assertFalse(interfaces.isAugmenting());
        assertFalse(interfaces.isConfiguration());
        ConstraintDefinition constraints = interfaces.getConstraints();
        assertNull(constraints.getWhenCondition());
        assertEquals(0, constraints.getMustConstraints().size());
        assertFalse(constraints.isMandatory());
        assertNull(constraints.getMinElements());
        assertNull(constraints.getMaxElements());
        // test AugmentationTarget args
        assertEquals(0, interfaces.getAvailableAugmentations().size());
        // test ContainerSchemaNode args
        assertFalse(interfaces.isPresenceContainer());
        // test DataNodeContainer args
        assertEquals(0, interfaces.getTypeDefinitions().size());
        assertEquals(1, interfaces.getChildNodes().size());
        assertEquals(0, interfaces.getGroupings().size());
        assertEquals(0, interfaces.getUses().size());

        ListSchemaNode ifEntry = (ListSchemaNode) interfaces
                .getDataChildByName("ifEntry");
        assertNotNull(ifEntry);
    }

    @Test
    public void testParseList() {
        Module test = TestUtils.findModule(modules, "types2");
        URI expectedNamespace = URI.create("urn:simple.types.data.demo");
        String expectedPrefix = "t2";
        Date expectedRevision = TestUtils.createDate("2013-02-27");

        ContainerSchemaNode interfaces = (ContainerSchemaNode) test
                .getDataChildByName("interfaces");

        ListSchemaNode ifEntry = (ListSchemaNode) interfaces
                .getDataChildByName("ifEntry");
        // test SchemaNode args
        QName expectedQName = new QName(expectedNamespace, expectedRevision,
                expectedPrefix, "ifEntry");
        assertEquals(expectedQName, ifEntry.getQName());
        SchemaPath expectedPath = TestUtils.createPath(true, expectedNamespace,
                expectedRevision, expectedPrefix, "interfaces", "ifEntry");
        assertEquals(expectedPath, ifEntry.getPath());
        assertNull(ifEntry.getDescription());
        assertNull(ifEntry.getReference());
        assertEquals(Status.CURRENT, ifEntry.getStatus());
        assertEquals(0, ifEntry.getUnknownSchemaNodes().size());
        // test DataSchemaNode args
        assertFalse(ifEntry.isAugmenting());
        assertFalse(ifEntry.isConfiguration());
        ConstraintDefinition constraints = ifEntry.getConstraints();
        assertNull(constraints.getWhenCondition());
        assertEquals(0, constraints.getMustConstraints().size());
        assertFalse(constraints.isMandatory());
        assertNull(constraints.getMinElements());
        assertNull(constraints.getMaxElements());
        // test AugmentationTarget args
        Set<AugmentationSchema> availableAugmentations = ifEntry
                .getAvailableAugmentations();
        assertEquals(2, availableAugmentations.size());
        AugmentationSchema augment = availableAugmentations.iterator().next();
        ContainerSchemaNode augmentHolder = (ContainerSchemaNode) augment
                .getDataChildByName("augment-holder");
        assertNotNull(augmentHolder);
        // test ListSchemaNode args
        List<QName> expectedKey = new ArrayList<QName>();
        expectedKey.add(new QName(expectedNamespace, expectedRevision,
                expectedPrefix, "ifIndex"));
        assertEquals(expectedKey, ifEntry.getKeyDefinition());
        assertFalse(ifEntry.isUserOrdered());
        // test DataNodeContainer args
        assertEquals(0, ifEntry.getTypeDefinitions().size());
        assertEquals(4, ifEntry.getChildNodes().size());
        assertEquals(0, ifEntry.getGroupings().size());
        assertEquals(0, ifEntry.getUses().size());

        LeafSchemaNode ifIndex = (LeafSchemaNode) ifEntry
                .getDataChildByName("ifIndex");
        assertEquals(new Uint32(), ifIndex.getType());
        LeafSchemaNode ifMtu = (LeafSchemaNode) ifEntry
                .getDataChildByName("ifMtu");
        assertEquals(new Int32(), ifMtu.getType());
    }

    @Test
    public void testAugmentResolving() {
        // testfile1
        Module module1 = TestUtils.findModule(modules, "types1");

        Set<AugmentationSchema> module1Augmentations = module1
                .getAugmentations();
        AugmentationSchema augment1 = module1Augmentations.iterator().next();
        LeafSchemaNode augmentedLeafDefinition = (LeafSchemaNode) augment1
                .getDataChildByName("ds0ChannelNumber");
        assertTrue(augmentedLeafDefinition.isAugmenting());

        // testfile2
        Module module2 = TestUtils.findModule(modules, "types2");

        ContainerSchemaNode interfaces = (ContainerSchemaNode) module2
                .getDataChildByName("interfaces");
        ListSchemaNode ifEntry = (ListSchemaNode) interfaces
                .getDataChildByName("ifEntry");
        ContainerSchemaNode augmentedContainer = (ContainerSchemaNode) ifEntry
                .getDataChildByName("augment-holder");
        Set<AugmentationSchema> augmentedContainerAugments = augmentedContainer
                .getAvailableAugmentations();
        LeafSchemaNode augmentedLeaf = (LeafSchemaNode) augmentedContainer
                .getDataChildByName("ds0ChannelNumber");
        assertTrue(augmentedLeaf.isAugmenting());
        assertEquals(augmentedLeafDefinition, augmentedLeaf);

        Set<AugmentationSchema> ifEntryAugments = ifEntry
                .getAvailableAugmentations();
        assertEquals(2, ifEntryAugments.size());

        // testfile3
        Module module3 = TestUtils.findModule(modules, "types3");

        Set<AugmentationSchema> module3Augmentations = module3
                .getAugmentations();
        assertEquals(2, module3Augmentations.size());
        AugmentationSchema augment3 = module3Augmentations.iterator().next();
        ContainerSchemaNode augmentedContainerDefinition = (ContainerSchemaNode) augment3
                .getDataChildByName("augment-holder");
        assertTrue(augmentedContainerDefinition.isAugmenting());

        // check
        assertEquals(augmentedContainer, augmentedContainerDefinition);
        assertEquals(augmentedContainerAugments.iterator().next(), augment1);

        assertEquals(augmentedLeaf, augmentedLeafDefinition);
        assertEquals(ifEntryAugments.iterator().next(), augment3);
    }

    @Test
    public void testAugmentTarget() {
        Module test = TestUtils.findModule(modules, "types2");

        ContainerSchemaNode interfaces = (ContainerSchemaNode) test
                .getDataChildByName("interfaces");
        ListSchemaNode ifEntry = (ListSchemaNode) interfaces
                .getDataChildByName("ifEntry");
        Set<AugmentationSchema> augmentations = ifEntry
                .getAvailableAugmentations();
        assertEquals(2, augmentations.size());

        AugmentationSchema augment = augmentations.iterator().next();

        ContainerSchemaNode augmentHolder = (ContainerSchemaNode) augment
                .getDataChildByName("augment-holder");
        assertNotNull(augmentHolder);
        assertTrue(augmentHolder.isAugmenting());
        QName augmentHolderQName = augmentHolder.getQName();
        assertEquals("augment-holder", augmentHolderQName.getLocalName());
        assertEquals("t3", augmentHolderQName.getPrefix());
        assertEquals("Description for augment holder",
                augmentHolder.getDescription());
    }

    @Test
    public void testTypedefRangesResolving() {
        Module testModule = TestUtils.findModule(modules, "types1");

        LeafSchemaNode testleaf = (LeafSchemaNode) testModule
                .getDataChildByName("testleaf");
        ExtendedType leafType = (ExtendedType) testleaf.getType();
        assertEquals("my-type1", leafType.getQName().getLocalName());
        assertEquals("t2", leafType.getQName().getPrefix());
        ExtendedType baseType = (ExtendedType) leafType.getBaseType();
        assertEquals("my-base-int32-type", baseType.getQName().getLocalName());
        assertEquals("t2", baseType.getQName().getPrefix());

        List<RangeConstraint> ranges = leafType.getRanges();
        assertEquals(1, ranges.size());
        RangeConstraint range = ranges.get(0);
        assertEquals(11L, range.getMin());
        assertEquals(20L, range.getMax());
    }

    @Test
    public void testTypedefPatternsResolving() {
        Module testModule = TestUtils.findModule(modules, "types1");

        LeafSchemaNode testleaf = (LeafSchemaNode) testModule
                .getDataChildByName("test-string-leaf");
        ExtendedType testleafType = (ExtendedType) testleaf.getType();
        QName testleafTypeQName = testleafType.getQName();
        assertEquals("my-string-type-ext", testleafTypeQName.getLocalName());
        assertEquals("t2", testleafTypeQName.getPrefix());

        Set<String> expectedRegex = new HashSet<String>();
        expectedRegex.add("[a-k]*");
        expectedRegex.add("[b-u]*");
        expectedRegex.add("[e-z]*");

        Set<String> actualRegex = new HashSet<String>();
        List<PatternConstraint> patterns = testleafType.getPatterns();
        assertEquals(3, patterns.size());
        for (PatternConstraint pc : patterns) {
            actualRegex.add(pc.getRegularExpression());
        }
        assertEquals(expectedRegex, actualRegex);

        TypeDefinition<?> baseType = testleafType.getBaseType();
        assertEquals("my-string-type2", baseType.getQName().getLocalName());

        List<LengthConstraint> lengths = testleafType.getLengths();
        assertEquals(1, lengths.size());

        LengthConstraint length = lengths.get(0);
        assertEquals(5L, length.getMin());
        assertEquals(10L, length.getMax());
    }

    @Test
    public void testTypedefLengthsResolving() {
        Module testModule = TestUtils.findModule(modules, "types1");

        LeafSchemaNode testleaf = (LeafSchemaNode) testModule
                .getDataChildByName("leaf-with-length");
        ExtendedType testleafType = (ExtendedType) testleaf.getType();
        assertEquals("my-string-type", testleafType.getQName().getLocalName());

        List<LengthConstraint> lengths = testleafType.getLengths();
        assertEquals(1, lengths.size());

        LengthConstraint length = lengths.get(0);
        assertEquals(7L, length.getMin());
        assertEquals(10L, length.getMax());
    }

    @Test
    public void testTypeDef() {
        Module testModule = TestUtils.findModule(modules, "types2");

        LeafSchemaNode testleaf = (LeafSchemaNode) testModule
                .getDataChildByName("nested-type-leaf");
        ExtendedType testleafType = (ExtendedType) testleaf.getType();
        assertEquals("my-type1", testleafType.getQName().getLocalName());

        ExtendedType baseType = (ExtendedType) testleafType.getBaseType();
        assertEquals("my-base-int32-type", baseType.getQName().getLocalName());

        Int32 int32base = (Int32) baseType.getBaseType();
        List<RangeConstraint> ranges = int32base.getRangeStatements();
        assertEquals(1, ranges.size());
        RangeConstraint range = ranges.get(0);
        assertEquals(2L, range.getMin());
        assertEquals(20L, range.getMax());
    }

    @Test
    public void testTypedefDecimal1() {
        Module testModule = TestUtils.findModule(modules, "types1");

        LeafSchemaNode testleaf = (LeafSchemaNode) testModule
                .getDataChildByName("test-decimal-leaf");
        ExtendedType type = (ExtendedType) testleaf.getType();
        assertEquals(4, (int)type.getFractionDigits());

        Decimal64 baseType = (Decimal64) type.getBaseType();
        assertEquals(6, (int) baseType.getFractionDigits());
    }

    @Test
    public void testTypedefDecimal2() {
        Module testModule = TestUtils.findModule(modules, "types1");

        LeafSchemaNode testleaf = (LeafSchemaNode) testModule
                .getDataChildByName("test-decimal-leaf2");
        TypeDefinition<?> baseType = testleaf.getType().getBaseType();
        assertTrue(testleaf.getType().getBaseType() instanceof Decimal64);
        Decimal64 baseTypeCast = (Decimal64) baseType;
        assertEquals(5, (int) baseTypeCast.getFractionDigits());
    }

    @Test
    public void testTypedefUnion() {
        Module testModule = TestUtils.findModule(modules, "types1");

        LeafSchemaNode testleaf = (LeafSchemaNode) testModule
                .getDataChildByName("union-leaf");
        ExtendedType testleafType = (ExtendedType) testleaf.getType();
        assertEquals("my-union-ext", testleafType.getQName().getLocalName());

        ExtendedType baseType = (ExtendedType) testleafType.getBaseType();
        assertEquals("my-union", baseType.getQName().getLocalName());

        UnionType unionBase = (UnionType) baseType.getBaseType();

        List<TypeDefinition<?>> unionTypes = unionBase.getTypes();
        Int16 unionType1 = (Int16) unionTypes.get(0);
        List<RangeConstraint> ranges = unionType1.getRangeStatements();
        assertEquals(1, ranges.size());
        RangeConstraint range = ranges.get(0);
        assertEquals(1L, range.getMin());
        assertEquals(100L, range.getMax());

        assertTrue(unionTypes.get(0) instanceof Int16);
        assertTrue(unionTypes.get(1) instanceof Int32);
    }

    @Test
    public void testNestedUnionResolving1() {
        Module testModule = TestUtils.findModule(modules, "types1");

        LeafSchemaNode testleaf = (LeafSchemaNode) testModule
                .getDataChildByName("nested-union-leaf");

        ExtendedType nestedUnion1 = (ExtendedType) testleaf.getType();
        assertEquals("nested-union1", nestedUnion1.getQName().getLocalName());

        ExtendedType nestedUnion2 = (ExtendedType) nestedUnion1.getBaseType();
        assertEquals("nested-union2", nestedUnion2.getQName().getLocalName());

        UnionType unionType1 = (UnionType) nestedUnion2.getBaseType();
        List<TypeDefinition<?>> unionTypes = unionType1.getTypes();
        assertEquals(2, unionTypes.size());
        assertTrue(unionTypes.get(0) instanceof StringType);
        assertTrue(unionTypes.get(1) instanceof ExtendedType);

        ExtendedType extendedUnion = (ExtendedType) unionTypes.get(1);
        ExtendedType extendedUnionBase = (ExtendedType) extendedUnion
                .getBaseType();
        assertEquals("my-union", extendedUnionBase.getQName().getLocalName());

        UnionType extendedTargetUnion = (UnionType) extendedUnionBase
                .getBaseType();
        List<TypeDefinition<?>> extendedTargetTypes = extendedTargetUnion
                .getTypes();
        assertTrue(extendedTargetTypes.get(0) instanceof Int16);
        assertTrue(extendedTargetTypes.get(1) instanceof Int32);

        Int16 int16 = (Int16) extendedTargetTypes.get(0);
        List<RangeConstraint> ranges = int16.getRangeStatements();
        assertEquals(1, ranges.size());
        RangeConstraint range = ranges.get(0);
        assertEquals(1L, range.getMin());
        assertEquals(100L, range.getMax());
    }

    @Test
    public void testNestedUnionResolving2() {
        Module testModule = TestUtils.findModule(modules, "types1");

        LeafSchemaNode testleaf = (LeafSchemaNode) testModule
                .getDataChildByName("custom-union-leaf");

        ExtendedType testleafType = (ExtendedType) testleaf.getType();
        QName testleafTypeQName = testleafType.getQName();
        assertEquals(URI.create("urn:simple.container.demo.test"),
                testleafTypeQName.getNamespace());
        assertEquals(TestUtils.createDate("2013-02-27"),
                testleafTypeQName.getRevision());
        assertEquals("t3", testleafTypeQName.getPrefix());
        assertEquals("union1", testleafTypeQName.getLocalName());

        ExtendedType union2 = (ExtendedType) testleafType.getBaseType();
        QName union2QName = union2.getQName();
        assertEquals(URI.create("urn:simple.container.demo.test"),
                union2QName.getNamespace());
        assertEquals(TestUtils.createDate("2013-02-27"),
                union2QName.getRevision());
        assertEquals("t3", union2QName.getPrefix());
        assertEquals("union2", union2QName.getLocalName());

        UnionType union2Base = (UnionType) union2.getBaseType();
        List<TypeDefinition<?>> unionTypes = union2Base.getTypes();
        assertEquals(2, unionTypes.size());
        assertTrue(unionTypes.get(0) instanceof Int32);
        assertTrue(unionTypes.get(1) instanceof ExtendedType);

        ExtendedType nestedUnion2 = (ExtendedType) unionTypes.get(1);
        QName nestedUnion2QName = nestedUnion2.getQName();
        assertEquals(URI.create("urn:simple.types.data.demo"),
                nestedUnion2QName.getNamespace());
        assertEquals(TestUtils.createDate("2013-02-27"),
                nestedUnion2QName.getRevision());
        assertEquals("t2", nestedUnion2QName.getPrefix());
        assertEquals("nested-union2", nestedUnion2QName.getLocalName());

        UnionType nestedUnion2Base = (UnionType) nestedUnion2.getBaseType();
        List<TypeDefinition<?>> nestedUnion2Types = nestedUnion2Base.getTypes();
        assertEquals(2, nestedUnion2Types.size());
        assertTrue(nestedUnion2Types.get(0) instanceof StringType);
        assertTrue(nestedUnion2Types.get(1) instanceof ExtendedType);

        ExtendedType myUnionExt = (ExtendedType) nestedUnion2Types.get(1);
        QName myUnionExtQName = myUnionExt.getQName();
        assertEquals(URI.create("urn:simple.types.data.demo"),
                myUnionExtQName.getNamespace());
        assertEquals(TestUtils.createDate("2013-02-27"),
                myUnionExtQName.getRevision());
        assertEquals("t2", myUnionExtQName.getPrefix());
        assertEquals("my-union-ext", myUnionExtQName.getLocalName());

        ExtendedType myUnion = (ExtendedType) myUnionExt.getBaseType();
        QName myUnionQName = myUnion.getQName();
        assertEquals(URI.create("urn:simple.types.data.demo"),
                myUnionQName.getNamespace());
        assertEquals(TestUtils.createDate("2013-02-27"),
                myUnionQName.getRevision());
        assertEquals("t2", myUnionQName.getPrefix());
        assertEquals("my-union", myUnionQName.getLocalName());

        UnionType myUnionBase = (UnionType) myUnion.getBaseType();
        List<TypeDefinition<?>> myUnionBaseTypes = myUnionBase.getTypes();
        assertEquals(2, myUnionBaseTypes.size());
        assertTrue(myUnionBaseTypes.get(0) instanceof Int16);
        assertTrue(myUnionBaseTypes.get(1) instanceof Int32);
        Int16 int16 = (Int16) myUnionBaseTypes.get(0);
        List<RangeConstraint> ranges = int16.getRangeStatements();
        assertEquals(1, ranges.size());
        RangeConstraint range = ranges.get(0);
        assertEquals(1L, range.getMin());
        assertEquals(100L, range.getMax());
    }

    @Test
    public void testRefine() {
        Module testModule = TestUtils.findModule(modules, "types2");

        ContainerSchemaNode peer = (ContainerSchemaNode) testModule
                .getDataChildByName("peer");
        ContainerSchemaNode destination = (ContainerSchemaNode) peer
                .getDataChildByName("destination");
        Set<UsesNode> usesNodes = destination.getUses();
        assertEquals(1, usesNodes.size());
        UsesNode usesNode = usesNodes.iterator().next();
        Map<SchemaPath, SchemaNode> refines = usesNode.getRefines();
        assertEquals(2, refines.size());

        for (Map.Entry<SchemaPath, SchemaNode> entry : refines.entrySet()) {
            SchemaNode value = entry.getValue();

            if (value instanceof LeafSchemaNode) {
                LeafSchemaNode refineLeaf = (LeafSchemaNode) value;
                assertNotNull(refineLeaf);
            } else {
                ContainerSchemaNode refineContainer = (ContainerSchemaNode) value;
                Set<MustDefinition> mustConstraints = refineContainer
                        .getConstraints().getMustConstraints();
                assertEquals(1, mustConstraints.size());
                MustDefinition must = mustConstraints.iterator().next();
                assertEquals("must-condition", must.toString());
                assertEquals("An error message test", must.getErrorMessage());
                assertEquals(("An error app tag test"), must.getErrorAppTag());
            }
        }
    }

    @Test
    public void testChoice() {
        Module testModule = TestUtils.findModule(modules, "types1");
        ContainerSchemaNode peer = (ContainerSchemaNode) testModule
                .getDataChildByName("transfer");
        ChoiceNode how = (ChoiceNode) peer.getDataChildByName("how");
        Set<ChoiceCaseNode> cases = how.getCases();
        assertEquals(3, cases.size());
    }

    @Test
    public void testAnyXml() {
        Module testModule = TestUtils.findModule(modules, "types1");
        AnyXmlSchemaNode data = (AnyXmlSchemaNode) testModule
                .getDataChildByName("data");
        assertNotNull(data);
    }

    @Test
    public void testDeviation() {
        Module testModule = TestUtils.findModule(modules, "types1");
        Set<Deviation> deviations = testModule.getDeviations();
        assertEquals(1, deviations.size());

        Deviation dev = deviations.iterator().next();
        SchemaPath expectedPath = TestUtils.createPath(true,
                null, null, "data",
                "system", "user");
        assertEquals(expectedPath, dev.getTargetPath());
        assertEquals(Deviate.ADD, dev.getDeviate());
    }

    @Test
    public void testUnknownNode() {
        Module testModule = TestUtils.findModule(modules, "types3");
        ContainerSchemaNode network = (ContainerSchemaNode)testModule.getDataChildByName("network");
        List<UnknownSchemaNode> unknownNodes = network.getUnknownSchemaNodes();
        assertEquals(1, unknownNodes.size());
    }

    @Test
    public void testFeature() {
        Module testModule = TestUtils.findModule(modules, "types3");
        Set<FeatureDefinition> features = testModule.getFeatures();
        assertEquals(1, features.size());
    }

    @Test
    public void testExtension() {
        Module testModule = TestUtils.findModule(modules, "types3");
        List<ExtensionDefinition> extensions = testModule.getExtensionSchemaNodes();
        assertEquals(1, extensions.size());
        ExtensionDefinition extension = extensions.get(0);
        assertEquals("name", extension.getArgument());
        assertFalse(extension.isYinElement());
    }

    @Test
    public void testNotification() {
        Module testModule = TestUtils.findModule(modules, "types3");
        URI expectedNamespace = URI.create("urn:simple.container.demo.test");
        String expectedPrefix = "t3";
        Date expectedRevision = TestUtils.createDate("2013-02-27");

        Set<NotificationDefinition> notifications = testModule.getNotifications();
        assertEquals(1, notifications.size());

        NotificationDefinition notification = notifications.iterator().next();
        // test SchemaNode args
        QName expectedQName = new QName(expectedNamespace, expectedRevision,
                expectedPrefix, "event");
        assertEquals(expectedQName, notification.getQName());
        SchemaPath expectedPath = TestUtils.createPath(true, expectedNamespace,
                expectedRevision, expectedPrefix, "event");
        assertEquals(expectedPath, notification.getPath());
        assertNull(notification.getDescription());
        assertNull(notification.getReference());
        assertEquals(Status.CURRENT, notification.getStatus());
        assertEquals(0, notification.getUnknownSchemaNodes().size());
        // test DataNodeContainer args
        assertEquals(0, notification.getTypeDefinitions().size());
        assertEquals(3, notification.getChildNodes().size());
        assertEquals(0, notification.getGroupings().size());
        assertEquals(0, notification.getUses().size());

        LeafSchemaNode eventClass = (LeafSchemaNode) notification
                .getDataChildByName("event-class");
        assertTrue(eventClass.getType() instanceof StringType);
        AnyXmlSchemaNode reportingEntity = (AnyXmlSchemaNode) notification
                .getDataChildByName("reporting-entity");
        assertNotNull(reportingEntity);
        LeafSchemaNode severity = (LeafSchemaNode) notification
                .getDataChildByName("severity");
        assertTrue(severity.getType() instanceof StringType);
    }

    @Test
    public void testRpc() {
        Module testModule = TestUtils.findModule(modules, "types3");

        Set<RpcDefinition> rpcs = testModule.getRpcs();
        assertEquals(1, rpcs.size());

        RpcDefinition rpc = rpcs.iterator().next();
        assertEquals("Retrieve all or part of a specified configuration.", rpc.getDescription());
        assertEquals("RFC 6241, Section 7.1", rpc.getReference());

        ContainerSchemaNode input = rpc.getInput();
        assertNotNull(input.getDataChildByName("source"));
        assertNotNull(input.getDataChildByName("filter"));
        ContainerSchemaNode output = rpc.getOutput();
        assertNotNull(output.getDataChildByName("data"));
    }

    @Test
    public void test() {
        Module testModule = TestUtils.findModule(modules, "types4");

        boolean flag = false;
    }

}
