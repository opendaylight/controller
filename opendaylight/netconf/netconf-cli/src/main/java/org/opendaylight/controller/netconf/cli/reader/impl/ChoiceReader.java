package org.opendaylight.controller.netconf.cli.reader.impl;

import static org.opendaylight.controller.netconf.cli.io.IOUtil.SKIP;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.opendaylight.controller.netconf.cli.io.ChoiceConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.controller.netconf.cli.reader.AbstractReader;
import org.opendaylight.controller.netconf.cli.reader.ReadingException;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.model.api.ChoiceCaseNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceNode;

public class ChoiceReader extends AbstractReader<ChoiceNode> {

    // FIXME remove any mutating instance variables for all readers/writers

    public ChoiceReader(final ConsoleIO console) {
        super(console);
    }

    @Override
    public List<Node<?>> readInner(final ChoiceNode choiceNode) throws IOException, ReadingException {
        console.writeLn("Select case for choice " + choiceNode.getQName().getLocalName()
                + ". Use TAB to propose values.");

        ChoiceCaseNode selectedCases = null;
        boolean isCaseSelected = false;
        while (!isCaseSelected) {
            final String rawValue = console.read();
            selectedCases = ((ChoiceConsoleContext<ChoiceNode>) console.getContext()).getCaseSchemaNode(rawValue);
            isCaseSelected = selectedCases != null;
            if (rawValue.equals(SKIP)) {
                return Collections.emptyList();
            }
        }

        return new GenericReader(console).read(selectedCases.getChildNodes());
    }

    @Override
    protected ConsoleContext getContext(final ChoiceNode schemaNode) {
        return new ChoiceConsoleContext<ChoiceNode>(schemaNode);
    }
}
