package org.example;

import org.example.component.*;
import org.example.component.BasePanel.StatusListener;
import org.example.database.DAO;
import org.example.util.EnvKeyLoader;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ModernParkingCardUI extends JFrame {

    private static final Color PRIMARY_COLOR = new Color(41, 128, 185);
    private static final Color WARNING_COLOR = new Color(241, 196, 15);
    private static final Color LIGHT_BG = new Color(236, 240, 241);
    private static final Color CARD_BG = Color.WHITE;

    private JLabel lblCardStatus;
    private JLabel lblBalance;

    private CardService cardService;
    private DAO cardDao;
    private EnvKeyLoader keyManager;

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

        cardService = new CardService();
        cardDao = new DAO();
        keyManager = new EnvKeyLoader();

        StatusListener statusListener = new StatusListener() {
            @Override
            public void updateStatus(String message, Color color) {
                lblCardStatus.setText(message);
                lblCardStatus.setForeground(color);
            }

            @Override
            public void updateBalance(int balance) {
                lblBalance.setText("Số dư: " + formatCurrency(balance));
            }
        };

        JTabbedPane tabbedPane = createStyledTabbedPane();
        tabbedPane.addTab("  Quản lý Thẻ  ", new ManagementPanel(cardService, cardDao, keyManager, statusListener));
        tabbedPane.addTab("  Nạp Tiền  ", new TopUpPanel(cardService, cardDao, keyManager, statusListener));
        tabbedPane.addTab("  Bãi Xe  ", new ParkingPanel(cardService, cardDao, keyManager, statusListener));
        tabbedPane.addTab("  Lịch sử giao dịch  ", new HistoryPanel(cardService, cardDao, keyManager, statusListener));
        tabbedPane.addTab(" Lịch sử ra vào ",
                new ParkingSessionHistoryPanel(cardService, cardDao, keyManager, statusListener));

        add(tabbedPane, BorderLayout.CENTER);

        add(createStatusBar(), BorderLayout.SOUTH);
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

    private String formatCurrency(double amount) {
        return String.format("%,.0f VNĐ", amount);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            ModernParkingCardUI app = new ModernParkingCardUI();
            app.setVisible(true);
        });
    }
}