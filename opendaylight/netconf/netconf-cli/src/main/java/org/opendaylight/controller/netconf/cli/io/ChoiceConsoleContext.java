package org.opendaylight.controller.netconf.cli.io;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import jline.console.completer.Completer;
import jline.console.completer.StringsCompleter;

import org.opendaylight.yangtools.yang.model.api.ChoiceCaseNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceNode;

public class ChoiceConsoleContext<T extends ChoiceNode> extends BaseConsoleContext<T> {

    protected Map<String, ChoiceCaseNode> availableCases = new HashMap<String, ChoiceCaseNode>();

    // TODO remove console context specific implementations from this package,
    // should be with readers they belong to

    public ChoiceConsoleContext(final T schemaNode) {
        super(schemaNode);
    }

    @Override
    public Completer getCompleter() {
        final Collection<String> completerValues = collectAllCases(dataSchemaNode);
        return new StringsCompleter(completerValues);
    }

    private Collection<String> collectAllCases(final ChoiceNode schemaNode) {
        final Collection<String> completerValues = new ArrayList<String>();
        for (final ChoiceCaseNode concreteCase : schemaNode.getCases()) {
            final String caseName = concreteCase.getQName().getLocalName();
            availableCases.put(caseName, concreteCase);
            completerValues.add(caseName);
        }
        return completerValues;
    }

    public ChoiceCaseNode getCaseSchemaNode(final String key) {
        return availableCases.get(key);
    }
}
