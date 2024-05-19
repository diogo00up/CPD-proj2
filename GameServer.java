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
    private final List<ClientHandler> clients;
    private final List<ClientHandler> authenticatedClients;
    private final ExecutorService pool;
    private final Map<String, ClientHandler> tokenMap;
    private final ReentrantLock lock = new ReentrantLock();
    private static final Logger logger = Logger.getLogger(GameServer.class.getName());

    // Priority Queue to manage client positions
    private final PriorityQueue<ClientHandler> queue = new PriorityQueue<>(Comparator.comparingInt(ClientHandler::getQueuePosition));


    public GameServer() {
        clients = new ArrayList<>();
        authenticatedClients = new ArrayList<>();
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
            if (!authenticatedClients.contains(clientHandler)) {
                authenticatedClients.add(clientHandler);
                queue.add(clientHandler); // Add to queue
                clientHandler.setQueuePosition(queue.size()); // Set position based on queue size
                logger.info("Number of authenticated clients: " + authenticatedClients.size());
            }
        } finally {
            lock.unlock();
        }
    }

    public boolean verifyNumberPlayers() {
        lock.lock();
        try {
            if (authenticatedClients.size() >= 2) {
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
            authenticatedClients.remove(clientHandler);
            queue.remove(clientHandler); // Remove from queue
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

    public static void main(String[] args) {
        GameServer server = new GameServer();
        server.start();
    }
}