package org.example.database;

import org.example.database.DTO.CardHolderDTO;
import org.example.database.DTO.ParkingCardDTO;
import org.example.database.DTO.UserDTO;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class CardHolderDAO {
    /**
     * Transaction tổng hợp: Đăng ký người dùng và thẻ
     * (Đây là hàm duy nhất UI cần gọi để thực hiện logic đăng ký)
     */
    public boolean registerUserTransaction(CardHolderDTO cardHolderDTO, ParkingCardDTO parkingCardDTO) {
        Connection conn = null;
        try {
            conn = DatabaseHelper.getConnection();
            conn.setAutoCommit(false); // Bắt đầu Transaction

            // 1. Tìm xem người này đã tồn tại chưa (dựa theo CMND/CCCD)
            int holderId = findHolderByIdentity(conn, cardHolderDTO.getIdentityCard());

            if (holderId != -1) {
                // --- TRƯỜNG HỢP 1: NGƯỜI CŨ (UPDATE) ---
                String currentPhone = getPhoneNumberById(conn, holderId);

                // Nếu SĐT thay đổi hoặc có ảnh mới -> Cần update
                boolean phoneChanged = !currentPhone.equals(cardHolderDTO.getPhoneNumber());
                boolean imageUpdated = (cardHolderDTO.getAvatarImage() != null);

                if (phoneChanged) {
                    // Nếu đổi SĐT, phải chắc chắn SĐT mới chưa bị ai khác dùng
                    if (isPhoneNumberExists(conn, cardHolderDTO.getPhoneNumber())) {
                        throw new RuntimeException("Số điện thoại mới này đang được người khác sử dụng!");
                    }
                }

                if (phoneChanged || imageUpdated) {
                    updateCardHolder(conn, holderId, cardHolderDTO);
                }
            } else {
                // --- TRƯỜNG HỢP 2: NGƯỜI MỚI (INSERT) ---
                // Kiểm tra SĐT trước khi insert
                if (isPhoneNumberExists(conn, cardHolderDTO.getPhoneNumber())) {
                    throw new RuntimeException("Số điện thoại này đã được đăng ký bởi CMND/CCCD khác!");
                }

                holderId = createCardHolder(conn, cardHolderDTO);
            }

            // 2. Tạo thẻ mới gắn vào HolderID
            createParkingCard(conn, holderId, parkingCardDTO);

            conn.commit(); // Thành công -> Commit
            return true;

        } catch (RuntimeException re) {
            if (conn != null)
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                }
            throw re; // Ném tiếp lỗi logic (trùng SĐT) ra UI
        } catch (Exception e) {
            if (conn != null)
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                }
            e.printStackTrace();
            return false; // Lỗi hệ thống
        } finally {
            if (conn != null)
                try {
                    conn.close();
                } catch (SQLException e) {
                }
        }
    }

    // ==================== CÁC HÀM CRUD CƠ BẢN (PRIVATE / HELPER)
    // ====================

    /**
     * Tìm ID chủ thẻ dựa trên CCCD/CMND
     */
    private int findHolderByIdentity(Connection conn, String identityCard) throws SQLException {
        String sql = "SELECT HolderID FROM CardHolders WHERE IdentityCard = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, identityCard);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getInt("HolderID");
            }
        }
        return -1;
    }

    /**
     * Lấy số điện thoại hiện tại của chủ thẻ
     */
    private String getPhoneNumberById(Connection conn, int holderId) throws SQLException {
        String sql = "SELECT PhoneNumber FROM CardHolders WHERE HolderID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, holderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getString("PhoneNumber");
            }
        }
        return null;
    }

    /**
     * Kiểm tra số điện thoại đã tồn tại trong hệ thống chưa
     */
    private boolean isPhoneNumberExists(Connection conn, String phone) throws SQLException {
        String sql = "SELECT COUNT(*) FROM CardHolders WHERE PhoneNumber = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, phone);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    /**
     * Tạo mới chủ thẻ (CardHolder)
     */
    private int createCardHolder(Connection conn, CardHolderDTO cardHolderDTO) throws SQLException {
        String sql = "INSERT INTO CardHolders (FullName, PhoneNumber, IdentityCard, AvatarImage) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, cardHolderDTO.getFullName());
            ps.setString(2, cardHolderDTO.getPhoneNumber());
            ps.setString(3, cardHolderDTO.getIdentityCard());

            if (cardHolderDTO.getAvatarImage() != null && cardHolderDTO.getAvatarImage().exists()) {
                try {
                    ps.setBytes(4, readFileToBytes(cardHolderDTO.getAvatarImage()));
                } catch (IOException e) {
                    ps.setNull(4, Types.BLOB);
                }
            } else {
                ps.setNull(4, Types.BLOB);
            }

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next())
                    return rs.getInt(1);
            }
        }
        throw new SQLException("Không thể tạo chủ thẻ mới.");
    }

    /**
     * Cập nhật thông tin chủ thẻ
     */
    private void updateCardHolder(Connection conn, int holderId, CardHolderDTO cardHolderDTO) throws SQLException {
        boolean hasImage = cardHolderDTO.getAvatarImage() != null && cardHolderDTO.getAvatarImage().exists();
        String sql = hasImage
                ? "UPDATE CardHolders SET FullName = ?, PhoneNumber = ?, AvatarImage = ? WHERE HolderID = ?"
                : "UPDATE CardHolders SET FullName = ?, PhoneNumber = ? WHERE HolderID = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cardHolderDTO.getFullName());
            ps.setString(2, cardHolderDTO.getPhoneNumber());

            if (hasImage) {
                try {
                    ps.setBytes(3, readFileToBytes(cardHolderDTO.getAvatarImage()));
                    ps.setInt(4, holderId);
                } catch (IOException e) {
                    throw new SQLException("Lỗi đọc file ảnh: " + e.getMessage());
                }
            } else {
                ps.setInt(3, holderId);
            }

            ps.executeUpdate();
        }
    }

    /**
     * Tạo thẻ gửi xe mới (ParkingCard)
     */
    private void createParkingCard(Connection conn, int holderId, ParkingCardDTO parkingCardDTO) throws SQLException {
        String sql = "INSERT INTO ParkingCards (HolderID, CardUID, LicensePlate, VehicleType, PublicKey, Balance) VALUES (?, ?, ?, ?, ?, 0)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, holderId);
            ps.setString(2, parkingCardDTO.getCardUID());
            ps.setString(3, parkingCardDTO.getLicensePlate());
            ps.setString(4, parkingCardDTO.getVehicleType());
            ps.setString(5, parkingCardDTO.getPublicKey());
            ps.executeUpdate();
        }
    }

    // ==================== CÁC HÀM CÔNG KHAI KHÁC (UPDATE, QUERY)
    // ====================

    /**
     * Kiểm tra thông tin đăng ký (Dùng cho Pre-check UI)
     */
    public void validateRegisterInfo(String phone, String identityCard) throws SQLException {
        try (Connection conn = DatabaseHelper.getConnection()) {
            int holderId = findHolderByIdentity(conn, identityCard);

            if (holderId != -1) {
                // Người cũ: Kiểm tra nếu đổi SĐT thì SĐT mới có bị trùng không
                String currentPhone = getPhoneNumberById(conn, holderId);
                if (!currentPhone.equals(phone)) {
                    if (isPhoneNumberExists(conn, phone)) {
                        throw new SQLException("Số điện thoại đã được người khác sử dụng!");
                    }
                }
            } else {
                // Người mới: Kiểm tra SĐT có bị trùng không
                if (isPhoneNumberExists(conn, phone)) {
                    throw new SQLException("Số điện thoại đã được đăng ký bởi người khác!");
                }
            }
        }
    }

    public boolean updateUser(CardHolderDTO cardHolderDTO, ParkingCardDTO parkingCardDTO) {
        String sqlFindHolder = "SELECT HolderID FROM ParkingCards WHERE CardUID = ?";
        String sqlUpdateCard = "UPDATE ParkingCards SET LicensePlate = ?, VehicleType = ? WHERE CardUID = ?";

        Connection conn = null;
        try {
            conn = DatabaseHelper.getConnection();
            conn.setAutoCommit(false);

            // 1. Tìm HolderID từ CardUID
            int holderId = -1;
            try (PreparedStatement psFind = conn.prepareStatement(sqlFindHolder)) {
                psFind.setString(1, parkingCardDTO.getCardUID());
                try (ResultSet rs = psFind.executeQuery()) {
                    if (rs.next())
                        holderId = rs.getInt("HolderID");
                }
            }

            if (holderId == -1)
                throw new SQLException("Không tìm thấy thẻ với UID: " + parkingCardDTO.getCardUID());

            // 2. Cập nhật thông tin chủ thẻ
            // (Lưu ý: Logic này tương tự updateCardHolder nhưng public và quản lý conn
            // riêng)
            updateCardHolder(conn, holderId, cardHolderDTO);

            // 3. Cập nhật thông tin thẻ
            try (PreparedStatement psCard = conn.prepareStatement(sqlUpdateCard)) {
                psCard.setString(1, parkingCardDTO.getLicensePlate());
                psCard.setString(2, parkingCardDTO.getVehicleType());
                psCard.setString(3, parkingCardDTO.getCardUID());
                psCard.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (Exception e) {
            if (conn != null)
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null)
                try {
                    conn.close();
                } catch (SQLException e) {
                }
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
                        rs.getBigDecimal("Balance"));
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
                return new String[] {
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
        String sql = "INSERT INTO TransactionLogs (CardID, TransactionType, Amount, DigitalSignature, TransactionTime) "
                +
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
        if (isCarParked(cardUID))
            return false;

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
            if (rs.next())
                return rs.getInt(1) > 0;
        } catch (Exception e) {
        }
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

                parkedList.add(new String[] { displayCar, displayTime });
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

        try (Connection conn = DatabaseHelper.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, cardUID);
            ResultSet rs = ps.executeQuery();
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss dd/MM/yyyy");
            java.text.NumberFormat nf = java.text.NumberFormat.getInstance(java.util.Locale.US);
            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("TransactionTime");
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
                historyList.add(new String[] { timeDisplay, typeDisplay, amountDisplay, status });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return historyList;
    }

    public List<String[]> getParkingSessionHistory(String cardUID) {
        List<String[]> sessionList = new ArrayList<>();
        String sql = "SELECT s.CheckInTime, s.CheckOutTime, c.LicensePlate, s.FeeAmount, s.Status " +
                "FROM ParkingSessions s " +
                "JOIN ParkingCards c ON s.CardID = c.CardID " +
                "WHERE c.CardUID = ? " +
                "ORDER BY s.CheckInTime DESC";

        try (Connection conn = DatabaseHelper.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, cardUID);
            ResultSet rs = ps.executeQuery();

            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm dd/MM/yyyy");
            java.text.NumberFormat nf = java.text.NumberFormat.getInstance(java.util.Locale.US);

            while (rs.next()) {
                Timestamp inTime = rs.getTimestamp("CheckInTime");
                Timestamp outTime = rs.getTimestamp("CheckOutTime");
                String license = rs.getString("LicensePlate");
                java.math.BigDecimal feeBg = rs.getBigDecimal("FeeAmount");
                String statusRaw = rs.getString("Status");

                String strIn = (inTime != null) ? sdf.format(inTime) : "";
                String strOut = (outTime != null) ? sdf.format(outTime) : "---";
                String strFee = (feeBg != null && feeBg.longValue() > 0) ? nf.format(feeBg) + " đ" : "0 đ";

                String strStatus = statusRaw;
                if ("PARKED".equalsIgnoreCase(statusRaw))
                    strStatus = "Đang gửi";
                else if ("COMPLETED".equalsIgnoreCase(statusRaw))
                    strStatus = "Đã ra";

                sessionList.add(new String[] { strIn, strOut, license, strFee, strStatus });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sessionList;
    }

    // --- HELPER METHODS ---
    private byte[] readFileToBytes(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
                ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }
            return bos.toByteArray();
        }
    }

//    private void closeResources(Connection conn, Statement... statements) {
//        for (Statement stmt : statements) {
//            if (stmt != null)
//                try {
//                    stmt.close();
//                } catch (SQLException e) {
//                }
//        }
//        if (conn != null)
//            try {
//                conn.close();
//            } catch (SQLException e) {
//            }
//    }
    public boolean rollbackRegistration(String cardUID) {
        String sqlDeleteCard = "DELETE FROM ParkingCards WHERE CardUID = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlDeleteCard)) {

            ps.setString(1, cardUID);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
