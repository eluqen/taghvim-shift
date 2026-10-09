package com.eluqen.shiftcalendar.ui;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
public final class ScreenPolish {
 private ScreenPolish(){}
 private static int dp(Context c,int n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
 public static void widget(LinearLayout root){
  ScrollView scroll=(ScrollView)root.getChildAt(1);
  WidgetEditor.build((android.app.Activity)root.getContext(),(LinearLayout)scroll.getChildAt(0));
 }
 public static void tools(LinearLayout root){
  ScrollView scroll=(ScrollView)root.getChildAt(1);
  LinearLayout body=(LinearLayout)scroll.getChildAt(0);
  for(int i=body.getChildCount()-1;i>=0;i--){
   View v=body.getChildAt(i);if(!(v instanceof LinearLayout))continue;
   LinearLayout card=(LinearLayout)v;
   if(card.getChildCount()==0||!(card.getChildAt(0) instanceof TextView))continue;
   String name=((TextView)card.getChildAt(0)).getText().toString();
   if(name.equals("تقویم چهارشیفت")||name.equals("نمودار شیفت")||name.equals("یادآوری شروع شیفت")||name.equals("هم‌شیفت‌ها و همکاران")||name.equals("پشتیبان‌گیری و بازیابی"))body.removeViewAt(i);
  }
 }
 public static void settings(LinearLayout root){
  if(root.getChildCount()<2 || !(root.getChildAt(1) instanceof ScrollView))return;
  ScrollView scroll=(ScrollView)root.getChildAt(1);
  if(scroll.getChildCount()<1 || !(scroll.getChildAt(0) instanceof LinearLayout))return;
  LinearLayout body=(LinearLayout)scroll.getChildAt(0);
  boolean general=((android.app.Activity)root.getContext()).getIntent().getBooleanExtra("eluqen_general_settings",false);
  for(int i=body.getChildCount()-1;i>=0;i--){
   View v=body.getChildAt(i);
   if(!(v instanceof TextView))continue;
   String heading=((TextView)v).getText().toString();
   boolean plan=heading.equals("الگوی شیفت");
   boolean duplicate=heading.contains("تعریف نوبت") || heading.equals("به‌روزرسانی") || heading.equals("درباره");
   if((general && plan) || duplicate || (!general && !plan)){
    if(i+1<body.getChildCount())body.removeViewAt(i+1);
    body.removeViewAt(i);
   }
  }
  if(general){
   for(int i=0;i<body.getChildCount();i++){
    View v=body.getChildAt(i);
    if(!(v instanceof LinearLayout))continue;
    LinearLayout card=(LinearLayout)v;
    for(int j=card.getChildCount()-1;j>=0;j--){
     View child=card.getChildAt(j);
     if(!(child instanceof Button))continue;
     String title=((Button)child).getText().toString();
     if(title.contains("ویجت"))card.removeViewAt(j);
    }
   }
  }
  if(!general)CompanyPlans.attach((android.app.Activity)root.getContext());
  if(root.getChildAt(0) instanceof LinearLayout){
   LinearLayout toolbar=(LinearLayout)root.getChildAt(0);
   if(toolbar.getChildCount()>0 && toolbar.getChildAt(0) instanceof TextView)
    ((TextView)toolbar.getChildAt(0)).setText(general?"تنظیمات عمومی":"مدیریت الگوها");
  }
 }
}
