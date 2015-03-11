/**
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.rest.common;

import com.google.common.base.Preconditions;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.opendaylight.controller.sal.rest.impl.test.providers.TestJsonBodyWriter;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.InstanceIdentifierContext;
import org.opendaylight.controller.sal.restconf.impl.NormalizedNodeContext;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.codec.xml.XmlUtils;
import org.opendaylight.yangtools.yang.data.impl.schema.transform.dom.parser.DomToNormalizedNodeParserFactory;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.opendaylight.yangtools.yang.model.parser.api.YangContextParser;
import org.opendaylight.yangtools.yang.model.parser.api.YangSyntaxErrorException;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * sal-rest-connector
 * org.opendaylight.controller.md.sal.rest.common
 *
 *
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 * Created: Mar 7, 2015
 */
public class TestRestconfUtils {

    private static final Logger LOG = LoggerFactory.getLogger(TestRestconfUtils.class);

    private static final YangContextParser parser = new YangParserImpl();

    private static final DocumentBuilderFactory BUILDERFACTORY;

    static {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
        } catch (final ParserConfigurationException e) {
            throw new ExceptionInInitializerError(e);
        }
        factory.setNamespaceAware(true);
        factory.setCoalescing(true);
        factory.setIgnoringElementContentWhitespace(true);
        factory.setIgnoringComments(true);
        BUILDERFACTORY = factory;
    }

    private TestRestconfUtils () {
        throw new UnsupportedOperationException("Test utility class");
    }

    public static SchemaContext loadSchemaContext(final String yangPath, final SchemaContext schemaContext) {
        try {
            Preconditions.checkArgument(yangPath != null, "Path can not be null.");
            Preconditions.checkArgument(( ! yangPath.isEmpty()), "Path can not be empty.");
            if (schemaContext == null) {
                return loadSchemaContext(yangPath);
            } else {
                return addSchemaContext(yangPath, schemaContext);
            }
        }
        catch (final Exception e) {
            LOG.error("Yang files at path: " + yangPath + " weren't loaded.");
        }
        return schemaContext;
    }

    public static NormalizedNodeContext loadNormalizedContextFromJsonFile() {
        throw new AbstractMethodError("Not implemented yet");
    }

    public static NormalizedNodeContext loadNormalizedContextFromXmlFile(final String pathToInputFile, final String uri) {
        final InstanceIdentifierContext<?> iiContext = ControllerContext.getInstance().toInstanceIdentifier(uri);
        final InputStream inputStream = TestJsonBodyWriter.class.getResourceAsStream(pathToInputFile);
        try {
            final DocumentBuilder dBuilder = BUILDERFACTORY.newDocumentBuilder();
            final Document doc = dBuilder.parse(inputStream);
            final NormalizedNode<?, ?> nn = parse(iiContext, doc);
            return new NormalizedNodeContext(iiContext, nn);
        }
        catch (final Exception e) {
            LOG.error("Load xml file " + pathToInputFile + " fail.", e);
        }
        return null;
    }

    private static NormalizedNode<?, ?> parse(final InstanceIdentifierContext<?> iiContext, final Document doc) {
        final List<Element> elements = Collections.singletonList(doc.getDocumentElement());
        final SchemaNode schemaNodeContext = iiContext.getSchemaNode();
        DataSchemaNode schemaNode = null;
        if (schemaNodeContext instanceof RpcDefinition) {
            if ("input".equalsIgnoreCase(doc.getDocumentElement().getLocalName())) {
                schemaNode = ((RpcDefinition) schemaNodeContext).getInput();
            } else if ("output".equalsIgnoreCase(doc.getDocumentElement().getLocalName())) {
                schemaNode = ((RpcDefinition) schemaNodeContext).getOutput();
            } else {
                throw new IllegalStateException("Unknown Rpc input node");
            }

        } else if (schemaNodeContext instanceof DataSchemaNode) {
            schemaNode = (DataSchemaNode) schemaNodeContext;
        } else {
            throw new IllegalStateException("Unknow SchemaNode");
        }

        final String docRootElm = doc.getDocumentElement().getLocalName();
        final String schemaNodeName = iiContext.getSchemaNode().getQName().getLocalName();

        if (!schemaNodeName.equalsIgnoreCase(docRootElm)) {
            final Collection<DataSchemaNode> children = ((DataNodeContainer) schemaNode).getChildNodes();
            for (final DataSchemaNode child : children) {
                if (child.getQName().getLocalName().equalsIgnoreCase(docRootElm)) {
                    schemaNode = child;
                    break;
                }
            }
        }
        final DomToNormalizedNodeParserFactory parserFactory =
                DomToNormalizedNodeParserFactory.getInstance(XmlUtils.DEFAULT_XML_CODEC_PROVIDER, iiContext.getSchemaContext());

        if(schemaNode instanceof ContainerSchemaNode) {
            return parserFactory.getContainerNodeParser().parse(Collections.singletonList(doc.getDocumentElement()), (ContainerSchemaNode) schemaNode);
        } else if(schemaNode instanceof ListSchemaNode) {
            final ListSchemaNode casted = (ListSchemaNode) schemaNode;
            return parserFactory.getMapEntryNodeParser().parse(elements, casted);
        } // FIXME : add another DataSchemaNode extensions e.g. LeafSchemaNode
        return null;
    }

    private static Collection<File> loadFiles(final String resourceDirectory) throws FileNotFoundException {
        final String path = TestRestconfUtils.class.getResource(resourceDirectory).getPath();
        final File testDir = new File(path);
        final String[] fileList = testDir.list();
        final List<File> testFiles = new ArrayList<File>();
        if (fileList == null) {
            throw new FileNotFoundException(resourceDirectory);
        }
        for (int i = 0; i < fileList.length; i++) {
            final String fileName = fileList[i];
            if (new File(testDir, fileName).isDirectory() == false) {
                testFiles.add(new File(testDir, fileName));
            }
        }
        return testFiles;
    }

    private static SchemaContext loadSchemaContext(final String resourceDirectory) throws IOException {
        final Collection<File> testFiles = loadFiles(resourceDirectory);
        return parser.parseFiles(testFiles);
    }

    private static SchemaContext addSchemaContext(final String resourceDirectory,
            final SchemaContext schemaContext) throws IOException, YangSyntaxErrorException {
        final Collection<File> testFiles = loadFiles(resourceDirectory);
        return parser.parseFiles(testFiles, schemaContext);
    }
}
