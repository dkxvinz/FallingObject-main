import javax.swing.*;
import java.awt.*;

public class Menu extends JFrame {

    public Menu() {
        setTitle("Game Menu");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // Panel สำหรับเมนู
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // ไล่สีพื้นหลัง
                Graphics2D g2d = (Graphics2D) g;
                GradientPaint gp = new GradientPaint(0, 0, Color.BLACK, 0, getHeight(), Color.DARK_GRAY);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());

                // Title ของเกม
                g.setColor(Color.WHITE);
                g.setFont(new Font("Arial", Font.BOLD, 48));
                String title = "MY GAME";
                FontMetrics fm = g.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(title)) / 2;
                g.drawString(title, x, 100);
            }
        };
        mainPanel.setLayout(new GridBagLayout());

        // ปุ่ม Play และ Exit
        JButton playButton = new JButton("Play");
        JButton exitButton = new JButton("Exit");

        playButton.setFont(new Font("Arial", Font.BOLD, 24));
        exitButton.setFont(new Font("Arial", Font.BOLD, 24));

        playButton.setBackground(new Color(50, 150, 50));
        playButton.setForeground(Color.WHITE);

        exitButton.setBackground(new Color(200, 50, 50));
        exitButton.setForeground(Color.WHITE);

        playButton.setFocusPainted(false);
        exitButton.setFocusPainted(false);

        // Action: Play → เปิดหน้าจอใหม่
        playButton.addActionListener(e -> {
            new GameClient();   // เปิดหน้าจอเกม
            dispose();          // ปิดหน้าจอเมนู
        });

        // Action: Exit → ปิดโปรแกรม
        exitButton.addActionListener(e -> System.exit(0));

        // จัด layout ปุ่ม
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 0, 10, 0);
        gbc.gridx = 0;
        gbc.gridy = 0;
        mainPanel.add(playButton, gbc);

        gbc.gridy = 1;
        mainPanel.add(exitButton, gbc);

        add(mainPanel);
        setVisible(true);
    }

    // ====== Class สำหรับหน้าจอที่ 2 (เกม) ======
    // static class GameWindow extends JFrame {
    //     public GameWindow() {
    //         setTitle("Game Window");
    //         setSize(800, 600);
    //         setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    //         setLocationRelativeTo(null);
    //         setResizable(false);

    //         // Panel สำหรับวาด "เกม"
    //         JPanel gamePanel = new JPanel() {
    //             @Override
    //             protected void paintComponent(Graphics g) {
    //                 super.paintComponent(g);
    //                 setBackground(Color.BLACK);

    //                 g.setColor(Color.GREEN);
    //                 g.fillRect(100, 100, 200, 150); // วาดสี่เหลี่ยมเขียว
    //             }
    //         };

    //         add(gamePanel);
    //         setVisible(true);
    //     }
    // }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Menu::new);
    }
}
