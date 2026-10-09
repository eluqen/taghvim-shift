package com.eluqen.shiftcalendar.ui;
import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.view.*;

public final class LiveTouch{
 private LiveTouch(){}
 private static int dp(Activity a,float v){return Math.round(v*a.getResources().getDisplayMetrics().density);}
 private static GradientDrawable bg(Activity a,int color,float r){
  GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(a,r));return g;
 }
 public static void bind(Activity a,View v,int normal,int pressed,float radius,Runnable action){
  v.setClickable(true);v.setFocusable(true);v.setBackground(bg(a,normal,radius));
  v.setOnTouchListener((x,e)->{
   int t=e.getActionMasked();
   if(t==MotionEvent.ACTION_DOWN){x.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);x.animate().cancel();x.animate().scaleX(.985f).scaleY(.985f).setDuration(70).start();x.setBackground(bg(a,pressed,radius));return true;}
   if(t==MotionEvent.ACTION_MOVE){boolean in=e.getX()>=0&&e.getY()>=0&&e.getX()<x.getWidth()&&e.getY()<x.getHeight();x.setBackground(bg(a,in?pressed:normal,radius));x.setScaleX(in?.985f:1f);x.setScaleY(in?.985f:1f);return true;}
   if(t==MotionEvent.ACTION_UP){boolean in=e.getX()>=0&&e.getY()>=0&&e.getX()<x.getWidth()&&e.getY()<x.getHeight();x.animate().scaleX(1f).scaleY(1f).setDuration(100).start();x.setBackground(bg(a,normal,radius));if(in){x.performClick();action.run();}return true;}
   if(t==MotionEvent.ACTION_CANCEL){x.animate().scaleX(1f).scaleY(1f).setDuration(100).start();x.setBackground(bg(a,normal,radius));return true;}
   return true;
  });
 }
}
