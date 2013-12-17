package org.opendaylight.controller.sal.restconf.impl.cnsn.to.xml.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import javax.ws.rs.WebApplicationException;

import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.controller.sal.rest.impl.StructuredDataToXmlProvider;
import org.opendaylight.controller.sal.restconf.impl.test.DummyType;
import org.opendaylight.controller.sal.restconf.impl.test.TestUtils;
import org.opendaylight.yangtools.yang.data.api.*;
import org.opendaylight.yangtools.yang.data.impl.NodeFactory;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.parser.builder.impl.ContainerSchemaNodeBuilder;
import org.opendaylight.yangtools.yang.parser.builder.impl.LeafSchemaNodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CnSnToXmlNotExistingLeafTypeTest {

    private static final Logger LOG = LoggerFactory.getLogger(CnSnToXmlNotExistingLeafTypeTest.class);

    @Ignore
    @Test
    public void incorrectTopLevelElementTest() {

        String xmlOutput = null;
        try {
            xmlOutput = TestUtils.writeCompNodeWithSchemaContextToOutput(prepareCompositeNode(),
                    (Set<Module>) Collections.EMPTY_SET, prepareDataSchemaNode(), StructuredDataToXmlProvider.INSTANCE);
        } catch (WebApplicationException | IOException e) {
            LOG.error("WebApplicationException or IOException was raised");
        }
        assertNotNull(xmlOutput);
        assertTrue(xmlOutput.contains("<lf1>any value</lf1>"));

    }

    private CompositeNode prepareCompositeNode() {
        MutableCompositeNode cont = NodeFactory.createMutableCompositeNode(
                TestUtils.buildQName("cont", "simple:uri", "2012-12-17"), null, null, ModifyAction.CREATE, null);
        MutableSimpleNode<?> lf1 = NodeFactory.createMutableSimpleNode(
                TestUtils.buildQName("lf1", "simple:uri", "2012-12-17"), cont, "any value", ModifyAction.CREATE, null);
        cont.getChildren().add(lf1);
        cont.init();
        return cont;
    }

    private DataSchemaNode prepareDataSchemaNode() {
        ContainerSchemaNodeBuilder contBuild = new ContainerSchemaNodeBuilder("module", 1, TestUtils.buildQName("cont",
                "simple:uri", "2012-12-17"), null);
        LeafSchemaNodeBuilder leafBuild = new LeafSchemaNodeBuilder("module", 2, TestUtils.buildQName("lf1",
                "simple:uri", "2012-12-17"), null);
        leafBuild.setType(new DummyType());
        leafBuild.setConfiguration(true);

        contBuild.addChildNode(leafBuild);
        return contBuild.build(null);

    }

}
