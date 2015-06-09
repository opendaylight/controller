/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.xml.to.nn.test;

import static org.junit.Assert.assertTrue;
import com.google.common.base.Preconditions;
import java.io.InputStream;
import javax.ws.rs.core.MediaType;
import org.junit.Test;
import org.opendaylight.controller.sal.rest.impl.XmlNormalizedNodeBodyReader;
import org.opendaylight.controller.sal.rest.impl.test.providers.AbstractBodyReaderTest;
import org.opendaylight.controller.sal.rest.impl.test.providers.TestXmlBodyReader;
import org.opendaylight.controller.sal.restconf.impl.NormalizedNodeContext;
import org.opendaylight.controller.sal.restconf.impl.RestconfDocumentedException;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class XmlLeafrefToNnTest extends AbstractBodyReaderTest {

    private static SchemaContext schemaContext;
    private final XmlNormalizedNodeBodyReader xmlBodyReader;

    public XmlLeafrefToNnTest() throws NoSuchFieldException, SecurityException {
        super();
        xmlBodyReader = new XmlNormalizedNodeBodyReader();
    }

    public static void initialize(SchemaContext schemaContext, final String path) {
        schemaContext = schemaContextLoader(path, schemaContext);
        controllerContext.setSchemas(schemaContext);
    }

    @Test
    public void testXmlDataContainer() throws Exception {

        initialize(schemaContext, "/xml-to-nn/data-container-yang");
        final String nn = toNn("data-container-yang:cont", "/xml-to-nn/data-container.xml");

        assertTrue(nn.contains("cont1"));
        assertTrue(nn.contains("lf11 100"));
        assertTrue(nn.contains("lflst1"));
        assertTrue(nn.contains("lflst1 121"));
        assertTrue(nn.contains("lflst1 str1"));
        assertTrue(nn.contains("lflst1 131"));
        assertTrue(nn.contains("lf3 null"));
        assertTrue(nn.contains("lf1 str0"));
        assertTrue(nn.contains("lst1"));
        assertTrue(nn.contains("lf11, value=str2"));
        assertTrue(nn.contains("lf2"));
    }

    @Test
    public void testXmlDataList() throws Exception {

        initialize(schemaContext, "/xml-to-nn/data-list-yang");

        final String nn = toNn("data-container-yang:cont", "/xml-to-nn/data-list.xml");

        assertTrue(nn.contains("lst1"));
        assertTrue(nn.contains("lflst11"));
        assertTrue(nn.contains("lflst11[131], value=131"));
        assertTrue(nn.contains("lflst11[121], value=121"));
        assertTrue(nn.contains("lflst11[str1], value=str1"));
        assertTrue(nn.contains("lf11, value=str0"));
        assertTrue(nn.contains("cont11"));
        assertTrue(nn.contains("lst11"));
        assertTrue(nn.contains("lf111, value=str2"));
        assertTrue(nn.contains("lflst11[221], value=221"));
        assertTrue(nn.contains("lf111, value=100"));
        assertTrue(nn.contains("lf1 lf1"));
    }

    @Test
    public void testXmlEmptyData() throws Exception{

        initialize(schemaContext, "/xml-to-nn/data-container-yang");

        final String nn = toNn("data-container-yang:cont", "/xml-to-nn/empty-data.xml");
        System.out.println(nn);
        assertTrue(nn.contains("lflst1"));
        assertTrue(nn.contains("lf1"));
        assertTrue(nn.contains("lst1"));
        assertTrue(nn.contains("lf11, value="));
    }

    @Test
    public void testIdentityrefNmspcInElement() throws Exception {
        initialize(schemaContext, "/xml-to-nn/identityref");
        final String nn = toNn("identityref-module:cont", "/xml-to-nn/identityref/xml/data-nmspc-in-element.xml");

        assertTrue(nn.contains("cont"));
        assertTrue(nn.contains("cont1"));
        assertTrue(nn.contains("lf11 (identity:module?revision=2013-12-02)iden"));
    }

    @Test
    public void testIdentityrefDefaultNmspcInElement() throws Exception {
        initialize(schemaContext, "/xml-to-nn/identityref/yang-augments");
        final String nn = toNn("general-module:cont", "/xml-to-nn/identityref/xml/data-default-nmspc-in-element.xml");

        assertTrue(nn.contains("cont"));
        assertTrue(nn.contains("cont1"));
        assertTrue(nn.contains("lf11 (identityref:module?revision=2013-12-02)iden"));
    }

    @Test
    public void testIdentityrefDefaultNmspcInParrentElement() throws Exception {
        initialize(schemaContext, "/xml-to-nn/identityref");
        final String nn = toNn("identityref-module:cont",
                "/xml-to-nn/identityref/xml/data-default-nmspc-in-parrent-element.xml");

        assertTrue(nn.contains("cont"));
        assertTrue(nn.contains("cont1"));
        assertTrue(nn.contains("lf11 (identityref:module?revision=2013-12-02)iden"));
    }

    @Test
    public void testIdentityrefNmspcInParrentElement() throws Exception {
        initialize(schemaContext, "/xml-to-nn/identityref");
        final String nn = toNn("identityref-module:cont",
                "/xml-to-nn/identityref/xml/data-nmspc-in-parrent-element.xml");
        System.out.println(nn);
        assertTrue(nn.contains("cont"));
        assertTrue(nn.contains("cont1"));
        assertTrue(nn.contains("lf11 (identityref:module?revision=2013-12-02)iden"));
    }

    @Test(expected = RestconfDocumentedException.class)
    public void testIdentityrefNoNmspcValueWithPrefix() throws Exception {
        initialize(schemaContext, "/xml-to-nn/identityref");
        final String nn = toNn("identityref-module:cont",
                "/xml-to-nn/identityref/xml/data-no-nmspc-value-with-prefix.xml");
        System.out.println(nn);
        assertTrue(nn.contains("cont"));
        assertTrue(nn.contains("cont1"));
        assertTrue(nn.contains("lf11 (identityref:module?revision=2013-12-02)iden"));
    }

    @Test(expected = RestconfDocumentedException.class)
    public void testIdentityrefNoNmspcValueWithoutPrefix() throws Exception {
        initialize(schemaContext, "/xml-to-nn/identityref");
        toNn("identityref-module:cont",
                "/xml-to-nn/identityref/xml/data-no-nmspc-value-without-prefix.xml");
    }

    private String toNn(final String uriVal, final String xmlPath) throws Exception {
        final String uri = uriVal;
        mockBodyReader(uri, xmlBodyReader, false);

        final InputStream inputStream = TestXmlBodyReader.class.getResourceAsStream(xmlPath);
        final NormalizedNodeContext normalizedNodeContext = xmlBodyReader.readFrom(null, null, null, mediaType, null,
                inputStream);

        Preconditions.checkNotNull(normalizedNodeContext);

        final String nn = NormalizedNodes.toStringTree(normalizedNodeContext.getData());
        return nn;
    }

    @Override
    protected MediaType getMediaType() {
        return null;
    }

}
