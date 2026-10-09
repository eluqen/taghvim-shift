package com.eluqen.shiftcalendar.ui;
import android.app.*;
import android.content.*;
import android.view.*;
import android.widget.*;
import java.lang.reflect.*;
import java.util.*;
public final class FourShiftPage {
 private FourShiftPage(){}
 public static void clear(Activity a){initial.remove(a);}
 private static final WeakHashMap<Activity,Boolean> initial=new WeakHashMap<>();
 private static int dp(Activity a,int n){return Math.round(n*a.getResources().getDisplayMetrics().density);}
 public static void afterRender(Activity a,Object month,LinearLayout table){
  ShiftMonth.footer(a,month,table);
  ShiftMonth.picker(a);
 }
 private static Object today()throws Exception{
  return Class.forName("com.eluqen.shiftcalendar.core.PersianDate").getMethod("todayTehran").invoke(null);
 }
 private static boolean currentMonth(Object month,Object date)throws Exception{
  return month.getClass().getField("year").getInt(month)==date.getClass().getField("year").getInt(date)
   &&month.getClass().getField("month").getInt(month)==date.getClass().getField("month").getInt(date);
 }
 private static void scrollToday(Activity a,LinearLayout table,Object month){
  try{
   Object now=today();if(!currentMonth(month,now))return;
   int day=now.getClass().getField("day").getInt(now);
   if(day<1||day>=table.getChildCount())return;
   ViewParent parent=table.getParent();
   if(parent instanceof ScrollView){
    ScrollView scroll=(ScrollView)parent;
    table.post(()->scroll.smoothScrollTo(0,Math.max(0,table.getChildAt(day).getTop()-dp(a,85))));
   }
  }catch(Exception e){android.util.Log.e("ELUQEN_FOUR","Scroll today",e);}
 }
 private static void status(Activity a,Object month,LinearLayout table,Button button){
  try{
   Object now=today();boolean in=currentMonth(month,now);
   if(!in){button.setVisibility(View.VISIBLE);return;}
   int day=now.getClass().getField("day").getInt(now);
   if(day<1||day>=table.getChildCount()){button.setVisibility(View.VISIBLE);return;}
   ViewParent parent=table.getParent();if(!(parent instanceof ScrollView))return;
   ScrollView scroll=(ScrollView)parent;
   int delta=Math.abs(table.getChildAt(day).getTop()-scroll.getScrollY()-dp(a,85));
   button.setVisibility(delta>dp(a,110)?View.VISIBLE:View.INVISIBLE);
  }catch(Exception e){android.util.Log.e("ELUQEN_FOUR","Today visibility",e);}
 }
 private static void ensureToday(Activity a,Object month,LinearLayout table){
  try{
   Field f=a.getClass().getDeclaredField("monthTitle");f.setAccessible(true);
   View title=(View)f.get(a);
   if(!(title.getParent() instanceof LinearLayout))return;
   LinearLayout bar=(LinearLayout)title.getParent();Button button=null;
   for(int i=0;i<bar.getChildCount();i++)if("eluqen_four_today".equals(bar.getChildAt(i).getTag()))
    button=(Button)bar.getChildAt(i);
   if(button==null){
    button=new Button(a);button.setTag("eluqen_four_today");button.setText("⌖ امروز");button.setAllCaps(false);
    button.setTextSize(12);button.setMinHeight(0);button.setMinimumWidth(0);button.setTextColor(Ui.SECONDARY);
    android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();
    bg.setColor(Ui.SURFACE_ALT);bg.setCornerRadius(dp(a,13));bg.setStroke(dp(a,1),Ui.SECONDARY);
    button.setBackground(bg);
    for(int i=0;i<bar.getChildCount();i++)if(bar.getChildAt(i) instanceof Button){
     LinearLayout.LayoutParams lp=(LinearLayout.LayoutParams)bar.getChildAt(i).getLayoutParams();
     lp.width=dp(a,70);bar.getChildAt(i).setLayoutParams(lp);
    }
    bar.addView(button,bar.indexOfChild(title)+1,new LinearLayout.LayoutParams(dp(a,73),dp(a,36)));
   }
   final Button control=button;
   control.setOnClickListener(v->{
    try{
     Object now=today();
     if(!currentMonth(month,now)){
      Object target=now.getClass().getMethod("firstOfMonth").invoke(now);
      Field current=a.getClass().getDeclaredField("month");current.setAccessible(true);current.set(a,target);
      Method render=a.getClass().getDeclaredMethod("render");render.setAccessible(true);render.invoke(a);
      Field targetTable=a.getClass().getDeclaredField("table");targetTable.setAccessible(true);
      scrollToday(a,(LinearLayout)targetTable.get(a),target);
     }else scrollToday(a,table,month);
    }catch(Exception ex){android.util.Log.e("ELUQEN_FOUR","Today action",ex);}
   });
   if(table.getParent() instanceof ScrollView){
    ScrollView scroll=(ScrollView)table.getParent();
    scroll.setOnScrollChangeListener((view,x,y,ox,oy)->status(a,month,table,control));
   }
   table.post(()->status(a,month,table,control));
  }catch(Exception e){android.util.Log.e("ELUQEN_FOUR","Today button",e);}
 }

}
