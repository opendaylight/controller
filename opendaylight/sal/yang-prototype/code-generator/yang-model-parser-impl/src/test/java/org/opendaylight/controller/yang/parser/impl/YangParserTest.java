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
import org.opendaylight.controller.yang.model.api.DataSchemaNode;
import org.opendaylight.controller.yang.model.api.Deviation;
import org.opendaylight.controller.yang.model.api.Deviation.Deviate;
import org.opendaylight.controller.yang.model.api.ExtensionDefinition;
import org.opendaylight.controller.yang.model.api.FeatureDefinition;
import org.opendaylight.controller.yang.model.api.GroupingDefinition;
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
import org.opendaylight.controller.yang.model.util.StringType;
import org.opendaylight.controller.yang.model.util.Uint32;
import org.opendaylight.controller.yang.model.util.UnionType;

public class YangParserTest {

    private final URI nodesNS = URI.create("urn:simple.nodes.test");
    private final URI typesNS = URI.create("urn:simple.types.test");
    private final URI customNS = URI.create("urn:custom.nodes.test");
    private Date nodesRev;
    private Date typesRev;
    private Date customRev;

    private final DateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private Set<Module> modules;

    @Before
    public void init() throws FileNotFoundException, ParseException {
        nodesRev = simpleDateFormat.parse("2013-02-27");
        typesRev = simpleDateFormat.parse("2013-07-03");
        customRev = simpleDateFormat.parse("2013-02-27");

        modules = TestUtils.loadModules(getClass().getResource("/model").getPath());
        assertEquals(3, modules.size());
    }

    @Test
    public void testHeaders() throws ParseException {
        Module test = TestUtils.findModule(modules, "nodes");

        assertEquals("nodes", test.getName());
        assertEquals("1", test.getYangVersion());
        assertEquals(nodesNS, test.getNamespace());
        assertEquals("n", test.getPrefix());

        Set<ModuleImport> imports = test.getImports();
        assertEquals(2, imports.size());

        ModuleImport import2 = TestUtils.findImport(imports, "t");
        assertEquals("types", import2.getModuleName());
        assertEquals(typesRev, import2.getRevision());

        ModuleImport import3 = TestUtils.findImport(imports, "c");
        assertEquals("custom", import3.getModuleName());
        assertEquals(customRev, import3.getRevision());

        assertEquals("opendaylight", test.getOrganization());
        assertEquals("http://www.opendaylight.org/", test.getContact());
        Date expectedRevision = TestUtils.createDate("2013-02-27");
        assertEquals(expectedRevision, test.getRevision());
        assertEquals(" WILL BE DEFINED LATER", test.getReference());
    }

    @Test
    public void testOrderingTypedef() {
        Module test = TestUtils.findModule(modules, "types");
        Set<TypeDefinition<?>> typedefs = test.getTypeDefinitions();
        String[] expectedOrder = new String[] { "int32-ext1", "int32-ext2", "my-decimal-type", "my-union",
                "my-union-ext", "nested-union2", "string-ext1", "string-ext2", "string-ext3", "string-ext4" };
        String[] actualOrder = new String[typedefs.size()];

        int i = 0;
        for (TypeDefinition<?> type : typedefs) {
            actualOrder[i] = type.getQName().getLocalName();
            i++;
        }
        assertArrayEquals(expectedOrder, actualOrder);
    }

    @Test
    public void testOrderingChildNodes() {
        Module test = TestUtils.findModule(modules, "nodes");
        AugmentationSchema augment1 = null;
        for (AugmentationSchema as : test.getAugmentations()) {
            if ("if:ifType='ds0'".equals(as.getWhenCondition().toString())) {
                augment1 = as;
                break;
            }
        }
        assertNotNull(augment1);

        String[] expectedOrder = new String[] { "ds0ChannelNumber", "interface-id", "my-type", "odl", "schemas" };
        String[] actualOrder = new String[expectedOrder.length];

        int i = 0;
        for (DataSchemaNode augmentChild : augment1.getChildNodes()) {
            actualOrder[i] = augmentChild.getQName().getLocalName();
            i++;
        }

        assertArrayEquals(expectedOrder, actualOrder);
    }

    @Test
    public void testOrderingNestedChildNodes() {
        Module test = TestUtils.findModule(modules, "custom");
        Set<GroupingDefinition> groupings = test.getGroupings();
        assertEquals(1, groupings.size());
        GroupingDefinition target = groupings.iterator().next();

        Set<DataSchemaNode> childNodes = target.getChildNodes();
        String[] expectedOrder = new String[] { "address", "addresses", "data", "how", "port" };
        String[] actualOrder = new String[childNodes.size()];

        int i = 0;
        for (DataSchemaNode child : childNodes) {
            actualOrder[i] = child.getQName().getLocalName();
            i++;
        }
        assertArrayEquals(expectedOrder, actualOrder);
    }

    @Test
    public void testParseContainer() {
        Module test = TestUtils.findModule(modules, "types");
        URI expectedNamespace = URI.create("urn:simple.types.test");
        String expectedPrefix = "t";

        ContainerSchemaNode interfaces = (ContainerSchemaNode) test.getDataChildByName("interfaces");
        // test SchemaNode args
        QName expectedQName = new QName(expectedNamespace, typesRev, expectedPrefix, "interfaces");
        assertEquals(expectedQName, interfaces.getQName());
        SchemaPath expectedPath = TestUtils.createPath(true, expectedNamespace, typesRev, expectedPrefix, "interfaces");
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
        Module test = TestUtils.findModule(modules, "types");
        URI expectedNamespace = URI.create("urn:simple.types.test");
        String expectedPrefix = "t";

        ContainerSchemaNode interfaces = (ContainerSchemaNode) test.getDataChildByName("interfaces");

        ListSchemaNode ifEntry = (ListSchemaNode) interfaces.getDataChildByName("ifEntry");
        // test SchemaNode args
        QName expectedQName = new QName(expectedNamespace, typesRev, expectedPrefix, "ifEntry");
        assertEquals(expectedQName, ifEntry.getQName());
        SchemaPath expectedPath = TestUtils.createPath(true, expectedNamespace, typesRev, expectedPrefix, "interfaces",
                "ifEntry");
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
        expectedKey.add(new QName(expectedNamespace, typesRev, expectedPrefix, "ifIndex"));
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
    public void testTypedefRangesResolving() throws ParseException {
        Module testModule = TestUtils.findModule(modules, "nodes");
        LeafSchemaNode int32Leaf = (LeafSchemaNode) testModule.getDataChildByName("int32-leaf");

        ExtendedType leafType = (ExtendedType) int32Leaf.getType();
        QName leafTypeQName = leafType.getQName();
        assertEquals("int32-ext2", leafTypeQName.getLocalName());
        assertEquals("n", leafTypeQName.getPrefix());
        assertEquals(nodesNS, leafTypeQName.getNamespace());
        assertEquals(nodesRev, leafTypeQName.getRevision());
        assertNull(leafType.getUnits());
        assertNull(leafType.getDefaultValue());
        assertTrue(leafType.getLengths().isEmpty());
        assertTrue(leafType.getPatterns().isEmpty());
        List<RangeConstraint> ranges = leafType.getRanges();
        assertEquals(1, ranges.size());
        RangeConstraint range = ranges.get(0);
        assertEquals(12L, range.getMin());
        assertEquals(20L, range.getMax());

        ExtendedType baseType = (ExtendedType) leafType.getBaseType();
        QName baseTypeQName = baseType.getQName();
        assertEquals("int32-ext2", baseTypeQName.getLocalName());
        assertEquals("t", baseTypeQName.getPrefix());
        assertEquals(typesNS, baseTypeQName.getNamespace());
        assertEquals(typesRev, baseTypeQName.getRevision());
        assertEquals("mile", baseType.getUnits());
        assertEquals("11", baseType.getDefaultValue());
        assertTrue(leafType.getLengths().isEmpty());
        assertTrue(leafType.getPatterns().isEmpty());
        List<RangeConstraint> baseTypeRanges = baseType.getRanges();
        assertEquals(2, baseTypeRanges.size());
        RangeConstraint baseTypeRange1 = baseTypeRanges.get(0);
        assertEquals(3L, baseTypeRange1.getMin());
        assertEquals(9L, baseTypeRange1.getMax());
        RangeConstraint baseTypeRange2 = baseTypeRanges.get(1);
        assertEquals(11L, baseTypeRange2.getMin());
        assertEquals(20L, baseTypeRange2.getMax());

        ExtendedType base = (ExtendedType) baseType.getBaseType();
        QName baseQName = base.getQName();
        assertEquals("int32-ext1", baseQName.getLocalName());
        assertEquals("t", baseQName.getPrefix());
        assertEquals(typesNS, baseQName.getNamespace());
        assertEquals(typesRev, baseQName.getRevision());
        assertNull(base.getUnits());
        assertNull(base.getDefaultValue());
        assertTrue(leafType.getLengths().isEmpty());
        assertTrue(leafType.getPatterns().isEmpty());
        List<RangeConstraint> baseRanges = base.getRanges();
        assertEquals(1, baseRanges.size());
        RangeConstraint baseRange = baseRanges.get(0);
        assertEquals(2L, baseRange.getMin());
        assertEquals(20L, baseRange.getMax());

        assertTrue(base.getBaseType() instanceof Int32);
    }

    @Test
    public void testTypedefPatternsResolving() {
        Module testModule = TestUtils.findModule(modules, "nodes");
        LeafSchemaNode stringleaf = (LeafSchemaNode) testModule.getDataChildByName("string-leaf");

        ExtendedType type = (ExtendedType) stringleaf.getType();
        QName typeQName = type.getQName();
        assertEquals("string-ext4", typeQName.getLocalName());
        assertEquals("t", typeQName.getPrefix());
        assertEquals(typesNS, typeQName.getNamespace());
        assertEquals(typesRev, typeQName.getRevision());
        assertNull(type.getUnits());
        assertNull(type.getDefaultValue());
        List<PatternConstraint> patterns = type.getPatterns();
        assertEquals(1, patterns.size());
        PatternConstraint pattern = patterns.iterator().next();
        assertEquals("[e-z]*", pattern.getRegularExpression());
        assertTrue(type.getLengths().isEmpty());
        assertTrue(type.getRanges().isEmpty());

        ExtendedType baseType1 = (ExtendedType) type.getBaseType();
        QName baseType1QName = baseType1.getQName();
        assertEquals("string-ext3", baseType1QName.getLocalName());
        assertEquals("t", baseType1QName.getPrefix());
        assertEquals(typesNS, baseType1QName.getNamespace());
        assertEquals(typesRev, baseType1QName.getRevision());
        assertNull(baseType1.getUnits());
        assertNull(baseType1.getDefaultValue());
        patterns = baseType1.getPatterns();
        assertEquals(1, patterns.size());
        pattern = patterns.iterator().next();
        assertEquals("[b-u]*", pattern.getRegularExpression());
        assertTrue(baseType1.getLengths().isEmpty());
        assertTrue(baseType1.getRanges().isEmpty());

        ExtendedType baseType2 = (ExtendedType) baseType1.getBaseType();
        QName baseType2QName = baseType2.getQName();
        assertEquals("string-ext2", baseType2QName.getLocalName());
        assertEquals("t", baseType2QName.getPrefix());
        assertEquals(typesNS, baseType2QName.getNamespace());
        assertEquals(typesRev, baseType2QName.getRevision());
        assertNull(baseType2.getUnits());
        assertNull(baseType2.getDefaultValue());
        assertTrue(baseType2.getPatterns().isEmpty());
        List<LengthConstraint> baseType2Lengths = baseType2.getLengths();
        assertEquals(1, baseType2Lengths.size());
        LengthConstraint length = baseType2Lengths.get(0);
        assertEquals(6L, length.getMin());
        assertEquals(10L, length.getMax());
        assertTrue(baseType2.getRanges().isEmpty());

        ExtendedType baseType3 = (ExtendedType) baseType2.getBaseType();
        QName baseType3QName = baseType3.getQName();
        assertEquals("string-ext1", baseType3QName.getLocalName());
        assertEquals("t", baseType3QName.getPrefix());
        assertEquals(typesNS, baseType3QName.getNamespace());
        assertEquals(typesRev, baseType3QName.getRevision());
        assertNull(baseType3.getUnits());
        assertNull(baseType3.getDefaultValue());
        patterns = baseType3.getPatterns();
        assertEquals(1, patterns.size());
        pattern = patterns.iterator().next();
        assertEquals("[a-k]*", pattern.getRegularExpression());
        List<LengthConstraint> baseType3Lengths = baseType3.getLengths();
        assertEquals(1, baseType3Lengths.size());
        length = baseType3Lengths.get(0);
        assertEquals(5L, length.getMin());
        assertEquals(11L, length.getMax());
        assertTrue(baseType3.getRanges().isEmpty());

        assertTrue(baseType3.getBaseType() instanceof StringType);
    }

    @Test
    public void testTypedefLengthsResolving() {
        Module testModule = TestUtils.findModule(modules, "nodes");

        LeafSchemaNode lengthLeaf = (LeafSchemaNode) testModule.getDataChildByName("length-leaf");
        ExtendedType type = (ExtendedType) lengthLeaf.getType();

        QName typeQName = type.getQName();
        assertEquals("string-ext2", typeQName.getLocalName());
        assertEquals("n", typeQName.getPrefix());
        assertEquals(nodesNS, typeQName.getNamespace());
        assertEquals(nodesRev, typeQName.getRevision());
        assertNull(type.getUnits());
        assertNull(type.getDefaultValue());
        assertTrue(type.getPatterns().isEmpty());
        List<LengthConstraint> typeLengths = type.getLengths();
        assertEquals(1, typeLengths.size());
        LengthConstraint length = typeLengths.get(0);
        assertEquals(7L, length.getMin());
        assertEquals(10L, length.getMax());
        assertTrue(type.getRanges().isEmpty());

        ExtendedType baseType1 = (ExtendedType) type.getBaseType();
        QName baseType1QName = baseType1.getQName();
        assertEquals("string-ext2", baseType1QName.getLocalName());
        assertEquals("t", baseType1QName.getPrefix());
        assertEquals(typesNS, baseType1QName.getNamespace());
        assertEquals(typesRev, baseType1QName.getRevision());
        assertNull(baseType1.getUnits());
        assertNull(baseType1.getDefaultValue());
        assertTrue(baseType1.getPatterns().isEmpty());
        List<LengthConstraint> baseType2Lengths = baseType1.getLengths();
        assertEquals(1, baseType2Lengths.size());
        length = baseType2Lengths.get(0);
        assertEquals(6L, length.getMin());
        assertEquals(10L, length.getMax());
        assertTrue(baseType1.getRanges().isEmpty());

        ExtendedType baseType2 = (ExtendedType) baseType1.getBaseType();
        QName baseType2QName = baseType2.getQName();
        assertEquals("string-ext1", baseType2QName.getLocalName());
        assertEquals("t", baseType2QName.getPrefix());
        assertEquals(typesNS, baseType2QName.getNamespace());
        assertEquals(typesRev, baseType2QName.getRevision());
        assertNull(baseType2.getUnits());
        assertNull(baseType2.getDefaultValue());
        List<PatternConstraint> patterns = baseType2.getPatterns();
        assertEquals(1, patterns.size());
        PatternConstraint pattern = patterns.iterator().next();
        assertEquals("[a-k]*", pattern.getRegularExpression());
        List<LengthConstraint> baseType3Lengths = baseType2.getLengths();
        assertEquals(1, baseType3Lengths.size());
        length = baseType3Lengths.get(0);
        assertEquals(5L, length.getMin());
        assertEquals(11L, length.getMax());
        assertTrue(baseType2.getRanges().isEmpty());

        assertTrue(baseType2.getBaseType() instanceof StringType);
    }

    @Test
    public void testTypedefDecimal1() {
        Module testModule = TestUtils.findModule(modules, "nodes");
        LeafSchemaNode testleaf = (LeafSchemaNode) testModule.getDataChildByName("decimal-leaf");

        ExtendedType type = (ExtendedType) testleaf.getType();
        QName typeQName = type.getQName();
        assertEquals("my-decimal-type", typeQName.getLocalName());
        assertEquals("n", typeQName.getPrefix());
        assertEquals(nodesNS, typeQName.getNamespace());
        assertEquals(nodesRev, typeQName.getRevision());
        assertNull(type.getUnits());
        assertNull(type.getDefaultValue());
        assertEquals(4, (int) type.getFractionDigits());
        assertTrue(type.getLengths().isEmpty());
        assertTrue(type.getPatterns().isEmpty());
        assertTrue(type.getRanges().isEmpty());

        ExtendedType typeBase = (ExtendedType) type.getBaseType();
        QName typeBaseQName = typeBase.getQName();
        assertEquals("my-decimal-type", typeBaseQName.getLocalName());
        assertEquals("t", typeBaseQName.getPrefix());
        assertEquals(typesNS, typeBaseQName.getNamespace());
        assertEquals(typesRev, typeBaseQName.getRevision());
        assertNull(typeBase.getUnits());
        assertNull(typeBase.getDefaultValue());
        assertNull(typeBase.getFractionDigits());
        assertTrue(typeBase.getLengths().isEmpty());
        assertTrue(typeBase.getPatterns().isEmpty());
        assertTrue(typeBase.getRanges().isEmpty());

        Decimal64 decimal = (Decimal64) typeBase.getBaseType();
        assertEquals(6, (int) decimal.getFractionDigits());
    }

    @Test
    public void testTypedefDecimal2() {
        Module testModule = TestUtils.findModule(modules, "nodes");
        LeafSchemaNode testleaf = (LeafSchemaNode) testModule.getDataChildByName("decimal-leaf2");

        ExtendedType type = (ExtendedType) testleaf.getType();
        QName typeQName = type.getQName();
        assertEquals("my-decimal-type", typeQName.getLocalName());
        assertEquals("t", typeQName.getPrefix());
        assertEquals(typesNS, typeQName.getNamespace());
        assertEquals(typesRev, typeQName.getRevision());
        assertNull(type.getUnits());
        assertNull(type.getDefaultValue());
        assertNull(type.getFractionDigits());
        assertTrue(type.getLengths().isEmpty());
        assertTrue(type.getPatterns().isEmpty());
        assertTrue(type.getRanges().isEmpty());

        Decimal64 baseTypeDecimal = (Decimal64) type.getBaseType();
        assertEquals(6, (int) baseTypeDecimal.getFractionDigits());
    }

    @Test
    public void testTypedefUnion() {
        Module testModule = TestUtils.findModule(modules, "nodes");
        LeafSchemaNode unionleaf = (LeafSchemaNode) testModule.getDataChildByName("union-leaf");

        ExtendedType type = (ExtendedType) unionleaf.getType();
        QName typeQName = type.getQName();
        assertEquals("my-union-ext", typeQName.getLocalName());
        assertEquals("t", typeQName.getPrefix());
        assertEquals(typesNS, typeQName.getNamespace());
        assertEquals(typesRev, typeQName.getRevision());
        assertNull(type.getUnits());
        assertNull(type.getDefaultValue());
        assertNull(type.getFractionDigits());
        assertTrue(type.getLengths().isEmpty());
        assertTrue(type.getPatterns().isEmpty());
        assertTrue(type.getRanges().isEmpty());

        ExtendedType baseType = (ExtendedType) type.getBaseType();
        QName baseTypeQName = baseType.getQName();
        assertEquals("my-union", baseTypeQName.getLocalName());
        assertEquals("t", baseTypeQName.getPrefix());
        assertEquals(typesNS, baseTypeQName.getNamespace());
        assertEquals(typesRev, baseTypeQName.getRevision());
        assertNull(baseType.getUnits());
        assertNull(baseType.getDefaultValue());
        assertNull(baseType.getFractionDigits());
        assertTrue(baseType.getLengths().isEmpty());
        assertTrue(baseType.getPatterns().isEmpty());
        assertTrue(baseType.getRanges().isEmpty());

        UnionType unionType = (UnionType) baseType.getBaseType();
        List<TypeDefinition<?>> unionTypes = unionType.getTypes();
        assertEquals(2, unionTypes.size());

        ExtendedType unionType1 = (ExtendedType) unionTypes.get(0);
        QName unionType1QName = baseType.getQName();
        assertEquals("my-union", unionType1QName.getLocalName());
        assertEquals("t", unionType1QName.getPrefix());
        assertEquals(typesNS, unionType1QName.getNamespace());
        assertEquals(typesRev, unionType1QName.getRevision());
        assertNull(unionType1.getUnits());
        assertNull(unionType1.getDefaultValue());
        assertNull(unionType1.getFractionDigits());
        assertTrue(unionType1.getLengths().isEmpty());
        assertTrue(unionType1.getPatterns().isEmpty());
        List<RangeConstraint> ranges = unionType1.getRanges();
        assertEquals(1, ranges.size());
        RangeConstraint range = ranges.get(0);
        assertEquals(1L, range.getMin());
        assertEquals(100L, range.getMax());
        assertTrue(unionType1.getBaseType() instanceof Int16);

        assertTrue(unionTypes.get(1) instanceof Int32);
    }

    @Test
    public void testNestedUnionResolving() {
        Module testModule = TestUtils.findModule(modules, "nodes");
        LeafSchemaNode testleaf = (LeafSchemaNode) testModule.getDataChildByName("custom-union-leaf");

        ExtendedType type = (ExtendedType) testleaf.getType();
        QName testleafTypeQName = type.getQName();
        assertEquals(customNS, testleafTypeQName.getNamespace());
        assertEquals(customRev, testleafTypeQName.getRevision());
        assertEquals("c", testleafTypeQName.getPrefix());
        assertEquals("union1", testleafTypeQName.getLocalName());
        assertNull(type.getUnits());
        assertNull(type.getDefaultValue());
        assertNull(type.getFractionDigits());
        assertTrue(type.getLengths().isEmpty());
        assertTrue(type.getPatterns().isEmpty());
        assertTrue(type.getRanges().isEmpty());

        ExtendedType typeBase = (ExtendedType) type.getBaseType();
        QName typeBaseQName = typeBase.getQName();
        assertEquals(customNS, typeBaseQName.getNamespace());
        assertEquals(customRev, typeBaseQName.getRevision());
        assertEquals("c", typeBaseQName.getPrefix());
        assertEquals("union2", typeBaseQName.getLocalName());
        assertNull(typeBase.getUnits());
        assertNull(typeBase.getDefaultValue());
        assertNull(typeBase.getFractionDigits());
        assertTrue(typeBase.getLengths().isEmpty());
        assertTrue(typeBase.getPatterns().isEmpty());
        assertTrue(typeBase.getRanges().isEmpty());

        UnionType union = (UnionType) typeBase.getBaseType();
        List<TypeDefinition<?>> unionTypes = union.getTypes();
        assertEquals(2, unionTypes.size());
        assertTrue(unionTypes.get(0) instanceof Int32);
        assertTrue(unionTypes.get(1) instanceof ExtendedType);

        ExtendedType unionType1 = (ExtendedType) unionTypes.get(1);
        QName uniontType1QName = unionType1.getQName();
        assertEquals(typesNS, uniontType1QName.getNamespace());
        assertEquals(typesRev, uniontType1QName.getRevision());
        assertEquals("t", uniontType1QName.getPrefix());
        assertEquals("nested-union2", uniontType1QName.getLocalName());
        assertNull(unionType1.getUnits());
        assertNull(unionType1.getDefaultValue());
        assertNull(unionType1.getFractionDigits());
        assertTrue(unionType1.getLengths().isEmpty());
        assertTrue(unionType1.getPatterns().isEmpty());
        assertTrue(unionType1.getRanges().isEmpty());

        UnionType nestedUnion = (UnionType) unionType1.getBaseType();
        List<TypeDefinition<?>> nestedUnion2Types = nestedUnion.getTypes();
        assertEquals(2, nestedUnion2Types.size());
        assertTrue(nestedUnion2Types.get(0) instanceof StringType);
        assertTrue(nestedUnion2Types.get(1) instanceof ExtendedType);

        ExtendedType myUnionExt = (ExtendedType) nestedUnion2Types.get(1);
        QName myUnionExtQName = myUnionExt.getQName();
        assertEquals(typesNS, myUnionExtQName.getNamespace());
        assertEquals(typesRev, myUnionExtQName.getRevision());
        assertEquals("t", myUnionExtQName.getPrefix());
        assertEquals("my-union-ext", myUnionExtQName.getLocalName());
        assertNull(myUnionExt.getUnits());
        assertNull(myUnionExt.getDefaultValue());
        assertNull(myUnionExt.getFractionDigits());
        assertTrue(myUnionExt.getLengths().isEmpty());
        assertTrue(myUnionExt.getPatterns().isEmpty());
        assertTrue(myUnionExt.getRanges().isEmpty());

        ExtendedType myUnion = (ExtendedType) myUnionExt.getBaseType();
        QName myUnionQName = myUnion.getQName();
        assertEquals(typesNS, myUnionQName.getNamespace());
        assertEquals(typesRev, myUnionQName.getRevision());
        assertEquals("t", myUnionQName.getPrefix());
        assertEquals("my-union", myUnionQName.getLocalName());
        assertNull(myUnion.getUnits());
        assertNull(myUnion.getDefaultValue());
        assertNull(myUnion.getFractionDigits());
        assertTrue(myUnion.getLengths().isEmpty());
        assertTrue(myUnion.getPatterns().isEmpty());
        assertTrue(myUnion.getRanges().isEmpty());

        UnionType myUnionBase = (UnionType) myUnion.getBaseType();
        List<TypeDefinition<?>> myUnionBaseTypes = myUnionBase.getTypes();
        assertEquals(2, myUnionBaseTypes.size());
        assertTrue(myUnionBaseTypes.get(0) instanceof ExtendedType);
        assertTrue(myUnionBaseTypes.get(1) instanceof Int32);

        ExtendedType int16Ext = (ExtendedType) myUnionBaseTypes.get(0);
        QName int16ExtQName = int16Ext.getQName();
        assertEquals(typesNS, int16ExtQName.getNamespace());
        assertEquals(typesRev, int16ExtQName.getRevision());
        assertEquals("t", int16ExtQName.getPrefix());
        assertEquals("int16", int16ExtQName.getLocalName());
        assertNull(int16Ext.getUnits());
        assertNull(int16Ext.getDefaultValue());
        assertNull(int16Ext.getFractionDigits());
        assertTrue(int16Ext.getLengths().isEmpty());
        assertTrue(int16Ext.getPatterns().isEmpty());
        List<RangeConstraint> ranges = int16Ext.getRanges();
        assertEquals(1, ranges.size());
        RangeConstraint range = ranges.get(0);
        assertEquals(1L, range.getMin());
        assertEquals(100L, range.getMax());

        assertTrue(int16Ext.getBaseType() instanceof Int16);
    }

    @Test
    public void testChoice() {
        Module testModule = TestUtils.findModule(modules, "nodes");
        ContainerSchemaNode transfer = (ContainerSchemaNode) testModule.getDataChildByName("transfer");
        ChoiceNode how = (ChoiceNode) transfer.getDataChildByName("how");
        Set<ChoiceCaseNode> cases = how.getCases();
        assertEquals(5, cases.size());
        ChoiceCaseNode input = null;
        ChoiceCaseNode output = null;
        for (ChoiceCaseNode caseNode : cases) {
            if ("input".equals(caseNode.getQName().getLocalName())) {
                input = caseNode;
            } else if ("output".equals(caseNode.getQName().getLocalName())) {
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
        Module testModule = TestUtils.findModule(modules, "nodes");
        AnyXmlSchemaNode data = (AnyXmlSchemaNode) testModule.getDataChildByName("data");
        assertNotNull("anyxml data not found", data);

        // test SchemaNode args
        QName qname = data.getQName();
        assertEquals("data", qname.getLocalName());
        assertEquals("n", qname.getPrefix());
        assertEquals(nodesNS, qname.getNamespace());
        assertEquals(nodesRev, qname.getRevision());
        assertTrue(data.getDescription().contains("Copy of the source typesstore subset that matched"));
        assertNull(data.getReference());
        assertEquals(Status.OBSOLETE, data.getStatus());
        assertEquals(0, data.getUnknownSchemaNodes().size());
        // test DataSchemaNode args
        assertFalse(data.isAugmenting());
        assertTrue(data.isConfiguration());
        ConstraintDefinition constraints = data.getConstraints();
        assertNull(constraints.getWhenCondition());
        assertEquals(0, constraints.getMustConstraints().size());
        assertFalse(constraints.isMandatory());
        assertNull(constraints.getMinElements());
        assertNull(constraints.getMaxElements());
    }

    @Test
    public void testDeviation() {
        Module testModule = TestUtils.findModule(modules, "nodes");
        Set<Deviation> deviations = testModule.getDeviations();
        assertEquals(1, deviations.size());
        Deviation dev = deviations.iterator().next();

        assertEquals("system/user ref", dev.getReference());

        List<QName> path = new ArrayList<QName>();
        path.add(new QName(typesNS, typesRev, "t", "interfaces"));
        path.add(new QName(typesNS, typesRev, "t", "ifEntry"));
        SchemaPath expectedPath = new SchemaPath(path, true);

        assertEquals(expectedPath, dev.getTargetPath());
        assertEquals(Deviate.ADD, dev.getDeviate());
    }

    @Test
    public void testUnknownNode() {
        Module testModule = TestUtils.findModule(modules, "custom");
        ContainerSchemaNode network = (ContainerSchemaNode) testModule.getDataChildByName("network");
        List<UnknownSchemaNode> unknownNodes = network.getUnknownSchemaNodes();
        assertEquals(1, unknownNodes.size());
        UnknownSchemaNode unknownNode = unknownNodes.get(0);
        assertNotNull(unknownNode.getNodeType());
        assertEquals("point", unknownNode.getNodeParameter());
    }

    @Test
    public void testFeature() {
        Module testModule = TestUtils.findModule(modules, "custom");
        Set<FeatureDefinition> features = testModule.getFeatures();
        assertEquals(1, features.size());
    }

    @Test
    public void testExtension() {
        Module testModule = TestUtils.findModule(modules, "custom");
        List<ExtensionDefinition> extensions = testModule.getExtensionSchemaNodes();
        assertEquals(1, extensions.size());
        ExtensionDefinition extension = extensions.get(0);
        assertEquals("name", extension.getArgument());
        assertTrue(extension.isYinElement());
    }

    @Test
    public void testNotification() {
        Module testModule = TestUtils.findModule(modules, "custom");
        String expectedPrefix = "c";

        Set<NotificationDefinition> notifications = testModule.getNotifications();
        assertEquals(1, notifications.size());

        NotificationDefinition notification = notifications.iterator().next();
        // test SchemaNode args
        QName expectedQName = new QName(customNS, customRev, expectedPrefix, "event");
        assertEquals(expectedQName, notification.getQName());
        SchemaPath expectedPath = TestUtils.createPath(true, customNS, customRev, expectedPrefix, "event");
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
        Module testModule = TestUtils.findModule(modules, "custom");

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
    public void testTypePath() throws ParseException {
        Module test = TestUtils.findModule(modules, "types");
        Set<TypeDefinition<?>> types = test.getTypeDefinitions();

        // my-base-int32-type
        ExtendedType int32Typedef = (ExtendedType) TestUtils.findTypedef(types, "int32-ext1");
        QName int32TypedefQName = int32Typedef.getQName();

        assertEquals(typesNS, int32TypedefQName.getNamespace());
        assertEquals(typesRev, int32TypedefQName.getRevision());
        assertEquals("t", int32TypedefQName.getPrefix());
        assertEquals("int32-ext1", int32TypedefQName.getLocalName());

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
        Module test = TestUtils.findModule(modules, "types");
        Set<TypeDefinition<?>> types = test.getTypeDefinitions();

        // my-base-int32-type
        ExtendedType myDecType = (ExtendedType) TestUtils.findTypedef(types, "my-decimal-type");
        QName myDecTypeQName = myDecType.getQName();

        assertEquals(typesNS, myDecTypeQName.getNamespace());
        assertEquals(typesRev, myDecTypeQName.getRevision());
        assertEquals("t", myDecTypeQName.getPrefix());
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
