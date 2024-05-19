// ClientHandler.java

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final GameServer server;
    private BufferedReader in;
    private PrintWriter out;
    private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());
    private static final Map<String, String> userDatabase = new HashMap<>();
    private static final ReentrantLock gameLock = new ReentrantLock();
    private static final List<ClientHandler> connectedClients = new CopyOnWriteArrayList<>();
    private static final LudoGame ludoGame = new LudoGame(4);
    private static int readyPlayers = 0;
    private int points;
    private String token;
    private int playerId;
    private boolean isReady;

    static {
        userDatabase.put("user1", "password1");
        userDatabase.put("user2", "password2");
    }

    public ClientHandler(Socket socket, GameServer server) {
        this.clientSocket = socket;
        this.server = server;
        this.points = 0;
        this.isReady = false;
    }

    public int getPoints() {
        return points;
    }

    public void addPoints(int points) {
        this.points += points;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);
    
            handleReconnection();
            if (authenticate()) {
                server.addAuthenticatedClient(this);
                token = UUID.randomUUID().toString();
                server.addToken(token, this);
                out.println("Your reconnection token: " + token);
                out.println("Authentication successful. Type 'exit' to disconnect, or type anything else to echo:");
                logger.info("Client authenticated: " + clientSocket.getInetAddress());
    
                synchronized (connectedClients) {
                    playerId = connectedClients.size();
                    connectedClients.add(this);
                    if (server.verifyNumberPlayers()) {
                        promptGameStart();
                    }
                }

                out.println("You are player " + playerId);
                logger.info("Player " + playerId + " connected.");

                handleClientInteraction();
            } else {
                out.println("Authentication failed. Disconnecting...");
                logger.warning("Client failed authentication: " + clientSocket.getInetAddress());
            }
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
    

    private void handleReconnection() throws IOException {
        out.println("Do you have a token? (yes/no)");
        String response = in.readLine().trim();
        if ("yes".equalsIgnoreCase(response)) {
            out.println("Enter your token:");
            String reconnectToken = in.readLine().trim();
            ClientHandler existingClient = server.getClientByToken(reconnectToken);
            if (existingClient != null) {
                this.token = reconnectToken;
                this.points = existingClient.getPoints();
                server.addAuthenticatedClient(this);
                logger.info("Client reconnected with token: " + reconnectToken);
                out.println("Reconnection successful. Type 'exit' to disconnect, or type anything else to echo:");
                handleClientInteraction();
                return;
            } else {
                out.println("Invalid token. Proceeding with normal authentication...");
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

    private void promptGameStart() throws IOException {
        for (ClientHandler client : connectedClients) {
            client.out.println("Minimum players connected. Type 'ready' to start the game.");
        }
    }

    private void handleClientInteraction() throws IOException {
        logger.info("Starting client interaction...");
        String inputLine;

        while ((inputLine = in.readLine()) != null) {
            inputLine = inputLine.trim();
            logger.info("Received from client: '" + inputLine + "'");
            if ("exit".equalsIgnoreCase(inputLine)) {
                logger.info("Exit command received, disconnecting client...");
                break;
            }
    
            if ("ready".equalsIgnoreCase(inputLine)) {
                logger.info("Handling 'ready' command...");
                handleReadyCommand();
            } else if ("roll".equalsIgnoreCase(inputLine)) {
                logger.info("Handling 'roll' command...");
                handleRollCommand();
            } else {
                out.println("Echo: " + inputLine);
                logger.info("Echoing back input to client.");
            }
        }
        logger.info("Exiting handleClient Interaction method.");
    }
    
    

    private void handleReadyCommand() throws IOException {
        synchronized (connectedClients) {
            if (!isReady) {
                isReady = true;
                readyPlayers++;
                logger.info("Player " + playerId + " is ready. Total ready players: " + readyPlayers);
                // Update readiness status to all clients
                for (ClientHandler client : connectedClients) {
                    client.out.println("Player " + playerId + " is ready (" + readyPlayers + "/" + connectedClients.size() + ").");
                }
                // Check if all players are ready
                if (readyPlayers == connectedClients.size()) {
                    for (ClientHandler client : connectedClients) {
                        client.out.println("All players are ready. Waiting for other players...");
                    }
                    startGame();
                }
            }
        }
    }
    

    private void handleRollCommand() {
        gameLock.lock();
        try {
            if (ludoGame.getPlayers().get(ludoGame.getPlayers().get(0).getId()).getId() == playerId) {
                Random random = new Random();
                int diceRoll = random.nextInt(6) + 1;
                out.println("You rolled a " + diceRoll);

                if (ludoGame.moveToken(playerId, diceRoll)) {
                    out.println("Token moved successfully.");
                } else {
                    out.println("Failed to move token.");
                }

                out.println(ludoGame.getBoardState());

                if (ludoGame.isGameEnded()) {
                    for (ClientHandler client : connectedClients) {
                        client.out.println("Game over! Thank you for playing.");
                    }
                }
            } else {
                out.println("It's not your turn.");
            }
        } finally {
            gameLock.unlock();
        }
    }

    private void startGame() throws IOException {
        logger.info("All players are ready. The game is starting!");
        for (ClientHandler client : connectedClients) {
            client.out.println("All players are ready. The game is starting!");
            client.out.println("The game has started! Type 'roll' to roll the dice.");
        }
        ludoGame.startGame(); // Assuming there's a method in LudoGame to start the game logic.
    }
    
}
