package org.opendaylight.persisted;

import java.io.File;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import org.opendaylight.persisted.codec.ISerializer;
import org.opendaylight.persisted.codec.MDSALColumn;
import org.opendaylight.persisted.codec.MDSALTable;
import org.opendaylight.persisted.codec.MDSALTableRepository;
import org.opendaylight.persisted.jdbc.JDBCResultSet;
import org.opendaylight.persisted.jdbc.JDBCServer;
import org.opendaylight.yangtools.yang.binding.DataObject;

public class MDSALDatabase {
    private Map<String, MDSALBlockDataLocation> location = new HashMap<String, MDSALBlockDataLocation>();
    private static int MAX_X_VECTOR = 10;
    private static int MAX_Y_VECTOR = 10;
    private static int MAX_Z_VECTOR = 10;
    public static String DB_LOCATION = "./DataObjectDB";
    private int X_VECTOR = 0;
    private int Y_VECTOR = 0;
    private int Z_VECTOR = 0;

    private int X_VECTOR_COUNT = 0;
    private int Y_VECTOR_COUNT = 0;
    private int Z_VECTOR_COUNT = 0;
    private boolean closed = false;
    private JDBCServer jdbcServer = new JDBCServer(this);
    private MDSALDBThreadpool threadpool = new MDSALDBThreadpool(2,
            "MDSAL Database Threadpool", 2000);

    public MDSALDatabase() {
        init();
    }

    public static void deleteDatabase() {
        File f = new File(DB_LOCATION);
        deleteDirectory(f);
        MDSALTableRepository.getInstance().deleteRepository();
    }

    public static void deleteDirectory(File dir) {
        File files[] = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory())
                    deleteDirectory(file);
                else
                    file.delete();
            }
        }
        dir.delete();
    }

    public void write(Object _object, int parentRecordIndex) {
        MDSALTable ctype = null;
        if (_object instanceof DataObject) {
            ctype = MDSALTableRepository.getInstance().getCtypeByClass(
                    ((DataObject) _object).getImplementedInterface());
        } else {
            ctype = MDSALTableRepository.getInstance().getCtypeByClass(
                    _object.getClass());
        }
        ISerializer s = ctype.getSerializer();
        String blockKey = s.getBlockKey(_object);
        if (blockKey == null) {
            blockKey = "Default";
        }
        String recordKey = (String) s.getRecordKey(_object);
        MD5ID md5ID = null;
        if (recordKey != null) {
            md5ID = MD5ID.createX(recordKey);
        }
        MDSALBlockDataLocation gl = getGLocation(blockKey);
        gl.writeObject(_object, md5ID, parentRecordIndex);
    }

    public Object read(Class<?> clazz, int index) {
        MDSALBlockDataLocation gl = getGLocation("Default");
        return gl.readObject(clazz, index);
    }

    public Object readAllGraphBySingleNode(Class<?> clazz,int index){
        MDSALBlockDataLocation gl = getGLocation("Default");
        return gl.readAllObjectFromNode(clazz, index);
    }

    private void init() {
        MDSALColumn.IS_SERVER_SIDE = true;
        File dbDir = new File(DB_LOCATION);
        if (dbDir.exists()) {
            File xDirs[] = dbDir.listFiles();
            if (xDirs != null) {
                for (File xDir : xDirs) {
                    int xID = xDir.getName().indexOf("X-");
                    if (xID != -1) {
                        int xx = Integer.parseInt(xDir.getName().substring(
                                xID + 2));
                        if (X_VECTOR < xx)
                            X_VECTOR = xx;
                        File yDirs[] = xDir.listFiles();
                        if (yDirs != null) {
                            for (File yDir : yDirs) {
                                int yID = yDir.getName().indexOf("Y-");
                                if (yID != -1) {
                                    int yy = Integer.parseInt(yDir.getName()
                                            .substring(yID + 2));
                                    if (Y_VECTOR < yy)
                                        Y_VECTOR = yy;
                                    File zDirs[] = yDir.listFiles();
                                    if (zDirs != null) {
                                        for (File zDir : zDirs) {
                                            int zID = zDir.getName().indexOf(
                                                    "Z-");
                                            if (zID != -1) {
                                                int zz = Integer.parseInt(zDir
                                                        .getName().substring(
                                                                zID + 2));
                                                if (Z_VECTOR < zz)
                                                    Z_VECTOR = zz;
                                                File objects[] = zDir
                                                        .listFiles();
                                                if (objects != null) {
                                                    for (File obj : objects) {
                                                        MDSALBlockDataLocation newLoc = new MDSALBlockDataLocation(
                                                                xx,
                                                                yy,
                                                                zz,
                                                                MDSALBlockDataLocation
                                                                        .readBlockKey(obj
                                                                                .getPath()));
                                                        this.location
                                                                .put(newLoc
                                                                        .getBlockKey(),
                                                                        newLoc);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private MDSALBlockDataLocation getGLocation(String blockKey) {
        synchronized (location) {
            MDSALBlockDataLocation loc = location.get(blockKey);
            if (loc == null) {
                Z_VECTOR_COUNT++;
                if (Z_VECTOR_COUNT > MAX_Z_VECTOR) {
                    Z_VECTOR++;
                    Z_VECTOR_COUNT = 0;
                    Y_VECTOR_COUNT++;
                }

                if (Y_VECTOR_COUNT > MAX_Y_VECTOR) {
                    Z_VECTOR = 0;
                    Y_VECTOR++;
                    Y_VECTOR_COUNT = 0;
                    X_VECTOR_COUNT++;
                }

                if (X_VECTOR_COUNT > MAX_X_VECTOR) {
                    Z_VECTOR = 0;
                    Y_VECTOR = 0;
                    X_VECTOR++;
                }
                loc = new MDSALBlockDataLocation(X_VECTOR, Y_VECTOR, Z_VECTOR,
                        blockKey);
                location.put(blockKey, loc);
            }
            return loc;
        }
    }

    public void executeSql(String sql, PrintStream out, boolean toCsv) {
        JDBCResultSet rs = new JDBCResultSet(sql);
        try {
            int count = 0;
            JDBCServer.execute(rs, this);
            boolean isFirst = true;
            int loc = rs.getFields().size() - 1;
            int totalWidth = 0;
            for (MDSALColumn c : rs.getFields()) {
                if (isFirst) {
                    isFirst = false;
                    if (toCsv) {
                        out.print("\"");
                    }
                }

                if (!toCsv) {
                    out.print("|");
                }

                out.print(c.getColumnName());

                if (!toCsv) {
                    int cw = c.getCharWidth();
                    int cnw = c.getColumnName().length();
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
                for (MDSALColumn c : rs.getFields()) {
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
        private MDSALTable mainTable = null;
        private MDSALDatabase db = null;

        public NETask(JDBCResultSet _rs, MDSALTable _main, MDSALDatabase _db) {
            this.rs = _rs;
            this.mainTable = _main;
            this.db = _db;
        }

        public void run() {
            for (int i = rs.fromIndex; i < rs.toIndex; i++) {
                Object rec = db.read(mainTable.getMyClass(), i);
                rs.addRecords(rec, mainTable, true,db);
            }
            synchronized (rs) {
                rs.numberOfTasks--;
                if (rs.numberOfTasks == 0) {
                    rs.setFinished(true);
                    rs.notifyAll();
                }
            }
        }
    }

    public void execute(JDBCResultSet rs) {
        MDSALTable table = rs.getMainTable();
        NETask task = new NETask(rs, table, this);
        rs.numberOfTasks = 1;
        threadpool.addTask(task);
    }

    public void commit() {
        for (MDSALBlockDataLocation bl : this.location.values()) {
            bl.save();
        }
    }

    public void close() {
        this.closed = true;
        for (MDSALBlockDataLocation bl : this.location.values()) {
            bl.close();
        }
        this.jdbcServer.close();
    }

    public boolean isClosed() {
        return this.closed;
    }
}
