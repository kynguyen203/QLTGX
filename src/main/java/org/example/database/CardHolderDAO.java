package org.example.database;

import org.example.database.DTO.UserDTO;

import java.io.FileInputStream;
import java.sql.*;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class CardHolderDAO {
    public void validateRegisterInfo(String phone, String identityCard) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseHelper.getConnection();

            // Kiểm tra xem Số điện thoại này đang thuộc về ai?
            String sql = "SELECT IdentityCard FROM CardHolders WHERE PhoneNumber = ?";
            ps = conn.prepareStatement(sql);
            ps.setString(1, phone);
            rs = ps.executeQuery();

            if (rs.next()) {
                String existingIdentity = rs.getString("IdentityCard");

                // Nếu SĐT đã có người dùng, kiểm tra xem có phải chính chủ không
                if (!existingIdentity.equals(identityCard)) {
                    // SĐT này thuộc về người khác (Khác CCCD) -> LỖI
                    throw new SQLException("Số điện thoại " + phone + " đã được đăng ký bởi người khác!");
                }

                // Nếu existingIdentity.equals(identityCard) -> OK (Chính chủ thêm thẻ mới)
            }

            // Nếu rs.next() là false -> SĐT chưa ai dùng -> OK (Người mới hoặc người cũ đổi số mới)

        } finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
            if (conn != null) conn.close();
        }
    }
    public boolean registerUser(UserDTO user) {

        String sqlFindHolder =
                "SELECT HolderID, PhoneNumber FROM CardHolders WHERE IdentityCard = ?";

        String sqlInsertHolder =
                "INSERT INTO CardHolders (FullName, PhoneNumber, IdentityCard, AvatarImage) " +
                        "VALUES (?, ?, ?, ?)";

        String sqlUpdatePhone =
                "UPDATE CardHolders SET PhoneNumber = ? WHERE HolderID = ?";

        String sqlInsertCard =
                "INSERT INTO ParkingCards " +
                        "(HolderID, CardUID, LicensePlate, VehicleType, PublicKey, Balance) " +
                        "VALUES (?, ?, ?, ?, ?, 0)";

        try (Connection conn = DatabaseHelper.getConnection()) {
            conn.setAutoCommit(false);

            int holderId = -1;
            String existingPhone = null;

            /* 1. Tìm Holder theo CCCD */
            try (PreparedStatement ps = conn.prepareStatement(sqlFindHolder)) {
                ps.setString(1, user.getIdentityCard());
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    holderId = rs.getInt("HolderID");
                    existingPhone = rs.getString("PhoneNumber");
                }
            }

            /* 2. Nếu CHƯA có → tạo Holder */
            if (holderId == -1) {
                try (PreparedStatement ps = conn.prepareStatement(
                        sqlInsertHolder, Statement.RETURN_GENERATED_KEYS)) {

                    ps.setString(1, user.getName());
                    ps.setString(2, user.getPhone());
                    ps.setString(3, user.getIdentityCard());

                    if (user.getImageFile() != null && user.getImageFile().exists()) {
                        ps.setBinaryStream(4,
                                new FileInputStream(user.getImageFile()),
                                (int) user.getImageFile().length());
                    } else {
                        ps.setNull(4, Types.BLOB);
                    }

                    ps.executeUpdate();
                    ResultSet rs = ps.getGeneratedKeys();
                    rs.next();
                    holderId = rs.getInt(1);
                }
            }
            /* 3. Nếu ĐÃ có → cập nhật SĐT nếu thay đổi */
            else if (!existingPhone.equals(user.getPhone())) {
                try (PreparedStatement ps = conn.prepareStatement(sqlUpdatePhone)) {
                    ps.setString(1, user.getPhone());
                    ps.setInt(2, holderId);
                    ps.executeUpdate();
                }
            }

            /* 4. GÁN THẺ MỚI cho Holder */
            try (PreparedStatement ps = conn.prepareStatement(sqlInsertCard)) {
                ps.setInt(1, holderId);
                ps.setString(2, user.getCardUID());
                ps.setString(3, user.getLicensePlate());
                ps.setString(4, user.getVehicleType());
                ps.setString(5, user.getPublicKey());
                ps.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
//    public boolean registerUser(UserDTO user) {
//
//        String sqlInsertHolder = "INSERT INTO CardHolders (FullName, PhoneNumber, IdentityCard, AvatarImage) VALUES (?, ?, ?, ?)";
//        String sqlInsertCard = "INSERT INTO ParkingCards (HolderID, CardUID, LicensePlate, VehicleType, PublicKey, Balance) VALUES (?, ?, ?, ?, ?, 0)";
//        try (Connection conn = DatabaseHelper.getConnection()) {
//            conn.setAutoCommit(false);
//
//            PreparedStatement psHolder = conn.prepareStatement(sqlInsertHolder, Statement.RETURN_GENERATED_KEYS);
//            psHolder.setString(1, user.getName());
//            psHolder.setString(2, user.getPhone());
//            psHolder.setString(3, user.getIdentityCard());
//
//            if (user.getImageFile() != null && user.getImageFile().exists()) {
//                FileInputStream fis = new FileInputStream(user.getImageFile());
//                psHolder.setBinaryStream(4, fis, (int) user.getImageFile().length());
//            } else {
//                psHolder.setNull(4, Types.BLOB);
//                System.out.println("Cảnh báo: File ảnh bị null hoặc không tồn tại!");
//            }
//            psHolder.executeUpdate();
//
//            ResultSet rs = psHolder.getGeneratedKeys();
//            int holderId = 0;
//            if (rs.next()) {
//                holderId = rs.getInt(1);
//            }
//
//            PreparedStatement psCard = conn.prepareStatement(sqlInsertCard);
//            psCard.setInt(1, holderId);
//            psCard.setString(2, user.getCardUID());
//            psCard.setString(3, user.getLicensePlate());
//            psCard.setString(4, user.getVehicleType());
//            psCard.setString(5, user.getPublicKey());
//            psCard.executeUpdate();
//
//            conn.commit();
//            return true;
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            return false;
//        }
//    }
    public boolean updateUser(UserDTO user) {
        String sqlFindHolder = "SELECT HolderID FROM ParkingCards WHERE CardUID = ?";
        String sqlUpdateHolderWithImg = "UPDATE CardHolders SET FullName = ?, PhoneNumber = ?, IdentityCard = ?, AvatarImage = ? WHERE HolderID = ?";
        String sqlUpdateHolderNoImg   = "UPDATE CardHolders SET FullName = ?, PhoneNumber = ?, IdentityCard = ? WHERE HolderID = ?";
        String sqlUpdateCard = "UPDATE ParkingCards SET LicensePlate = ?, VehicleType = ? WHERE CardUID = ?";

        Connection conn = null;
        try {
            conn = DatabaseHelper.getConnection();
            conn.setAutoCommit(false);
            int holderId = -1;
            try (PreparedStatement psFind = conn.prepareStatement(sqlFindHolder)) {
                psFind.setString(1, user.getCardUID());
                ResultSet rs = psFind.executeQuery();
                if (rs.next()) {
                    holderId = rs.getInt("HolderID");
                }
            }

            if (holderId == -1) {
                throw new SQLException("Không tìm thấy thẻ với UID: " + user.getCardUID());
            }
            PreparedStatement psHolder;
            if (user.getImageFile() != null && user.getImageFile().exists()) {
                psHolder = conn.prepareStatement(sqlUpdateHolderWithImg);
                psHolder.setString(1, user.getName());
                psHolder.setString(2, user.getPhone());
                psHolder.setString(3, user.getIdentityCard());
                FileInputStream fis = new FileInputStream(user.getImageFile());
                psHolder.setBinaryStream(4, fis, (int) user.getImageFile().length());
                psHolder.setInt(5, holderId);
            } else {
                psHolder = conn.prepareStatement(sqlUpdateHolderNoImg);
                psHolder.setString(1, user.getName());
                psHolder.setString(2, user.getPhone());
                psHolder.setString(3, user.getIdentityCard());
                psHolder.setInt(4, holderId);
            }
            psHolder.executeUpdate();
            psHolder.close();
            PreparedStatement psCard = conn.prepareStatement(sqlUpdateCard);
            psCard.setString(1, user.getLicensePlate());
            psCard.setString(2, user.getVehicleType());
            psCard.setString(3, user.getCardUID());
            psCard.executeUpdate();
            psCard.close();

            conn.commit();
            return true;

        } catch (Exception e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException e) {}
        }
    }
    public UserDTO getUserByCardID(String cardUID) {
        String sql = "SELECT h.FullName, h.PhoneNumber, h.IdentityCard, c.LicensePlate, c.VehicleType, c.Balance " +
                "FROM ParkingCards c " +
                "JOIN CardHolders h ON c.HolderID = h.HolderID " +
                "WHERE c.CardUID = ?";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, cardUID);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new UserDTO(
                        rs.getString("FullName"),
                        rs.getString("PhoneNumber"),
                        rs.getString("IdentityCard"),
                        rs.getString("LicensePlate"),
                        rs.getString("VehicleType"),
                        cardUID,
                        rs.getBigDecimal("Balance")
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String[] getCardInfoByUID(String cardUID) {
        String sql = "SELECT h.FullName, h.PhoneNumber, c.LicensePlate, c.VehicleType " +
                "FROM ParkingCards c " +
                "JOIN CardHolders h ON c.HolderID = h.HolderID " +
                "WHERE c.CardUID = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, cardUID);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new String[]{
                        rs.getString("FullName"),
                        rs.getString("PhoneNumber"),
                        rs.getString("LicensePlate"),
                        rs.getString("VehicleType")
                };
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean saveTransaction(String cardUID, String type, int amount, String signature) {
        String sql = "INSERT INTO TransactionLogs (CardID, TransactionType, Amount, DigitalSignature, TransactionTime) " +
                "VALUES ((SELECT CardID FROM ParkingCards WHERE CardUID = ?), ?, ?, ?, NOW())";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, cardUID);
            ps.setString(2, type);
            ps.setBigDecimal(3, new java.math.BigDecimal(amount));
            ps.setString(4, signature);

            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateCardBalance(String cardUID, int newBalance) {
        String sql = "UPDATE ParkingCards SET Balance = ? WHERE CardUID = ?";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBigDecimal(1, new java.math.BigDecimal(newBalance));
            ps.setString(2, cardUID);

            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean checkIn(String cardUID) {
        if (isCarParked(cardUID)) return false;

        String sql = "INSERT INTO ParkingSessions (CardID, CheckInTime, Status) " +
                "VALUES ((SELECT CardID FROM ParkingCards WHERE CardUID = ?), NOW(), 'PARKED')";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, cardUID);

            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public long getCheckInTime(String cardUID) {
        String sql = "SELECT CheckInTime FROM ParkingSessions " +
                "WHERE CardID = (SELECT CardID FROM ParkingCards WHERE CardUID = ?) " +
                "AND Status = 'PARKED'";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, cardUID);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Timestamp ts = rs.getTimestamp("CheckInTime");
                return ts.getTime();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public boolean checkOut(String cardUID, double fee) {
        String sql = "UPDATE ParkingSessions " +
                "SET CheckOutTime = NOW(), FeeAmount = ?, Status = 'COMPLETED' " +
                "WHERE CardID = (SELECT CardID FROM ParkingCards WHERE CardUID = ?) " +
                "AND Status = 'PARKED'";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBigDecimal(1, java.math.BigDecimal.valueOf(fee));
            ps.setString(2, cardUID);

            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getCardPublicKey(String cardUID) {
        String sql = "SELECT PublicKey FROM ParkingCards WHERE CardUID = ?";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, cardUID);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getString("PublicKey");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean isCarParked(String cardUID) {
        String sql = "SELECT count(*) FROM ParkingSessions " +
                "WHERE CardID = (SELECT CardID FROM ParkingCards WHERE CardUID = ?) AND Status = 'PARKED'";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cardUID);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (Exception e) {}
        return false;
    }

    public List<String[]> getParkedCars() {
        List<String[]> parkedList = new ArrayList<>();

        String sql = "SELECT c.VehicleType, c.LicensePlate, s.CheckInTime " +
                "FROM ParkingSessions s " +
                "JOIN ParkingCards c ON s.CardID = c.CardID " +
                "WHERE s.Status = 'PARKED' " +
                "ORDER BY s.CheckInTime DESC";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss dd/MM/yyyy");

            while (rs.next()) {
                String vehicleType = rs.getString("VehicleType");
                String license = rs.getString("LicensePlate");
                Timestamp checkInTime = rs.getTimestamp("CheckInTime");

                String displayCar = vehicleType + " - " + license;
                String displayTime = sdf.format(checkInTime);

                parkedList.add(new String[]{displayCar, displayTime});
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return parkedList;
    }
    public List<String[]> getTransactionHistory(String cardUID) {
        List<String[]> historyList = new ArrayList<>();
        String sql = "SELECT t.TransactionTime, t.TransactionType, t.Amount " +
                "FROM TransactionLogs t " +
                "JOIN ParkingCards c ON t.CardID = c.CardID " +
                "WHERE c.CardUID = ? " +
                "ORDER BY t.TransactionTime DESC";

        try (java.sql.Connection conn = DatabaseHelper.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, cardUID);
            java.sql.ResultSet rs = ps.executeQuery();
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy");
            NumberFormat nf = NumberFormat.getInstance(java.util.Locale.US);
            while (rs.next()) {
                java.sql.Timestamp ts = rs.getTimestamp("TransactionTime");
                String typeRaw = rs.getString("TransactionType");
                java.math.BigDecimal amountBg = rs.getBigDecimal("Amount");
                long amount = (amountBg != null) ? amountBg.longValue() : 0;
                String timeDisplay = (ts != null) ? sdf.format(ts) : "N/A";
                String typeDisplay;
                String amountDisplay = nf.format(amount) + " VNĐ";
                String status = "Thành công";
                if ("TOPUP".equalsIgnoreCase(typeRaw)) {
                    typeDisplay = "Nạp tiền";
                    amountDisplay = "+ " + amountDisplay;
                } else if ("PAYMENT".equalsIgnoreCase(typeRaw)) {
                    typeDisplay = "Thanh toán phí";
                    amountDisplay = "- " + amountDisplay;
                } else {
                    typeDisplay = typeRaw;
                }
                historyList.add(new String[]{timeDisplay, typeDisplay, amountDisplay, status});
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return historyList;
    }
    /**
     * Lấy lịch sử ra vào bãi xe của một thẻ
     */
    public java.util.List<String[]> getParkingSessionHistory(String cardUID) {
        java.util.List<String[]> sessionList = new java.util.ArrayList<>();

        // Truy vấn: Join bảng Session với bảng Card để lọc theo CardUID
        String sql = "SELECT s.CheckInTime, s.CheckOutTime, c.LicensePlate, s.FeeAmount, s.Status " +
                "FROM ParkingSessions s " +
                "JOIN ParkingCards c ON s.CardID = c.CardID " +
                "WHERE c.CardUID = ? " +
                "ORDER BY s.CheckInTime DESC"; // Mới nhất lên đầu

        try (java.sql.Connection conn = DatabaseHelper.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, cardUID);
            java.sql.ResultSet rs = ps.executeQuery();

            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm dd/MM/yyyy");
            java.text.NumberFormat nf = java.text.NumberFormat.getInstance(java.util.Locale.US);

            while (rs.next()) {
                java.sql.Timestamp inTime = rs.getTimestamp("CheckInTime");
                java.sql.Timestamp outTime = rs.getTimestamp("CheckOutTime");
                String license = rs.getString("LicensePlate");
                java.math.BigDecimal feeBg = rs.getBigDecimal("FeeAmount");
                String statusRaw = rs.getString("Status");

                // Format dữ liệu
                String strIn = (inTime != null) ? sdf.format(inTime) : "";
                String strOut = (outTime != null) ? sdf.format(outTime) : "---"; // Chưa ra
                String strFee = (feeBg != null && feeBg.longValue() > 0) ? nf.format(feeBg) + " đ" : "0 đ";

                // Dịch trạng thái sang tiếng Việt cho thân thiện
                String strStatus = statusRaw;
                if ("PARKED".equalsIgnoreCase(statusRaw)) strStatus = "Đang gửi";
                else if ("COMPLETED".equalsIgnoreCase(statusRaw)) strStatus = "Đã ra";

                sessionList.add(new String[]{strIn, strOut, license, strFee, strStatus});
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sessionList;
    }
}
