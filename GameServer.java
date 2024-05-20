import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GameServer {
    private static final int PORT = 12345;
    private static final int MAX_PLAYERS = 4; // Adjust as needed
    private final List<ClientHandler> clients;
    private final PriorityQueue<ClientHandler> queue = new PriorityQueue<>(Comparator.comparingInt(ClientHandler::getQueuePosition));
    private final ExecutorService pool;
    private final Map<String, ClientHandler> tokenMap;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition interactionCondition = lock.newCondition();
    private boolean firstThreadTurn = true; // To track whose turn it is
    private static final Logger logger = Logger.getLogger(GameServer.class.getName());

    public GameServer() {
        clients = new ArrayList<>();
        pool = Executors.newFixedThreadPool(MAX_PLAYERS); // A thread pool to execute tasks concurrently
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

    public ClientHandler getPlayerWithMostPoints() {
        lock.lock();
        try {
            ClientHandler topPlayer = null;
            int highestPoints = 0;

            for (ClientHandler client : queue) {
                if (client.getPoints() > highestPoints) {
                    highestPoints = client.getPoints();
                    topPlayer = client;
                }
            }
            
            return topPlayer;
        } finally {
            lock.unlock();
        }
    }

    public void removeClient(ClientHandler clientHandler) {
        lock.lock();
        try {
            clients.remove(clientHandler);
            queue.remove(clientHandler);
            // ... rest of the code ...
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

    public void waitForTurn(boolean isFirstThread) {
        lock.lock();
        try {
            while (firstThreadTurn != isFirstThread) {
                interactionCondition.await();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }
    }

    public void signalTurn(boolean isFirstThread) {
        lock.lock();
        try {
            firstThreadTurn = !isFirstThread;
            interactionCondition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public static void main(String[] args) {
        GameServer server = new GameServer();
        server.start();
    }

    public void closeAllConnections() {
        lock.lock();
        try {
            pool.shutdown();
            if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
            logger.info("All client connections closed and server shutdown.");
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }
    }
}
