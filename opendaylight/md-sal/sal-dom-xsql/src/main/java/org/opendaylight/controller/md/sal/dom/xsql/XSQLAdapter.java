/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.xsql;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.xsql.jdbc.JDBCResultSet;
import org.opendaylight.controller.md.sal.dom.xsql.jdbc.JDBCServer;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/
public class XSQLAdapter extends Thread implements SchemaContextListener {
    //The XSQL adapter instance
    private static XSQLAdapter adapterInstance = new XSQLAdapter();
    //Is the adapter stopped
    public boolean stopped = false;
    //The last input string in the console
    private StringBuffer lastInputString = new StringBuffer();
    //The blue print container that was generated from the scenma context
    private XSQLBluePrint bluePrint = new XSQLBluePrint();
    //Should the query output be forward to a comma delimited file format
    private boolean toCsv = false;
    //The name of the csv file, if not define will be generated
    private String exportToFileName = null;
    //The thread pool to handle the seeks in the data store
    private XSQLThreadPool threadPool = new XSQLThreadPool(10, "Tasks", 2000);
    //The jdbc server listening for jdbc connections
    private JDBCServer jdbcServer = new JDBCServer(this);
    //The socket for the console connection
    private ServerSocket serverSocket = null;
    //The Data Broker service
    private DOMDataBroker domDataBroker = null;
    //The Data Broker Binding Aware codec used to translate normalized node to POJOs
    private Object codec = null;
    //The logger
    private static Logger logger = LoggerFactory.getLogger(XSQLAdapter.class);

    private XSQLAdapter() {
        XSQLAdapter.log("Starting Adapter");
        this.setDaemon(true);
        try {
            serverSocket = new ServerSocket(34343);
        } catch (Exception err) {
            XSQLAdapter.log(err);
        }
        this.start();
        XSQLAdapter.log("Adapter Started!");

    }

    //Used to extract the codec via reflection .
    //When a dependency is added, i got some issues so just using reflection here.
    private static Object extractCodec(Object dataBroker){
        try{
            Field field = XSQLODLUtils.findField(dataBroker.getClass(),"codec");
            field.setAccessible(true);
            return field.get(dataBroker);
        }catch(Exception err){
            err.printStackTrace();
        }
        return null;
    }

    //For testing purposes, i added this to load a generated blue print from file.
    public void loadBluePrint(){
        try{
            InputStream in = this.getClass().getClassLoader().getResourceAsStream("BluePrintCache.dat");
            if(in!=null){
                this.bluePrint =  XSQLBluePrint.load(in);
            }
            in.close();
        }catch(Exception err){
            err.printStackTrace();
        }
    }

    //Returns an instance of the apater
    public static XSQLAdapter getInstance() {
        return adapterInstance;
    }

    //logging everything under XSQLAdapter
    public static void log(String str) {
        if(logger==null){
            logger = LoggerFactory.getLogger(XSQLAdapter.class);
        }
        if(logger!=null)
            logger.info(str);
    }

    //logging everything under XSQLAdapter
    public static void log(Exception e) {
        if(logger==null){
            logger = LoggerFactory.getLogger(XSQLAdapter.class);
        }
        if(logger!=null)
            logger.error(e.getMessage(),e);
    }
    //Activated when the scema context gets updated, e.g. a new feature was loaded
    //so need to create a blue print for its yang model
    @Override
    public void onGlobalContextUpdated(SchemaContext context) {
        Set<Module> modules = context.getModules();
        for (Module m : modules) {
            XSQLODLUtils.createOpenDaylightCache(this.bluePrint, m);
        }
    }

    //Sets the data broker service
    public void setDataBroker(DOMDataBroker ddb) {
        this.domDataBroker = ddb;
        this.codec = extractCodec(this.domDataBroker);
    }

    //Return the blue print container
    public XSQLBluePrint getBluePrint() {
        return this.bluePrint;
    }

    //Execute a jdbc query
    public void execute(JDBCResultSet rs) {
        if(this.domDataBroker==null){
            rs.setFinished(true);
            return;
        }
        List<XSQLBluePrintNode> tables = rs.getTables();
        List<Object> roots = XSQLODLUtils.collectModuleRoots(tables.get(0),LogicalDatastoreType.OPERATIONAL,this.domDataBroker);
        roots.addAll(XSQLODLUtils.collectModuleRoots(tables.get(0),LogicalDatastoreType.CONFIGURATION,this.domDataBroker));
        if(roots.isEmpty()){
            rs.setFinished(true);
        }
        XSQLBluePrintNode main = rs.getMainTable();
        List<ModuleRootTask> tasks = new LinkedList<XSQLAdapter.ModuleRootTask>();

        for (Object entry : roots) {
            ModuleRootTask task = new ModuleRootTask(rs, entry, main, bluePrint);
            rs.numberOfTasks++;
            tasks.add(task);
        }
        for (ModuleRootTask task : tasks) {
            threadPool.addTask(task);
        }
    }

    //The main run method for the accepting console connections
    public void run() {
        while (!stopped) {
            try {
                Socket s = serverSocket.accept();
                new TelnetConnection(s);
            } catch (Exception err) {
                err.printStackTrace();
                try {
                    Thread.sleep(20000);
                } catch (Exception err2) {
                }
                stopped = true;
            }
        }
    }

    //parse a console command
    public void processCommand(StringBuffer inputString, PrintStream sout) {
        if (inputString.toString().trim().equals("r")) {
            sout.println(lastInputString);
            inputString = lastInputString;
        }
        lastInputString = inputString;
        String input = inputString.toString().trim();
        if (input.startsWith("setExcel")) {
            String substr = input.substring("setExcel".length()).trim();
            if (!substr.equals("")) {
                // excelPath01 = substr;
            }
            // sout.println("Excel Path="+excelPath01);
        } else if (input.startsWith("list vrel")) {
            String substr = input.substring("list vrel".length()).trim();
            XSQLBluePrintNode node = bluePrint
                    .getBluePrintNodeByTableName(substr);
            if (node == null) {
                sout.println("Unknown Interface " + substr);
                return;
            }
            List<String> fld = new ArrayList<String>();
            for (XSQLBluePrintRelation r : node.getRelations()) {
                fld.add(r.toString());
            }
            String p[] = (String[]) fld.toArray(new String[fld.size()]);
            Arrays.sort(p);
            for (int i = 0; i < p.length; i++) {
                sout.println(p[i]);
            }
        } else if (input.startsWith("list vfields")) {
            String substr = input.substring("list vfields".length()).trim();
            XSQLBluePrintNode node = bluePrint
                    .getBluePrintNodeByTableName(substr);
            if (node == null) {
                sout.println("Unknown Interface " + substr);
                return;
            }
            List<String> fld = new ArrayList<String>();
            for (XSQLColumn c : node.getColumns()) {
                fld.add(c.getName());
            }
            String p[] = (String[]) fld.toArray(new String[fld.size()]);
            Arrays.sort(p);
            for (int i = 0; i < p.length; i++) {
                sout.println(p[i]);
            }
        } else if (input.startsWith("jdbc")) {
            String addr = input.substring(5).trim();
            jdbcServer.connectToClient(addr);
            sout.println("Connected To " + addr);
        } else if (input.startsWith("fetch")) {
            // fetchSize = Integer.parseInt(input.substring(6).trim());
        } else if (input.startsWith("list vtables")) {

            String iNames[] = bluePrint.getAllTableNames().toArray(
                    new String[0]);
            Arrays.sort(iNames);
            sout.println();
            for (int i = 0; i < iNames.length; i++) {
                sout.println(iNames[i]);
            }
        } else if (input.equals("help") || input.equals("?")) {
            // sout.println(getLongDescription());
        } else if (input.equals("avmdata")) {
            try {
                // myConnection.getManagedData();
            } catch (Exception err) {
            }
        } else if (input.equals("innerjoin")) {
            // innerJoin = !innerJoin;
            // sout.println("Inner Join set to "+innerJoin);
        } else if (input.equals("exit")) {
            try {
                sout.close();
            } catch (Exception err) {
            }
        } else if (input.equals("save")) {
            XSQLBluePrint.save(this.bluePrint);
        } else if (input.equals("tocsv")) {
            toCsv = !toCsv;
            sout.println("to csv file is " + toCsv);
        } else if (input.indexOf("filename") != -1) {
            exportToFileName = input.substring(input.indexOf(" ")).trim();
            sout.println("Exporting to file:" + exportToFileName);
        } else if (!input.equals("")) {
            if (toCsv) {
                if (exportToFileName != null) {
                    try {
                        PrintStream o = new PrintStream(new File(
                                exportToFileName));
                        executeSql(inputString.toString(), o);
                        o.close();
                    } catch (Exception err) {
                        err.printStackTrace();
                    }
                } else {
                    try {
                        String fName = "export-" + System.currentTimeMillis()
                                + ".csv";
                        PrintStream o = new PrintStream(new File(fName));
                        executeSql(inputString.toString(), o);
                        o.close();
                        sout.println("Exported to file " + fName);
                    } catch (Exception err) {
                        err.printStackTrace();
                    }

                }
            } else {
                executeSql(inputString.toString(), sout);
            }
        }
        sout.println();
    }

    //execute an SQL query and returning a standard JDBC ResultSet object
    public ResultSet executeQuery(String sql) throws SQLException{
        JDBCResultSet rs = new JDBCResultSet(sql);
        JDBCServer.execute(rs, this);
        return rs;
    }

    //execute an SQL query and output the result either to the console or to a csv file
    public void executeSql(String sql, PrintStream out) {
        JDBCResultSet rs = new JDBCResultSet(sql);
        try {
            int count = 0;
            JDBCServer.execute(rs, this);
            boolean isFirst = true;
            int loc = rs.getFields().size() - 1;
            int totalWidth = 0;
            if(rs.getFields().isEmpty()){
                out.print("  Objects Instance Identifier  ");
                totalWidth = 120;
            }
            for (XSQLColumn c : rs.getFields()) {
                if (isFirst) {
                    isFirst = false;
                    if (toCsv) {
                        out.print("\"");
                    }
                }

                if (!toCsv) {
                    out.print("|");
                }

                out.print(c.getName());

                if (!toCsv) {
                    int cw = c.getCharWidth();
                    int cnw = c.getName().length();
                    if (cnw > cw) {
                        c.setCharWidth(cnw);
                    }
                    int gap = cw - cnw;
                    for (int i = 0; i < gap; i++) {
                        out.print(" ");
                    }
                }

                totalWidth += c.getCharWidth() + 1;

                if (loc > 0) {
                    if (toCsv) {
                        out.print("\",\"");
                    }
                }
                loc--;
            }

            if (toCsv) {
                out.println("\"");
            } else {
                totalWidth++;
                out.println("|");
                for (int i = 0; i < totalWidth; i++) {
                    out.print("-");
                }
                out.println();
            }

            while (rs.next()) {
                isFirst = true;
                if(rs.getFields().isEmpty()){
                    InstanceIdentifierDataObjectPairPath<Object, Object> pairs = (InstanceIdentifierDataObjectPairPath<Object, Object>)rs.getObject("Object");
                    out.print(pairs.getPairPath().get(0).getKey());
                    out.println("|");
                }else{
                    loc = rs.getFields().size() - 1;
                    for (XSQLColumn c : rs.getFields()) {
                        if (isFirst) {
                            isFirst = false;
                            if (toCsv) {
                                out.print("\"");
                            }
                        }

                        if (!toCsv) {
                            out.print("|");
                        }

                        Object sValue = rs.getObject(c.toString());
                        if (sValue == null) {
                            sValue = "";
                        }
                        out.print(sValue);

                        int cw = c.getCharWidth();
                        int vw = sValue.toString().length();
                        int gap = cw - vw;
                        for (int i = 0; i < gap; i++) {
                            out.print(" ");
                        }

                        if (loc > 0) {
                            if (toCsv) {
                                out.print("\",\"");
                            }
                        }
                        loc--;
                    }
                    if (toCsv) {
                        out.println("\"");
                    } else {
                        out.println("|");
                    }
                }
                count++;
            }
            out.println("Total Number Of Records=" + count);
        } catch (Exception err) {
            err.printStackTrace(out);
        }
    }

    //Currently i assume there will only one but need to think of
    //when to separate to multiple tasks
    public static class ModuleRootTask implements Runnable {

        private JDBCResultSet resultSet = null;
        private Object moduleRootPOJO = null;
        private XSQLBluePrintNode moduleRootBluePrintNode = null;
        private XSQLBluePrint bluePrint = null;

        public ModuleRootTask(JDBCResultSet _rs, Object _modelRoot,
                XSQLBluePrintNode _main, XSQLBluePrint _bluePrint) {
            this.resultSet = _rs;
            this.moduleRootPOJO = _modelRoot;
            this.moduleRootBluePrintNode = _main;
            this.bluePrint = _bluePrint;
        }

        public void run() {
            resultSet.addRecords(moduleRootPOJO, moduleRootBluePrintNode, true, moduleRootBluePrintNode.getBluePrintNodeName(),
                    bluePrint);
            synchronized (resultSet) {
                resultSet.numberOfTasks--;
                if (resultSet.numberOfTasks == 0) {
                    resultSet.setFinished(true);
                    resultSet.notifyAll();
                }
            }
        }
    }

    //A console connection
    private class TelnetConnection extends Thread {

        private Socket socket = null;
        private InputStream in = null;
        private PrintStream out = null;
        private Module currentModule = null;

        public TelnetConnection(Socket s) {
            this.socket = s;
            try {
                this.in = s.getInputStream();
                this.out = new PrintStream(s.getOutputStream());
                this.start();
            } catch (Exception err) {
                XSQLAdapter.log(err);
            }
        }

        public void run() {
            StringBuffer inputString = new StringBuffer();
            String prompt = "XSQL>";
            try {
                while (!stopped) {
                    if (currentModule != null) {
                        prompt = "XQL/" + currentModule.getName() + ">";
                    }
                    out.print(prompt);
                    char c = 0;
                    byte data[] = new byte[1];
                    while (!socket.isClosed() && socket.isConnected() && !socket.isInputShutdown() && c != '\n') {
                        try {
                            in.read(data);
                            c = (char) data[0];
                            inputString.append(c);
                        } catch (Exception err) {
                            err.printStackTrace(out);
                            stopped = true;
                            break;
                        }
                    }

                    processCommand(inputString, out);
                    inputString = new StringBuffer();
                }
            } catch (Exception err) {
                try {
                    socket.close();
                } catch (Exception err2) {
                }
            }
        }
    }

    public InstanceIdentifierDataObjectPairPath<Object,Object> toBindingAwareObject(LinkedList<Object> objects){
        return XSQLODLUtils.toBindingAwareObject(objects,this.codec);
    }
}
