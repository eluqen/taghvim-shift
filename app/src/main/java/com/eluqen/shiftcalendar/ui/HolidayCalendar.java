package com.eluqen.shiftcalendar.ui;
import android.app.Activity;
import android.graphics.Typeface;
import android.view.*;
import android.widget.*;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Holiday metadata/decorations that do not own the visual background of a
 * calendar day cell. MainActivity is the single source of truth for cell
 * fill/stroke/text so holiday, selected and today states cannot fight.
 */
public final class HolidayCalendar {
 private static final String TAG="eluqen_official_month_list";
 private HolidayCalendar(){}
 private static int dp(Activity c,int n){return Math.round(c.getResources().getDisplayMetrics().density*n);}
 public static void cell(Activity a,Object d,LinearLayout root,TextView day){
  try{
   Field displayed=a.getClass().getDeclaredField("displayedMonth");displayed.setAccessible(true);
   Object month=displayed.get(a);
   if(OfficialHoliday.num(d,"year")!=OfficialHoliday.num(month,"year")||
       OfficialHoliday.num(d,"month")!=OfficialHoliday.num(month,"month"))return;
   if(!OfficialHoliday.official(a,d))return;
   day.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
   root.setContentDescription(OfficialHoliday.fa(OfficialHoliday.num(d,"day"))+
     "؛ تعطیلی مناسبتی: "+android.text.TextUtils.join("، ",OfficialHoliday.reasons(a,d)));
  }catch(Exception error){android.util.Log.e("ELUQEN_HOLIDAY","Decorate calendar",error);}
 }
 public static void month(Activity a,Object month,GridLayout grid){
  if(!(grid.getParent() instanceof LinearLayout))return;
  LinearLayout parent=(LinearLayout)grid.getParent();
  for(int i=parent.getChildCount()-1;i>=0;i--)if(TAG.equals(parent.getChildAt(i).getTag()))parent.removeViewAt(i);
  LinearLayout card=new LinearLayout(a);card.setTag(TAG);card.setOrientation(android.widget.LinearLayout.VERTICAL);
  card.setPadding(dp(a,13),dp(a,11),dp(a,13),dp(a,12));card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
  android.graphics.drawable.GradientDrawable background=new android.graphics.drawable.GradientDrawable();
  background.setColor(0xfffff4f5);background.setCornerRadius(dp(a,12));background.setStroke(dp(a,1),0xffedbdc6);
  card.setBackground(background);
  TextView title=new TextView(a);title.setText("تعطیلات مناسبتی این ماه");
  title.setTextSize(16);title.setTextColor(0xff8e2843);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
  title.setGravity(Gravity.RIGHT);card.addView(title);
  int count=0;
  try{
   int yy=OfficialHoliday.num(month,"year"),mm=OfficialHoliday.num(month,"month");
   String monthName=String.valueOf(month.getClass().getMethod("monthName").invoke(month));
   int days=(Integer)Class.forName("com.eluqen.shiftcalendar.core.JalaliCalendar").getMethod("daysInMonth",int.class,int.class).invoke(null,yy,mm);
   for(int i=1;i<=days;i++){
    Object date=OfficialHoliday.date(yy,mm,i);
    List<String> reasons=OfficialHoliday.reasons(a,date);if(reasons.isEmpty())continue;
    TextView item=new TextView(a);
    item.setText(OfficialHoliday.fa(i)+" "+monthName+" — "+android.text.TextUtils.join("، ",reasons));
    item.setTextColor(0xff633e49);item.setTextSize(13);item.setGravity(Gravity.RIGHT);
    item.setPadding(0,dp(a,6),0,dp(a,6));card.addView(item);count++;
   }
  }catch(Exception e){android.util.Log.e("ELUQEN_HOLIDAY","Monthly list",e);}
  if(count==0){TextView none=new TextView(a);none.setText("تعطیلی مناسبتی برای این ماه ثبت نشده است.");
   none.setTextColor(0xff705965);none.setTextSize(13);none.setGravity(Gravity.RIGHT);card.addView(none);}
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
  lp.setMargins(0,dp(a,8),0,dp(a,7));parent.addView(card,lp);
 }
}
