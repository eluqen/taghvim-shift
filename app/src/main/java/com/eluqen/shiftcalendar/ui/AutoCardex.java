package com.eluqen.shiftcalendar.ui;
import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.lang.reflect.*;
import java.util.*;
public final class AutoCardex {
 private static final String TODAY="eluqen_cardex_today";
 private static final String LEGACY="eluqen_cardex_holiday_work";
 private static final String SUMMARY="eluqen_cardex_summary";
 private static final WeakHashMap<Activity,Boolean> YEAR_MODE=new WeakHashMap<>();
 private AutoCardex(){}
 public static void resetMode(Activity a){YEAR_MODE.remove(a);}
 private static int dp(Activity a,int n){return Math.round(a.getResources().getDisplayMetrics().density*n);}
 private static final class Period {
  int overtime,leave,rest,swap,note,recordedDays;
  int leaveDaily,leaveHourly,restDaily,restHourly;
  int overtimeMinutes,leaveMinutes,restMinutes;
 }
 private static boolean text(Object row,String field){
  try{Object v=row.getClass().getField(field).get(row);return v!=null&&!String.valueOf(v).trim().isEmpty();}catch(Exception e){return false;}
 }
 private static boolean flag(Object row,String field){
  try{return row.getClass().getField(field).getBoolean(row);}catch(Exception e){return false;}
 }
 private static Period period(Activity a,int year,int fromMonth,int toMonth){
  Period out=new Period();
  try{
   Object first=OfficialHoliday.date(year,fromMonth,1),last=OfficialHoliday.date(year,toMonth,1);
   last=last.getClass().getMethod("plusMonths",int.class).invoke(last,1);
   last=last.getClass().getMethod("plusDays",long.class).invoke(last,-1L);
   long start=(Long)first.getClass().getMethod("toJdn").invoke(first),end=(Long)last.getClass().getMethod("toJdn").invoke(last);
   Object db=OfficialHoliday.db(a);
   List<?> rows=(List<?>)db.getClass().getMethod("cardexBetween",long.class,long.class).invoke(db,start,end);
   out.recordedDays=rows.size();
   for(Object row:rows){
    int mode=com.eluqen.shiftcalendar.work.DayWorkResolver.mode((com.eluqen.shiftcalendar.data.CardexRecord)row);
    if(mode==4)out.overtime++;
    if(mode==2){
     out.leave++;
     if(flag(row,"leaveFullShift")||!text(row,"leaveStart")||!text(row,"leaveEnd"))out.leaveDaily++;else out.leaveHourly++;
    }
    if(mode==1){
     out.rest++;
     if(flag(row,"restFullShift")||!text(row,"restStart")||!text(row,"restEnd"))out.restDaily++;else out.restHourly++;
    }
    if(mode==3)out.swap++;
    if(text(row,"note")||text(row,"noteTime")||text(row,"noteInk"))out.note++;
   }
   boolean excludeLunch=(Boolean)Class.forName("com.eluqen.shiftcalendar.data.Prefs")
    .getMethod("excludeLunchHour",android.content.Context.class).invoke(null,a);
   Class<?> stats=Class.forName("com.eluqen.shiftcalendar.data.CardexStats");
   Object calculated=stats.getMethod("calculate",android.content.Context.class,List.class,boolean.class).invoke(null,a,rows,excludeLunch);
   out.overtimeMinutes=stats.getField("overtimeMinutes").getInt(calculated);
   out.leaveMinutes=stats.getField("leaveMinutes").getInt(calculated);
   out.restMinutes=stats.getField("restMinutes").getInt(calculated);
  }catch(Exception e){android.util.Log.e("ELUQEN_CARDEX","Period stats",e);}
  return out;
 }
 private static Period periodToDate(Activity a,int year,int fromMonth,int toMonth){
  Period out=new Period();
  try{
   Object first=OfficialHoliday.date(year,fromMonth,1),last=OfficialHoliday.date(year,toMonth,1);
   last=last.getClass().getMethod("plusMonths",int.class).invoke(last,1);
   last=last.getClass().getMethod("plusDays",long.class).invoke(last,-1L);
   Object today=Class.forName("com.eluqen.shiftcalendar.core.PersianDate").getMethod("todayTehran").invoke(null);
   long start=(Long)first.getClass().getMethod("toJdn").invoke(first);
   long end=(Long)last.getClass().getMethod("toJdn").invoke(last);
   long now=(Long)today.getClass().getMethod("toJdn").invoke(today);
   if(start>now)return out;
   end=Math.min(end,now);
   Object db=OfficialHoliday.db(a);
   List<?> rows=(List<?>)db.getClass().getMethod("cardexBetween",long.class,long.class).invoke(db,start,end);
   out.recordedDays=rows.size();
   for(Object row:rows){
    int mode=com.eluqen.shiftcalendar.work.DayWorkResolver.mode((com.eluqen.shiftcalendar.data.CardexRecord)row);
    if(mode==4)out.overtime++;
    if(mode==2){
     out.leave++;
     if(flag(row,"leaveFullShift")||!text(row,"leaveStart")||!text(row,"leaveEnd"))out.leaveDaily++;else out.leaveHourly++;
    }
    if(mode==1){
     out.rest++;
     if(flag(row,"restFullShift")||!text(row,"restStart")||!text(row,"restEnd"))out.restDaily++;else out.restHourly++;
    }
    if(mode==3)out.swap++;
    if(text(row,"note")||text(row,"noteTime")||text(row,"noteInk"))out.note++;
   }
   boolean excludeLunch=(Boolean)Class.forName("com.eluqen.shiftcalendar.data.Prefs")
    .getMethod("excludeLunchHour",android.content.Context.class).invoke(null,a);
   Class<?> stats=Class.forName("com.eluqen.shiftcalendar.data.CardexStats");
   Object calculated=stats.getMethod("calculate",android.content.Context.class,List.class,boolean.class).invoke(null,a,rows,excludeLunch);
   out.overtimeMinutes=stats.getField("overtimeMinutes").getInt(calculated);
   out.leaveMinutes=stats.getField("leaveMinutes").getInt(calculated);
   out.restMinutes=stats.getField("restMinutes").getInt(calculated);
  }catch(Exception e){android.util.Log.e("ELUQEN_CARDEX","Period-to-date stats",e);}
  return out;
 }
 private static int shiftMinutes(Activity a,String code){
  try{
   Class<?> prefs=Class.forName("com.eluqen.shiftcalendar.data.ShiftPreferences");
   Object profile=prefs.getMethod("get",android.content.Context.class,String.class).invoke(null,a,code);
   if(!profile.getClass().getField("working").getBoolean(profile))return 0;
   int start=profile.getClass().getField("startMinute").getInt(profile);
   int end=profile.getClass().getField("endMinute").getInt(profile);
   if(start<0||end<0||start==end)return 0;
   String from=(String)prefs.getMethod("formatMinute",int.class).invoke(null,start);
   String to=(String)prefs.getMethod("formatMinute",int.class).invoke(null,end);
   boolean excludeLunch=(Boolean)Class.forName("com.eluqen.shiftcalendar.data.Prefs")
    .getMethod("excludeLunchHour",android.content.Context.class).invoke(null,a);
   return (Integer)Class.forName("com.eluqen.shiftcalendar.data.CardexStats")
    .getMethod("duration",String.class,String.class,boolean.class).invoke(null,from,to,excludeLunch);
  }catch(Exception e){android.util.Log.e("ELUQEN_CARDEX","Shift duration",e);return 0;}
 }
 private static int holidayMinutesToDate(Activity a,Object plan,int year,int fromMonth,int toMonth){
  int total=0;
  try{
   Object first=OfficialHoliday.date(year,fromMonth,1),last=OfficialHoliday.date(year,toMonth,1);
   last=last.getClass().getMethod("plusMonths",int.class).invoke(last,1);
   last=last.getClass().getMethod("plusDays",long.class).invoke(last,-1L);
   Object today=Class.forName("com.eluqen.shiftcalendar.core.PersianDate").getMethod("todayTehran").invoke(null);
   long start=(Long)first.getClass().getMethod("toJdn").invoke(first);
   long end=Math.min((Long)last.getClass().getMethod("toJdn").invoke(last),(Long)today.getClass().getMethod("toJdn").invoke(today));
   if(start>end)return 0;
   Class<?> calendar=Class.forName("com.eluqen.shiftcalendar.core.JalaliCalendar");
   Method fromJdn=calendar.getMethod("fromJdn",long.class);
   for(long j=start;j<=end;j++){
    Object date=fromJdn.invoke(null,j);
    if(OfficialHoliday.friday(date)||!OfficialHoliday.official(a,date))continue;
    com.eluqen.shiftcalendar.work.DayWorkResolver.State state=com.eluqen.shiftcalendar.work.DayWorkResolver.resolve(a,
     (com.eluqen.shiftcalendar.core.PersianDate)date,(com.eluqen.shiftcalendar.data.ShiftPlan)plan);
    if(state.shiftSuppressed)continue;
    if(state.hasBaseWork())total+=state.baseWorkedMinutes();
    if(state.hasOvertime)total+=com.eluqen.shiftcalendar.work.DayWorkResolver.durationMinutes(state.overtimeStartMinute,state.overtimeEndMinute);
   }
  }catch(Exception e){android.util.Log.e("ELUQEN_CARDEX","Holiday duration",e);}
  return total;
 }
 private static String clock(int minutes){
  int m=Math.max(0,minutes),h=m/60,r=m%60;
  return OfficialHoliday.fa(String.format(java.util.Locale.US,"%d:%02d",h,r));
 }
 private static TextView smallNote(Activity a,String text){
  TextView n=new TextView(a);n.setText(text);n.setTextSize(11.5f);n.setTextColor(0xff667585);
  n.setGravity(Gravity.RIGHT);n.setPadding(dp(a,5),dp(a,4),dp(a,5),dp(a,5));return n;
 }
 private static String absenceCount(int daily,int hourly){
  if(hourly<=0)return daily+" روز";
  if(daily<=0)return hourly+" مورد ساعتی";
  return daily+" روز / "+hourly+" مورد ساعتی";
 }
 private static String duration(int minutes){
  int m=Math.max(0,minutes),h=m/60,r=m%60;
  if(h==0&&r==0)return "";
  if(h==0)return r+" دقیقه";
  if(r==0)return h+" ساعت";
  return h+" ساعت و "+r+" دقیقه";
 }
 private static String dayDuration(int days,int minutes){
  String d=days+" روز",t=duration(minutes);return t.isEmpty()?d:d+" / "+t;
 }
 private static GradientDrawable bg(Activity a,int fill,int stroke,int radius){
  GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(a,radius));g.setStroke(dp(a,1),stroke);return g;
 }
 private static TextView section(Activity a,String title){
  TextView t=new TextView(a);t.setText(title);t.setTextSize(14.5f);t.setTextColor(0xff263f59);
  t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setGravity(Gravity.RIGHT);
  t.setPadding(dp(a,4),dp(a,8),dp(a,4),dp(a,5));return t;
 }
 private static View metric(Activity a,String title,String value,int fill,int ink){
  LinearLayout box=new LinearLayout(a);box.setOrientation(LinearLayout.VERTICAL);box.setGravity(Gravity.RIGHT);
  box.setPadding(dp(a,9),dp(a,7),dp(a,9),dp(a,7));box.setBackground(bg(a,fill,ink,11));
  TextView name=new TextView(a);name.setText(title);name.setTextSize(11.5f);name.setTextColor(ink);
  name.setTypeface(Typeface.DEFAULT,Typeface.BOLD);name.setGravity(Gravity.RIGHT);
  TextView data=new TextView(a);data.setText(OfficialHoliday.fa(value));data.setTextSize(13.5f);data.setTextColor(0xff24384c);
  data.setTypeface(Typeface.DEFAULT,Typeface.BOLD);data.setGravity(Gravity.RIGHT);data.setSingleLine(false);
  box.addView(name,new LinearLayout.LayoutParams(-1,-2));box.addView(data,new LinearLayout.LayoutParams(-1,-2));
  return box;
 }
 private static void row(Activity a,LinearLayout root,View right,View left){
  LinearLayout r=new LinearLayout(a);r.setOrientation(LinearLayout.HORIZONTAL);r.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
  LinearLayout.LayoutParams p1=new LinearLayout.LayoutParams(0,-2,1);p1.setMargins(dp(a,3),dp(a,3),dp(a,3),dp(a,3));
  LinearLayout.LayoutParams p2=new LinearLayout.LayoutParams(0,-2,1);p2.setMargins(dp(a,3),dp(a,3),dp(a,3),dp(a,3));
  r.addView(right,p1);r.addView(left,p2);root.addView(r,new LinearLayout.LayoutParams(-1,-2));
 }
 private static Button tab(Activity a,String title,boolean active){
  Button b=new Button(a);b.setAllCaps(false);b.setText(title);b.setTextSize(12.5f);b.setMinHeight(0);b.setMinimumWidth(0);
  b.setTextColor(active?0xffffffff:0xff506577);
  b.setBackground(bg(a,active?0xff355f7b:0xffedf3f7,active?0xff355f7b:0xffcbd9e3,10));
  return b;
 }
 private static void tabs(Activity a,LinearLayout root,LinearLayout monthPanel,LinearLayout yearPanel){
  boolean yearly=Boolean.TRUE.equals(YEAR_MODE.get(a));
  LinearLayout bar=new LinearLayout(a);bar.setOrientation(LinearLayout.HORIZONTAL);bar.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
  Button month=tab(a,"ماه",!yearly),year=tab(a,"سال",yearly);
  LinearLayout.LayoutParams pm=new LinearLayout.LayoutParams(0,dp(a,38),1);pm.setMargins(dp(a,3),dp(a,2),dp(a,3),dp(a,4));
  LinearLayout.LayoutParams py=new LinearLayout.LayoutParams(0,dp(a,38),1);py.setMargins(dp(a,3),dp(a,2),dp(a,3),dp(a,4));
  bar.addView(month,pm);bar.addView(year,py);root.addView(bar);
  monthPanel.setVisibility(yearly?View.GONE:View.VISIBLE);yearPanel.setVisibility(yearly?View.VISIBLE:View.GONE);
  month.setOnClickListener(v->{YEAR_MODE.put(a,false);monthPanel.setVisibility(View.VISIBLE);yearPanel.setVisibility(View.GONE);
   month.setTextColor(0xffffffff);month.setBackground(bg(a,0xff355f7b,0xff355f7b,10));
   year.setTextColor(0xff506577);year.setBackground(bg(a,0xffedf3f7,0xffcbd9e3,10));});
  year.setOnClickListener(v->{YEAR_MODE.put(a,true);yearPanel.setVisibility(View.VISIBLE);monthPanel.setVisibility(View.GONE);
   year.setTextColor(0xffffffff);year.setBackground(bg(a,0xff355f7b,0xff355f7b,10));
   month.setTextColor(0xff506577);month.setBackground(bg(a,0xffedf3f7,0xffcbd9e3,10));});
 }
 private static LinearLayout summary(Activity a,TextView totals){
  if(!(totals.getParent() instanceof LinearLayout))return null;
  LinearLayout parent=(LinearLayout)totals.getParent();LinearLayout panel=null;
  for(int i=0;i<parent.getChildCount();i++)if(SUMMARY.equals(parent.getChildAt(i).getTag()))panel=(LinearLayout)parent.getChildAt(i);
  if(panel==null){
   panel=new LinearLayout(a);panel.setTag(SUMMARY);panel.setOrientation(LinearLayout.VERTICAL);
   panel.setPadding(dp(a,7),dp(a,5),dp(a,7),dp(a,7));panel.setBackground(bg(a,0xfff8fbfd,0xffd4e0ea,13));
   int at=parent.indexOfChild(totals);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(a,8),0,0);
   parent.addView(panel,Math.min(at+1,parent.getChildCount()),lp);
  }
  panel.removeAllViews();totals.setVisibility(View.GONE);return panel;
 }
 private static void clearLegacy(Activity a){
  try{android.content.SharedPreferences p=a.getSharedPreferences(LEGACY,0);if(!p.getAll().isEmpty())p.edit().clear().commit();}
  catch(Exception e){android.util.Log.e("ELUQEN_CARDEX","Clear retired holiday-work data",e);}
 }
 private static void installToday(Activity a,Object selectedButton){
  if(!(selectedButton instanceof Button))return;
  Button current=(Button)selectedButton;if(!(current.getParent() instanceof LinearLayout))return;
  LinearLayout parent=(LinearLayout)current.getParent();Button today=null;
  for(int i=0;i<parent.getChildCount();i++)if(TODAY.equals(parent.getChildAt(i).getTag()))today=(Button)parent.getChildAt(i);
  if(today==null){
   today=new Button(a);today.setTag(TODAY);today.setText("⌖ امروز");today.setAllCaps(false);
   today.setTextSize(12);today.setTextColor(0xff087e80);today.setMinHeight(0);today.setMinimumWidth(0);
   GradientDrawable b=new GradientDrawable();b.setColor(0xffdff3ef);b.setCornerRadius(dp(a,13));b.setStroke(dp(a,1),0xff80c6ba);today.setBackground(b);
   parent.addView(today,Math.min(1,parent.getChildCount()),new LinearLayout.LayoutParams(-1,dp(a,38)));
  }
  final Button control=today;
  control.setOnClickListener(v->{try{
   Object now=Class.forName("com.eluqen.shiftcalendar.core.PersianDate").getMethod("todayTehran").invoke(null);
   Field date=a.getClass().getDeclaredField("selected");date.setAccessible(true);date.set(a,now);
   try{Class.forName("com.eluqen.shiftcalendar.ui.MonthStep").getMethod("reset",Activity.class).invoke(null,a);}catch(Exception ignored){}
   for(String name:new String[]{"updateSelectedButton","refresh"}){Method m=a.getClass().getDeclaredMethod(name);m.setAccessible(true);m.invoke(a);}
  }catch(Exception e){android.util.Log.e("ELUQEN_CARDEX","Today action",e);}});
 }
 public static void status(Activity a,Object selected,TextView totals,Object selectedButton){
  clearLegacy(a);installToday(a,selectedButton);
  try{
   if(selectedButton instanceof Button&&((Button)selectedButton).getParent() instanceof LinearLayout){
    LinearLayout parent=(LinearLayout)((Button)selectedButton).getParent();
    Object today=Class.forName("com.eluqen.shiftcalendar.core.PersianDate").getMethod("todayTehran").invoke(null);
    for(int i=0;i<parent.getChildCount();i++)if(TODAY.equals(parent.getChildAt(i).getTag()))
     parent.getChildAt(i).setVisibility(today.equals(selected)?View.GONE:View.VISIBLE);
   }
   int year=OfficialHoliday.num(selected,"year"),month=OfficialHoliday.num(selected,"month");
   String monthName=String.valueOf(selected.getClass().getMethod("monthName").invoke(selected));
   Object plan=OfficialHoliday.active(a);
   OfficialHoliday.Stats m=OfficialHoliday.calculate(a,plan,year,month,true);
   OfficialHoliday.Stats y=OfficialHoliday.calculate(a,plan,year,0,true);
   Period pm=period(a,year,month,month),py=period(a,year,1,12);
   Period pmNow=periodToDate(a,year,month,month),pyNow=periodToDate(a,year,1,12);
   int hmNow=holidayMinutesToDate(a,plan,year,month,month);
   int hyNow=holidayMinutesToDate(a,plan,year,1,12);
   LinearLayout panel=summary(a,totals);if(panel==null)return;

   panel.addView(section(a,"گزارش کارکرد"));
   LinearLayout monthPanel=new LinearLayout(a);monthPanel.setOrientation(LinearLayout.VERTICAL);
   LinearLayout yearPanel=new LinearLayout(a);yearPanel.setOrientation(LinearLayout.VERTICAL);yearPanel.setVisibility(View.GONE);
   tabs(a,panel,monthPanel,yearPanel);

   monthPanel.addView(section(a,monthName+" "+OfficialHoliday.fa(year)));
   monthPanel.addView(section(a,"مجموع مدت تا امروز"));
   monthPanel.addView(smallNote(a,"نمایش مدت به صورت دقیقه : ساعت"));
   row(a,monthPanel,
    metric(a,"اضافه‌کاری",clock(pmNow.overtimeMinutes),0xffe7f5ec,0xff2b7a50),
    metric(a,"مرخصی",clock(pmNow.leaveMinutes),0xfffff0df,0xffa95f16));
   row(a,monthPanel,
    metric(a,"استراحت",clock(pmNow.restMinutes),0xffe7f2fb,0xff2f6f9d),
    metric(a,"تعطیل‌کاری",clock(hmNow),0xffffe9f1,0xffa14369));

   monthPanel.addView(section(a,"تعداد و روزها"));
   row(a,monthPanel,
    metric(a,"اضافه‌کاری",pm.overtime+" روز",0xffe7f5ec,0xff2b7a50),
    metric(a,"مرخصی",absenceCount(pm.leaveDaily,pm.leaveHourly),0xfffff0df,0xffa95f16));
   row(a,monthPanel,
    metric(a,"استراحت",absenceCount(pm.restDaily,pm.restHourly),0xffe7f2fb,0xff2f6f9d),
    metric(a,"تعویض شیفت",pm.swap+" روز",0xfff0eafb,0xff74519a));
   row(a,monthPanel,
    metric(a,"یادداشت",pm.note+" روز",0xffeef1f4,0xff667585),
    metric(a,"روز ثبت‌شده",pm.recordedDays+" روز",0xffe5f4f3,0xff287d78));
   row(a,monthPanel,
    metric(a,"روزکاری",m.day+" روز",0xffe7f4ec,0xff397451),
    metric(a,"عصرکاری",m.evening+" روز",0xfffff5dc,0xff9a7017));
   row(a,monthPanel,
    metric(a,"شب‌کاری",m.night+" روز",0xffebeafa,0xff56528d),
    metric(a,"جمعه‌کاری",m.fridayWork+" روز",0xffffe8e8,0xffa94444));
   row(a,monthPanel,
    metric(a,"تعطیل‌کاری",m.officialWork+" روز",0xffffe9f1,0xffa14369),
    metric(a,"استراحت الگو",m.rest+" روز",0xffedf4f7,0xff577281));

   yearPanel.addView(section(a,"سال "+OfficialHoliday.fa(year)));
   yearPanel.addView(section(a,"مجموع مدت تا امروز"));
   yearPanel.addView(smallNote(a,"نمایش مدت به صورت دقیقه : ساعت"));
   row(a,yearPanel,
    metric(a,"اضافه‌کاری",clock(pyNow.overtimeMinutes),0xffe7f5ec,0xff2b7a50),
    metric(a,"مرخصی",clock(pyNow.leaveMinutes),0xfffff0df,0xffa95f16));
   row(a,yearPanel,
    metric(a,"استراحت",clock(pyNow.restMinutes),0xffe7f2fb,0xff2f6f9d),
    metric(a,"تعطیل‌کاری",clock(hyNow),0xffffe9f1,0xffa14369));

   yearPanel.addView(section(a,"تعداد و روزها"));
   row(a,yearPanel,
    metric(a,"اضافه‌کاری",py.overtime+" روز",0xffe7f5ec,0xff2b7a50),
    metric(a,"مرخصی",absenceCount(py.leaveDaily,py.leaveHourly),0xfffff0df,0xffa95f16));
   row(a,yearPanel,
    metric(a,"استراحت",absenceCount(py.restDaily,py.restHourly),0xffe7f2fb,0xff2f6f9d),
    metric(a,"تعویض شیفت",py.swap+" روز",0xfff0eafb,0xff74519a));
   row(a,yearPanel,
    metric(a,"یادداشت",py.note+" روز",0xffeef1f4,0xff667585),
    metric(a,"روز ثبت‌شده",py.recordedDays+" روز",0xffe5f4f3,0xff287d78));
   row(a,yearPanel,
    metric(a,"جمعه‌کاری",y.fridayWork+" روز",0xffffe8e8,0xffa94444),
    metric(a,"تعطیل‌کاری",y.officialWork+" روز",0xffffe9f1,0xffa14369));
   panel.addView(monthPanel,new LinearLayout.LayoutParams(-1,-2));
   panel.addView(yearPanel,new LinearLayout.LayoutParams(-1,-2));

   if(m.leaveExcluded>0||m.overlap>0){
    TextView note=new TextView(a);StringBuilder n=new StringBuilder();
    if(m.leaveExcluded>0)n.append("مرخصی یا استراحت ثبت‌شده از محاسبه کارکرد خودکار کسر شده: ").append(m.leaveExcluded).append(" روز");
    if(m.overlap>0){if(n.length()>0)n.append("\n");n.append("جمعه‌های مناسبتی فقط جمعه‌کاری محسوب می‌شوند: ").append(m.overlap).append(" روز");}
    note.setText(OfficialHoliday.fa(n.toString()));note.setTextSize(11.5f);note.setTextColor(0xff6b7887);note.setGravity(Gravity.RIGHT);
    note.setPadding(dp(a,5),dp(a,7),dp(a,5),dp(a,2));panel.addView(note);
   }
  }catch(Exception error){android.util.Log.e("ELUQEN_CARDEX","Automatic cardex stats",error);}
 }
}
