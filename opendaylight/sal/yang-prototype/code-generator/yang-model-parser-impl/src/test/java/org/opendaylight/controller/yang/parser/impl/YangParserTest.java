/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.parser.impl;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.net.URI;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
import org.opendaylight.controller.yang.model.api.LeafListSchemaNode;
import org.opendaylight.controller.yang.model.api.LeafSchemaNode;
import org.opendaylight.controller.yang.model.api.ListSchemaNode;
import org.opendaylight.controller.yang.model.api.Module;
import org.opendaylight.controller.yang.model.api.ModuleImport;
import org.opendaylight.controller.yang.model.api.NotificationDefinition;
import org.opendaylight.controller.yang.model.api.RpcDefinition;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.Status;
import org.opendaylight.controller.yang.model.api.TypeDefinition;
import org.opendaylight.controller.yang.model.api.UnknownSchemaNode;
import org.opendaylight.controller.yang.model.api.type.LengthConstraint;
import org.opendaylight.controller.yang.model.api.type.PatternConstraint;
import org.opendaylight.controller.yang.model.api.type.RangeConstraint;
import org.opendaylight.controller.yang.model.util.Decimal64;
import org.opendaylight.controller.yang.model.util.ExtendedType;
import org.opendaylight.controller.yang.model.util.Int16;
import org.opendaylight.controller.yang.model.util.Int32;
import org.opendaylight.controller.yang.model.util.Int8;
import org.opendaylight.controller.yang.model.util.Leafref;
import org.opendaylight.controller.yang.model.util.StringType;
import org.opendaylight.controller.yang.model.util.Uint32;
import org.opendaylight.controller.yang.model.util.UnionType;

public class YangParserTest {
    private final DateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private Set<Module> modules;

    @Before
    public void init() throws FileNotFoundException {
        modules = TestUtils.loadModules(getClass().getResource("/model").getPath());
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

        ContainerSchemaNode interfaces = (ContainerSchemaNode) test.getDataChildByName("interfaces");
        // test SchemaNode args
        QName expectedQName = new QName(expectedNamespace, expectedRevision, expectedPrefix, "interfaces");
        assertEquals(expectedQName, interfaces.getQName());
        SchemaPath expectedPath = TestUtils.createPath(true, expectedNamespace, expectedRevision, expectedPrefix,
                "interfaces");
        assertEquals(expectedPath, interfaces.getPath());
        assertNull(interfaces.getDescription());
        assertNull(interfaces.getReference());
        assertEquals(Status.CURRENT, interfaces.getStatus());
        assertEquals(0, interfaces.getUnknownSchemaNodes().size());
        // test DataSchemaNode args
        assertFalse(interfaces.isAugmenting());
        assertTrue(interfaces.isConfiguration());
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

        ListSchemaNode ifEntry = (ListSchemaNode) interfaces.getDataChildByName("ifEntry");
        assertNotNull(ifEntry);
    }

    @Test
    public void testParseList() {
        Module test = TestUtils.findModule(modules, "types2");
        URI expectedNamespace = URI.create("urn:simple.types.data.demo");
        String expectedPrefix = "t2";
        Date expectedRevision = TestUtils.createDate("2013-02-27");

        ContainerSchemaNode interfaces = (ContainerSchemaNode) test.getDataChildByName("interfaces");

        ListSchemaNode ifEntry = (ListSchemaNode) interfaces.getDataChildByName("ifEntry");
        // test SchemaNode args
        QName expectedQName = new QName(expectedNamespace, expectedRevision, expectedPrefix, "ifEntry");
        assertEquals(expectedQName, ifEntry.getQName());
        SchemaPath expectedPath = TestUtils.createPath(true, expectedNamespace, expectedRevision, expectedPrefix,
                "interfaces", "ifEntry");
        assertEquals(expectedPath, ifEntry.getPath());
        assertNull(ifEntry.getDescription());
        assertNull(ifEntry.getReference());
        assertEquals(Status.CURRENT, ifEntry.getStatus());
        assertEquals(0, ifEntry.getUnknownSchemaNodes().size());
        // test DataSchemaNode args
        assertFalse(ifEntry.isAugmenting());
        assertTrue(ifEntry.isConfiguration());
        ConstraintDefinition constraints = ifEntry.getConstraints();
        assertNull(constraints.getWhenCondition());
        assertEquals(0, constraints.getMustConstraints().size());
        assertFalse(constraints.isMandatory());
        assertEquals(1, (int) constraints.getMinElements());
        assertEquals(11, (int) constraints.getMaxElements());
        // test AugmentationTarget args
        Set<AugmentationSchema> availableAugmentations = ifEntry.getAvailableAugmentations();
        assertEquals(2, availableAugmentations.size());
        // test ListSchemaNode args
        List<QName> expectedKey = new ArrayList<QName>();
        expectedKey.add(new QName(expectedNamespace, expectedRevision, expectedPrefix, "ifIndex"));
        assertEquals(expectedKey, ifEntry.getKeyDefinition());
        assertFalse(ifEntry.isUserOrdered());
        // test DataNodeContainer args
        assertEquals(0, ifEntry.getTypeDefinitions().size());
        assertEquals(4, ifEntry.getChildNodes().size());
        assertEquals(0, ifEntry.getGroupings().size());
        assertEquals(0, ifEntry.getUses().size());

        LeafSchemaNode ifIndex = (LeafSchemaNode) ifEntry.getDataChildByName("ifIndex");
        assertTrue(ifIndex.getType() instanceof Uint32);
        LeafSchemaNode ifMtu = (LeafSchemaNode) ifEntry.getDataChildByName("ifMtu");
        assertTrue(ifMtu.getType() instanceof Int32);
    }

    @Test
    public void testParseLeaf() throws ParseException {
        Module test = TestUtils.findModule(modules, "types2");

        // leaf if-name
        LeafSchemaNode ifName = (LeafSchemaNode) test.getDataChildByName("if-name");
        Leafref ifNameType = (Leafref) ifName.getType();
        QName qname = ifNameType.getQName();

        URI baseYangTypeNS = URI.create("urn:ietf:params:xml:ns:yang:1");
        assertEquals(baseYangTypeNS, qname.getNamespace());
        assertNull(qname.getRevision());
        assertEquals("", qname.getPrefix());
        assertEquals("leafref", qname.getLocalName());

        // leaf name
        LeafSchemaNode name = (LeafSchemaNode) test.getDataChildByName("name");
        StringType nameType = (StringType) name.getType();
        QName nameQName = nameType.getQName();

        assertEquals(baseYangTypeNS, nameQName.getNamespace());
        assertNull(nameQName.getRevision());
        assertEquals("", nameQName.getPrefix());
        assertEquals("string", nameQName.getLocalName());

        // leaf count
        LeafSchemaNode count = (LeafSchemaNode) test.getDataChildByName("count");
        ExtendedType countType = (ExtendedType) count.getType();
        QName countTypeQName = countType.getQName();

        URI expectedNS = URI.create("urn:simple.types.data.demo");
        Date expectedDate = simpleDateFormat.parse("2013-02-27");
        assertEquals(expectedNS, countTypeQName.getNamespace());
        assertEquals(expectedDate, countTypeQName.getRevision());
        assertEquals("t2", countTypeQName.getPrefix());
        assertEquals("int8", countTypeQName.getLocalName());

        Int8 countTypeBase = (Int8) countType.getBaseType();
        QName countTypeBaseQName = countTypeBase.getQName();

        assertEquals(baseYangTypeNS, countTypeBaseQName.getNamespace());
        assertNull(countTypeBaseQName.getRevision());
        assertEquals("", countTypeBaseQName.getPrefix());
        assertEquals("int8", countTypeBaseQName.getLocalName());
    }

    @Test
    public void testAugmentResolving() {
        // testfile1
        Module module1 = TestUtils.findModule(modules, "types1");

        Set<AugmentationSchema> module1Augmentations = module1.getAugmentations();
        AugmentationSchema augment1 = module1Augmentations.iterator().next();
        LeafSchemaNode augmentedLeafDefinition = (LeafSchemaNode) augment1.getDataChildByName("ds0ChannelNumber");
        assertTrue(augmentedLeafDefinition.isAugmenting());

        // testfile2
        Module module2 = TestUtils.findModule(modules, "types2");

        ContainerSchemaNode interfaces = (ContainerSchemaNode) module2.getDataChildByName("interfaces");
        ListSchemaNode ifEntry = (ListSchemaNode) interfaces.getDataChildByName("ifEntry");
        ContainerSchemaNode augmentedContainer = (ContainerSchemaNode) ifEntry.getDataChildByName("augment-holder");

        ContainerSchemaNode schemas = (ContainerSchemaNode) augmentedContainer.getDataChildByName("schemas");
        LeafSchemaNode linkleaf = (LeafSchemaNode) schemas.getDataChildByName("linkleaf");
        assertNotNull(linkleaf);

        // augmentation defined in testfile1 and augmentation returned from
        // augmented container have to be same
        Set<AugmentationSchema> augmentedContainerAugments = augmentedContainer.getAvailableAugmentations();
        AugmentationSchema augmentDefinition = augmentedContainerAugments.iterator().next();
        assertEquals(augment1, augmentDefinition);

        LeafSchemaNode augmentedLeaf = (LeafSchemaNode) augmentedContainer.getDataChildByName("ds0ChannelNumber");
        assertTrue(augmentedLeaf.isAugmenting());
        assertEquals(augmentedLeafDefinition, augmentedLeaf);

        Set<AugmentationSchema> ifEntryAugments = ifEntry.getAvailableAugmentations();
        assertEquals(2, ifEntryAugments.size());

        // testfile3
        Module module3 = TestUtils.findModule(modules, "types3");

        Set<AugmentationSchema> module3Augmentations = module3.getAugmentations();
        assertEquals(3, module3Augmentations.size());
        AugmentationSchema augment3 = null;
        for (AugmentationSchema as : module3Augmentations) {
            if ("if:ifType='ds0'".equals(as.getWhenCondition().toString())) {
                augment3 = as;
            }
        }
        ContainerSchemaNode augmentedContainerDefinition = (ContainerSchemaNode) augment3
                .getDataChildByName("augment-holder");
        assertTrue(augmentedContainerDefinition.isAugmenting());

        // check
        assertEquals(augmentedContainer, augmentedContainerDefinition);
        assertEquals(augmentedLeaf, augmentedLeafDefinition);
    }

    @Test
    public void testAugmentTarget() {
        Module test = TestUtils.findModule(modules, "types2");

        ContainerSchemaNode interfaces = (ContainerSchemaNode) test.getDataChildByName("interfaces");
        ListSchemaNode ifEntry = (ListSchemaNode) interfaces.getDataChildByName("ifEntry");
        Set<AugmentationSchema> augmentations = ifEntry.getAvailableAugmentations();
        assertEquals(2, augmentations.size());

        AugmentationSchema augment = null;
        for (AugmentationSchema as : augmentations) {
            if ("if:ifType='ds0'".equals(as.getWhenCondition().toString())) {
                augment = as;
            }
        }
        ContainerSchemaNode augmentHolder = (ContainerSchemaNode) augment.getDataChildByName("augment-holder");
        assertNotNull(augmentHolder);
        assertTrue(augmentHolder.isAugmenting());
        QName augmentHolderQName = augmentHolder.getQName();
        assertEquals("augment-holder", augmentHolderQName.getLocalName());
        assertEquals("t3", augmentHolderQName.getPrefix());
        assertEquals("Description for augment holder", augmentHolder.getDescription());
    }

    @Test
    public void testTypedefRangesResolving() throws ParseException {
        Module testModule = TestUtils.findModule(modules, "types1");

        LeafSchemaNode testleaf = (LeafSchemaNode) testModule.getDataChildByName("testleaf");
        ExtendedType leafType = (ExtendedType) testleaf.getType();
        QName leafTypeQName = leafType.getQName();
        assertEquals("my-type1", leafTypeQName.getLocalName());
        assertEquals("t1", leafTypeQName.getPrefix());
        assertEquals(URI.create("urn:simple.container.demo"), leafTypeQName.getNamespace());
        Date expectedDate = simpleDateFormat.parse("2013-02-27");
        assertEquals(expectedDate, leafTypeQName.getRevision());
        assertEquals(1, leafType.getRanges().size());

        ExtendedType baseType = (ExtendedType) leafType.getBaseType();
        QName baseTypeQName = baseType.getQName();
        assertEquals("my-type1", baseTypeQName.getLocalName());
        assertEquals("t2", baseTypeQName.getPrefix());
        assertEquals(URI.create("urn:simple.types.data.demo"), baseTypeQName.getNamespace());
        assertEquals(expectedDate, baseTypeQName.getRevision());
        assertEquals(2, baseType.getRanges().size());

        List<RangeConstraint> ranges = leafType.getRanges();
        assertEquals(1, ranges.size());
        RangeConstraint range = ranges.get(0);
        assertEquals(12L, range.getMin());
        assertEquals(20L, range.getMax());
    }

    @Test
    public void testTypedefPatternsResolving() {
        Module testModule = TestUtils.findModule(modules, "types1");

        LeafSchemaNode testleaf = (LeafSchemaNode) testModule.getDataChildByName("test-string-leaf");
        ExtendedType testleafType = (ExtendedType) testleaf.getType();
        QName testleafTypeQName = testleafType.getQName();
        assertEquals("my-string-type-ext", testleafTypeQName.getLocalName());
        assertEquals("t2", testleafTypeQName.getPrefix());

        List<PatternConstraint> patterns = testleafType.getPatterns();
        assertEquals(1, patterns.size());
        PatternConstraint pattern = patterns.iterator().next();
        assertEquals("[e-z]*", pattern.getRegularExpression());

        ExtendedType baseType = (ExtendedType) testleafType.getBaseType();
        assertEquals("my-string-type2", baseType.getQName().getLocalName());

        patterns = baseType.getPatterns();
        assertEquals(1, patterns.size());
        pattern = patterns.iterator().next();
        assertEquals("[b-u]*", pattern.getRegularExpression());

        List<LengthConstraint> lengths = testleafType.getLengths();
        assertTrue(lengths.isEmpty());
    }

    @Test
    public void testTypedefLengthsResolving() {
        Module testModule = TestUtils.findModule(modules, "types1");

        LeafSchemaNode testleaf = (LeafSchemaNode) testModule.getDataChildByName("leaf-with-length");
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

        LeafSchemaNode testleaf = (LeafSchemaNode) testModule.getDataChildByName("nested-type-leaf");
        ExtendedType testleafType = (ExtendedType) testleaf.getType();
        assertEquals("my-type1", testleafType.getQName().getLocalName());

        ExtendedType baseType = (ExtendedType) testleafType.getBaseType();
        assertEquals("my-base-int32-type", baseType.getQName().getLocalName());

        Int32 int32Type = (Int32) baseType.getBaseType();
        QName qname = int32Type.getQName();
        assertEquals(URI.create("urn:ietf:params:xml:ns:yang:1"), qname.getNamespace());
        assertNull(qname.getRevision());
        assertEquals("", qname.getPrefix());
        assertEquals("int32", qname.getLocalName());
        List<RangeConstraint> ranges = baseType.getRanges();
        assertEquals(1, ranges.size());
        RangeConstraint range = ranges.get(0);
        assertEquals(2L, range.getMin());
        assertEquals(20L, range.getMax());
    }

    @Test
    public void testTypedefDecimal1() {
        Module testModule = TestUtils.findModule(modules, "types1");

        LeafSchemaNode testleaf = (LeafSchemaNode) testModule.getDataChildByName("test-decimal-leaf");
        ExtendedType type = (ExtendedType) testleaf.getType();
        assertEquals(4, (int) type.getFractionDigits());

        ExtendedType typeBase = (ExtendedType) type.getBaseType();
        assertEquals("my-decimal-type", typeBase.getQName().getLocalName());
        assertNull(typeBase.getFractionDigits());

        Decimal64 decimal = (Decimal64) typeBase.getBaseType();
        assertEquals(6, (int) decimal.getFractionDigits());
    }

    @Test
    public void testTypedefDecimal2() {
        Module testModule = TestUtils.findModule(modules, "types1");

        LeafSchemaNode testleaf = (LeafSchemaNode) testModule.getDataChildByName("test-decimal-leaf2");
        TypeDefinition<?> baseType = testleaf.getType().getBaseType();
        assertTrue(testleaf.getType().getBaseType() instanceof Decimal64);
        Decimal64 baseTypeCast = (Decimal64) baseType;
        assertEquals(5, (int) baseTypeCast.getFractionDigits());
    }

    @Test
    public void testTypedefUnion() {
        Module testModule = TestUtils.findModule(modules, "types1");

        LeafSchemaNode testleaf = (LeafSchemaNode) testModule.getDataChildByName("union-leaf");
        ExtendedType testleafType = (ExtendedType) testleaf.getType();
        assertEquals("my-union-ext", testleafType.getQName().getLocalName());

        ExtendedType baseType = (ExtendedType) testleafType.getBaseType();
        assertEquals("my-union", baseType.getQName().getLocalName());

        UnionType unionBase = (UnionType) baseType.getBaseType();

        List<TypeDefinition<?>> unionTypes = unionBase.getTypes();
        ExtendedType unionType1 = (ExtendedType) unionTypes.get(0);
        List<RangeConstraint> ranges = unionType1.getRanges();
        assertEquals(1, ranges.size());
        RangeConstraint range = ranges.get(0);
        assertEquals(1L, range.getMin());
        assertEquals(100L, range.getMax());

        assertTrue(unionType1.getBaseType() instanceof Int16);
        assertTrue(unionTypes.get(1) instanceof Int32);
    }

    @Test
    public void testNestedUnionResolving1() {
        Module testModule = TestUtils.findModule(modules, "types1");

        LeafSchemaNode testleaf = (LeafSchemaNode) testModule.getDataChildByName("nested-union-leaf");

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
        ExtendedType extendedUnionBase = (ExtendedType) extendedUnion.getBaseType();
        assertEquals("my-union", extendedUnionBase.getQName().getLocalName());

        UnionType extendedTargetUnion = (UnionType) extendedUnionBase.getBaseType();
        List<TypeDefinition<?>> extendedTargetTypes = extendedTargetUnion.getTypes();
        assertTrue(extendedTargetTypes.get(0).getBaseType() instanceof Int16);
        assertTrue(extendedTargetTypes.get(1) instanceof Int32);

        ExtendedType int16 = (ExtendedType) extendedTargetTypes.get(0);
        assertTrue(int16.getBaseType() instanceof Int16);
        List<RangeConstraint> ranges = int16.getRanges();
        assertEquals(1, ranges.size());
        RangeConstraint range = ranges.get(0);
        assertEquals(1L, range.getMin());
        assertEquals(100L, range.getMax());
    }

    @Test
    public void testNestedUnionResolving2() {
        Module testModule = TestUtils.findModule(modules, "types1");

        LeafSchemaNode testleaf = (LeafSchemaNode) testModule.getDataChildByName("custom-union-leaf");

        ExtendedType testleafType = (ExtendedType) testleaf.getType();
        QName testleafTypeQName = testleafType.getQName();
        assertEquals(URI.create("urn:simple.container.demo.test"), testleafTypeQName.getNamespace());
        assertEquals(TestUtils.createDate("2013-02-27"), testleafTypeQName.getRevision());
        assertEquals("t3", testleafTypeQName.getPrefix());
        assertEquals("union1", testleafTypeQName.getLocalName());

        ExtendedType union2 = (ExtendedType) testleafType.getBaseType();
        QName union2QName = union2.getQName();
        assertEquals(URI.create("urn:simple.container.demo.test"), union2QName.getNamespace());
        assertEquals(TestUtils.createDate("2013-02-27"), union2QName.getRevision());
        assertEquals("t3", union2QName.getPrefix());
        assertEquals("union2", union2QName.getLocalName());

        UnionType union2Base = (UnionType) union2.getBaseType();
        List<TypeDefinition<?>> unionTypes = union2Base.getTypes();
        assertEquals(2, unionTypes.size());
        assertTrue(unionTypes.get(0) instanceof Int32);
        assertTrue(unionTypes.get(1) instanceof ExtendedType);

        ExtendedType nestedUnion2 = (ExtendedType) unionTypes.get(1);
        QName nestedUnion2QName = nestedUnion2.getQName();
        assertEquals(URI.create("urn:simple.types.data.demo"), nestedUnion2QName.getNamespace());
        assertEquals(TestUtils.createDate("2013-02-27"), nestedUnion2QName.getRevision());
        assertEquals("t2", nestedUnion2QName.getPrefix());
        assertEquals("nested-union2", nestedUnion2QName.getLocalName());

        UnionType nestedUnion2Base = (UnionType) nestedUnion2.getBaseType();
        List<TypeDefinition<?>> nestedUnion2Types = nestedUnion2Base.getTypes();
        assertEquals(2, nestedUnion2Types.size());
        assertTrue(nestedUnion2Types.get(0) instanceof StringType);
        assertTrue(nestedUnion2Types.get(1) instanceof ExtendedType);

        ExtendedType myUnionExt = (ExtendedType) nestedUnion2Types.get(1);
        QName myUnionExtQName = myUnionExt.getQName();
        assertEquals(URI.create("urn:simple.types.data.demo"), myUnionExtQName.getNamespace());
        assertEquals(TestUtils.createDate("2013-02-27"), myUnionExtQName.getRevision());
        assertEquals("t2", myUnionExtQName.getPrefix());
        assertEquals("my-union-ext", myUnionExtQName.getLocalName());

        ExtendedType myUnion = (ExtendedType) myUnionExt.getBaseType();
        QName myUnionQName = myUnion.getQName();
        assertEquals(URI.create("urn:simple.types.data.demo"), myUnionQName.getNamespace());
        assertEquals(TestUtils.createDate("2013-02-27"), myUnionQName.getRevision());
        assertEquals("t2", myUnionQName.getPrefix());
        assertEquals("my-union", myUnionQName.getLocalName());

        UnionType myUnionBase = (UnionType) myUnion.getBaseType();
        List<TypeDefinition<?>> myUnionBaseTypes = myUnionBase.getTypes();
        assertEquals(2, myUnionBaseTypes.size());
        assertTrue(myUnionBaseTypes.get(0).getBaseType() instanceof Int16);
        assertTrue(myUnionBaseTypes.get(1) instanceof Int32);
        ExtendedType int16 = (ExtendedType) myUnionBaseTypes.get(0);
        List<RangeConstraint> ranges = int16.getRanges();
        assertEquals(1, ranges.size());
        RangeConstraint range = ranges.get(0);
        assertEquals(1L, range.getMin());
        assertEquals(100L, range.getMax());
    }

    @Test
    public void testChoice() {
        Module testModule = TestUtils.findModule(modules, "types1");
        ContainerSchemaNode peer = (ContainerSchemaNode) testModule.getDataChildByName("transfer");
        ChoiceNode how = (ChoiceNode) peer.getDataChildByName("how");
        Set<ChoiceCaseNode> cases = how.getCases();
        assertEquals(5, cases.size());
        ChoiceCaseNode input = null;
        ChoiceCaseNode output = null;
        for(ChoiceCaseNode caseNode : cases) {
            if("input".equals(caseNode.getQName().getLocalName())) {
                input = caseNode;
            } else if("output".equals(caseNode.getQName().getLocalName())) {
                output = caseNode;
            }
        }
        assertNotNull(input);
        assertNotNull(input.getPath());
        assertNotNull(output);
        assertNotNull(output.getPath());
    }

    @Test
    public void testAnyXml() {
        Module testModule = TestUtils.findModule(modules, "types1");
        AnyXmlSchemaNode data = (AnyXmlSchemaNode) testModule.getDataChildByName("data");
        assertNotNull(data);
    }

    @Test
    public void testDeviation() {
        Module testModule = TestUtils.findModule(modules, "types1");
        Set<Deviation> deviations = testModule.getDeviations();
        assertEquals(1, deviations.size());

        Deviation dev = deviations.iterator().next();
        SchemaPath expectedPath = TestUtils.createPath(true, null, null, "data", "system", "user");
        assertEquals(expectedPath, dev.getTargetPath());
        assertEquals(Deviate.ADD, dev.getDeviate());
    }

    @Test
    public void testUnknownNode() {
        Module testModule = TestUtils.findModule(modules, "types3");
        ContainerSchemaNode network = (ContainerSchemaNode) testModule.getDataChildByName("network");
        List<UnknownSchemaNode> unknownNodes = network.getUnknownSchemaNodes();
        assertEquals(1, unknownNodes.size());
        UnknownSchemaNode unknownNode = unknownNodes.get(0);
        assertNotNull(unknownNode.getNodeType());
        assertEquals("point", unknownNode.getNodeParameter());
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
        QName expectedQName = new QName(expectedNamespace, expectedRevision, expectedPrefix, "event");
        assertEquals(expectedQName, notification.getQName());
        SchemaPath expectedPath = TestUtils.createPath(true, expectedNamespace, expectedRevision, expectedPrefix,
                "event");
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

        LeafSchemaNode eventClass = (LeafSchemaNode) notification.getDataChildByName("event-class");
        assertTrue(eventClass.getType() instanceof StringType);
        AnyXmlSchemaNode reportingEntity = (AnyXmlSchemaNode) notification.getDataChildByName("reporting-entity");
        assertNotNull(reportingEntity);
        LeafSchemaNode severity = (LeafSchemaNode) notification.getDataChildByName("severity");
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
    public void testAugmentNodesTypesSchemaPath() throws Exception {
        Module testModule = TestUtils.findModule(modules, "types1");
        Set<AugmentationSchema> augments = testModule.getAugmentations();
        assertEquals(1, augments.size());
        AugmentationSchema augment = augments.iterator().next();

        LeafSchemaNode ifcId = (LeafSchemaNode) augment.getDataChildByName("interface-id");
        Leafref ifcIdType = (Leafref) ifcId.getType();
        SchemaPath ifcIdTypeSchemaPath = ifcIdType.getPath();
        List<QName> ifcIdTypePath = ifcIdTypeSchemaPath.getPath();

        URI types1URI = URI.create("urn:simple.container.demo");
        URI types2URI = URI.create("urn:simple.types.data.demo");
        URI types3URI = URI.create("urn:simple.container.demo.test");
        Date expectedDate = simpleDateFormat.parse("2013-02-27");

        QName q0 = new QName(types2URI, expectedDate, "data", "interfaces");
        QName q1 = new QName(types2URI, expectedDate, "data", "ifEntry");
        QName q2 = new QName(types3URI, expectedDate, "data", "augment-holder");
        QName q3 = new QName(types1URI, expectedDate, "data", "interface-id");
        assertEquals(q0, ifcIdTypePath.get(0));
        assertEquals(q1, ifcIdTypePath.get(1));
        assertEquals(q2, ifcIdTypePath.get(2));
        assertEquals(q3, ifcIdTypePath.get(3));

        LeafListSchemaNode higherLayer = (LeafListSchemaNode) augment.getDataChildByName("higher-layer-if");
        Leafref higherLayerType = (Leafref) higherLayer.getType();
        SchemaPath higherLayerTypeSchemaPath = higherLayerType.getPath();
        List<QName> higherLayerTypePath = higherLayerTypeSchemaPath.getPath();
        assertEquals(q0, higherLayerTypePath.get(0));
        assertEquals(q1, higherLayerTypePath.get(1));
        assertEquals(q2, higherLayerTypePath.get(2));
        q3 = new QName(types1URI, expectedDate, "data", "higher-layer-if");
        assertEquals(q3, higherLayerTypePath.get(3));

        LeafSchemaNode myType = (LeafSchemaNode) augment.getDataChildByName("my-type");
        ExtendedType leafType = (ExtendedType) myType.getType();

        testModule = TestUtils.findModule(modules, "types2");
        TypeDefinition<?> typedef = TestUtils.findTypedef(testModule.getTypeDefinitions(), "my-type1");

        assertEquals(typedef, leafType);
    }

    @Test
    public void testTypePath() throws ParseException {
        Module test = TestUtils.findModule(modules, "types2");
        Set<TypeDefinition<?>> types = test.getTypeDefinitions();

        // my-base-int32-type
        ExtendedType int32Typedef = (ExtendedType) TestUtils.findTypedef(types, "my-base-int32-type");
        QName int32TypedefQName = int32Typedef.getQName();

        URI expectedNS = URI.create("urn:simple.types.data.demo");
        Date expectedDate = simpleDateFormat.parse("2013-02-27");
        assertEquals(expectedNS, int32TypedefQName.getNamespace());
        assertEquals(expectedDate, int32TypedefQName.getRevision());
        assertEquals("t2", int32TypedefQName.getPrefix());
        assertEquals("my-base-int32-type", int32TypedefQName.getLocalName());

        SchemaPath typeSchemaPath = int32Typedef.getPath();
        List<QName> typePath = typeSchemaPath.getPath();
        assertEquals(1, typePath.size());
        assertEquals(int32TypedefQName, typePath.get(0));

        // my-base-int32-type/int32
        Int32 int32 = (Int32) int32Typedef.getBaseType();
        QName int32QName = int32.getQName();
        assertEquals(URI.create("urn:ietf:params:xml:ns:yang:1"), int32QName.getNamespace());
        assertNull(int32QName.getRevision());
        assertEquals("", int32QName.getPrefix());
        assertEquals("int32", int32QName.getLocalName());

        SchemaPath int32SchemaPath = int32.getPath();
        List<QName> int32Path = int32SchemaPath.getPath();
        assertEquals(3, int32Path.size());
        assertEquals(int32TypedefQName, int32Path.get(0));
        assertEquals(int32QName, int32Path.get(2));
    }

    @Test
    public void testTypePath2() throws ParseException {
        Module test = TestUtils.findModule(modules, "types2");
        Set<TypeDefinition<?>> types = test.getTypeDefinitions();

        // my-base-int32-type
        ExtendedType myDecType = (ExtendedType) TestUtils.findTypedef(types, "my-decimal-type");
        QName myDecTypeQName = myDecType.getQName();

        URI expectedNS = URI.create("urn:simple.types.data.demo");
        Date expectedDate = simpleDateFormat.parse("2013-02-27");
        assertEquals(expectedNS, myDecTypeQName.getNamespace());
        assertEquals(expectedDate, myDecTypeQName.getRevision());
        assertEquals("t2", myDecTypeQName.getPrefix());
        assertEquals("my-decimal-type", myDecTypeQName.getLocalName());

        SchemaPath typeSchemaPath = myDecType.getPath();
        List<QName> typePath = typeSchemaPath.getPath();
        assertEquals(1, typePath.size());
        assertEquals(myDecTypeQName, typePath.get(0));

        // my-base-int32-type/int32
        Decimal64 dec64 = (Decimal64) myDecType.getBaseType();
        QName dec64QName = dec64.getQName();

        assertEquals(URI.create("urn:ietf:params:xml:ns:yang:1"), dec64QName.getNamespace());
        assertNull(dec64QName.getRevision());
        assertEquals("", dec64QName.getPrefix());
        assertEquals("decimal64", dec64QName.getLocalName());

        SchemaPath dec64SchemaPath = dec64.getPath();
        List<QName> dec64Path = dec64SchemaPath.getPath();
        assertEquals(2, dec64Path.size());
        assertEquals(myDecTypeQName, dec64Path.get(0));
        assertEquals(dec64QName, dec64Path.get(1));
    }

}
