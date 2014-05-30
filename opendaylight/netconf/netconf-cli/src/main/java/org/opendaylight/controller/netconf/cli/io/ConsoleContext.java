package org.opendaylight.controller.netconf.cli.io;

import jline.console.completer.Completer;

/**
 * Context to be set in the IO. Different prompts + completers are required in different contexts of the CLI.
 */
public interface ConsoleContext {

    Completer getCompleter();

    String getPrompt();

}
