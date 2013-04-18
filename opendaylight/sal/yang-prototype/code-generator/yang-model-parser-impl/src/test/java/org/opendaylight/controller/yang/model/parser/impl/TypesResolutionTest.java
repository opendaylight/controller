/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.parser.impl;

import static org.junit.Assert.*;

import java.io.File;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.yang.model.api.IdentitySchemaNode;
import org.opendaylight.controller.yang.model.api.LeafSchemaNode;
import org.opendaylight.controller.yang.model.api.Module;
import org.opendaylight.controller.yang.model.api.TypeDefinition;
import org.opendaylight.controller.yang.model.api.type.BitsTypeDefinition.Bit;
import org.opendaylight.controller.yang.model.api.type.EnumTypeDefinition.EnumPair;
import org.opendaylight.controller.yang.model.api.type.LengthConstraint;
import org.opendaylight.controller.yang.model.api.type.PatternConstraint;
import org.opendaylight.controller.yang.model.parser.api.YangModelParser;
import org.opendaylight.controller.yang.model.util.BitsType;
import org.opendaylight.controller.yang.model.util.EnumerationType;
import org.opendaylight.controller.yang.model.util.ExtendedType;
import org.opendaylight.controller.yang.model.util.InstanceIdentifier;
import org.opendaylight.controller.yang.model.util.StringType;
import org.opendaylight.controller.yang.model.util.UnionType;

public class TypesResolutionTest {

    private Set<Module> testedModules;

    @Before
    public void init() {
        YangModelParser parser = new YangModelParserImpl();
        File testDir = new File("src/test/resources/types");
        String[] fileList = testDir.list();
        String[] testFiles = new String[fileList.length];
        for (int i = 0; i < fileList.length; i++) {
            String fileName = fileList[i];
            File file = new File(testDir, fileName);
            testFiles[i] = file.getAbsolutePath();
        }
        testedModules = parser.parseYangModels(testFiles);
        assertEquals(fileList.length, testedModules.size());
    }

    @Test
    public void testIPVersion() {
        Module tested = findModule(testedModules, "ietf-inet-types");
        Set<TypeDefinition<?>> typedefs = tested.getTypeDefinitions();
        assertEquals(14, typedefs.size());

        TypeDefinition<?> type = findTypedef(typedefs, "ip-version");
        assertTrue(type.getDescription().contains(
                "This value represents the version of the IP protocol."));
        assertTrue(type.getReference().contains(
                "RFC 2460: Internet Protocol, Version 6 (IPv6) Specification"));

        EnumerationType enumType = (EnumerationType) type.getBaseType();
        List<EnumPair> values = enumType.getValues();
        assertEquals(3, values.size());

        EnumPair value0 = values.get(0);
        assertEquals("unknown", value0.getName());
        assertEquals(0, (int) value0.getValue());
        assertEquals(
                "An unknown or unspecified version of the Internet protocol.",
                value0.getDescription());

        EnumPair value1 = values.get(1);
        assertEquals("ipv4", value1.getName());
        assertEquals(1, (int) value1.getValue());
        assertEquals("The IPv4 protocol as defined in RFC 791.",
                value1.getDescription());

        EnumPair value2 = values.get(2);
        assertEquals("ipv6", value2.getName());
        assertEquals(2, (int) value2.getValue());
        assertEquals("The IPv6 protocol as defined in RFC 2460.",
                value2.getDescription());
    }

    @Test
    public void testIpAddress() {
        Module tested = findModule(testedModules, "ietf-inet-types");
        Set<TypeDefinition<?>> typedefs = tested.getTypeDefinitions();
        TypeDefinition<?> type = findTypedef(typedefs, "ip-address");
        UnionType baseType = (UnionType) type.getBaseType();
        List<TypeDefinition<?>> unionTypes = baseType.getTypes();

        ExtendedType ipv4 = (ExtendedType)unionTypes.get(0);
        StringType ipv4Base = (StringType) ipv4.getBaseType();
        String expectedPattern = "(([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])\\.){3}"
                + "([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])"
                + "(%[\\p{N}\\p{L}]+)?";
        assertEquals(expectedPattern, ipv4Base.getPatterns().get(0)
                .getRegularExpression());

        ExtendedType ipv6 = (ExtendedType)unionTypes.get(1);
        StringType ipv6Base = (StringType) ipv6.getBaseType();
        List<PatternConstraint> ipv6Patterns = ipv6Base.getPatterns();
        expectedPattern = "((:|[0-9a-fA-F]{0,4}):)([0-9a-fA-F]{0,4}:){0,5}"
                + "((([0-9a-fA-F]{0,4}:)?(:|[0-9a-fA-F]{0,4}))|"
                + "(((25[0-5]|2[0-4][0-9]|[01]?[0-9]?[0-9])\\.){3}"
                + "(25[0-5]|2[0-4][0-9]|[01]?[0-9]?[0-9])))"
                + "(%[\\p{N}\\p{L}]+)?";
        assertEquals(expectedPattern, ipv6Patterns.get(0)
                .getRegularExpression());

        expectedPattern = "(([^:]+:){6}(([^:]+:[^:]+)|(.*\\..*)))|"
                + "((([^:]+:)*[^:]+)?::(([^:]+:)*[^:]+)?)" + "(%.+)?";
        assertEquals(expectedPattern, ipv6Patterns.get(1)
                .getRegularExpression());
    }

    @Test
    public void testDomainName() {
        Module tested = findModule(testedModules, "ietf-inet-types");
        Set<TypeDefinition<?>> typedefs = tested.getTypeDefinitions();
        TypeDefinition<?> type = findTypedef(typedefs, "domain-name");
        StringType baseType = (StringType) type.getBaseType();
        List<PatternConstraint> patterns = baseType.getPatterns();
        assertEquals(1, patterns.size());
        String expectedPattern = "((([a-zA-Z0-9_]([a-zA-Z0-9\\-_]){0,61})?[a-zA-Z0-9]\\.)*"
                + "([a-zA-Z0-9_]([a-zA-Z0-9\\-_]){0,61})?[a-zA-Z0-9]\\.?)"
                + "|\\.";
        assertEquals(expectedPattern, patterns.get(0).getRegularExpression());

        List<LengthConstraint> lengths = baseType.getLengthStatements();
        assertEquals(1, lengths.size());
        LengthConstraint length = baseType.getLengthStatements().get(0);
        assertEquals(1L, length.getMin());
        assertEquals(253L, length.getMax());
    }

    @Test
    public void testInstanceIdentifier1() {
        Module tested = findModule(testedModules, "custom-types-test");
        LeafSchemaNode leaf = (LeafSchemaNode) tested
                .getDataChildByName("inst-id-leaf1");
        InstanceIdentifier leafType = (InstanceIdentifier) leaf.getType();
        assertFalse(leafType.requireInstance());
    }

    @Test
    public void testInstanceIdentifier2() {
        Module tested = findModule(testedModules, "custom-types-test");
        LeafSchemaNode leaf = (LeafSchemaNode) tested
                .getDataChildByName("inst-id-leaf2");
        InstanceIdentifier leafType = (InstanceIdentifier) leaf.getType();
        assertTrue(leafType.requireInstance());
    }

    @Test
    public void testIdentity() {
        Module tested = findModule(testedModules, "custom-types-test");
        Set<IdentitySchemaNode> identities = tested.getIdentities();
        IdentitySchemaNode testedIdentity = null;
        for (IdentitySchemaNode id : identities) {
            if (id.getQName().getLocalName().equals("crypto-alg")) {
                testedIdentity = id;
                IdentitySchemaNode baseIdentity = id.getBaseIdentity();
                assertEquals("crypto-base", baseIdentity.getQName()
                        .getLocalName());
                assertNull(baseIdentity.getBaseIdentity());
            }
        }
        assertNotNull(testedIdentity);
    }

    @Test
    public void testBitsType1() {
        Module tested = findModule(testedModules, "custom-types-test");
        LeafSchemaNode leaf = (LeafSchemaNode) tested
                .getDataChildByName("mybits");
        BitsType leafType = (BitsType) leaf.getType();
        List<Bit> bits = leafType.getBits();
        assertEquals(3, bits.size());

        Bit bit1 = bits.get(0);
        assertEquals("disable-nagle", bit1.getName());
        assertEquals(0L, (long) bit1.getPosition());

        Bit bit2 = bits.get(1);
        assertEquals("auto-sense-speed", bit2.getName());
        assertEquals(1L, (long) bit2.getPosition());

        Bit bit3 = bits.get(2);
        assertEquals("10-Mb-only", bit3.getName());
        assertEquals(2L, (long) bit3.getPosition());
    }

    @Test
    public void testBitsType2() {
        Module tested = findModule(testedModules, "custom-types-test");
        Set<TypeDefinition<?>> typedefs = tested.getTypeDefinitions();
        TypeDefinition<?> testedType = findTypedef(typedefs,
                "access-operations-type");

        BitsType bitsType = (BitsType) testedType.getBaseType();
        List<Bit> bits = bitsType.getBits();
        assertEquals(5, bits.size());

        Bit bit0 = bits.get(0);
        assertEquals(0L, (long) bit0.getPosition());

        Bit bit1 = bits.get(1);
        assertEquals(500L, (long) bit1.getPosition());

        Bit bit2 = bits.get(2);
        assertEquals(501L, (long) bit2.getPosition());

        Bit bit3 = bits.get(3);
        assertEquals(365L, (long) bit3.getPosition());

        Bit bit4 = bits.get(4);
        assertEquals(502L, (long) bit4.getPosition());
    }

    private Module findModule(Set<Module> modules, String name) {
        Module result = null;
        for (Module module : modules) {
            if (module.getName().equals(name)) {
                result = module;
                break;
            }
        }
        return result;
    }

    private TypeDefinition<?> findTypedef(Set<TypeDefinition<?>> typedefs,
            String name) {
        TypeDefinition<?> result = null;
        for (TypeDefinition<?> td : typedefs) {
            if (td.getQName().getLocalName().equals(name)) {
                result = td;
                break;
            }
        }
        return result;
    }

}
