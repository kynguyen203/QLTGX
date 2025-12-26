package org.example;

import org.example.database.CardHolderDAO;
import org.example.database.DTO.UserDTO;
import org.example.util.EnvKeyLoader;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

public class ModernParkingCardUI extends JFrame {

    private static final Color PRIMARY_COLOR = new Color(41, 128, 185);
    private static final Color ACCENT_COLOR = new Color(52, 152, 219);
    private static final Color SUCCESS_COLOR = new Color(46, 204, 113);
    private static final Color WARNING_COLOR = new Color(241, 196, 15);
    private static final Color DANGER_COLOR = new Color(231, 76, 60);
    private static final Color LIGHT_BG = new Color(236, 240, 241);
    private static final Color CARD_BG = Color.WHITE;
    private static final Color TEXT_PRIMARY = new Color(44, 62, 80);

    private JTextField txtOwnerName, txtPhone, txtIdentityCard, txtLicensePlate;
    private JComboBox<String> comboVehicleType;
    private JPasswordField txtNewPin, txtConfirmPin, txtPin;
    private JTextField txtTopUpAmount;
    private JLabel lblTopUpCardInfo;
    private JLabel lblAvatar;
    private File selectedAvatarFile;
    private JLabel lblPin;
    private JTable parkingTable;
    private DefaultTableModel parkingTableModel;
    private JLabel lblExitInfoBienSo, lblExitInfoVao, lblExitInfoRa, lblExitInfoTongTien;
    private JLabel lblCardStatus, lblBalance;
    private JButton btnCheckCard;
    private JButton btnSave;
    private JButton btnChangePin;
    private JButton btnTopUp;
    private JButton btnViewInfo;

    private JTable tblHistory;
    private DefaultTableModel historyModel;
    private JLabel lblHistoryOwner;

    private JTable tblParkingHistory;
    private DefaultTableModel parkingHistoryModel;
    private JLabel lblParkingHistoryOwner;

    private CardService cardService;
    CardHolderDAO cardDao;
    EnvKeyLoader keyManager;
    public ModernParkingCardUI() {

        setTitle("Hệ thống Quản lý Thẻ Gửi Xe Thông Minh");
        setSize(1200, 1050);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(LIGHT_BG);
        add(createHeader(), BorderLayout.NORTH);

        JTabbedPane tabbedPane = createStyledTabbedPane();
        tabbedPane.addTab("  Quản lý Thẻ  ", createManagementPanel());
        tabbedPane.addTab("  Nạp Tiền  ", createTopUpPanel());
        tabbedPane.addTab("  Bãi Xe  ", createParkingPanel());
        tabbedPane.addTab("  Lịch sử giao dịch  ", createHistoryPanel());
        tabbedPane.addTab(" Lịch sử ra vào ", createParkingSessionHistoryPanel());
        add(tabbedPane, BorderLayout.CENTER);

        add(createStatusBar(), BorderLayout.SOUTH);

        cardService = new CardService();
        cardDao = new CardHolderDAO();
        keyManager = new EnvKeyLoader();
        loadParkingData();
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(PRIMARY_COLOR);
        header.setPreferredSize(new Dimension(0, 80));
        header.setBorder(new EmptyBorder(15, 25, 15, 25));

        JLabel lblTitle = new JLabel("QUẢN LÝ THẺ GỬI XE THÔNG MINH");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTitle.setForeground(Color.WHITE);
        header.add(lblTitle, BorderLayout.WEST);

        JPanel cardInfoPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        cardInfoPanel.setOpaque(false);

        lblCardStatus = new JLabel("● Thẻ: Chưa kết nối");
        lblCardStatus.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblCardStatus.setForeground(Color.WHITE);

        lblBalance = new JLabel("Số dư: " + formatCurrency(0));
        lblBalance.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblBalance.setForeground(WARNING_COLOR);

        cardInfoPanel.add(lblCardStatus);
        cardInfoPanel.add(lblBalance);
        header.add(cardInfoPanel, BorderLayout.EAST);

        return header;
    }

    private JTabbedPane createStyledTabbedPane() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tabbedPane.setBackground(CARD_BG);
        tabbedPane.setBorder(new EmptyBorder(10, 10, 10, 10));
        return tabbedPane;
    }

    private JPanel createManagementPanel() {
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(LIGHT_BG);
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 0, 15, 0);

        // 1. Panel Đăng ký
        mainPanel.add(createRegisterCard(), gbc);

        // 2. Panel Đổi PIN
        gbc.gridy = 1;
        gbc.weighty = 0;
        mainPanel.add(createChangePinCard(), gbc);

        // 3. Panel Mở khóa thẻ (Unblock)
        gbc.gridy = 2;
        gbc.weighty = 0;
        mainPanel.add(createUnblockCardPanel(), gbc);

        // Spacer
        gbc.gridy = 3;
        gbc.weighty = 1.0;
        mainPanel.add(Box.createVerticalGlue(), gbc);

        return mainPanel;
    }

    private JPanel createHistoryPanel() {
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(LIGHT_BG);
        mainPanel.setBorder(new EmptyBorder(30, 30, 30, 30));

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
        String[] columns = {"Thời gian", "Loại giao dịch", "Số tiền (VNĐ)", "Trạng thái"};
        historyModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Không cho sửa
            }
        };

        tblHistory = new JTable(historyModel);
        styleTable(tblHistory); // Tái sử dụng hàm styleTable cũ của bạn (nếu có), hoặc xem code dưới

        // Tùy chỉnh căn lề cho các cột
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);

        tblHistory.getColumnModel().getColumn(0).setCellRenderer(centerRenderer); // Thời gian
        tblHistory.getColumnModel().getColumn(1).setCellRenderer(centerRenderer); // Loại
        tblHistory.getColumnModel().getColumn(2).setCellRenderer(rightRenderer);  // Tiền
        tblHistory.getColumnModel().getColumn(3).setCellRenderer(centerRenderer); // Trạng thái

        JScrollPane scrollPane = new JScrollPane(tblHistory);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
        card.add(scrollPane, BorderLayout.CENTER);

        // Add card vào main panel để căn giữa đẹp
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        mainPanel.add(card, gbc);

        return mainPanel;
    }
    private JPanel createParkingSessionHistoryPanel() {
        // 1. Panel chính (Nền xám nhạt, căn giữa nội dung)
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(LIGHT_BG);
        mainPanel.setBorder(new EmptyBorder(30, 30, 30, 30));

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
        String[] columns = {"Giờ vào", "Giờ ra", "Biển số xe", "Phí gửi", "Trạng thái"};

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
        tblParkingHistory.getColumnModel().getColumn(3).setCellRenderer(rightRenderer);  // Phí gửi (Số tiền căn phải)
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
        mainPanel.add(card, gbc);

        return mainPanel;
    }
    private enum FormMode { REGISTER_MODE, UPDATE_MODE }
    private FormMode currentMode = FormMode.REGISTER_MODE;

//    private JPanel createRegisterCard() {
//        JPanel card = createCard("Quản lý Thông tin Thẻ");
//        card.setLayout(new GridBagLayout());
//        GridBagConstraints gbc = new GridBagConstraints();
//        gbc.insets = new Insets(5, 10, 5, 10);
//        gbc.fill = GridBagConstraints.HORIZONTAL;
//
//        JPanel infoPanel = new JPanel(new GridBagLayout());
//        infoPanel.setOpaque(false);
//        GridBagConstraints gbcInfo = new GridBagConstraints();
//        gbcInfo.insets = new Insets(5, 5, 5, 5);
//        gbcInfo.fill = GridBagConstraints.HORIZONTAL;
//
//        JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
//        modePanel.setOpaque(false);
//        JRadioButton rbRegister = new JRadioButton("Đăng ký mới");
//        JRadioButton rbUpdate = new JRadioButton("Xem / Cập nhật");
//        rbRegister.setFont(new Font("Segoe UI", Font.BOLD, 14));
//        rbUpdate.setFont(new Font("Segoe UI", Font.BOLD, 14));
//        rbRegister.setOpaque(false);
//        rbUpdate.setOpaque(false);
//
//        ButtonGroup modeGroup = new ButtonGroup();
//        modeGroup.add(rbRegister);
//        modeGroup.add(rbUpdate);
//        rbRegister.setSelected(true);
//
//        modePanel.add(rbRegister);
//        modePanel.add(Box.createHorizontalStrut(20));
//        modePanel.add(rbUpdate);
//
//        gbcInfo.gridx = 0; gbcInfo.gridy = 0; gbcInfo.gridwidth = 2;
//        infoPanel.add(modePanel, gbcInfo);
//
//        btnCheckCard = createWarningButton("Kiểm tra thẻ trắng");
//        gbcInfo.gridy = 1;
//        infoPanel.add(btnCheckCard, gbcInfo);
//
//        gbcInfo.gridwidth = 1;
//
//        gbcInfo.gridx = 0; gbcInfo.gridy = 2; gbcInfo.weightx = 0.3;
//        infoPanel.add(createLabel("Tên chủ xe:"), gbcInfo);
//        gbcInfo.gridx = 1; gbcInfo.weightx = 0.7;
//        txtOwnerName = createStyledTextField();
//        infoPanel.add(txtOwnerName, gbcInfo);
//
//        gbcInfo.gridx = 0; gbcInfo.gridy = 3;
//        infoPanel.add(createLabel("Số điện thoại:"), gbcInfo);
//        gbcInfo.gridx = 1;
//        txtPhone = createStyledTextField();
//        infoPanel.add(txtPhone, gbcInfo);
//
//        gbcInfo.gridx = 0; gbcInfo.gridy = 4;
//        infoPanel.add(createLabel("CCCD/CMND:"), gbcInfo);
//        gbcInfo.gridx = 1;
//        txtIdentityCard = createStyledTextField();
//        infoPanel.add(txtIdentityCard, gbcInfo);
//
//        gbcInfo.gridx = 0; gbcInfo.gridy = 5;
//        infoPanel.add(createLabel("Biển số xe:"), gbcInfo);
//        gbcInfo.gridx = 1;
//        txtLicensePlate = createStyledTextField();
//        infoPanel.add(txtLicensePlate, gbcInfo);
//
//        gbcInfo.gridx = 0; gbcInfo.gridy = 6;
//        infoPanel.add(createLabel("Loại xe:"), gbcInfo);
//        gbcInfo.gridx = 1;
//        comboVehicleType = new JComboBox<>(new String[]{"Xe máy", "Ô tô", "Xe đạp"});
//        comboVehicleType.setFont(new Font("Segoe UI", Font.PLAIN, 14));
//        comboVehicleType.setPreferredSize(new Dimension(0, 35));
//        infoPanel.add(comboVehicleType, gbcInfo);
//
//        gbcInfo.gridx = 0; gbcInfo.gridy = 7;
//        lblPin = createLabel("Mã PIN:");
//        infoPanel.add(lblPin, gbcInfo);
//        gbcInfo.gridx = 1;
//        txtPin = createStyledPasswordField();
//        infoPanel.add(txtPin, gbcInfo);
//
//        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.65;
//        card.add(infoPanel, gbc);
//
//        JPanel avatarPanel = new JPanel(new BorderLayout(0, 5));
//        avatarPanel.setOpaque(false);
//        avatarPanel.setBorder(new TitledBorder("Ảnh Chủ xe"));
//
//        lblAvatar = new JLabel("Chưa có ảnh", SwingConstants.CENTER);
//        lblAvatar.setPreferredSize(new Dimension(140, 180));
//        lblAvatar.setBorder(BorderFactory.createLineBorder(Color.GRAY));
//        lblAvatar.setOpaque(true);
//        lblAvatar.setBackground(Color.WHITE);
//
//        JButton btnSelectImage = createSecondaryButton("Chọn ảnh...");
//        btnSelectImage.addActionListener(e -> logicSelectImage());
//
//        avatarPanel.add(lblAvatar, BorderLayout.CENTER);
//        avatarPanel.add(btnSelectImage, BorderLayout.SOUTH);
//
//        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.35;
//        gbc.anchor = GridBagConstraints.NORTH;
//        card.add(avatarPanel, gbc);
//
//        btnSave = createPrimaryButton("Đăng ký");
//        btnSave.setPreferredSize(new Dimension(200, 45));
//        btnSave.setEnabled(false);
//
//        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
//        buttonPanel.setOpaque(false);
//        buttonPanel.add(btnSave);
//
//        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
//        gbc.anchor = GridBagConstraints.CENTER;
//        gbc.weighty = 1.0;
//        card.add(buttonPanel, gbc);
//
//        ActionListener modeListener = e -> {
//            resetForm();
//            selectedAvatarFile = null;
//            lblAvatar.setIcon(null);
//            lblAvatar.setText("Chưa có ảnh");
//            btnSave.setEnabled(false);
//
//            if (rbRegister.isSelected()) {
//                currentMode = FormMode.REGISTER_MODE;
//                btnCheckCard.setText("Quẹt thẻ");
//                btnSave.setText("Đăng ký");
//                lblPin.setVisible(true);
//                txtPin.setVisible(true);
//                txtPin.setText("");
//            } else {
//                currentMode = FormMode.UPDATE_MODE;
//                btnCheckCard.setText("Đọc thông tin từ thẻ");
//                btnSave.setText("Lưu thay đổi");
//                lblPin.setVisible(false);
//                txtPin.setVisible(false);
//                txtPin.setText("");
//            }
//        };
//        rbRegister.addActionListener(modeListener);
//        rbUpdate.addActionListener(modeListener);
//
//        btnCheckCard.addActionListener(e -> {
//            if (currentMode == FormMode.REGISTER_MODE) {
//                performBlankCardCheck();
//            } else {
//                logicShowCardInfo();
//            }
//        });
//
//        btnSave.addActionListener(e -> {
//            if (currentMode == FormMode.REGISTER_MODE) {
//                logicRegister();
//            } else {
//                logicUpdateCardInfo();
//            }
//        });
//
//        return card;
//    }

    private JPanel createRegisterCard() {
        JPanel card = createCard("Quản lý Thông tin Thẻ");
        card.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // --- PANEL NHẬP LIỆU (Giữ nguyên) ---
        JPanel infoPanel = new JPanel(new GridBagLayout());
        infoPanel.setOpaque(false);
        GridBagConstraints gbcInfo = new GridBagConstraints();
        gbcInfo.insets = new Insets(5, 5, 5, 5);
        gbcInfo.fill = GridBagConstraints.HORIZONTAL;

        // Radio Buttons
        JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        modePanel.setOpaque(false);
        JRadioButton rbRegister = new JRadioButton("Đăng ký mới");
        JRadioButton rbUpdate = new JRadioButton("Xem / Cập nhật");
        rbRegister.setFont(new Font("Segoe UI", Font.BOLD, 14));
        rbUpdate.setFont(new Font("Segoe UI", Font.BOLD, 14));
        rbRegister.setOpaque(false);
        rbUpdate.setOpaque(false);

        ButtonGroup modeGroup = new ButtonGroup();
        modeGroup.add(rbRegister);
        modeGroup.add(rbUpdate);
        rbRegister.setSelected(true);

        modePanel.add(rbRegister);
        modePanel.add(Box.createHorizontalStrut(20));
        modePanel.add(rbUpdate);

        gbcInfo.gridx = 0; gbcInfo.gridy = 0; gbcInfo.gridwidth = 2;
        infoPanel.add(modePanel, gbcInfo);

        // Nút Quẹt thẻ (Top Button)
        btnCheckCard = createWarningButton("Kiểm tra thẻ trắng");
        gbcInfo.gridy = 1;
        infoPanel.add(btnCheckCard, gbcInfo);

        // Các trường nhập liệu (Giữ nguyên)
        gbcInfo.gridwidth = 1;
        gbcInfo.gridx = 0; gbcInfo.gridy = 2; gbcInfo.weightx = 0.3;
        infoPanel.add(createLabel("Tên chủ xe:"), gbcInfo);
        gbcInfo.gridx = 1; gbcInfo.weightx = 0.7;
        txtOwnerName = createStyledTextField();
        infoPanel.add(txtOwnerName, gbcInfo);

        gbcInfo.gridx = 0; gbcInfo.gridy = 3;
        infoPanel.add(createLabel("Số điện thoại:"), gbcInfo);
        gbcInfo.gridx = 1;
        txtPhone = createStyledTextField();
        infoPanel.add(txtPhone, gbcInfo);

        gbcInfo.gridx = 0; gbcInfo.gridy = 4;
        infoPanel.add(createLabel("CCCD/CMND:"), gbcInfo);
        gbcInfo.gridx = 1;
        txtIdentityCard = createStyledTextField();
        infoPanel.add(txtIdentityCard, gbcInfo);

        gbcInfo.gridx = 0; gbcInfo.gridy = 5;
        infoPanel.add(createLabel("Biển số xe:"), gbcInfo);
        gbcInfo.gridx = 1;
        txtLicensePlate = createStyledTextField();
        infoPanel.add(txtLicensePlate, gbcInfo);

        gbcInfo.gridx = 0; gbcInfo.gridy = 6;
        infoPanel.add(createLabel("Loại xe:"), gbcInfo);
        gbcInfo.gridx = 1;
        comboVehicleType = new JComboBox<>(new String[]{"Xe máy", "Ô tô", "Xe đạp"});
        comboVehicleType.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        comboVehicleType.setPreferredSize(new Dimension(0, 35));
        infoPanel.add(comboVehicleType, gbcInfo);

        gbcInfo.gridx = 0; gbcInfo.gridy = 7;
        lblPin = createLabel("Mã PIN:");
        infoPanel.add(lblPin, gbcInfo);
        gbcInfo.gridx = 1;
        txtPin = createStyledPasswordField();
        infoPanel.add(txtPin, gbcInfo);

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.65;
        card.add(infoPanel, gbc);

        // --- PANEL ẢNH (Giữ nguyên) ---
        JPanel avatarPanel = new JPanel(new BorderLayout(0, 5));
        avatarPanel.setOpaque(false);
        avatarPanel.setBorder(new TitledBorder("Ảnh Chủ xe"));

        lblAvatar = new JLabel("Chưa có ảnh", SwingConstants.CENTER);
        lblAvatar.setPreferredSize(new Dimension(140, 180));
        lblAvatar.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        lblAvatar.setOpaque(true);
        lblAvatar.setBackground(Color.WHITE);

        JButton btnSelectImage = createSecondaryButton("Chọn ảnh...");
        btnSelectImage.addActionListener(e -> logicSelectImage());

        avatarPanel.add(lblAvatar, BorderLayout.CENTER);
        avatarPanel.add(btnSelectImage, BorderLayout.SOUTH);

        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.35;
        gbc.anchor = GridBagConstraints.NORTH;
        card.add(avatarPanel, gbc);

        // --- BUTTON PANEL (SỬA ĐỔI QUAN TRỌNG) ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.setOpaque(false);

        // Nút Xem thông tin (Mới)
        btnViewInfo = createPrimaryButton("Xem thông tin");
        btnViewInfo.setPreferredSize(new Dimension(150, 45));
        btnViewInfo.setVisible(false); // Mặc định ẩn (chế độ đăng ký)

        // Nút Lưu/Đăng ký
        btnSave = createPrimaryButton("Đăng ký");
        btnSave.setPreferredSize(new Dimension(150, 45));
        btnSave.setEnabled(false);

        buttonPanel.add(btnViewInfo);
        buttonPanel.add(btnSave);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.weighty = 1.0;
        card.add(buttonPanel, gbc);

        // --- XỬ LÝ SỰ KIỆN ---

        // 1. Chuyển đổi chế độ
        ActionListener modeListener = e -> {
            resetForm();
            selectedAvatarFile = null;
            lblAvatar.setIcon(null);
            lblAvatar.setText("Chưa có ảnh");
            btnSave.setEnabled(false);
            btnViewInfo.setEnabled(false);

            if (rbRegister.isSelected()) {
                // Chế độ Đăng ký
                currentMode = FormMode.REGISTER_MODE;
                btnCheckCard.setText("Kiểm tra thẻ trắng");

                btnViewInfo.setVisible(false); // Ẩn nút xem

                btnSave.setText("Đăng ký");
                btnSave.setVisible(true);

                lblPin.setVisible(true);
                txtPin.setVisible(true);
            } else {
                // Chế độ Cập nhật
                currentMode = FormMode.UPDATE_MODE;
                btnCheckCard.setText("Quẹt thẻ (Kết nối)");

                btnViewInfo.setVisible(true); // Hiện nút xem
                btnViewInfo.setEnabled(false); // Chờ quẹt thẻ mới sáng

                btnSave.setText("Lưu thay đổi");
                btnSave.setVisible(true);

                lblPin.setVisible(false);
                txtPin.setVisible(false);
            }
        };
        rbRegister.addActionListener(modeListener);
        rbUpdate.addActionListener(modeListener);

        // 2. Logic nút "Quẹt thẻ" (Nút trên cùng)
        btnCheckCard.addActionListener(e -> {
            if (currentMode == FormMode.REGISTER_MODE) {
                performBlankCardCheck(); // Kiểm tra thẻ trắng
            } else {
                logicCheckCardExisting(); // Kiểm tra thẻ cũ (Hàm mới)
            }
        });

        // 3. Logic nút "Xem thông tin"
        btnViewInfo.addActionListener(e -> logicShowCardInfo());

        // 4. Logic nút "Lưu / Đăng ký"
        btnSave.addActionListener(e -> {
            if (currentMode == FormMode.REGISTER_MODE) {
                logicRegister();
            } else {
                logicUpdateCardInfo();
            }
        });

        return card;
    }

    private JPanel createChangePinCard() {
        JPanel card = createCard("Đổi mã PIN");
        card.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 0;gbc.weightx = 1.0;gbc.gridwidth = 2;
        JButton btnCheckCard = createWarningButton("Quẹt thẻ");
        btnCheckCard.addActionListener(e -> logicCheckCard());
        card.add(btnCheckCard, gbc);
        gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = 1;
        card.add(createLabel("PIN mới:"), gbc);
        gbc.gridx = 1;
        txtNewPin = createStyledPasswordField();
        card.add(txtNewPin, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        card.add(createLabel("Xác nhận PIN:"), gbc);
        gbc.gridx = 1;
        txtConfirmPin = createStyledPasswordField();
        card.add(txtConfirmPin, gbc);

        gbc.gridx = 1; gbc.gridy = 3; gbc.anchor = GridBagConstraints.EAST;
        btnChangePin = createWarningButton("Đổi PIN");
        btnChangePin.setEnabled(false);
        btnChangePin.addActionListener(e -> logicChangePin());
        card.add(btnChangePin, gbc);

        return card;
    }

    private JPanel createUnblockCardPanel() {
        JPanel card = createCard("Khôi phục / Mở khóa Thẻ");
        card.setLayout(new BorderLayout(15, 0));

        JLabel lblInfo = new JLabel("<html><i>Sử dụng khi thẻ bị khóa do đã nhập sai PIN quá 3 lần.<br>Cần mật khẩu (Personal Unblocking Key) để mở khóa.</i></html>");
        lblInfo.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        lblInfo.setForeground(TEXT_PRIMARY);
        card.add(lblInfo, BorderLayout.CENTER);

        JButton btnUnblock = new JButton("Mở khóa Thẻ");
        btnUnblock.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnUnblock.setBackground(new Color(155, 89, 182));
        btnUnblock.setForeground(Color.BLUE);
        btnUnblock.setPreferredSize(new Dimension(150, 40));
        btnUnblock.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btnUnblock.addActionListener(e -> logicUnblockCard());

        card.add(btnUnblock, BorderLayout.EAST);
        return card;
    }

    private JPanel createTopUpPanel() {
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(LIGHT_BG);
        mainPanel.setBorder(new EmptyBorder(50, 50, 50, 50));

        JPanel card = createCard("Nạp tiền vào thẻ");
        card.setLayout(new GridBagLayout());
        card.setPreferredSize(new Dimension(500, 750));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 20, 15, 20);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        JButton btnCheckCard = createWarningButton("Quẹt thẻ");
        btnCheckCard.addActionListener(e -> logicCheckCardForTopUp());
        card.add(btnCheckCard, gbc);

        gbc.gridy = 1;
        lblTopUpCardInfo = new JLabel("Vui lòng quẹt thẻ...", SwingConstants.CENTER);
        lblTopUpCardInfo.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        lblTopUpCardInfo.setPreferredSize(new Dimension(450, 250));
        lblTopUpCardInfo.setForeground(Color.GRAY);
        card.add(lblTopUpCardInfo, gbc);

        gbc.gridy = 2;
        JLabel lblAmount = new JLabel("Số tiền nạp (VNĐ):");
        lblAmount.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblAmount.setForeground(TEXT_PRIMARY);
        card.add(lblAmount, gbc);

        gbc.gridy = 3;
        txtTopUpAmount = createStyledTextField();
        txtTopUpAmount.setFont(new Font("Segoe UI", Font.BOLD, 20));
        txtTopUpAmount.setPreferredSize(new Dimension(0, 50));
        card.add(txtTopUpAmount, gbc);

        gbc.gridy = 4;
        JPanel quickPanel = new JPanel(new GridLayout(1, 4, 10, 0));
        quickPanel.setOpaque(false);
        String[] amounts = {"50,000", "100,000", "200,000", "500,000"};
        for (String amount : amounts) {
            JButton btn = createSecondaryButton(amount);
            btn.addActionListener(e -> txtTopUpAmount.setText(amount.replace(",", "")));
            quickPanel.add(btn);
        }
        card.add(quickPanel, gbc);
        // Nút Nạp
        gbc.gridy = 5;
        gbc.insets = new Insets(25, 20, 15, 20);
        btnTopUp = createSuccessButton("Nạp tiền vào thẻ");
        btnTopUp.setEnabled(false);
        btnTopUp.setPreferredSize(new Dimension(0, 45));
        btnTopUp.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnTopUp.addActionListener(e -> logicTopUp());
        card.add(btnTopUp, gbc);

        mainPanel.add(card);
        return mainPanel;
    }

    private JPanel createParkingPanel() {
        JPanel mainPanel = new JPanel(new GridLayout(1, 2, 15, 0));
        mainPanel.setBackground(LIGHT_BG);
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        mainPanel.add(createEntryCard());
        mainPanel.add(createExitCard());
        return mainPanel;
    }

    private JPanel createEntryCard() {
        JPanel card = createCard("CỔNG VÀO");
        card.setLayout(new BorderLayout(0, 15));

        JPanel inputPanel = new JPanel(new BorderLayout(10, 0));
        inputPanel.setOpaque(false);
        inputPanel.setBorder(new EmptyBorder(0, 0, 15, 0));

        JButton btnEntry = createSuccessButton("Quẹt thẻ");
        btnEntry.setPreferredSize(new Dimension(120, 40));
        btnEntry.addActionListener(e -> logicVehicleEntry());
        inputPanel.add(btnEntry, BorderLayout.CENTER);

        card.add(inputPanel, BorderLayout.NORTH);

        parkingTableModel = new DefaultTableModel(new Object[]{"Biển số xe", "Thời gian vào"}, 0);
        parkingTable = new JTable(parkingTableModel);
        styleTable(parkingTable);

        JScrollPane scrollPane = new JScrollPane(parkingTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
        card.add(scrollPane, BorderLayout.CENTER);

        return card;
    }

//    private JPanel createExitCard() {
//        JPanel card = createCard("CỔNG RA");
//        card.setLayout(new BorderLayout(0, 15));
//
//        // Panel thông tin (Hiển thị kết quả sau khi xử lý)
//        JPanel infoPanel = new JPanel(new GridLayout(4, 2, 10, 15));
//        infoPanel.setOpaque(false);
//        infoPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
//
//        infoPanel.add(createLabel("Biển số xe ra:"));
//        lblExitInfoBienSo = createInfoLabel("---");
//        infoPanel.add(lblExitInfoBienSo);
//
//        infoPanel.add(createLabel("Thời gian vào:"));
//        lblExitInfoVao = createInfoLabel("---");
//        infoPanel.add(lblExitInfoVao);
//
//        infoPanel.add(createLabel("Thời gian ra:"));
//        lblExitInfoRa = createInfoLabel("---");
//        infoPanel.add(lblExitInfoRa);
//
//        infoPanel.add(createLabel("Phí gửi xe:"));
//        lblExitInfoTongTien = createInfoLabel("---");
//        lblExitInfoTongTien.setFont(new Font("Segoe UI", Font.BOLD, 24));
//        lblExitInfoTongTien.setForeground(DANGER_COLOR);
//        infoPanel.add(lblExitInfoTongTien);
//
//        card.add(infoPanel, BorderLayout.CENTER);
//
//        // Nút hành động duy nhất
//        JButton btnProcessExit = createDangerButton("XỬ LÝ XE RA (QUẸT THẺ)");
//        btnProcessExit.setPreferredSize(new Dimension(0, 60));
//        btnProcessExit.setFont(new Font("Segoe UI", Font.BOLD, 18));
//        btnProcessExit.addActionListener(e -> logicProcessExit()); // Gọi hàm xử lý gộp
//        card.add(btnProcessExit, BorderLayout.SOUTH);
//
//        return card;
//    }
private JPanel createExitCard() {
    JPanel card = createCard("CỔNG RA - THANH TOÁN");
    card.setLayout(new GridBagLayout()); // Dùng GridBag để căn giữa nút

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(20, 20, 20, 20);
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridx = 0;
    gbc.gridy = 0;

    // Thêm một icon hoặc label trạng thái cho đỡ trống (Optional)
    JLabel lblIcon = new JLabel("<html><div style='text-align: center;'>HỆ THỐNG TỰ ĐỘNG<br>Sẵn sàng xử lý xe ra</div></html>", SwingConstants.CENTER);
    lblIcon.setFont(new Font("Segoe UI", Font.BOLD, 16));
    lblIcon.setForeground(Color.GRAY);
    card.add(lblIcon, gbc);

    gbc.gridy = 1;
    // Nút hành động duy nhất
    JButton btnProcessExit = createDangerButton("XỬ LÝ XE RA (QUẸT THẺ)");
    btnProcessExit.setPreferredSize(new Dimension(300, 80)); // Nút to hơn
    btnProcessExit.setFont(new Font("Segoe UI", Font.BOLD, 20));
    btnProcessExit.addActionListener(e -> logicProcessExit());

    card.add(btnProcessExit, gbc);

    return card;
}
    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(new Color(52, 73, 94));
        statusBar.setPreferredSize(new Dimension(0, 35));
        statusBar.setBorder(new EmptyBorder(5, 15, 5, 15));

        JLabel lblTime = new JLabel();
        lblTime.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblTime.setForeground(Color.WHITE);
        statusBar.add(lblTime, BorderLayout.EAST);

        Timer timer = new Timer(1000, e -> {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss - dd/MM/yyyy");
            lblTime.setText(sdf.format(new Date()));
        });
        timer.start();

        return statusBar;
    }

    // ==== UI Helper Methods ====
    private JPanel createCard(String title) {
        JPanel card = new JPanel();
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
                new EmptyBorder(20, 20, 20, 20)
        ));
        TitledBorder titledBorder = BorderFactory.createTitledBorder(
                BorderFactory.createEmptyBorder(), title, TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 16), PRIMARY_COLOR);
        card.setBorder(BorderFactory.createCompoundBorder(titledBorder, new EmptyBorder(10, 15, 15, 15)));
        return card;
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        label.setForeground(TEXT_PRIMARY);
        return label;
    }

    private JLabel createInfoLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        label.setForeground(TEXT_PRIMARY);
        return label;
    }

    private JTextField createStyledTextField() {
        JTextField field = new JTextField();
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setPreferredSize(new Dimension(0, 35));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                new EmptyBorder(5, 10, 5, 10)));
        return field;
    }

    private JPasswordField createStyledPasswordField() {
        JPasswordField field = new JPasswordField();
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setPreferredSize(new Dimension(0, 35));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                new EmptyBorder(5, 10, 5, 10)));
        return field;
    }

    private JButton createPrimaryButton(String text) {
        return createStyledButton(text, PRIMARY_COLOR, Color.WHITE);
    }
    private JButton createSecondaryButton(String text) {
        return createStyledButton(text, LIGHT_BG, TEXT_PRIMARY);
    }
    private JButton createSuccessButton(String text) {
        return createStyledButton(text, SUCCESS_COLOR, Color.WHITE);
    }
    private JButton createWarningButton(String text) {
        return createStyledButton(text, WARNING_COLOR, Color.WHITE);
    }
    private JButton createDangerButton(String text) {
        return createStyledButton(text, DANGER_COLOR, Color.WHITE);
    }

    private JButton createStyledButton(String text, Color bg, Color fg) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setBackground(bg);
        button.setForeground(fg);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setPreferredSize(new Dimension(150, 35));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { button.setBackground(bg.darker()); }
            public void mouseExited(MouseEvent e) { button.setBackground(bg); }
        });
        return button;
    }

    private void styleTable(JTable table) {
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(35);
        table.setSelectionBackground(ACCENT_COLOR);
        table.setSelectionForeground(Color.WHITE);
        table.setGridColor(new Color(230, 230, 230));
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setBackground(PRIMARY_COLOR);
        header.setForeground(Color.BLUE);
        header.setPreferredSize(new Dimension(0, 40));
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
    }

    private String formatCurrency(double amount) {
        return String.format("%,.0f VNĐ", amount);
    }
    private void logicViewParkingHistory() {
        // 1. Reset giao diện: Xóa dữ liệu bảng cũ và cập nhật trạng thái
        parkingHistoryModel.setRowCount(0);
        lblParkingHistoryOwner.setText("Đang đọc dữ liệu...");
        lblParkingHistoryOwner.setForeground(Color.BLUE);

        // 2. Thực hiện bắt tay với thẻ (Kết nối -> Lấy UID -> Xác thực chữ ký)
        // Hàm performCardHandshake trả về UID nếu thẻ xịn, null nếu lỗi/giả
        performCardHandshake();
        String cardUID = cardService.getCardID();
        if (cardUID == null) {
            lblParkingHistoryOwner.setText("Lỗi đọc thẻ hoặc thẻ không hợp lệ!");
            lblParkingHistoryOwner.setForeground(DANGER_COLOR);
            return;
        }

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
                lblParkingHistoryOwner.setText("<html>Nhật ký xe của: <b style='color:#27ae60'>" + ownerName + "</b> - Biển số: <b>" + licensePlate + "</b></html>");
                lblParkingHistoryOwner.setForeground(TEXT_PRIMARY);
            } else {
                lblParkingHistoryOwner.setText("Thẻ hợp lệ nhưng chưa có thông tin chủ xe (UID: " + cardUID + ")");
                lblParkingHistoryOwner.setForeground(Color.DARK_GRAY);
            }

            // 4. Truy vấn danh sách lịch sử ra vào từ DAO
            // Đảm bảo bạn đã thêm hàm getParkingSessionHistory vào CardHolderDAO
            java.util.List<String[]> sessions = cardDao.getParkingSessionHistory(cardUID);

            if (sessions == null || sessions.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Thẻ này chưa có lịch sử gửi xe nào!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
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
    private void logicSelectImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Chọn ảnh chân dung");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Hình ảnh (JPG, PNG)", "jpg", "png", "jpeg"));

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedAvatarFile = fileChooser.getSelectedFile();
            if (selectedAvatarFile.length() > 1024 * 1024) {
                JOptionPane.showMessageDialog(this, "Ảnh quá lớn! Vui lòng chọn ảnh < 1MB.", "Lỗi", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                ImageIcon originalIcon = new ImageIcon(selectedAvatarFile.getPath());
                Image img = originalIcon.getImage();
                Image newImg = img.getScaledInstance(120, 150, Image.SCALE_SMOOTH);
                lblAvatar.setIcon(new ImageIcon(newImg));
                lblAvatar.setText("");
            } catch (Exception e) {
                showError("Không thể đọc file ảnh!");
                e.printStackTrace();
            }
        }
    }

    // register
    private void performBlankCardCheck() {
        if (!cardService.connect()) {
            showError("Không tìm thấy thẻ! Vui lòng đặt thẻ lên đầu đọc.");
            updateStatus("Thẻ: Offline", Color.WHITE);
            return;
        }
        try {
            int status = cardService.checkCardStatus();
            if (status == 1) {
                showError("Thẻ này đã được đăng ký!\nVui lòng dùng thẻ trắng.");
                cardService.disconnect();
                updateStatus("Thẻ: Offline", Color.WHITE);
                return;
            }
            else if (status == -1) {
                showError("Lỗi đọc trạng thái thẻ (Unknown Status).");
                cardService.disconnect();
                updateStatus("Thẻ: Offline", Color.WHITE);
                return;
            }
            showSuccess("Thẻ trắng hợp lệ! Bạn có thể tiếp tục đăng ký.");
            updateStatus("Thẻ: Online", SUCCESS_COLOR);
            btnSave.setEnabled(true);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Lỗi kiểm tra thẻ trắng: " + e.getMessage());
            cardService.disconnect();
            updateStatus("Thẻ: Offline", Color.WHITE);
        }
    }
    private void logicRegister() {
        String name = txtOwnerName.getText().trim();
        String phone = txtPhone.getText().trim();
        String identity = txtIdentityCard.getText().trim();
        String license = txtLicensePlate.getText().trim();
        String vehicleType = (String) comboVehicleType.getSelectedItem();
        String userPin = txtPin.getText().trim();
        if (name.isEmpty()) {
            showError("Tên chủ sở hữu không được để trống!");
            txtOwnerName.requestFocus();
            return;
        }

        if (phone.isEmpty()) {
            showError("Số điện thoại không được để trống!");
            txtPhone.requestFocus();
            return;
        }

        if (!phone.matches("^(0|84)(\\d{9})$")) {
            showError("Số điện thoại không hợp lệ!");
            txtPhone.requestFocus();
            return;
        }

        if (identity.isEmpty()) {
            showError("CMND/CCCD không được để trống!");
            txtIdentityCard.requestFocus();
            return;
        }

        if (!identity.matches("^\\d{12}$")) {
            showError("CCCD phải là 12 số!");
            txtIdentityCard.requestFocus();
            return;
        }

        if (license.isEmpty()) {
            showError("Biển số xe không được để trống!");
            txtLicensePlate.requestFocus();
            return;
        }

        if (!license.matches("^[0-9]{2}[A-Z][0-9][0-9]{5}$")) {
            showError("Biển số xe không đúng định dạng!");
            txtLicensePlate.requestFocus();
            return;
        }

        if (selectedAvatarFile == null) {
            showError("Bạn chưa chọn ảnh đại diện!");
            return;
        }

        if (!selectedAvatarFile.exists()) {
            showError("File ảnh đại diện không tồn tại!");
            return;
        }

        if (userPin.length() != 6) {
            showError("PIN phải có 6 ký tự !");
            return;
        }
        try {
            // Hàm này sẽ ném Exception nếu SĐT bị trùng với người KHÁC
            // Nếu là người cũ thêm thẻ -> Hàm này chạy qua êm ru (không lỗi)
            cardDao.validateRegisterInfo(txtPhone.getText().trim(), txtIdentityCard.getText().trim());

        } catch (SQLException e) {
            // Bắt được lỗi trùng lặp -> Báo lỗi và DỪNG NGAY
            showError(e.getMessage());
            return;
        } catch (Exception e) {
            e.printStackTrace(); // Lỗi kết nối DB
            return;
        }
        try {

            if (!cardService.importHostPublicKey(keyManager.getPublicKey())) {
                showError("Lỗi: Không thể nạp Key bảo mật của Host vào thẻ!");
                resetForm();
                return;
            }
            boolean setupOk = cardService.setupCard(userPin);
            if (!setupOk) {
                showError("Lỗi khởi tạo thẻ (Setup failed)!");
                resetForm();
                return;
            }

            int sw = cardService.verifyPin(userPin);
            if (sw != CardService.SW_SUCCESS) {
                showError("Lỗi xác thực sau khi khởi tạo! Code: " + Integer.toHexString(sw));
                resetForm();
                return;
            }

            String cardUID = cardService.getCardID();

            if (cardUID == null) {
                showError("Lỗi: Thẻ không có carduid!");
                resetForm();
                return;
            }
            PublicKey cardPubKey = cardService.getCardPublicKey();
            if (cardPubKey == null) {
                showError("Lỗi: Thẻ không có publickey!");
                resetForm();
                return;
            }
            String pubKeyBase64 = Base64.getEncoder().encodeToString(cardPubKey.getEncoded());

            UserDTO userDTO = new UserDTO(name, phone, identity, license, vehicleType, cardUID, pubKeyBase64,selectedAvatarFile);
            boolean isDbSaved = cardDao.registerUser(userDTO);
            if (!isDbSaved) {
                //showError("Lỗi: Không thể lưu vào Database! Hủy đăng ký.");
                resetForm();
                return;
            }
            this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            boolean isInfoSet = cardService.setUserInfo(name, license, vehicleType);
            boolean isImgUploaded = cardService.uploadImage(selectedAvatarFile);
            this.setCursor(Cursor.getDefaultCursor());
            if (isInfoSet && isImgUploaded) {
                showSuccess("ĐĂNG KÝ THÀNH CÔNG!\nUID: " + cardUID);
                updateStatus("Thẻ: " + cardUID, SUCCESS_COLOR);
                resetForm();
            } else {
                showError("Đã lưu DB nhưng lỗi ghi chip (Info hoặc Ảnh)!");
            }
        } catch (RuntimeException re) {
            // Bắt lỗi nghiệp vụ từ DAO (Ví dụ: Số điện thoại đã tồn tại)
            this.setCursor(Cursor.getDefaultCursor());
            showError(re.getMessage());
            resetForm();
        } catch (Exception e) {
            e.printStackTrace();
            resetForm();
            showError("Lỗi hệ thống: " + e.getMessage());
        } finally {
            cardService.disconnect();
            updateStatus("Thẻ: chưa kết nối", Color.WHITE);
        }
    }

    // xem/cập nhật
    private void logicCheckCardExisting() {
//        // Gọi hàm Handshake chuẩn (đã có ở các bài trước)
//        // Hàm này Connect -> GetUID -> Verify Signature
//        performCardHandshake();
//
//        if (uid != null) {
//
//
//            lblCardStatus.setText("● Đã kết nối: " + uid);
//            lblCardStatus.setForeground(SUCCESS_COLOR);
//            showSuccess("Kết nối thành công!\nBạn có thể 'Xem thông tin' hoặc 'Lưu thay đổi'.");
//
//            // Mở khóa 2 nút bên dưới
//            btnViewInfo.setEnabled(true);
//            btnSave.setEnabled(true);
//
//            // Ngắt kết nối tạm để giải phóng đầu đọc (các hàm View/Save sẽ tự connect lại)
//            cardService.disconnect();
//        } else {
//            // Nếu lỗi
//            currentVerifiedUID = null;
//            btnViewInfo.setEnabled(false);
//            btnSave.setEnabled(false);
//        }
    }
    private void logicShowCardInfo() {
        boolean perform = performCardHandshake();
        if (!perform) {
            return;
        }
        try {
            if (!performSecureAuthentication("xem thông tin")) {
                updateStatus("Thẻ: chưa kết nối", Color.WHITE);
                cardService.disconnect();
                return;
            }

            this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            String cardUID = cardService.getCardID();
            if (cardUID == null) {
                showError("Không đọc được Card ID.");
                updateStatus("Thẻ: chưa kết nối", DANGER_COLOR);
                cardService.disconnect();
                return;
            }
            String chipInfo = cardService.getUserInfo();
            UserDTO dbUser = cardDao.getUserByCardID(cardUID);

            byte[] avatarBytes = cardService.downloadImage();

            this.setCursor(Cursor.getDefaultCursor());

            if (chipInfo != null) {
                Map<String, String> infoMap = cardService.getUserInfoMap(chipInfo);
                if (infoMap != null) {
                    txtOwnerName.setText(infoMap.get("name"));
                    txtLicensePlate.setText(infoMap.get("license"));
                    comboVehicleType.setSelectedItem(infoMap.get("type"));
                }
            }

            if (dbUser != null) {
                txtPhone.setText(dbUser.getPhone());
                txtIdentityCard.setText(dbUser.getIdentityCard());
                if (txtOwnerName.getText().isEmpty()) txtOwnerName.setText(dbUser.getName());
                if (txtLicensePlate.getText().isEmpty()) txtLicensePlate.setText(dbUser.getLicensePlate());
            } else {
                txtPhone.setText("");
                txtIdentityCard.setText("");
                updateStatus("Cảnh báo: Không tìm thấy dữ liệu trên Server", Color.ORANGE);
            }

            if (avatarBytes != null && avatarBytes.length > 0) {
                ImageIcon icon = org.example.util.ImageUtils.convertBytesToIcon(avatarBytes);
                lblAvatar.setIcon(icon);
                lblAvatar.setText("");
                selectedAvatarFile = null;
            } else {
                lblAvatar.setIcon(null);
            }
            updateStatus("Thẻ Online: " + cardUID, SUCCESS_COLOR);
            btnSave.setEnabled(true);
        } catch (Exception e) {
            e.printStackTrace();
            updateStatus("Thẻ: chưa kết nối", DANGER_COLOR);
            cardService.disconnect();
            showError("Lỗi khi đọc thẻ: " + e.getMessage());
        } finally {
            this.setCursor(Cursor.getDefaultCursor());
        }
    }

    private void logicUpdateCardInfo() {
        String name = txtOwnerName.getText().trim();
        String phone = txtPhone.getText().trim();
        String identity = txtIdentityCard.getText().trim();
        String license = txtLicensePlate.getText().trim();
        String vehicleType = (String) comboVehicleType.getSelectedItem();
        if (name.isEmpty()) {
            showError("Tên chủ sở hữu không được để trống!");
            txtOwnerName.requestFocus();
            return;
        }

        if (phone.isEmpty()) {
            showError("Số điện thoại không được để trống!");
            txtPhone.requestFocus();
            return;
        }

        if (!phone.matches("^(0|84)(\\d{9})$")) {
            showError("Số điện thoại không hợp lệ!");
            txtPhone.requestFocus();
            return;
        }

        if (identity.isEmpty()) {
            showError("CMND/CCCD không được để trống!");
            txtIdentityCard.requestFocus();
            return;
        }

        if (!identity.matches("^\\d{12}$")) {
            showError("CCCD phải là 12 số!");
            txtIdentityCard.requestFocus();
            return;
        }

        if (license.isEmpty()) {
            showError("Biển số xe không được để trống!");
            txtLicensePlate.requestFocus();
            return;
        }

        if (!license.matches("^[0-9]{2}[A-Z][0-9][0-9]{5}$")) {
            showError("Biển số xe không đúng định dạng!");
            txtLicensePlate.requestFocus();
            return;
        }


        try {
            if (!performSecureAuthentication("cập nhật thông tin")) {
                resetForm();
                btnSave.setEnabled(false);
                return;
            }
            // 4. Lấy UID để update DB
            String cardUID = cardService.getCardID();
            if (cardUID == null) {
                showError("Lỗi: Thẻ không có carduid!");
                resetForm();
                return;
            }

            this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            UserDTO userDTO = new UserDTO(name, phone, identity, license, vehicleType, cardUID, selectedAvatarFile);
            boolean dbUpdated = cardDao.updateUser(userDTO);

            if (!dbUpdated) {
                this.setCursor(Cursor.getDefaultCursor());
                showError("Lỗi: Không thể cập nhật Database.");
                return;
            }

            boolean cardTextUpdated = cardService.setUserInfo(name, license, vehicleType);
            if (!cardTextUpdated) {
                this.setCursor(Cursor.getDefaultCursor());
                showError("Đã update DB nhưng lỗi ghi thông tin vào Chip!");
                return;
            }

            boolean cardImgUpdated = true;
            if (selectedAvatarFile != null) {
                cardImgUpdated = cardService.uploadImage(selectedAvatarFile);
            }

            this.setCursor(Cursor.getDefaultCursor());

            if (cardImgUpdated) {
                showSuccess("CẬP NHẬT THÀNH CÔNG!");
                selectedAvatarFile = null;
            } else {
                showError("Đã update thông tin nhưng lỗi upload ảnh vào Chip!");
            }
            resetForm();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Lỗi hệ thống: " + e.getMessage());
        } finally {
            this.setCursor(Cursor.getDefaultCursor());
            cardService.disconnect();
            btnSave.setEnabled(false);
        }
    }

    private void logicUnblockCard() {
        boolean connected = performCardHandshake();
        if(!connected) {
            return;
        }
        JPasswordField passwordField = new JPasswordField();
        int option = JOptionPane.showConfirmDialog(
                this,
                passwordField,
                "Thẻ đang bị KHÓA (BLOCKED)!\nVui lòng nhập mã PUK để mở khóa thẻ:",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (option != JOptionPane.OK_OPTION) return;
        String puk = new String(passwordField.getPassword());


        if (cardService.unblockCard(puk)) {
            showSuccess("Mở khóa thẻ thành công!\nSố lần thử PIN đã được reset.");
            lblCardStatus.setText("● Thẻ: Online (Active)");
            lblCardStatus.setForeground(SUCCESS_COLOR);
        } else {
            showError("Mở khóa thất bại! Mã PUK không chính xác.");
        }
        cardService.disconnect();
    }
    private void logicCheckCard(){
        boolean connected = performCardHandshake();
        if(!connected) {
            btnChangePin.setEnabled(false);
        }
        else {
            showSuccess("Thẻ đã kết nối! Bạn có thể đổi PIN.");
            btnChangePin.setEnabled(true);
        }
    }
    private void logicChangePin() {
        String newPin = new String(txtNewPin.getPassword());
        String confirmPin = new String(txtConfirmPin.getPassword());

        if (newPin.isEmpty() || confirmPin.isEmpty()) { showError("Vui lòng điền đầy đủ thông tin PIN!"); return; }
        if (!newPin.equals(confirmPin)) { showError("PIN mới không khớp!"); return; }
        if (newPin.length() != 6) { showError("PIN phải có 6 ký tự !"); return; }
        if (!performSecureAuthentication("đổi mật khẩu")) {
            return;
        }
        try {
            cardService.changePin(newPin);
            showSuccess("Đổi PIN thành công!\nKhóa bảo mật và PIN đã được cập nhật.");
            txtNewPin.setText("");
            txtConfirmPin.setText("");
            btnChangePin.setEnabled(false);
        } catch (Exception e) {
            showError(e.getMessage());
        } finally {
            updateStatus("Thẻ: chưa kết nối", Color.WHITE);
            cardService.disconnect();
        }
    }

    private void logicCheckCardForTopUp() {

        boolean connect = performCardHandshake();
        if (!connect) {
            lblTopUpCardInfo.setText("Vui lòng quẹt thẻ...");
            lblTopUpCardInfo.setIcon(null);
            return;
        }
        try {
            if (!performSecureAuthentication("kiểm tra thẻ")) {
                // Reset giao diện nếu hủy nhập PIN
                lblTopUpCardInfo.setText("Vui lòng quẹt thẻ...");
                lblTopUpCardInfo.setIcon(null);
                return;
            }
            String cardUID = cardService.getCardID();
            if (cardUID == null || cardUID.isEmpty()) {
                showError("Thẻ này chưa được định danh (Trống ID)!");
                return;
            }
            if (!verifyCardSignature(cardUID)) {
                showError("Phát hiện thẻ giả mạo!");
                return;
            }
            int chipBalance = cardService.getBalance();
            if (chipBalance == -1) {
                showError("Lỗi đọc thẻ (Applet chưa sẵn sàng)!");
                return;
            }
            byte[] avatarBytes = cardService.downloadImage();
            String chipInfo = cardService.getUserInfo();
            Map<String, String> infoMap = cardService.getUserInfoMap(chipInfo);
            if (infoMap != null) {
                String infoHtml = "<html><div style='text-align: center; width: 300px;'>"
                        + "<b style='font-size:12px; color:#2980b9'>THÔNG TIN THẺ</b><br>"
                        + "--------------------------------<br>"
                        + "UID: <b>" + cardUID + "</b><br>"
                        + "Chủ xe: <b>" + infoMap.get("name") + "</b><br>"
                        + "Biển số: <b>" + infoMap.get("license") + "</b><br>"
                        + "Số dư: <span style='color:red; font-size:14px'>" + formatCurrency(chipBalance) + "</span>"
                        + "</div></html>";
                lblTopUpCardInfo.setText(infoHtml);

                if (avatarBytes != null && avatarBytes.length > 0) {
                    ImageIcon icon = org.example.util.ImageUtils.convertBytesToIcon(avatarBytes);

                    if (icon != null) {
                        lblTopUpCardInfo.setIcon(icon);
                        lblTopUpCardInfo.setHorizontalTextPosition(JLabel.RIGHT);
                        lblTopUpCardInfo.setIconTextGap(15);
                    }
                } else {
                    lblTopUpCardInfo.setIcon(null);
                }
                lblBalance.setText("Số dư: " + formatCurrency(chipBalance));
                showSuccess("Đã nhận diện thẻ: " + cardUID);
                btnTopUp.setEnabled(true);
            } else {
                showError("Thẻ có ID " + cardUID + " nhưng không tìm thấy trong Database!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Lỗi trong quá trình xác thực thẻ: " + e.getMessage());
        }
    }
    private void logicTopUp() {
        String amountStr = txtTopUpAmount.getText().trim().replace(",", "");
        int amount;
        try {
            amount = Integer.parseInt(amountStr);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            showError("Số tiền nạp không hợp lệ!");
            return;
        }
        if (!performSecureAuthentication("nạp tiền")) {

            cardService.disconnect();
            lblTopUpCardInfo.setIcon(null);
            lblTopUpCardInfo.setText("Vui lòng quẹt thẻ...");
            txtTopUpAmount.setText("");
            btnTopUp.setEnabled(false);
            return;
        }

        try {
            PrivateKey hostPrivKey = keyManager.getPrivateKey();
            if (hostPrivKey == null) {
                showError("Lỗi hệ thống: Không tìm thấy khóa bảo mật của Host!");
                return;
            }
            this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            String cardUID = cardService.getCardID();
            if (cardUID == null){
                this.setCursor(Cursor.getDefaultCursor());
                showError("Không đọc được UID thẻ.");
                return;
            }

            String cardSignature = cardService.topUp(amount,hostPrivKey);
            if (cardSignature != null) {
                int newBalance = cardService.getBalance();
                boolean logSaved = cardDao.saveTransaction(cardUID, "TOPUP", amount, cardSignature);
                boolean balanceUpdated = cardDao.updateCardBalance(cardUID, newBalance);
                if (logSaved && balanceUpdated) {
                showSuccess("NẠP TIỀN THÀNH CÔNG!\n"
                        + "Số tiền: " + String.format("%,d", amount) + " VND\n"
                        + "Số dư mới: " + String.format("%,d", newBalance) + " VND");
                lblTopUpCardInfo.setText("Vui lòng quẹt thẻ...");
                lblTopUpCardInfo.setIcon(null);
                txtTopUpAmount.setText("");
                lblBalance.setText("Số dư: " +  String.format("%,d", newBalance) + " VND");
                btnTopUp.setEnabled(false);
                } else {
                    showError("Cảnh báo: Tiền đã vào thẻ nhưng lỗi cập nhật Database!\nSố dư trên thẻ: " + newBalance);
                }

            } else {
                this.setCursor(Cursor.getDefaultCursor());
                showError("Nạp tiền thất bại! Thẻ từ chối giao dịch (Lỗi xác thực hoặc chữ ký).");
            }
        } catch (Exception e) {
            this.setCursor(Cursor.getDefaultCursor());
            e.printStackTrace();
            btnTopUp.setEnabled(false);
            showError("Lỗi hệ thông: " + e.getMessage());
        } finally {
            this.setCursor(Cursor.getDefaultCursor());
            btnTopUp.setEnabled(false);
            updateStatus("Thẻ: chưa kết nối", Color.WHITE);
            cardService.disconnect();
        }
    }

    private void logicVehicleEntry() {
        performCardHandshake();
        if (!performSecureAuthentication("xe vào bến")) {
            cardService.disconnect();
            return;
        }
        try {
            String cartUid = cardService.getCardID();
            String info = cardService.getUserInfo();
            if (info == null || info.isEmpty()) {
                showError("Không có thông tin người dùng trên thẻ!");
                return;
            }
            Map<String, String> userMap = cardService.getUserInfoMap(info);
            if (userMap != null) {
                if (cardDao.isCarParked(cartUid)) {
                    showError("Thẻ này đang có xe trong bãi! (Chưa check-out)");
                    return;
                }
                if (cardDao.checkIn(cartUid)) {
                    loadParkingData();
                    showSuccess("Mời xe vào!\nBiển số: " + userMap.get("license"));
                } else {
                    showError("Lỗi Check-in Database!");
                }
            }
            cardService.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    private void logicProcessExit() {
//        performCardHandshake();
//
//        try {
//            String cardUID = cardService.getCardID();
//            if (cardUID == null || cardUID.trim().isEmpty()) {
//                showError("Không đọc được UID từ thẻ!");
//                return;
//            }
//
//            if (!verifyCardSignature(cardUID)) {
//                showError("Cảnh báo: Phát hiện thẻ giả mạo!");
//                return;
//            }
//            // A3. Lấy thông tin Check-in từ DB
//            long inTime = cardDao.getCheckInTime(cardUID);
//            if (inTime == 0) {
//                showError("Thẻ này chưa Check-in (Xe không có trong bãi)!");
//                return;
//            }
//
//            String[] cardInfo = cardDao.getCardInfoByUID(cardUID);
//            String licensePlate = (cardInfo != null) ? cardInfo[2] : "Unknown";
//            String vehicleType = (cardInfo != null) ? cardInfo[3] : "Xe máy";
//
//
//            double unitPrice = 5000;
//            String typeLower = vehicleType.toLowerCase();
//            if (typeLower.contains("ô tô") || typeLower.contains("oto")) {
//                unitPrice = 20000;
//            } else if (typeLower.contains("đạp")) {
//                unitPrice = 3000;
//            }
//
//            long outTime = System.currentTimeMillis();
//            long durationSeconds = (outTime - inTime) / 1000;
//            long blocks = (long) Math.ceil(durationSeconds / 15.0);
//            if (blocks < 1) blocks = 1;
//
//            int totalFee = (int) (blocks * unitPrice);
//
//            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy");
//            lblExitInfoBienSo.setText(licensePlate + " (" + vehicleType + ")");
//            lblExitInfoVao.setText(sdf.format(new Date(inTime)));
//            lblExitInfoRa.setText(sdf.format(new Date(outTime)));
//            lblExitInfoTongTien.setText(formatCurrency(totalFee));
//
//            int currentBalance = cardService.getBalance();
//            if (currentBalance == -1) {
//                showError("Không đọc được số dư! (Có thể cần xác thực)");
//                return;
//            }
//
//            if (currentBalance < totalFee) {
//                showError("Số dư không đủ!\nPhí: " + formatCurrency(totalFee) + "\nSố dư: " + formatCurrency(currentBalance));
//                resetExitGate();
//                return;
//            }
//
//            if (totalFee > 200000) {
//                boolean authOk = performSecureAuthentication("thanh toán " + formatCurrency(totalFee));
//                if (!authOk) {
//                    lblExitInfoTongTien.setText("Đã hủy");
//                    return;
//                }
//            }
//            PrivateKey hostPrivKey = keyManager.getPrivateKey();
//            if (hostPrivKey == null) {
//                showError("Lỗi hệ thống: Không tìm thấy Host Key!");
//                return;
//            }
//
//            String signature = cardService.pay(totalFee, hostPrivKey);
//
//            if (signature == null) {
//                showError("Thanh toán thất bại! Lỗi xử lý Chip.");
//                return;
//            }
//            cardDao.saveTransaction(cardUID, "PAYMENT", totalFee, signature);
//
//            int newBalance = cardService.getBalance();
//            cardDao.updateCardBalance(cardUID, newBalance);
//
//            cardDao.checkOut(cardUID, totalFee);
//
//            lblBalance.setText("Số dư: " + formatCurrency(newBalance));
//
//            loadParkingData();
//
//            String msg = "XE RA THÀNH CÔNG!\n" +
//                    "Biển số: " + licensePlate + "\n" +
//                    "Phí: " + formatCurrency(totalFee) + "\n" +
//                    "Số dư còn lại: " + formatCurrency(newBalance);
//
//            JOptionPane.showMessageDialog(this, msg, "Thông báo", JOptionPane.INFORMATION_MESSAGE);
//
//            resetExitGate();
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            showError("Lỗi hệ thống: " + e.getMessage());
//        } finally {
//            cardService.disconnect();
//        }
//    }

    private void logicProcessExit() {
        // 1. Kết nối và Kiểm tra thẻ (Bắt buộc)
        performCardHandshake();
        String cardUID = cardService.getCardID();
        if (cardUID == null) {
            return; // Lỗi kết nối hoặc thẻ giả
        }

        try {
            // 2. Lấy dữ liệu Check-in
            long inTime = cardDao.getCheckInTime(cardUID);
            if (inTime == 0) {
                showError("Thẻ này chưa Check-in (Xe không có trong bãi)!");
                return;
            }

            // 3. Lấy thông tin xe
            String[] cardInfo = cardDao.getCardInfoByUID(cardUID);
            String licensePlate = (cardInfo != null) ? cardInfo[2] : "Unknown";
            String vehicleType = (cardInfo != null) ? cardInfo[3] : "Xe máy";

            // 4. Tính toán phí ngầm
            double unitPrice = 5000;
            String typeLower = vehicleType.toLowerCase();
            if (typeLower.contains("ô tô") || typeLower.contains("oto")) {
                unitPrice = 20000;
            } else if (typeLower.contains("đạp")) {
                unitPrice = 3000;
            }

            long outTime = System.currentTimeMillis();
            long durationSeconds = (outTime - inTime) / 1000;
            long blocks = (long) Math.ceil(durationSeconds / 15.0);
            if (blocks < 1) blocks = 1;

            int totalFee = (int) (blocks * unitPrice);

            // 5. Kiểm tra số dư (Bắt buộc phải check trước khi trừ)
            int currentBalance = cardService.getBalance();
            if (currentBalance == -1) {
                showError("Lỗi đọc số dư! (Có thể cần nhập PIN để mở khóa đọc)");
                return;
            }

            if (currentBalance < totalFee) {
                showError("Số dư không đủ thanh toán!\n" +
                        "Phí: " + formatCurrency(totalFee) + "\n" +
                        "Số dư: " + formatCurrency(currentBalance) + "\n" +
                        "Thiếu: " + formatCurrency(totalFee - currentBalance));
                return;
            }

            // --- ĐÃ BỎ BƯỚC POPUP XÁC NHẬN "CÓ/KHÔNG" TẠI ĐÂY ---

            // 6. Xử lý bảo mật (Chỉ hỏi PIN nếu số tiền lớn > 200k)
            if (totalFee > 200000) {
                boolean authOk = performSecureAuthentication("thanh toán " + formatCurrency(totalFee));
                if (!authOk) return; // Hủy nhập PIN
            }

            // 7. TRỪ TIỀN NGAY LẬP TỨC
            PrivateKey hostPrivKey = keyManager.getPrivateKey();
            if (hostPrivKey == null) {
                showError("Lỗi hệ thống: Không tìm thấy Host Key!");
                return;
            }

            // Gọi lệnh xuống chip
            String signature = cardService.pay(totalFee, hostPrivKey);

            if (signature == null) {
                showError("Thanh toán thất bại! Lỗi xử lý Chip.");
                return;
            }

            // 8. Cập nhật Database & Hoàn tất
            cardDao.saveTransaction(cardUID, "PAYMENT", totalFee, signature);

            int newBalance = cardService.getBalance();
            cardDao.updateCardBalance(cardUID, newBalance);
            cardDao.checkOut(cardUID, totalFee);

            // Update UI
            loadParkingData();

            // 9. THÔNG BÁO THÀNH CÔNG & MỞ CỔNG
            // Hiển thị thông tin chi tiết sau khi đã trừ tiền xong
            String successMsg = "<html><div style='width: 250px; text-align: center; font-family: Segoe UI;'>"
                    + "<h2 style='color: #27ae60;'>XE RA THÀNH CÔNG!</h2>"
                    + "<hr>"
                    + "Biển số: <b>" + licensePlate + "</b><br>"
                    + "Thời gian: " + durationSeconds + " giây<br>"
                    + "--------------------------------<br>"
                    + "Phí: <b style='color: red; font-size: 14px;'>" + formatCurrency(totalFee) + "</b><br>"
                    + "Số dư còn lại: <b>" + formatCurrency(newBalance) + "</b>"
                    + "</div></html>";

            // Dùng INFORMATION_MESSAGE để thông báo xong là đi
            JOptionPane.showMessageDialog(this, successMsg, "Hoàn tất", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            e.printStackTrace();
            showError("Lỗi hệ thống: " + e.getMessage());
        } finally {
            cardService.disconnect();
        }
    }

    private void resetExitGate() {
        lblExitInfoBienSo.setText("---");
        lblExitInfoVao.setText("---");
        lblExitInfoRa.setText("---");
        lblExitInfoTongTien.setText("---");
        lblExitInfoTongTien.setForeground(Color.BLACK);

        System.out.println("Đã reset giao diện cổng ra");
    }



    private boolean verifyCardSignature(String cardUID) {
        try {
            if (!performSecureAuthentication("xác thực thẻ")) {
                cardService.disconnect();
                return false;
            }
            byte[] nonce = new byte[16];
            new SecureRandom().nextBytes(nonce);
            byte[] signature = cardService.authenticateCard(nonce);
            String pubKeyBase64 = cardDao.getCardPublicKey(cardUID);
            if (signature == null || pubKeyBase64 == null) return false;

            byte[] pubKeyBytes = Base64.getDecoder().decode(pubKeyBase64);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(pubKeyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PublicKey publicKey = kf.generatePublic(spec);

            java.security.Signature sig = java.security.Signature.getInstance("SHA1withRSA");
            sig.initVerify(publicKey);
            sig.update(nonce);
            return sig.verify(signature);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    private boolean performCardHandshake(){
        if(!cardService.connect()){
            showError("Không tìm thấy thẻ hoặc thẻ không hợp lệ!");
            updateStatus("Thẻ: Offline", Color.WHITE);
            return false;
        }
        try {
            int status = cardService.checkCardStatus();
            if (status == 0) {
                showError("Thẻ trắng!\nVui lòng đăng ký thẻ trước.");
                updateStatus("Thẻ: Offline", Color.WHITE);
                cardService.disconnect();
                return false;
            }
            else if (status == -1) {
                showError("Lỗi đọc trạng thái thẻ (Unknown Status).");
                updateStatus("Thẻ: Offline", Color.WHITE);
                cardService.disconnect();
                return false;
            }

            String cardUID = cardService.getCardID();
            if (cardUID == null || cardUID.isEmpty()) {
                showError("Lỗi: Không đọc được định danh thẻ (UID)!");
                updateStatus("Thẻ: Offline", Color.WHITE);
                cardService.disconnect();
                return false;
            }
            if (!verifyCardSignature(cardUID)) {
                showError("CẢNH BÁO AN NINH: Phát hiện thẻ giả mạo (Clone)!");
                updateStatus("Thẻ: Offline", Color.WHITE);
                cardService.disconnect();
                return false;
            }
            updateStatus("Thẻ: Online: " + cardUID, SUCCESS_COLOR);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            showError("Lỗi trong quá trình kết nối thẻ: " + e.getMessage());
            updateStatus("Thẻ: Offline", Color.WHITE);
            cardService.disconnect();
            return false;
        }
    }

    private void updateStatus(String message, Color color) {
        lblCardStatus.setText("● " + message);
        lblCardStatus.setForeground(color);
    }

    private void resetForm() {
        txtOwnerName.setText("");
        txtPhone.setText("");
        txtIdentityCard.setText("");
        txtLicensePlate.setText("");
        comboVehicleType.setSelectedIndex(0);
        lblAvatar.setIcon(null);
        lblAvatar.setText("Chưa có ảnh");
        selectedAvatarFile = null;
        txtPin.setText("");
        btnSave.setEnabled(false);
        updateStatus("Thẻ: Chưa kết nối", Color.WHITE);
    }

    private boolean performSecureAuthentication(String action) {
        while (true) {
            JPasswordField pf = new JPasswordField();
            int okCxl = JOptionPane.showConfirmDialog(this, pf,
                    "Nhập PIN để " + action + ":", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (okCxl != JOptionPane.OK_OPTION) {
                return false;
            }
            String pin = new String(pf.getPassword());
            if (pin.isEmpty()) {
                showError("Vui lòng nhập PIN!");
                continue;
            }

            int sw = cardService.verifyPin(pin);
            if (sw == CardService.SW_SUCCESS) {
                return true;
            }
            else if ((sw & 0xFFF0) == 0x63C0) {
                // 0x63Cx: Sai PIN (Mặt nạ bit để lấy số lần còn lại)
                int retries = sw & 0x0F;
                showError("Mã PIN sai! Số lần thử còn lại: " + retries);
                // Không return false, vòng lặp tiếp tục để cho nhập lại
            }
            else if (sw == CardService.SW_CARD_BLOCKED) {
                // 0x6983: Thẻ bị khóa
                showError("Thẻ đang bị KHÓA! Vui lòng liên hệ admin để mở khóa.");
                return false;
            }
            else if (sw == CardService.SW_CARD_NOT_SETUP) {
                // 0x6901: Thẻ trắng
                showError("Thẻ chưa được cài đặt (Thẻ trắng).");
                return false;
            }
            else {
                showError("Mất kêt nối với thẻ.");
                return false;
            }
        }
    }

    private void loadParkingData() {
        parkingTableModel.setRowCount(0);
        java.util.List<String[]> parkedCars = cardDao.getParkedCars();
        if (parkedCars != null) {
            for (String[] car : parkedCars) {
                parkingTableModel.addRow(car);
            }
        }
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
                lblHistoryOwner.setText("<html>Lịch sử của: <b style='color:#27ae60'>" + ownerName + "</b> - Biển số: <b>" + license + "</b></html>");
                lblHistoryOwner.setForeground(TEXT_PRIMARY);
            } else {
                lblHistoryOwner.setText("Thẻ chưa định danh: " + cardUID);
            }
            java.util.List<String[]> logs = cardDao.getTransactionHistory(cardUID);

            if (logs.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Thẻ này chưa có giao dịch nào!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
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
    private boolean validateInputs() {
        String name = txtOwnerName.getText().trim();
        String phone = txtPhone.getText().trim();
        String identity = txtIdentityCard.getText().trim();
        String license = txtLicensePlate.getText().trim();
        String userPin = txtPin.getText().trim();

        if (name.isEmpty()) { showError("Tên chủ sở hữu không được để trống!"); txtOwnerName.requestFocus(); return false; }

        if (phone.isEmpty()) { showError("Số điện thoại không được để trống!"); txtPhone.requestFocus(); return false; }
        if (!phone.matches("^(0|84)(\\d{9})$")) { showError("Số điện thoại không hợp lệ!"); txtPhone.requestFocus(); return false; }

        if (identity.isEmpty()) { showError("CMND/CCCD không được để trống!"); txtIdentityCard.requestFocus(); return false; }
        if (!identity.matches("^\\d{12}$")) { showError("CCCD phải là 12 số!"); txtIdentityCard.requestFocus(); return false; }

        if (license.isEmpty()) { showError("Biển số xe không được để trống!"); txtLicensePlate.requestFocus(); return false; }
        if (!license.matches("^[0-9]{2}[A-Z][0-9]-[0-9]{5}$")) { // Đã thêm dấu gạch ngang cho chuẩn format
            showError("Biển số xe không đúng định dạng (VD: 29A1-12345)!"); txtLicensePlate.requestFocus(); return false;
        }

        if (selectedAvatarFile == null || !selectedAvatarFile.exists()) {
            showError("Bạn chưa chọn ảnh đại diện hoặc file không tồn tại!");
            return false;
        }

        if (userPin.length() != 6 || !userPin.matches("\\d+")) {
            showError("PIN phải có 6 ký tự số!");
            txtPin.requestFocus();
            return false;
        }

        return true;
    }
    private void showSuccess(String message) { JOptionPane.showMessageDialog(this, message, "Thành công", JOptionPane.INFORMATION_MESSAGE); }
    private void showError(String message) { JOptionPane.showMessageDialog(this, message, "Lỗi", JOptionPane.ERROR_MESSAGE); }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) { e.printStackTrace(); }
            ModernParkingCardUI app = new ModernParkingCardUI();
            app.setVisible(true);
        });
    }
}