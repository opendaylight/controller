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
import org.opendaylight.controller.yang.model.api.type.EnumTypeDefinition.EnumPair;
import org.opendaylight.controller.yang.model.api.type.LengthConstraint;
import org.opendaylight.controller.yang.model.api.type.PatternConstraint;
import org.opendaylight.controller.yang.model.parser.api.YangModelParser;
import org.opendaylight.controller.yang.model.util.EnumerationType;
import org.opendaylight.controller.yang.model.util.InstanceIdentifier;
import org.opendaylight.controller.yang.model.util.StringType;
import org.opendaylight.controller.yang.model.util.UnionType;

public class TypesResolutionTest {

    private YangModelParser parser;
    private String[] testFiles;
    private Set<Module> modules;

    @Before
    public void init() {
        parser = new YangModelParserImpl();
        File testDir = new File("src/test/resources/types");
        String[] fileList = testDir.list();
        testFiles = new String[fileList.length];
        int i = 0;
        for(String fileName : fileList) {
            File file = new File(testDir, fileName);
            testFiles[i] = file.getAbsolutePath();
            i++;
        }
        modules = parser.parseYangModels(testFiles);
        assertEquals(fileList.length, modules.size());
    }

    @Test
    public void testIPVersion() {
        Module tested = findModule(modules, "ietf-inet-types");
        Set<TypeDefinition<?>> typedefs = tested.getTypeDefinitions();
        assertEquals(14, typedefs.size());

        TypeDefinition<?> type = findTypedef(typedefs, "ip-version");
        EnumerationType en = (EnumerationType)type.getBaseType();
        List<EnumPair> values = en.getValues();
        assertEquals(3, values.size());

        EnumPair value0 = values.get(0);
        assertEquals("unknown", value0.getName());
        assertEquals(0, (int)value0.getValue());

        EnumPair value1 = values.get(1);
        assertEquals("ipv4", value1.getName());
        assertEquals(1, (int)value1.getValue());

        EnumPair value2 = values.get(2);
        assertEquals("ipv6", value2.getName());
        assertEquals(2, (int)value2.getValue());
    }

    @Test
    public void testIpAddress() {
        Module tested = findModule(modules, "ietf-inet-types");
        Set<TypeDefinition<?>> typedefs = tested.getTypeDefinitions();
        TypeDefinition<?> type = findTypedef(typedefs, "ip-address");
        UnionType baseType = (UnionType)type.getBaseType();
        List<TypeDefinition<?>> unionTypes = baseType.getTypes();

        StringType ipv4 = (StringType)unionTypes.get(0);
        String expectedPattern =
        "(([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])\\.){3}"
      +  "([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])"
      + "(%[\\p{N}\\p{L}]+)?";
        assertEquals(expectedPattern, ipv4.getPatterns().get(0).getRegularExpression());

        StringType ipv6 = (StringType)unionTypes.get(1);
        List<PatternConstraint> ipv6Patterns = ipv6.getPatterns();
        expectedPattern = "((:|[0-9a-fA-F]{0,4}):)([0-9a-fA-F]{0,4}:){0,5}"
        + "((([0-9a-fA-F]{0,4}:)?(:|[0-9a-fA-F]{0,4}))|"
        + "(((25[0-5]|2[0-4][0-9]|[01]?[0-9]?[0-9])\\.){3}"
        + "(25[0-5]|2[0-4][0-9]|[01]?[0-9]?[0-9])))"
        + "(%[\\p{N}\\p{L}]+)?";
        assertEquals(expectedPattern, ipv6Patterns.get(0).getRegularExpression());

        expectedPattern = "(([^:]+:){6}(([^:]+:[^:]+)|(.*\\..*)))|"
        + "((([^:]+:)*[^:]+)?::(([^:]+:)*[^:]+)?)"
        + "(%.+)?";
        assertEquals(expectedPattern, ipv6Patterns.get(1).getRegularExpression());
    }

    @Test
    public void testDomainName() {
        Module tested = findModule(modules, "ietf-inet-types");
        Set<TypeDefinition<?>> typedefs = tested.getTypeDefinitions();
        TypeDefinition<?> type = findTypedef(typedefs, "domain-name");
        StringType baseType = (StringType)type.getBaseType();
        List<PatternConstraint> patterns = baseType.getPatterns();
        assertEquals(1, patterns.size());
        String expectedPattern = "((([a-zA-Z0-9_]([a-zA-Z0-9\\-_]){0,61})?[a-zA-Z0-9]\\.)*"
        +  "([a-zA-Z0-9_]([a-zA-Z0-9\\-_]){0,61})?[a-zA-Z0-9]\\.?)"
        +  "|\\.";
        assertEquals(expectedPattern, patterns.get(0).getRegularExpression());

        List<LengthConstraint> lengths = baseType.getLengthStatements();
        assertEquals(1, lengths.size());
        LengthConstraint length = baseType.getLengthStatements().get(0);
        assertEquals(1L, length.getMin().longValue());
        assertEquals(253L, length.getMax().longValue());
    }

    @Test
    public void testInstanceIdentifier1() {
        Module tested = findModule(modules, "custom-types-test");
        LeafSchemaNode leaf = (LeafSchemaNode)tested.getDataChildByName("inst-id-leaf1");
        InstanceIdentifier leafType = (InstanceIdentifier)leaf.getType();
        assertFalse(leafType.requireInstance());
    }

    @Test
    public void testInstanceIdentifier2() {
        Module tested = findModule(modules, "custom-types-test");
        LeafSchemaNode leaf = (LeafSchemaNode)tested.getDataChildByName("inst-id-leaf2");
        InstanceIdentifier leafType = (InstanceIdentifier)leaf.getType();
        assertTrue(leafType.requireInstance());
    }

    @Test
    public void testIdentity() {
        Module tested = findModule(modules, "custom-types-test");
        Set<IdentitySchemaNode> identities = tested.getIdentities();
        IdentitySchemaNode testedIdentity = null;
        for(IdentitySchemaNode id : identities) {
            if(id.getQName().getLocalName().equals("crypto-alg")) {
                testedIdentity = id;
                IdentitySchemaNode baseIdentity = id.getBaseIdentity();
                assertEquals("crypto-base", baseIdentity.getQName().getLocalName());
                assertNull(baseIdentity.getBaseIdentity());
            }
        }
        assertNotNull(testedIdentity);
    }

    private Module findModule(Set<Module> modules, String name) {
        for(Module module : modules) {
            if(module.getName().equals(name)) {
                return module;
            }
        }
        return null;
    }

    private TypeDefinition<?> findTypedef(Set<TypeDefinition<?>> typedefs, String name) {
        for(TypeDefinition<?> td : typedefs) {
            if(td.getQName().getLocalName().equals(name)) {
                return td;
            }
        }
        return null;
    }

}
