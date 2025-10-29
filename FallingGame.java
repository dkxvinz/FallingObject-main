import javax.swing.*;     // สำหรับ JFrame, JPanel, Timer
import java.awt.*;        // สำหรับ Graphics, Color, Font
import java.awt.event.*;  // สำหรับ KeyListener, ActionListener
import java.util.ArrayList; // สำหรับเก็บ object ที่ตกลงมา
import java.util.Random;    // สำหรับสุ่มตำแหน่ง object

// คลาสหลักของเกม
public class FallingGame extends JFrame {

    // สร้าง Panel ของเกม (พื้นที่วาด)
    private GamePanel gamePanel;

    public FallingGame() {
        setTitle("Falling Objects Game"); // ตั้งชื่อหน้าต่าง
        setSize(800, 600);                // ขนาดหน้าจอ
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // ปิดโปรแกรมเมื่อกด X
        setLocationRelativeTo(null);      // ให้อยู่กลางจอ
        setResizable(false);              // ไม่ให้ย่อขยาย
        gamePanel = new GamePanel();      // สร้าง panel เกม
        add(gamePanel);                   // ใส่ panel ลงใน frame
        setVisible(true);                 // แสดงหน้าต่าง
    }

    // คลาสย่อยที่ใช้วาดเกม
    static class GamePanel extends JPanel implements ActionListener, KeyListener {

        // ตัวละครผู้เล่น 3 คน
        private Rectangle player1, player2, player3;

        // คะแนนผู้เล่น
        private int score1 = 0, score2 = 0, score3 = 0;

        // เก็บวัตถุ (object) ที่ตกลงมา
        private ArrayList<Rectangle> objects;


        // ตัวจับเวลา (เกม loop)
        private Timer timer;

        // สำหรับสุ่มตำแหน่ง object
        private Random rand;

        // ตัวแปรควบคุมทิศทางการกดปุ่ม
        private boolean p1Left, p1Right;
        private boolean p2Left, p2Right;
        private boolean p3Left, p3Right;

        public GamePanel() {
            setBackground(Color.BLACK); // สีพื้นหลัง
            setFocusable(true);         // ทำให้ panel รับการกดปุ่มได้
            addKeyListener(this);       // เพิ่ม key listener

            // สร้างผู้เล่น 3 คน (x, y, กว้าง, สูง)
            player1 = new Rectangle(150, 500, 50, 30); // สีแดง
            player2 = new Rectangle(350, 500, 50, 30); // สีเขียว
            player3 = new Rectangle(550, 500, 50, 30); // สีน้ำเงิน

            // สร้าง list ของวัตถุ
            objects = new ArrayList<>();

            // สำหรับสุ่ม
            rand = new Random();

            // ตั้งเวลา 20ms ต่อการอัพเดต (ประมาณ 50 fps)
            timer = new Timer(20, this);
            timer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g); // วาดพื้นหลัง

            Graphics2D g2d = (Graphics2D) g;

            // วาดผู้เล่น 1 (แดง)
            g.setColor(Color.RED);
            g.fillRect(player1.x, player1.y, player1.width, player1.height);

            // วาดผู้เล่น 2 (เขียว)
            g.setColor(Color.GREEN);
            g.fillRect(player2.x, player2.y, player2.width, player2.height);

            // วาดผู้เล่น 3 (น้ำเงิน)
            g.setColor(Color.BLUE);
            g.fillRect(player3.x, player3.y, player3.width, player3.height);

            // วาดวัตถุที่ตก (สี่เหลี่ยม)
            g.setColor(Color.YELLOW);
            for (Rectangle obj : objects) {
                g.fillRect(obj.x, obj.y, obj.width, obj.height);
            }

            // แสดงคะแนนด้านบน
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 16));
            g.drawString("Player1 (RED): " + score1, 20, 20);
            g.drawString("Player2 (GREEN): " + score2, 20, 40);
            g.drawString("Player3 (BLUE): " + score3, 20, 60);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // ทุกครั้งที่ timer เดิน → อัพเดตตำแหน่ง

            // สุ่มสร้างวัตถุใหม่บางครั้ง
            if (rand.nextInt(20) == 0) { // โอกาสสุ่ม 1/20
                int x = rand.nextInt(getWidth() - 20); // สุ่มตำแหน่ง x
                objects.add(new Rectangle(x, 0, 20, 20)); // สร้างสี่เหลี่ยมใหม่
            }

            // อัพเดตการเคลื่อนที่ของผู้เล่น
            if (p1Left && player1.x > 0) player1.x -= 5;
            if (p1Right && player1.x < getWidth() - player1.width) player1.x += 5;

            if (p2Left && player2.x > 0) player2.x -= 5;
            if (p2Right && player2.x < getWidth() - player2.width) player2.x += 5;

            if (p3Left && player3.x > 0) player3.x -= 5;
            if (p3Right && player3.x < getWidth() - player3.width) player3.x += 5;

            // อัพเดตการตกของวัตถุ
            for (int i = 0; i < objects.size(); i++) {
                Rectangle obj = objects.get(i);
                obj.y += 5; // ตกลงมา

                // ตรวจจับการชนกับผู้เล่น 1
                if (obj.intersects(player1)) {
                    score1++;
                    objects.remove(i);
                    i--;
                    continue;
                }

                // ตรวจจับการชนกับผู้เล่น 2
                if (obj.intersects(player2)) {
                    score2++;
                    objects.remove(i);
                    i--;
                    continue;
                }

                // ตรวจจับการชนกับผู้เล่น 3
                if (obj.intersects(player3)) {
                    score3++;
                    objects.remove(i);
                    i--;
                    continue;
                }

                // ถ้าตกถึงขอบล่าง → ลบออก
                if (obj.y > getHeight()) {
                    objects.remove(i);
                    i--;
                }
            }

            // เรียกวาดใหม่
            repaint();
        }

        // ====== KeyListener ส่วนการกดปุ่ม ======

        @Override
        public void keyPressed(KeyEvent e) {
            int key = e.getKeyCode();

            // Player 1 → ปุ่ม A, D
            if (key == KeyEvent.VK_A) p1Left = true;
            if (key == KeyEvent.VK_D) p1Right = true;

            // Player 2 → ปุ่ม LEFT, RIGHT
            if (key == KeyEvent.VK_LEFT) p2Left = true;
            if (key == KeyEvent.VK_RIGHT) p2Right = true;

            // Player 3 → ปุ่ม J, L
            if (key == KeyEvent.VK_J) p3Left = true;
            if (key == KeyEvent.VK_L) p3Right = true;
        }

        @Override
        public void keyReleased(KeyEvent e) {
            int key = e.getKeyCode();

            // Player 1
            if (key == KeyEvent.VK_A) p1Left = false;
            if (key == KeyEvent.VK_D) p1Right = false;

            // Player 2
            if (key == KeyEvent.VK_LEFT) p2Left = false;
            if (key == KeyEvent.VK_RIGHT) p2Right = false;

            // Player 3
            if (key == KeyEvent.VK_J) p3Left = false;
            if (key == KeyEvent.VK_L) p3Right = false;
        }

        @Override
        public void keyTyped(KeyEvent e) {
            // ไม่ใช้
        }
    }

    // ฟังก์ชันเริ่มเกม
    public static void main(String[] args) {
        SwingUtilities.invokeLater(FallingGame::new);
    }
}
