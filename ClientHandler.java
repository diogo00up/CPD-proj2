import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientHandler implements Runnable {
    private Socket clientSocket;    //The socket connected to the client.
    private GameServer server;      //Reference to the GameServer instance to manage the client
    private BufferedReader in;       //BufferedReader for reading input from the client
    private PrintWriter out;          //PrintWriter for sending output to the client.
    private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());  //: Logger to log activities related to the client handler.
    private static Map<String, String> userDatabase = new HashMap<>(); // Mock user database
    private int points;

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


    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            // Handle authentication
            if (authenticate()) {
                server.addAuthenticatedClient(this);  // verificar o numero de jogadores conectados e com login feito
                out.println("Authentication successful. Type 'exit' to disconnect, or type anything else to echo:");
                logger.info("Client authenticated: " + clientSocket.getInetAddress());  // printing  the server 

                // Handle echo commands

                
                String inputLine;
                while ((inputLine = in.readLine()) != null) {

                    if ("exit".equalsIgnoreCase(inputLine)) {
                        break;
                    }


                    //server.verify_number_players();  // a verificar se estao 2 ou mais jogadores conectados

                    if(server.verify_number_players() == true ){
                        logger.info("ESTAO 2 JOGADORES");
                        logger.info("Responding to client, after receiving " + inputLine + " from the client");
                        out.println("Two players authenticated! Joining the game!Receiving the board!");
                        startGame();
                        break;
                    }

                    else{
                        logger.info("ESTA SO UM JOGADOR");
                        out.println(inputLine);  
                        logger.info("Responding to client, after receiving " + inputLine + " from the client");
                    }
            

                  
                }
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

    private boolean authenticate() throws IOException {
        out.println("Enter username:");
        String username = in.readLine();
        out.println("Enter password:");
        String password = in.readLine();

        String storedPassword = userDatabase.get(username);
        boolean authenticated = storedPassword != null && storedPassword.equals(password);
        if (authenticated) {
            logger.info("User authenticated: " + username);
        } else {
            logger.warning("Authentication failed for user: " + username);
        }
        return authenticated;
    }

    private void startGame() throws IOException {
        for (int i = 1; i <= 4; i++) {
            String inputLine = in.readLine();
            if (inputLine == null) {
                logger.info("ITS A NULL ");
                break;
            }
            logger.info("Responding to client, after receiving " + inputLine + " from the client");
            // Process the input and update points
            // Here, we assume some logic to calculate points
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

