package org.example.component;

import org.example.CardService;
import org.example.database.DAO;
import org.example.util.EnvKeyLoader;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class ParkingSessionHistoryPanel extends BasePanel {

    private JTable tblParkingHistory;
    private DefaultTableModel parkingHistoryModel;
    private JLabel lblParkingHistoryOwner;

    public ParkingSessionHistoryPanel(CardService cardService, DAO cardDao, EnvKeyLoader keyManager,
                                      StatusListener statusListener) {
        super(cardService, cardDao, keyManager, statusListener);
        setLayout(new GridBagLayout());
        setBorder(new EmptyBorder(30, 30, 30, 30));

        initUI();
    }

    private void initUI() {
        // 2. Card chứa nội dung (Nền trắng, bo góc - dùng hàm helper có sẵn)
        JPanel card = createCard("Nhật ký Ra/Vào Bãi xe");
        card.setLayout(new BorderLayout(0, 15)); // Khoảng cách dọc giữa các phần tử là 15px

        // --- PHẦN TRÊN (TOP): Nút bấm và Thông tin chủ thẻ ---
        JPanel topPanel = new JPanel(new BorderLayout(15, 0));
        topPanel.setOpaque(false);

        // Nút "Quẹt thẻ xem nhật ký"
        JButton btnCheckSessions = createWarningButton("Quẹt thẻ xem nhật ký");
        btnCheckSessions.setPreferredSize(new Dimension(220, 45));
        btnCheckSessions.setFont(new Font("Segoe UI", Font.BOLD, 14));

        // Gắn sự kiện: Gọi hàm logicViewParkingHistory (đã viết ở bước trước)
        btnCheckSessions.addActionListener(e -> logicViewParkingHistory());

        topPanel.add(btnCheckSessions, BorderLayout.WEST);

        // Label hiển thị tên người dùng sau khi quẹt
        lblParkingHistoryOwner = new JLabel("Vui lòng quẹt thẻ để xem dữ liệu...", SwingConstants.LEFT);
        lblParkingHistoryOwner.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        lblParkingHistoryOwner.setForeground(TEXT_PRIMARY);
        topPanel.add(lblParkingHistoryOwner, BorderLayout.CENTER);

        card.add(topPanel, BorderLayout.NORTH);

        // --- PHẦN GIỮA (CENTER): Bảng dữ liệu ---

        // Định nghĩa cột
        String[] columns = { "Giờ vào", "Giờ ra", "Biển số xe", "Phí gửi", "Trạng thái" };

        // Khởi tạo Model (Không cho phép sửa dữ liệu trực tiếp trên bảng)
        parkingHistoryModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblParkingHistory = new JTable(parkingHistoryModel);

        // Áp dụng Style chung cho bảng (Hàm styleTable bạn đã có)
        styleTable(tblParkingHistory);

        // --- Tinh chỉnh hiển thị cột (Căn lề) ---
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);

        // Áp dụng renderer
        tblParkingHistory.getColumnModel().getColumn(0).setCellRenderer(centerRenderer); // Giờ vào
        tblParkingHistory.getColumnModel().getColumn(1).setCellRenderer(centerRenderer); // Giờ ra
        tblParkingHistory.getColumnModel().getColumn(2).setCellRenderer(centerRenderer); // Biển số (Căn giữa cho đẹp)
        tblParkingHistory.getColumnModel().getColumn(3).setCellRenderer(rightRenderer); // Phí gửi (Số tiền căn phải)
        tblParkingHistory.getColumnModel().getColumn(4).setCellRenderer(centerRenderer); // Trạng thái

        // Đặt bảng vào ScrollPane (để có thanh cuộn)
        JScrollPane scrollPane = new JScrollPane(tblParkingHistory);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));

        card.add(scrollPane, BorderLayout.CENTER);

        // --- KẾT THÚC: Add Card vào MainPanel ---
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH; // Giãn đều cả 2 chiều
        gbc.weightx = 1.0; // Chiếm hết chiều ngang
        gbc.weighty = 1.0; // Chiếm hết chiều dọc
        add(card, gbc);
    }

    private void logicViewParkingHistory() {
        // 1. Reset giao diện: Xóa dữ liệu bảng cũ và cập nhật trạng thái
        parkingHistoryModel.setRowCount(0);
        lblParkingHistoryOwner.setText("Đang đọc dữ liệu...");
        lblParkingHistoryOwner.setForeground(Color.BLUE);

        // 2. Thực hiện bắt tay với thẻ (Kết nối -> Lấy UID -> Xác thực chữ ký)
        // Hàm performCardHandshake trả về UID nếu thẻ xịn, null nếu lỗi/giả
        boolean connect = performCardHandshake();
        if (!connect) {
            lblParkingHistoryOwner.setText("Lỗi đọc thẻ hoặc thẻ không hợp lệ!");
            lblParkingHistoryOwner.setForeground(DANGER_COLOR);
            return;
        }

        // We reuse cardService from BasePanel
        String cardUID = cardService.getCardID();

        try {
            // Bắt đầu xử lý nặng -> Đổi con trỏ chuột sang hình đồng hồ cát
            this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            // 3. Lấy thông tin định danh chủ thẻ từ Database (để hiển thị tên cho đẹp)
            // Giả sử hàm trả về mảng: [Tên, SĐT, Biển số, Loại xe]
            String[] info = cardDao.getCardInfoByUID(cardUID);

            if (info != null) {
                String ownerName = info[0];
                String licensePlate = info[2];
                // Sử dụng HTML để format màu chữ
                lblParkingHistoryOwner.setText("<html>Nhật ký xe của: <b style='color:#27ae60'>" + ownerName
                        + "</b> - Biển số: <b>" + licensePlate + "</b></html>");
                lblParkingHistoryOwner.setForeground(TEXT_PRIMARY);
            } else {
                lblParkingHistoryOwner.setText("Thẻ hợp lệ nhưng chưa có thông tin chủ xe (UID: " + cardUID + ")");
                lblParkingHistoryOwner.setForeground(Color.DARK_GRAY);
            }

            // 4. Truy vấn danh sách lịch sử ra vào từ DAO
            // Đảm bảo bạn đã thêm hàm getParkingSessionHistory vào CardHolderDAO
            List<String[]> sessions = cardDao.getParkingSessionHistory(cardUID);

            if (sessions == null || sessions.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Thẻ này chưa có lịch sử gửi xe nào!", "Thông báo",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                // 5. Đổ dữ liệu vào bảng
                for (String[] row : sessions) {
                    parkingHistoryModel.addRow(row);
                }

                // Hiển thị thông báo nhỏ ở góc hoặc log
                System.out.println("Đã tải " + sessions.size() + " dòng lịch sử.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            showError("Lỗi truy vấn lịch sử: " + e.getMessage());
            lblParkingHistoryOwner.setText("Lỗi hệ thống khi tải dữ liệu");
            lblParkingHistoryOwner.setForeground(DANGER_COLOR);
        } finally {
            // 6. Kết thúc: Trả lại con trỏ chuột và ngắt kết nối thẻ
            this.setCursor(Cursor.getDefaultCursor());
            cardService.disconnect();
        }
    }
}
