package org.example.component;

import org.example.CardService;
import org.example.database.CardHolderDAO;
import org.example.util.EnvKeyLoader;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class HistoryPanel extends BasePanel {

    private JTable tblHistory;
    private DefaultTableModel historyModel;
    private JLabel lblHistoryOwner;

    public HistoryPanel(CardService cardService, CardHolderDAO cardDao, EnvKeyLoader keyManager,
            StatusListener statusListener) {
        super(cardService, cardDao, keyManager, statusListener);
        setLayout(new GridBagLayout());
        setBorder(new EmptyBorder(30, 30, 30, 30));

        initUI();
    }

    private void initUI() {
        JPanel card = createCard("Tra cứu Lịch sử Giao dịch");
        card.setLayout(new BorderLayout(0, 15));

        // --- 1. PANEL TOP: Nút Quẹt thẻ & Thông tin chủ thẻ ---
        JPanel topPanel = new JPanel(new BorderLayout(15, 0));
        topPanel.setOpaque(false);

        JButton btnCheckHistory = createWarningButton("Quẹt thẻ xem lịch sử");
        btnCheckHistory.setPreferredSize(new Dimension(200, 45));
        btnCheckHistory.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnCheckHistory.addActionListener(e -> logicViewHistory());

        topPanel.add(btnCheckHistory, BorderLayout.WEST);

        lblHistoryOwner = new JLabel("Vui lòng quẹt thẻ...", SwingConstants.LEFT);
        lblHistoryOwner.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        lblHistoryOwner.setForeground(TEXT_PRIMARY);
        topPanel.add(lblHistoryOwner, BorderLayout.CENTER);

        card.add(topPanel, BorderLayout.NORTH);

        // --- 2. PANEL CENTER: Bảng dữ liệu ---
        String[] columns = { "Thời gian", "Loại giao dịch", "Số tiền (VNĐ)", "Trạng thái" };
        historyModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Không cho sửa
            }
        };

        tblHistory = new JTable(historyModel);
        styleTable(tblHistory);

        // Tùy chỉnh căn lề cho các cột
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);

        tblHistory.getColumnModel().getColumn(0).setCellRenderer(centerRenderer); // Thời gian
        tblHistory.getColumnModel().getColumn(1).setCellRenderer(centerRenderer); // Loại
        tblHistory.getColumnModel().getColumn(2).setCellRenderer(rightRenderer); // Tiền
        tblHistory.getColumnModel().getColumn(3).setCellRenderer(centerRenderer); // Trạng thái

        JScrollPane scrollPane = new JScrollPane(tblHistory);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
        card.add(scrollPane, BorderLayout.CENTER);

        // Add card vào main panel để căn giữa đẹp
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        add(card, gbc);
    }

    private void logicViewHistory() {
        historyModel.setRowCount(0);
        lblHistoryOwner.setForeground(Color.BLUE);

        boolean connect = performCardHandshake();
        if (!connect) {
            return;
        }
        try {
            this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            String cardUID = cardService.getCardID();
            String[] info = cardDao.getCardInfoByUID(cardUID);

            if (info != null) {
                String ownerName = info[0];
                String license = info[2];
                lblHistoryOwner.setText("<html>Lịch sử của: <b style='color:#27ae60'>" + ownerName
                        + "</b> - Biển số: <b>" + license + "</b></html>");
                lblHistoryOwner.setForeground(TEXT_PRIMARY);
            } else {
                lblHistoryOwner.setText("Thẻ chưa định danh: " + cardUID);
            }
            java.util.List<String[]> logs = cardDao.getTransactionHistory(cardUID);

            if (logs.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Thẻ này chưa có giao dịch nào!", "Thông báo",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                for (String[] row : logs) {
                    historyModel.addRow(row);
                }
                showSuccess("Đã tải " + logs.size() + " giao dịch.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Lỗi truy vấn lịch sử: " + e.getMessage());
        } finally {
            this.setCursor(Cursor.getDefaultCursor());
            cardService.disconnect();
        }
    }
}
