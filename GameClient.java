import java.io.*;
import java.net.*;

public class GameClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    public static void main(String[] args) {
        GameClient client = new GameClient();
        client.start();
    }

    public void start() {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
             BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {

            out.flush(); // Ensure stream header is sent immediately
            System.out.println("Connected to the game server.");

            // Authentication
            authenticate(in, out, reader);

            // Playing the game
            playGame(in, out, reader);

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error in client: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void authenticate(ObjectInputStream in, ObjectOutputStream out, BufferedReader reader) throws IOException, ClassNotFoundException {
        String response;
        while (true) {
            response = (String) in.readObject();
            System.out.print(response);
            String username = reader.readLine();
            out.writeObject(username);
            out.flush();

            response = (String) in.readObject();
            System.out.print(response);
            String password = reader.readLine();
            out.writeObject(password);
            out.flush();

            response = (String) in.readObject();
            System.out.println(response);
            if (response.equals("Authentication successful.")) {
                break;
            }
        }
    }

    private void playGame(ObjectInputStream in, ObjectOutputStream out, BufferedReader reader) throws IOException, ClassNotFoundException {
        String response;
        while (true) {
            response = (String) in.readObject();
            System.out.println(response);

            if (response.contains("It's your turn")) {
                System.out.print("Input command: ");
                String command = reader.readLine();
                System.out.println("Sending command: " + command);
                out.writeObject(command);
                out.flush();
            } else if (response.contains("You rolled a") || response.contains("Game state updated")) {
                // Handle game state updates
                System.out.println(response);
            }
        }
    }
}
