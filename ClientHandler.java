import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;    // The socket connected to the client.
    private final GameServer server;      // Reference to the GameServer instance to manage the client
    private BufferedReader in;       // BufferedReader for reading input from the client
    private PrintWriter out;          // PrintWriter for sending output to the client.
    private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());  // Logger to log activities related to the client handler.
    private static final Map<String, String> userDatabase = new HashMap<>(); // Mock user database
    private int points;
    private String token;

    private int queuePosition;

    static {
        // Adding some dummy users for testing
        userDatabase.put("user1", "password1");
        userDatabase.put("user2", "password2");
    }

    public ClientHandler(Socket socket, GameServer server) {
        this.clientSocket = socket;
        this.server = server;
        this.points = 0;
    }

    public int getPoints() {
        return points;
    }

    public void addPoints(int points) {
        this.points += points;
    }

    public int getQueuePosition() {
        return queuePosition;
    }
    
    public void setQueuePosition(int position) {
        this.queuePosition = position;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            // Handle reconnection
            out.println("Do you have a token? (yes/no)");
            String response = in.readLine().trim();
            if ("yes".equalsIgnoreCase(response)) {
                out.println("Enter your token:");
                String reconnectToken = in.readLine().trim();
                ClientHandler existingClient = server.getClientByToken(reconnectToken);
                if (existingClient != null) {
                    this.token = reconnectToken;
                    this.points = existingClient.getPoints();
                    this.queuePosition = existingClient.getQueuePosition();
                    logger.info("Client reconnected with token: " + this.queuePosition);
                    out.println("Reconnection successful. Type 'exit' to disconnect, or type anything else to echo:");
                    // Continue with the game or interaction logic
                    handleClientInteraction();
                    return;
                } else {
                    out.println("Invalid token. Proceeding with normal authentication...");
                }
            }

            // Handle authentication
            if (authenticate()) {
                server.addAuthenticatedClient(this);  // Verify the number of players connected and with login done
                token = UUID.randomUUID().toString();
                server.addToken(token, this);
                out.println("Your reconnection token: " + token);
                out.println("Authentication successful. Type 'exit' to disconnect, or type anything else to echo:");
                logger.info("Client authenticated: " + clientSocket.getInetAddress());  // Printing the server

                // Handle client interaction
                handleClientInteraction();
            } else {
                out.println("Authentication failed. Disconnecting...");
                logger.warning("Client failed authentication: " + clientSocket.getInetAddress());
            }
        } catch (SocketException e) {
            logger.log(Level.SEVERE, "Client connection reset", e);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error in client handler", e);
        } finally {
            try {
                clientSocket.close();
                server.removeClient(this);
                logger.info("Client disconnected: " + clientSocket.getInetAddress());
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error closing client socket", e);
            }
        }
    }

    private boolean authenticate() throws IOException {
        out.println("Enter username:");
        String username = in.readLine().trim();
        out.println("Enter password:");
        String password = in.readLine().trim();

        String storedPassword = userDatabase.get(username);
        boolean authenticated = storedPassword != null && storedPassword.equals(password);
        if (authenticated) {
            logger.info("User authenticated: " + username);
        } else {
            logger.warning("Authentication failed for user: " + username);
        }
        return authenticated;
    }

    private void handleClientInteraction() throws IOException {
        logger.info("Handling client interaction...");
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            if ("exit".equalsIgnoreCase(inputLine.trim())) {
                break;
            }

            if (server.verifyNumberPlayers()) {
                logger.info("There are 2 players authenticated");
                out.println("Two players authenticated! Joining the game! Receiving the board!");
                startGame();
                break;
            } else {
                out.println(inputLine);
                logger.info("Responding to client, after receiving " + inputLine + " from the client");
            }
        }
    }

    private void startGame() throws IOException {
        for (int i = 1; i <= 4; i++) {
            String inputLine = in.readLine();
            if (inputLine == null) {
                logger.info("Received null input, breaking...");
                break;
            }
            logger.info("Responding to client, after receiving " + inputLine + " from the client");
            addPoints(10); // Example of adding points
            out.println("Your current: " + inputLine);
        }

        // After the game, notify the client about their points
        out.println("Game over! Your points: " + getPoints());

        clientSocket.close();
        server.removeClient(this);
        logger.info("Client disconnected: " + clientSocket.getInetAddress());
    }
}