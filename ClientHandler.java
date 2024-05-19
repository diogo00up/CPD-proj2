import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final GameServer server;
    private BufferedReader in;
    PrintWriter out;
    private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());
    private static final Map<String, String> userDatabase = new HashMap<>();
    private int points;
    String token;
    private int queuePosition;
    static final int MAX_ROUNDS = 5;
    private boolean isReady;
    private boolean isTurn;

    static {
        userDatabase.put("user1", "password1");
        userDatabase.put("user2", "password2");
        userDatabase.put("user3", "password3");
        userDatabase.put("user4", "password4");
    }

    public ClientHandler(Socket socket, GameServer server) {
        this.clientSocket = socket;
        this.server = server;
        this.isReady = false;
        this.isTurn = false;
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

    public void setReady(boolean ready) {
        isReady = ready;
    }

    public boolean isReady() {
        return isReady;
    }

    public void setTurn(boolean turn) {
        isTurn = turn;
    }

    public boolean isTurn() {
        return isTurn;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

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
                    handleClientInteraction();
                    return;
                } else {
                    out.println("Invalid token. Proceeding with normal authentication...");
                }
            }

            if (authenticate()) {
                server.addAuthenticatedClient(this);
                token = UUID.randomUUID().toString();
                server.addToken(token, this);
                out.println("Your reconnection token: " + token);
                out.println("Authentication successful. Type 'exit' to disconnect, or type anything else to echo:");
                logger.info("Client authenticated: " + clientSocket.getInetAddress());

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
                logger.info("There are 2 players authenticated, Joining the game function");
                out.println("Two players authenticated! Joining the game function!");
                server.startGame();
                break;

            } else {
                out.println("Received: " + inputLine);
                logger.info("Responding to client, after receiving " + inputLine + " from the client");
            }
        }
    }

    public void playTurn() throws IOException {
        out.println("It's your turn! Type 'roll' to roll the dice.");
        setTurn(true);

        // Clear any existing input buffer
        while (in.ready()) {
            in.readLine();
        }

        long startTime = System.currentTimeMillis();
        long timeout = 30000; // 30 seconds timeout

        while ((System.currentTimeMillis() - startTime) < timeout) {
            if (in.ready()) {
                String inputLine = in.readLine().trim();
                if ("roll".equalsIgnoreCase(inputLine) && isTurn) {
                    int roll = new Random().nextInt(6) + 1;
                    addPoints(roll);
                    out.println("You rolled a " + roll + ". Your total points: " + getPoints());
                    setTurn(false);
                    return;
                } else {
                    out.println("Invalid command. Type 'roll' to roll the dice.");
                }
            }
        }

        // If timeout, automatically roll the dice
        if (isTurn) {
            int roll = new Random().nextInt(6) + 1;
            addPoints(roll);
            out.println("Time's up! Rolling the dice automatically.");
            out.println("You rolled a " + roll + ". Your total points: " + getPoints());
            setTurn(false);
        }
    }

    public void notifyGameStart() {
        out.println("Game is starting! Waiting for your turn.");
    }

    public void notifyGameEnd() {
        out.println("Game over! Your total points: " + getPoints());
    }

    public void notifyRoundEnd() {
        out.println("Round complete. Waiting for other players...");
    }
}
