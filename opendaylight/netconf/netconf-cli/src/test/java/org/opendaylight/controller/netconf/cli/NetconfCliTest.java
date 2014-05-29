package org.opendaylight.controller.netconf.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.opendaylight.controller.netconf.cli.io.IOUtil;
import org.opendaylight.controller.netconf.cli.reader.ReadingException;
import org.opendaylight.controller.netconf.cli.reader.impl.GenericReader;
import org.opendaylight.controller.netconf.cli.writer.WriteException;
import org.opendaylight.controller.netconf.cli.writer.impl.GenericWriter;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.parser.api.YangModelParser;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;

public class NetconfCliTest {

    private final static YangModelParser parser = new YangParserImpl();

    private static Set<Module> loadModules(String resourceDirectory) throws FileNotFoundException {
        final File testDir = new File(resourceDirectory);
        final String[] fileList = testDir.list();
        final List<File> testFiles = new ArrayList<File>();
        if (fileList == null) {
            throw new FileNotFoundException(resourceDirectory);
        }
        for (int i = 0; i < fileList.length; i++) {
            String fileName = fileList[i];
            if (new File(testDir, fileName).isDirectory() == false) {
                testFiles.add(new File(testDir, fileName));
            }
        }
        return parser.parseYangModels(testFiles);
    }

    public static Set<Module> loadModulesFrom(String yangPath) {
        try {
            return loadModules(NetconfCliTest.class.getResource(yangPath).getPath());
        } catch (FileNotFoundException e) {
            // LOG.error("Yang files at path: " + yangPath +
            // " weren't loaded.");
        }

        return null;
    }

    public static SchemaContext loadSchemaContext(String resourceDirectory) throws FileNotFoundException {
        return parser.resolveSchemaContext(loadModulesFrom(resourceDirectory));
    }

    // @Ignore
    @Test
    public void cliTest() throws ReadingException, IOException, WriteException {

        SchemaContext schemaContext = loadSchemaContext("/schema-context");
        assertNotNull(schemaContext);

        DataSchemaNode cont1 = findTopLevelElement("ns:model1", "2014-05-14", "cont1", schemaContext);
        Map<String, String> values = new HashMap<>();

        values.put("lf1111", "55");
        values.put("lf1112", "value for lf1112");
        values.put("lflst1111", "10");
        values.put("lf1211", "value for lf1211");
        values.put("lf12111", "5");
        values.put("lf12112", "value for lf12112");
        values.put("chcA", IOUtil.qNameToKeyString(QName.create("ns:model1", "2014-05-14", "AB")));
        values.put("lf12AB1", "value for lf12AB1");
        values.put("lf111", "value for lf111");

        List<ValueForMessage> valuesForMessages = new ArrayList<>();
        valuesForMessages.add(new ValueForMessage("Y", "lst111", "[Y|N]"));
        valuesForMessages.add(new ValueForMessage("Y", "lst121", "[Y|N]"));
        valuesForMessages.add(new ValueForMessage("Y", "lst11", "[Y|N]"));

        ConsoleIOTestImpl console = new ConsoleIOTestImpl(values, valuesForMessages);

        List<Node<?>> redData = new GenericReader(console).read(cont1);
        assertNotNull(redData);
        assertEquals(1, redData.size());

        assertTrue(redData.get(0) instanceof CompositeNode);
        CompositeNode redTopLevelNode = (CompositeNode) redData.get(0);

        System.out.println("============================");
        new GenericWriter(console, "").write(cont1, redData);
        // System.out.println(output);

    }

    private DataSchemaNode findTopLevelElement(String namespace, String revision, String topLevelElement,
            SchemaContext schemaContext) {
        QName requiredElement = QName.create(namespace, revision, topLevelElement);
        for (DataSchemaNode dataSchemaNode : schemaContext.getChildNodes()) {
            if (dataSchemaNode.getQName().equals(requiredElement)) {
                return dataSchemaNode;
            }
        }
        return null;

    }

}
