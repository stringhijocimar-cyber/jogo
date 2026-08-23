package com.jocimar.trilha;

import android.app.Activity;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import com.jocimar.trilha.game.TrilhaAI;
import com.jocimar.trilha.game.TrilhaGame;
import com.jocimar.trilha.ui.BoardView;

public class MainActivity extends Activity {
    private enum Screen { MENU, DIFFICULTY, GAME }

    private static final int BG = Color.rgb(7, 13, 24);
    private static final int PANEL = Color.rgb(17, 29, 50);
    private static final int PANEL_2 = Color.rgb(24, 38, 59);
    private static final int GOLD = Color.rgb(215, 180, 106);
    private static final int BRONZE = Color.rgb(154, 109, 51);
    private static final int GREEN = Color.rgb(59, 209, 165);
    private static final int RED = Color.rgb(226, 103, 103);
    private static final int TEXT = Color.rgb(244, 246, 248);
    private static final int MUTED = Color.rgb(170, 181, 196);

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final TrilhaAI ai = new TrilhaAI();
    private TrilhaGame game;
    private BoardView boardView;

    private TextView playerCountView;
    private TextView aiCountView;
    private TextView phaseView;
    private TextView turnView;
    private TextView statusView;
    private TextView difficultyHudView;

    private final LinearLayout[] difficultyCards = new LinearLayout[3];
    private TrilhaAI.Difficulty selectedDifficulty = TrilhaAI.Difficulty.MEDIUM;
    private Screen currentScreen = Screen.MENU;
    private boolean aiBusy = false;
    private boolean resultShown = false;
    private boolean hapticsEnabled = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs = getSharedPreferences("trilha_settings", MODE_PRIVATE);
        hapticsEnabled = prefs.getBoolean("haptics", true);
        showMainMenu();
    }

    private void showMainMenu() {
        currentScreen = Screen.MENU;
        handler.removeCallbacksAndMessages(null);

        LinearLayout root = screenRoot();
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(22), dp(36), dp(22), dp(24));

        TextView emblem = text("◆  ━  ◆  ━  ◆", 18, GOLD, Typeface.BOLD);
        emblem.setGravity(Gravity.CENTER);
        root.addView(emblem, lpMatchWrap(0, dp(8)));

        TextView title = text("TRILHA", 46, GOLD, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setLetterSpacing(0.08f);
        root.addView(title, lpMatchWrap(0, 0));

        TextView edition = text("J O C I M A R", 15, Color.rgb(228, 190, 111), Typeface.BOLD);
        edition.setGravity(Gravity.CENTER);
        root.addView(edition, lpMatchWrap(0, dp(8)));

        TextView subtitle = text("Estratégia clássica. Decisões precisas.", 14, MUTED, Typeface.NORMAL);
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle, lpMatchWrap(0, dp(34)));

        root.addView(menuButton("▶   NOVO JOGO", GREEN, v -> showDifficultyScreen()), lpButton(dp(58), dp(12)));
        root.addView(menuButton("▦   COMO JOGAR", GOLD, v -> showRulesDialog()), lpButton(dp(56), dp(10)));
        root.addView(menuButton("⚙   CONFIGURAÇÕES", Color.rgb(95, 139, 183), v -> showSettingsDialog()), lpButton(dp(56), dp(10)));
        root.addView(menuButton("★   SOBRE", BRONZE, v -> showAboutDialog()), lpButton(dp(54), 0));

        TextView footer = text("Trilha Jocimar • Android", 11, Color.rgb(103, 116, 136), Typeface.NORMAL);
        footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams footerParams = lpMatchWrap(0, 0);
        footerParams.topMargin = dp(30);
        root.addView(footer, footerParams);

        setContentView(root);
    }

    private void showDifficultyScreen() {
        currentScreen = Screen.DIFFICULTY;
        LinearLayout root = screenRoot();
        root.setPadding(dp(18), dp(22), dp(18), dp(20));

        root.addView(topBar("DIFICULDADE", v -> showMainMenu()), lpMatchWrap(0, dp(16)));

        TextView hint = text("Escolha como a IA deve jogar contra você.", 14, MUTED, Typeface.NORMAL);
        hint.setGravity(Gravity.CENTER);
        root.addView(hint, lpMatchWrap(0, dp(18)));

        difficultyCards[0] = difficultyCard(0, "FÁCIL", "Jogadas mais simples e menos agressivas.", GREEN);
        difficultyCards[1] = difficultyCard(1, "MÉDIO", "Cria moinhos, bloqueia jogadas e joga estrategicamente.", GOLD);
        difficultyCards[2] = difficultyCard(2, "DIFÍCIL", "Analisa suas respostas e procura punir erros.", RED);

        root.addView(difficultyCards[0], lpMatchWrap(0, dp(12)));
        root.addView(difficultyCards[1], lpMatchWrap(0, dp(12)));
        root.addView(difficultyCards[2], lpMatchWrap(0, dp(22)));
        updateDifficultyCards();

        root.addView(menuButton("COMEÇAR JOGO", GREEN, v -> showGameScreen()), lpButton(dp(58), 0));
        setContentView(root);
    }

    private void showGameScreen() {
        currentScreen = Screen.GAME;
        resultShown = false;
        ai.setDifficulty(selectedDifficulty);

        LinearLayout root = screenRoot();
        root.setPadding(dp(12), dp(10), dp(12), dp(10));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = smallAction("‹ MENU", v -> showMainMenu());
        header.addView(back, new LinearLayout.LayoutParams(dp(82), dp(38)));
        TextView gameTitle = text("TRILHA", 19, GOLD, Typeface.BOLD);
        gameTitle.setGravity(Gravity.CENTER);
        header.addView(gameTitle, new LinearLayout.LayoutParams(0, dp(38), 1f));
        TextView rules = smallAction("? REGRAS", v -> showRulesDialog());
        header.addView(rules, new LinearLayout.LayoutParams(dp(82), dp(38)));
        root.addView(header, lpMatchWrap(0, dp(8)));

        LinearLayout hud = new LinearLayout(this);
        hud.setOrientation(LinearLayout.HORIZONTAL);
        hud.setGravity(Gravity.CENTER);

        LinearLayout playerCard = hudCard(true);
        TextView playerLabel = text("VOCÊ", 12, GREEN, Typeface.BOLD);
        playerLabel.setGravity(Gravity.CENTER);
        playerCountView = text("0", 26, TEXT, Typeface.BOLD);
        playerCountView.setGravity(Gravity.CENTER);
        playerCard.addView(playerLabel, lpMatchWrap(0, 0));
        playerCard.addView(playerCountView, lpMatchWrap(0, 0));
        hud.addView(playerCard, new LinearLayout.LayoutParams(0, dp(82), 1f));

        View hGap1 = new View(this);
        hud.addView(hGap1, new LinearLayout.LayoutParams(dp(7), 1));

        LinearLayout centerCard = hudCard(false);
        phaseView = text("COLOCAÇÃO", 12, GOLD, Typeface.BOLD);
        phaseView.setGravity(Gravity.CENTER);
        turnView = text("SUA VEZ", 15, TEXT, Typeface.BOLD);
        turnView.setGravity(Gravity.CENTER);
        centerCard.addView(phaseView, lpMatchWrap(0, dp(4)));
        centerCard.addView(turnView, lpMatchWrap(0, 0));
        hud.addView(centerCard, new LinearLayout.LayoutParams(0, dp(82), 1.3f));

        View hGap2 = new View(this);
        hud.addView(hGap2, new LinearLayout.LayoutParams(dp(7), 1));

        LinearLayout aiCard = hudCard(false);
        difficultyHudView = text("IA • " + difficultyName(), 11, RED, Typeface.BOLD);
        difficultyHudView.setGravity(Gravity.CENTER);
        aiCountView = text("0", 26, TEXT, Typeface.BOLD);
        aiCountView.setGravity(Gravity.CENTER);
        aiCard.addView(difficultyHudView, lpMatchWrap(0, 0));
        aiCard.addView(aiCountView, lpMatchWrap(0, 0));
        hud.addView(aiCard, new LinearLayout.LayoutParams(0, dp(82), 1f));
        root.addView(hud, lpMatchWrap(0, dp(8)));

        boardView = new BoardView(this);
        boardView.setHapticsEnabled(hapticsEnabled);
        boardView.setListener(this::onGameChanged);
        LinearLayout.LayoutParams boardParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        boardParams.bottomMargin = dp(8);
        root.addView(boardView, boardParams);

        statusView = text("", 14, TEXT, Typeface.BOLD);
        statusView.setGravity(Gravity.CENTER);
        statusView.setPadding(dp(10), dp(10), dp(10), dp(10));
        statusView.setBackground(rounded(PANEL_2, dp(14), Color.rgb(54, 73, 99), dp(1)));
        root.addView(statusView, lpMatchWrap(0, dp(8)));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER);
        actions.addView(bottomAction("MENU", v -> showMainMenu()), new LinearLayout.LayoutParams(0, dp(48), 1f));
        actions.addView(horizontalSpacer(dp(7)), new LinearLayout.LayoutParams(dp(7), 1));
        actions.addView(bottomAction("NOVO JOGO", v -> startNewGame()), new LinearLayout.LayoutParams(0, dp(48), 1.25f));
        actions.addView(horizontalSpacer(dp(7)), new LinearLayout.LayoutParams(dp(7), 1));
        actions.addView(bottomAction("REGRAS", v -> showRulesDialog()), new LinearLayout.LayoutParams(0, dp(48), 1f));
        root.addView(actions, lpMatchWrap(0, 0));

        setContentView(root);
        startNewGame();
    }

    private void startNewGame() {
        handler.removeCallbacksAndMessages(null);
        aiBusy = false;
        resultShown = false;
        ai.setDifficulty(selectedDifficulty);
        game = new TrilhaGame();
        if (boardView != null) {
            boardView.setGame(game);
            boardView.setHapticsEnabled(hapticsEnabled);
            boardView.setInputEnabled(true);
        }
        updateUi();
    }

    private void onGameChanged() {
        updateUi();
        if (game != null && game.getPhase() != TrilhaGame.Phase.GAME_OVER && game.getCurrentPlayer() == TrilhaGame.AI) {
            runAiTurn();
        }
    }

    private void runAiTurn() {
        if (aiBusy || game == null || game.getCurrentPlayer() != TrilhaGame.AI) return;
        aiBusy = true;
        boardView.setInputEnabled(false);
        turnView.setText("IA PENSANDO…");
        statusView.setText("Oponente analisando a jogada.");

        handler.postDelayed(() -> {
            if (game == null || game.getPhase() == TrilhaGame.Phase.GAME_OVER || currentScreen != Screen.GAME) {
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
        }, 480);
    }

    private void updateUi() {
        if (game == null || currentScreen != Screen.GAME || statusView == null) return;

        playerCountView.setText(String.valueOf(game.getPiecesOnBoard(TrilhaGame.HUMAN)));
        aiCountView.setText(String.valueOf(game.getPiecesOnBoard(TrilhaGame.AI)));
        difficultyHudView.setText("IA • " + difficultyName());

        String phase;
        if (game.getPhase() == TrilhaGame.Phase.PLACEMENT) phase = "COLOCAÇÃO";
        else if (game.getPhase() == TrilhaGame.Phase.GAME_OVER) phase = "FINALIZADO";
        else if (game.canFly(game.getCurrentPlayer())) phase = "VOO";
        else phase = "MOVIMENTO";
        phaseView.setText(phase);

        if (game.getPhase() == TrilhaGame.Phase.GAME_OVER) {
            turnView.setText(game.getWinner() == TrilhaGame.HUMAN ? "VITÓRIA" : "DERROTA");
        } else if (aiBusy || game.getCurrentPlayer() == TrilhaGame.AI) {
            turnView.setText("IA PENSANDO…");
        } else if (game.isAwaitingRemoval()) {
            turnView.setText("MOINHO!");
        } else {
            turnView.setText("SUA VEZ");
        }

        statusView.setText(contextMessage());
        if (boardView != null) {
            boardView.setInputEnabled(!aiBusy && game.getCurrentPlayer() == TrilhaGame.HUMAN && game.getPhase() != TrilhaGame.Phase.GAME_OVER);
            boardView.invalidate();
        }

        if (game.getPhase() == TrilhaGame.Phase.GAME_OVER && !resultShown) {
            resultShown = true;
            handler.postDelayed(this::showResultDialog, 260);
        }
    }

    private String contextMessage() {
        if (game.getPhase() == TrilhaGame.Phase.GAME_OVER) return game.getLastEvent();
        if (aiBusy || game.getCurrentPlayer() == TrilhaGame.AI) return "IA pensando…";
        if (game.isAwaitingRemoval()) return "Moinho! Toque em uma peça destacada do oponente para capturar.";
        if (game.getPhase() == TrilhaGame.Phase.PLACEMENT) return "Sua vez: toque em um ponto livre para colocar uma peça.";
        if (game.canFly(TrilhaGame.HUMAN)) return "Fase de voo: selecione uma peça e mova para qualquer ponto livre.";
        return "Selecione uma peça verde. Os destinos válidos serão destacados.";
    }

    private void showResultDialog() {
        if (game == null || currentScreen != Screen.GAME) return;
        boolean won = game.getWinner() == TrilhaGame.HUMAN;
        Dialog dialog = baseDialog();
        LinearLayout box = dialogBox();

        TextView icon = text(won ? "★" : "◆", 38, won ? GOLD : RED, Typeface.BOLD);
        icon.setGravity(Gravity.CENTER);
        box.addView(icon, lpMatchWrap(0, dp(4)));
        TextView title = text(won ? "VITÓRIA" : "DERROTA", 27, won ? GREEN : RED, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        box.addView(title, lpMatchWrap(0, dp(8)));
        TextView message = text(won ? "Você venceu a partida." : "A IA venceu esta partida.", 15, TEXT, Typeface.NORMAL);
        message.setGravity(Gravity.CENTER);
        box.addView(message, lpMatchWrap(0, dp(20)));

        TextView again = dialogAction("JOGAR NOVAMENTE", GREEN);
        again.setOnClickListener(v -> { dialog.dismiss(); startNewGame(); });
        box.addView(again, lpButton(dp(52), dp(9)));
        TextView change = dialogAction("TROCAR DIFICULDADE", GOLD);
        change.setOnClickListener(v -> { dialog.dismiss(); showDifficultyScreen(); });
        box.addView(change, lpButton(dp(50), dp(9)));
        TextView menu = dialogAction("MENU PRINCIPAL", BRONZE);
        menu.setOnClickListener(v -> { dialog.dismiss(); showMainMenu(); });
        box.addView(menu, lpButton(dp(50), 0));

        dialog.setContentView(box);
        fitDialog(dialog);
        dialog.show();
    }

    private void showRulesDialog() {
        Dialog dialog = baseDialog();
        ScrollView scroll = new ScrollView(this);
        LinearLayout box = dialogBox();
        scroll.addView(box);

        TextView title = text("COMO JOGAR", 24, GOLD, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        box.addView(title, lpMatchWrap(0, dp(16)));
        box.addView(ruleSection("OBJETIVO", "Forme moinhos e reduza o adversário a menos de três peças ou deixe-o sem movimentos."), lpMatchWrap(0, dp(10)));
        box.addView(ruleSection("1  COLOCAÇÃO", "Cada jogador coloca nove peças, alternando os turnos."), lpMatchWrap(0, dp(10)));
        box.addView(ruleSection("2  MOINHO", "Três peças alinhadas formam um moinho e permitem capturar uma peça adversária."), lpMatchWrap(0, dp(10)));
        box.addView(ruleSection("3  MOVIMENTAÇÃO", "Após colocar as 18 peças, mova para um ponto vizinho conectado."), lpMatchWrap(0, dp(10)));
        box.addView(ruleSection("4  VOO", "Com apenas três peças, você pode mover para qualquer ponto vazio."), lpMatchWrap(0, dp(18)));

        TextView close = dialogAction("ENTENDI", GREEN);
        close.setOnClickListener(v -> dialog.dismiss());
        box.addView(close, lpButton(dp(52), 0));
        dialog.setContentView(scroll);
        fitDialog(dialog);
        dialog.show();
    }

    private void showSettingsDialog() {
        Dialog dialog = baseDialog();
        LinearLayout box = dialogBox();
        TextView title = text("CONFIGURAÇÕES", 22, GOLD, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        box.addView(title, lpMatchWrap(0, dp(16)));

        Switch haptics = new Switch(this);
        haptics.setText("Feedback tátil");
        haptics.setTextColor(TEXT);
        haptics.setTextSize(16);
        haptics.setChecked(hapticsEnabled);
        haptics.setPadding(dp(4), dp(10), dp(4), dp(10));
        haptics.setOnCheckedChangeListener((buttonView, isChecked) -> {
            hapticsEnabled = isChecked;
            getSharedPreferences("trilha_settings", MODE_PRIVATE).edit().putBoolean("haptics", isChecked).apply();
            if (boardView != null) boardView.setHapticsEnabled(isChecked);
        });
        box.addView(haptics, lpMatchWrap(0, dp(16)));

        TextView close = dialogAction("FECHAR", GOLD);
        close.setOnClickListener(v -> dialog.dismiss());
        box.addView(close, lpButton(dp(50), 0));
        dialog.setContentView(box);
        fitDialog(dialog);
        dialog.show();
    }

    private void showAboutDialog() {
        Dialog dialog = baseDialog();
        LinearLayout box = dialogBox();
        TextView title = text("TRILHA JOCIMAR", 22, GOLD, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        box.addView(title, lpMatchWrap(0, dp(10)));
        TextView body = text("Jogo clássico de estratégia para Android.\n\nTrês níveis de dificuldade, regras completas de moinho, captura, movimentação e voo.", 14, MUTED, Typeface.NORMAL);
        body.setGravity(Gravity.CENTER);
        box.addView(body, lpMatchWrap(0, dp(18)));
        TextView close = dialogAction("FECHAR", GREEN);
        close.setOnClickListener(v -> dialog.dismiss());
        box.addView(close, lpButton(dp(50), 0));
        dialog.setContentView(box);
        fitDialog(dialog);
        dialog.show();
    }

    private LinearLayout difficultyCard(int index, String titleValue, String description, int accent) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(15), dp(18), dp(15));
        card.setClickable(true);
        card.setOnClickListener(v -> {
            if (index == 0) selectedDifficulty = TrilhaAI.Difficulty.EASY;
            else if (index == 2) selectedDifficulty = TrilhaAI.Difficulty.HARD;
            else selectedDifficulty = TrilhaAI.Difficulty.MEDIUM;
            updateDifficultyCards();
        });
        TextView title = text("●  " + titleValue, 18, accent, Typeface.BOLD);
        card.addView(title, lpMatchWrap(0, dp(5)));
        TextView desc = text(description, 13, MUTED, Typeface.NORMAL);
        card.addView(desc, lpMatchWrap(0, 0));
        return card;
    }

    private void updateDifficultyCards() {
        for (int i = 0; i < difficultyCards.length; i++) {
            if (difficultyCards[i] == null) continue;
            boolean selected = (i == 0 && selectedDifficulty == TrilhaAI.Difficulty.EASY)
                    || (i == 1 && selectedDifficulty == TrilhaAI.Difficulty.MEDIUM)
                    || (i == 2 && selectedDifficulty == TrilhaAI.Difficulty.HARD);
            int accent = i == 0 ? GREEN : (i == 1 ? GOLD : RED);
            difficultyCards[i].setBackground(rounded(selected ? Color.rgb(27, 43, 47) : PANEL, dp(16), selected ? accent : Color.rgb(50, 64, 82), dp(selected ? 2 : 1)));
            difficultyCards[i].setAlpha(selected ? 1f : 0.82f);
        }
    }

    private LinearLayout topBar(String titleValue, View.OnClickListener backAction) {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = smallAction("‹ VOLTAR", backAction);
        bar.addView(back, new LinearLayout.LayoutParams(dp(90), dp(40)));
        TextView title = text(titleValue, 20, GOLD, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        bar.addView(title, new LinearLayout.LayoutParams(0, dp(40), 1f));
        View placeholder = new View(this);
        bar.addView(placeholder, new LinearLayout.LayoutParams(dp(90), dp(40)));
        return bar;
    }

    private LinearLayout hudCard(boolean human) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(5), dp(8), dp(5), dp(8));
        card.setBackground(rounded(PANEL, dp(14), human ? Color.rgb(48, 101, 89) : Color.rgb(61, 70, 88), dp(1)));
        return card;
    }

    private TextView menuButton(String label, int accent, View.OnClickListener action) {
        TextView button = text(label, 16, TEXT, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setBackground(rippleButton(accent, dp(15)));
        button.setOnClickListener(action);
        return button;
    }

    private TextView bottomAction(String label, View.OnClickListener action) {
        TextView button = text(label, 12, TEXT, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setBackground(rippleButton(Color.rgb(57, 72, 92), dp(13)));
        button.setOnClickListener(action);
        return button;
    }

    private TextView smallAction(String label, View.OnClickListener action) {
        TextView button = text(label, 11, MUTED, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setBackground(rippleButton(Color.rgb(38, 51, 70), dp(10)));
        button.setOnClickListener(action);
        return button;
    }

    private TextView dialogAction(String label, int accent) {
        TextView button = text(label, 14, TEXT, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setBackground(rippleButton(accent, dp(13)));
        return button;
    }

    private LinearLayout ruleSection(String heading, String body) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackground(rounded(PANEL_2, dp(13), Color.rgb(61, 76, 97), dp(1)));
        TextView h = text(heading, 14, GREEN, Typeface.BOLD);
        card.addView(h, lpMatchWrap(0, dp(4)));
        TextView b = text(body, 13, TEXT, Typeface.NORMAL);
        card.addView(b, lpMatchWrap(0, 0));
        return card;
    }

    private LinearLayout screenRoot() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable background = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.rgb(7, 13, 24), Color.rgb(10, 19, 31), Color.rgb(5, 10, 18)});
        root.setBackground(background);
        return root;
    }

    private Dialog baseDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        return dialog;
    }

    private LinearLayout dialogBox() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(20), dp(20), dp(20));
        box.setBackground(rounded(Color.rgb(14, 24, 40), dp(20), BRONZE, dp(1)));
        return box;
    }

    private void fitDialog(Dialog dialog) {
        dialog.setOnShowListener(d -> {
            Window window = dialog.getWindow();
            if (window != null) {
                WindowManager.LayoutParams p = new WindowManager.LayoutParams();
                p.copyFrom(window.getAttributes());
                p.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.91f);
                p.height = WindowManager.LayoutParams.WRAP_CONTENT;
                window.setAttributes(p);
            }
        });
    }

    private RippleDrawable rippleButton(int color, int radius) {
        GradientDrawable shape = rounded(color, radius, lighten(color, 0.14f), dp(1));
        return new RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), shape, null);
    }

    private GradientDrawable rounded(int fill, int radius, int stroke, int strokeWidth) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(radius);
        drawable.setStroke(strokeWidth, stroke);
        return drawable;
    }

    private int lighten(int color, float amount) {
        int r = Math.min(255, (int) (Color.red(color) + 255 * amount));
        int g = Math.min(255, (int) (Color.green(color) + 255 * amount));
        int b = Math.min(255, (int) (Color.blue(color) + 255 * amount));
        return Color.rgb(r, g, b);
    }

    private TextView text(String value, float sp, int color, int style) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setTypeface(Typeface.create("sans", style));
        return view;
    }

    private View horizontalSpacer(int width) {
        View view = new View(this);
        view.setMinimumWidth(width);
        return view;
    }

    private LinearLayout.LayoutParams lpMatchWrap(int top, int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = top;
        p.bottomMargin = bottom;
        return p;
    }

    private LinearLayout.LayoutParams lpButton(int height, int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
        p.bottomMargin = bottom;
        return p;
    }

    private String difficultyName() {
        if (selectedDifficulty == TrilhaAI.Difficulty.EASY) return "FÁCIL";
        if (selectedDifficulty == TrilhaAI.Difficulty.HARD) return "DIFÍCIL";
        return "MÉDIO";
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    public void onBackPressed() {
        if (currentScreen == Screen.GAME || currentScreen == Screen.DIFFICULTY) showMainMenu();
        else super.onBackPressed();
    }
}
