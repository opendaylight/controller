/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package org.opendaylight.controller.sal.connect.netconf.schema.mapping;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.GET_SCHEMA_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_CANDIDATE_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_COMMIT_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_DATA_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_DISCARD_CHANGES_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_EDIT_CONFIG_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_GET_CONFIG_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_GET_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_RUNNING_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.createEditConfigStructure;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.toFilterStructure;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.toId;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.toPath;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.xml.transform.dom.DOMSource;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.ElementNameAndAttributeQualifier;
import org.custommonkey.xmlunit.XMLUnit;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.controller.sal.connect.netconf.NetconfDevice;
import org.opendaylight.controller.sal.connect.netconf.schema.NetconfRemoteSchemaYangSourceProvider;
import org.opendaylight.controller.sal.connect.netconf.util.NetconfBaseOps;
import org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.base._1._0.rev110601.$YangModuleInfoImpl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.NetconfState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.Capabilities;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.Schemas;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.schemas.Schema;
import org.opendaylight.yangtools.sal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.ModifyAction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.AnyXmlNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class NetconfMessageTransformerTest {

    private NetconfMessageTransformer netconfMessageTransformer;
    private SchemaContext schema;

    @Before
    public void setUp() throws Exception {
        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreAttributeOrder(true);
        XMLUnit.setIgnoreComments(true);

        schema = getSchema();
        netconfMessageTransformer = getTransformer(schema);

    }

    @Test
    public void testDiscardChangesRequest() throws Exception {
        final NetconfMessage netconfMessage = netconfMessageTransformer.toRpcRequest(toPath(NETCONF_DISCARD_CHANGES_QNAME),
                NetconfMessageTransformUtil.DISCARD_CHANGES_RPC_CONTENT);
        assertThat(XmlUtil.toString(netconfMessage.getDocument()), CoreMatchers.containsString("<discard"));
    }

    @Test
    public void tesGetSchemaRequest() throws Exception {
        final NetconfMessage netconfMessage = netconfMessageTransformer.toRpcRequest(toPath(GET_SCHEMA_QNAME),
                NetconfRemoteSchemaYangSourceProvider.createGetSchemaRequest("module", Optional.of("2012-12-12")));
        assertSimilarXml(netconfMessage, "<rpc message-id=\"m-0\" xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n" +
                "<get-schema xmlns=\"urn:ietf:params:xml:ns:yang:ietf-netconf-monitoring\">\n" +
                "<format xmlns:x=\"urn:ietf:params:xml:ns:yang:ietf-netconf-monitoring\">x:yang</format>\n" +
                "<identifier>module</identifier>\n" +
                "<version>2012-12-12</version>\n" +
                "</get-schema>\n" +
                "</rpc>");
    }

    @Test
    public void tesGetSchemaResponse() throws Exception {
        final NetconfMessageTransformer netconfMessageTransformer = getTransformer(getSchema());
        final NetconfMessage response = new NetconfMessage(XmlUtil.readXmlToDocument(
                "<rpc-reply message-id=\"101\"\n" +
                        "xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n" +
                        "<data\n" +
                        "xmlns=\"urn:ietf:params:xml:ns:yang:ietf-netconf-monitoring\">\n" +
                        "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
                        "Random YANG SCHEMA\n" +
                        "</xs:schema>\n" +
                        "</data>\n" +
                        "</rpc-reply>"
        ));
        final DOMRpcResult compositeNodeRpcResult = netconfMessageTransformer.toRpcResult(response, toPath(GET_SCHEMA_QNAME));
        assertTrue(compositeNodeRpcResult.getErrors().isEmpty());
        assertNotNull(compositeNodeRpcResult.getResult());
        final DOMSource schemaContent = ((AnyXmlNode) ((ContainerNode) compositeNodeRpcResult.getResult()).getValue().iterator().next()).getValue();
        assertThat(((Element) schemaContent.getNode()).getTextContent(), CoreMatchers.containsString("Random YANG SCHEMA"));
    }

    @Test
    public void testGetConfigResponse() throws Exception {
        final NetconfMessage response = new NetconfMessage(XmlUtil.readXmlToDocument("<rpc-reply message-id=\"101\"\n" +
                "xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n" +
                "<data>\n" +
                "<netconf-state xmlns=\"urn:ietf:params:xml:ns:yang:ietf-netconf-monitoring\">\n" +
                "<schemas>\n" +
                "<schema>\n" +
                "<identifier>module</identifier>\n" +
                "<version>2012-12-12</version>\n" +
                "<format xmlns:x=\"urn:ietf:params:xml:ns:yang:ietf-netconf-monitoring\">x:yang</format>\n" +
                "</schema>\n" +
                "</schemas>\n" +
                "</netconf-state>\n" +
                "</data>\n" +
                "</rpc-reply>"));

        final NetconfMessageTransformer netconfMessageTransformer = getTransformer(getSchema());
        final DOMRpcResult compositeNodeRpcResult = netconfMessageTransformer.toRpcResult(response, toPath(NETCONF_GET_CONFIG_QNAME));
        assertTrue(compositeNodeRpcResult.getErrors().isEmpty());
        assertNotNull(compositeNodeRpcResult.getResult());

        final List<DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?>> values = Lists.newArrayList(
                NetconfRemoteSchemaYangSourceProvider.createGetSchemaRequest("module", Optional.of("2012-12-12")).getValue());

        final Map<QName, Object> keys = Maps.newHashMap();
        for (final DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?> value : values) {
            keys.put(value.getNodeType(), value.getValue());
        }

        final YangInstanceIdentifier.NodeIdentifierWithPredicates identifierWithPredicates = new YangInstanceIdentifier.NodeIdentifierWithPredicates(Schema.QNAME, keys);
        final MapEntryNode schemaNode = Builders.mapEntryBuilder().withNodeIdentifier(identifierWithPredicates).withValue(values).build();

        final ContainerNode data = (ContainerNode) ((ContainerNode) compositeNodeRpcResult.getResult()).getChild(toId(NETCONF_DATA_QNAME)).get();
        final ContainerNode state = (ContainerNode) data.getChild(toId(NetconfState.QNAME)).get();
        final ContainerNode schemas = (ContainerNode) state.getChild(toId(Schemas.QNAME)).get();
        final MapNode schemaParent = (MapNode) schemas.getChild(toId(Schema.QNAME)).get();
        assertEquals(1, Iterables.size(schemaParent.getValue()));

        assertEquals(schemaNode, schemaParent.getValue().iterator().next());
    }

    @Test
    public void testGetConfigRequest() throws Exception {
        final DataContainerChild<?, ?> filter = toFilterStructure(
                YangInstanceIdentifier.create(toId(NetconfState.QNAME), toId(Schemas.QNAME)), schema);

        final DataContainerChild<?, ?> source = NetconfBaseOps.getSourceNode(NETCONF_RUNNING_QNAME);

        final NetconfMessage netconfMessage = netconfMessageTransformer.toRpcRequest(toPath(NETCONF_GET_CONFIG_QNAME),
                NetconfMessageTransformUtil.wrap(NETCONF_GET_CONFIG_QNAME, source, filter));

        assertSimilarXml(netconfMessage, "<rpc message-id=\"m-0\" xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n" +
                "<get-config xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n" +
                "<filter xmlns:ns0=\"urn:ietf:params:xml:ns:netconf:base:1.0\" ns0:type=\"subtree\">\n" +
                "<netconf-state xmlns=\"urn:ietf:params:xml:ns:yang:ietf-netconf-monitoring\">\n" +
                "<schemas/>\n" +
                "</netconf-state>" +
                "</filter>\n" +
                "<source>\n" +
                "<running/>\n" +
                "</source>\n" +
                "</get-config>" +
                "</rpc>");
    }

    @Test
    public void testEditConfigRequest() throws Exception {
        final List<DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?>> values = Lists.newArrayList(
            NetconfRemoteSchemaYangSourceProvider.createGetSchemaRequest("module", Optional.of("2012-12-12")).getValue());

        final Map<QName, Object> keys = Maps.newHashMap();
        for (final DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?> value : values) {
            keys.put(value.getNodeType(), value.getValue());
        }

        final YangInstanceIdentifier.NodeIdentifierWithPredicates identifierWithPredicates = new YangInstanceIdentifier.NodeIdentifierWithPredicates(Schema.QNAME, keys);
        final MapEntryNode schemaNode = Builders.mapEntryBuilder().withNodeIdentifier(identifierWithPredicates).withValue(values).build();

        final YangInstanceIdentifier id = YangInstanceIdentifier.builder().node(NetconfState.QNAME).node(Schemas.QNAME).node(Schema.QNAME).nodeWithKey(Schema.QNAME, keys).build();
        final DataContainerChild<?, ?> editConfigStructure = createEditConfigStructure(NetconfDevice.INIT_SCHEMA_CTX, id, Optional.of(ModifyAction.REPLACE), Optional.<NormalizedNode<?, ?>>fromNullable(schemaNode));

        final DataContainerChild<?, ?> target = NetconfBaseOps.getTargetNode(NETCONF_CANDIDATE_QNAME);

        final ContainerNode wrap = NetconfMessageTransformUtil.wrap(NETCONF_EDIT_CONFIG_QNAME, editConfigStructure, target);
        final NetconfMessage netconfMessage = netconfMessageTransformer.toRpcRequest(toPath(NETCONF_EDIT_CONFIG_QNAME), wrap);

        assertSimilarXml(netconfMessage, "<rpc message-id=\"m-0\" xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n" +
                "<edit-config>\n" +
                "<target>\n" +
                "<candidate/>\n" +
                "</target>\n" +
                "<config>\n" +
                "<netconf-state xmlns=\"urn:ietf:params:xml:ns:yang:ietf-netconf-monitoring\">\n" +
                "<schemas>\n" +
                "<schema>\n" +
                "<identifier>module</identifier>\n" +
                "<version>2012-12-12</version>\n" +
                "<format xmlns:x=\"urn:ietf:params:xml:ns:yang:ietf-netconf-monitoring\">x:yang</format>\n" +
                "</schema>\n" +
                "</schemas>\n" +
                "</netconf-state>\n" +
                "</config>\n" +
                "</edit-config>\n" +
                "</rpc>");
    }

    private void assertSimilarXml(final NetconfMessage netconfMessage, final String xmlContent) throws SAXException, IOException {
        final Diff diff = XMLUnit.compareXML(netconfMessage.getDocument(), XmlUtil.readXmlToDocument(xmlContent));
        diff.overrideElementQualifier(new ElementNameAndAttributeQualifier());
        assertTrue(diff.toString(), diff.similar());
    }

    @Test
    public void testGetRequest() throws Exception {

        final QName capability = QName.create(Capabilities.QNAME, "capability");
        final DataContainerChild<?, ?> filter = toFilterStructure(
                YangInstanceIdentifier.create(toId(NetconfState.QNAME), toId(Capabilities.QNAME), toId(capability), new YangInstanceIdentifier.NodeWithValue(capability, "a:b:c")), schema);

        final NetconfMessage netconfMessage = netconfMessageTransformer.toRpcRequest(toPath(NETCONF_GET_QNAME),
                NetconfMessageTransformUtil.wrap(NETCONF_GET_QNAME, filter));

        assertSimilarXml(netconfMessage, "<rpc message-id=\"m-0\" xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">" +
                "<get xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n" +
                "<filter xmlns:ns0=\"urn:ietf:params:xml:ns:netconf:base:1.0\" ns0:type=\"subtree\">\n" +
                "<netconf-state xmlns=\"urn:ietf:params:xml:ns:yang:ietf-netconf-monitoring\">\n" +
                "<capabilities>\n" +
                "<capability>a:b:c</capability>\n" +
                "</capabilities>\n" +
                "</netconf-state>" +
                "</filter>\n" +
                "</get>" +
                "</rpc>");
    }

    private NetconfMessageTransformer getTransformer(final SchemaContext schema) {
        return new NetconfMessageTransformer(schema);
    }

    @Test
    public void testCommitResponse() throws Exception {
        final NetconfMessage response = new NetconfMessage(XmlUtil.readXmlToDocument(
                "<rpc-reply xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\"><ok/></rpc-reply>"
        ));
        final DOMRpcResult compositeNodeRpcResult = netconfMessageTransformer.toRpcResult(response, toPath(NETCONF_COMMIT_QNAME));
        assertTrue(compositeNodeRpcResult.getErrors().isEmpty());
        assertNull(compositeNodeRpcResult.getResult());
    }

    public SchemaContext getSchema() {
        final ModuleInfoBackedContext moduleInfoBackedContext = ModuleInfoBackedContext.create();
        moduleInfoBackedContext.addModuleInfos(Collections.singleton($YangModuleInfoImpl.getInstance()));
        moduleInfoBackedContext.addModuleInfos(Collections.singleton(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.$YangModuleInfoImpl.getInstance()));
        return moduleInfoBackedContext.tryToCreateSchemaContext().get();
    }
}
