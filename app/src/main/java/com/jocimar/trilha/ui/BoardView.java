package com.jocimar.trilha.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import com.jocimar.trilha.game.TrilhaGame;

import java.util.ArrayList;
import java.util.List;

public class BoardView extends View {
    public interface Listener {
        void onGameChanged();
    }

    private static final float[][] P = {
            {0f,0f}, {.5f,0f}, {1f,0f},
            {.1667f,.1667f}, {.5f,.1667f}, {.8333f,.1667f},
            {.3333f,.3333f}, {.5f,.3333f}, {.6667f,.3333f},
            {0f,.5f}, {.1667f,.5f}, {.3333f,.5f},
            {.6667f,.5f}, {.8333f,.5f}, {1f,.5f},
            {.3333f,.6667f}, {.5f,.6667f}, {.6667f,.6667f},
            {.1667f,.8333f}, {.5f,.8333f}, {.8333f,.8333f},
            {0f,1f}, {.5f,1f}, {1f,1f}
    };

    private static final int[][] LINKS = {
            {0,1},{1,2},{0,9},{2,14},
            {3,4},{4,5},{3,10},{5,13},
            {6,7},{7,8},{6,11},{8,12},
            {9,10},{10,11},{12,13},{13,14},
            {11,15},{12,17},{15,16},{16,17},
            {10,18},{13,20},{18,19},{19,20},
            {9,21},{14,23},{21,22},{22,23},
            {1,4},{4,7},{16,19},{19,22}
    };

    private final Paint panelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint panelBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint woodGrainPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint boardShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint boardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint piecePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pieceBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hintPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint capturePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint millPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private TrilhaGame game;
    private Listener listener;
    private int selected = -1;
    private boolean inputEnabled = true;
    private boolean hapticsEnabled = true;

    private float left;
    private float top;
    private float size;
    private float pieceRadius;

    public BoardView(Context context) {
        super(context);
        init();
    }

    public BoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        setBackgroundColor(Color.TRANSPARENT);

        panelBorderPaint.setColor(Color.rgb(111, 79, 39));
        panelBorderPaint.setStyle(Paint.Style.STROKE);
        panelBorderPaint.setStrokeWidth(dp(1.4f));

        woodGrainPaint.setColor(0x125C321A);
        woodGrainPaint.setStrokeWidth(dp(1));

        boardShadowPaint.setColor(0x55000000);
        boardShadowPaint.setStrokeWidth(dp(6));
        boardShadowPaint.setStyle(Paint.Style.STROKE);

        boardPaint.setColor(Color.rgb(210, 165, 87));
        boardPaint.setStrokeWidth(dp(2.6f));
        boardPaint.setStyle(Paint.Style.STROKE);
        boardPaint.setStrokeCap(Paint.Cap.ROUND);
        boardPaint.setShadowLayer(dp(2), 0, dp(1), 0x66000000);

        pointPaint.setColor(Color.rgb(229, 194, 126));
        pointPaint.setStyle(Paint.Style.FILL);

        pieceBorderPaint.setStyle(Paint.Style.STROKE);
        pieceBorderPaint.setStrokeWidth(dp(1.5f));
        pieceBorderPaint.setColor(0xCCFFFFFF);

        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(dp(3));
        ringPaint.setColor(Color.WHITE);
        ringPaint.setShadowLayer(dp(7), 0, 0, 0xAA45E8B5);

        hintPaint.setStyle(Paint.Style.STROKE);
        hintPaint.setStrokeWidth(dp(2.3f));
        hintPaint.setColor(Color.rgb(67, 229, 177));
        hintPaint.setShadowLayer(dp(7), 0, 0, 0x8843E5B1);

        capturePaint.setStyle(Paint.Style.STROKE);
        capturePaint.setStrokeWidth(dp(3));
        capturePaint.setColor(Color.rgb(255, 92, 92));
        capturePaint.setShadowLayer(dp(8), 0, 0, 0xAAFF3B3B);

        millPaint.setStyle(Paint.Style.STROKE);
        millPaint.setStrokeWidth(dp(3));
        millPaint.setColor(Color.rgb(238, 198, 105));
        millPaint.setShadowLayer(dp(9), 0, 0, 0x99EEC669);
    }

    public void setGame(TrilhaGame game) {
        this.game = game;
        selected = -1;
        invalidate();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setInputEnabled(boolean enabled) {
        inputEnabled = enabled;
        if (!enabled) selected = -1;
        invalidate();
    }

    public void setHapticsEnabled(boolean enabled) {
        hapticsEnabled = enabled;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int desired = Math.min(width, height > 0 ? height : width);
        if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) desired = width;
        setMeasuredDimension(width, Math.max(desired, dpInt(280)));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float availableW = getWidth() - dp(30);
        float availableH = getHeight() - dp(26);
        size = Math.min(availableW, availableH);
        left = (getWidth() - size) / 2f;
        top = (getHeight() - size) / 2f;
        pieceRadius = Math.max(dp(11), size * 0.039f);

        drawPanel(canvas);
        drawBoard(canvas);
        drawInteractionHints(canvas);
        drawPieces(canvas);

        if (selected >= 0 || (game != null && game.isAwaitingRemoval())) {
            postInvalidateDelayed(40);
        }
    }

    private void drawPanel(Canvas canvas) {
        RectF shadow = new RectF(left - dp(17), top - dp(15), left + size + dp(17), top + size + dp(19));
        Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint.setColor(0x77000000);
        shadowPaint.setShadowLayer(dp(12), 0, dp(6), 0x99000000);
        canvas.drawRoundRect(shadow, dp(21), dp(21), shadowPaint);

        RectF panel = new RectF(left - dp(14), top - dp(14), left + size + dp(14), top + size + dp(14));
        panelPaint.setShader(new LinearGradient(panel.left, panel.top, panel.right, panel.bottom,
                new int[]{Color.rgb(105, 62, 30), Color.rgb(82, 45, 23), Color.rgb(62, 34, 20)},
                null, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(panel, dp(20), dp(20), panelPaint);
        panelPaint.setShader(null);
        canvas.drawRoundRect(panel, dp(20), dp(20), panelBorderPaint);

        float step = Math.max(dp(16), size / 18f);
        for (float y = panel.top + step; y < panel.bottom; y += step) {
            float offset = ((int) (y / step) % 2 == 0) ? dp(7) : 0;
            canvas.drawLine(panel.left + dp(8) + offset, y, panel.right - dp(8), y + dp(1.5f), woodGrainPaint);
        }
    }

    private void drawBoard(Canvas canvas) {
        for (int[] link : LINKS) {
            float x1 = x(link[0]); float y1 = y(link[0]);
            float x2 = x(link[1]); float y2 = y(link[1]);
            canvas.drawLine(x1, y1 + dp(1.2f), x2, y2 + dp(1.2f), boardShadowPaint);
            canvas.drawLine(x1, y1, x2, y2, boardPaint);
        }
        for (int i = 0; i < P.length; i++) {
            Paint darkPoint = new Paint(Paint.ANTI_ALIAS_FLAG);
            darkPoint.setColor(Color.rgb(42, 27, 19));
            canvas.drawCircle(x(i), y(i), dp(5.5f), darkPoint);
            canvas.drawCircle(x(i), y(i), dp(3.1f), pointPaint);
        }
    }

    private void drawInteractionHints(Canvas canvas) {
        if (game == null || game.getPhase() == TrilhaGame.Phase.GAME_OVER) return;
        float pulse = pulse();

        if (game.isAwaitingRemoval() && game.getCurrentPlayer() == TrilhaGame.HUMAN) {
            List<Integer> removable = game.getRemovablePositions(TrilhaGame.AI);
            for (int pos : removable) {
                capturePaint.setAlpha((int) (150 + 95 * pulse));
                canvas.drawCircle(x(pos), y(pos), pieceRadius + dp(7) + dp(2) * pulse, capturePaint);
            }
            return;
        }

        if (!inputEnabled || game.getCurrentPlayer() != TrilhaGame.HUMAN) return;

        if (game.getPhase() == TrilhaGame.Phase.PLACEMENT) {
            hintPaint.setAlpha(110);
            for (int i = 0; i < 24; i++) {
                if (game.getAt(i) == TrilhaGame.EMPTY) canvas.drawCircle(x(i), y(i), dp(7), hintPaint);
            }
            return;
        }

        if (selected >= 0) {
            for (int dest : validDestinations(selected)) {
                hintPaint.setAlpha((int) (145 + 100 * pulse));
                float r = dp(7.5f) + dp(2.5f) * pulse;
                canvas.drawCircle(x(dest), y(dest), r, hintPaint);
                Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
                fill.setColor(0x5543E5B1);
                canvas.drawCircle(x(dest), y(dest), dp(4.5f) + dp(1) * pulse, fill);
            }
        }
    }

    private void drawPieces(Canvas canvas) {
        if (game == null) return;
        float pulse = pulse();
        int current = game.getCurrentPlayer();

        for (int i = 0; i < 24; i++) {
            int value = game.getAt(i);
            if (value == TrilhaGame.EMPTY) continue;

            float cx = x(i);
            float cy = y(i);
            boolean human = value == TrilhaGame.HUMAN;
            int center = human ? Color.rgb(111, 239, 191) : Color.rgb(255, 238, 217);
            int mid = human ? Color.rgb(43, 166, 116) : Color.rgb(202, 197, 185);
            int edge = human ? Color.rgb(20, 78, 57) : Color.rgb(106, 102, 94);

            Paint outer = new Paint(Paint.ANTI_ALIAS_FLAG);
            outer.setColor(0x55000000);
            outer.setShadowLayer(dp(6), 0, dp(3), 0x99000000);
            canvas.drawCircle(cx, cy + dp(1), pieceRadius + dp(2), outer);

            piecePaint.setShader(new RadialGradient(cx - pieceRadius * .32f, cy - pieceRadius * .35f,
                    pieceRadius * 1.6f, new int[]{center, mid, edge}, new float[]{0f, .52f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawCircle(cx, cy, pieceRadius, piecePaint);
            piecePaint.setShader(null);

            pieceBorderPaint.setColor(human ? Color.rgb(137, 238, 199) : Color.rgb(247, 236, 215));
            pieceBorderPaint.setAlpha(210);
            canvas.drawCircle(cx, cy, pieceRadius - dp(.8f), pieceBorderPaint);

            Paint shine = new Paint(Paint.ANTI_ALIAS_FLAG);
            shine.setColor(0x66FFFFFF);
            canvas.drawCircle(cx - pieceRadius * .30f, cy - pieceRadius * .32f, pieceRadius * .23f, shine);

            if (i == selected) {
                ringPaint.setAlpha((int) (175 + 80 * pulse));
                canvas.drawCircle(cx, cy, pieceRadius + dp(6) + dp(1.8f) * pulse, ringPaint);
            }

            if (game.isAwaitingRemoval() && value == current && game.isPartOfMill(i, current)) {
                millPaint.setAlpha((int) (145 + 100 * pulse));
                canvas.drawCircle(cx, cy, pieceRadius + dp(4) + dp(1.5f) * pulse, millPaint);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_UP) return true;
        if (!inputEnabled || game == null || game.getPhase() == TrilhaGame.Phase.GAME_OVER) return true;
        if (game.getCurrentPlayer() != TrilhaGame.HUMAN) return true;

        int position = nearestPosition(event.getX(), event.getY());
        if (position < 0) return true;

        boolean wasAwaitingRemoval = game.isAwaitingRemoval();
        boolean changed = handleHumanTap(position);
        if (changed) {
            if (hapticsEnabled) {
                int feedback = (wasAwaitingRemoval || game.isAwaitingRemoval())
                        ? HapticFeedbackConstants.LONG_PRESS : HapticFeedbackConstants.KEYBOARD_TAP;
                performHapticFeedback(feedback);
            }
            invalidate();
            if (listener != null) listener.onGameChanged();
        }
        return true;
    }

    private boolean handleHumanTap(int position) {
        if (game.isAwaitingRemoval()) {
            selected = -1;
            return game.remove(position);
        }

        if (game.getPhase() == TrilhaGame.Phase.PLACEMENT) {
            selected = -1;
            return game.place(position);
        }

        if (game.getAt(position) == TrilhaGame.HUMAN) {
            selected = position;
            if (hapticsEnabled) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            invalidate();
            return false;
        }

        if (selected >= 0 && game.getAt(position) == TrilhaGame.EMPTY) {
            boolean moved = game.move(selected, position);
            if (moved) selected = -1;
            return moved;
        }
        return false;
    }

    private List<Integer> validDestinations(int from) {
        List<Integer> result = new ArrayList<>();
        if (game == null) return result;
        for (TrilhaGame.Action action : game.getLegalActions(TrilhaGame.HUMAN)) {
            if (!action.isPlacement() && action.from == from) result.add(action.to);
        }
        return result;
    }

    private int nearestPosition(float touchX, float touchY) {
        int best = -1;
        float bestDistance = Float.MAX_VALUE;
        float threshold = Math.max(pieceRadius * 1.8f, dp(29));
        for (int i = 0; i < 24; i++) {
            float dx = touchX - x(i);
            float dy = touchY - y(i);
            float distance = (float) Math.sqrt(dx * dx + dy * dy);
            if (distance < bestDistance && distance <= threshold) {
                bestDistance = distance;
                best = i;
            }
        }
        return best;
    }

    private float pulse() {
        return (float) ((Math.sin(SystemClock.uptimeMillis() / 180.0) + 1.0) * 0.5);
    }

    private float x(int position) { return left + P[position][0] * size; }
    private float y(int position) { return top + P[position][1] * size; }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private int dpInt(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
