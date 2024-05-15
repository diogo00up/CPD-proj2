import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.locks.*;

public class GameServer {
    private static final int PORT = 12345;
    private static final int PLAYERS_PER_GAME = 2; // Adjust as needed
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
            e.printStackTrace();
        }
    }

    private static void loadUserCredentials() {
        // Load user credentials from a file or create dummy data
        userCredentials.put("ian", "ian090903");
        userCredentials.put("user1", "pass1");
        userCredentials.put("user2", "pass2");
        // Add more users as needed
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
                out.flush(); // Ensure stream header is sent immediately
                in = new ObjectInputStream(clientSocket.getInputStream());
                System.out.println("Streams initialized for client: " + clientSocket.getInetAddress().getHostAddress());

                // Authenticate user
                authenticateUser();

                // Add user to the waiting queue
                addPlayerToQueue();

                // Inform user to wait for the game to start
                out.writeObject("Please wait until enough players are available to start the game...");
                out.flush();
                System.out.println("User " + username + " added to queue and informed to wait.");

                // Keep the connection open while waiting for the game to start
                synchronized (this) {
                    wait();
                }

            } catch (IOException | ClassNotFoundException | InterruptedException e) {
                System.err.println("Error during client handling: " + e.getMessage());
                e.printStackTrace();
            } finally {
                closeResources();
            }
        }

        private void authenticateUser() throws IOException, ClassNotFoundException {
            while (true) {
                System.out.println("Prompting user for username...");
                out.writeObject("Enter username: ");
                out.flush();
                String username = (String) in.readObject();
                System.out.println("Received username: " + username);
                out.writeObject("Enter password: ");
                out.flush();
                String password = (String) in.readObject();
                System.out.println("Received password for user " + username);

                // Logging the values being compared for authentication
                System.out.println("Comparing with stored credentials: " + userCredentials.get(username));

                if (userCredentials.containsKey(username) && userCredentials.get(username).equals(password)) {
                    this.username = username;
                    System.out.println("User authenticated: " + username);  // Log the username
                    out.writeObject("Authentication successful.");
                    out.flush();
                    break;
                } else {
                    System.out.println("Authentication failed for user: " + username);
                    out.writeObject("Authentication failed. Try again.");
                    out.flush();
                }
            }
        }

        private void addPlayerToQueue() throws IOException {
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
                e.printStackTrace();
            }
        }
    }
}
