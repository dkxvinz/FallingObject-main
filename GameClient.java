// --- GameClient.java ---

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.Set; 
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap; // ใช้ ConcurrentHashMap สำหรับ otherPlayers

public class GameClient extends JFrame {
    private GamePanel panel;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String serverIP = "10.160.17.177"; // เปลี่ยนเป็น IP ของ Server
    private final int SERVER_PORT = 5555;

    private String myID; 
    private Color myColor; // เก็บสีที่ Server กำหนด

    public GameClient() {
        try {
            socket = new Socket(serverIP, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("Connected to server");

            // รับ ID และ Color ของตัวเองจาก Server (Server ส่งมาเป็น "ID,ColorInt")
            String idAndColor = in.readLine();
            String[] parts = idAndColor.split(",");
            if (parts.length == 2) {
                myID = parts[0];
                int colorRGB = Integer.parseInt(parts[1]);
                myColor = new Color(colorRGB);
            } else {
                 throw new IOException("Invalid ID/Color from server.");
            }

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "❌ Cannot connect to server or connection error:\n" + e.getMessage());
            System.exit(0);
            return;
        }

        panel = new GamePanel(out, myID, myColor);
        add(panel);
        setTitle("Falling Game Online - " + myID);
        setSize(600, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setVisible(true);

        // Thread รับตำแหน่งและสถานะผู้เล่นทุกคน
        new Thread(() -> {
            try {
                String msg;
                while ((msg = in.readLine()) != null) {
                    panel.updateOtherPlayers(msg);
                }
            } catch (IOException e) {
                // Connection lost or error while reading
                System.out.println("❌ Connection lost or server shutdown.");
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Connection lost to server.", "Error", JOptionPane.ERROR_MESSAGE);
                    System.exit(0);
                });
            }
        }).start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(GameClient::new);
    }

    // ===========================================
    static class GamePanel extends JPanel implements KeyListener {
        private Rectangle myPlayer = new Rectangle(150, 500, 50, 30);
        // เก็บสถานะผู้เล่นคนอื่น
        private Map<String, PlayerInfo> otherPlayers = new ConcurrentHashMap<>(); 
        
        // เก็บสถานะคะแนน, สี, และสถานะการชนะทั้งหมด (รวมตัวเองด้วย)
        private Map<String, PlayerInfo> playerStatus = new ConcurrentHashMap<>(); 

        private java.util.List<Rectangle> objects = new java.util.ArrayList<>();
        private boolean moveLeft, moveRight;
        private PrintWriter out;
        private java.util.Random rand = new java.util.Random();
        private String myID;
        private Color myColor;
        private boolean isGameOver = false;

        // Class สำหรับเก็บข้อมูลผู้เล่นที่จำเป็นในการแสดงผล (Client Side)
        static class PlayerInfo {
            Rectangle rect;
            int score;
            Color color;
            boolean hasWon;
            public PlayerInfo(Rectangle r, int s, Color c, boolean won) {
                this.rect = r;
                this.score = s;
                this.color = c;
                this.hasWon = won;
            }
        }


        public GamePanel(PrintWriter out, String myID, Color myColor) {
            this.out = out;
            this.myID = myID;
            this.myColor = myColor;
            setBackground(Color.BLACK);
            setFocusable(true);
            addKeyListener(this);

            // เพิ่มตัวเองเข้าใน playerStatus ตั้งต้น
            playerStatus.put(myID, new PlayerInfo(myPlayer, 0, myColor, false));
            
            // Game loop
            new Thread(() -> {
                while (!isGameOver) { 
                    gameUpdate();
                    repaint();
                    try { Thread.sleep(20); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                }
                // ถ้าเกมจบแล้ว ส่งสถานะสุดท้ายไปยัง Server
                out.println(myPlayer.x + "," + playerStatus.get(myID).score);
                repaint(); // วาดหน้าจอสุดท้าย
            }).start();
        }

        private void gameUpdate() {
            if (isGameOver) return;

            // 1. การเคลื่อนที่ของผู้เล่น
            if (moveLeft && myPlayer.x > 0) myPlayer.x -= 5;
            if (moveRight && myPlayer.x < getWidth() - myPlayer.width) myPlayer.x += 5;

            // 2. สร้าง object ตก
            if (rand.nextInt(30) == 0)
                objects.add(new Rectangle(rand.nextInt(getWidth() - 20), 0, 20, 20));

            // 3. อัปเดตการตกและการชน
            PlayerInfo myStatus = playerStatus.get(myID);
            int currentScore = myStatus != null ? myStatus.score : 0;
            boolean scoreChanged = false;
            
            for (int i = 0; i < objects.size(); i++) {
                Rectangle o = objects.get(i);
                o.y += 5;

                boolean removed = false;
                
                // ตรวจจับชนกับผู้เล่นตัวเอง
                if (myPlayer.intersects(o)) {
                    removed = true;
                    currentScore++; // เพิ่มคะแนน
                    scoreChanged = true;
                } 
                
                // ตรวจจับชนกับผู้เล่นคนอื่น
                // (Client ไม่ควรคำนวณคะแนนให้คนอื่น เพราะ Server เป็นผู้คำนวณ)
                // แต่ต้องเอา Object ออกจากหน้าจอเพื่อไม่ให้ Object นั้นถูกนับซ้ำ
                for (PlayerInfo info : otherPlayers.values()) {
                    if (info.rect.intersects(o)) {
                        removed = true;
                        break;
                    }
                }
                

                if (removed || o.y > getHeight()) {
                    objects.remove(i);
                    i--;
                }
            }

            // 4. ส่งตำแหน่งและคะแนนตัวเองไป Server
            if (scoreChanged) {
                // ส่งตำแหน่ง X และคะแนนที่นับได้เมื่อชนไปให้ Server
                out.println(myPlayer.x + "," + currentScore);
                // อัปเดตสถานะของตัวเองใน Client
                if (myStatus != null) {
                    myStatus.score = currentScore;
                    if (myStatus.score >= 10) isGameOver = true;
                }
                
            } else {
                 // ส่งตำแหน่ง X และคะแนนปัจจุบันทุก Loop (แม้ไม่ชน)
                out.println(myPlayer.x + "," + currentScore);
            }
        }

        // อัปเดตผู้เล่นคนอื่นจาก Server
        public void updateOtherPlayers(String msg) {
            if (isGameOver) return;
            try {
                // Server ส่งข้อมูลแต่ละผู้เล่นเป็น "ID,x,score,color,hasWon;ID,x,score,color,hasWon;..."
                String[] players = msg.split(";");
                // ใช้ Set เพื่อติดตามว่าผู้เล่นคนใดที่ยังอยู่ในเกม
                Set<String> activePlayers = new HashSet<>();
                
                for (String p : players) {
                    if (p.isEmpty()) continue;
                    String[] parts = p.split(",");
                    if (parts.length < 5) continue; 

                    String pid = parts[0];
                    int x = Integer.parseInt(parts[1]);
                    int score = Integer.parseInt(parts[2]);
                    int colorRGB = Integer.parseInt(parts[3]);
                    boolean won = parts[4].equals("1");

                    activePlayers.add(pid);

                    Color playerColor = new Color(colorRGB);
                    
                    // Update สถานะผู้เล่นทุกคนใน Map รวม
                    PlayerInfo info = playerStatus.getOrDefault(pid, new PlayerInfo(new Rectangle(x, 500, 50, 30), score, playerColor, won));
                    info.rect.x = x;
                    info.score = score;
                    info.color = playerColor;
                    info.hasWon = won;
                    playerStatus.put(pid, info);

                    if (won) {
                        isGameOver = true; // มีผู้ชนะแล้ว
                    }
                    
                    // Update ใน Map ผู้เล่นคนอื่น
                    if (!pid.equals(myID)) {
                        otherPlayers.put(pid, info);
                    } else {
                        // อัปเดตสถานะของตัวเอง (จาก Server)
                        myPlayer.x = x; 
                        myColor = playerColor; 
                    }
                }
                
                // ลบผู้เล่นที่ไม่อยู่ใน activePlayers แล้วออกจาก otherPlayers
                playerStatus.keySet().removeIf(id -> !activePlayers.contains(id));
                otherPlayers.keySet().removeIf(id -> !activePlayers.contains(id));
                
            } catch (Exception ignored) {
                 // ควร Log ข้อผิดพลาด แต่ละเลยเพื่อไม่ให้โปรแกรมหยุดทำงาน
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            // วาด Object ตก
            g.setColor(Color.YELLOW);
            for (Rectangle o : objects) g.fillRect(o.x, o.y, o.width, o.height);

            // วาดผู้เล่นทุกคน (รวมตัวเอง)
            for (PlayerInfo info : playerStatus.values()) {
                g.setColor(info.color);
                g.fillRect(info.rect.x, info.rect.y, info.rect.width, info.rect.height);
            }

            // แสดงคะแนน/สถานะ
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 16));
            
            int y = 20;
            for (Map.Entry<String, PlayerInfo> entry : playerStatus.entrySet()) {
                String pid = entry.getKey();
                PlayerInfo info = entry.getValue();
                
                String displayID = pid.equals(myID) ? "You" : pid;
                String winStatus = info.hasWon ? " (WINNER!)" : "";
                
                g.drawString(displayID + ": " + info.score + winStatus, 20, y);
                y += 20;
            }
            
            // แสดงสถานะเกมจบ
            if (isGameOver) {
                g.setColor(Color.RED);
                g.setFont(new Font("Arial", Font.BOLD, 48));
                g.drawString("GAME OVER", getWidth() / 2 - 150, getHeight() / 2);
            }
        }

        @Override
        public void keyPressed(KeyEvent e) {
            if (isGameOver) return;
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