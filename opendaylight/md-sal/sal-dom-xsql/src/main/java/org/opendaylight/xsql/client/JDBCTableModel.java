package org.opendaylight.xsql.client;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

public class JDBCTableModel implements TableModel {

    private String columns[] = null;
    private List<String[]> rows = new ArrayList<String[]>();

    public JDBCTableModel(ResultSet rs) {
        try {
            columns = new String[rs.getMetaData().getColumnCount()];
            for (int i = 1; i <= columns.length; i++) {
                columns[i - 1] = rs.getMetaData().getColumnLabel(i).substring(rs.getMetaData().getColumnLabel(i).lastIndexOf(".")+1);
            }
            while (rs.next()) {
                String[] row = new String[columns.length];
                for (int i = 0; i < row.length; i++) {
                    Object value = rs.getObject(i + 1);
                    if (value != null)
                        row[i] = value.toString();
                    else
                        value = "";
                }
                rows.add(row);
            }
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    @Override
    public void addTableModelListener(TableModelListener l) {
        // TODO Auto-generated method stub

    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    @Override
    public int getColumnCount() {
        return columns.length;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return columns[columnIndex];
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return rows.get(rowIndex)[columnIndex];
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void removeTableModelListener(TableModelListener l) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        // TODO Auto-generated method stub

    }
}
