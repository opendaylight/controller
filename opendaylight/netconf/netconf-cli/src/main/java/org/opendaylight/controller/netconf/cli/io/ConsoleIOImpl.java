package org.opendaylight.controller.netconf.cli.io;

import static org.opendaylight.controller.netconf.cli.io.IOUtil.PATH_SEPARATOR;
import static org.opendaylight.controller.netconf.cli.io.IOUtil.PROMPT_PREFIX;
import static org.opendaylight.controller.netconf.cli.io.IOUtil.PROMPT_SUFIX;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;

import jline.console.ConsoleReader;
import jline.console.completer.Completer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

/**
 * Jline based IO implementation
 */
public class ConsoleIOImpl implements ConsoleIO {

    private static final Logger LOG = LoggerFactory.getLogger(ConsoleIOImpl.class);

    private final ConsoleReader console;
    private final Deque<ConsoleContext> contexts = new ArrayDeque<>();

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
    public String read(final Character mask) throws IOException {
        return console.readLine(mask).trim();
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
    public void formatLn(final String format, final Object... args) throws IOException {
        console.println(String.format(format, args));
        console.flush();
    }

    @Override
    public void enterContext(final ConsoleContext consoleContext) {
        contexts.push(consoleContext);
        setCompleter(consoleContext.getCompleter());
        console.setPrompt(buildPrompt());
    }

    @Override
    public void leaveContext() {
        contexts.pop();
        console.setPrompt(buildPrompt());
        if (contexts.peek() != null) {
            setCompleter(contexts.peek().getCompleter());
        }
    }

    protected String buildPrompt() {
        final StringBuilder newPrompt = new StringBuilder();
        newPrompt.append(PROMPT_PREFIX);
        newPrompt.append(contexts.isEmpty() ? "" : "(");

        final Iterator<ConsoleContext> descendingIterator = contexts.descendingIterator();
        while (descendingIterator.hasNext()) {
            final ConsoleContext consoleContext = descendingIterator.next();
            final Optional<String> promptPart = consoleContext.getPrompt();
            if (promptPart.isPresent()) {
                newPrompt.append(PATH_SEPARATOR);
                newPrompt.append(promptPart.get());
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
