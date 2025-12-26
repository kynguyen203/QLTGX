package org.example.component;

import org.example.CardService;
import org.example.database.CardHolderDAO;
import org.example.util.EnvKeyLoader;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.security.PrivateKey;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class ParkingPanel extends BasePanel {

    private JTable parkingTable;
    private DefaultTableModel parkingTableModel;

    public ParkingPanel(CardService cardService, CardHolderDAO cardDao, EnvKeyLoader keyManager,
            StatusListener statusListener) {
        super(cardService, cardDao, keyManager, statusListener);
        setLayout(new GridLayout(1, 2, 15, 0));
        setBorder(new EmptyBorder(20, 20, 20, 20));

        initUI();
        loadParkingData();
    }

    private void initUI() {
        add(createEntryCard());
        add(createExitCard());
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

        parkingTableModel = new DefaultTableModel(new Object[] { "Biển số xe", "Thời gian vào" }, 0);
        parkingTable = new JTable(parkingTableModel);
        styleTable(parkingTable);

        JScrollPane scrollPane = new JScrollPane(parkingTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
        card.add(scrollPane, BorderLayout.CENTER);

        return card;
    }

    private JPanel createExitCard() {
        JPanel card = createCard("CỔNG RA");
        card.setLayout(new BorderLayout(0, 15));

        JPanel inputPanel = new JPanel(new BorderLayout(10, 0));
        inputPanel.setOpaque(false);
        inputPanel.setBorder(new EmptyBorder(0, 0, 15, 0));

        JButton btnProcessExit = createSuccessButton("Quẹt thẻ");
        btnProcessExit.setPreferredSize(new Dimension(120, 40));
        btnProcessExit.addActionListener(e -> logicProcessExit());

        card.add(btnProcessExit, BorderLayout.NORTH);

        return card;
    }

    private void logicVehicleEntry() {
        performCardHandshake();

        try {
            String cardUID = cardService.getCardID();
            if (!performSecureLogin(cardUID,"xe vào bến")) {
                cardService.disconnect();
                notifyStatus("Thẻ: Offline", Color.WHITE);
                return;
            }
            String info = cardService.getUserInfo();
            if (info == null || info.isEmpty()) {
                showError("Không có thông tin người dùng trên thẻ!");
                return;
            }
            Map<String, String> userMap = cardService.getUserInfoMap(info);
            if (userMap != null) {
                if (cardDao.isCarParked(cardUID)) {
                    showError("Thẻ này đang có xe trong bãi! (Chưa check-out)");
                    return;
                }
                if (cardDao.checkIn(cardUID)) {
                    loadParkingData();
                    showSuccess("Mời xe vào!\nBiển số: " + userMap.get("license"));
                    notifyStatus("Thẻ: Online: " + cardUID, SUCCESS_COLOR);
                } else {
                    showError("Lỗi Check-in Database!");
                }
            }
            cardService.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void logicProcessExit() {
        performCardHandshake();
        try {
            String cardUID = cardService.getCardID();
            if (!performSecureLogin(cardUID,"xe vào bến")) {
                cardService.disconnect();
                notifyStatus("Thẻ: Offline", Color.WHITE);
                return;
            }
            long inTime = cardDao.getCheckInTime(cardUID);
            if (inTime == 0) {
                showError("Thẻ này chưa Check-in (Xe không có trong bãi)!");
                return;
            }

            String[] cardInfo = cardDao.getCardInfoByUID(cardUID);
            String licensePlate = (cardInfo != null) ? cardInfo[2] : "Unknown";
            String vehicleType = (cardInfo != null) ? cardInfo[3] : "Xe máy";

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
            if (blocks < 1)
                blocks = 1;

            int totalFee = (int) (blocks * unitPrice);

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

            PrivateKey hostPrivKey = keyManager.getPrivateKey();
            if (hostPrivKey == null) {
                showError("Lỗi hệ thống: Không tìm thấy Host Key!");
                return;
            }

            String signature = cardService.pay(totalFee, hostPrivKey);

            if (signature == null) {
                showError("Thanh toán thất bại! Lỗi xử lý Chip.");
                return;
            }

            cardDao.saveTransaction(cardUID, "PAYMENT", totalFee, signature);

            int newBalance = cardService.getBalance();
            cardDao.updateCardBalance(cardUID, newBalance);
            cardDao.checkOut(cardUID, totalFee);

            // Notify balance update
            notifyBalance(newBalance);

            loadParkingData();

            String successMsg = "<html><div style='width: 250px; text-align: center; font-family: Segoe UI;'>"
                    + "<h2 style='color: #27ae60;'>XE RA THÀNH CÔNG!</h2>"
                    + "<hr>"
                    + "Biển số: <b>" + licensePlate + "</b><br>"
                    + "Thời gian: " + durationSeconds + " giây<br>"
                    + "--------------------------------<br>"
                    + "Phí: <b style='color: red; font-size: 14px;'>" + formatCurrency(totalFee) + "</b><br>"
                    + "Số dư còn lại: <b>" + formatCurrency(newBalance) + "</b>"
                    + "</div></html>";

            JOptionPane.showMessageDialog(this, successMsg, "Hoàn tất", JOptionPane.INFORMATION_MESSAGE);
            notifyStatus("Xe ra: " + licensePlate, SUCCESS_COLOR);

        } catch (Exception e) {
            e.printStackTrace();
            showError("Lỗi hệ thống: " + e.getMessage());
        } finally {
            cardService.disconnect();
        }
    }

    private void loadParkingData() {
        if (parkingTableModel == null)
            return;
        parkingTableModel.setRowCount(0);
        java.util.List<String[]> parkedCars = cardDao.getParkedCars();
        if (parkedCars != null) {
            for (String[] car : parkedCars) {
                parkingTableModel.addRow(car);
            }
        }
    }
}
