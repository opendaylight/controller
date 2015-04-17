/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.xsql.client;

import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;

import org.opendaylight.controller.md.sal.dom.xsql.XSQLBluePrint;
import org.opendaylight.controller.md.sal.dom.xsql.XSQLBluePrintNode;
import org.opendaylight.controller.md.sal.dom.xsql.XSQLColumn;
/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/
public class FieldSelector extends JDialog implements KeyListener {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private JList<String> list = new JList<String>();

    public FieldSelector(JFrame f, Connection c, String table,int x, int y) {
        super(f);
        if (table.endsWith("."))
            table = table.substring(0, table.length() - 1);
        this.setModal(true);
        this.setUndecorated(true);
        this.getContentPane().setLayout(new BorderLayout());
        this.setSize(400, 300);
        this.setLocation(x, y);
        ((JComponent) this.getContentPane()).setBorder(BorderFactory
                .createTitledBorder(BorderFactory.createEtchedBorder(),
                        "Available Fields"));
        DefaultListModel<String> model = new DefaultListModel<>();
        try {
            List<String> l = new ArrayList<>();
            XSQLBluePrint bl = (XSQLBluePrint) c.getMetaData();
            XSQLBluePrintNode node = bl.getBluePrintNodeByTableName(table);
            for (XSQLColumn col : node.getColumns()) {
                l.add(col.getName());
            }
            l.add("*");
            Collections.sort(l);
            for (String e : l) {
                model.addElement(e);
            }
        } catch (Exception err) {
            err.printStackTrace();
        }
        this.add(new JScrollPane(list), BorderLayout.CENTER);
        list.setModel(model);
        list.addKeyListener(this);
        this.setVisible(true);
    }

    @Override
    public void keyPressed(KeyEvent arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void keyReleased(KeyEvent arg0) {
        if (arg0.getKeyCode() == KeyEvent.VK_ENTER) {
            this.setVisible(false);
        }
    }

    @Override
    public void keyTyped(KeyEvent arg0) {
        // TODO Auto-generated method stub

    }

    public String getField() {
        return this.list.getSelectedValue().toString();
    }
}
