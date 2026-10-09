package com.eluqen.shiftcalendar.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/** Clear miniature illustrations for the monthly Work table. */
public final class WorkStatusGlyph extends View {
    public static final int NOTE = 1, SWAP = 2, LEAVE = 3, REST = 4, OVERTIME = 5;
    public static final int SHIFT_DAY = 6, SHIFT_EVENING = 7, SHIFT_NIGHT = 8;

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int type;
    private final boolean active;
    private final int muted;

    public WorkStatusGlyph(Context context, int type, int muted, boolean active) {
        super(context);
        this.type = type;
        this.active = active;
        this.muted = muted;
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
    }

    public WorkStatusGlyph(Context context, int type, int color) {
        this(context, type, color, false);
    }

    private int c(int activeColor) { return active ? activeColor : muted; }
    private int fill(int activeColor) { return active ? activeColor : 0x11909aa3; }

    private void stroke(float width, int color) {
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(width);
        p.setColor(color);
    }

    private void solid(int color) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(color);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float s = Math.min(getWidth(), getHeight());
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        if (type == NOTE) note(canvas, cx, cy, s);
        else if (type == SWAP) swap(canvas, cx, cy, s);
        else if (type == LEAVE) leave(canvas, cx, cy, s);
        else if (type == REST) rest(canvas, cx, cy, s);
        else if (type == OVERTIME) overtime(canvas, cx, cy, s);
        else if (type == SHIFT_DAY) shiftDay(canvas, cx, cy, s);
        else if (type == SHIFT_EVENING) shiftEvening(canvas, cx, cy, s);
        else shiftNight(canvas, cx, cy, s);
    }

    private void note(Canvas canvas, float cx, float cy, float s) {
        RectF paper = new RectF(cx-s*.29f, cy-s*.31f, cx+s*.18f, cy+s*.27f);
        solid(fill(0xffffe79b)); canvas.drawRoundRect(paper, s*.07f, s*.07f, p);
        stroke(s*.05f, c(0xff5f5b72)); canvas.drawRoundRect(paper, s*.07f, s*.07f, p);
        canvas.drawLine(cx-s*.19f, cy-s*.13f, cx+s*.07f, cy-s*.13f, p);
        canvas.drawLine(cx-s*.19f, cy-s*.01f, cx+s*.03f, cy-s*.01f, p);
        canvas.drawLine(cx-s*.19f, cy+s*.11f, cx-s*.01f, cy+s*.11f, p);
        stroke(s*.08f, c(0xffe16067));
        canvas.drawLine(cx+s*.01f, cy+s*.25f, cx+s*.29f, cy-s*.18f, p);
        Path tip = new Path();
        tip.moveTo(cx+s*.29f, cy-s*.18f);
        tip.lineTo(cx+s*.20f, cy-s*.14f);
        tip.lineTo(cx+s*.26f, cy-s*.07f);
        tip.close();
        solid(c(0xffe16067)); canvas.drawPath(tip, p);
    }

    /** Full stick-person silhouette: head, torso, two arms and two legs. */
    private void human(Canvas canvas, float x, float y, float s, int color) {
        int col = c(color);
        solid(col);
        canvas.drawCircle(x, y-s*.22f, s*.085f, p);
        stroke(s*.065f, col);
        canvas.drawLine(x, y-s*.11f, x, y+s*.11f, p);              // torso
        canvas.drawLine(x, y-s*.04f, x-s*.12f, y+s*.04f, p);       // left arm
        canvas.drawLine(x, y-s*.04f, x+s*.12f, y+s*.04f, p);       // right arm
        canvas.drawLine(x, y+s*.11f, x-s*.10f, y+s*.27f, p);       // left leg
        canvas.drawLine(x, y+s*.11f, x+s*.10f, y+s*.27f, p);       // right leg
    }

    private void arrow(Canvas canvas, float x1, float y, float x2, float s, int color) {
        stroke(s*.045f, c(color));
        canvas.drawLine(x1, y, x2, y, p);
        float dir = x2 > x1 ? 1f : -1f;
        canvas.drawLine(x2, y, x2-dir*s*.09f, y-s*.065f, p);
        canvas.drawLine(x2, y, x2-dir*s*.09f, y+s*.065f, p);
    }

    private void swap(Canvas canvas, float cx, float cy, float s) {
        // Two colleagues in a flowing, bidirectional handover.
        human(canvas, cx-s*.23f, cy+s*.025f, s*.82f, 0xff3187c8);
        human(canvas, cx+s*.23f, cy+s*.025f, s*.82f, 0xffe58a2d);
        stroke(s*.052f, c(0xff7156c8));
        Path upper = new Path();
        upper.moveTo(cx-s*.31f, cy-s*.23f);
        upper.quadTo(cx, cy-s*.43f, cx+s*.30f, cy-s*.23f);
        canvas.drawPath(upper, p);
        canvas.drawLine(cx+s*.30f, cy-s*.23f, cx+s*.19f, cy-s*.24f, p);
        canvas.drawLine(cx+s*.30f, cy-s*.23f, cx+s*.25f, cy-s*.33f, p);
        stroke(s*.052f, c(0xff258f78));
        Path lower = new Path();
        lower.moveTo(cx+s*.31f, cy+s*.29f);
        lower.quadTo(cx, cy+s*.47f, cx-s*.30f, cy+s*.29f);
        canvas.drawPath(lower, p);
        canvas.drawLine(cx-s*.30f, cy+s*.29f, cx-s*.19f, cy+s*.28f, p);
        canvas.drawLine(cx-s*.30f, cy+s*.29f, cx-s*.25f, cy+s*.39f, p);
    }

    private void leave(Canvas canvas, float cx, float cy, float s) {
        // A reclining vacationer shaded by a beach umbrella.
        Path shade = new Path();
        shade.moveTo(cx-s*.38f, cy-s*.08f);
        shade.quadTo(cx-s*.16f, cy-s*.42f, cx+s*.30f, cy-s*.13f);
        shade.quadTo(cx+s*.07f, cy-s*.16f, cx-s*.03f, cy-s*.10f);
        shade.quadTo(cx-s*.21f, cy-s*.17f, cx-s*.38f, cy-s*.08f);
        shade.close();
        solid(fill(0xffff8a65)); canvas.drawPath(shade, p);
        stroke(s*.043f, c(0xffb94f34)); canvas.drawPath(shade, p);
        stroke(s*.044f, c(0xff76634e));
        canvas.drawLine(cx-s*.07f, cy-s*.18f, cx+s*.13f, cy+s*.29f, p);
        solid(c(0xffffc44e)); canvas.drawCircle(cx+s*.32f, cy-s*.29f, s*.067f, p);
        // Head, relaxed torso and stretched-out legs on the sand.
        solid(c(0xff3386ae)); canvas.drawCircle(cx-s*.20f, cy+s*.035f, s*.072f, p);
        stroke(s*.071f, c(0xff3386ae));
        canvas.drawLine(cx-s*.15f, cy+s*.09f, cx+s*.025f, cy+s*.20f, p);
        canvas.drawLine(cx+s*.025f, cy+s*.20f, cx+s*.23f, cy+s*.20f, p);
        canvas.drawLine(cx+s*.025f, cy+s*.20f, cx+s*.19f, cy+s*.29f, p);
        canvas.drawLine(cx-s*.11f, cy+s*.12f, cx-s*.24f, cy+s*.24f, p);
        stroke(s*.049f, c(0xffbf914f));
        canvas.drawLine(cx-s*.36f, cy+s*.32f, cx+s*.36f, cy+s*.32f, p);
    }

    private void rest(Canvas canvas, float cx, float cy, float s) {
        // Reclining person on a distinct armchair / chaise silhouette.
        stroke(s*.083f, c(0xff6688b4));
        canvas.drawLine(cx-s*.30f, cy-s*.21f, cx-s*.18f, cy+s*.15f, p);
        canvas.drawLine(cx-s*.18f, cy+s*.15f, cx+s*.25f, cy+s*.15f, p);
        canvas.drawLine(cx-s*.17f, cy+s*.15f, cx-s*.20f, cy+s*.34f, p);
        canvas.drawLine(cx+s*.23f, cy+s*.15f, cx+s*.27f, cy+s*.34f, p);
        solid(c(0xff7868d8)); canvas.drawCircle(cx-s*.23f, cy-s*.23f, s*.082f, p);
        stroke(s*.074f, c(0xff7868d8));
        canvas.drawLine(cx-s*.20f, cy-s*.12f, cx-s*.04f, cy+s*.075f, p);
        canvas.drawLine(cx-s*.04f, cy+s*.075f, cx+s*.17f, cy+s*.075f, p);
        canvas.drawLine(cx+s*.17f, cy+s*.075f, cx+s*.32f, cy+s*.18f, p);
        canvas.drawLine(cx-s*.13f, cy-s*.03f, cx+s*.035f, cy-s*.01f, p);
    }


    private void rays(Canvas canvas, float cx, float cy, float r, float s, int color) {
        stroke(s*.035f, c(color));
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2.0 * i / 8.0;
            float x1 = cx + (float)Math.cos(a) * r * 1.25f;
            float y1 = cy + (float)Math.sin(a) * r * 1.25f;
            float x2 = cx + (float)Math.cos(a) * r * 1.60f;
            float y2 = cy + (float)Math.sin(a) * r * 1.60f;
            canvas.drawLine(x1, y1, x2, y2, p);
        }
    }

    private void shiftDay(Canvas canvas, float cx, float cy, float s) {
        float r = s*.19f;
        solid(c(0xffffc93d));
        canvas.drawCircle(cx, cy-s*.05f, r, p);
        rays(canvas, cx, cy-s*.05f, r, s, 0xffffa000);
        stroke(s*.055f, c(0xff7e6320));
        canvas.drawLine(cx-s*.28f, cy+s*.29f, cx+s*.28f, cy+s*.29f, p);
    }

    private void shiftEvening(Canvas canvas, float cx, float cy, float s) {
        float horizon = cy+s*.13f;
        solid(c(0xffff9d4a));
        canvas.drawCircle(cx, horizon, s*.18f, p);
        solid(active ? 0xfffff3dc : fill(0xfffff3dc));
        canvas.drawRect(cx-s*.30f, horizon, cx+s*.30f, cy+s*.34f, p);
        stroke(s*.05f, c(0xffb6652f));
        canvas.drawLine(cx-s*.31f, horizon, cx+s*.31f, horizon, p);
        canvas.drawLine(cx-s*.26f, cy+s*.27f, cx-s*.07f, cy+s*.20f, p);
        canvas.drawLine(cx+s*.07f, cy+s*.20f, cx+s*.26f, cy+s*.27f, p);
    }

    private void shiftNight(Canvas canvas, float cx, float cy, float s) {
        Path moon = new Path();
        moon.moveTo(cx+s*.10f, cy-s*.31f);
        moon.cubicTo(cx-s*.22f, cy-s*.23f, cx-s*.26f, cy+s*.12f, cx+s*.01f, cy+s*.24f);
        moon.cubicTo(cx+s*.25f, cy+s*.34f, cx+s*.36f, cy+s*.06f, cx+s*.24f, cy-s*.02f);
        moon.cubicTo(cx+s*.04f, cy+s*.05f, cx-s*.05f, cy-s*.13f, cx+s*.10f, cy-s*.31f);
        solid(c(0xff6656c9));
        canvas.drawPath(moon, p);
        solid(c(0xffffd65d));
        canvas.drawCircle(cx+s*.27f, cy-s*.24f, s*.035f, p);
        canvas.drawCircle(cx+s*.34f, cy-s*.12f, s*.022f, p);
    }

    private void clock(Canvas canvas, float x, float y, float r, float s) {
        solid(fill(0xfffff4c5)); canvas.drawCircle(x, y, r, p);
        stroke(s*.045f, c(0xff4f6774)); canvas.drawCircle(x, y, r, p);
        canvas.drawLine(x, y, x, y-r*.55f, p);
        canvas.drawLine(x, y, x+r*.46f, y+r*.18f, p);
        solid(c(0xff4f6774)); canvas.drawCircle(x, y, s*.025f, p);
    }

    private void overtime(Canvas canvas, float cx, float cy, float s) {
        // Worker leaning into a task; a clock makes the extra hours explicit.
        solid(c(0xff3386ae)); canvas.drawCircle(cx-s*.22f, cy-s*.20f, s*.078f, p);
        stroke(s*.077f, c(0xff3386ae));
        canvas.drawLine(cx-s*.18f, cy-s*.11f, cx-s*.07f, cy+s*.13f, p);
        canvas.drawLine(cx-s*.07f, cy+s*.13f, cx-s*.24f, cy+s*.32f, p);
        canvas.drawLine(cx-s*.07f, cy+s*.13f, cx+s*.075f, cy+s*.32f, p);
        canvas.drawLine(cx-s*.13f, cy-s*.04f, cx+s*.08f, cy+s*.035f, p);
        canvas.drawLine(cx+s*.08f, cy+s*.035f, cx+s*.17f, cy-s*.055f, p);
        // Compact workbench and tool/work piece.
        stroke(s*.055f, c(0xff927050));
        canvas.drawLine(cx+s*.03f, cy+s*.13f, cx+s*.34f, cy+s*.13f, p);
        canvas.drawLine(cx+s*.27f, cy+s*.13f, cx+s*.27f, cy+s*.34f, p);
        solid(c(0xffffb64a));
        canvas.drawRect(cx+s*.13f, cy+s*.055f, cx+s*.23f, cy+s*.125f, p);
        clock(canvas, cx+s*.23f, cy-s*.23f, s*.13f, s);
        stroke(s*.032f, c(0xffe58a2d));
        canvas.drawLine(cx-s*.37f, cy-s*.07f, cx-s*.41f, cy-s*.12f, p);
        canvas.drawLine(cx-s*.39f, cy+s*.03f, cx-s*.44f, cy+.02f, p);
    }
}
