import java.io.*;
import java.net.*;

public class GameClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
             BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {

            out.flush(); // Ensure stream header is sent immediately
            System.out.println("Connected to the game server.");

            // Authentication
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

            // Waiting for the game to start
            while (true) {
                response = (String) in.readObject();
                System.out.println(response);
                if (response.contains("Game started")) {
                    break;
                }
            }

            // Playing the game
            while (true) {
                response = (String) in.readObject();
                System.out.println(response);

                if (response.contains("Your turn")) {
                    String command = reader.readLine();
                    out.writeObject(command);
                    out.flush();
                }
            }

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
