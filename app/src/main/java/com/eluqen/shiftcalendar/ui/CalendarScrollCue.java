package com.eluqen.shiftcalendar.ui;
import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;

public final class CalendarScrollCue {
 private static final String TAG="ELUQEN_CAL_SCROLL_CUE";
 private CalendarScrollCue(){}
 private static int dp(Activity a,float n){return Math.round(n*a.getResources().getDisplayMetrics().density);}
 private static ScrollView find(View v){
  if(v instanceof ScrollView)return (ScrollView)v;
  if(v instanceof ViewGroup){
   ViewGroup g=(ViewGroup)v;
   for(int i=0;i<g.getChildCount();i++){ScrollView s=find(g.getChildAt(i));if(s!=null)return s;}
  }
  return null;
 }
 private static TextView existing(ViewGroup g){
  for(int i=0;i<g.getChildCount();i++){View v=g.getChildAt(i);if(TAG.equals(v.getTag())&&v instanceof TextView)return (TextView)v;}
  return null;
 }
 public static void show(Activity a){
  View content=a.findViewById(android.R.id.content);
  final ScrollView scroll=find(content);if(scroll==null)return;
  if(!(scroll.getParent() instanceof LinearLayout))return;
  final LinearLayout parent=(LinearLayout)scroll.getParent();
  TextView cue=existing(parent);
  if(cue==null){
   cue=new TextView(a);cue.setTag(TAG);cue.setText("⌄  تعطیلات و مناسبت‌ها پایین‌تر");
   cue.setTextSize(12.5f);cue.setTextColor(0xff31546f);cue.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
   cue.setGravity(Gravity.CENTER);cue.setIncludeFontPadding(false);cue.setPadding(dp(a,12),0,dp(a,12),0);
   GradientDrawable g=new GradientDrawable();g.setColor(0xffeef5f8);g.setCornerRadius(dp(a,13));g.setStroke(dp(a,1),0xffd6e4eb);cue.setBackground(g);
   int index=parent.indexOfChild(scroll)+1;
   LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(a,34));lp.setMargins(dp(a,16),dp(a,2),dp(a,16),dp(a,4));
   parent.addView(cue,index,lp);
  }
  final TextView hint=cue;
  hint.setOnClickListener(v->scroll.smoothScrollBy(0,Math.max(dp(a,160),(int)(scroll.getHeight()*.72f))));
  scroll.setOnScrollChangeListener((v,x,y,ox,oy)->sync(a,scroll,hint));
  scroll.post(()->sync(a,scroll,hint));
 }
 private static void sync(Activity a,ScrollView scroll,TextView hint){
  boolean top=scroll.getScrollY()<=dp(a,6);
  hint.animate().cancel();
  hint.setAlpha(top?1f:0f);
  hint.setVisibility(top?View.VISIBLE:View.GONE);
 }
}
