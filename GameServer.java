import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GameServer {
    private static final int PORT = 12345;
    private static final int MAX_PLAYERS = 4; // Adjust as needed
    private List<ClientHandler> clients;
    private List<ClientHandler> authenticatedClients;
    private ExecutorService pool;
    private static final Logger logger = Logger.getLogger(GameServer.class.getName());

    public GameServer() {
        clients = new ArrayList<>();
        authenticatedClients = new ArrayList<>();
        pool = Executors.newVirtualThreadPerTaskExecutor(); //A thread pool to execute tasks concurrently
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {  //Creates a ServerSocket bound to the specified port
            logger.info("Game server started on port " + PORT);

            logger.info("WAITING FOR CLIENT CONECTION FROM PORT: " + PORT);

            while (true) {  // accept incoming client connection
                Socket clientSocket = serverSocket.accept();
                logger.info("New client connected: " + clientSocket.getInetAddress());  // new connection, it logs the connection
                ClientHandler clientHandler = new ClientHandler(clientSocket, this);  //creates a ClientHandler for the client
                clients.add(clientHandler);
                logger.info("Number of connected clients: " + clients.size()); // Log the number of connected clients
                pool.execute(clientHandler);  // submits the handler to the thread pool for execution
                                              //Task Execution: The ClientHandler handles communication with the client, performing authentication and echoing messages.
             
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error starting server", e);
        }
    }

    public synchronized void addAuthenticatedClient(ClientHandler clientHandler) {
        authenticatedClients.add(clientHandler);
        logger.info("Number of authenticated clients: " + authenticatedClients.size());

    }

    public boolean verify_number_players(){
        if (authenticatedClients.size() >= 2) {
            logger.info("THERE ARE A AT LEAST 2 SIGNED IN CLIENTS");
            return true;
        }
        return false;
    }
    

    
    public synchronized void removeClient(ClientHandler clientHandler) {
        clients.remove(clientHandler);
    }

    public static void main(String[] args) {
        GameServer server = new GameServer();
        server.start();
    }

    
}
