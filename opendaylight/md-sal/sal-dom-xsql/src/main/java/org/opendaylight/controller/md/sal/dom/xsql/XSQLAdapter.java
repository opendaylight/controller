/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.xsql;

import com.google.common.base.Optional;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.xsql.jdbc.JDBCResultSet;
import org.opendaylight.controller.md.sal.dom.xsql.jdbc.JDBCServer;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/
/**
 * To be removed in Nitrogen
 */
@Deprecated
public class XSQLAdapter extends Thread implements SchemaContextListener {
    private static final Logger LOG = LoggerFactory.getLogger(XSQLAdapter.class);

    private static final int SLEEP = 10000;
    private static XSQLAdapter a = new XSQLAdapter();
    private static PrintStream l = null;
    private static String tmpDir = null;
    private static File xqlLog = null;
    public boolean stopped = false;
    private String username;
    private String password;
    private final String transport = "tcp";
    private int reconnectTimeout;
    private int nThreads;
    private int qsize;
    private final String applicationName = "NQL Adapter";
    private StringBuffer lastInputString = new StringBuffer();
    private boolean toCsv = false;
    private String exportToFileName = null;
    private final XSQLThreadPool threadPool = new XSQLThreadPool(1, "Tasks", 2000);
    private final JDBCServer jdbcServer = new JDBCServer(this);
    private String pinningFile;
    private ServerSocket serverSocket = null;
    private DOMDataBroker domDataBroker = null;

    @GuardedBy("this")
    private SchemaContext context;
    @GuardedBy("this")
    private XSQLBluePrint bluePrint = new XSQLBluePrint();

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

    public synchronized void loadBluePrint(){
        try{
            InputStream in = this.getClass().getClassLoader().getResourceAsStream("BluePrintCache.dat");
            if(in!=null){
                this.bluePrint = XSQLBluePrint.load(in);
                in.close();
            }
        }catch(Exception err){
            err.printStackTrace();
        }
    }

    public static XSQLAdapter getInstance() {
        return a;
    }

    public static File getXQLLogfile() {
        tmpDir = System.getProperty("java.io.tmpdir");
        xqlLog = new File(tmpDir + "/xql.log");
        return xqlLog;
    }

    public static void main(final String args[]) {
        XSQLAdapter adapter = new XSQLAdapter();
        adapter.start();
    }

    public static void log(final String str) {
        try {
            if (l == null) {
                synchronized (XSQLAdapter.class) {
                    if (l == null) {
                        l = new PrintStream(
                                new FileOutputStream(getXQLLogfile()));
                    }
                }
            }
            l.print(new Date());
            l.print(" - ");
            l.println(str);
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    public static void log(final Exception e) {
        try {
            if (l == null) {
                synchronized (XSQLAdapter.class) {
                    if (l == null) {
                        l = new PrintStream(
                                new FileOutputStream(getXQLLogfile()));
                    }
                }
            }
            l.print(new Date());
            l.print(" - ");
            e.printStackTrace(l);
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    @Override
    public synchronized void onGlobalContextUpdated(final SchemaContext context) {
        this.bluePrint = null;
        this.context = context;
    }

    public void setDataBroker(final DOMDataBroker ddb) {
        this.domDataBroker = ddb;
    }

    public synchronized XSQLBluePrint getBluePrint() {
        if (bluePrint == null) {
            LOG.warn("XSQL is not supported in production environments and will be removed in a future release");
            bluePrint = XSQLBluePrint.create(context);
        }

        return bluePrint;
    }

    public List<NormalizedNode<?, ?>> collectModuleRoots(final XSQLBluePrintNode table,final LogicalDatastoreType type) {
        if (table.getParent().isModule()) {
            try {
                List<NormalizedNode<?, ?>> result = new LinkedList<>();
                YangInstanceIdentifier instanceIdentifier = YangInstanceIdentifier
                        .builder()
                        .node(XSQLODLUtils.getPath(table.getFirstFromSchemaNodes()).get(0))
                        .build();
                DOMDataReadOnlyTransaction t = this.domDataBroker
                        .newReadOnlyTransaction();
                Optional<NormalizedNode<?, ?>> node = t.read(type,
                        instanceIdentifier).get();
                t.close();

                if (node.isPresent()) {
                    result.add(node.get());
                }

                return result;
            } catch (Exception err) {
                XSQLAdapter.log(err);
            }
        } else {
            return collectModuleRoots(table.getParent(),type);
        }
        return null;
    }

    public void execute(final JDBCResultSet rs) {
        if(this.domDataBroker==null){
            rs.setFinished(true);
            return;
        }
        List<XSQLBluePrintNode> tables = rs.getTables();
        List<NormalizedNode<?, ?>> roots = collectModuleRoots(tables.get(0),LogicalDatastoreType.OPERATIONAL);
        roots.addAll(collectModuleRoots(tables.get(0),LogicalDatastoreType.CONFIGURATION));
        if(roots.isEmpty()){
            rs.setFinished(true);
        }
        XSQLBluePrintNode main = rs.getMainTable();
        List<NETask> tasks = new LinkedList<>();

        for (NormalizedNode<?, ?> entry : roots) {
            NETask task = new NETask(rs, entry, main, getBluePrint());
            rs.numberOfTasks++;
            tasks.add(task);
        }
        for (NETask task : tasks) {
            threadPool.addTask(task);
        }
    }

    @Override
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

    public void processCommand(StringBuffer inputString, final PrintStream sout) {
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
            XSQLBluePrintNode node = getBluePrint().getBluePrintNodeByTableName(substr);
            if (node == null) {
                sout.println("Unknown Interface " + substr);
                return;
            }
            List<String> fld = new ArrayList<>();
            for (XSQLBluePrintRelation r : node.getRelations()) {
                fld.add(r.toString());
            }
            String p[] = fld.toArray(new String[fld.size()]);
            Arrays.sort(p);
            for (String element : p) {
                sout.println(element);
            }
        } else if (input.startsWith("list vfields")) {
            String substr = input.substring("list vfields".length()).trim();
            XSQLBluePrintNode node = getBluePrint().getBluePrintNodeByTableName(substr);
            if (node == null) {
                sout.println("Unknown Interface " + substr);
                return;
            }
            List<String> fld = new ArrayList<>();
            for (XSQLColumn c : node.getColumns()) {
                fld.add(c.getName());
            }
            String p[] = fld.toArray(new String[fld.size()]);
            Arrays.sort(p);
            for (String element : p) {
                sout.println(element);
            }
        } else if (input.startsWith("jdbc")) {
            String addr = input.substring(5).trim();
            jdbcServer.connectToClient(addr);
            sout.println("Connected To " + addr);
        } else if (input.startsWith("fetch")) {
            // fetchSize = Integer.parseInt(input.substring(6).trim());
        } else if (input.startsWith("list vtables")) {

            String iNames[] = getBluePrint().getAllTableNames().toArray(
                    new String[0]);
            Arrays.sort(iNames);
            sout.println();
            for (String iName : iNames) {
                sout.println(iName);
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
            getBluePrint().save();
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

    public void executeSql(final String sql, final PrintStream out) {
        JDBCResultSet rs = new JDBCResultSet(sql);
        try {
            int count = 0;
            JDBCServer.execute(rs, this);
            boolean isFirst = true;
            int loc = rs.getFields().size() - 1;
            int totalWidth = 0;
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
                count++;
            }
            out.println("Total Number Of Records=" + count);
        } catch (Exception err) {
            err.printStackTrace(out);
        }
    }

    public static class NETask implements Runnable {

        private final JDBCResultSet rs;
        private final NormalizedNode<?, ?> modelRoot;
        private final XSQLBluePrintNode main;
        private final XSQLBluePrint bluePrint;

        public NETask(final JDBCResultSet _rs, final NormalizedNode<?, ?> _modelRoot,
                final XSQLBluePrintNode _main, final XSQLBluePrint _bluePrint) {
            this.rs = _rs;
            this.modelRoot = _modelRoot;
            this.main = _main;
            this.bluePrint = _bluePrint;
        }

        @Override
        public void run() {
            rs.addRecords(modelRoot, main, true, main.getBluePrintNodeName(),
                    bluePrint);
            synchronized (rs) {
                rs.numberOfTasks--;
                if (rs.numberOfTasks == 0) {
                    rs.setFinished(true);
                    rs.notifyAll();
                }
            }
        }
    }

    private class TelnetConnection extends Thread {

        private Socket socket = null;
        private InputStream in = null;
        private PrintStream out = null;
        private final Module currentModule = null;

        public TelnetConnection(final Socket s) {
            this.socket = s;
            try {
                this.in = s.getInputStream();
                this.out = new PrintStream(s.getOutputStream());
                this.start();
            } catch (Exception err) {
                XSQLAdapter.log(err);
            }
        }

        @Override
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
}
