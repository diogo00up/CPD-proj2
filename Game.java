import java.net.Socket;
import java.util.List;

public class Game implements Runnable {
    private List<Socket> userSockets;

    public Game(int players, List<Socket> userSockets) {
        this.userSockets = userSockets;
    }

    public void run() {
        System.out.println("Starting game with " + userSockets.size() + " players");
        // Game logic goes here
    }
}
