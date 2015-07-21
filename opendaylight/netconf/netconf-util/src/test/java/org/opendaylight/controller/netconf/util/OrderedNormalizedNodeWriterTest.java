package org.opendaylight.controller.netconf.util;

import static org.mockito.Mockito.doReturn;
import static org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes.leafNode;

import com.google.common.io.ByteSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import junit.framework.Assert;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.SimpleDateFormatUtil;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.codec.xml.XMLStreamNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableChoiceNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.ModuleIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.model.parser.api.YangSyntaxErrorException;
import org.opendaylight.yangtools.yang.parser.builder.impl.BuilderUtils;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;

public class OrderedNormalizedNodeWriterTest extends TestCase {

    @Mock
    private Module oldModule;

    @Mock
    private Module newModule;

    private Map<ModuleIdentifier, String> sources;

    private URI ns;
    private Date oldDate;
    private Date newDate;

    static final XMLOutputFactory XML_FACTORY;
    static {
        XML_FACTORY = XMLOutputFactory.newFactory();
        XML_FACTORY.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, false);
    }

    NormalizedNodeStreamWriter normalizedStreamWriter;//Mockito.mock(NormalizedNodeStreamWriter.class,Mockito.CALLS_REAL_METHODS);

    SchemaContext context;

    private ChoiceNode choiceNode = null;

    private LeafNode leafNode = null;

    public static final String TOP_LEVEL_LIST_FOO_KEY_VALUE = "foo";
    //public static final TopLevelListKey TOP_LEVEL_LIST_FOO_KEY = new TopLevelListKey(TOP_LEVEL_LIST_FOO_KEY_VALUE);

    public static final QName TOP_QNAME =
            QName.create("urn:opendaylight:params:xml:ns:yang:yangtools:test:binding", "2014-07-01", "top");
    //public static final QName TOP_LEVEL_LIST_QNAME = QName.create(TOP_QNAME, "top-level-list");
    //public static final QName TOP_LEVEL_LIST_KEY_QNAME = QName.create(TOP_QNAME, "name");
    //public static final QName TOP_LEVEL_LEAF_LIST_QNAME = QName.create(TOP_QNAME, "top-level-leaf-list");
    //public static final QName NESTED_LIST_QNAME = QName.create(TOP_QNAME, "nested-list");
    //public static final QName NESTED_LIST_KEY_QNAME = QName.create(TOP_QNAME, "name");
    public static final QName CHOICE_CONTAINER_QNAME =
            QName.create("urn:opendaylight:params:xml:ns:yang:yangtools:test:binding", "2014-07-01", "choice-container");
    public static final QName CHOICE_IDENTIFIER_QNAME = QName.create(CHOICE_CONTAINER_QNAME, "identifier");
    public static final QName CHOICE_IDENTIFIER_ID_QNAME = QName.create(CHOICE_CONTAINER_QNAME, "id");
    //public static final QName SIMPLE_ID_QNAME = QName.create(CHOICE_CONTAINER_QNAME, "simple-id");
    public static final QName EXTENDED_ID_QNAME = QName.create(CHOICE_CONTAINER_QNAME, "extended-id");
    //private static final QName SIMPLE_VALUE_QNAME = QName.create(TreeComplexUsesAugment.QNAME, "simple-value");

    public void tearDown() throws Exception {

    }

    @Before
    public void setUp() throws ParseException, URISyntaxException, IOException, YangSyntaxErrorException, XMLStreamException {
        MockitoAnnotations.initMocks(this);

        //SchemaContext schemaContext = Mockito.mock(SchemaContext.class);

        context = parseSchemas(getYangModulesAsStreamList());

        final SchemaNode schemaNode = Mockito.mock(SchemaNode.class, Mockito.CALLS_REAL_METHODS);

        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        //writer.writeStartElement("element");

        XMLStreamWriter writer = XML_FACTORY.createXMLStreamWriter(out);

        normalizedStreamWriter = XMLStreamNormalizedNodeStreamWriter.create(writer, context , SchemaPath.create(true));

        ns = new URI("http://abc");
        oldDate = SimpleDateFormatUtil.getRevisionFormat().parse("2014-07-20");
        newDate = SimpleDateFormatUtil.getRevisionFormat().parse("2014-07-22");

        leafNode = leafNode(CHOICE_IDENTIFIER_ID_QNAME, "identifier_value");//ImmutableLeafNodeBuilder.create()

        choiceNode = ImmutableChoiceNodeBuilder.create().withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(CHOICE_IDENTIFIER_QNAME))
                .withChild(ImmutableContainerNodeBuilder.create().withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(EXTENDED_ID_QNAME))
                        .withChild(leafNode(CHOICE_IDENTIFIER_ID_QNAME, "identifier_value")).build()).build();
        //normalizedStreamWriter = Mockito.spy(NormalizedNodeStreamWriter.class);
        //try {
        //    doNothing().when(normalizedStreamWriter).startChoiceNode(new YangInstanceIdentifier.NodeIdentifier(CHOICE_IDENTIFIER_QNAME),1);
        //} catch (IOException e) {
//
        //}

        doReturn("abc").when(oldModule).getName();
        doReturn(oldDate).when(oldModule).getRevision();
        doReturn(ns).when(oldModule).getNamespace();
        doReturn("abc").when(newModule).getName();
        doReturn(newDate).when(newModule).getRevision();
        doReturn(ns).when(newModule).getNamespace();
        //doReturn();

        //doAnswer(Answer ).when()

        sources = Collections.emptyMap();
    }

    @Test(expected = java.lang.IllegalStateException.class)
    public void testWasProcessedAsCompositeNode() throws IOException {

        //SchemaContext schemaContext = mock(SchemaContext.class, )

        //Mockito.spy(schemaContext);

        //doReturn(null).when()

        OrderedNormalizedNodeWriter writeNode =
                new OrderedNormalizedNodeWriter(normalizedStreamWriter, context,
                        SchemaPath.create(true));

        final UnkeyedListNode unkeyedListNode = Mockito.mock(UnkeyedListNode.class);

        //final ChoiceNode choiceNode = Mockito.spy(ImmutableChoiceNodeBuilder.class);
        //final ChoiceNode choiceNode = Mockito.mock(ChoiceNode.class, Mockito.CALLS_REAL_METHODS);
        //ImmutableChoiceNodeBuilder builder = new ImmutableChoiceNodeBuilder();
        //ImmutableChoiceNodeBuilder.create()
        writeNode.write(leafNode);
        Assert.assertTrue(true);

        //try {
        //    assertNotNull(writeNode.write(choiceNode));
        //} catch (IOException e) {
        //    assertThat(e.getMessage(),);
        //}
    }

    public List<InputStream> getYangModulesAsStreamList() {

        List<String> schemaPaths = Arrays.asList("/yang/mdsal-netconf-mapping-test.yang");
        List<InputStream> schemasStreams = new ArrayList<>();

        for (String schema : schemaPaths) {
            InputStream stream = getClass().getResourceAsStream(schema);
            schemasStreams.add(stream);
        }

        return schemasStreams;
    }

    public SchemaContext parseSchemas(Collection<InputStream> schemas) throws IOException, YangSyntaxErrorException {

        YangParserImpl parser = new YangParserImpl();

        Collection<ByteSource> sources = BuilderUtils.streamsToByteSources(schemas);

        return parser.parseSources(sources);

    }

}