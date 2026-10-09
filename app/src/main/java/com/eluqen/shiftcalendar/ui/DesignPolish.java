package com.eluqen.shiftcalendar.ui;
import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
public final class DesignPolish implements View.OnTouchListener {
 private static int NAVY=0xff203a63, DEEP=0xff122541, BLUE=0xff355d93;
 private static int TEAL=0xff108b86, INK=0xff182b45, MUTED=0xff63758d;
 private static int SOFT=0xffedf3fb, BORDER=0xffdce5ed, WHITE=Color.WHITE;
 static void applyTheme(AppTheme.Palette p){
  NAVY=p.primary;DEEP=p.primaryDark;BLUE=p.secondary;
  TEAL=p.secondary;INK=p.text;MUTED=p.muted;
  SOFT=p.surfaceAlt;BORDER=p.border;WHITE=p.card;
 }
 private final LinearLayout nav;
 private int selected=0, hover=-1;
 private DesignPolish(LinearLayout parent){nav=parent;}
 private static int dp(Context c,float x){return Math.round(c.getResources().getDisplayMetrics().density*x);}
 private static GradientDrawable shape(Context c,int fill,int radius,int stroke){
  GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(c,radius));
  if(stroke!=0)d.setStroke(dp(c,1),stroke);return d;
 }
 private static int mix(int foreground,int background,int foregroundPercent){
  int p=Math.max(0,Math.min(100,foregroundPercent)),q=100-p;
  return Color.rgb(
   (Color.red(foreground)*p+Color.red(background)*q)/100,
   (Color.green(foreground)*p+Color.green(background)*q)/100,
   (Color.blue(foreground)*p+Color.blue(background)*q)/100);
 }
 public static void card(LinearLayout c){
  Context ctx=c.getContext();c.setBackground(shape(ctx,WHITE,22,BORDER));
  c.setPadding(dp(ctx,18),dp(ctx,16),dp(ctx,18),dp(ctx,16));
  c.setElevation(dp(ctx,2));
 }
 public static void toolbar(LinearLayout bar){
  Context ctx=bar.getContext();GradientDrawable d=new GradientDrawable(
   GradientDrawable.Orientation.LEFT_RIGHT,
   new int[]{DEEP,NAVY,mix(BLUE,NAVY,42)});
  bar.setBackground(d);bar.setPadding(dp(ctx,12),dp(ctx,5),dp(ctx,16),dp(ctx,5));
  if(bar.getChildCount()>0 && bar.getChildAt(0) instanceof TextView){
   TextView title=(TextView)bar.getChildAt(0);
   String name=title.getText().toString();if(name.contains("نوبت"))title.setText("تقویم شیفت");
   title.setLetterSpacing(.01f);
  }
 }
 public static void button(Button b,boolean tonal){
  Context ctx=b.getContext();int normal=tonal?SOFT:NAVY, pressed=tonal?mix(BLUE,SOFT,18):BLUE;
  StateListDrawable bg=new StateListDrawable();
  bg.addState(new int[]{android.R.attr.state_pressed},shape(ctx,pressed,15,tonal?mix(BLUE,BORDER,38):0));
  bg.addState(new int[]{},shape(ctx,normal,15,tonal?BORDER:0));
  b.setBackgroundTintList(null);b.setBackground(bg);
  b.setTextColor(tonal?NAVY:WHITE);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
  b.setAllCaps(false);b.setMinHeight(dp(ctx,48));
  b.setOnTouchListener((v,e)->{
   int a=e.getActionMasked();
   if(a==MotionEvent.ACTION_DOWN)v.animate().scaleX(.97f).scaleY(.97f).setDuration(100).start();
   else if(a==MotionEvent.ACTION_UP||a==MotionEvent.ACTION_CANCEL)v.animate().scaleX(1f).scaleY(1f).setDuration(130).start();
   return false;
  });
 }
 public static void attachNav(LinearLayout bar){
  DesignPolish handler=new DesignPolish(bar);Context ctx=bar.getContext();
  bar.setBackground(shape(ctx,WHITE,19,BORDER));
  bar.setPadding(dp(ctx,5),dp(ctx,8),dp(ctx,5),dp(ctx,9));
  bar.setElevation(dp(ctx,11));bar.setClipChildren(false);bar.setClipToPadding(false);
  String[] labels={"▦\nتقویم","◷\nچهارشیفت","▤\nکارکرد","▥\nنمودار","✦\nامکانات"};
  for(int i=0;i<bar.getChildCount();i++){
   View v=bar.getChildAt(i);
   if(!(v instanceof Button))continue;Button b=(Button)v;
   b.setText(i<labels.length?labels[i]:b.getText());b.setTextSize(11);
   b.setMinHeight(dp(ctx,62));b.setMinWidth(0);b.setAllCaps(false);
   b.setPadding(dp(ctx,1),dp(ctx,4),dp(ctx,1),dp(ctx,4));
   LinearLayout.LayoutParams lp=(LinearLayout.LayoutParams)b.getLayoutParams();
   lp.height=dp(ctx,64);lp.setMargins(dp(ctx,2),0,dp(ctx,2),0);b.setLayoutParams(lp);
   b.setBackgroundTintList(null);b.setOnTouchListener(handler);
  }
  handler.paint(false);
 }
 private int location(float rawX,float rawY){
  int[] point=new int[2];nav.getLocationOnScreen(point);
  if(rawY<point[1]-dp(nav.getContext(),18)||rawY>point[1]+nav.getHeight()+dp(nav.getContext(),18))return -1;
  for(int i=0;i<nav.getChildCount();i++){
   View child=nav.getChildAt(i);child.getLocationOnScreen(point);
   if(rawX>=point[0]&&rawX<point[0]+child.getWidth())return i;
  }return -1;
 }
 private void paint(boolean animated){
  for(int i=0;i<nav.getChildCount();i++){
   if(!(nav.getChildAt(i) instanceof Button))continue;
   Button b=(Button)nav.getChildAt(i);
   boolean highlighted=(hover>=0?i==hover:i==selected);
   int shade=hover>=0?TEAL:NAVY;
   b.setBackgroundTintList(null);
   b.setBackground(shape(nav.getContext(),highlighted?shade:WHITE,14,0));
   b.setTextColor(highlighted?WHITE:MUTED);
   b.setTypeface(Typeface.DEFAULT,highlighted?Typeface.BOLD:Typeface.NORMAL);
   b.setElevation(dp(nav.getContext(),highlighted?3:0));
   if(animated)b.animate().scaleX(highlighted?1.03f:1f).scaleY(highlighted?1.03f:1f).setDuration(110).start();
   else {b.setScaleX(highlighted?1.03f:1f);b.setScaleY(highlighted?1.03f:1f);}
  }
 }
 @Override public boolean onTouch(View source,MotionEvent event){
  int action=event.getActionMasked();
  if(action==MotionEvent.ACTION_DOWN){
   hover=nav.indexOfChild(source);nav.requestDisallowInterceptTouchEvent(true);
   paint(true);return true;
  }
  if(action==MotionEvent.ACTION_MOVE){
   int choice=location(event.getRawX(),event.getRawY());
   if(choice!=hover){hover=choice;paint(true);}return true;
  }
  if(action==MotionEvent.ACTION_UP){
   int choice=location(event.getRawX(),event.getRawY());
   hover=-1;if(choice>=0){selected=choice;paint(true);nav.getChildAt(choice).performClick();}
   else paint(true);
   return true;
  }
  if(action==MotionEvent.ACTION_CANCEL){hover=-1;paint(true);return true;}
  return true;
 }
}
