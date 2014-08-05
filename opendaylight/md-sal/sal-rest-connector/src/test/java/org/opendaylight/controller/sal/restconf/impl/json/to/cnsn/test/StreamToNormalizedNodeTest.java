/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.json.to.cnsn.test;

import com.google.gson.stream.JsonReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import org.junit.Test;
import org.opendaylight.controller.sal.rest.gson.JsonParserStream;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.test.TestUtils;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.NormalizedNodeContainerBuilder;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class StreamToNormalizedNodeTest {

    @Test
    public void advancedJsonReaderTest() throws FileNotFoundException {
        SchemaContext schemaContext = TestUtils.loadSchemaContext("/complexjson/yang");

        FileInputStream is = new FileInputStream(StreamToNormalizedNodeTest.class.getResource(
                "/complexjson/complex-json.json").getPath());

//        JsonParserStream parser = new JsonParserStream(new NormalizedNodeStreamWriterImpl(), null);
        NormalizedNodeContainerBuilder<?, ?, ?, ?> result = Builders.containerBuilder();
        NormalizedNodeStreamWriter streamWriter = ImmutableNormalizedNodeStreamWriter.from(result);

        JsonParserStream parser = new JsonParserStream( streamWriter, null);
        ControllerContext.getInstance().setSchemas(schemaContext);
        parser.parse(new JsonReader(new InputStreamReader(is)), schemaContext);
        System.out.println(result.build());

    }

    private class NormalizedNodeStreamWriterImpl implements NormalizedNodeStreamWriter {

        int indent = 0;

        private String ind() {
            final StringBuilder builder = new StringBuilder();
            for (int i = 0; i < indent; i++) {
                builder.append(" ");
            }
            return builder.toString();
        }

        @Override
        public void startUnkeyedListItem(NodeIdentifier name, int childSizeHint) throws IllegalStateException {
            System.out.println(ind() + name + "[](no key)");
            indent += 2;
        }

        @Override
        public void startUnkeyedList(NodeIdentifier name, int childSizeHint) throws IllegalArgumentException {
            System.out.println(ind() + name + "(no key)");
            indent += 2;
        }

        @Override
        public void startOrderedMapNode(NodeIdentifier name, int childSizeHint) throws IllegalArgumentException {

        }

        @Override
        public void startMapNode(NodeIdentifier name, int childSizeHint) throws IllegalArgumentException {
            System.out.println(ind() + name + "(key)");
            indent += 2;
        }

        @Override
        public void startMapEntryNode(NodeIdentifierWithPredicates identifier, int childSizeHint)
                throws IllegalArgumentException {
            System.out.println(ind() + identifier + "[](key)");
            indent += 2;
        }

        @Override
        public void startLeafSet(NodeIdentifier name, int childSizeHint) throws IllegalArgumentException {
            System.out.println(ind() + name + "(leaf-list)");
            indent += 2;
        }

        @Override
        public void startContainerNode(NodeIdentifier name, int childSizeHint) throws IllegalArgumentException {
            System.out.println(ind() + name + "(container)");
            indent += 2;
        }

        @Override
        public void startChoiceNode(NodeIdentifier name, int childSizeHint) throws IllegalArgumentException {
            System.out.println(ind() + name + "(choice)");
            indent += 2;
        }

        @Override
        public void startAugmentationNode(AugmentationIdentifier identifier) throws IllegalArgumentException {
            System.out.println(ind() + identifier + "(augmentation)");
            indent += 2;
        }

        @Override
        public void leafSetEntryNode(Object value) throws IllegalArgumentException {
            System.out.println(ind() + value + "("+value.getClass().getSimpleName() +") ");
        }

        @Override
        public void leafNode(NodeIdentifier name, Object value) throws IllegalArgumentException {
            System.out.println(ind() + name + "(leaf"+"("+value.getClass().getSimpleName()+")"+")=" + value);
        }

        @Override
        public void endNode() throws IllegalStateException {
            indent -= 2;
            System.out.println(ind() + "(end)");
        }

        @Override
        public void anyxmlNode(NodeIdentifier name, Object value) throws IllegalArgumentException {
            System.out.println(ind() + name + "(anyxml)=" + value);
        }
    }
}
