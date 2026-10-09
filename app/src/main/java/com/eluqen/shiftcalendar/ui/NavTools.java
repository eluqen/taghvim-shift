package com.eluqen.shiftcalendar.ui;
import android.app.*;
import android.content.*;
import android.view.*;
import android.widget.*;
public final class NavTools {
 private NavTools(){}
 private static int dp(Activity a,int n){return Math.round(a.getResources().getDisplayMetrics().density*n);}
 public static void hub(Activity a,LinearLayout root){
  if(root.getChildCount()<2||!(root.getChildAt(1) instanceof ScrollView))return;
  ScrollView scroll=(ScrollView)root.getChildAt(1);
  if(scroll.getChildCount()<1||!(scroll.getChildAt(0) instanceof LinearLayout))return;
  LinearLayout body=(LinearLayout)scroll.getChildAt(0);int where=-1;
  for(int i=body.getChildCount()-1;i>=0;i--){
   View v=body.getChildAt(i);if(!(v instanceof LinearLayout))continue;
   LinearLayout card=(LinearLayout)v;
   if(card.getChildCount()==0||!(card.getChildAt(0) instanceof TextView))continue;
   if(((TextView)card.getChildAt(0)).getText().toString().equals("محاسبات و خروجی")){where=i;body.removeViewAt(i);break;}
  }
  if(where<0)return;
  String[] titles={"محاسبه شیفت","خروجی فایل ICS","اشتراک برنامه ۳۰ روز آینده"};
  String[] notes={"تعداد انواع شیفت در بازه دلخواه","دریافت خروجی استاندارد تقویم","اشتراک خلاصه شیفت‌های سی روز آینده"};
  String[] actions={"calculate","ics","share"};
  for(int i=0;i<titles.length;i++){
   final String action=actions[i];LinearLayout card=new LinearLayout(a);card.setOrientation(android.widget.LinearLayout.VERTICAL);card.setPadding(dp(a,12),dp(a,10),dp(a,12),dp(a,10));android.graphics.drawable.GradientDrawable shape=new android.graphics.drawable.GradientDrawable();shape.setColor(Ui.CARD);shape.setCornerRadius(dp(a,17));shape.setStroke(dp(a,1),Ui.BORDER);card.setBackground(shape);
   TextView heading=new TextView(a);heading.setText(titles[i]);heading.setTextSize(18);heading.setTextColor(Ui.TEXT);card.addView(heading);TextView sub=new TextView(a);sub.setText(notes[i]);sub.setTextSize(13);sub.setTextColor(Ui.MUTED);card.addView(sub);
   Button go=new Button(a);go.setText(action.equals("calculate")?"محاسبه":action.equals("ics")?"ساخت فایل ICS":"اشتراک متن");go.setAllCaps(false);go.setTextColor(Ui.PRIMARY);go.setOnClickListener(v->ToolActions.run(a,action));
   card.addView(go,new LinearLayout.LayoutParams(-1,dp(a,46)));
   LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,dp(a,10));body.addView(card,where+i,lp);
  }
 }
 public static void direct(Activity a){
  String action=a.getIntent().getStringExtra("eluqen_direct_tool");if(action==null)return;
  String method=action.equals("calculate")?"calculateShifts":action.equals("convert")?"convertDate":action.equals("ics")?"chooseIcsRange":action.equals("share")?"shareNextThirtyDays":null;
  if(method==null)return;
  try{java.lang.reflect.Method m=a.getClass().getDeclaredMethod(method);m.setAccessible(true);m.invoke(a);}
  catch(Exception e){android.util.Log.e("ELUQEN_TOOLS","Direct action failed",e);Toast.makeText(a,"اجرای این ابزار امکان‌پذیر نشد",Toast.LENGTH_LONG).show();}
 }
}
