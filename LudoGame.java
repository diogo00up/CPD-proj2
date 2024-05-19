// LudoGame.java

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class LudoGame {
    private static final int BOARD_SIZE = 52;
    private static final int TOKENS_PER_PLAYER = 4;
    private final int playerCount;
    private final List<Player> players;
    private final ReentrantLock gameLock;
    private boolean gameEnded;
    private int currentPlayerIndex;

    public LudoGame(int playerCount) {
        this.playerCount = playerCount;
        this.players = new ArrayList<>(playerCount);
        this.gameLock = new ReentrantLock();
        this.gameEnded = false;
        this.currentPlayerIndex = 0;
        initializePlayers();
    }

    private void initializePlayers() {
        for (int i = 0; i < playerCount; i++) {
            players.add(new Player(i, TOKENS_PER_PLAYER));
        }
    }

    public boolean moveToken(int playerId, int steps) {
        gameLock.lock();
        try {
            if (gameEnded || players.get(currentPlayerIndex).getId() != playerId) return false;

            Player player = players.get(playerId);
            for (Token token : player.getTokens()) {
                if (token.getPosition() + steps <= BOARD_SIZE) {
                    int newPosition = token.getPosition() + steps;

                    if (newPosition == BOARD_SIZE) {
                        token.setPosition(BOARD_SIZE);
                        player.incrementFinishedTokens();
                        if (player.getFinishedTokens() == TOKENS_PER_PLAYER) {
                            gameEnded = true;
                        }
                    } else {
                        token.setPosition(newPosition);
                        checkForCaptures(playerId, newPosition);
                    }
                    break;
                }
            }

            currentPlayerIndex = (currentPlayerIndex + 1) % playerCount;
            return true;
        } finally {
            gameLock.unlock();
        }
    }

    private void checkForCaptures(int playerId, int position) {
        for (Player player : players) {
            if (player.getId() != playerId) {
                for (Token token : player.getTokens()) {
                    if (token.getPosition() == position) {
                        token.setPosition(0);
                    }
                }
            }
        }
    }

    public boolean isGameEnded() {
        return gameEnded;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public String getBoardState() {
        StringBuilder boardState = new StringBuilder("Board positions:\n");
        for (Player player : players) {
            boardState.append("Player ").append(player.getId()).append(": ");
            for (Token token : player.getTokens()) {
                boardState.append(token.getPosition()).append(" ");
            }
            boardState.append("\n");
        }
        return boardState.toString();
    }

    public void startGame() {
        System.out.println("Starting the game!");
        while (!isGameEnded()) {
            Player currentPlayer = players.get(currentPlayerIndex);
            int playerId = currentPlayer.getId();
            int steps = rollDice(); // Assuming you have a rollDice() method to generate random steps
            boolean moveSuccessful = moveToken(playerId, steps);
            if (moveSuccessful) {
                System.out.println("Player " + playerId + " moved " + steps + " steps.");
                System.out.println(getBoardState());
            } else {
                System.out.println("Invalid move for Player " + playerId + ".");
            }
        }
        System.out.println("Game ended!");
    }

    private int rollDice() {
        // Generate a random number between 1 and 6
        return (int) (Math.random() * 6) + 1;
    }
}

class Player {
    private final int id;
    private final List<Token> tokens;
    private int finishedTokens;

    public Player(int id, int tokenCount) {
        this.id = id;
        this.tokens = new ArrayList<>(tokenCount);
        for (int i = 0; i < tokenCount; i++) {
            tokens.add(new Token());
        }
        this.finishedTokens = 0;
    }

    public int getId() {
        return id;
    }

    public List<Token> getTokens() {
        return tokens;
    }

    public int getFinishedTokens() {
        return finishedTokens;
    }

    public void incrementFinishedTokens() {
        finishedTokens++;
    }
}

class Token {
    private int position;

    public Token() {
        this.position = 0;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}
