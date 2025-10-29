import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;


public class GameClient extends JFrame {
    private GamePanel panel;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String serverIP = "10.160.94.239"; // เปลี่ยนเป็น IP ของ Server

    private String myID; // ID ของตัวเอง (Server assign)

    public GameClient() {
        try {
            socket = new Socket(serverIP, 5555);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("✅ Connected to server");

            // รับ ID ของตัวเองจาก Server
            myID = in.readLine();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "❌ Cannot connect to server");
            System.exit(0);
        }

        panel = new GamePanel(out, myID);
        add(panel);
        setTitle("Falling Game Online");
        setSize(600, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setVisible(true);

        // Thread รับตำแหน่งผู้เล่นทุกคน
        new Thread(() -> {
            try {
                String msg;
                while ((msg = in.readLine()) != null) {
                    panel.updateOtherPlayers(msg);
                }
            } catch (IOException e) {
                System.out.println("❌ Connection lost");
            }
        }).start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(GameClient::new);
    }

    // ===========================================
    static class GamePanel extends JPanel implements KeyListener {
        private Rectangle myPlayer = new Rectangle(150, 500, 50, 30);
        private Map<String, Rectangle> otherPlayers = new HashMap<>();
        private Map<String, Integer> scores = new HashMap<>();
        private java.util.List<Rectangle> objects = new java.util.ArrayList<>();
        private boolean moveLeft, moveRight;
        private PrintWriter out;
        private java.util.Random rand = new java.util.Random();
        private String myID;
        

        public GamePanel(PrintWriter out, String myID) {
            this.out = out;
            this.myID = myID;
            setBackground(Color.BLACK);
            setFocusable(true);
            addKeyListener(this);
           
            // Game loop
            new Thread(() -> {
                while (true) {
                  
                    gameUpdate();
                    repaint();
                    try { Thread.sleep(20); } catch (InterruptedException e) { e.printStackTrace(); }
                }
            }).start();
        }

        private void gameUpdate() {
            if (moveLeft && myPlayer.x > 0) myPlayer.x -= 5;
            if (moveRight && myPlayer.x < getWidth() - myPlayer.width) myPlayer.x += 5;

            // สร้าง object ตก
            if (rand.nextInt(30) == 0)
                objects.add(new Rectangle(rand.nextInt(getWidth() - 20), 0, 20, 20));

            // อัปเดตการตก
            for (int i = 0; i < objects.size(); i++) {
                Rectangle o = objects.get(i);
                o.y += 5;

                boolean removed = false;
                // ตรวจจับชนกับผู้เล่น
                if (myPlayer.intersects(o)) {
                    removed = true;
                    out.println(myID + "," + myPlayer.x + "," + (scores.getOrDefault(myID, 0)+1));
                    scores.put(myID, scores.getOrDefault(myID, 0)+1);
                } else {
                    for (Map.Entry<String, Rectangle> entry : otherPlayers.entrySet()) {
                        if (entry.getValue().intersects(o)) {
                            removed = true;
                            String pid = entry.getKey();
                            scores.put(pid, scores.getOrDefault(pid, 0)+1);
                            break;
                        }
                    }
                }

                if (removed || o.y > getHeight()) {
                    objects.remove(i);
                    i--;
                }
            }

            // ส่งตำแหน่งและคะแนนตัวเองทุก loop
            out.println(myID + "," + myPlayer.x + "," + scores.getOrDefault(myID, 0));
        }

        // อัปเดตผู้เล่นคนอื่น
        public void updateOtherPlayers(String msg) {
            try {
                // server ส่งข้อมูลแต่ละผู้เล่นเป็น "ID,x,score;ID,x,score;..."
                String[] players = msg.split(";");
                for (String p : players) {
                    String[] parts = p.split(",");
                    if (parts.length < 3) continue;
                    String pid = parts[0];
                    int x = Integer.parseInt(parts[1]);
                    int score = Integer.parseInt(parts[2]);

                    if (!pid.equals(myID)) {
                        Rectangle r = otherPlayers.getOrDefault(pid, new Rectangle(x, 500, 50, 30));
                        r.x = x;
                        otherPlayers.put(pid, r);
                        scores.put(pid, score);
                    }
                }
            } catch (Exception ignored) {}
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            g.setColor(Color.YELLOW);
            for (Rectangle o : objects) g.fillRect(o.x, o.y, o.width, o.height);

            g.setColor(Color.GREEN);
            g.fillRect(myPlayer.x, myPlayer.y, myPlayer.width, myPlayer.height);

            g.setColor(Color.RED);
            for (Rectangle r : otherPlayers.values()) g.fillRect(r.x, r.y, r.width, r.height);

            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 16));
            g.drawString("You: " + scores.getOrDefault(myID, 0), 20, 20);
            int y = 40;
            for (Map.Entry<String, Integer> entry : scores.entrySet()) {
                if (!entry.getKey().equals(myID)) {
                    g.drawString(entry.getKey() + ": " + entry.getValue(), 20, y);
                    y += 20;
                }
            }
        }

        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_LEFT) moveLeft = true;
            if (e.getKeyCode() == KeyEvent.VK_RIGHT) moveRight = true;
        }

        @Override
        public void keyReleased(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_LEFT) moveLeft = false;
            if (e.getKeyCode() == KeyEvent.VK_RIGHT) moveRight = false;
        }

        @Override
        public void keyTyped(KeyEvent e) {}
    }

  
}
