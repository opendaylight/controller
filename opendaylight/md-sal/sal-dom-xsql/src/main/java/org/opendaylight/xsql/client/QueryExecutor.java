package org.opendaylight.xsql.client;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class QueryExecutor extends JPanel implements KeyListener {
    private JTextField host = new JTextField("127.0.0.1");
    private JTextArea query = new JTextArea();
    private JTable result = new JTable();
    private JFrame frame = null;
    private Set<String> qTables = new HashSet<String>();

    public QueryExecutor(JFrame _frame) {
        this.frame = _frame;
        this.setLayout(new BorderLayout());
        this.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        JPanel top1 = new JPanel(new BorderLayout());
        top1.add(host, BorderLayout.CENTER);
        top1.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Host"));
        JPanel top2 = new JPanel(new BorderLayout());
        query.setPreferredSize(new Dimension(0, 50));
        top2.add(new JScrollPane(query), BorderLayout.CENTER);
        top2.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Query"));
        JPanel top = new JPanel(new BorderLayout());
        top.add(top1, BorderLayout.NORTH);
        top.add(top2, BorderLayout.CENTER);
        this.add(top, BorderLayout.NORTH);
        JPanel results = new JPanel(new BorderLayout());
        results.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Query Results"));
        results.add(new JScrollPane(result), BorderLayout.CENTER);
        this.add(results, BorderLayout.CENTER);
        query.addKeyListener(this);
    }

    public void go() {
        Connection c = getConnection(host.getText());
        Statement st = null;
        ResultSet rs = null;

        try {
            // Creste a JDBC statement
            st = c.createStatement();
            // Define the xsql query to execute
            String xsql = this.query.getText();
            // Execute the xsql query
            rs = st.executeQuery(xsql);
            JDBCTableModel model = new JDBCTableModel(rs);
            this.result.setModel(model);
        } catch (Exception err) {
            JOptionPane.showMessageDialog(this, err.getMessage(),"Query Execution Error",JOptionPane.ERROR_MESSAGE);
            err.printStackTrace();
        } finally {
            closeAll(rs, st, c);
        }
    }

    public static void main(String args[]) {
        JFrame f = new JFrame();
        f.getContentPane().setLayout(new BorderLayout());
        QueryExecutor q = new QueryExecutor(f);
        f.getContentPane().add(q, BorderLayout.CENTER);
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        f.setSize(d.getSize().width,d.getSize().height-50);
        f.setTitle("Data Broker Query Executor");
        f.addWindowListener(new WindowListener() {

            @Override
            public void windowOpened(WindowEvent arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void windowIconified(WindowEvent arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void windowDeiconified(WindowEvent arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void windowDeactivated(WindowEvent arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void windowClosing(WindowEvent arg0) {
                System.exit(0);
            }

            @Override
            public void windowClosed(WindowEvent arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void windowActivated(WindowEvent arg0) {
                // TODO Auto-generated method stub

            }
        });
        f.setVisible(true);
    }

    @Override
    public void keyPressed(KeyEvent arg0) {
        if (arg0.getKeyChar() == '.') {
            Connection c = getConnection(host.getText());
            if(c==null){
                JOptionPane.showMessageDialog(this, "Failed to connecto to "+host.getText()+", please check that ODL is up, XSQL module is loaded and firewall is allowing port 34343","Connection Failed",JOptionPane.ERROR_MESSAGE);
                return;
            }
            String str = query.getText();
            int pos = query.getCaretPosition();
            String str1 = str.substring(0, pos);
            String str2 = str.substring(pos);
            int lastIndex = str1.lastIndexOf(" ");
            if (str1.lastIndexOf(",") > lastIndex)
                lastIndex = str1.lastIndexOf(",")+1;
            String tableName = str1.substring(lastIndex).trim();
            int caret = query.getCaret().getDot();
            FontMetrics fm = query.getFontMetrics(query.getFont());
            int x = fm.stringWidth(query.getText().substring(0,query.getCaretPosition()))+8+frame.getLocation().x;
            int y = getYLocation(query)+fm.getHeight();
            FieldSelector selector = new FieldSelector(frame, c, tableName,x,y);
            query.setText(str1.trim() + "." + selector.getField() + str2);
            query.setCaretPosition(str1.length() + selector.getField().length() + 1);
            try {
                c.close();
            } catch (Exception err) {
            }
        } else if (arg0.isControlDown() && arg0.getKeyChar() == ' ') {
            String str = query.getText();
            Connection c = getConnection(host.getText());
            if(c==null){
                JOptionPane.showMessageDialog(this, "Failed to connecto to "+host.getText()+", please check that ODL is up, XSQL module is loaded and firewall is allowing port 34343","Connection Failed",JOptionPane.ERROR_MESSAGE);
                return;
            }
            int pos = query.getCaretPosition();
            String str1 = str.substring(0, pos);
            String str2 = str.substring(pos);
            int lastIndex = str1.lastIndexOf(" ");
            if (str1.lastIndexOf(",") > lastIndex)
                lastIndex = str1.lastIndexOf(",")+1;
            if(lastIndex==-1)
                lastIndex = 0;
            String subset = str1.substring(lastIndex).trim();
            int caret = query.getCaret().getDot();
            FontMetrics fm = query.getFontMetrics(query.getFont());
            int x = fm.stringWidth(query.getText().substring(0,query.getCaretPosition()))+8+frame.getLocation().x;
            int y = getYLocation(query)+fm.getHeight();
            TableSelector selector = new TableSelector(frame, c, subset,x,y);
            query.setText(str1.substring(0, str1.length() - subset.length())+ selector.getTable() + str2);
            query.setCaretPosition(str1.substring(0, str1.length() - subset.length()).length() + selector.getTable().length());
            qTables.add(selector.getTable().trim());
            try {
                c.close();
            } catch (Exception err) {
            }
        }
    }

    private int getYLocation(Component c){
        int result = c.getLocation().y;
        while(c.getParent()!=null){
            result+=c.getParent().getLocation().y;
            c = c.getParent();
        }
        return result;
    }

    @Override
    public void keyReleased(KeyEvent arg0) {
        if (arg0.getKeyCode() == KeyEvent.VK_ENTER) {
            go();
        } else if (arg0.getKeyChar() == 'm'
                && query.getText().toLowerCase().trim().endsWith("from")) {
            StringBuffer buff = new StringBuffer();
            boolean isFirst = true;
            for (String t : qTables) {
                if(query.getText().indexOf(t)!=-1){
                    buff.append(" ");
                    if (!isFirst)
                        buff.append(",");
                    buff.append(t);
                    isFirst = false;
                }
            }
            query.setText(query.getText() + buff.toString());
            query.setCaretPosition(query.getText().length());
        }
    }

    @Override
    public void keyTyped(KeyEvent arg0) {
        // TODO Auto-generated method stub

    }

    public static void center(Component c) {
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        c.setLocation((d.width - c.getSize().width) / 2,
                (d.height - c.getSize().height) / 2);
    }

    private static Connection getConnection(String host){
        try{
            Class.forName("org.odl.xsql.JDBCDriver");
            return DriverManager.getConnection(host);
        }catch(Exception err){
            err.printStackTrace();
        }
        return null;
    }

    public static void closeAll(ResultSet rs,Statement st,Connection c){
        if (rs != null)try {rs.close();} catch (Exception err) {}
        if (st != null)try {st.close();} catch (Exception err) {}
        if (c != null)try {c.close();} catch (Exception err) {}
    }
}
