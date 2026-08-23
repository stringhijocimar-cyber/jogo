package com.jocimar.trilha.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TrilhaGame {
    public static final int EMPTY = 0;
    public static final int HUMAN = 1;
    public static final int AI = 2;

    public enum Phase { PLACEMENT, MOVEMENT, GAME_OVER }

    public static final class Action {
        public final int from;
        public final int to;

        public Action(int from, int to) {
            this.from = from;
            this.to = to;
        }

        public boolean isPlacement() {
            return from < 0;
        }
    }

    private static final int[][] MILLS = {
            {0,1,2}, {3,4,5}, {6,7,8}, {9,10,11},
            {12,13,14}, {15,16,17}, {18,19,20}, {21,22,23},
            {0,9,21}, {3,10,18}, {6,11,15}, {1,4,7},
            {16,19,22}, {8,12,17}, {5,13,20}, {2,14,23}
    };

    private static final int[][] ADJ = {
            {1,9}, {0,2,4}, {1,14},
            {4,10}, {1,3,5,7}, {4,13},
            {7,11}, {4,6,8}, {7,12},
            {0,10,21}, {3,9,11,18}, {6,10,15},
            {8,13,17}, {5,12,14,20}, {2,13,23},
            {11,16}, {15,17,19}, {12,16},
            {10,19}, {16,18,20,22}, {13,19},
            {9,22}, {19,21,23}, {14,22}
    };

    private final int[] board = new int[24];
    private int placedHuman = 0;
    private int placedAi = 0;
    private int currentPlayer = HUMAN;
    private Phase phase = Phase.PLACEMENT;
    private boolean awaitingRemoval = false;
    private int winner = EMPTY;
    private String lastEvent = "Sua vez: coloque uma peça.";

    public TrilhaGame() {}

    private TrilhaGame(TrilhaGame other) {
        System.arraycopy(other.board, 0, board, 0, board.length);
        placedHuman = other.placedHuman;
        placedAi = other.placedAi;
        currentPlayer = other.currentPlayer;
        phase = other.phase;
        awaitingRemoval = other.awaitingRemoval;
        winner = other.winner;
        lastEvent = other.lastEvent;
    }

    public TrilhaGame copy() {
        return new TrilhaGame(this);
    }

    public int[] getBoardCopy() {
        int[] copy = new int[board.length];
        System.arraycopy(board, 0, copy, 0, board.length);
        return copy;
    }

    public int getAt(int position) {
        return isValidPosition(position) ? board[position] : EMPTY;
    }

    public int getCurrentPlayer() { return currentPlayer; }
    public Phase getPhase() { return phase; }
    public boolean isAwaitingRemoval() { return awaitingRemoval; }
    public int getWinner() { return winner; }
    public String getLastEvent() { return lastEvent; }
    public int getPlacedHuman() { return placedHuman; }
    public int getPlacedAi() { return placedAi; }

    public int getPiecesOnBoard(int player) {
        int count = 0;
        for (int value : board) if (value == player) count++;
        return count;
    }

    public boolean place(int position) {
        if (phase != Phase.PLACEMENT || awaitingRemoval || !isValidPosition(position) || board[position] != EMPTY) {
            return false;
        }
        int player = currentPlayer;
        board[position] = player;
        if (player == HUMAN) placedHuman++; else placedAi++;
        lastEvent = player == HUMAN ? "Peça colocada." : "Oponente colocou uma peça.";
        finishPrimaryAction(position, player);
        return true;
    }

    public boolean move(int from, int to) {
        if (phase != Phase.MOVEMENT || awaitingRemoval || !isValidPosition(from) || !isValidPosition(to)) return false;
        if (board[from] != currentPlayer || board[to] != EMPTY) return false;
        if (!canFly(currentPlayer) && !areAdjacent(from, to)) return false;

        int player = currentPlayer;
        board[from] = EMPTY;
        board[to] = player;
        lastEvent = player == HUMAN ? "Peça movimentada." : "Oponente movimentou uma peça.";
        finishPrimaryAction(to, player);
        return true;
    }

    public boolean remove(int position) {
        if (!awaitingRemoval || !isValidPosition(position)) return false;
        int opponent = other(currentPlayer);
        if (board[position] != opponent || !canRemovePosition(position, opponent)) return false;

        board[position] = EMPTY;
        awaitingRemoval = false;
        lastEvent = currentPlayer == HUMAN ? "Você capturou uma peça." : "Oponente capturou uma peça.";

        if (phase == Phase.MOVEMENT && getPiecesOnBoard(opponent) < 3) {
            setWinner(currentPlayer, "Vitória por reduzir o adversário a menos de 3 peças.");
            return true;
        }

        switchTurn();
        return true;
    }

    private void finishPrimaryAction(int destination, int player) {
        if (formsMill(destination, player)) {
            awaitingRemoval = true;
            lastEvent = player == HUMAN ? "Moinho! Remova uma peça adversária." : "Oponente formou um moinho.";
            return;
        }
        switchTurn();
    }

    private void switchTurn() {
        if (phase == Phase.PLACEMENT && placedHuman >= 9 && placedAi >= 9) {
            phase = Phase.MOVEMENT;
        }
        currentPlayer = other(currentPlayer);
        updateTerminalState();
        if (phase != Phase.GAME_OVER) {
            lastEvent = buildTurnMessage();
        }
    }

    private String buildTurnMessage() {
        String who = currentPlayer == HUMAN ? "Sua vez" : "Vez do oponente";
        if (phase == Phase.PLACEMENT) return who + ": coloque uma peça.";
        if (canFly(currentPlayer)) return who + ": você pode voar para qualquer ponto livre.";
        return who + ": mova uma peça para um ponto conectado.";
    }

    private void updateTerminalState() {
        if (phase != Phase.MOVEMENT) return;
        if (getPiecesOnBoard(currentPlayer) < 3) {
            setWinner(other(currentPlayer), "Fim de jogo: menos de 3 peças.");
            return;
        }
        if (getLegalActions(currentPlayer).isEmpty()) {
            setWinner(other(currentPlayer), "Fim de jogo: jogador sem movimentos legais.");
        }
    }

    private void setWinner(int player, String reason) {
        winner = player;
        phase = Phase.GAME_OVER;
        awaitingRemoval = false;
        lastEvent = (player == HUMAN ? "Você venceu! " : "Oponente venceu. ") + reason;
    }

    public boolean formsMill(int position, int player) {
        if (!isValidPosition(position) || board[position] != player) return false;
        for (int[] mill : MILLS) {
            if (contains(mill, position) && board[mill[0]] == player && board[mill[1]] == player && board[mill[2]] == player) {
                return true;
            }
        }
        return false;
    }

    public boolean isPartOfMill(int position, int player) {
        return formsMill(position, player);
    }

    public boolean canRemovePosition(int position, int opponent) {
        if (!isValidPosition(position) || board[position] != opponent) return false;
        if (!isPartOfMill(position, opponent)) return true;
        for (int i = 0; i < board.length; i++) {
            if (board[i] == opponent && !isPartOfMill(i, opponent)) return false;
        }
        return true;
    }

    public List<Integer> getRemovablePositions(int opponent) {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < board.length; i++) {
            if (canRemovePosition(i, opponent)) out.add(i);
        }
        return out;
    }

    public List<Action> getLegalActions(int player) {
        if (phase == Phase.GAME_OVER || awaitingRemoval) return Collections.emptyList();
        List<Action> actions = new ArrayList<>();
        if (phase == Phase.PLACEMENT) {
            for (int i = 0; i < board.length; i++) if (board[i] == EMPTY) actions.add(new Action(-1, i));
            return actions;
        }

        boolean fly = canFly(player);
        for (int from = 0; from < board.length; from++) {
            if (board[from] != player) continue;
            if (fly) {
                for (int to = 0; to < board.length; to++) if (board[to] == EMPTY) actions.add(new Action(from, to));
            } else {
                for (int to : ADJ[from]) if (board[to] == EMPTY) actions.add(new Action(from, to));
            }
        }
        return actions;
    }

    public boolean applyAction(Action action) {
        if (action == null) return false;
        return action.isPlacement() ? place(action.to) : move(action.from, action.to);
    }

    public boolean canFly(int player) {
        return phase == Phase.MOVEMENT && getPiecesOnBoard(player) == 3;
    }

    public int countMills(int player) {
        int count = 0;
        for (int[] mill : MILLS) {
            if (board[mill[0]] == player && board[mill[1]] == player && board[mill[2]] == player) count++;
        }
        return count;
    }

    public int countPotentialMills(int player) {
        int count = 0;
        for (int[] mill : MILLS) {
            int own = 0;
            int empty = 0;
            for (int p : mill) {
                if (board[p] == player) own++;
                else if (board[p] == EMPTY) empty++;
            }
            if (own == 2 && empty == 1) count++;
        }
        return count;
    }

    public int mobility(int player) {
        if (phase != Phase.MOVEMENT) return 0;
        return getLegalActions(player).size();
    }

    public int otherPlayer(int player) { return other(player); }

    public void forceCurrentPlayerForSimulation(int player) {
        currentPlayer = player;
    }

    private static int other(int player) {
        return player == HUMAN ? AI : HUMAN;
    }

    private static boolean contains(int[] array, int value) {
        for (int item : array) if (item == value) return true;
        return false;
    }

    public static boolean areAdjacent(int a, int b) {
        if (!isValidPosition(a) || !isValidPosition(b)) return false;
        for (int neighbor : ADJ[a]) if (neighbor == b) return true;
        return false;
    }

    private static boolean isValidPosition(int p) {
        return p >= 0 && p < 24;
    }
}
