package com.jocimar.trilha.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class TrilhaAI {
    public enum Difficulty { EASY, MEDIUM, HARD }

    private final Random random = new Random();
    private Difficulty difficulty = Difficulty.MEDIUM;

    private static final int[] POSITION_WEIGHT = {
            2,3,2, 3,5,3, 3,5,3, 3,5,3,
            3,5,3, 3,5,3, 3,5,3, 2,3,2
    };

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty == null ? Difficulty.MEDIUM : difficulty;
    }

    public Difficulty getDifficulty() { return difficulty; }

    public TrilhaGame.Action chooseAction(TrilhaGame game) {
        List<TrilhaGame.Action> legal = new ArrayList<>(game.getLegalActions(TrilhaGame.AI));
        if (legal.isEmpty()) return null;
        if (difficulty == Difficulty.EASY) return legal.get(random.nextInt(legal.size()));

        Collections.shuffle(legal, random);
        TrilhaGame.Action best = legal.get(0);
        double bestScore = -Double.MAX_VALUE;

        for (TrilhaGame.Action action : legal) {
            double score = scoreAiAction(game, action);
            if (difficulty == Difficulty.HARD) score += lookAheadPenalty(game, action);
            if (score > bestScore) {
                bestScore = score;
                best = action;
            }
        }
        return best;
    }

    public int chooseRemoval(TrilhaGame game) {
        List<Integer> removable = game.getRemovablePositions(TrilhaGame.HUMAN);
        if (removable.isEmpty()) return -1;
        if (difficulty == Difficulty.EASY) return removable.get(random.nextInt(removable.size()));

        int best = removable.get(0);
        double bestScore = -Double.MAX_VALUE;
        for (int position : removable) {
            TrilhaGame copy = game.copy();
            if (!copy.remove(position)) continue;
            double score = evaluate(copy);
            int strategic = humanPieceStrategicValue(game, position);
            score += strategic * (difficulty == Difficulty.HARD ? 9 : 5);
            if (score > bestScore) {
                bestScore = score;
                best = position;
            }
        }
        return best;
    }

    private double scoreAiAction(TrilhaGame game, TrilhaGame.Action action) {
        double score = POSITION_WEIGHT[action.to] * 2.0;

        boolean blocksHumanMill = action.to >= 0 && wouldFormMill(game, action.to, TrilhaGame.HUMAN);
        if (blocksHumanMill) score += 190;

        TrilhaGame copy = game.copy();
        if (!copy.applyAction(action)) return -999999;

        if (copy.isAwaitingRemoval()) {
            score += 340;
            int removal = chooseBestRemovalForSimulation(copy, TrilhaGame.AI);
            if (removal >= 0) copy.remove(removal);
        }

        score += evaluate(copy);
        if (!action.isPlacement()) {
            score += copy.countPotentialMills(TrilhaGame.AI) * 20;
            if (game.isPartOfMill(action.from, TrilhaGame.AI) && !copy.isPartOfMill(action.to, TrilhaGame.AI)) {
                score += 14; // abrir moinho pode permitir fechá-lo novamente no próximo turno
            }
        }
        return score;
    }

    private double lookAheadPenalty(TrilhaGame game, TrilhaGame.Action action) {
        TrilhaGame afterAi = game.copy();
        if (!afterAi.applyAction(action)) return -100000;
        if (afterAi.isAwaitingRemoval()) {
            int removal = chooseBestRemovalForSimulation(afterAi, TrilhaGame.AI);
            if (removal >= 0) afterAi.remove(removal);
        }
        if (afterAi.getPhase() == TrilhaGame.Phase.GAME_OVER) return 100000;

        if (afterAi.getCurrentPlayer() != TrilhaGame.HUMAN) return 0;
        List<TrilhaGame.Action> humanActions = afterAi.getLegalActions(TrilhaGame.HUMAN);
        if (humanActions.isEmpty()) return 50000;

        double worstForAi = Double.MAX_VALUE;
        int inspected = 0;
        humanActions.sort(Comparator.comparingDouble((TrilhaGame.Action a) -> quickHumanPriority(afterAi, a)).reversed());
        for (TrilhaGame.Action humanAction : humanActions) {
            TrilhaGame reply = afterAi.copy();
            if (!reply.applyAction(humanAction)) continue;
            if (reply.isAwaitingRemoval()) {
                int removal = chooseBestRemovalForSimulation(reply, TrilhaGame.HUMAN);
                if (removal >= 0) reply.remove(removal);
            }
            worstForAi = Math.min(worstForAi, evaluate(reply));
            inspected++;
            if (inspected >= 14) break; // mantém resposta rápida mesmo em celulares modestos
        }
        if (worstForAi == Double.MAX_VALUE) return 0;
        return worstForAi * 0.55;
    }

    private double quickHumanPriority(TrilhaGame game, TrilhaGame.Action action) {
        double score = POSITION_WEIGHT[action.to];
        if (wouldFormMill(game, action.to, TrilhaGame.HUMAN)) score += 100;
        if (wouldFormMill(game, action.to, TrilhaGame.AI)) score += 50;
        return score;
    }

    private int chooseBestRemovalForSimulation(TrilhaGame game, int actor) {
        int opponent = actor == TrilhaGame.AI ? TrilhaGame.HUMAN : TrilhaGame.AI;
        List<Integer> removable = game.getRemovablePositions(opponent);
        if (removable.isEmpty()) return -1;

        int best = removable.get(0);
        double bestActorScore = -Double.MAX_VALUE;
        for (int pos : removable) {
            TrilhaGame copy = game.copy();
            if (!copy.remove(pos)) continue;
            double aiScore = evaluate(copy);
            double actorScore = actor == TrilhaGame.AI ? aiScore : -aiScore;
            if (actorScore > bestActorScore) {
                bestActorScore = actorScore;
                best = pos;
            }
        }
        return best;
    }

    private boolean wouldFormMill(TrilhaGame game, int position, int player) {
        if (position < 0 || game.getAt(position) != TrilhaGame.EMPTY) return false;
        TrilhaGame copy = game.copy();
        copy.forceCurrentPlayerForSimulation(player);
        if (copy.getPhase() == TrilhaGame.Phase.PLACEMENT) {
            if (!copy.place(position)) return false;
        } else {
            // Para bloqueio na movimentação, avalia se o ponto completa duas peças existentes.
            int[] board = game.getBoardCopy();
            board[position] = player;
            return completesMillOnBoard(board, position, player);
        }
        return copy.isAwaitingRemoval();
    }

    private boolean completesMillOnBoard(int[] board, int position, int player) {
        int[][] mills = {
                {0,1,2}, {3,4,5}, {6,7,8}, {9,10,11}, {12,13,14}, {15,16,17}, {18,19,20}, {21,22,23},
                {0,9,21}, {3,10,18}, {6,11,15}, {1,4,7}, {16,19,22}, {8,12,17}, {5,13,20}, {2,14,23}
        };
        for (int[] mill : mills) {
            boolean includes = false;
            for (int p : mill) if (p == position) includes = true;
            if (includes && board[mill[0]] == player && board[mill[1]] == player && board[mill[2]] == player) return true;
        }
        return false;
    }

    private int humanPieceStrategicValue(TrilhaGame game, int position) {
        int value = POSITION_WEIGHT[position];
        if (game.isPartOfMill(position, TrilhaGame.HUMAN)) value += 8;

        int before = game.countPotentialMills(TrilhaGame.HUMAN);
        TrilhaGame copy = game.copy();
        if (copy.isAwaitingRemoval() && copy.remove(position)) {
            int after = copy.countPotentialMills(TrilhaGame.HUMAN);
            value += Math.max(0, before - after) * 6;
        }
        return value;
    }

    private double evaluate(TrilhaGame game) {
        if (game.getPhase() == TrilhaGame.Phase.GAME_OVER) {
            if (game.getWinner() == TrilhaGame.AI) return 100000;
            if (game.getWinner() == TrilhaGame.HUMAN) return -100000;
        }

        int aiPieces = game.getPiecesOnBoard(TrilhaGame.AI);
        int humanPieces = game.getPiecesOnBoard(TrilhaGame.HUMAN);
        int pieceDelta = aiPieces - humanPieces;
        int millDelta = game.countMills(TrilhaGame.AI) - game.countMills(TrilhaGame.HUMAN);
        int potentialDelta = game.countPotentialMills(TrilhaGame.AI) - game.countPotentialMills(TrilhaGame.HUMAN);
        int mobilityDelta = game.getPhase() == TrilhaGame.Phase.MOVEMENT
                ? game.mobility(TrilhaGame.AI) - game.mobility(TrilhaGame.HUMAN) : 0;

        return pieceDelta * 125.0 + millDelta * 72.0 + potentialDelta * 18.0 + mobilityDelta * 2.2;
    }
}
