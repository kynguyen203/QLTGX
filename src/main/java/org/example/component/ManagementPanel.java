package org.example.component;

import org.example.CardService;
import org.example.database.CardHolderDAO;
import org.example.database.DTO.CardHolderDTO;
import org.example.database.DTO.ParkingCardDTO;
import org.example.database.DTO.UserDTO;
import org.example.util.EnvKeyLoader;
import org.example.util.ImageUtils;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.security.PublicKey;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Map;

public class ManagementPanel extends BasePanel {

    private JTextField txtOwnerName, txtPhone, txtIdentityCard, txtLicensePlate;
    private JComboBox<String> comboVehicleType;
    private JPasswordField txtNewPin, txtConfirmPin, txtPin;
    private JLabel lblAvatar;
    private File selectedAvatarFile;
    private JLabel lblPin;
    private JButton btnCheckCard;
    private JButton btnSave;
    private JButton btnChangePin;
    private JButton btnViewInfo;

    private enum FormMode {
        REGISTER_MODE, UPDATE_MODE
    }

    private FormMode currentMode = FormMode.REGISTER_MODE;

    public ManagementPanel(CardService cardService, CardHolderDAO cardDao, EnvKeyLoader keyManager,
            StatusListener statusListener) {
        super(cardService, cardDao, keyManager, statusListener);
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 0, 15, 0);

        add(createRegisterCard(), gbc);

        gbc.gridy = 1;
        gbc.weighty = 0;
        add(createChangePinCard(), gbc);

        gbc.gridy = 2;
        gbc.weighty = 0;
        add(createUnblockCardPanel(), gbc);

        gbc.gridy = 3;
        gbc.weighty = 1.0;
        add(Box.createVerticalGlue(), gbc);
    }

    private JPanel createRegisterCard() {
        JPanel card = createCard("Quản lý Thông tin Thẻ");
        card.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JPanel infoPanel = new JPanel(new GridBagLayout());
        infoPanel.setOpaque(false);
        GridBagConstraints gbcInfo = new GridBagConstraints();
        gbcInfo.insets = new Insets(5, 5, 5, 5);
        gbcInfo.fill = GridBagConstraints.HORIZONTAL;

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

        gbcInfo.gridx = 0;
        gbcInfo.gridy = 0;
        gbcInfo.gridwidth = 2;
        infoPanel.add(modePanel, gbcInfo);

        btnCheckCard = createWarningButton("Kiểm tra thẻ trắng");
        gbcInfo.gridy = 1;
        infoPanel.add(btnCheckCard, gbcInfo);

        gbcInfo.gridwidth = 1;
        gbcInfo.gridx = 0;
        gbcInfo.gridy = 2;
        gbcInfo.weightx = 0.3;
        infoPanel.add(createLabel("Tên chủ xe:"), gbcInfo);
        gbcInfo.gridx = 1;
        gbcInfo.weightx = 0.7;
        txtOwnerName = createStyledTextField();
        infoPanel.add(txtOwnerName, gbcInfo);

        gbcInfo.gridx = 0;
        gbcInfo.gridy = 3;
        infoPanel.add(createLabel("Số điện thoại:"), gbcInfo);
        gbcInfo.gridx = 1;
        txtPhone = createStyledTextField();
        infoPanel.add(txtPhone, gbcInfo);

        gbcInfo.gridx = 0;
        gbcInfo.gridy = 4;
        infoPanel.add(createLabel("CCCD/CMND:"), gbcInfo);
        gbcInfo.gridx = 1;
        txtIdentityCard = createStyledTextField();
        infoPanel.add(txtIdentityCard, gbcInfo);

        gbcInfo.gridx = 0;
        gbcInfo.gridy = 5;
        infoPanel.add(createLabel("Biển số xe:"), gbcInfo);
        gbcInfo.gridx = 1;
        txtLicensePlate = createStyledTextField();
        infoPanel.add(txtLicensePlate, gbcInfo);

        gbcInfo.gridx = 0;
        gbcInfo.gridy = 6;
        infoPanel.add(createLabel("Loại xe:"), gbcInfo);
        gbcInfo.gridx = 1;
        comboVehicleType = new JComboBox<>(new String[] { "Xe máy", "Ô tô", "Xe đạp" });
        comboVehicleType.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        comboVehicleType.setPreferredSize(new Dimension(0, 35));
        infoPanel.add(comboVehicleType, gbcInfo);

        gbcInfo.gridx = 0;
        gbcInfo.gridy = 7;
        lblPin = createLabel("Mã PIN:");
        infoPanel.add(lblPin, gbcInfo);
        gbcInfo.gridx = 1;
        txtPin = createStyledPasswordField();
        infoPanel.add(txtPin, gbcInfo);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.65;
        card.add(infoPanel, gbc);

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

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.35;
        gbc.anchor = GridBagConstraints.NORTH;
        card.add(avatarPanel, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.setOpaque(false);

        btnViewInfo = createPrimaryButton("Xem thông tin");
        btnViewInfo.setPreferredSize(new Dimension(150, 45));
        btnViewInfo.setVisible(false);

        btnSave = createPrimaryButton("Đăng ký");
        btnSave.setPreferredSize(new Dimension(150, 45));
        btnSave.setEnabled(false);

        buttonPanel.add(btnViewInfo);
        buttonPanel.add(btnSave);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.weighty = 1.0;
        card.add(buttonPanel, gbc);

        ActionListener modeListener = e -> {
            resetForm();
            selectedAvatarFile = null;
            lblAvatar.setIcon(null);
            lblAvatar.setText("Chưa có ảnh");
            btnSave.setEnabled(false);
            btnViewInfo.setEnabled(false);

            if (rbRegister.isSelected()) {
                currentMode = FormMode.REGISTER_MODE;
                btnCheckCard.setText("Kiểm tra thẻ trắng");
                btnViewInfo.setVisible(false);
                btnSave.setText("Đăng ký");
                btnSave.setVisible(true);
                lblPin.setVisible(true);
                txtPin.setVisible(true);
            } else {
                currentMode = FormMode.UPDATE_MODE;
                btnCheckCard.setText("Quẹt thẻ");
                btnViewInfo.setVisible(true);
                btnViewInfo.setEnabled(false);
                btnSave.setText("Lưu thay đổi");
                btnSave.setVisible(true);
                lblPin.setVisible(false);
                txtPin.setVisible(false);
            }
        };
        rbRegister.addActionListener(modeListener);
        rbUpdate.addActionListener(modeListener);

        btnCheckCard.addActionListener(e -> {
            if (currentMode == FormMode.REGISTER_MODE) {
                performBlankCardCheck();
            } else {
                logicCheckCardExisting();
            }
        });

        btnViewInfo.addActionListener(e -> logicShowCardInfo());

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
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.gridwidth = 2;
        JButton btnCheckCard = createWarningButton("Quẹt thẻ");
        btnCheckCard.addActionListener(e -> logicCheckCard());
        card.add(btnCheckCard, gbc);
        gbc.gridwidth = 1;

        gbc.gridx = 0;
        gbc.gridy = 1;
        card.add(createLabel("PIN mới:"), gbc);
        gbc.gridx = 1;
        txtNewPin = createStyledPasswordField();
        card.add(txtNewPin, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        card.add(createLabel("Xác nhận PIN:"), gbc);
        gbc.gridx = 1;
        txtConfirmPin = createStyledPasswordField();
        card.add(txtConfirmPin, gbc);

        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.EAST;
        btnChangePin = createWarningButton("Đổi PIN");
        btnChangePin.setEnabled(false);
        btnChangePin.addActionListener(e -> logicChangePin());
        card.add(btnChangePin, gbc);

        return card;
    }

    private JPanel createUnblockCardPanel() {
        JPanel card = createCard("Khôi phục / Mở khóa Thẻ");
        card.setLayout(new BorderLayout(15, 0));

        JLabel lblInfo = new JLabel(
                "<html><i>Sử dụng khi thẻ bị khóa do đã nhập sai PIN quá 3 lần.<br>Cần mật khẩu (Personal Unblocking Key) để mở khóa.</i></html>");
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

    private void logicSelectImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Chọn ảnh chân dung");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Hình ảnh (JPG, PNG)", "jpg", "png", "jpeg"));

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedAvatarFile = fileChooser.getSelectedFile();
            if (selectedAvatarFile.length() > 1024 * 1024) {
                JOptionPane.showMessageDialog(this, "Ảnh quá lớn! Vui lòng chọn ảnh < 1MB.", "Lỗi",
                        JOptionPane.WARNING_MESSAGE);
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

    private void performBlankCardCheck() {
        if (!cardService.connect()) {
            showError("Không tìm thấy thẻ! Vui lòng đặt thẻ lên đầu đọc.");
            notifyStatus("Thẻ: Offline", Color.WHITE);
            return;
        }
        try {
            int status = cardService.checkCardStatus();
            if (status == 1) {
                showError("Thẻ này đã được đăng ký!\nVui lòng dùng thẻ trắng.");
                cardService.disconnect();
                notifyStatus("Thẻ: Offline", Color.WHITE);
                return;
            } else if (status == -1) {
                showError("Lỗi đọc trạng thái thẻ (Unknown Status).");
                cardService.disconnect();
                notifyStatus("Thẻ: Offline", Color.WHITE);
                return;
            }
            showSuccess("Thẻ trắng hợp lệ! Bạn có thể tiếp tục đăng ký.");
            notifyStatus("Thẻ: Online", SUCCESS_COLOR);
            btnSave.setEnabled(true);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Lỗi kiểm tra thẻ trắng: " + e.getMessage());
            cardService.disconnect();
            notifyStatus("Thẻ: Offline", Color.WHITE);
        }
    }

    private void logicRegister() {
        if (!validateInputs()) {
            return;
        }
        try {
            cardDao.validateRegisterInfo(txtPhone.getText().trim(), txtIdentityCard.getText().trim());
        } catch (SQLException e) {
            showError(e.getMessage());
            return;
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        try {
            if (!cardService.importHostPublicKey(keyManager.getPublicKey())) {
                showError("Lỗi: Không thể nạp Key bảo mật của Host vào thẻ!");
                resetForm();
                return;
            }
            boolean setupOk = cardService.setupCard(txtPin.getText().trim());
            if (!setupOk) {
                showError("Lỗi khởi tạo thẻ (Setup failed)!");
                resetForm();
                return;
            }

            int sw = cardService.verifyPin(txtPin.getText().trim());
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
            CardHolderDTO cardHolderDTO = new CardHolderDTO();
            cardHolderDTO.setFullName(txtOwnerName.getText().trim());
            cardHolderDTO.setPhoneNumber(txtPhone.getText().trim());
            cardHolderDTO.setIdentityCard(txtIdentityCard.getText().trim());
            cardHolderDTO.setAvatarImage(selectedAvatarFile);

            ParkingCardDTO parkingCardDTO = new ParkingCardDTO();
            parkingCardDTO.setCardUID(cardUID);
            parkingCardDTO.setPublicKey(pubKeyBase64);
            parkingCardDTO.setLicensePlate(txtLicensePlate.getText().trim());
            parkingCardDTO.setVehicleType((String) comboVehicleType.getSelectedItem());

            boolean isDbSaved = cardDao.registerUserTransaction(cardHolderDTO,parkingCardDTO);
            if (!isDbSaved) {
                showError("Lỗi: Không thể lưu vào Database!");
                resetForm();
                return;
            }
            this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            boolean isInfoSet = cardService.setUserInfo(cardHolderDTO.getFullName(), parkingCardDTO.getLicensePlate(), parkingCardDTO.getVehicleType());
            boolean isImgUploaded = cardService.uploadImage(selectedAvatarFile);
            this.setCursor(Cursor.getDefaultCursor());
            if (isInfoSet && isImgUploaded) {
                showSuccess("ĐĂNG KÝ THÀNH CÔNG!\nUID: " + cardUID);
                notifyStatus("Thẻ: " + cardUID, SUCCESS_COLOR);
                resetForm();
            } else {
                showError("Lỗi ghi dữ liệu vào Chip! Đang hoàn tác Database...");
                boolean rollbackOk = cardDao.rollbackRegistration(cardUID);
                if (rollbackOk) {
                    showError("Đã hoàn tác Database. Vui lòng thử đăng ký lại.");
                } else {
                    showError("LỖI NGHIÊM TRỌNG: Dữ liệu không đồng bộ! Hãy liên hệ Admin xóa thẻ " + cardUID);
                }
            }
        } catch (RuntimeException re) {
            this.setCursor(Cursor.getDefaultCursor());
            showError(re.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            resetForm();
            showError("Lỗi hệ thống: " + e.getMessage());
        } finally {
            cardService.disconnect();
            notifyStatus("Thẻ: chưa kết nối", Color.WHITE);
        }
    }

    private void logicCheckCardExisting() {
        boolean connected = performCardHandshake();
        if (!connected) {
            notifyStatus("Thẻ: chưa kết nối", Color.WHITE);
        } else {
            showSuccess("Thẻ đã kết nối! Bạn có thể xem thông tin thẻ.");
            btnViewInfo.setEnabled(true);
        }
    }

    private void logicShowCardInfo() {
        try {

            String cardUID = cardService.getCardID();
            if (!performSecureLogin(cardUID,"xem thông tin thẻ")) {
                notifyStatus("Thẻ: chưa kết nối", Color.WHITE);
                cardService.disconnect();
                return;
            }
            this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
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
                if (txtOwnerName.getText().isEmpty())
                    txtOwnerName.setText(dbUser.getName());
                if (txtLicensePlate.getText().isEmpty())
                    txtLicensePlate.setText(dbUser.getLicensePlate());
            } else {
                txtPhone.setText("");
                txtIdentityCard.setText("");
                notifyStatus("Cảnh báo: Không tìm thấy dữ liệu trên Server", Color.ORANGE);
            }

            if (avatarBytes != null && avatarBytes.length > 0) {
                ImageIcon icon = ImageUtils.convertBytesToIcon(avatarBytes);
                lblAvatar.setIcon(icon);
                lblAvatar.setText("");
                selectedAvatarFile = null;
            } else {
                lblAvatar.setIcon(null);
            }
            notifyStatus("Thẻ Online: " + cardUID, SUCCESS_COLOR);
            btnSave.setEnabled(true);
        } catch (Exception e) {
            e.printStackTrace();
            notifyStatus("Thẻ: chưa kết nối", DANGER_COLOR);
            cardService.disconnect();
            showError("Lỗi khi đọc thẻ: " + e.getMessage());
        } finally {
            this.setCursor(Cursor.getDefaultCursor());
        }
    }

    private void logicUpdateCardInfo() {
        if (!validateUpdateInputs()) {
            return;
        }
        try {
            cardDao.validateRegisterInfo(txtPhone.getText().trim(), txtIdentityCard.getText().trim());
        } catch (SQLException e) {
            showError(e.getMessage());
            return;
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        try {
            String cardUID = cardService.getCardID();
            if (!performSecureLogin(cardUID,"cập nhật thông tin")) {
                resetForm();
                btnSave.setEnabled(false);
                return;
            }

            this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            CardHolderDTO cardHolderDTO = new CardHolderDTO();
            cardHolderDTO.setFullName(txtOwnerName.getText().trim());
            cardHolderDTO.setPhoneNumber(txtPhone.getText().trim());
            cardHolderDTO.setIdentityCard(txtIdentityCard.getText().trim());
            cardHolderDTO.setAvatarImage(selectedAvatarFile);

            ParkingCardDTO parkingCardDTO = new ParkingCardDTO();
            parkingCardDTO.setCardUID(cardUID);
            parkingCardDTO.setLicensePlate(txtLicensePlate.getText().trim());
            parkingCardDTO.setVehicleType((String) comboVehicleType.getSelectedItem());
            boolean dbUpdated = cardDao.updateUser(cardHolderDTO, parkingCardDTO);

            if (!dbUpdated) {
                this.setCursor(Cursor.getDefaultCursor());
                showError("Lỗi: Không thể cập nhật Database.");
                return;
            }

            boolean cardTextUpdated = cardService.setUserInfo(cardHolderDTO.getFullName(), parkingCardDTO.getLicensePlate(), parkingCardDTO.getVehicleType());
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
        if (!connected) {
            return;
        }
        JPasswordField passwordField = new JPasswordField();
        int option = JOptionPane.showConfirmDialog(
                this,
                passwordField,
                "Thẻ đang bị KHÓA (BLOCKED)!\nVui lòng nhập mã PUK để mở khóa thẻ:",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (option != JOptionPane.OK_OPTION)
            return;
        String puk = new String(passwordField.getPassword());

        if (cardService.unblockCard(puk)) {
            showSuccess("Mở khóa thẻ thành công!\nSố lần thử PIN đã được reset.");
            notifyStatus("● Thẻ: Online (Active)", SUCCESS_COLOR);
        } else {
            showError("Mở khóa thất bại! Mã PUK không chính xác.");
        }
        cardService.disconnect();
    }

    private void logicCheckCard() {
        boolean connected = performCardHandshake();
        if (!connected) {
            btnChangePin.setEnabled(false);
        } else {
            showSuccess("Thẻ đã kết nối! Bạn có thể đổi PIN.");
            btnChangePin.setEnabled(true);
        }
    }

    private void logicChangePin() {
        String newPin = new String(txtNewPin.getPassword());
        String confirmPin = new String(txtConfirmPin.getPassword());

        if (newPin.isEmpty() || confirmPin.isEmpty()) {
            showError("Vui lòng điền đầy đủ thông tin PIN!");
            return;
        }
        if (!newPin.equals(confirmPin)) {
            showError("PIN mới không khớp!");
            return;
        }
        if (newPin.length() != 6) {
            showError("PIN phải có 6 ký tự !");
            return;
        }

        try {
            String cardUID = cardService.getCardID();
            if (!performSecureLogin(cardUID,"đổi mật khẩu")) {
                return;
            }
            cardService.changePin(newPin);
            showSuccess("Đổi PIN thành công!\nKhóa bảo mật và PIN đã được cập nhật.");
            txtNewPin.setText("");
            txtConfirmPin.setText("");
            btnChangePin.setEnabled(false);
        } catch (Exception e) {
            showError(e.getMessage());
        } finally {
            notifyStatus("Thẻ: chưa kết nối", Color.WHITE);
            cardService.disconnect();
        }
    }

    private boolean validateInputs() {
        if (txtOwnerName.getText().trim().isEmpty()) { showError("Tên chủ sở hữu không được để trống!"); return false; }
        if (txtPhone.getText().trim().isEmpty()) {showError("Số điện thoại không được để trống!");txtPhone.requestFocus();return false;}
        if (!txtPhone.getText().trim().matches("^(0|84)(\\d{9})$")) { showError("Số điện thoại phải 10 số và bắt đầu bằng số 0!"); return false; }
        if (txtIdentityCard.getText().trim().isEmpty()) {showError("CMND/CCCD không được để trống!");txtIdentityCard.requestFocus();return false;}
        if (!txtIdentityCard.getText().trim().matches("^\\d{12}$")) { showError("CCCD phải là 12 số!"); return false; }
        if (!txtLicensePlate.getText().trim().matches("^[0-9]{2}[A-Z][0-9][0-9]{5}$")) { showError("Biển số sai định dạng (ví dụ:37A123456)!"); return false; }
        if (selectedAvatarFile == null || !selectedAvatarFile.exists()) { showError("Chưa chọn ảnh!"); return false; }
        if (txtPin.getText().trim().length() != 6) { showError("PIN phải 6 ký tự!"); return false; }
        return true;
    }

    private boolean validateUpdateInputs() {
        if (txtOwnerName.getText().trim().isEmpty()) { showError("Tên chủ sở hữu không được để trống!"); return false; }
        if (txtPhone.getText().trim().isEmpty()) {showError("Số điện thoại không được để trống!");txtPhone.requestFocus();return false;}
        if (!txtPhone.getText().trim().matches("^(0|84)(\\d{9})$")) { showError("Số điện thoại phải 10 số và bắt đầu bằng số 0!"); return false; }
        if (txtIdentityCard.getText().trim().isEmpty()) {showError("CMND/CCCD không được để trống!");txtIdentityCard.requestFocus();return false;}
        if (!txtIdentityCard.getText().trim().matches("^\\d{12}$")) { showError("CCCD phải là 12 số!"); return false; }
        if (!txtLicensePlate.getText().trim().matches("^[0-9]{2}[A-Z][0-9][0-9]{5}$")) { showError("Biển số sai định dạng (ví dụ:37A123456)!"); return false; }
        return true;
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
        notifyStatus("Thẻ: Chưa kết nối", Color.WHITE);
    }
}
