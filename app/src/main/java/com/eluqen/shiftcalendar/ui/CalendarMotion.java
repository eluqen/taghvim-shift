package com.eluqen.shiftcalendar.ui;
import android.app.Activity;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import java.lang.reflect.*;
import java.util.WeakHashMap;
public final class CalendarMotion {
 private static final WeakHashMap<Activity,FrameLayout> running=new WeakHashMap<>();
 private CalendarMotion(){}
 private static GridLayout grid(Activity a)throws Exception{
  Field f=a.getClass().getDeclaredField("calendarGrid");f.setAccessible(true);return (GridLayout)f.get(a);
 }
 private static Bitmap capture(View view){
  Bitmap bitmap=Bitmap.createBitmap(view.getWidth(),view.getHeight(),Bitmap.Config.ARGB_8888);
  view.draw(new Canvas(bitmap));return bitmap;
 }
 private static void change(Activity a,int offset)throws Exception{
  Method m=a.getClass().getDeclaredMethod("changeMonth",int.class);m.setAccessible(true);m.invoke(a,offset);
 }
 public static void navigate(Activity a,int delta){
  if(delta==0||running.containsKey(a))return;
  try{
   GridLayout calendar=grid(a);
   if(calendar==null||calendar.getWidth()<12||calendar.getHeight()<12){change(a,delta);return;}
   Bitmap previous=capture(calendar);
   change(a,delta);
   Bitmap next=capture(calendar);
   FrameLayout root=a.findViewById(android.R.id.content);
   if(root==null){previous.recycle();next.recycle();return;}
   int[] gridPos=new int[2],rootPos=new int[2];
   calendar.getLocationOnScreen(gridPos);root.getLocationOnScreen(rootPos);
   int w=calendar.getWidth(),h=calendar.getHeight();
   FrameLayout mask=new FrameLayout(a);mask.setClipChildren(true);mask.setClipToPadding(true);
   FrameLayout.LayoutParams location=new FrameLayout.LayoutParams(w,h,Gravity.TOP|Gravity.LEFT);
   location.leftMargin=gridPos[0]-rootPos[0];location.topMargin=gridPos[1]-rootPos[1];
   ImageView leaving=image(a,previous,w,h),entering=image(a,next,w,h);
   mask.addView(leaving);mask.addView(entering);
   int direction=delta>0?-1:1;
   entering.setTranslationX(-direction*w);
   calendar.setAlpha(0f);
   root.addView(mask,location);running.put(a,mask);
   long duration=255;
   leaving.animate().translationX(direction*w).setDuration(duration).setInterpolator(new DecelerateInterpolator()).start();
   entering.animate().translationX(0).setDuration(duration).setInterpolator(new DecelerateInterpolator())
    .setListener(new AnimatorListenerAdapter(){
     boolean completed;
     private void finish(){
      if(completed)return;completed=true;
      if(running.get(a)==mask){
       calendar.setAlpha(1f);root.removeView(mask);running.remove(a);
      }
      leaving.setImageDrawable(null);entering.setImageDrawable(null);
      previous.recycle();next.recycle();
     }
     @Override public void onAnimationEnd(Animator animation){finish();}
     @Override public void onAnimationCancel(Animator animation){finish();}
    }).start();
  }catch(Throwable error){
   Log.e("ELUQEN_CALENDAR","Month animation",error);
   try{GridLayout current=grid(a);if(current!=null)current.setAlpha(1f);}catch(Exception ignored){}
  }
 }
 private static ImageView image(Activity a,Bitmap bitmap,int w,int h){
  ImageView view=new ImageView(a);view.setImageBitmap(bitmap);view.setScaleType(ImageView.ScaleType.FIT_XY);
  view.setLayoutParams(new FrameLayout.LayoutParams(w,h,Gravity.TOP|Gravity.LEFT));return view;
 }
}
