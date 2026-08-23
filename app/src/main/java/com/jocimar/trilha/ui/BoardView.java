package com.jocimar.trilha.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import com.jocimar.trilha.game.TrilhaGame;

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

    private final Paint boardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint humanPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint aiPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private TrilhaGame game;
    private Listener listener;
    private int selected = -1;
    private boolean inputEnabled = true;

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
        boardPaint.setColor(Color.rgb(215, 184, 112));
        boardPaint.setStrokeWidth(dp(3));
        boardPaint.setStyle(Paint.Style.STROKE);
        boardPaint.setStrokeCap(Paint.Cap.ROUND);

        pointPaint.setColor(Color.rgb(215, 184, 112));
        pointPaint.setStyle(Paint.Style.FILL);

        humanPaint.setColor(Color.rgb(59, 208, 168));
        humanPaint.setStyle(Paint.Style.FILL);
        humanPaint.setShadowLayer(dp(7), 0, dp(2), 0x66000000);

        aiPaint.setColor(Color.rgb(234, 95, 95));
        aiPaint.setStyle(Paint.Style.FILL);
        aiPaint.setShadowLayer(dp(7), 0, dp(2), 0x66000000);

        ringPaint.setColor(Color.WHITE);
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(dp(3));

        glowPaint.setColor(0x55FFFFFF);
        glowPaint.setStyle(Paint.Style.FILL);
        setBackgroundColor(Color.rgb(11, 18, 32));
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
        float availableW = getWidth() - dp(40);
        float availableH = getHeight() - dp(40);
        size = Math.min(availableW, availableH);
        left = (getWidth() - size) / 2f;
        top = (getHeight() - size) / 2f;
        pieceRadius = Math.max(dp(11), size * 0.035f);

        RectF panel = new RectF(left - dp(15), top - dp(15), left + size + dp(15), top + size + dp(15));
        Paint panelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        panelPaint.setColor(Color.rgb(17, 29, 50));
        canvas.drawRoundRect(panel, dp(18), dp(18), panelPaint);

        for (int[] link : LINKS) {
            float x1 = x(link[0]); float y1 = y(link[0]);
            float x2 = x(link[1]); float y2 = y(link[1]);
            canvas.drawLine(x1, y1, x2, y2, boardPaint);
        }

        for (int i = 0; i < P.length; i++) {
            canvas.drawCircle(x(i), y(i), dp(4), pointPaint);
        }

        if (game == null) return;
        for (int i = 0; i < 24; i++) {
            int value = game.getAt(i);
            if (value == TrilhaGame.EMPTY) continue;
            Paint piece = value == TrilhaGame.HUMAN ? humanPaint : aiPaint;
            canvas.drawCircle(x(i), y(i), pieceRadius + dp(2), glowPaint);
            canvas.drawCircle(x(i), y(i), pieceRadius, piece);
            if (i == selected) canvas.drawCircle(x(i), y(i), pieceRadius + dp(6), ringPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_UP) return true;
        if (!inputEnabled || game == null || game.getPhase() == TrilhaGame.Phase.GAME_OVER) return true;
        if (game.getCurrentPlayer() != TrilhaGame.HUMAN) return true;

        int position = nearestPosition(event.getX(), event.getY());
        if (position < 0) return true;
        boolean changed = handleHumanTap(position);
        if (changed) {
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
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

    private int nearestPosition(float touchX, float touchY) {
        int best = -1;
        float bestDistance = Float.MAX_VALUE;
        float threshold = Math.max(pieceRadius * 1.7f, dp(28));
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

    private float x(int position) { return left + P[position][0] * size; }
    private float y(int position) { return top + P[position][1] * size; }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private int dpInt(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
