import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

public class GameServer {
    private final int port;
    private ServerSocket serverSocket;
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, User> authenticatedUsers = new ConcurrentHashMap<>();
    private final List<Game> games = new ArrayList<>();
    private final Lock gamesLock = new ReentrantLock();

    public GameServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("Server started on port " + port);
        while (true) {
            Socket clientSocket = serverSocket.accept();
            new ClientHandler(clientSocket, this).start();
        }
    }

    public static void main(String[] args) {
        int port = 12345; // default port
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }
        try {
            new GameServer(port).start();
        } catch (IOException e) {
            System.err.println("Server failed to start: " + e.getMessage());
        }
    }

    // User authentication and management
    public boolean authenticate(String username, String password) {
        User user = users.get(username);
        if (user != null && user.getPassword().equals(password)) {
            authenticatedUsers.put(username, user);
            return true;
        }
        return false;
    }

    // Game session management
    public void startGame(List<User> players) {
        Game game = new Game(players.size(), new ArrayList<>(players));
        gamesLock.lock();
        try {
            games.add(game);
        } finally {
            gamesLock.unlock();
        }
        new Thread(game::start).start();
    }
}

class ClientHandler extends Thread {
    private final Socket socket;
    private final GameServer server;
    private User user;

    public ClientHandler(Socket socket, GameServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try (ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream())) {

            // Handle user authentication
            Object obj = input.readObject();
            if (obj instanceof String) {
                String[] credentials = ((String) obj).split(":");
                if (credentials.length == 2 && server.authenticate(credentials[0], credentials[1])) {
                    user = new User(credentials[0], credentials[1]);
                    output.writeObject("Authenticated");
                    // Further communication
                } else {
                    output.writeObject("Authentication Failed");
                }
            }

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error handling client: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Error closing socket: " + e.getMessage());
            }
        }
    }
}
