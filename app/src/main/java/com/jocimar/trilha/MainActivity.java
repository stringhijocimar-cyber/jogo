package com.jocimar.trilha;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.jocimar.trilha.game.TrilhaAI;
import com.jocimar.trilha.game.TrilhaGame;
import com.jocimar.trilha.ui.BoardView;

public class MainActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TrilhaGame game;
    private final TrilhaAI ai = new TrilhaAI();

    private BoardView boardView;
    private TextView statusView;
    private TextView counterView;
    private Spinner difficultySpinner;
    private boolean aiBusy = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        startNewGame();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(14), dp(16), dp(14));
        root.setBackgroundColor(Color.rgb(7, 13, 24));

        TextView title = text("TRILHA", 28, Color.rgb(226, 195, 119));
        title.setGravity(Gravity.CENTER);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        root.addView(title, matchWrap());

        TextView subtitle = text("Estratégia clássica • 9 peças por jogador", 13, Color.rgb(166, 177, 196));
        subtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subParams = matchWrap();
        subParams.bottomMargin = dp(10);
        root.addView(subtitle, subParams);

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER_VERTICAL);

        TextView difficultyLabel = text("Dificuldade", 14, Color.WHITE);
        controls.addView(difficultyLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        difficultySpinner = new Spinner(this);
        String[] levels = {"Fácil", "Médio", "Difícil"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, levels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        difficultySpinner.setAdapter(adapter);
        difficultySpinner.setSelection(1);
        difficultySpinner.setBackgroundColor(Color.rgb(235, 239, 245));
        difficultySpinner.setPadding(dp(10), 0, dp(10), 0);
        controls.addView(difficultySpinner, new LinearLayout.LayoutParams(dp(130), dp(44)));
        root.addView(controls, matchWrap());

        statusView = text("", 16, Color.WHITE);
        statusView.setGravity(Gravity.CENTER);
        statusView.setPadding(dp(8), dp(10), dp(8), dp(4));
        root.addView(statusView, matchWrap());

        counterView = text("", 12, Color.rgb(166, 177, 196));
        counterView.setGravity(Gravity.CENTER);
        root.addView(counterView, matchWrap());

        boardView = new BoardView(this);
        LinearLayout.LayoutParams boardParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        boardParams.topMargin = dp(6);
        boardParams.bottomMargin = dp(8);
        root.addView(boardView, boardParams);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.CENTER);

        Button newGame = new Button(this);
        newGame.setText("NOVO JOGO");
        newGame.setAllCaps(false);
        newGame.setOnClickListener(v -> startNewGame());
        buttons.addView(newGame, new LinearLayout.LayoutParams(0, dp(50), 1f));

        View spacer = new View(this);
        buttons.addView(spacer, new LinearLayout.LayoutParams(dp(10), 1));

        Button rules = new Button(this);
        rules.setText("COMO JOGAR");
        rules.setAllCaps(false);
        rules.setOnClickListener(v -> showRules());
        buttons.addView(rules, new LinearLayout.LayoutParams(0, dp(50), 1f));

        root.addView(buttons, matchWrap());
        setContentView(root);

        boardView.setListener(this::onGameChanged);
        difficultySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) ai.setDifficulty(TrilhaAI.Difficulty.EASY);
                else if (position == 2) ai.setDifficulty(TrilhaAI.Difficulty.HARD);
                else ai.setDifficulty(TrilhaAI.Difficulty.MEDIUM);
            }

            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void startNewGame() {
        handler.removeCallbacksAndMessages(null);
        aiBusy = false;
        game = new TrilhaGame();
        if (boardView != null) {
            boardView.setGame(game);
            boardView.setInputEnabled(true);
        }
        updateUi();
    }

    private void onGameChanged() {
        updateUi();
        if (game.getPhase() != TrilhaGame.Phase.GAME_OVER && game.getCurrentPlayer() == TrilhaGame.AI) {
            runAiTurn();
        }
    }

    private void runAiTurn() {
        if (aiBusy || game == null || game.getCurrentPlayer() != TrilhaGame.AI) return;
        aiBusy = true;
        boardView.setInputEnabled(false);
        statusView.setText("Oponente pensando…");

        handler.postDelayed(() -> {
            if (game == null || game.getPhase() == TrilhaGame.Phase.GAME_OVER) {
                aiBusy = false;
                updateUi();
                return;
            }

            if (!game.isAwaitingRemoval()) {
                TrilhaGame.Action action = ai.chooseAction(game);
                if (action != null) game.applyAction(action);
            }

            if (game.isAwaitingRemoval() && game.getCurrentPlayer() == TrilhaGame.AI) {
                int removal = ai.chooseRemoval(game);
                if (removal >= 0) game.remove(removal);
            }

            aiBusy = false;
            boardView.setInputEnabled(game.getCurrentPlayer() == TrilhaGame.HUMAN);
            boardView.invalidate();
            updateUi();
        }, 320);
    }

    private void updateUi() {
        if (game == null || statusView == null) return;
        statusView.setText(game.getLastEvent());

        String phase;
        if (game.getPhase() == TrilhaGame.Phase.PLACEMENT) phase = "Colocação";
        else if (game.getPhase() == TrilhaGame.Phase.MOVEMENT) phase = "Movimentação";
        else phase = "Finalizado";

        counterView.setText("Fase: " + phase + "   •   Você: " + game.getPiecesOnBoard(TrilhaGame.HUMAN)
                + "   •   Oponente: " + game.getPiecesOnBoard(TrilhaGame.AI));

        if (game.getPhase() == TrilhaGame.Phase.GAME_OVER) {
            boardView.setInputEnabled(false);
        }
    }

    private void showRules() {
        new AlertDialog.Builder(this)
                .setTitle("Como jogar Trilha")
                .setMessage("Objetivo: deixar o adversário com menos de 3 peças ou sem movimentos.\n\n"
                        + "1. COLOCAÇÃO — cada jogador coloca 9 peças, uma por turno.\n\n"
                        + "2. MOINHO — ao alinhar 3 peças, remova uma peça adversária. Uma peça dentro de um moinho só pode ser removida quando todas as peças adversárias estiverem em moinhos.\n\n"
                        + "3. MOVIMENTAÇÃO — depois das 18 peças colocadas, mova para um ponto vizinho conectado.\n\n"
                        + "4. VOO — quando restarem apenas 3 peças, elas podem ir para qualquer ponto livre.\n\n"
                        + "Você joga com as peças verdes. O oponente usa as vermelhas.")
                .setPositiveButton("ENTENDI", null)
                .show();
    }

    private TextView text(String value, float sp, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        return view;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
