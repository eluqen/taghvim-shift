package com.eluqen.shiftcalendar.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.eluqen.shiftcalendar.core.PersianDate;
import com.eluqen.shiftcalendar.core.PersianDigits;
import org.json.JSONObject;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/** Single-purpose, RTL personal reminder/task editor. Not a nested AlertDialog. */
public final class ReminderEditorActivity extends Activity {
    static final String EXTRA_ITEM_ID="eluqen_planner_id";
    static final String EXTRA_IS_EVENT="eluqen_planner_is_event";
    private static final String LOG="ELUQEN_REMINDER_EDITOR";
    private final TimeZone zone=TimeZone.getDefault();
    private long due;
    private boolean hasDate,hasTime,isEvent;
    private String sound;
    private JSONObject original;
    private EditText title,notes;
    private CheckBox notify;
    private TextView dateValue,timeValue,soundValue;
    private LinearLayout soundRow;

    @Override protected void onCreate(Bundle saved){
        super.onCreate(saved);
        AppTheme.apply(this);
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                |WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        if(Build.VERSION.SDK_INT>=35){
            WindowInsetsController controller=getWindow().getInsetsController();
            if(controller!=null){
                int flags=WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        |WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                controller.setSystemBarsAppearance(flags,flags);
            }
        }else{
            getWindow().setStatusBarColor(Ui.SURFACE);
            getWindow().setNavigationBarColor(Ui.SURFACE);
            if(Build.VERSION.SDK_INT>=26)getWindow().getDecorView()
                .setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                        |View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
        int id=getIntent().getIntExtra(EXTRA_ITEM_ID,0);
        original=id>0?PersonalStore.get(this,id):null;
        if(id>0&&original==null){
            Toast.makeText(this,"این یادآوری دیگر وجود ندارد",Toast.LENGTH_LONG).show();
            finish();return;
        }
        isEvent=original==null?getIntent().getBooleanExtra(EXTRA_IS_EVENT,true)
                :"event".equals(original.optString("kind","todo"));
        due=original==null?0:original.optLong("when",0);
        hasDate=original!=null&&original.optBoolean("has_date",due>0);
        hasTime=original!=null&&original.optBoolean("has_time",due>0);
        sound=original==null?"default_notification":original.optString("sound","default_notification");
        build();
    }

    private void build(){
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setBackgroundColor(Ui.SURFACE);
        // The editor is a standalone Activity: status/nav insets are handled once here.
        if(Build.VERSION.SDK_INT>=35){
            root.setOnApplyWindowInsetsListener((view,insets)->{
                android.graphics.Insets bars=insets.getInsets(
                    WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
                android.graphics.Insets ime=insets.getInsets(WindowInsets.Type.ime());
                view.setPadding(bars.left,bars.top,bars.right,Math.max(bars.bottom,ime.bottom));
                return insets;
            });
        }

        LinearLayout header=new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        header.setPadding(d(14),d(12),d(14),d(12));
        header.setBackgroundColor(Ui.PRIMARY);
        TextView back=plain("بازگشت",13,-1);
        back.setGravity(Gravity.CENTER);
        back.setBackground(Ui.roundedStroke(Ui.PRIMARY,Ui.SECONDARY,10,this));
        back.setOnClickListener(v->finish());
        header.addView(back,new LinearLayout.LayoutParams(d(72),d(44)));
        LinearLayout titles=new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setGravity(Gravity.CENTER_VERTICAL);
        titles.setPadding(d(12),0,0,0);
        TextView name=plain((original==null?"افزودن ":"ویرایش ")+(isEvent?"یادآوری":"کار"),20,-1);
        name.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        titles.addView(name);
        TextView subtitle=plain("موعد شمسی  •  ساعت ۲۴ ساعته",12,0xffd7e5f4);
        titles.addView(subtitle);
        header.addView(titles,new LinearLayout.LayoutParams(0,-2,1));
        root.addView(header,new LinearLayout.LayoutParams(-1,d(70)));

        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setClipToPadding(false);
        LinearLayout body=new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        body.setPadding(d(16),d(16),d(16),d(22));
        scroll.addView(body);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

        body.addView(heading("عنوان",14));
        title=Ui.input(this,isEvent?"مثلاً: وقت دندان‌پزشکی":"عنوان کار");
        title.setSingleLine(true);
        title.setText(original==null?"":original.optString("title",""));
        body.addView(title,Ui.margins(this,-1,d(53),0,6,0,17));

        body.addView(heading("زمان‌بندی",15));
        LinearLayout dateRow=new LinearLayout(this);
        dateRow.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        LinearLayout dateCard=pickerCard("تاریخ شمسی",true);
        LinearLayout timeCard=pickerCard("ساعت",false);
        LinearLayout.LayoutParams dlp=new LinearLayout.LayoutParams(0,d(99),1.25f);
        dlp.setMargins(0,0,d(6),0);
        LinearLayout.LayoutParams tlp=new LinearLayout.LayoutParams(0,d(99),1);
        tlp.setMargins(d(6),0,0,0);
        dateRow.addView(dateCard,dlp);
        dateRow.addView(timeCard,tlp);
        body.addView(dateRow,Ui.margins(this,-1,-2,0,9,0,0));
        dateCard.setOnClickListener(v->{
            PersianDate current=PlannerDateTime.jalali(
                hasDate&&due>0?due:System.currentTimeMillis(),zone);
            ReminderPersianCalendar.show(this,current,choice->{
                due=PlannerDateTime.chooseDate(due,choice,hasTime,zone);
                hasDate=true;refresh();
            });
        });
        timeCard.setOnClickListener(v->{
            Calendar c=Calendar.getInstance(zone,Locale.US);
            c.setTimeInMillis(due>0?due:System.currentTimeMillis());
            ReminderTimePicker.show(this,c.get(Calendar.HOUR_OF_DAY),
                c.get(Calendar.MINUTE),(hour,minute)->{
                    due=PlannerDateTime.chooseTime(due,hour,minute,zone);
                    hasTime=true;refresh();
                });
        });

        if(isEvent){
            // A reminder always schedules an alert. Date/time are mandatory.
            TextView tip=plain("در زمان تعیین‌شده، اعلان یادآوری ارسال می‌شود.",12.5f,Ui.MUTED);
            body.addView(tip,Ui.margins(this,-1,-2,0,6,0,14));
        }else{
            // Undated tasks and silent to-dos are still supported.
            LinearLayout clearRow=new LinearLayout(this);
            clearRow.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            TextView clearDate=plain("پاک‌کردن تاریخ",12,Ui.MUTED);
            TextView clearTime=plain("پاک‌کردن ساعت",12,Ui.MUTED);
            clearDate.setOnClickListener(v->{hasDate=false;refresh();});
            clearTime.setOnClickListener(v->{
                hasTime=false;
                due=PlannerDateTime.withoutTime(due,zone);
                refresh();
            });
            clearRow.addView(clearDate,new LinearLayout.LayoutParams(0,d(33),1));
            clearRow.addView(clearTime,new LinearLayout.LayoutParams(0,d(33),1));
            body.addView(clearRow,Ui.margins(this,-1,-2,0,2,0,9));
            LinearLayout notification=panel();
            notify=new CheckBox(this);
            notify.setText("اعلان برای این کار");
            notify.setTextSize(14.5f);
            notify.setTextColor(Ui.TEXT);
            notify.setChecked(original!=null&&original.optBoolean("alert",false));
            notification.addView(notify,new LinearLayout.LayoutParams(-1,d(47)));
            notification.addView(plain("برای اعلان کار، تاریخ و ساعت لازم است.",12,Ui.MUTED));
            body.addView(notification);
            notify.setOnCheckedChangeListener((box,checked)->refresh());
        }

        soundRow=panel();
        soundValue=plain("",14,Ui.TEXT);
        soundValue.setGravity(Gravity.CENTER_VERTICAL);
        soundRow.addView(plain("صدای اعلان",12,Ui.MUTED));
        soundRow.addView(soundValue,Ui.margins(this,-1,-2,0,3,0,0));
        soundRow.setOnClickListener(v->SoundPicker.open(this,sound,choice->{
            sound=choice;refresh();
        }));
        body.addView(soundRow,Ui.margins(this,-1,-2,0,8,0,15));

        body.addView(heading("توضیحات",14));
        notes=Ui.input(this,"اختیاری");
        notes.setSingleLine(false);
        notes.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                |android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
                |android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        notes.setGravity(Gravity.TOP|Gravity.RIGHT);
        notes.setTextDirection(View.TEXT_DIRECTION_RTL);
        notes.setHorizontallyScrolling(false);
        notes.setMinLines(6);
        // No MaxLines or fixed short height in the EDITOR: full saved text remains editable.
        notes.setMinHeight(d(190));
        notes.setText(original==null?"":original.optString("note",""));
        body.addView(notes,Ui.margins(this,-1,-2,0,6,0,0));

        LinearLayout footer=new LinearLayout(this);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        footer.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        footer.setPadding(d(16),d(9),d(16),d(10));
        footer.setBackgroundColor(Ui.CARD);
        TextView save=action("ذخیره",true);
        TextView cancel=action("انصراف",false);
        LinearLayout.LayoutParams slp=new LinearLayout.LayoutParams(0,d(51),1.2f);
        slp.setMargins(0,0,d(6),0);
        LinearLayout.LayoutParams clp=new LinearLayout.LayoutParams(0,d(51),1);
        clp.setMargins(d(6),0,0,0);
        footer.addView(save,slp);
        footer.addView(cancel,clp);
        cancel.setOnClickListener(v->finish());
        save.setOnClickListener(v->save());
        root.addView(footer,new LinearLayout.LayoutParams(-1,d(70)));
        setContentView(root);
        if(Build.VERSION.SDK_INT>=35)root.requestApplyInsets();
        refresh();
    }

    private void refresh(){
        dateValue.setText(hasDate&&due>0
                ?PersianDigits.of(PlannerDateTime.jalali(due,zone).toString())
                :"انتخاب تاریخ");
        timeValue.setText(hasTime&&due>0
                ?PlannerDateTime.timeLabel(due,zone):"انتخاب ساعت");
        soundValue.setText(SoundPicker.title(sound));
        soundRow.setAlpha(notificationEnabled()?1f:.48f);
        soundRow.setEnabled(notificationEnabled());
    }

    private boolean notificationEnabled(){
        return isEvent||(notify!=null&&notify.isChecked());
    }

    private void save(){
        String text=title.getText().toString().trim();
        if(text.isEmpty()){title.setError("عنوان لازم است");title.requestFocus();return;}
        if(notificationEnabled()&&(!hasDate||!hasTime)){
            Ui.toast(this,"تاریخ و ساعت یادآوری را مشخص کنید");return;
        }
        if(notificationEnabled()&&due<=System.currentTimeMillis()){
            Ui.toast(this,"برای اعلان، زمان آینده را انتخاب کنید");return;
        }
        try{
            JSONObject item=original==null?new JSONObject():new JSONObject(original.toString());
            item.put("title",text);
            item.put("note",notes.getText().toString().trim());
            item.put("kind",isEvent?"event":"todo");
            item.put("when",due);
            item.put("has_date",hasDate);
            item.put("has_time",hasTime);
            item.put("alert",notificationEnabled());
            item.put("sound",sound);
            item.put("fired",false);
            int id=PersonalStore.save(this,item);
            if(PersonalStore.get(this,id)==null)throw new IllegalStateException("Row not persisted");
            setResult(RESULT_OK);
            if(notificationEnabled()&&due>System.currentTimeMillis()
                    &&Build.VERSION.SDK_INT>=31){
                AlarmManager manager=(AlarmManager)getSystemService(Context.ALARM_SERVICE);
                if(manager!=null&&!manager.canScheduleExactAlarms()){
                    new AlertDialog.Builder(this)
                        .setTitle("مجوز یادآوری دقیق")
                        .setMessage("موعد ذخیره شد، اما بدون مجوز زمان‌بندی دقیق ممکن است اعلان با تأخیر برسد.")
                        .setNegativeButton("بعداً",(dialog,which)->finish())
                        .setPositiveButton("تنظیمات",(dialog,which)->{
                            try{
                                startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                    .setData(android.net.Uri.parse("package:"+getPackageName())));
                            }catch(Exception ignored){
                                Ui.toast(this,"مجوز را از تنظیمات برنامه فعال کنید");
                            }
                            finish();
                        })
                        .setOnCancelListener(dialog->finish())
                        .show();
                    return;
                }
            }
            finish();
        }catch(Exception error){
            android.util.Log.e(LOG,"Saving personal reminder failed",error);
            Ui.toast(this,"ذخیره یادآوری انجام نشد");
        }
    }
    private LinearLayout pickerCard(String label,boolean isDate){
        LinearLayout tile=new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER_VERTICAL);
        tile.setPadding(d(13),d(10),d(12),d(10));
        tile.setBackground(Ui.roundedStroke(Ui.CARD,Ui.BORDER,13,this));
        tile.setClickable(true);
        tile.setFocusable(true);
        tile.addView(plain(label,12,Ui.MUTED));
        TextView selected=plain("",isDate?15:19,Ui.PRIMARY);
        selected.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        tile.addView(selected,Ui.margins(this,-1,-2,0,8,0,0));
        if(isDate)dateValue=selected;
        else timeValue=selected;
        return tile;
    }
    private LinearLayout panel(){
        LinearLayout panel=new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        panel.setPadding(d(13),d(8),d(13),d(9));
        panel.setBackground(Ui.roundedStroke(Ui.CARD,Ui.BORDER,13,this));
        return panel;
    }
    private TextView action(String title,boolean primary){
        TextView b=plain(title,15,primary?-1:Ui.TEXT);
        b.setGravity(Gravity.CENTER);
        b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        b.setBackground(Ui.roundedStroke(primary?Ui.PRIMARY:Ui.SURFACE_ALT,
                primary?Ui.PRIMARY:Ui.BORDER,12,this));
        b.setClickable(true);b.setFocusable(true);
        return b;
    }
    private TextView heading(String title,float size){
        TextView h=plain(title,size,Ui.TEXT);
        h.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return h;
    }
    private TextView plain(String text,float size,int color){
        TextView t=Ui.text(this,text,size,color);
        t.setTextDirection(View.TEXT_DIRECTION_RTL);
        t.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        return t;
    }
    private int d(int amount){return Ui.dp(this,amount);}
    @Override public void onBackPressed(){finish();}
    @Override protected void onActivityResult(int req,int res,Intent data){
        if(!SoundPicker.result(this,req,res,data))super.onActivityResult(req,res,data);
    }
    @Override protected void onDestroy(){
        SoundPicker.stopPreview(this);
        super.onDestroy();
    }
}
