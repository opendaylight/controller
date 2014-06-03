package org.opendaylight.controller.netconf.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.opendaylight.controller.netconf.cli.io.IOUtil.PROMPT_SUFIX;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.opendaylight.controller.netconf.cli.reader.ReadingException;
import org.opendaylight.controller.netconf.cli.reader.impl.GenericReader;
import org.opendaylight.controller.netconf.cli.writer.OutFormatter;
import org.opendaylight.controller.netconf.cli.writer.WriteException;
import org.opendaylight.controller.netconf.cli.writer.impl.GenericWriter;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.parser.api.YangContextParser;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;

public class NetconfCliTest {

    private final static YangContextParser parser = new YangParserImpl();

    private static SchemaContext loadSchemaContext(final String resourceDirectory) throws IOException,
            URISyntaxException {
        final URI uri = NetconfCliTest.class.getResource(resourceDirectory).toURI();
        final File testDir = new File(uri);
        final String[] fileList = testDir.list();
        final List<File> testFiles = new ArrayList<File>();
        if (fileList == null) {
            throw new FileNotFoundException(resourceDirectory);
        }
        for (final String fileName : fileList) {
            if (new File(testDir, fileName).isDirectory() == false) {
                testFiles.add(new File(testDir, fileName));
            }
        }
        return parser.parseFiles(testFiles);
    }

    // @Ignore
    @Test
    public void cliTest() throws ReadingException, IOException, WriteException, URISyntaxException {

        final SchemaContext schemaContext = loadSchemaContext("/schema-context");
        assertNotNull(schemaContext);

        final DataSchemaNode cont1 = findTopLevelElement("ns:model1", "2014-05-14", "cont1", schemaContext);
        final Map<String, Deque<String>> values = new HashMap<>();

        values.put(prompt("/cont1/cont11/lst111/[1]/lf1111"), value("55"));
        values.put(prompt("/cont1/cont11/lst111/[1]"), value("Y"));
        values.put(prompt("/cont1/cont11/lst111/[1]/cont111/lf1112"), value("value for lf1112"));
        values.put(prompt("/cont1/cont11/lst111/[1]/cont111/lflst1111"), value("Y", "N"));
        values.put(prompt("/cont1/cont11/lst111/[1]/cont111/lflst1111/[1]"), value("10"));
        values.put(prompt("/cont1/cont11/lst111/[1]/cont111/lflst1111/[2]"), value("15"));

        values.put(prompt("/cont1/cont11/lst111"), value("Y", "N"));

        values.put(prompt("/cont1/cont11/lst111/[2]/lf1111"), value("32"));
        values.put(prompt("/cont1/cont11/lst111/[2]"), value("Y"));
        values.put(prompt("/cont1/cont11/lst111/[2]/cont111/lf1112"), value("2value for lf1112"));
        values.put(prompt("/cont1/cont11/lst111/[2]/cont111/lflst1111"), value("Y", "N"));
        values.put(prompt("/cont1/cont11/lst111/[2]/cont111/lflst1111/[1]"), value("20"));
        values.put(prompt("/cont1/cont11/lst111/[2]/cont111/lflst1111/[2]"), value("30"));

        values.put(prompt("/cont1/cont12/chcA"), value("AB"));
        values.put(prompt("/cont1/cont12/chcA/cont12AB1/lf12AB1"), value("value for lf12AB1"));

        values.put(prompt("/cont1/cont12/lst121/[1]/lf1211"), value("value for lf12112"));
        values.put(prompt("/cont1/cont12/lst121/[1]"), value("Y"));
        values.put(prompt("/cont1/cont12/lst121/[1]/lst1211"), value("Y", "N"));
        values.put(prompt("/cont1/cont12/lst121/[1]/lst1211/[1]"), value("Y"));
        values.put(prompt("/cont1/cont12/lst121/[1]/lst1211/[1]/lf12111"), value("5"));
        values.put(prompt("/cont1/cont12/lst121/[1]/lst1211/[1]/lf12112"), value("value for lf12112"));
        values.put(prompt("/cont1/cont12/lst121/[1]/lst1211/[2]"), value("Y"));
        values.put(prompt("/cont1/cont12/lst121/[1]/lst1211/[2]/lf12111"), value("10"));
        values.put(prompt("/cont1/cont12/lst121/[1]/lst1211/[2]/lf12112"), value("2value for lf12112"));

        values.put(prompt("/cont1/cont12/lst121"), value("Y", "N"));

        values.put(prompt("/cont1/cont12/lst121/[2]/lf1211"), value("2value for lf12112"));
        values.put(prompt("/cont1/cont12/lst121/[2]"), value("Y"));
        values.put(prompt("/cont1/cont12/lst121/[2]/lst1211"), value("Y", "N"));
        values.put(prompt("/cont1/cont12/lst121/[2]/lst1211/[1]"), value("Y"));
        values.put(prompt("/cont1/cont12/lst121/[2]/lst1211/[1]/lf12111"), value("21"));
        values.put(prompt("/cont1/cont12/lst121/[2]/lst1211/[1]/lf12112"), value("2value for lf12112"));
        values.put(prompt("/cont1/cont12/lst121/[2]/lst1211/[2]"), value("Y"));
        values.put(prompt("/cont1/cont12/lst121/[2]/lst1211/[2]/lf12111"), value("50"));
        values.put(prompt("/cont1/cont12/lst121/[2]/lst1211/[2]/lf12112"), value("22value for lf12112"));

        values.put(prompt("/cont1/cont12/lst122"), value("Y", "N"));

        values.put(prompt("/cont1/lst11"), value("Y", "Y", "N"));
        values.put(prompt("/cont1/lst11/[1]"), value("Y"));
        values.put(prompt("/cont1/lst11/[2]"), value("Y"));
        values.put(prompt("/cont1/lst11/[3]"), value("Y"));
        values.put(prompt("/cont1/lst11/[1]/lf111"), value("1value for lf111"));
        values.put(prompt("/cont1/lst11/[2]/lf111"), value("2value for lf111"));
        values.put(prompt("/cont1/lst11/[3]/lf111"), value("3value for lf111"));

        values.put(prompt("/cont1/cont12/data"), value("<el1><el11>value</el11><el12>value1</el12></el1>"));

        final List<ValueForMessage> valuesForMessages = new ArrayList<>();
        valuesForMessages.add(new ValueForMessage("Y", "lst111", "[Y|N]"));
        valuesForMessages.add(new ValueForMessage("Y", "lst121", "[Y|N]"));
        valuesForMessages.add(new ValueForMessage("Y", "lst11", "[Y|N]"));

        final ConsoleIOTestImpl console = new ConsoleIOTestImpl(values, valuesForMessages);

        final List<Node<?>> redData = new GenericReader(console).read(cont1);
        assertNotNull(redData);
        assertEquals(1, redData.size());

        assertTrue(redData.get(0) instanceof CompositeNode);
        final CompositeNode redTopLevelNode = (CompositeNode) redData.get(0);

        System.out.println("============================");
        new GenericWriter(console, new OutFormatter()).write(cont1, redData);

    }

    private Deque<String> value(final String... values) {
        return new ArrayDeque<>(Arrays.asList(values));
    }

    private String prompt(final String path) {
        return path + PROMPT_SUFIX;
    }

    private DataSchemaNode findTopLevelElement(final String namespace, final String revision,
            final String topLevelElement, final SchemaContext schemaContext) {
        final QName requiredElement = QName.create(namespace, revision, topLevelElement);
        for (final DataSchemaNode dataSchemaNode : schemaContext.getChildNodes()) {
            if (dataSchemaNode.getQName().equals(requiredElement)) {
                return dataSchemaNode;
            }
        }
        return null;

    }

}
