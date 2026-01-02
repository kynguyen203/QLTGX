package org.example.component;

import org.example.CardService;
import org.example.database.DAO;
import org.example.util.EnvKeyLoader;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public abstract class BasePanel extends JPanel {
    protected static final Color PRIMARY_COLOR = new Color(41, 128, 185);
    protected static final Color ACCENT_COLOR = new Color(52, 152, 219);
    protected static final Color SUCCESS_COLOR = new Color(46, 204, 113);
    protected static final Color WARNING_COLOR = new Color(241, 196, 15);
    protected static final Color DANGER_COLOR = new Color(231, 76, 60);
    protected static final Color LIGHT_BG = new Color(236, 240, 241);
    protected static final Color CARD_BG = Color.WHITE;
    protected static final Color TEXT_PRIMARY = new Color(44, 62, 80);

    protected CardService cardService;
    protected DAO cardDao;
    protected EnvKeyLoader keyManager;
    protected StatusListener statusListener;

    public interface StatusListener {
        void updateStatus(String message, Color color);

        void updateBalance(int balance);
    }

    public BasePanel(CardService cardService, DAO cardDao, EnvKeyLoader keyManager,
                     StatusListener statusListener) {
        this.cardService = cardService;
        this.cardDao = cardDao;
        this.keyManager = keyManager;
        this.statusListener = statusListener;
        setBackground(LIGHT_BG);
    }

    protected void notifyStatus(String message, Color color) {
        if (statusListener != null) {
            statusListener.updateStatus(message, color);
        }
    }

    protected void notifyBalance(int balance) {
        if (statusListener != null) {
            statusListener.updateBalance(balance);
        }
    }

    protected boolean performCardHandshake() {
        if (!cardService.connect()) {
            showError("Không tìm thấy thẻ hoặc thẻ không hợp lệ!");
            notifyStatus("Thẻ: Offline", Color.WHITE);
            return false;
        }
        try {
            int status = cardService.checkCardStatus();
            if (status == 0) {
                showError("Thẻ trắng!\nVui lòng đăng ký thẻ trước.");
                notifyStatus("Thẻ: Offline", Color.WHITE);
                cardService.disconnect();
                return false;
            } else if (status == -1) {
                showError("Lỗi đọc trạng thái thẻ (Unknown Status).");
                notifyStatus("Thẻ: Offline", Color.WHITE);
                cardService.disconnect();
                return false;
            }

            String cardUID = cardService.getCardID();
            if (cardUID == null || cardUID.isEmpty()) {
                showError("Lỗi: Không đọc được định danh thẻ (UID)!");
                notifyStatus("Thẻ: Offline", Color.WHITE);
                cardService.disconnect();
                return false;
            }

            notifyStatus("Thẻ: Online: " + cardUID, SUCCESS_COLOR);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            showError("Lỗi trong quá trình kết nối thẻ: " + e.getMessage());
            notifyStatus("Thẻ: Offline", Color.WHITE);
            cardService.disconnect();
            return false;
        }
    }

    protected boolean verifyCardSignature(String cardUID) {
        try {
            byte[] nonce = new byte[16];
            new SecureRandom().nextBytes(nonce);
            byte[] signature = cardService.authenticateCard(nonce);
            String pubKeyBase64 = cardDao.getCardPublicKey(cardUID);
            if (signature == null || pubKeyBase64 == null)
                return false;

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

    protected boolean performSecureLogin(String cardUID, String action) {
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
            try {
                int sw = cardService.verifyPin(pin);
                if (sw == CardService.SW_SUCCESS) {
                    if (verifyCardSignature(cardUID)) {
                        return true;
                    } else {
                        showError("CẢNH BÁO CỰC KỲ NGHIÊM TRỌNG:\nPhát hiện thẻ giả mạo (Clone)!");
                        return false;
                    }
                } else if (sw == CardService.SW_CARD_BLOCKED) {
                    showError("Thẻ đang bị KHÓA! Vui lòng liên hệ admin để mở khóa.");
                    return false;
                } else if ((sw & 0xFFF0) == 0x63C0) {
                    int retries = sw & 0x0F;
                    showError("Mã PIN sai! Số lần thử còn lại: " + retries);
                } else if (sw == CardService.SW_CARD_NOT_SETUP) {
                    showError("Thẻ chưa được cài đặt (Thẻ trắng).");
                    return false;
                } else {
                    showError("Mất kêt nối với thẻ.");
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                showError("Lỗi hệ thống: " + e.getMessage());
                return false;
            }
        }
    }

    protected JPanel createCard(String title) {
        JPanel card = new JPanel();
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
                new EmptyBorder(20, 20, 20, 20)));
        TitledBorder titledBorder = BorderFactory.createTitledBorder(
                BorderFactory.createEmptyBorder(), title, TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 16), PRIMARY_COLOR);
        card.setBorder(BorderFactory.createCompoundBorder(titledBorder, new EmptyBorder(10, 15, 15, 15)));
        return card;
    }

    protected JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        label.setForeground(TEXT_PRIMARY);
        return label;
    }

    protected JLabel createInfoLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        label.setForeground(TEXT_PRIMARY);
        return label;
    }

    protected JTextField createStyledTextField() {
        JTextField field = new JTextField();
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setPreferredSize(new Dimension(0, 35));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                new EmptyBorder(5, 10, 5, 10)));
        return field;
    }

    protected JPasswordField createStyledPasswordField() {
        JPasswordField field = new JPasswordField();
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setPreferredSize(new Dimension(0, 35));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                new EmptyBorder(5, 10, 5, 10)));
        return field;
    }

    protected JButton createPrimaryButton(String text) {
        return createStyledButton(text, PRIMARY_COLOR, Color.WHITE);
    }

    protected JButton createSecondaryButton(String text) {
        return createStyledButton(text, LIGHT_BG, TEXT_PRIMARY);
    }

    protected JButton createSuccessButton(String text) {
        return createStyledButton(text, SUCCESS_COLOR, Color.WHITE);
    }

    protected JButton createWarningButton(String text) {
        return createStyledButton(text, WARNING_COLOR, Color.WHITE);
    }

    protected JButton createDangerButton(String text) {
        return createStyledButton(text, DANGER_COLOR, Color.WHITE);
    }

    protected JButton createStyledButton(String text, Color bg, Color fg) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setBackground(bg);
        button.setForeground(fg);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setPreferredSize(new Dimension(150, 35));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setBackground(bg.darker());
            }

            public void mouseExited(MouseEvent e) {
                button.setBackground(bg);
            }
        });
        return button;
    }

    protected void styleTable(JTable table) {
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

    protected String formatCurrency(double amount) {
        return String.format("%,.0f VNĐ", amount);
    }

    protected void showSuccess(String message) {
        JOptionPane.showMessageDialog(this, message, "Thành công", JOptionPane.INFORMATION_MESSAGE);
    }

    protected void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Lỗi", JOptionPane.ERROR_MESSAGE);
    }
}
