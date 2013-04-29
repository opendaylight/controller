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
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.IdentitySchemaNode;
import org.opendaylight.controller.yang.model.api.LeafSchemaNode;
import org.opendaylight.controller.yang.model.api.Module;
import org.opendaylight.controller.yang.model.api.Status;
import org.opendaylight.controller.yang.model.api.TypeDefinition;
import org.opendaylight.controller.yang.model.api.type.BitsTypeDefinition.Bit;
import org.opendaylight.controller.yang.model.api.type.EnumTypeDefinition.EnumPair;
import org.opendaylight.controller.yang.model.api.type.LengthConstraint;
import org.opendaylight.controller.yang.model.api.type.PatternConstraint;
import org.opendaylight.controller.yang.model.util.BitsType;
import org.opendaylight.controller.yang.model.util.EnumerationType;
import org.opendaylight.controller.yang.model.util.ExtendedType;
import org.opendaylight.controller.yang.model.util.IdentityrefType;
import org.opendaylight.controller.yang.model.util.InstanceIdentifier;
import org.opendaylight.controller.yang.model.util.StringType;
import org.opendaylight.controller.yang.model.util.UnionType;

public class TypesResolutionTest {
    private Set<Module> testedModules;

    @Before
    public void init() {
        testedModules = TestUtils.loadModules("src/test/resources/types");
    }

    @Test
    public void testIPVersion() {
        Module tested = TestUtils.findModule(testedModules, "ietf-inet-types");
        Set<TypeDefinition<?>> typedefs = tested.getTypeDefinitions();
        assertEquals(14, typedefs.size());

        TypeDefinition<?> type = TestUtils.findTypedef(typedefs, "ip-version");
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
    public void testEnumeration() {
        Module tested = TestUtils.findModule(testedModules, "custom-types-test");
        Set<TypeDefinition<?>> typedefs = tested.getTypeDefinitions();

        TypeDefinition<?> type = TestUtils.findTypedef(typedefs, "ip-version");
        EnumerationType enumType = (EnumerationType) type.getBaseType();
        List<EnumPair> values = enumType.getValues();
        assertEquals(4, values.size());

        EnumPair value0 = values.get(0);
        assertEquals("unknown", value0.getName());
        assertEquals(0, (int) value0.getValue());
        assertEquals(
                "An unknown or unspecified version of the Internet protocol.",
                value0.getDescription());

        EnumPair value1 = values.get(1);
        assertEquals("ipv4", value1.getName());
        assertEquals(19, (int) value1.getValue());
        assertEquals("The IPv4 protocol as defined in RFC 791.",
                value1.getDescription());

        EnumPair value2 = values.get(2);
        assertEquals("ipv6", value2.getName());
        assertEquals(7, (int) value2.getValue());
        assertEquals("The IPv6 protocol as defined in RFC 2460.",
                value2.getDescription());

        EnumPair value3 = values.get(3);
        assertEquals("default", value3.getName());
        assertEquals(20, (int) value3.getValue());
        assertEquals("default ip",
                value3.getDescription());
    }

    @Test
    public void testIpAddress() {
        Module tested = TestUtils.findModule(testedModules, "ietf-inet-types");
        Set<TypeDefinition<?>> typedefs = tested.getTypeDefinitions();
        TypeDefinition<?> type = TestUtils.findTypedef(typedefs, "ip-address");
        UnionType baseType = (UnionType) type.getBaseType();
        List<TypeDefinition<?>> unionTypes = baseType.getTypes();

        ExtendedType ipv4 = (ExtendedType) unionTypes.get(0);
        StringType ipv4Base = (StringType) ipv4.getBaseType();
        String expectedPattern = "(([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])\\.){3}"
                + "([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])"
                + "(%[\\p{N}\\p{L}]+)?";
        assertEquals(expectedPattern, ipv4Base.getPatterns().get(0)
                .getRegularExpression());

        ExtendedType ipv6 = (ExtendedType) unionTypes.get(1);
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
        Module tested = TestUtils.findModule(testedModules, "ietf-inet-types");
        Set<TypeDefinition<?>> typedefs = tested.getTypeDefinitions();
        TypeDefinition<?> type = TestUtils.findTypedef(typedefs, "domain-name");
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
        Module tested = TestUtils
                .findModule(testedModules, "custom-types-test");
        LeafSchemaNode leaf = (LeafSchemaNode) tested
                .getDataChildByName("inst-id-leaf1");
        InstanceIdentifier leafType = (InstanceIdentifier) leaf.getType();
        assertFalse(leafType.requireInstance());
    }

    @Test
    public void testInstanceIdentifier2() {
        Module tested = TestUtils
                .findModule(testedModules, "custom-types-test");
        LeafSchemaNode leaf = (LeafSchemaNode) tested
                .getDataChildByName("inst-id-leaf2");
        InstanceIdentifier leafType = (InstanceIdentifier) leaf.getType();
        assertTrue(leafType.requireInstance());
    }

    @Test
    public void testIdentity() {
        Module tested = TestUtils
                .findModule(testedModules, "custom-types-test");
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
        Module tested = TestUtils
                .findModule(testedModules, "custom-types-test");
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
        Module tested = TestUtils
                .findModule(testedModules, "custom-types-test");
        Set<TypeDefinition<?>> typedefs = tested.getTypeDefinitions();
        TypeDefinition<?> testedType = TestUtils.findTypedef(typedefs,
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


    @Test
    public void testIanaTimezones() {
        Module tested = TestUtils.findModule(testedModules, "iana-timezones");
        Set<TypeDefinition<?>> typedefs = tested.getTypeDefinitions();
        TypeDefinition<?> testedType = TestUtils.findTypedef(typedefs,
                "iana-timezone");
        /*
        // FIXME: Refactor sources not to be timezone specific.
        String expectedDesc = "A timezone location as defined by the IANA timezone\n       database (http://www.iana.org/time-zones)";
        assertEquals(expectedDesc, testedType.getDescription());
        assertNull(testedType.getReference());
        assertEquals(Status.CURRENT, testedType.getStatus());
        */
        QName testedTypeQName = testedType.getQName();
        assertEquals(URI.create("urn:ietf:params:xml:ns:yang:iana-timezones"),
                testedTypeQName.getNamespace());
        assertEquals(TestUtils.createDate("2012-07-09"),
                testedTypeQName.getRevision());
        assertEquals("ianatz", testedTypeQName.getPrefix());
        assertEquals("iana-timezone", testedTypeQName.getLocalName());

        EnumerationType enumType = (EnumerationType) testedType.getBaseType();
        List<EnumPair> values = enumType.getValues();
        assertEquals(415, values.size()); // 0-414

        EnumPair enum168 = values.get(168);
        assertEquals("America/Danmarkshavn", enum168.getName());
        assertEquals(168, (int) enum168.getValue());
        assertEquals("east coast, north of Scoresbysund",
                enum168.getDescription());

        EnumPair enum374 = values.get(374);
        assertEquals("America/Indiana/Winamac", enum374.getName());
        assertEquals(374, (int) enum374.getValue());
        assertEquals("Eastern Time - Indiana - Pulaski County",
                enum374.getDescription());
    }

    @Test
    public void testObjectId128() {
        Module tested = TestUtils.findModule(testedModules, "ietf-yang-types");
        Set<TypeDefinition<?>> typedefs = tested.getTypeDefinitions();
        ExtendedType testedType = (ExtendedType) TestUtils.findTypedef(
                typedefs, "object-identifier-128");

        QName testedTypeQName = testedType.getQName();
        assertEquals(URI.create("urn:ietf:params:xml:ns:yang:ietf-yang-types"),
                testedTypeQName.getNamespace());
        assertEquals(TestUtils.createDate("2010-09-24"),
                testedTypeQName.getRevision());
        assertEquals("yang", testedTypeQName.getPrefix());
        assertEquals("object-identifier-128", testedTypeQName.getLocalName());

        ExtendedType testedTypeBase = (ExtendedType) testedType.getBaseType();

        List<PatternConstraint> patterns = testedTypeBase.getPatterns();
        assertEquals(2, patterns.size());
        PatternConstraint pattern1 = patterns.get(0);
        assertEquals("\\d*(\\.\\d*){1,127}", pattern1.getRegularExpression());
        PatternConstraint pattern2 = patterns.get(1);
        assertEquals(
                "(([0-1](\\.[1-3]?[0-9]))|(2\\.(0|([1-9]\\d*))))(\\.(0|([1-9]\\d*)))*",
                pattern2.getRegularExpression());

        QName testedTypeBaseQName = testedTypeBase.getQName();
        assertEquals(URI.create("urn:ietf:params:xml:ns:yang:ietf-yang-types"),
                testedTypeBaseQName.getNamespace());
        assertEquals(TestUtils.createDate("2010-09-24"),
                testedTypeBaseQName.getRevision());
        assertEquals("yang", testedTypeBaseQName.getPrefix());
        assertEquals("object-identifier", testedTypeBaseQName.getLocalName());
    }

    @Test
    public void testIdentityref() {
        Module tested = TestUtils
                .findModule(testedModules, "custom-types-test");
        Set<TypeDefinition<?>> typedefs = tested.getTypeDefinitions();
        TypeDefinition<?> testedType = TestUtils.findTypedef(typedefs,
                "service-type-ref");
        IdentityrefType baseType = (IdentityrefType)testedType.getBaseType();
        QName identity = baseType.getIdentity();
        assertEquals(URI.create("urn:simple.container.demo"), identity.getNamespace());
        assertEquals(TestUtils.createDate("2012-04-16"), identity.getRevision());
        assertEquals("iit", identity.getPrefix());
        assertEquals("service-type", identity.getLocalName());
    }

}
