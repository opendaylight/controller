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
import java.sql.ResultSet;
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
/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/
public class TableSelector extends JDialog implements KeyListener {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private JList<String> list = new JList<String>();

    public TableSelector(JFrame f, Connection c, String subset,int x, int y) {
        super(f);
        this.setModal(true);
        this.setUndecorated(true);
        this.getContentPane().setLayout(new BorderLayout());
        this.setSize(500, 300);
        this.setLocation(x,y);
        ((JComponent) this.getContentPane()).setBorder(BorderFactory
                .createTitledBorder(BorderFactory.createEtchedBorder(),
                        "Available Tables"));
        DefaultListModel<String> model = new DefaultListModel<>();
        try {
            List<String> l = new ArrayList<>();
            ResultSet rs = c.getMetaData().getTables(null, null, null, null);
            while (rs.next()) {
                String name = rs.getString(1).toString();
                if (name.indexOf(subset) != -1)
                    l.add(name);
            }
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

    public String getTable() {
        return this.list.getSelectedValue().toString();
    }

}
