/*
 * Copyright (c) 2016 Inocybe Technologies.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.mdsal.cli;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.apache.karaf.shell.table.ShellTable;

/**
 * @author mserngawy
 *
 */
public class Utils {

    public static void printBeanTable(MBeanServer server, ObjectName objName, String title) throws IntrospectionException, InstanceNotFoundException, ReflectionException, AttributeNotFoundException, MBeanException {
        ShellTable table = new ShellTable();
        System.out.println("::" + title + "::");
        table.column("Name");
        table.column("Value");
        MBeanInfo beanInfo = server.getMBeanInfo(objName);
        MBeanAttributeInfo[] attrInfo = beanInfo.getAttributes();
        for (MBeanAttributeInfo att : attrInfo) {
            table.addRow().addContent(splitCapitalLetter(att.getName()), server.getAttribute(objName, att.getName()));
        }
        table.print(System.out);
        System.out.println("");
    }

    private static String splitCapitalLetter(String field) {
        String[] strArr = field.split("(?=\\p{Lu})");
        String temp = "";
        for (String str : strArr) {
            temp += str + " ";
        }
        return temp;
    }
}
