package org.opendaylight.controller.netconf.cli.reader.impl;

import static org.opendaylight.controller.netconf.cli.io.IOUtil.SKIP;
import static org.opendaylight.controller.netconf.cli.io.IOUtil.qNameToKeyString;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jline.console.completer.Completer;
import jline.console.completer.StringsCompleter;

import org.opendaylight.controller.netconf.cli.io.BaseConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.controller.netconf.cli.reader.AbstractReader;
import org.opendaylight.controller.netconf.cli.reader.ReadingException;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.model.api.ChoiceCaseNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceNode;

public class ChoiceReader extends AbstractReader<ChoiceNode> {

    // FIXME remove any mutating instance variables for all readers/writers
    private Map<String, ChoiceCaseNode> availableCases = new HashMap<String, ChoiceCaseNode>();

    public ChoiceReader(ConsoleIO console) {
        super(console);
    }

    @Override
    public List<Node<?>> readInner(ChoiceNode choiceNode) throws IOException, ReadingException {
        console.writeLn("Select case for choice " + choiceNode.getQName().getLocalName()
                + ". Use TAB to propose values.");

        ChoiceCaseNode selectedCase = null;
        boolean isCaseSelected = false;
        while (!isCaseSelected) {
            String rawValue = console.read();
            selectedCase = availableCases.get(rawValue);
            isCaseSelected = selectedCase != null;
            if (rawValue.equals(SKIP)) {
                return Collections.emptyList();
            }
        }

        return new GenericReader(console).read(selectedCase.getChildNodes());
    }

    @Override
    protected ConsoleContext getContext(final ChoiceNode schemaNode) {
        return new BaseConsoleContext(schemaNode) {

            @Override
            public Completer getCompleter() {
                return prepareCaseCompleters(schemaNode);
            }
        };
    }

    private Completer prepareCaseCompleters(ChoiceNode choiceNode) {
        Collection<String> completerCases = new ArrayList<String>();
        for (ChoiceCaseNode concreteCase : choiceNode.getCases()) {
            String menuParamValue = qNameToKeyString(concreteCase.getQName());
            completerCases.add(menuParamValue);
            availableCases.put(menuParamValue, concreteCase);
        }
        return new StringsCompleter(completerCases);
    }
}
