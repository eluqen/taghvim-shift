package com.eluqen.shiftcalendar.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.text.InputType;
import com.eluqen.shiftcalendar.core.JalaliCalendar;
import com.eluqen.shiftcalendar.core.PersianDate;
import com.eluqen.shiftcalendar.core.PersianDigits;

/** Purpose-built Persian monthly calendar, no Gregorian tabs or spinning wheels. */
final class ReminderPersianCalendar {
    interface Result { void selected(PersianDate date); }
    private static final String[] WEEK={"ش","ی","د","س","چ","پ","ج"};
    private ReminderPersianCalendar(){}

    static void show(Activity a,PersianDate initial,Result callback){
        final PersianDate[] picked={initial};
        final PersianDate[] month={initial.firstOfMonth()};
        final AlertDialog dialog=new AlertDialog.Builder(a).create();
        LinearLayout root=new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setPadding(d(a,15),d(a,15),d(a,15),d(a,12));
        root.setBackground(Ui.roundedStroke(Ui.CARD,Ui.BORDER,19,a));

        TextView heading=Ui.title(a,"انتخاب تاریخ شمسی",19);
        root.addView(heading);
        TextView note=Ui.text(a,"روز موردنظر را از تقویم انتخاب کنید.",12,Ui.MUTED);
        root.addView(note,Ui.margins(a,-1,-2,0,4,0,10));

        LinearLayout nav=new LinearLayout(a);
        nav.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        nav.setGravity(Gravity.CENTER_VERTICAL);
        TextView prev=action(a,"❯",40);
        TextView monthTitle=Ui.title(a,"",17);
        monthTitle.setGravity(Gravity.CENTER);
        monthTitle.setContentDescription("انتخاب سال");
        monthTitle.setClickable(true);
        TextView next=action(a,"❮",40);
        nav.addView(prev,new LinearLayout.LayoutParams(d(a,42),d(a,44)));
        nav.addView(monthTitle,new LinearLayout.LayoutParams(0,d(a,44),1));
        nav.addView(next,new LinearLayout.LayoutParams(d(a,42),d(a,44)));
        root.addView(nav);

        LinearLayout grid=new LinearLayout(a);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.addView(grid);
        TextView chosen=Ui.text(a,"",13,Ui.SECONDARY);
        chosen.setGravity(Gravity.CENTER);
        root.addView(chosen,Ui.margins(a,-1,d(a,37),0,8,0,7));

        LinearLayout actions=new LinearLayout(a);
        actions.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        TextView accept=action(a,"تأیید تاریخ",52);
        accept.setTextColor(-1);
        accept.setBackground(Ui.roundedStroke(Ui.PRIMARY,Ui.PRIMARY,11,a));
        TextView cancel=action(a,"انصراف",52);
        TextView today=action(a,"امروز",52);
        LinearLayout.LayoutParams wide=new LinearLayout.LayoutParams(0,d(a,47),1.6f);
        LinearLayout.LayoutParams normal=new LinearLayout.LayoutParams(0,d(a,47),1);
        normal.setMargins(d(a,5),0,0,0);
        LinearLayout.LayoutParams small=new LinearLayout.LayoutParams(0,d(a,47),.75f);
        small.setMargins(d(a,5),0,0,0);
        actions.addView(accept,wide);actions.addView(cancel,normal);actions.addView(today,small);
        root.addView(actions);
        final Runnable[] draw={null};
        draw[0]=()->{
            PersianDate visible=month[0];
            monthTitle.setText(PersianDigits.of(visible.monthName()+"  "+visible.year));
            chosen.setText(PersianDigits.of("تاریخ انتخاب‌شده: "+picked[0].day+" "+picked[0].monthName()+" "+picked[0].year));
            grid.removeAllViews();
            LinearLayout weekdays=new LinearLayout(a);
            weekdays.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            for(String name:WEEK){
                TextView v=Ui.text(a,name,12.5f,Ui.MUTED);
                v.setGravity(Gravity.CENTER);
                weekdays.addView(v,new LinearLayout.LayoutParams(0,d(a,28),1));
            }
            grid.addView(weekdays);
            int start=visible.dayOfWeek();
            int len=JalaliCalendar.daysInMonth(visible.year,visible.month);
            int cells=((start+len+6)/7)*7;
            PersianDate now=PersianDate.todayTehran();
            for(int base=0;base<cells;base+=7){
                LinearLayout week=new LinearLayout(a);
                week.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
                for(int j=0;j<7;j++){
                    final int day=base+j-start+1;
                    TextView cell=Ui.text(a,
                        day<1||day>len?"":PersianDigits.of(day),15,Ui.TEXT);
                    cell.setGravity(Gravity.CENTER);
                    boolean active=day>0&&day<=len
                        &&picked[0].year==visible.year
                        &&picked[0].month==visible.month&&picked[0].day==day;
                    boolean isToday=day>0&&day<=len
                        &&now.year==visible.year&&now.month==visible.month&&now.day==day;
                    cell.setTypeface(Typeface.DEFAULT,active?Typeface.BOLD:Typeface.NORMAL);
                    if(active){cell.setTextColor(-1);cell.setBackground(Ui.roundedStroke(Ui.PRIMARY,Ui.PRIMARY,12,a));}
                    else if(isToday){cell.setTextColor(Ui.SECONDARY);cell.setBackground(Ui.roundedStroke(Ui.SURFACE_ALT,Ui.BORDER,12,a));}
                    else cell.setBackground(Ui.rounded(Ui.CARD,11,a));
                    if(day>0&&day<=len){
                        cell.setClickable(true);
                        cell.setFocusable(true);
                        cell.setContentDescription(PersianDigits.of(day+" "+visible.monthName()+" "+visible.year));
                        cell.setOnClickListener(v->{
                            picked[0]=new PersianDate(visible.year,visible.month,day);
                            draw[0].run();
                        });
                    }
                    week.addView(cell,new LinearLayout.LayoutParams(0,d(a,43),1));
                }
                grid.addView(week,Ui.margins(a,-1,-2,0,2,0,0));
            }
        };
        prev.setOnClickListener(v->{month[0]=month[0].plusMonths(-1).firstOfMonth();draw[0].run();});
        next.setOnClickListener(v->{month[0]=month[0].plusMonths(1).firstOfMonth();draw[0].run();});
        today.setOnClickListener(v->{picked[0]=PersianDate.todayTehran();month[0]=picked[0].firstOfMonth();draw[0].run();});
        monthTitle.setOnClickListener(v->{
            EditText yearInput=Ui.input(a,"سال شمسی");
            yearInput.setText(String.valueOf(month[0].year));
            yearInput.setInputType(InputType.TYPE_CLASS_NUMBER);
            LinearLayout wrap=new LinearLayout(a);
            wrap.setPadding(d(a,19),d(a,12),d(a,19),d(a,4));
            wrap.addView(yearInput,new LinearLayout.LayoutParams(-1,d(a,53)));
            AlertDialog prompt=new AlertDialog.Builder(a)
                .setTitle("انتخاب سال شمسی").setView(wrap)
                .setNegativeButton("انصراف",null)
                .setPositiveButton("تأیید",null).create();
            prompt.setOnShowListener(x->prompt.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(b->{
                    try{
                        int year=Integer.parseInt(yearInput.getText().toString()
                            .replace('۰','0').replace('۱','1').replace('۲','2')
                            .replace('۳','3').replace('۴','4').replace('۵','5')
                            .replace('۶','6').replace('۷','7').replace('۸','8').replace('۹','9'));
                        if(year<1200||year>1700)throw new NumberFormatException();
                        month[0]=new PersianDate(year,month[0].month,1);
                        draw[0].run();prompt.dismiss();
                    }catch(Exception ex){yearInput.setError("سال معتبر بین ۱۲۰۰ تا ۱۷۰۰ وارد کنید");}
                }));
            prompt.show();
        });
        accept.setOnClickListener(v->{callback.selected(picked[0]);dialog.dismiss();});
        cancel.setOnClickListener(v->dialog.dismiss());
        draw[0].run();
        dialog.setView(root);
        dialog.show();
        Window window=dialog.getWindow();
        if(window!=null){
            window.setBackgroundDrawableResource(android.R.color.transparent);
            int w=Math.min(a.getResources().getDisplayMetrics().widthPixels-d(a,22),d(a,390));
            window.setLayout(w,WindowManager.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.CENTER);
        }
    }
    private static TextView action(Activity a,String title,int h){
        TextView b=Ui.text(a,title,14.5f,Ui.TEXT);
        b.setGravity(Gravity.CENTER);
        b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        b.setBackground(Ui.roundedStroke(Ui.SURFACE_ALT,Ui.BORDER,10,a));
        b.setClickable(true);b.setFocusable(true);
        return b;
    }
    private static int d(Activity a,int v){return Ui.dp(a,v);}
}
