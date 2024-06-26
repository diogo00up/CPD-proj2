import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TestClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);  // Creates a socket to establish a connection with the server
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true); // Sets up input and output streams to communicate with the server.
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in))) { // Handles user input from the console

            String serverResponse;
            while ((serverResponse = in.readLine()) != null) {
                System.out.println(serverResponse);
                if (serverResponse.contains("Do you have a token?")) {
                    out.println(stdIn.readLine().trim());
                } else if (serverResponse.contains("Enter your token:") || serverResponse.contains("Enter username:") || serverResponse.contains("Enter password:")) {
                    out.println(stdIn.readLine().trim());
                } else if (serverResponse.contains("Authentication successful") || serverResponse.contains("Reconnection successful")) {
                    break;
                }
            }

            String userInput;
            boolean assister = false;

            // If authenticated, handle further commands
            while ((userInput = stdIn.readLine()) != null) {
                out.println(userInput);
                serverResponse = in.readLine();
                System.out.println("Response from server: " + serverResponse);
                if ("You are ready. Waiting for other players...".equals(serverResponse) || "Game is starting!".equals(serverResponse)) {
                    continue;
                }
                if (serverResponse.startsWith("Round") || serverResponse.startsWith("You rolled")) {
                    continue;
                }
                if ("exit".equalsIgnoreCase(userInput)) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}