import java.io.*;
import java.util.*;

public class LudoGame implements Runnable {
    private int numPlayers;
    private List<Player> players;
    private int currentPlayerIndex;
    private List<ObjectOutputStream> outputStreams;
    private List<ObjectInputStream> inputStreams;

    public LudoGame(int numPlayers, List<GameServer.ClientHandler> handlers) {
        this.numPlayers = numPlayers;
        this.players = new ArrayList<>();
        this.outputStreams = new ArrayList<>();
        this.inputStreams = new ArrayList<>();
        for (int i = 0; i < numPlayers; i++) {
            players.add(new Player("Player" + (i + 1)));
            try {
                outputStreams.add(handlers.get(i).getOut());
                inputStreams.add(handlers.get(i).getIn());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        this.currentPlayerIndex = 0;
    }

    @Override
    public void run() {
        start();
    }

    public void start() {
        System.out.println("Starting game with " + numPlayers + " players");
        for (int i = 0; i < numPlayers; i++) {
            final int playerIndex = i;
            new Thread(() -> handlePlayer(playerIndex)).start();
        }
    }

    private void handlePlayer(int playerIndex) {
        Player player = players.get(playerIndex);
        ObjectOutputStream out = outputStreams.get(playerIndex);
        ObjectInputStream in = inputStreams.get(playerIndex);

        try {
            while (true) {
                synchronized (this) {
                    while (players.get(currentPlayerIndex) != player) {
                        wait();
                    }
                }

                // Notify the player that it's their turn
                out.writeObject("It's your turn. Roll the die (type 'roll')");
                out.flush();

                // Wait for the player to roll the die
                String command = (String) in.readObject();
                if ("roll".equalsIgnoreCase(command)) {
                    int roll = rollDie();
                    out.writeObject("You rolled a " + roll);
                    out.flush();
                    player.moveToken(roll);

                    // Update all players with the new game state
                    updateAllPlayers();

                    // Check for a win condition
                    if (player.hasWon()) {
                        out.writeObject("You won!");
                        out.flush();
                        break;
                    }
                }

                // Move to the next player
                synchronized (this) {
                    currentPlayerIndex = (currentPlayerIndex + 1) % numPlayers;
                    notifyAll();
                }
            }
        } catch (IOException | ClassNotFoundException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            closeResources(playerIndex);
        }
    }

    private void updateAllPlayers() {
        for (ObjectOutputStream out : outputStreams) {
            try {
                out.writeObject("Game state updated: " + getGameState());
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String getGameState() {
        StringBuilder gameState = new StringBuilder();
        for (Player player : players) {
            gameState.append(player.getName()).append(": ").append(player.getPosition()).append("\n");
        }
        return gameState.toString();
    }

    private int rollDie() {
        return new Random().nextInt(6) + 1;
    }

    private void closeResources(int playerIndex) {
        try {
            if (outputStreams.get(playerIndex) != null) outputStreams.get(playerIndex).close();
            if (inputStreams.get(playerIndex) != null) inputStreams.get(playerIndex).close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class Player {
        private String name;
        private int position;

        public Player(String name) {
            this.name = name;
            this.position = 0;
        }

        public String getName() {
            return name;
        }

        public int getPosition() {
            return position;
        }

        public void moveToken(int roll) {
            position += roll;
        }

        public boolean hasWon() {
            return position >= 100; // Example win condition
        }
    }
}
