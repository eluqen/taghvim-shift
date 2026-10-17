package com.eluqen.shiftcalendar.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.eluqen.shiftcalendar.core.HijriDate;
import com.eluqen.shiftcalendar.core.PersianDate;
import com.eluqen.shiftcalendar.core.PersianDigits;
import com.eluqen.shiftcalendar.data.Prefs;
import com.eluqen.shiftcalendar.events.EventRepository;
import com.eluqen.shiftcalendar.hijri.HijriResolver;
import com.eluqen.shiftcalendar.widget.ShiftWidgetProvider;

/**
 * Deliberately small one-screen Hijri correction sheet.
 * -2..+2 are UI-only candidates; changes persist ONLY on Save.
 */
final class HijriCorrectionDialog {
    private static final String TAG="ELUQEN_HIJRI";
    private HijriCorrectionDialog(){}

    static void show(Activity a,Runnable afterSave){
        if(a.isFinishing()||a.isDestroyed())return;
        final int current=Prefs.hijriManualOffset(a);
        final int[] value={current};
        final PersianDate today=PersianDate.todayTehran();
        final EventRepository bank=EventRepository.get(a);

        AlertDialog dialog=new AlertDialog.Builder(a).create();
        LinearLayout root=new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(android.view.View.LAYOUT_DIRECTION_RTL);
        root.setPadding(d(a,17),d(a,16),d(a,17),d(a,15));
        root.setBackground(Ui.roundedStroke(Ui.CARD,Ui.BORDER,19,a));

        root.addView(label(a,"اصلاح تاریخ قمری",20,Ui.TEXT,true));
        root.addView(label(a,
            "اختلاف با تاریخ قمری محلی را تعیین کنید.",12.5f,Ui.MUTED,false),
            Ui.margins(a,-1,-2,0,4,0,12));

        LinearLayout preview=new LinearLayout(a);
        preview.setOrientation(LinearLayout.VERTICAL);
        preview.setPadding(d(a,12),d(a,11),d(a,12),d(a,12));
        preview.setBackground(Ui.roundedStroke(Ui.SURFACE_ALT,Ui.BORDER,13,a));
        preview.addView(label(a,"نتیجه برای امروز",12,Ui.MUTED,true));
        TextView date=label(a,"",18,Ui.PRIMARY,true);
        date.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        preview.addView(date,Ui.margins(a,-1,-2,0,4,0,2));
        TextView basis=label(a,"",11.5f,Ui.MUTED,false);
        preview.addView(basis);
        root.addView(preview);

        root.addView(label(a,"اصلاح روز",13,Ui.TEXT,true),
                Ui.margins(a,-1,-2,0,14,0,7));
        LinearLayout options=new LinearLayout(a);
        options.setLayoutDirection(android.view.View.LAYOUT_DIRECTION_RTL);
        TextView[] cells=new TextView[5];
        String[] captions={"−۲","−۱","صفر","+۱","+۲"};
        for(int i=0;i<5;i++){
            final int offset=i-2;
            TextView chip=label(a,captions[i],15.5f,Ui.TEXT,true);
            chip.setGravity(Gravity.CENTER);
            chip.setClickable(true);
            chip.setFocusable(true);
            chip.setContentDescription(PersianDigits.of(
                HijriAdjustmentOptions.optionLabel(offset)));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,d(a,47),1);
            if(i>0)lp.setMargins(d(a,4),0,0,0);
            options.addView(chip,lp);
            cells[i]=chip;
        }
        root.addView(options);

        TextView currentNote=label(a,PersianDigits.of(
            "تنظیم ذخیره‌شده: "+HijriAdjustmentOptions.savedDescription(current)),
            12,Ui.MUTED,false);
        root.addView(currentNote,Ui.margins(a,-1,-2,0,9,0,13));

        LinearLayout footer=new LinearLayout(a);
        footer.setLayoutDirection(android.view.View.LAYOUT_DIRECTION_RTL);
        TextView save=button(a,"ذخیره",true);
        TextView cancel=button(a,"انصراف",false);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,d(a,51),1);
        lp.setMargins(0,0,d(a,5),0);
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,d(a,51),1);
        cp.setMargins(d(a,5),0,0,0);
        footer.addView(save,lp);
        footer.addView(cancel,cp);
        root.addView(footer);

        Runnable refresh=()->{
            for(int i=0;i<5;i++){
                boolean active=(i-2)==value[0];
                cells[i].setTextColor(active?Color.WHITE:Ui.TEXT);
                cells[i].setBackground(Ui.roundedStroke(
                    active?Ui.PRIMARY:Ui.SURFACE_ALT,
                    active?Ui.PRIMARY:Ui.BORDER,11,a));
                cells[i].setSelected(active);
            }
            try{
                PersianDate shifted=today.plusDays(value[0]);
                HijriDate calculated=HijriResolver.resolve(today,value[0],
                    bank::resolveServerHijri);
                date.setText(PersianDigits.of(calculated.displayLong()));
                basis.setText(bank.hasAuthoritativeHijri(shifted)
                        ?"مبنای رسمی ثبت‌شده":"مبنای محاسبات تقویمی");
            }catch(RuntimeException e){
                android.util.Log.e(TAG,"Preview failed",e);
                date.setText("پیش‌نمایش در دسترس نیست");
                basis.setText("تنظیم قبلی تغییر نکرده است.");
            }
        };
        for(int i=0;i<5;i++){
            final int offset=i-2;
            cells[i].setOnClickListener(v->{value[0]=offset;refresh.run();});
        }
        refresh.run();

        cancel.setOnClickListener(v->dialog.dismiss());
        save.setOnClickListener(v->{
            try{Prefs.setHijriManualOffset(a,value[0]);}
            catch(RuntimeException e){
                android.util.Log.e(TAG,"Save failed",e);
                Ui.toast(a,"ذخیره تاریخ قمری انجام نشد");return;
            }
            try{
                if(afterSave!=null)afterSave.run();
                else ShiftWidgetProvider.updateAll(a);
            }catch(RuntimeException e){
                android.util.Log.w(TAG,"Post-save refresh failed",e);
            }
            dialog.dismiss();
            Ui.toast(a,"اصلاح تاریخ قمری ذخیره شد");
        });
        dialog.setView(root);
        dialog.show();
        Window window=dialog.getWindow();
        if(window!=null){
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(Math.min(a.getResources().getDisplayMetrics().widthPixels-d(a,24),
                d(a,385)),WindowManager.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.CENTER);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        }
    }

    private static TextView button(Activity a,String title,boolean primary){
        TextView t=label(a,title,15,primary?Color.WHITE:Ui.TEXT,true);
        t.setGravity(Gravity.CENTER);
        t.setBackground(Ui.roundedStroke(primary?Ui.PRIMARY:Ui.SURFACE_ALT,
                primary?Ui.PRIMARY:Ui.BORDER,12,a));
        t.setClickable(true);t.setFocusable(true);
        return t;
    }
    private static TextView label(Activity a,String value,float size,int color,boolean bold){
        TextView t=Ui.text(a,value,size,color);
        t.setTextDirection(android.view.View.TEXT_DIRECTION_RTL);
        t.setLayoutDirection(android.view.View.LAYOUT_DIRECTION_RTL);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return t;
    }
    private static int d(Activity a,int value){return Ui.dp(a,value);}
}
