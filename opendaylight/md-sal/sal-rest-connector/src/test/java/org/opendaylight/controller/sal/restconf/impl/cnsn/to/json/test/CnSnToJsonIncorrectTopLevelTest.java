/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.cnsn.to.json.test;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.activation.UnsupportedDataTypeException;
import javax.ws.rs.WebApplicationException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.sal.rest.impl.StructuredDataToJsonProvider;
import org.opendaylight.controller.sal.rest.impl.XmlToCompositeNodeProvider;
import org.opendaylight.controller.sal.restconf.impl.test.TestUtils;
import org.opendaylight.controller.sal.restconf.impl.test.YangAndXmlAndDataSchemaLoader;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.model.api.ConstraintDefinition;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.GroupingDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.model.api.Status;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.UnknownSchemaNode;
import org.opendaylight.yangtools.yang.model.api.UsesNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CnSnToJsonIncorrectTopLevelTest extends YangAndXmlAndDataSchemaLoader {

    private static final Logger LOG = LoggerFactory.getLogger(CnSnToJsonIncorrectTopLevelTest.class);

    @BeforeClass
    public static void initialize() {
        dataLoad("/cnsn-to-json/simple-data-types");
    }

    private class IncorrectDataSchema implements DataSchemaNode, DataNodeContainer {

        @Override
        public String getDescription() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public SchemaPath getPath() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public QName getQName() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getReference() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Status getStatus() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public List<UnknownSchemaNode> getUnknownSchemaNodes() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Set<DataSchemaNode> getChildNodes() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public DataSchemaNode getDataChildByName(QName arg0) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public DataSchemaNode getDataChildByName(String arg0) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Set<GroupingDefinition> getGroupings() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Set<TypeDefinition<?>> getTypeDefinitions() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Set<UsesNode> getUses() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public ConstraintDefinition getConstraints() {
            // TODO Auto-generated method stub
            return null;
        }

        public boolean isAddedByUses() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isAugmenting() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isConfiguration() {
            // TODO Auto-generated method stub
            return false;
        }

    }

    @Test
    public void incorrectTopLevelElementTest() {

        CompositeNode compositeNode = TestUtils.readInputToCnSn("/cnsn-to-json/simple-data-types/xml/data.xml", XmlToCompositeNodeProvider.INSTANCE);
        DataSchemaNode incorrectDataSchema = null;
        incorrectDataSchema = new IncorrectDataSchema();

        TestUtils.normalizeCompositeNode(compositeNode, modules, "simple-data-types:cont");

        boolean exceptionRaised = false;
        try {
            TestUtils.writeCompNodeWithSchemaContextToOutput(compositeNode, modules, incorrectDataSchema,
                    StructuredDataToJsonProvider.INSTANCE);
        } catch (UnsupportedDataTypeException e) {
            exceptionRaised = true;
        } catch (WebApplicationException | IOException e) {
            LOG.error("WebApplicationException or IOException was raised");
        }

        assertTrue(exceptionRaised);
    }

}
