/*
 *
 *  Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.controller.cluster.datastore.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;
import org.opendaylight.controller.protobuff.messages.common.SimpleNormalizedNodeMessage;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.impl.codec.xml.XmlDocumentUtils;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.CollectionNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.ListNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.NormalizedNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.transform.dom.DomUtils;
import org.opendaylight.yangtools.yang.data.impl.schema.transform.dom.parser.DomToNormalizedNodeParserFactory;
import org.opendaylight.yangtools.yang.data.impl.schema.transform.dom.serializer.DomFromNormalizedNodeSerializerFactory;
import org.opendaylight.yangtools.yang.model.api.ChoiceNode;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Two of the testcases in the yangtools/yang-data-impl are leveraged (with modification) to create
 * the serialization of NormalizedNode using the ProtocolBuffer
 *
 * @syedbahm
 *
 */


public class NormalizedNodeXmlConverterTest {
  private static final Logger logger = LoggerFactory
      .getLogger(NormalizedNodeXmlConverterTest.class);
  public static final String NAMESPACE =
      "urn:opendaylight:params:xml:ns:yang:controller:test";
  private static Date revision;
  private ContainerNode expectedNode;
  private ContainerSchemaNode containerNode;
  private String xmlPath;

  static {
    try {
      revision = new SimpleDateFormat("yyyy-MM-dd").parse("2014-03-13");
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }

  public static DataSchemaNode getSchemaNode(final SchemaContext context,
      final String moduleName, final String childNodeName) {
    for (Module module : context.getModules()) {
      if (module.getName().equals(moduleName)) {
        DataSchemaNode found =
            findChildNode(module.getChildNodes(), childNodeName);
        Preconditions.checkState(found != null, "Unable to find %s",
            childNodeName);
        return found;
      }
    }
    throw new IllegalStateException("Unable to find child node "
        + childNodeName);
  }

  static DataSchemaNode findChildNode(
      final Collection<DataSchemaNode> children, final String name) {
    List<DataNodeContainer> containers = Lists.newArrayList();

    for (DataSchemaNode dataSchemaNode : children) {
      if (dataSchemaNode.getQName().getLocalName().equals(name)) {
        return dataSchemaNode;
      }
      if (dataSchemaNode instanceof DataNodeContainer) {
        containers.add((DataNodeContainer) dataSchemaNode);
      } else if (dataSchemaNode instanceof ChoiceNode) {
        containers.addAll(((ChoiceNode) dataSchemaNode).getCases());
      }
    }

    for (DataNodeContainer container : containers) {
      DataSchemaNode retVal = findChildNode(container.getChildNodes(), name);
      if (retVal != null) {
        return retVal;
      }
    }

    return null;
  }

  public static YangInstanceIdentifier.NodeIdentifier getNodeIdentifier(
      final String localName) {
    return new YangInstanceIdentifier.NodeIdentifier(QName.create(
        URI.create(NAMESPACE), revision, localName));
  }

  public static YangInstanceIdentifier.AugmentationIdentifier getAugmentIdentifier(
      final String... childNames) {
    Set<QName> qn = Sets.newHashSet();

    for (String childName : childNames) {
      qn.add(getNodeIdentifier(childName).getNodeType());
    }

    return new YangInstanceIdentifier.AugmentationIdentifier(qn);
  }


  public static ContainerNode augmentChoiceExpectedNode() {

    DataContainerNodeBuilder<YangInstanceIdentifier.NodeIdentifier, ContainerNode> b =
        Builders.containerBuilder();
    b.withNodeIdentifier(getNodeIdentifier("container"));

    b.withChild(Builders
        .choiceBuilder()
        .withNodeIdentifier(getNodeIdentifier("ch2"))
        .withChild(
            Builders.leafBuilder()
                .withNodeIdentifier(getNodeIdentifier("c2Leaf")).withValue("2")
                .build())
        .withChild(
            Builders
                .choiceBuilder()
                .withNodeIdentifier(getNodeIdentifier("c2DeepChoice"))
                .withChild(
                    Builders
                        .leafBuilder()
                        .withNodeIdentifier(
                            getNodeIdentifier("c2DeepChoiceCase1Leaf2"))
                        .withValue("2").build()).build()).build());

    b.withChild(Builders
        .choiceBuilder()
        .withNodeIdentifier(getNodeIdentifier("ch3"))
        .withChild(
            Builders.leafBuilder()
                .withNodeIdentifier(getNodeIdentifier("c3Leaf")).withValue("3")
                .build()).build());

    b.withChild(Builders
        .augmentationBuilder()
        .withNodeIdentifier(getAugmentIdentifier("augLeaf"))
        .withChild(
            Builders.leafBuilder()
                .withNodeIdentifier(getNodeIdentifier("augLeaf"))
                .withValue("augment").build()).build());

    b.withChild(Builders
        .augmentationBuilder()
        .withNodeIdentifier(getAugmentIdentifier("ch"))
        .withChild(
            Builders
                .choiceBuilder()
                .withNodeIdentifier(getNodeIdentifier("ch"))
                .withChild(
                    Builders.leafBuilder()
                        .withNodeIdentifier(getNodeIdentifier("c1Leaf"))
                        .withValue("1").build())
                .withChild(
                    Builders
                        .augmentationBuilder()
                        .withNodeIdentifier(
                            getAugmentIdentifier("c1Leaf_AnotherAugment",
                                "deepChoice"))
                        .withChild(
                            Builders
                                .leafBuilder()
                                .withNodeIdentifier(
                                    getNodeIdentifier("c1Leaf_AnotherAugment"))
                                .withValue("1").build())
                        .withChild(
                            Builders
                                .choiceBuilder()
                                .withNodeIdentifier(
                                    getNodeIdentifier("deepChoice"))
                                .withChild(
                                    Builders
                                        .leafBuilder()
                                        .withNodeIdentifier(
                                            getNodeIdentifier("deepLeafc1"))
                                        .withValue("1").build()).build())
                        .build()).build()).build());

    return b.build();
  }



  public void init(final String yangPath, final String xmlPath,
      final ContainerNode expectedNode) throws Exception {
    SchemaContext schema = parseTestSchema(yangPath);
    this.xmlPath = xmlPath;
    this.containerNode =
        (ContainerSchemaNode) getSchemaNode(schema, "test", "container");
    this.expectedNode = expectedNode;
  }

  SchemaContext parseTestSchema(final String yangPath) throws Exception {

    YangParserImpl yangParserImpl = new YangParserImpl();
    InputStream stream =
        NormalizedNodeXmlConverterTest.class.getResourceAsStream(yangPath);
    ArrayList<InputStream> al = new ArrayList<InputStream>();
    al.add(stream);
    Set<Module> modules = yangParserImpl.parseYangModelsFromStreams(al);
    return yangParserImpl.resolveSchemaContext(modules);

  }


  @Test
  public void testConversionWithAugmentChoice() throws Exception {
    init("/augment_choice.yang", "/augment_choice.xml",
        augmentChoiceExpectedNode());
    Document doc = loadDocument(xmlPath);

    ContainerNode built =
        DomToNormalizedNodeParserFactory
            .getInstance(DomUtils.defaultValueCodecProvider())
            .getContainerNodeParser()
            .parse(Collections.singletonList(doc.getDocumentElement()),
                containerNode);

    if (expectedNode != null) {
      junit.framework.Assert.assertEquals(expectedNode, built);
    }

    logger.info("{}", built);

    Iterable<Element> els =
        DomFromNormalizedNodeSerializerFactory
            .getInstance(XmlDocumentUtils.getDocument(),
                DomUtils.defaultValueCodecProvider())
            .getContainerNodeSerializer().serialize(containerNode, built);

    Element el = els.iterator().next();

    XMLUnit.setIgnoreWhitespace(true);
    XMLUnit.setIgnoreComments(true);

    System.out.println(toString(doc.getDocumentElement()));
    System.out.println(toString(el));

    new Diff(XMLUnit.buildControlDocument(toString(doc.getDocumentElement())),
        XMLUnit.buildTestDocument(toString(el))).similar();
  }

  private static ContainerNode listLeafListWithAttributes() {
    DataContainerNodeBuilder<YangInstanceIdentifier.NodeIdentifier, ContainerNode> b =
        Builders.containerBuilder();
    b.withNodeIdentifier(getNodeIdentifier("container"));

    CollectionNodeBuilder<MapEntryNode, MapNode> listBuilder =
        Builders.mapBuilder().withNodeIdentifier(getNodeIdentifier("list"));

    Map<QName, Object> predicates = Maps.newHashMap();
    predicates.put(getNodeIdentifier("uint32InList").getNodeType(), 3L);

    DataContainerNodeBuilder<YangInstanceIdentifier.NodeIdentifierWithPredicates, MapEntryNode> list1Builder =
        Builders.mapEntryBuilder().withNodeIdentifier(
            new YangInstanceIdentifier.NodeIdentifierWithPredicates(
                getNodeIdentifier("list").getNodeType(), predicates));
    NormalizedNodeBuilder<YangInstanceIdentifier.NodeIdentifier, Object, LeafNode<Object>> uint32InListBuilder =
        Builders.leafBuilder().withNodeIdentifier(
            getNodeIdentifier("uint32InList"));

    list1Builder.withChild(uint32InListBuilder.withValue(3L).build());

    listBuilder.withChild(list1Builder.build());
    b.withChild(listBuilder.build());

    NormalizedNodeBuilder<YangInstanceIdentifier.NodeIdentifier, Object, LeafNode<Object>> booleanBuilder =
        Builders.leafBuilder().withNodeIdentifier(getNodeIdentifier("boolean"));
    booleanBuilder.withValue(false);
    b.withChild(booleanBuilder.build());

    ListNodeBuilder<Object, LeafSetEntryNode<Object>> leafListBuilder =
        Builders.leafSetBuilder().withNodeIdentifier(
            getNodeIdentifier("leafList"));

    NormalizedNodeBuilder<YangInstanceIdentifier.NodeWithValue, Object, LeafSetEntryNode<Object>> leafList1Builder =
        Builders.leafSetEntryBuilder().withNodeIdentifier(
            new YangInstanceIdentifier.NodeWithValue(getNodeIdentifier(
                "leafList").getNodeType(), "a"));

    leafList1Builder.withValue("a");

    leafListBuilder.withChild(leafList1Builder.build());
    b.withChild(leafListBuilder.build());

    return b.build();
  }


  @Test
  public void testConversionWithAttributes() throws Exception {
    init("/test.yang", "/simple_xml_with_attributes.xml",
        listLeafListWithAttributes());
    Document doc = loadDocument(xmlPath);

    ContainerNode built =
        DomToNormalizedNodeParserFactory
            .getInstance(DomUtils.defaultValueCodecProvider())
            .getContainerNodeParser()
            .parse(Collections.singletonList(doc.getDocumentElement()),
                containerNode);

    if (expectedNode != null) {
      junit.framework.Assert.assertEquals(expectedNode, built);
    }

    logger.info("{}", built);

    Iterable<Element> els =
        DomFromNormalizedNodeSerializerFactory
            .getInstance(XmlDocumentUtils.getDocument(),
                DomUtils.defaultValueCodecProvider())
            .getContainerNodeSerializer().serialize(containerNode, built);

    Element el = els.iterator().next();

    XMLUnit.setIgnoreWhitespace(true);
    XMLUnit.setIgnoreComments(true);

    System.out.println(toString(doc.getDocumentElement()));
    System.out.println(toString(el));

    new Diff(XMLUnit.buildControlDocument(toString(doc.getDocumentElement())),
        XMLUnit.buildTestDocument(toString(el))).similar();
  }


  private Document loadDocument(final String xmlPath) throws Exception {
    InputStream resourceAsStream =
        NormalizedNodeXmlConverterTest.class.getResourceAsStream(xmlPath);

    Document currentConfigElement = readXmlToDocument(resourceAsStream);
    Preconditions.checkNotNull(currentConfigElement);
    return currentConfigElement;
  }

  private static final DocumentBuilderFactory BUILDERFACTORY;

  static {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    factory.setCoalescing(true);
    factory.setIgnoringElementContentWhitespace(true);
    factory.setIgnoringComments(true);
    BUILDERFACTORY = factory;
  }

  private Document readXmlToDocument(final InputStream xmlContent)
      throws IOException, SAXException {
    DocumentBuilder dBuilder;
    try {
      dBuilder = BUILDERFACTORY.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      throw new RuntimeException("Failed to parse XML document", e);
    }
    Document doc = dBuilder.parse(xmlContent);

    doc.getDocumentElement().normalize();
    return doc;
  }

  public static String toString(final Element xml) {
    try {
      Transformer transformer =
          TransformerFactory.newInstance().newTransformer();
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");

      StreamResult result = new StreamResult(new StringWriter());
      DOMSource source = new DOMSource(xml);
      transformer.transform(source, result);

      return result.getWriter().toString();
    } catch (IllegalArgumentException | TransformerFactoryConfigurationError
        | TransformerException e) {
      throw new RuntimeException("Unable to serialize xml element " + xml, e);
    }
  }

  @Test
  public void testConversionToNormalizedXml() throws Exception {
    SimpleNormalizedNodeMessage.NormalizedNodeXml nnXml =
        EncoderDecoderUtil.encode(parseTestSchema("/augment_choice.yang"),
            augmentChoiceExpectedNode());
    Document expectedDoc = loadDocument("/augment_choice.xml");
    Document convertedDoc =
        EncoderDecoderUtil.factory.newDocumentBuilder().parse(
            new ByteArrayInputStream(nnXml.getXmlString().getBytes("utf-8")));
    System.out.println(toString(convertedDoc.getDocumentElement()));
    XMLUnit.setIgnoreWhitespace(true);
    XMLUnit.setIgnoreComments(true);
    new Diff(XMLUnit.buildControlDocument(toString(expectedDoc
        .getDocumentElement())),
        XMLUnit.buildTestDocument(toString(convertedDoc.getDocumentElement())))
        .similar();
    System.out.println(toString(expectedDoc.getDocumentElement()));

  }


  @Test
  public void testConversionFromXmlToNormalizedNode() throws Exception {
    SimpleNormalizedNodeMessage.NormalizedNodeXml nnXml =
        EncoderDecoderUtil.encode(parseTestSchema("/test.yang"),
            listLeafListWithAttributes());
    Document expectedDoc = loadDocument("/simple_xml_with_attributes.xml");
    Document convertedDoc =
        EncoderDecoderUtil.factory.newDocumentBuilder().parse(
            new ByteArrayInputStream(nnXml.getXmlString().getBytes("utf-8")));
    System.out.println(toString(convertedDoc.getDocumentElement()));
    XMLUnit.setIgnoreWhitespace(true);
    XMLUnit.setIgnoreComments(true);
    new Diff(XMLUnit.buildControlDocument(toString(expectedDoc
        .getDocumentElement())),
        XMLUnit.buildTestDocument(toString(convertedDoc.getDocumentElement())))
        .similar();
    System.out.println(toString(expectedDoc.getDocumentElement()));

    // now we will try to convert xml back to normalize node.
    ContainerNode cn =
        (ContainerNode) EncoderDecoderUtil.decode(
            parseTestSchema("/test.yang"), nnXml);
    junit.framework.Assert.assertEquals(listLeafListWithAttributes(), cn);

  }

}
