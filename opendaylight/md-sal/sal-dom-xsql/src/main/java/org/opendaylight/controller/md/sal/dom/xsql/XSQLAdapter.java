package org.opendaylight.controller.md.sal.dom.xsql;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadTransaction;
import org.opendaylight.controller.md.sal.dom.xsql.jdbc.JDBCResultSet;
import org.opendaylight.controller.md.sal.dom.xsql.jdbc.JDBCServer;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;

public class XSQLAdapter extends Thread implements SchemaContextListener {

    private static final int SLEEP = 10000;
    private static XSQLAdapter a = new XSQLAdapter();
    private static PrintStream l = null;
    private static String tmpDir = null;
    private static File xqlLog = null;
    public boolean stopped = false;
    private List<String> elementHosts = new ArrayList<String>();
    private String username;
    private String password;
    private String transport = "tcp";
    private int reconnectTimeout;
    private int nThreads;
    private int qsize;
    private String applicationName = "NQL Adapter";
    private Map<String, NEEntry> elements = new ConcurrentHashMap<String, XSQLAdapter.NEEntry>();
    private StringBuffer lastInputString = new StringBuffer();
    private XSQLBluePrint bluePrint = new XSQLBluePrint();
    private boolean toCsv = false;
    private String exportToFileName = null;
    private XSQLThreadPool threadPool = new XSQLThreadPool(1, "Tasks", 2000);
    private JDBCServer jdbcServer = new JDBCServer(this);
    private String pinningFile;
    private ServerSocket serverSocket = null;
    private DOMDataBroker domDataBroker = null;

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

    public static XSQLAdapter getInstance() {
        return a;
    }

    public static File getXQLLogfile() {
        tmpDir = System.getProperty("java.io.tmpdir");
        xqlLog = new File(tmpDir + "/xql.log");
        return xqlLog;
    }

    public static void main(String args[]) {
        XSQLAdapter adapter = new XSQLAdapter();
        adapter.start();
    }

    public static void log(String str) {
        try {
            if (l == null) {
                synchronized (XSQLAdapter.class) {
                    if (l == null) {
                        l = new PrintStream(
                                new FileOutputStream(getXQLLogfile()));
                    }
                }
            }
            l.print(Calendar.getInstance().getTime());
            l.print(" - ");
            l.println(str);
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    public static void log(Exception e) {
        try {
            if (l == null) {
                synchronized (XSQLAdapter.class) {
                    if (l == null) {
                        l = new PrintStream(
                                new FileOutputStream(getXQLLogfile()));
                    }
                }
            }
            l.print(Calendar.getInstance().getTime());
            l.print(" - ");
            e.printStackTrace(l);
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    @Override
    public void onGlobalContextUpdated(SchemaContext context) {
        Set<Module> modules = context.getModules();
        for (Module m : modules) {
            if (XSQLODLUtils.createOpenDaylightCache(this.bluePrint, m)) {
                this.addRootElement(m);
            }
        }
    }

    public void setDataBroker(DOMDataBroker ddb) {
        this.domDataBroker = ddb;
    }

    public XSQLBluePrint getBluePrint() {
        return this.bluePrint;
    }

    public List<Object> collectModuleRoots(XSQLBluePrintNode table,LogicalDatastoreType type) {
        if (table.getParent().isModule()) {
            try {
                List<Object> result = new LinkedList<Object>();
                YangInstanceIdentifier instanceIdentifier = YangInstanceIdentifier
                        .builder()
                        .node(XSQLODLUtils.getPath(table.getODLNodes().get(0)).get(0))
                        .toInstance();
                DOMDataReadTransaction t = this.domDataBroker
                        .newReadOnlyTransaction();
                Object node = t.read(type,
                        instanceIdentifier).get();

                node = XSQLODLUtils.get(node, "reference");
                if (node == null) {
                    return result;
                }
                result.add(node);
                return result;
            } catch (Exception err) {
                XSQLAdapter.log(err);
            }
        } else {
            return collectModuleRoots(table.getParent(),type);
        }
        return null;
    }

    public void execute(JDBCResultSet rs) {
        if(this.domDataBroker==null){
            rs.setFinished(true);
            return;
        }
        List<XSQLBluePrintNode> tables = rs.getTables();
        List<Object> roots = collectModuleRoots(tables.get(0),LogicalDatastoreType.OPERATIONAL);
        roots.addAll(collectModuleRoots(tables.get(0),LogicalDatastoreType.CONFIGURATION));
        if(roots.isEmpty()){
            rs.setFinished(true);
        }
        XSQLBluePrintNode main = rs.getMainTable();
        List<NETask> tasks = new LinkedList<XSQLAdapter.NETask>();

        for (Object entry : roots) {
            NETask task = new NETask(rs, entry, main, bluePrint);
            rs.numberOfTasks++;
            tasks.add(task);
        }
        for (NETask task : tasks) {
            threadPool.addTask(task);
        }
    }

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

    public void addRootElement(Object o) {
        NEEntry entry = new NEEntry(o);
        elements.put(o.toString(), entry);

    }

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

    public void executeSql(String sql, PrintStream out) {
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

        private JDBCResultSet rs = null;
        private Object modelRoot = null;
        private XSQLBluePrintNode main = null;
        private XSQLBluePrint bluePrint = null;

        public NETask(JDBCResultSet _rs, Object _modelRoot,
                XSQLBluePrintNode _main, XSQLBluePrint _bluePrint) {
            this.rs = _rs;
            this.modelRoot = _modelRoot;
            this.main = _main;
            this.bluePrint = _bluePrint;
        }

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

    private static class NEEntry {
        private Object ne = null;

        public NEEntry(Object _ne) {
            this.ne = _ne;
        }

        public String toString() {
            Module m = (Module) ne;
            return m.getName() + "  [" + m.getNamespace().toString() + "]";
        }
    }

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
}
