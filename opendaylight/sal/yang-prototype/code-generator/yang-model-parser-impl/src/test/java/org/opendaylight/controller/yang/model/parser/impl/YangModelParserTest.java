/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.parser.impl;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.AugmentationSchema;
import org.opendaylight.controller.yang.model.api.ContainerSchemaNode;
import org.opendaylight.controller.yang.model.api.LeafSchemaNode;
import org.opendaylight.controller.yang.model.api.ListSchemaNode;
import org.opendaylight.controller.yang.model.api.Module;
import org.opendaylight.controller.yang.model.api.TypeDefinition;
import org.opendaylight.controller.yang.model.api.type.LengthConstraint;
import org.opendaylight.controller.yang.model.api.type.PatternConstraint;
import org.opendaylight.controller.yang.model.api.type.RangeConstraint;
import org.opendaylight.controller.yang.model.parser.api.YangModelParser;
import org.opendaylight.controller.yang.model.util.Decimal64;
import org.opendaylight.controller.yang.model.util.ExtendedType;
import org.opendaylight.controller.yang.model.util.Int16;
import org.opendaylight.controller.yang.model.util.Int32;
import org.opendaylight.controller.yang.model.util.StringType;
import org.opendaylight.controller.yang.model.util.UnionType;

public class YangModelParserTest {

    private static final String TEST_FILE1 = "src/test/resources/model/testfile1.yang";
    private static final String TEST_FILE2 = "src/test/resources/model/testfile2.yang";
    private YangModelParser tested;

    @Before
    public void init() {
        tested = new YangModelParserImpl();
    }

    @Test
    public void testAugment() {
        Set<Module> modules = tested.parseYangModels(TEST_FILE1, TEST_FILE2);
        assertEquals(2, modules.size());

        Module testModule = findModule(modules, "types2");
        assertNotNull(testModule);

        AugmentationSchema augment = testModule.getAugmentations().iterator().next();
        assertNotNull(augment);
    }

    @Test
    public void testAugmentTarget() {
        Set<Module> modules = tested.parseYangModels(TEST_FILE1, TEST_FILE2);
        assertEquals(2, modules.size());

        Module testModule = findModule(modules, "types1");
        assertNotNull(testModule);

        ContainerSchemaNode container = (ContainerSchemaNode)testModule.getDataChildByName("interfaces");
        assertNotNull(container);

        ListSchemaNode list = (ListSchemaNode)container.getDataChildByName("ifEntry");
        assertNotNull(list);
        assertEquals(1, list.getAvailableAugmentations().size());

        LeafSchemaNode leaf = (LeafSchemaNode)list.getDataChildByName("ds0ChannelNumber");
        assertNotNull(leaf);
    }

    @Test
    public void testTypedefRangesResolving() {
        Set<Module> modules = tested.parseYangModels(TEST_FILE1, TEST_FILE2);
        assertEquals(2, modules.size());

        Module testModule = findModule(modules, "types1");
        assertNotNull(testModule);

        LeafSchemaNode testleaf = (LeafSchemaNode)testModule.getDataChildByName("testleaf");
        ExtendedType leafType = (ExtendedType)testleaf.getType();
        assertEquals("my-type1", leafType.getQName().getLocalName());
        assertEquals("t2", leafType.getQName().getPrefix());
        ExtendedType baseType = (ExtendedType)leafType.getBaseType();
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
        Set<Module> modules = tested.parseYangModels(TEST_FILE1, TEST_FILE2);
        assertEquals(2, modules.size());

        Module testModule = findModule(modules, "types1");
        assertNotNull(testModule);

        LeafSchemaNode testleaf = (LeafSchemaNode)testModule.getDataChildByName("test-string-leaf");
        ExtendedType testleafType = (ExtendedType)testleaf.getType();
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
        Set<Module> modules = tested.parseYangModels(TEST_FILE1, TEST_FILE2);
        assertEquals(2, modules.size());

        Module testModule = findModule(modules, "types1");
        assertNotNull(testModule);

        LeafSchemaNode testleaf = (LeafSchemaNode)testModule.getDataChildByName("leaf-with-length");
        ExtendedType testleafType = (ExtendedType)testleaf.getType();
        assertEquals("my-string-type", testleafType.getQName().getLocalName());

        List<LengthConstraint> lengths = testleafType.getLengths();
        assertEquals(1, lengths.size());

        LengthConstraint length = lengths.get(0);
        assertEquals(7L, length.getMin());
        assertEquals(10L, length.getMax());
    }

    @Test
    public void testTypeDef() {
        Set<Module> modules = tested.parseYangModels(TEST_FILE1, TEST_FILE2);
        assertEquals(2, modules.size());

        Module testModule = findModule(modules, "types2");
        assertNotNull(testModule);

        LeafSchemaNode testleaf = (LeafSchemaNode)testModule.getDataChildByName("nested-type-leaf");
        ExtendedType testleafType = (ExtendedType)testleaf.getType();
        assertEquals("my-type1", testleafType.getQName().getLocalName());

        ExtendedType baseType = (ExtendedType)testleafType.getBaseType();
        assertEquals("my-base-int32-type", baseType.getQName().getLocalName());

        Int32 int32base = (Int32)baseType.getBaseType();
        List<RangeConstraint> ranges = int32base.getRangeStatements();
        assertEquals(1, ranges.size());
        RangeConstraint range = ranges.get(0);
        assertEquals(2L, range.getMin());
        assertEquals(20L, range.getMax());
    }

    @Test
    public void testTypedefDecimal1() {
        Set<Module> modules = tested.parseYangModels(TEST_FILE1, TEST_FILE2);
        assertEquals(2, modules.size());

        Module testModule = findModule(modules, "types1");
        assertNotNull(testModule);

        LeafSchemaNode testleaf = (LeafSchemaNode)testModule.getDataChildByName("test-decimal-leaf");
        ExtendedType type = (ExtendedType)testleaf.getType();

        TypeDefinition<?> baseType = type.getBaseType();
        assertTrue(baseType instanceof Decimal64);
        Decimal64 baseTypeCast = (Decimal64)baseType;
        assertEquals(6, (int)baseTypeCast.getFractionDigits());
    }

    @Test
    public void testTypedefDecimal2() {
        Set<Module> modules = tested.parseYangModels(TEST_FILE1, TEST_FILE2);
        assertEquals(2, modules.size());

        Module testModule = findModule(modules, "types1");
        assertNotNull(testModule);

        LeafSchemaNode testleaf = (LeafSchemaNode)testModule.getDataChildByName("test-decimal-leaf2");
        TypeDefinition<?> baseType = testleaf.getType().getBaseType();
        assertTrue(testleaf.getType().getBaseType() instanceof Decimal64);
        Decimal64 baseTypeCast = (Decimal64)baseType;
        assertEquals(5, (int)baseTypeCast.getFractionDigits());
    }

    @Test
    public void testTypedefUnion() {
        Set<Module> modules = tested.parseYangModels(TEST_FILE1, TEST_FILE2);
        assertEquals(2, modules.size());

        Module testModule = findModule(modules, "types1");
        assertNotNull(testModule);

        LeafSchemaNode testleaf = (LeafSchemaNode)testModule.getDataChildByName("union-leaf");
        ExtendedType testleafType = (ExtendedType)testleaf.getType();
        assertEquals("my-union-ext", testleafType.getQName().getLocalName());

        ExtendedType baseType = (ExtendedType)testleafType.getBaseType();
        assertEquals("my-union", baseType.getQName().getLocalName());

        UnionType unionBase = (UnionType) baseType.getBaseType();

        List<TypeDefinition<?>> unionTypes = unionBase.getTypes();
        Int16 unionType1 = (Int16)unionTypes.get(0);
        List<RangeConstraint> ranges = unionType1.getRangeStatements();
        assertEquals(1, ranges.size());
        RangeConstraint range = ranges.get(0);
        assertEquals(1L, range.getMin());
        assertEquals(100L, range.getMax());

        assertTrue(unionTypes.get(0) instanceof Int16);
        assertTrue(unionTypes.get(1) instanceof Int32);
    }

    @Test
    public void testNestedUnionResolving() {
        Set<Module> modules = tested.parseYangModels(TEST_FILE1, TEST_FILE2);
        assertEquals(2, modules.size());

        Module testModule = findModule(modules, "types1");
        assertNotNull(testModule);

        LeafSchemaNode testleaf = (LeafSchemaNode)testModule.getDataChildByName("nested-union-leaf");

        ExtendedType nestedUnion1 = (ExtendedType)testleaf.getType();
        assertEquals("nested-union1", nestedUnion1.getQName().getLocalName());

        ExtendedType nestedUnion2 = (ExtendedType)nestedUnion1.getBaseType();
        assertEquals("nested-union2", nestedUnion2.getQName().getLocalName());

        UnionType unionType1 = (UnionType)nestedUnion2.getBaseType();
        List<TypeDefinition<?>> unionTypes = unionType1.getTypes();
        assertEquals(2, unionTypes.size());
        assertTrue(unionTypes.get(0) instanceof StringType);
        assertTrue(unionTypes.get(1) instanceof ExtendedType);

        ExtendedType extendedUnion = (ExtendedType)unionTypes.get(1);
        ExtendedType extendedUnionBase = (ExtendedType)extendedUnion.getBaseType();
        assertEquals("my-union", extendedUnionBase.getQName().getLocalName());

        UnionType extendedTargetUnion = (UnionType)extendedUnionBase.getBaseType();
        List<TypeDefinition<?>> extendedTargetTypes = extendedTargetUnion.getTypes();
        assertTrue(extendedTargetTypes.get(0) instanceof Int16);
        assertTrue(extendedTargetTypes.get(1) instanceof Int32);

        Int16 int16 = (Int16) extendedTargetTypes.get(0);
        List<RangeConstraint> ranges = int16.getRangeStatements();
        assertEquals(1, ranges.size());
        RangeConstraint range = ranges.get(0);
        assertEquals(1L, range.getMin());
        assertEquals(100L, range.getMax());
    }

    private Module findModule(Set<Module> modules, String moduleName) {
        Module result = null;
        for(Module module : modules) {
            if(module.getName().equals(moduleName)) {
                result = module;
                break;
            }
        }
        return result;
    }

}
