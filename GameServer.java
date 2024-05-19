import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GameServer {
    private static final int PORT = 12345;
    private static final int MAX_PLAYERS = 4; // Adjust as needed
    private static final int MIN_PLAYERS = 2; // Adjust as needed
    private final List<ClientHandler> clients;
    private final Queue<ClientHandler> queue = new LinkedList<>();
    private final ExecutorService pool;
    private final Map<String, ClientHandler> tokenMap;
    private final ReentrantLock lock = new ReentrantLock(); // ReentrantLock for synchronizing client interactions
    private static final Logger logger = Logger.getLogger(GameServer.class.getName());
    private boolean gameRunning;

    public GameServer() {
        clients = new ArrayList<>();
        pool = Executors.newVirtualThreadPerTaskExecutor(); // A thread pool to execute tasks concurrently
        tokenMap = new ConcurrentHashMap<>();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {  // Creates a ServerSocket bound to the specified port
            logger.info("Game server started on port " + PORT);
            logger.info("WAITING FOR CLIENT CONNECTION FROM PORT: " + PORT);

            while (true) {  // Accept incoming client connection
                Socket clientSocket = serverSocket.accept();
                logger.info("New client connected: " + clientSocket.getInetAddress());  // New connection, it logs the connection
                ClientHandler clientHandler = new ClientHandler(clientSocket, this);  // Creates a ClientHandler for the client
                lock.lock();
                try {
                    clients.add(clientHandler);
                    logger.info("Number of connected clients: " + clients.size()); // Log the number of connected clients
                } finally {
                    lock.unlock();
                }
                pool.execute(clientHandler);  // Submits the handler to the thread pool for execution
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error starting server", e);
        }
    }

    public void addAuthenticatedClient(ClientHandler clientHandler) {
        lock.lock();
        try {
            if (!queue.contains(clientHandler)) {
                queue.add(clientHandler);
                clientHandler.setQueuePosition(queue.size());
                logger.info("Number of authenticated clients: " + queue.size());
            }
        } finally {
            lock.unlock();
        }
    }

    public boolean verifyNumberPlayers() {
        lock.lock();
        try {
            if (queue.size() >= 2) {
                logger.info("THERE ARE AT LEAST 2 SIGNED IN CLIENTS");
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    public void removeClient(ClientHandler clientHandler) {
        lock.lock();
        try {
            clients.remove(clientHandler);
            queue.remove(clientHandler);
            tokenMap.remove(clientHandler.token);
        } finally {
            lock.unlock();
        }
    }

    public void addToken(String token, ClientHandler clientHandler) {
        lock.lock();
        try {
            tokenMap.put(token, clientHandler);
        } finally {
            lock.unlock();
        }
    }

    public ClientHandler getClientByToken(String token) {
        lock.lock();
        try {
            return tokenMap.get(token);
        } finally {
            lock.unlock();
        }
    }

    public void startGame() {
        lock.lock();
        try {
            if (!gameRunning) {
                gameRunning = true;
                clients.forEach(ClientHandler::notifyGameStart);
                runGameRounds();
            }
        } finally {
            lock.unlock();
        }
    }

    private void runGameRounds() {
        for (int round = 1; round <= ClientHandler.MAX_ROUNDS; round++) {
            for (ClientHandler client : queue) {
                lock.lock();
                try {
                    client.playTurn();
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Error during client's turn", e);
                } finally {
                    lock.unlock();
                }
            }
            notifyRoundEnd();
        }
        endGame();
    }

    private void notifyRoundEnd() {
        for (ClientHandler client : queue) {
            client.notifyRoundEnd();
        }
    }

    private void endGame() {
        lock.lock();
        try {
            clients.forEach(ClientHandler::notifyGameEnd);
            gameRunning = false;
            announceWinner();
        } finally {
            lock.unlock();
        }
    }

    private void announceWinner() {
        Optional<ClientHandler> winner = clients.stream().max(Comparator.comparingInt(ClientHandler::getPoints));
        winner.ifPresent(clientHandler -> {
            try {
                clients.forEach(client -> client.out.println("The winner is " + clientHandler.getPoints() + " points."));
                clientHandler.out.println("Congratulations! You are the winner with " + clientHandler.getPoints() + " points.");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error announcing winner", e);
            }
        });
    }

    public static void main(String[] args) {
        GameServer server = new GameServer();
        server.start();
    }
}
