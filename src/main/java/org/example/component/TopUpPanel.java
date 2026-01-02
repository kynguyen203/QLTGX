package org.example.component;

import org.example.CardService;
import org.example.database.DAO;
import org.example.util.EnvKeyLoader;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class TopUpPanel extends BasePanel {

    private JTextField txtTopUpAmount;
    private JLabel lblTopUpCardInfo;
    private JButton btnTopUp;
    private JLabel lblBalance;

    public TopUpPanel(CardService cardService, DAO cardDao, EnvKeyLoader keyManager,
                      StatusListener statusListener) {
        super(cardService, cardDao, keyManager, statusListener);
        setLayout(new GridBagLayout());
        setBorder(new EmptyBorder(50, 50, 50, 50));

        initUI();
    }

    private void initUI() {
        JPanel card = createCard("Nạp tiền vào thẻ");
        card.setLayout(new GridBagLayout());
        card.setPreferredSize(new Dimension(500, 750));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 20, 15, 20);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        JButton btnCheckCard = createWarningButton("Quẹt thẻ");
        btnCheckCard.addActionListener(e -> logicCheckCardForTopUp());
        card.add(btnCheckCard, gbc);

        gbc.gridy = 1;
        lblTopUpCardInfo = new JLabel("Vui lòng quẹt thẻ...", SwingConstants.CENTER);
        lblTopUpCardInfo.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        lblTopUpCardInfo.setPreferredSize(new Dimension(450, 250));
        lblTopUpCardInfo.setForeground(Color.GRAY);
        card.add(lblTopUpCardInfo, gbc);

        lblBalance = new JLabel("Số dư: 0 VNĐ"); // Local display
        lblBalance.setVisible(false); // Maybe show it in the info label instead

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
        String[] amounts = { "50,000", "100,000", "200,000", "500,000" };
        for (String amount : amounts) {
            JButton btn = createSecondaryButton(amount);
            btn.addActionListener(e -> txtTopUpAmount.setText(amount.replace(",", "")));
            quickPanel.add(btn);
        }
        card.add(quickPanel, gbc);

        gbc.gridy = 5;
        gbc.insets = new Insets(25, 20, 15, 20);
        btnTopUp = createSuccessButton("Nạp tiền vào thẻ");
        btnTopUp.setEnabled(false);
        btnTopUp.setPreferredSize(new Dimension(0, 45));
        btnTopUp.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnTopUp.addActionListener(e -> logicTopUp());
        card.add(btnTopUp, gbc);

        add(card);
    }

    private void logicCheckCardForTopUp() {
        boolean connect = performCardHandshake();
        if (!connect) {
            lblTopUpCardInfo.setText("Vui lòng quẹt thẻ...");
            lblTopUpCardInfo.setIcon(null);
            return;
        }
        try {
            String cardUID = cardService.getCardID();
            if (cardUID == null || cardUID.isEmpty()) {
                showError("Thẻ này chưa được định danh (Trống ID)!");
                return;
            }

            int chipBalance = cardService.getBalance();
            if (chipBalance == -1) {
                showError("Lỗi đọc thẻ (Applet chưa sẵn sàng)!");
                return;
            }
            String[] info = cardDao.getCardInfoByUID(cardUID);
            if (info != null) {
                String name = info[0];
                String plate = info[2];

                String infoHtml = "<html><div style='text-align: center; width: 300px;'>"
                        + "<b style='font-size:12px; color:#2980b9'>THÔNG TIN THẺ</b><br>"
                        + "--------------------------------<br>"
                        + "UID: <b>" + cardUID + "</b><br>"
                        + "Chủ xe: <b>" + name + "</b><br>"
                        + "Biển số: <b>" + plate + "</b><br>"
                        + "Số dư: <span style='color:red; font-size:14px'>" + formatCurrency(chipBalance) + "</span>"
                        + "</div></html>";
                        lblTopUpCardInfo.setText(infoHtml);
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
            if (amount <= 0)
                throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            showError("Số tiền nạp không hợp lệ!");
            return;
        }

        try {
            String cardUID = cardService.getCardID();
            if (!performSecureLogin(cardUID,"nạp tiền")) {
                cardService.disconnect();
                lblTopUpCardInfo.setIcon(null);
                lblTopUpCardInfo.setText("Vui lòng quẹt thẻ...");
                txtTopUpAmount.setText("");
                btnTopUp.setEnabled(false);
                return;
            }
            PrivateKey hostPrivKey = keyManager.getPrivateKey();
            String pubKeyBase64 = cardDao.getCardPublicKey(cardUID);
            if (pubKeyBase64 == null) {
                showError("Thẻ chưa đăng ký khóa công khai trong hệ thống!");
                return;
            }
            byte[] keyBytes = Base64.getDecoder().decode(pubKeyBase64);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            PublicKey cardPubKey = KeyFactory.getInstance("RSA").generatePublic(spec);
            if (hostPrivKey == null || cardPubKey == null) {
                showError("Lỗi hệ thống: Thiếu khóa bảo mật!");
                return;
            }
            this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            String cardSignature = cardService.topUp(amount, hostPrivKey, cardPubKey);
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
                    lblBalance.setText("Số dư: " + String.format("%,d", newBalance) + " VND");
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
            notifyStatus("Thẻ: chưa kết nối", Color.WHITE);
            cardService.disconnect();
        }
    }
}
