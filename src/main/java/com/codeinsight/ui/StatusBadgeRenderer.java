package com.codeinsight.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/**
 * Custom table cell renderer that draws status values as colored badges.
 */
public class StatusBadgeRenderer extends DefaultTableCellRenderer {
    private static final Color OK_BG = new Color(220, 252, 231);
    private static final Color OK_FG = new Color(22, 101, 52);
    private static final Color REVIEW_BG = new Color(254, 226, 226);
    private static final Color REVIEW_FG = new Color(153, 27, 27);

    public StatusBadgeRenderer() {
        setHorizontalAlignment(CENTER);
    }

    @Override
    public Component getTableCellRendererComponent(
            JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (value != null) {
            String status = value.toString();
            if ("REVIEW".equalsIgnoreCase(status)) {
                c.setBackground(REVIEW_BG);
                c.setForeground(REVIEW_FG);
                setFont(getFont().deriveFont(Font.BOLD));
            } else if ("OK".equalsIgnoreCase(status)) {
                c.setBackground(OK_BG);
                c.setForeground(OK_FG);
                setFont(getFont().deriveFont(Font.BOLD));
            } else {
                c.setBackground(table.getBackground());
                c.setForeground(table.getForeground());
            }
        }
        return c;
    }
}
