package com.eluqen.shiftcalendar.ui;

import android.app.AlertDialog;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.eluqen.shiftcalendar.core.PersianDate;
import com.eluqen.shiftcalendar.data.AppDatabase;
import com.eluqen.shiftcalendar.data.CardexRecord;

import org.json.JSONArray;

/** Dedicated typed + handwritten note editor for one work date. */
public final class WorkNoteActivity extends BaseActivity implements BackDock.LocalBackHandler {
    public static final String EXTRA_DATE = "date";

    private AppDatabase database;
    private PersianDate date;
    private EditText note;
    private InkPad ink;
    private String initialText = "";
    private String initialInk = "[]";

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        database = AppDatabase.get(this);
        try {
            date = PersianDate.parse(getIntent().getStringExtra(EXTRA_DATE));
        } catch (RuntimeException e) {
            finish();
            return;
        }
        buildUi();
        if (Build.VERSION.SDK_INT >= 33) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                    this::requestClose);
        }
    }

    private void buildUi() {
        CardexRecord stored = database.cardex(date.toString());
        initialText = stored == null || stored.note == null ? "" : stored.note;
        initialInk = stored == null || TextUtils.isEmpty(stored.noteInk) ? "[]" : stored.noteInk;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.SURFACE);
        root.addView(Ui.toolbar(this, "یادداشت روز", false));

        LinearLayout dateCard = Ui.card(this);
        TextView day = Ui.title(this,
                date.weekdayName() + "  " + date.day + " " + date.monthName() + " " + date.year, 18);
        day.setGravity(Gravity.CENTER);
        dateCard.addView(day);
        TextView hint = Ui.text(this, "می‌توانید تایپ کنید، با انگشت یا قلم بنویسید، یا هر دو را با هم ذخیره کنید.", 12.5f, Ui.MUTED);
        hint.setGravity(Gravity.CENTER);
        dateCard.addView(hint, Ui.margins(this, -1, -2, 0, 5, 0, 0));
        root.addView(dateCard, Ui.margins(this, -1, -2, 10, 9, 10, 5));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setVerticalScrollBarEnabled(true);
        scroll.setScrollbarFadingEnabled(false);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(Ui.dp(this, 10), Ui.dp(this, 5), Ui.dp(this, 10), Ui.dp(this, 20));
        scroll.addView(content);

        LinearLayout textCard = Ui.card(this);
        TextView textTitle = Ui.title(this, "متن یادداشت", 15);
        textCard.addView(textTitle);
        note = new EditText(this);
        note.setHint("اینجا بنویسید...");
        note.setText(initialText);
        note.setTextSize(16);
        note.setTextColor(Ui.TEXT);
        note.setHintTextColor(0xff8795a3);
        note.setGravity(Gravity.TOP | Gravity.RIGHT);
        note.setTextDirection(View.TEXT_DIRECTION_RTL);
        note.setPadding(Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 10));
        note.setBackground(Ui.roundedStroke(0xffffffff, 0xffd4dee6, 13, this));
        textCard.addView(note, Ui.margins(this, -1, Ui.dp(this, 170), 0, 8, 0, 0));
        content.addView(textCard, Ui.margins(this, -1, -2, 0, 0, 0, 9));

        LinearLayout drawCard = Ui.card(this);
        TextView drawTitle = Ui.title(this, "دست‌نویس", 15);
        drawCard.addView(drawTitle);

        LinearLayout tools = new LinearLayout(this);
        tools.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        tools.setGravity(Gravity.CENTER);
        Button pen = tool("✎  قلم");
        Button eraser = tool("پاک‌کن");
        Button undo = tool("↶  واگرد");
        Button clear = tool("پاک‌کردن");
        tools.addView(pen, weight());
        tools.addView(eraser, weight());
        tools.addView(undo, weight());
        tools.addView(clear, weight());
        drawCard.addView(tools, Ui.margins(this, -1, Ui.dp(this, 48), 0, 7, 0, 7));

        ink = new InkPad(this);
        ink.load(parseInk(initialInk));
        drawCard.addView(ink, Ui.margins(this, -1, Ui.dp(this, 420), 0, 0, 0, 0));
        content.addView(drawCard);

        pen.setOnClickListener(v -> ink.erase = false);
        eraser.setOnClickListener(v -> ink.erase = true);
        undo.setOnClickListener(v -> ink.undo());
        clear.setOnClickListener(v -> {
            if (!ink.hasStrokes()) return;
            new AlertDialog.Builder(this)
                    .setTitle("پاک‌کردن دست‌نویس")
                    .setMessage("تمام دست‌نویس این روز پاک شود؟")
                    .setNegativeButton("انصراف", null)
                    .setPositiveButton("پاک‌کردن", (d, w) -> ink.clear())
                    .show();
        });

        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        LinearLayout actions = new LinearLayout(this);
        actions.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        actions.setPadding(Ui.dp(this, 10), Ui.dp(this, 7), Ui.dp(this, 10), Ui.dp(this, 7));
        Button save = Ui.button(this, "✓  ذخیره یادداشت");
        Button delete = Ui.tonalButton(this, "حذف یادداشت");
        Button cancel = Ui.tonalButton(this, "لغو");
        delete.setTextColor(0xffb24545);
        delete.setEnabled(hasStoredNote(stored));
        actions.addView(save, new LinearLayout.LayoutParams(0, Ui.dp(this, 50), 1.20f));
        actions.addView(delete, new LinearLayout.LayoutParams(0, Ui.dp(this, 50), .90f));
        actions.addView(cancel, new LinearLayout.LayoutParams(0, Ui.dp(this, 50), .65f));
        save.setOnClickListener(v -> saveAndFinish());
        delete.setOnClickListener(v -> confirmDeleteNote());
        cancel.setOnClickListener(v -> requestClose());
        root.addView(actions);

        setContentView(root);
    }

    private Button tool(String label) {
        Button b = Ui.tonalButton(this, label);
        b.setTextSize(12);
        return b;
    }

    private LinearLayout.LayoutParams weight() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, Ui.dp(this, 46), 1f);
        p.setMargins(Ui.dp(this, 2), 0, Ui.dp(this, 2), 0);
        return p;
    }

    private JSONArray parseInk(String raw) {
        try { return new JSONArray(raw == null ? "[]" : raw); }
        catch (Exception ignored) { return new JSONArray(); }
    }

    private boolean dirty() {
        if (note == null || ink == null) return false;
        return !initialText.equals(note.getText().toString())
                || !initialInk.equals(ink.dump().toString());
    }

    private boolean save() {
        try {
            CardexRecord record = database.cardex(date.toString());
            if (record == null) record = new CardexRecord();
            record.date = date.toString();
            record.jdn = date.toJdn();
            record.note = note.getText().toString().trim();
            record.noteInk = ink.hasStrokes() ? ink.dump().toString() : "";
            database.saveCardex(record);
            initialText = record.note;
            initialInk = record.noteInk;
            return true;
        } catch (RuntimeException e) {
            Toast.makeText(this, "ذخیره یادداشت انجام نشد", Toast.LENGTH_LONG).show();
            return false;
        }
    }

    private void saveAndFinish() {
        if (save()) finish();
    }

    private boolean hasStoredNote(CardexRecord record) {
        return record != null
                && (!TextUtils.isEmpty(record.note)
                || !TextUtils.isEmpty(record.noteTime)
                || !TextUtils.isEmpty(record.noteInk));
    }

    private void confirmDeleteNote() {
        CardexRecord stored = database.cardex(date.toString());
        if (!hasStoredNote(stored)) {
            Toast.makeText(this, "یادداشتی برای حذف وجود ندارد", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("حذف یادداشت")
                .setMessage("متن و دست‌نویس این روز حذف شود؟ وضعیت کارکرد روز دست‌نخورده می‌ماند.")
                .setNegativeButton("انصراف", null)
                .setPositiveButton("حذف یادداشت", (d, w) -> deleteNoteAndFinish())
                .show();
    }

    private void deleteNoteAndFinish() {
        try {
            CardexRecord record = database.cardex(date.toString());
            if (record == null) {
                finish();
                return;
            }
            record.note = "";
            record.noteTime = "";
            record.noteInk = "";
            if (record.isEmpty()) database.deleteCardex(date.toString());
            else database.saveCardex(record);
            finish();
        } catch (RuntimeException e) {
            Toast.makeText(this, "حذف یادداشت انجام نشد", Toast.LENGTH_LONG).show();
        }
    }

    private void requestClose() {
        if (!dirty()) {
            finish();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("تغییرات ذخیره نشده")
                .setMessage("یادداشت این روز تغییر کرده است. قبل از خروج ذخیره شود؟")
                .setNegativeButton("ادامه ویرایش", null)
                .setNeutralButton("بدون ذخیره", (d, w) -> finish())
                .setPositiveButton("ذخیره", (d, w) -> saveAndFinish())
                .show();
    }

    @Override
    public boolean onDockBack() {
        requestClose();
        return true;
    }

    @Override
    public void onBackPressed() {
        requestClose();
    }

    private static final class InkPad extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private JSONArray paths = new JSONArray();
        private JSONArray current;
        boolean erase;
        int penColor = 0xff17665c;
        int penWidth = 3;

        InkPad(android.content.Context context) {
            super(context);
            setBackground(Ui.roundedStroke(Color.WHITE, 0xffd4dee6, 14, context));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
        }

        void load(JSONArray saved) {
            paths = saved == null ? new JSONArray() : saved;
            invalidate();
        }

        JSONArray dump() { return paths; }
        boolean hasStrokes() { return paths.length() > 0 || current != null; }

        void undo() {
            if (paths.length() == 0) return;
            JSONArray next = new JSONArray();
            for (int i = 0; i < paths.length() - 1; i++) next.put(paths.opt(i));
            paths = next;
            invalidate();
        }

        void clear() {
            paths = new JSONArray();
            current = null;
            invalidate();
        }

        private void point(JSONArray stroke, float x, float y) {
            if (stroke == null || stroke.length() >= 2500) return;
            try {
                JSONArray pt = new JSONArray();
                pt.put(Math.max(0, Math.min(1, x / Math.max(1, getWidth()))));
                pt.put(Math.max(0, Math.min(1, y / Math.max(1, getHeight()))));
                stroke.put(pt);
            } catch (Exception ignored) {
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            float x = e.getX(), y = e.getY();
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    getParent().requestDisallowInterceptTouchEvent(true);
                    current = new JSONArray();
                    point(current, x, y);
                    invalidate();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (current != null) {
                        for (int i = 0; i < e.getHistorySize(); i++)
                            point(current, e.getHistoricalX(i), e.getHistoricalY(i));
                        point(current, x, y);
                        invalidate();
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    if (current != null) {
                        point(current, x, y);
                        if (paths.length() < 500 && (!erase || paths.length() > 0)) {
                            JSONArray stroke = new JSONArray();
                            stroke.put(erase ? Color.WHITE : penColor);
                            stroke.put(erase ? 24 : penWidth);
                            stroke.put(current);
                            paths.put(stroke);
                        }
                        current = null;
                        invalidate();
                    }
                    getParent().requestDisallowInterceptTouchEvent(false);
                    performClick();
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    current = null;
                    getParent().requestDisallowInterceptTouchEvent(false);
                    invalidate();
                    return true;
                default:
                    return true;
            }
        }

        @Override public boolean performClick() { super.performClick(); return true; }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            for (int i = 0; i < paths.length(); i++) drawStroke(canvas, paths.optJSONArray(i));
            if (current != null) {
                JSONArray stroke = new JSONArray();
                stroke.put(erase ? Color.WHITE : penColor);
                stroke.put(erase ? 24 : penWidth);
                stroke.put(current);
                drawStroke(canvas, stroke);
            }
        }

        private void drawStroke(Canvas canvas, JSONArray stroke) {
            if (stroke == null) return;
            JSONArray pts = stroke.optJSONArray(2);
            if (pts == null || pts.length() == 0) return;
            paint.setColor(stroke.optInt(0, penColor));
            paint.setStrokeWidth(stroke.optInt(1, 3) * getResources().getDisplayMetrics().density);
            Path path = new Path();
            for (int i = 0; i < pts.length(); i++) {
                JSONArray pt = pts.optJSONArray(i);
                if (pt == null) continue;
                float x = (float) pt.optDouble(0) * getWidth();
                float y = (float) pt.optDouble(1) * getHeight();
                if (i == 0) {
                    path.moveTo(x, y);
                    path.lineTo(x + .1f, y + .1f);
                } else path.lineTo(x, y);
            }
            canvas.drawPath(path, paint);
        }
    }
}
