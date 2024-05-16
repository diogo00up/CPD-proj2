import java.io.*;
import java.util.*;

public class LudoGame implements Runnable {
    private int numPlayers;
    private List<Player> players;
    private List<ObjectOutputStream> outputStreams;
    private List<ObjectInputStream> inputStreams;

    public LudoGame(int numPlayers, List<GameServer.ClientHandler> handlers) {
        this.numPlayers = numPlayers;
        this.players = new ArrayList<>();
        this.outputStreams = new ArrayList<>();
        this.inputStreams = new ArrayList<>();
        for (int i = 0; i < numPlayers; i++) {
            players.add(new Player("Player" + (i + 1)));
            outputStreams.add(handlers.get(i).getOut());
            inputStreams.add(handlers.get(i).getIn());
        }
    }

    @Override
    public void run() {
        int currentPlayerIndex = 0;
        try {
            while (!isGameOver()) {
                Player currentPlayer = players.get(currentPlayerIndex);
                ObjectOutputStream out = outputStreams.get(currentPlayerIndex);
                ObjectInputStream in = inputStreams.get(currentPlayerIndex);

                out.writeObject("It's your turn. Roll the die (type 'roll')");
                out.flush();

                System.out.println("Waiting for command from Player " + (currentPlayerIndex + 1));

                String command = (String) in.readObject();
                System.out.println("Received command from Player " + (currentPlayerIndex + 1) + ": " + command);

                if ("roll".equalsIgnoreCase(command)) {
                    int roll = rollDie();
                    System.out.println("Player " + (currentPlayerIndex + 1) + " rolled a " + roll);
                    out.writeObject("You rolled a " + roll);
                    out.flush();
                    currentPlayer.moveToken(roll);
                    updateAllPlayers();
                    currentPlayerIndex = (currentPlayerIndex + 1) % numPlayers;
                } else {
                    out.writeObject("Invalid command. Please type 'roll'");
                    out.flush();
                }
            }
        } catch (Exception e) {
            System.err.println("Error during game execution: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean isGameOver() {
        for (Player player : players) {
            if (player.hasWon()) {
                return true;
            }
        }
        return false;
    }

    private void updateAllPlayers() {
        String gameState = getGameState();
        for (ObjectOutputStream out : outputStreams) {
            try {
                out.writeObject("Game state updated: " + gameState);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String getGameState() {
        StringBuilder gameState = new StringBuilder();
        for (Player player : players) {
            gameState.append(player.getName()).append(": Position ").append(player.getPosition()).append("\n");
        }
        return gameState.toString();
    }

    private int rollDie() {
        return new Random().nextInt(6) + 1;
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
            return position >= 100; // Assuming 100 is the win condition
        }
    }
}
