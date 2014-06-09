package org.opendaylight.controller.netconf.cli.reader.impl;

import static org.opendaylight.controller.netconf.cli.io.IOUtil.isSkipInput;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import jline.console.completer.Completer;
import jline.console.completer.StringsCompleter;
import org.opendaylight.controller.netconf.cli.CommandArgHandlerRegistry;
import org.opendaylight.controller.netconf.cli.io.BaseConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.controller.netconf.cli.reader.AbstractReader;
import org.opendaylight.controller.netconf.cli.reader.ReadingException;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.model.api.ChoiceCaseNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.Maps;

public class ChoiceReader extends AbstractReader<ChoiceNode> {

    private static final Logger LOG = LoggerFactory.getLogger(ChoiceReader.class);

    private final CommandArgHandlerRegistry argumentHandlerRegistry;

    public ChoiceReader(final ConsoleIO console, final CommandArgHandlerRegistry argumentHandlerRegistry) {
        super(console);
        this.argumentHandlerRegistry = argumentHandlerRegistry;
    }

    @Override
    public List<Node<?>> readWithContext(final ChoiceNode choiceNode) throws IOException, ReadingException {
        final Map<String, ChoiceCaseNode> availableCases = collectAllCases(choiceNode);
        console.formatLn("Select case for choice %s from %s", choiceNode.getQName().getLocalName(),
                availableCases.keySet());

        ChoiceCaseNode selectedCase = null;
        final String rawValue = console.read();
        if (isSkipInput(rawValue)) {
            return Collections.emptyList();
        }

        selectedCase = availableCases.get(rawValue);
        if (selectedCase == null) {
            final String message = String.format("Incorrect value (%s) for choice %s was selected.", rawValue,
                    choiceNode.getQName().getLocalName());
            LOG.error(message);
            throw new ReadingException(message);
        }

        final List<Node<?>> newNodes = new ArrayList<>();
        for (final DataSchemaNode schemaNode : selectedCase.getChildNodes()) {
            newNodes.addAll(argumentHandlerRegistry.getGenericReader().read(schemaNode));
        }
        return newNodes;
    }

    private Map<String, ChoiceCaseNode> collectAllCases(final ChoiceNode schemaNode) {
        return Maps.uniqueIndex(schemaNode.getCases(), new Function<ChoiceCaseNode, String>() {
            @Override
            public String apply(final ChoiceCaseNode input) {
                return input.getQName().getLocalName();
            }
        });
    }

    @Override
    protected ConsoleContext getContext(final ChoiceNode schemaNode) {
        return new BaseConsoleContext<ChoiceNode>(schemaNode) {
            @Override
            public List<Completer> getAdditionalCompleters() {
                return Collections
                        .<Completer> singletonList(new StringsCompleter(collectAllCases(schemaNode).keySet()));
            }
        };
    }
}
