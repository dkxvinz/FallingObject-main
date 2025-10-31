import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class GameServer {
    private static final int PORT = 5555;
    private static Map<String, Socket> clients = new ConcurrentHashMap<>();
    private static Map<String, PlayerState> playerStates = new ConcurrentHashMap<>();
    private static ExecutorService pool = Executors.newCachedThreadPool();
    private static int playerCounter = 1;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("🎮 Game Server started on port " + PORT);

            while (true) {
                Socket client = serverSocket.accept();
                String playerID = "Player" + playerCounter++;

                Random rand = new Random();
                int playerColor = new java.awt.Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256)).getRGB();

                PlayerState newPlayer = new PlayerState(playerID, playerColor);

                clients.put(playerID, client);
                playerStates.put(playerID, newPlayer);
                System.out.println(playerID + " connected: " + client.getInetAddress());

                // ส่ง ID ให้ client ตัวเอง
                PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                out.println(playerID + "," + playerColor);

                // เริ่ม thread ต่อ client
                pool.execute(() -> handleClient(playerID, client));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class PlayerState {
        String id;
        int x;
        int score;
        int color; // เก็บเป็นค่า int ของ Color (ARGB)
        boolean hasWon = false;

        public PlayerState(String id, int color) {
            this.id = id;
            this.color = color;
            this.x = 150; // ตำแหน่งเริ่มต้น
            this.score = 0;
        }

        // เมธอดสำหรับอัปเดตและส่งข้อมูลกลับไปให้ Client
        // Format: ID,x,score,color,hasWon
        @Override
        public String toString() {
            return id + "," + x + "," + score + "," + color + "," + (hasWon ? 1 : 0);
        }
    }

    private static void handleClient(String playerID, Socket client) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()))) {
            String msg;
            while ((msg = in.readLine()) != null) {
                // msg = "x,newScoreIfCollision" (Client จะไม่ส่ง score ที่นับเองมาแล้ว)
                String[] parts = msg.split(",");
                if (parts.length < 2) continue;

                int clientX = Integer.parseInt(parts[0]);
                int newScore = Integer.parseInt(parts[1]); // ค่านี้จะเป็น score ที่ Client คำนวณเมื่อเกิดการชน

                PlayerState state = playerStates.get(playerID);
                if (state != null) {
                    state.x = clientX;
                    
                    // 🚩 การจัดการคะแนนและการชนะที่ Server
                    if (newScore > state.score) {
                        state.score = newScore;
                        if (state.score >= 10) {
                            state.hasWon = true;
                            System.out.println("🏆 " + playerID + " WINS!");
                        }
                    }
                    
                    // อัปเดต State ล่าสุด
                    playerStates.put(playerID, state);
                }
                broadcastPositions();
            }
        } catch (IOException e) {
            System.out.println("❌ " + playerID + " disconnected");
        } finally {
            clients.remove(playerID);
            playerStates.remove(playerID);
            System.out.println("👋 " + playerID + " removed. Current players: " + clients.size());
            try { client.close(); } catch (IOException ignored) {}
            broadcastPositions(); // อัปเดตให้ Client คนอื่นรู้ว่ามีคนออก
        }
    }

    private static void broadcastPositions() {
        // รวมข้อมูลผู้เล่นทุกคนเป็น "ID,x,score;ID,x,score;..."
        StringBuilder sb = new StringBuilder();
        for (PlayerState state: playerStates.values()) {
            sb.append(state.toString()).append(";");
        }
        String message = sb.toString();

        // ส่งไปทุก client
        for (Socket client : clients.values()) {
            try {
                PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                out.println(message);
            } catch (IOException e) {
                // Log failed to send, but continue broadcasting to others
                // System.out.println("❌ Failed to send to a client.");
            }
        }
    }
}