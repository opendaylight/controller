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
import org.opendaylight.controller.yang.model.api.AugmentationSchema;
import org.opendaylight.controller.yang.model.api.ContainerSchemaNode;
import org.opendaylight.controller.yang.model.api.LeafSchemaNode;
import org.opendaylight.controller.yang.model.api.ListSchemaNode;
import org.opendaylight.controller.yang.model.api.Module;
import org.opendaylight.controller.yang.model.api.TypeDefinition;
import org.opendaylight.controller.yang.model.api.type.LengthConstraint;
import org.opendaylight.controller.yang.model.api.type.PatternConstraint;
import org.opendaylight.controller.yang.model.api.type.RangeConstraint;
import org.opendaylight.controller.yang.model.api.type.StringTypeDefinition;
import org.opendaylight.controller.yang.model.parser.api.YangModelParser;
import org.opendaylight.controller.yang.model.util.Decimal64;
import org.opendaylight.controller.yang.model.util.Int32;
import org.opendaylight.controller.yang.model.util.StringType;

public class YangModelParserTest {

    private final String testFile1 = "src/test/resources/model/testfile1.yang";
    private final String testFile2 = "src/test/resources/model/testfile2.yang";
    private YangModelParser tested;

    @Before
    public void init() {
        tested = new YangModelParserImpl();
    }

    @Test
    public void testAugment() {
        Set<Module> modules = tested.parseYangModels(testFile1, testFile2);
        assertEquals(2, modules.size());

        Module m2 = null;
        for(Module m : modules) {
            if(m.getName().equals("types2")) {
                m2 = m;
            }
        }
        assertNotNull(m2);

        AugmentationSchema augment = m2.getAugmentations().iterator().next();
        assertNotNull(augment);
    }

    @Test
    public void testAugmentTarget() {
        Set<Module> modules = tested.parseYangModels(testFile1, testFile2);
        assertEquals(2, modules.size());

        Module m1 = null;
        for(Module m : modules) {
            if(m.getName().equals("types1")) {
                m1 = m;
            }
        }
        assertNotNull(m1);

        ContainerSchemaNode container = (ContainerSchemaNode)m1.getDataChildByName("interfaces");
        assertNotNull(container);

        ListSchemaNode list = (ListSchemaNode)container.getDataChildByName("ifEntry");
        assertNotNull(list);
        assertEquals(1, list.getAvailableAugmentations().size());

        LeafSchemaNode leaf = (LeafSchemaNode)list.getDataChildByName("ds0ChannelNumber");
        assertNotNull(leaf);
    }

    @Test
    public void testTypedefRangesResolving() {
        Set<Module> modules = tested.parseYangModels(testFile1, testFile2);
        assertEquals(2, modules.size());

        Module m1 = null;
        for(Module m : modules) {
            if(m.getName().equals("types1")) {
                m1 = m;
            }
        }
        assertNotNull(m1);

        LeafSchemaNode testleaf = (LeafSchemaNode)m1.getDataChildByName("testleaf");
        TypeDefinition<?> baseType = testleaf.getType().getBaseType();
        assertTrue(testleaf.getType().getBaseType() instanceof Int32);
        Int32 baseTypeCast = (Int32)baseType;
        List<RangeConstraint> ranges = baseTypeCast.getRangeStatements();
        assertEquals(1, ranges.size());
        RangeConstraint range = ranges.get(0);
        assertEquals(11L, range.getMin());
        assertEquals(20L, range.getMax());
    }

    @Test
    public void testTypedefPatternsResolving() {
        Set<Module> modules = tested.parseYangModels(testFile1, testFile2);
        assertEquals(2, modules.size());

        Module m1 = null;
        for(Module m : modules) {
            if(m.getName().equals("types1")) {
                m1 = m;
            }
        }
        assertNotNull(m1);

        LeafSchemaNode testleaf = (LeafSchemaNode)m1.getDataChildByName("test-string-leaf");
        TypeDefinition<?> baseType = testleaf.getType().getBaseType();
        assertTrue(testleaf.getType().getBaseType() instanceof StringType);
        StringType baseTypeCast = (StringType)baseType;

        Set<String> expectedRegularExpressions = new HashSet<String>();
        expectedRegularExpressions.add("[a-k]*");
        expectedRegularExpressions.add("[b-u]*");
        expectedRegularExpressions.add("[e-z]*");

        Set<String> actualRegularExpressions = new HashSet<String>();
        List<PatternConstraint> patterns = baseTypeCast.getPatterns();
        for(PatternConstraint pc : patterns) {
            actualRegularExpressions.add(pc.getRegularExpression());
        }

        assertEquals(expectedRegularExpressions, actualRegularExpressions);
    }

    @Test
    public void testTypedefLengthsResolving() {
        Set<Module> modules = tested.parseYangModels(testFile1, testFile2);
        assertEquals(2, modules.size());

        Module m1 = null;
        for(Module m : modules) {
            if(m.getName().equals("types1")) {
                m1 = m;
            }
        }
        assertNotNull(m1);

        LeafSchemaNode testleaf = (LeafSchemaNode)m1.getDataChildByName("leaf-with-length");
        TypeDefinition<?> baseType = testleaf.getType().getBaseType();
        assertTrue(testleaf.getType().getBaseType() instanceof StringTypeDefinition);
        StringType baseTypeCast = (StringType)baseType;

        List<LengthConstraint> actualLengths = baseTypeCast.getLengthStatements();
        assertEquals(1, actualLengths.size());
        assertEquals(7L, actualLengths.get(0).getMin());
        assertEquals(10L, actualLengths.get(0).getMax());
    }

    @Test
    public void testTypeDef() {
        Set<Module> modules = tested.parseYangModels(testFile1, testFile2);
        assertEquals(2, modules.size());

        Module m2 = null;
        for(Module m : modules) {
            if(m.getName().equals("types2")) {
                m2 = m;
            }
        }
        assertNotNull(m2);

        LeafSchemaNode testleaf = (LeafSchemaNode)m2.getDataChildByName("nested-type-leaf");
        TypeDefinition<?> baseType = testleaf.getType().getBaseType();
        assertTrue(testleaf.getType().getBaseType() instanceof Int32);
        Int32 baseTypeCast = (Int32)baseType;
        List<RangeConstraint> ranges = baseTypeCast.getRangeStatements();
        assertEquals(1, ranges.size());
        RangeConstraint range = ranges.get(0);
        assertEquals(11L, range.getMin());
        assertEquals(20L, range.getMax());
    }

    @Test
    public void testTypedefDecimal1() {
        Set<Module> modules = tested.parseYangModels(testFile1, testFile2);
        assertEquals(2, modules.size());

        Module m1 = null;
        for(Module m : modules) {
            if(m.getName().equals("types1")) {
                m1 = m;
            }
        }
        assertNotNull(m1);

        LeafSchemaNode testleaf = (LeafSchemaNode)m1.getDataChildByName("test-decimal-leaf");
        TypeDefinition<?> baseType = testleaf.getType().getBaseType();
        assertTrue(testleaf.getType().getBaseType() instanceof Decimal64);
        Decimal64 baseTypeCast = (Decimal64)baseType;
        assertEquals(4, (int)baseTypeCast.getFractionDigits());
    }

    @Test
    public void testTypedefDecimal2() {
        Set<Module> modules = tested.parseYangModels(testFile1, testFile2);
        assertEquals(2, modules.size());

        Module m1 = null;
        for(Module m : modules) {
            if(m.getName().equals("types1")) {
                m1 = m;
            }
        }
        assertNotNull(m1);

        LeafSchemaNode testleaf = (LeafSchemaNode)m1.getDataChildByName("test-decimal-leaf2");
        TypeDefinition<?> baseType = testleaf.getType().getBaseType();
        assertTrue(testleaf.getType().getBaseType() instanceof Decimal64);
        Decimal64 baseTypeCast = (Decimal64)baseType;
        assertEquals(5, (int)baseTypeCast.getFractionDigits());
    }

}
