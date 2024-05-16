import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.locks.*;

public class GameServer {
    private static final int PORT = 12345;
    private static final int PLAYERS_PER_GAME = 2;
    private static Map<String, String> userCredentials = new HashMap<>();
    private static List<ClientHandler> waitingPlayers = new ArrayList<>();
    private static ReentrantLock lock = new ReentrantLock();

    public static void main(String[] args) {
        loadUserCredentials();
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Game server started...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress().getHostAddress());
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("Error in server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void loadUserCredentials() {
        try (BufferedReader reader = new BufferedReader(new FileReader("user_credentials.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    userCredentials.put(parts[0], parts[1]);
                }
            }
        } catch (IOException e) {
            System.out.println("No credentials file found, using default credentials.");
            userCredentials.put("ian", "ian090903");
            userCredentials.put("user1", "pass1");
            userCredentials.put("user2", "pass2");
        }
    }

    public static class ClientHandler implements Runnable {
        private Socket clientSocket;
        private String username;
        private ObjectOutputStream out;
        private ObjectInputStream in;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        public void run() {
            try {
                out = new ObjectOutputStream(clientSocket.getOutputStream());
                in = new ObjectInputStream(clientSocket.getInputStream());
                out.flush(); // Ensure stream header is sent immediately

                authenticateUser();
                addPlayerToQueue();

                // Keep the connection open while waiting for the game to start
                while (true) {
                    try {
                        String command = (String) in.readObject();
                        System.out.println("Received command: " + command);
                        out.writeObject("Echo: " + command);
                        out.flush();
                    } catch (EOFException e) {
                        System.out.println("Client disconnected.");
                        break;
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Error during client handling: " + e.getMessage());
                e.printStackTrace();
            } finally {
                closeResources();
            }
        }

        private void authenticateUser() throws IOException, ClassNotFoundException {
            while (true) {
                out.writeObject("Enter username: ");
                out.flush();
                String username = (String) in.readObject();
                out.writeObject("Enter password: ");
                out.flush();
                String password = (String) in.readObject();

                if (userCredentials.containsKey(username) && userCredentials.get(username).equals(password)) {
                    this.username = username;
                    out.writeObject("Authentication successful.");
                    out.flush();
                    break;
                } else {
                    out.writeObject("Authentication failed. Try again.");
                    out.flush();
                }
            }
        }

        private void addPlayerToQueue() {
            lock.lock();
            try {
                waitingPlayers.add(this);
                if (waitingPlayers.size() >= PLAYERS_PER_GAME) {
                    startGame();
                }
            } finally {
                lock.unlock();
            }
        }

        private void startGame() {
            List<ClientHandler> gamePlayers = new ArrayList<>();
            for (ClientHandler handler : waitingPlayers.subList(0, PLAYERS_PER_GAME)) {
                gamePlayers.add(handler);
            }
            waitingPlayers.subList(0, PLAYERS_PER_GAME).clear();
            LudoGame game = new LudoGame(PLAYERS_PER_GAME, gamePlayers);
            new Thread(game).start();
        }

        public ObjectOutputStream getOut() {
            return out;
        }

        public ObjectInputStream getIn() {
            return in;
        }

        private void closeResources() {
            try {
                if (out != null) out.close();
                if (in != null) in.close();
                if (clientSocket != null) clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing resources for client: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
