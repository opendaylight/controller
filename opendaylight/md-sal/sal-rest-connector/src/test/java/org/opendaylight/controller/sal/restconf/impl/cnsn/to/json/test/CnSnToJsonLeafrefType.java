/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.cnsn.to.json.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.sal.restconf.impl.test.YangAndXmlAndDataSchemaLoader;

/**
 *
 * All tests are commented now because leafref isn't supported now
 *
 */

public class CnSnToJsonLeafrefType extends YangAndXmlAndDataSchemaLoader {

    @BeforeClass
    public static void initialization() {
        dataLoad("/cnsn-to-json/leafref", 2, "main-module", "cont");
    }

    @Test
    public void leafrefAbsolutePathToExistingLeafTest() {
//        final String json = toJson("/cnsn-to-json/leafref/xml/data_absolut_ref_to_existing_leaf.xml");
//        validateJson(".*\"lf3\":\\p{Blank}*\"true\".*", json);
    }

    @Test
    public void leafrefRelativePathToExistingLeafTest() {
//        final String json = toJson("/cnsn-to-json/leafref/xml/data_relativ_ref_to_existing_leaf.xml");
//        validateJson(".*\"lf2\":\\p{Blank}*\"121\".*", json);
    }

    /**
     * Tests case when reference to not existing element is present. In this case value from single node is printed as
     * string.
     */
    @Test
    public void leafrefToNonExistingLeafTest() {
//        final String json = toJson("/cnsn-to-json/leafref/xml/data_ref_to_non_existing_leaf.xml");
//        validateJson(".*\"lf5\":\\p{Blank}*\"137\".*", json);
    }

    /**
     * Tests case when non leaf element is referenced. In this case value from single node is printed as string.
     */
    @Test
    public void leafrefToNotLeafTest() {
//        final String json = toJson("/cnsn-to-json/leafref/xml/data_ref_to_not_leaf.xml");
//        validateJson(".*\"cont-augment-module\\p{Blank}*:\\p{Blank}*lf6\":\\p{Blank}*\"44\".*", json);
    }

    /**
     * Tests case when leaflist element is refers to leaf.
     */
    @Test
    public void leafrefFromLeafListToLeafTest() {
//        final String json = toJson("/cnsn-to-json/leafref/xml/data_relativ_ref_from_leaflist_to_existing_leaf.xml");
//        validateJson(
//                ".*\"cont-augment-module\\p{Blank}*:\\p{Blank}*lflst1\":\\p{Blank}*.*\"345\",\\p{Space}*\"346\",\\p{Space}*\"347\".*",
//                json);
    }

    /**
     * Tests case when leaflist element is refers to leaf.
     */
    @Test
    public void leafrefFromLeafrefToLeafrefTest() {
//        final String json = toJson("/cnsn-to-json/leafref/xml/data_from_leafref_to_leafref.xml");
//        validateJson(".*\"cont-augment-module\\p{Blank}*:\\p{Blank}*lf7\":\\p{Blank}*\"200\".*", json);
    }

    private void validateJson(final String regex, final String value) {
        assertNotNull(value);
        final Pattern ptrn = Pattern.compile(regex, Pattern.DOTALL);
        final Matcher mtch = ptrn.matcher(value);
        assertTrue(mtch.matches());
    }

//    private String toJson(String xmlDataPath) {
//        try {
//            Node<?> node = TestUtils.readInputToCnSn(xmlDataPath, XmlToCompositeNodeProvider.INSTANCE);
//            TestUtils.normalizeCompositeNode(node, modules, searchedModuleName + ":" + searchedDataSchemaName);
//            return TestUtils.writeCompNodeWithSchemaContextToOutput(node, modules, dataSchemaNode,
//                    StructuredDataToJsonProvider.INSTANCE);
//        } catch (WebApplicationException | IOException e) {
//        }
//        return "";
//    }

}
