/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli.reader.custom;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.util.List;
import org.opendaylight.controller.netconf.cli.CommandArgHandlerRegistry;
import org.opendaylight.controller.netconf.cli.commands.CommandConstants;
import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.controller.netconf.cli.reader.ReadingException;
import org.opendaylight.controller.netconf.cli.reader.impl.ChoiceReader;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.model.api.ChoiceCaseNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class EditContentReader extends ChoiceReader {

    public static final QName EDIT_CONTENT_QNAME = QName.create(CommandConstants.NETCONF_BASE_QNAME, "edit-content");
    public static final QName CONFIG_QNAME = QName.create(EDIT_CONTENT_QNAME, "config");

    // FIXME this could be removed if feature/if-feature are supported

    public EditContentReader(final ConsoleIO console, final CommandArgHandlerRegistry argumentHandlerRegistry, final SchemaContext schemaContext) {
        super(console, argumentHandlerRegistry, schemaContext);
    }

    @Override
    public List<Node<?>> readWithContext(final ChoiceSchemaNode choiceNode) throws IOException, ReadingException {
        Preconditions.checkState(choiceNode.getQName().equals(EDIT_CONTENT_QNAME), "Unexpected choice %s, expected %s", choiceNode, EDIT_CONTENT_QNAME);
        final ChoiceCaseNode selectedCase = choiceNode.getCaseNodeByName(CONFIG_QNAME);
        Preconditions.checkNotNull(selectedCase, "Unexpected choice %s, expected %s that contains %s", choiceNode, EDIT_CONTENT_QNAME, CONFIG_QNAME);
        return readSelectedCase(selectedCase);
    }

}
