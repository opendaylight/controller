/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker.util.operations;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.ModifyAction;
import org.opendaylight.yangtools.yang.data.impl.codec.xml.XmlDocumentUtils;
import org.opendaylight.yangtools.yang.model.api.*;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.activation.UnsupportedDataTypeException;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.List;

@RunWith(Parameterized.class)
public class YangDataOperationsTest {

    public static final String CURRENT_XML_NAME = "/current.xml";
    public static final String MODIFICATION_XML_NAME = "/merge.xml";
    private static final String XML_FOLDER_NAME = "/xmls";
    public static final String RESULT_XML_NAME = "/result.xml";
    private static final Object OPERATION_XML_NAME = "/defaultOperation.txt";

    protected final DataSchemaNode containerNode;
    protected final String testDirName;
    protected final Optional<CompositeNode> currentConfig;
    protected final Optional<CompositeNode> modification;
    protected final ModifyAction modifyAction;

    @Parameterized.Parameters()
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            // Container
            { "/containerTest_createContainer" },
            { "/containerTest_deleteContainer" },
            { "/containerTest_innerContainerContainer" },
            { "/containerTest_innerLeavesBaseOperationsContainer" },
            { "/containerTest_noneContainer" },
            { "/containerTest_removeContainer"},
            { "/containerTest_replaceContainer"},
            { "/containerTest_choiceActualModificationSameCase"},
            { "/containerTest_choiceActualModificationDifferentCases"},
            { "/containerTest_choiceActualOneCaseModificationOtherCase"},
            // LeafList
            { "/leafListTest" },
            // List
            { "/listTest" },
            // Additional
            {"/none_NoChange"},
            {"/listTest_alterInnerValue"}
        });
    }

    public YangDataOperationsTest(String testDir) throws Exception {
        SchemaContext schema = parseTestSchema();
        containerNode = getSchemaNode(schema, "test", "container");
        this.testDirName = testDir;

        currentConfig = loadXmlToCompositeNode(getXmlFolderName() + testDirName + CURRENT_XML_NAME);
        modification = loadXmlToCompositeNode(getXmlFolderName() + testDirName + MODIFICATION_XML_NAME);
        Preconditions.checkState(modification.isPresent(), "Modification xml has to be present under "
                + getXmlFolderName() + testDirName + MODIFICATION_XML_NAME);

        modifyAction = loadModifyAction(getXmlFolderName() + testDirName + OPERATION_XML_NAME);
    }

    protected String getXmlFolderName() {
        return XML_FOLDER_NAME;
    }


    @Test
    public void testModification() throws Exception {


        Optional<CompositeNode> result = DataOperations.modify((ContainerSchemaNode) containerNode,
                currentConfig.orNull(), modification.orNull(), modifyAction);

        String expectedResultXmlPath = getXmlFolderName() + testDirName + RESULT_XML_NAME;
        Optional<CompositeNode> expectedResult = loadXmlToCompositeNode(expectedResultXmlPath);

        if (result.isPresent()) {
            verifyModificationResult(result, expectedResult);
        } else {
            junit.framework.Assert.assertNull("Result of modification is empty node, result xml should not be present "
                    + expectedResultXmlPath, getClass().getResourceAsStream(expectedResultXmlPath));
        }

    }

    private ModifyAction loadModifyAction(String path) throws Exception {
        URL resource = getClass().getResource(path);
        if (resource == null) {
            return ModifyAction.MERGE;
        }

        return ModifyAction.fromXmlValue(Files.toString(new File(resource.getFile()), Charsets.UTF_8).trim());
    }

    private void verifyModificationResult(Optional<CompositeNode> result, Optional<CompositeNode> expectedResult)
            throws UnsupportedDataTypeException {
        Assert.assertEquals(
                String.format(
                        "Test result %n %s %n Expected %n %s %n",
                        XmlUtil.toString(XmlDocumentUtils.toDocument(result.get(),
                                XmlDocumentUtils.defaultValueCodecProvider())),
                        XmlUtil.toString(XmlDocumentUtils.toDocument(expectedResult.get(),
                                XmlDocumentUtils.defaultValueCodecProvider()))), expectedResult.get(), result.get());
    }

    private Optional<CompositeNode> loadXmlToCompositeNode(String xmlPath) throws IOException, SAXException {
        InputStream resourceAsStream = getClass().getResourceAsStream(xmlPath);
        if (resourceAsStream == null) {
            return Optional.absent();
        }

        Document currentConfigElement = XmlUtil.readXmlToDocument(resourceAsStream);
        Preconditions.checkNotNull(currentConfigElement);

        // FIXME XmlDocumentUtils.fromElement not implemented
        return Optional.of((CompositeNode) XmlDocumentUtils.toDomNode(currentConfigElement.getDocumentElement(),
                Optional.of(containerNode), Optional.of(XmlDocumentUtils.defaultValueCodecProvider())));
    }

    SchemaContext parseTestSchema() {
        YangParserImpl yangParserImpl = new YangParserImpl();
        Set<Module> modules = yangParserImpl.parseYangModelsFromStreams(getTestYangs());
        return yangParserImpl.resolveSchemaContext(modules);
    }

    List<InputStream> getTestYangs() {

        return Lists.newArrayList(Collections2.transform(Lists.newArrayList("/schemas/test.yang"),
                new Function<String, InputStream>() {
                    @Nullable
                    @Override
                    public InputStream apply(String input) {
                        InputStream resourceAsStream = getClass().getResourceAsStream(input);
                        Preconditions.checkNotNull(resourceAsStream, "File %s was null", resourceAsStream);
                        return resourceAsStream;
                    }
                }));
    }

    DataSchemaNode getSchemaNode(SchemaContext context, String moduleName, String childNodeName) {
        for (Module module : context.getModules()) {
            if (module.getName().equals(moduleName)) {
                for (DataSchemaNode dataSchemaNode : module.getChildNodes()) {
                    if (dataSchemaNode.getQName().getLocalName().equals(childNodeName))
                        return dataSchemaNode;
                }
            }
        }

        throw new IllegalStateException("Unable to find child node " + childNodeName);
    }

}
