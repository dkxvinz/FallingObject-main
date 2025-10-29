import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class GameServer {
    private static final int PORT = 5555;
    private static Map<String, Socket> clients = new ConcurrentHashMap<>();
    private static Map<String, String> latestPositions = new ConcurrentHashMap<>();
    private static ExecutorService pool = Executors.newCachedThreadPool();
    private static int playerCounter = 1;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("🎮 Game Server started on port " + PORT);

            while (true) {
                Socket client = serverSocket.accept();
                String playerID = "Player" + playerCounter++;
                clients.put(playerID, client);
                System.out.println(playerID + " connected: " + client.getInetAddress());

                // ส่ง ID ให้ client ตัวเอง
                PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                out.println(playerID);

                // เริ่ม thread ต่อ client
                pool.execute(() -> handleClient(playerID, client));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(String playerID, Socket client) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()))) {
            String msg;
            while ((msg = in.readLine()) != null) {
                // msg = "PlayerID,x,score"
                latestPositions.put(playerID, msg);

                // Broadcast ไปทุก client
                broadcastPositions();
            }
        } catch (IOException e) {
            System.out.println("❌ " + playerID + " disconnected");
        } finally {
            clients.remove(playerID);
            latestPositions.remove(playerID);
            try { client.close(); } catch (IOException ignored) {}
        }
    }

    private static void broadcastPositions() {
        // รวมข้อมูลผู้เล่นทุกคนเป็น "ID,x,score;ID,x,score;..."
        StringBuilder sb = new StringBuilder();
        for (String id : latestPositions.keySet()) {
            sb.append(latestPositions.get(id)).append(";");
        }
        String message = sb.toString();

        // ส่งไปทุก client
        for (Map.Entry<String, Socket> entry : clients.entrySet()) {
            try {
                PrintWriter out = new PrintWriter(entry.getValue().getOutputStream(), true);
                out.println(message);
            } catch (IOException e) {
                System.out.println("❌ Failed to send to " + entry.getKey());
            }
        }
    }
}
