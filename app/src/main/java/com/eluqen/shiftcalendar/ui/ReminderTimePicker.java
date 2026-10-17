package com.eluqen.shiftcalendar.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.eluqen.shiftcalendar.core.PersianDigits;
import java.util.Locale;

/** Explicit 24-hour numeric selector; no platform NumberPicker wheel or AM/PM. */
final class ReminderTimePicker {
    interface Result { void selected(int hour,int minute); }
    private ReminderTimePicker(){}

    static void show(Activity a,int startHour,int startMinute,Result callback){
        AlertDialog dialog=new AlertDialog.Builder(a).create();
        LinearLayout root=new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setPadding(d(a,18),d(a,18),d(a,18),d(a,14));
        root.setBackground(Ui.roundedStroke(Ui.CARD,Ui.BORDER,19,a));

        root.addView(Ui.title(a,"انتخاب ساعت",20));
        root.addView(Ui.text(a,"ساعت ۲۴ ساعته؛ مقدار را وارد کنید یا تغییر دهید.",
                12.5f,Ui.MUTED),Ui.margins(a,-1,-2,0,5,0,14));

        LinearLayout fields=new LinearLayout(a);
        fields.setGravity(Gravity.CENTER_VERTICAL);
        fields.setLayoutDirection(View.LAYOUT_DIRECTION_LTR); // Hours on LEFT, minutes on RIGHT.
        EditText hour=input(a,startHour);
        EditText minute=input(a,startMinute);
        fields.addView(field(a,"ساعت",hour,23),new LinearLayout.LayoutParams(0,-2,1));
        TextView divider=Ui.title(a,":",27);
        divider.setGravity(Gravity.CENTER);
        fields.addView(divider,new LinearLayout.LayoutParams(d(a,27),d(a,130)));
        fields.addView(field(a,"دقیقه",minute,59),new LinearLayout.LayoutParams(0,-2,1));
        root.addView(fields);
        TextView label=Ui.text(a,"برای مثال ۰۷:۳۰ یا ۲۱:۴۵",12.5f,Ui.MUTED);
        label.setGravity(Gravity.CENTER);
        root.addView(label,Ui.margins(a,-1,-2,0,5,0,14));

        LinearLayout actions=new LinearLayout(a);
        actions.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        TextView ok=button(a,"تأیید ساعت",true);
        TextView cancel=button(a,"انصراف",false);
        actions.addView(ok,new LinearLayout.LayoutParams(0,d(a,52),1));
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,d(a,52),1);
        cp.setMargins(d(a,7),0,0,0);
        actions.addView(cancel,cp);
        root.addView(actions);
        ok.setOnClickListener(v->{
            try{
                int h=parse(hour.getText().toString()),m=parse(minute.getText().toString());
                if(h>23||h<0){hour.setError("ساعت باید بین ۰ تا ۲۳ باشد");return;}
                if(m>59||m<0){minute.setError("دقیقه باید بین ۰ تا ۵۹ باشد");return;}
                InputMethodManager imm=(InputMethodManager)a.getSystemService(Context.INPUT_METHOD_SERVICE);
                if(imm!=null)imm.hideSoftInputFromWindow(hour.getWindowToken(),0);
                callback.selected(h,m);
                dialog.dismiss();
            }catch(NumberFormatException ex){
                hour.setError("ساعت و دقیقه را با عدد وارد کنید");
                minute.setError("ساعت و دقیقه را با عدد وارد کنید");
            }
        });
        cancel.setOnClickListener(v->dialog.dismiss());
        dialog.setView(root);
        dialog.show();
        Window window=dialog.getWindow();
        if(window!=null){
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(Math.min(a.getResources().getDisplayMetrics().widthPixels-d(a,26),
                    d(a,355)),WindowManager.LayoutParams.WRAP_CONTENT);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
                    |WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        root.setFocusableInTouchMode(true);
        root.requestFocus();
    }

    private static LinearLayout field(Activity a,String title,EditText edit,int max){
        LinearLayout col=new LinearLayout(a);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        TextView caption=Ui.text(a,title,13.5f,Ui.MUTED);
        caption.setGravity(Gravity.CENTER);
        col.addView(caption,new LinearLayout.LayoutParams(-1,d(a,29)));
        col.addView(edit,new LinearLayout.LayoutParams(-1,d(a,66)));
        LinearLayout changes=new LinearLayout(a);
        changes.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        TextView up=button(a,"＋",false),down=button(a,"−",false);
        changes.addView(up,new LinearLayout.LayoutParams(0,d(a,39),1));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,d(a,39),1);
        lp.setMargins(d(a,5),0,0,0);
        changes.addView(down,lp);
        col.addView(changes,Ui.margins(a,-1,-2,0,5,0,0));
        up.setOnClickListener(v->change(edit,1,max));
        down.setOnClickListener(v->change(edit,-1,max));
        return col;
    }
    private static EditText input(Activity a,int n){
        EditText edit=Ui.input(a,"۰۰");
        edit.setInputType(InputType.TYPE_CLASS_NUMBER);
        edit.setSingleLine(true);
        edit.setSelectAllOnFocus(true);
        edit.setGravity(Gravity.CENTER);
        edit.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        edit.setTextSize(28);
        edit.setText(PersianDigits.of(String.format(Locale.US,"%02d",n)));
        edit.setPadding(0,0,0,0);
        edit.setContentDescription("عدد دو رقمی زمان");
        return edit;
    }
    private static void change(EditText edit,int by,int max){
        int n;
        try{n=parse(edit.getText().toString());}
        catch(NumberFormatException error){n=0;}
        n=(n+by+max+1)%(max+1);
        edit.setText(PersianDigits.of(String.format(Locale.US,"%02d",n)));
        edit.setSelection(edit.length());
    }
    private static int parse(String raw){
        StringBuilder b=new StringBuilder();
        for(char x:raw.trim().toCharArray()){
            if(x>='۰'&&x<='۹')b.append((char)('0'+x-'۰'));
            else if(x>='٠'&&x<='٩')b.append((char)('0'+x-'٠'));
            else b.append(x);
        }
        return Integer.parseInt(b.toString());
    }
    private static TextView button(Activity a,String name,boolean main){
        TextView b=Ui.text(a,name,15,main?-1:Ui.TEXT);
        b.setGravity(Gravity.CENTER);
        b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        b.setBackground(Ui.roundedStroke(main?Ui.PRIMARY:Ui.SURFACE_ALT,
                main?Ui.PRIMARY:Ui.BORDER,11,a));
        b.setClickable(true);b.setFocusable(true);
        return b;
    }
    private static int d(Activity a,int v){return Ui.dp(a,v);}
}
