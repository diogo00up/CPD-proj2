import java.io.*;
import java.net.*;

public class GameClient {
    private Socket socket;
    private ObjectOutputStream output;
    private ObjectInputStream input;

    public GameClient(String host, int port) {
        try {
            socket = new Socket(host, port);
            output = new ObjectOutputStream(socket.getOutputStream());
            input = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            System.err.println("Failed to connect to server: " + e.getMessage());
        }
    }

    public void authenticate(String username, String password) throws IOException {
        output.writeObject(username + ":" + password);
        try {
            Object response = input.readObject();
            if (response instanceof String) {
                System.out.println(response);
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Unknown data received from server: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java GameClient <host> <port> <username:password>");
            return;
        }
        GameClient client = new GameClient(args[0], Integer.parseInt(args[1]));
        try {
            client.authenticate(args[2].split(":")[0], args[2].split(":")[1]);
        } catch (IOException e) {
            System.err.println("Authentication failed: " + e.getMessage());
        }
    }
}
