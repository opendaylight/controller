package org.opendaylight.controller.netconf.cli.io;

import static org.opendaylight.controller.netconf.cli.io.IOUtil.PATH_SEPARATOR;
import static org.opendaylight.controller.netconf.cli.io.IOUtil.PROMPT_PREFIX;
import static org.opendaylight.controller.netconf.cli.io.IOUtil.PROMPT_SUFIX;

import com.google.common.collect.Lists;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;

import java.util.List;
import jline.console.ConsoleReader;
import jline.console.completer.Completer;

import jline.console.completer.CompletionHandler;
import org.opendaylight.controller.netconf.cli.writer.WriteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Jline based IO implementation
 */
public class ConsoleIOImpl implements ConsoleIO {

    private static final Logger LOG = LoggerFactory.getLogger(ConsoleIOImpl.class);

    private final ConsoleReader console;
    protected Deque<ConsoleContext> contexts = new ArrayDeque<>();

    public ConsoleIOImpl() throws IOException {
        console = new ConsoleReader();
        console.setHandleUserInterrupt(true);
        console.setPaginationEnabled(true);
        console.setHistoryEnabled(true);
        // TODO trifferedActions not supported by jline in current version
        // https://github.com/jline/jline2/issues/149
        console.addTriggeredAction('?', new QuestionMarkActionListener());
    }

    @Override
    public String read() throws IOException {
        return console.readLine().trim();
    }

    @Override
    public void write(final CharSequence data) throws IOException {
        console.print(data);
        console.flush();
    }

    @Override
    public void writeLn(final CharSequence data) throws IOException {
        console.println(data);
        console.flush();
    }

    @Override
    public ConsoleContext getContext() {
        return contexts.peek();
    }

    @Override
    public void enterContext(final ConsoleContext consoleContext) {
        enterEntryConsoleContex(consoleContext);
        contexts.push(consoleContext);
        setCompleter(consoleContext.getCompleter());
        console.setPrompt(buildPrompt());
    }

    private void enterEntryConsoleContex(final ConsoleContext consoleContext) {
        if (contexts.peek() instanceof ListConsoleContext) {
            int entryCount = ((ListConsoleContext) contexts.peek()).getEntryCount();
            if (consoleContext instanceof EntryConsoleContext) {
                entryCount++;
                ((EntryConsoleContext) consoleContext).setEntryNumber(entryCount);
            }
        }
    }

    @Override
    public void leaveContext() {
        final ConsoleContext topConsoleContext = contexts.pop();
        leaveEntryConsoleContex(topConsoleContext);
        console.setPrompt(buildPrompt());
        if (contexts.peek() != null) {
            setCompleter(contexts.peek().getCompleter());
        }
    }

    private void leaveEntryConsoleContex(final ConsoleContext consoleContext) {
        if (consoleContext instanceof EntryConsoleContext) {
            final int entryNumber = ((EntryConsoleContext) consoleContext).getEntryNumber();
            if (contexts.peek() instanceof ListConsoleContext) {
                ((ListConsoleContext) contexts.peek()).setEntryCount(entryNumber);
            }
        }
    }

    protected String buildPrompt() {
        final StringBuilder newPrompt = new StringBuilder();
        newPrompt.append(PROMPT_PREFIX);
        newPrompt.append(contexts.isEmpty() ? "" : "(");

        final Iterator<ConsoleContext> descendingIterator = contexts.descendingIterator();
        while (descendingIterator.hasNext()) {
            final ConsoleContext consoleContext = descendingIterator.next();
            final String promptPart = consoleContext.getPrompt();
            if (promptPart != null) {
                newPrompt.append(PATH_SEPARATOR);
                newPrompt.append(promptPart);
            }
        }
        newPrompt.append(contexts.isEmpty() ? "" : ")");
        newPrompt.append(PROMPT_SUFIX);

        return newPrompt.toString();
    }

    private void setCompleter(final Completer newCompleter) {
        for (final Completer concreteCompleter : console.getCompleters()) {
            console.removeCompleter(concreteCompleter);
        }
        console.addCompleter(newCompleter);
    }

    private class QuestionMarkActionListener implements ActionListener {
        @Override
        public void actionPerformed(final ActionEvent e) {
            final ArrayList<CharSequence> candidates = Lists.newArrayList();
            contexts.peek().getCompleter().complete("", 0, candidates);
            try {
                console.getCompletionHandler().complete(console, candidates, 0);
            } catch (final IOException ex) {
                throw new IllegalStateException("Unable to write to output", ex);
            }
        }
    }
}
