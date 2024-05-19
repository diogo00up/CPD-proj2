// TestClient.java

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TestClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in))) {

            String serverResponse;
            while ((serverResponse = in.readLine()) != null) {
                System.out.println(serverResponse);
                if (serverResponse.contains("Do you have a token?")) {
                    out.println(stdIn.readLine().trim());
                } else if (serverResponse.contains("Enter your token:")) {
                    out.println(stdIn.readLine().trim());
                } else if (serverResponse.contains("Enter username:") || serverResponse.contains("Enter password:")) {
                    out.println(stdIn.readLine().trim());
                } else if (serverResponse.contains("Minimum players connected")) {
                    System.out.println("Type 'ready' to start the game.");
                } else if (serverResponse.contains("The game has started")) {
                    System.out.println("Type 'roll' to roll the dice.");
                    break;
                }
            }

            String userInput;
            while ((userInput = stdIn.readLine()) != null) {
                out.println(userInput);
                String answer = in.readLine();
                System.out.println("Response from server: " + answer);

                if ("exit".equalsIgnoreCase(userInput)) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
