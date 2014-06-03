package org.opendaylight.controller.netconf.cli.io;

import jline.console.completer.Completer;

import com.google.common.base.Optional;

/**
 * Context to be set in the IO. Different prompts + completers are required in different contexts of the CLI.
 */
public interface ConsoleContext {

    Completer getCompleter();

    Optional<String> getPrompt();

}
