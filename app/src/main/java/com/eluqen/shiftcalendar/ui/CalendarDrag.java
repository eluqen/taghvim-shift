package com.eluqen.shiftcalendar.ui;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.*;
import android.widget.*;
import android.animation.*;
import android.view.animation.DecelerateInterpolator;
import java.lang.reflect.*;
import java.util.WeakHashMap;
public final class CalendarDrag {
 private static final WeakHashMap<Activity,Session> sessions=new WeakHashMap<>();
 private CalendarDrag(){}
 private static final class Session {
  float x,y,dx;int direction,width,height;boolean active,started;
  Bitmap oldImage,newImage;FrameLayout layer,root;ImageView oldView,newView;GridLayout grid;
 }
 private static GridLayout grid(Activity a)throws Exception{
  Field f=a.getClass().getDeclaredField("calendarGrid");f.setAccessible(true);return (GridLayout)f.get(a);
 }
 private static Bitmap bitmap(View v){
  Bitmap b=Bitmap.createBitmap(v.getWidth(),v.getHeight(),Bitmap.Config.ARGB_8888);v.draw(new Canvas(b));return b;
 }
 private static void month(Activity a,int delta)throws Exception{
  Method m=a.getClass().getDeclaredMethod("changeMonth",int.class);m.setAccessible(true);m.invoke(a,delta);
 }
 private static Object field(Activity a,String name)throws Exception{
  Field f=a.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(a);
 }
 private static void field(Activity a,String name,Object value)throws Exception{
  Field f=a.getClass().getDeclaredField(name);f.setAccessible(true);f.set(a,value);
 }
 private static void render(Activity a)throws Exception{
  for(String name:new String[]{"renderMonth","renderSelected"}){
   Method m=a.getClass().getDeclaredMethod(name);m.setAccessible(true);m.invoke(a);
  }
 }
 private static ImageView image(Activity a,Bitmap b,int w,int h){
  ImageView v=new ImageView(a);v.setImageBitmap(b);v.setScaleType(ImageView.ScaleType.FIT_XY);
  v.setLayoutParams(new FrameLayout.LayoutParams(w,h,Gravity.LEFT|Gravity.TOP));return v;
 }
 private static void preview(Activity a,Session s,int direction)throws Exception{
  s.direction=direction;s.oldImage=bitmap(s.grid);
  Object chosen=field(a,"selected"),shown=field(a,"displayedMonth");
  Object[] desired=MonthStep.snapshot(a);
  try{month(a,direction<0?1:-1);s.newImage=bitmap(s.grid);}
  finally{
   field(a,"selected",chosen);field(a,"displayedMonth",shown);
   MonthStep.restore(a,desired);render(a);
  }
  s.root=a.findViewById(android.R.id.content);
  int[] gp=new int[2],rp=new int[2];s.grid.getLocationOnScreen(gp);s.root.getLocationOnScreen(rp);
  s.layer=new FrameLayout(a);s.layer.setClipChildren(true);
  FrameLayout.LayoutParams pos=new FrameLayout.LayoutParams(s.width,s.height,Gravity.TOP|Gravity.LEFT);
  pos.leftMargin=gp[0]-rp[0];pos.topMargin=gp[1]-rp[1];
  s.oldView=image(a,s.oldImage,s.width,s.height);s.newView=image(a,s.newImage,s.width,s.height);
  s.layer.addView(s.oldView);s.layer.addView(s.newView);
  s.newView.setTranslationX(-direction*s.width);
  s.grid.setAlpha(0f);s.root.addView(s.layer,pos);s.active=true;
 }
 private static void move(Session s,float dx){
  s.dx=Math.max(-s.width,Math.min(s.width,dx));
  s.oldView.setTranslationX(s.dx);
  s.newView.setTranslationX(s.dx-s.direction*s.width);
 }
 private static void cleanup(Activity a,Session s){
  if(s.layer!=null&&s.root!=null)s.root.removeView(s.layer);
  if(s.grid!=null)s.grid.setAlpha(1f);
  if(s.oldView!=null)s.oldView.setImageDrawable(null);
  if(s.newView!=null)s.newView.setImageDrawable(null);
  if(s.oldImage!=null&&!s.oldImage.isRecycled())s.oldImage.recycle();
  if(s.newImage!=null&&!s.newImage.isRecycled())s.newImage.recycle();
  sessions.remove(a);
 }
 private static void settle(Activity a,Session s,boolean commit){
  float target=commit?s.direction*s.width:0;
  float next=commit?0:-s.direction*s.width;
  long duration=Math.min(220,Math.max(90,(long)(180f*Math.abs(target-s.dx)/Math.max(1,s.width))));
  s.oldView.animate().translationX(target).setDuration(duration).setInterpolator(new DecelerateInterpolator()).start();
  s.newView.animate().translationX(next).setDuration(duration).setInterpolator(new DecelerateInterpolator())
   .withEndAction(()->{
    try{if(commit)month(a,s.direction<0?1:-1);}
    catch(Exception e){android.util.Log.e("ELUQEN_DRAG","Commit month failed",e);}
    cleanup(a,s);
   }).start();
 }
 public static boolean handle(Activity a,MotionEvent e){
  try{
   int act=e.getActionMasked();Session s=sessions.get(a);
   if(act==MotionEvent.ACTION_DOWN){
    if(s!=null&&s.active)return true;
    GridLayout grid=grid(a);int[] p=new int[2];grid.getLocationOnScreen(p);
    if(e.getRawX()<p[0]||e.getRawX()>p[0]+grid.getWidth()||e.getRawY()<p[1]||e.getRawY()>p[1]+grid.getHeight())return false;
    s=new Session();s.x=e.getRawX();s.y=e.getRawY();s.grid=grid;s.width=grid.getWidth();s.height=grid.getHeight();
    sessions.put(a,s);return false;
   }
   if(s==null)return false;
   if(act==MotionEvent.ACTION_POINTER_DOWN||act==MotionEvent.ACTION_CANCEL){
    if(s.active)settle(a,s,false);else sessions.remove(a);return s.active;
   }
   if(act==MotionEvent.ACTION_MOVE){
    float dx=e.getRawX()-s.x,dy=e.getRawY()-s.y;
    float slop=14*a.getResources().getDisplayMetrics().density;
    if(!s.active&&Math.abs(dx)>slop&&Math.abs(dx)>Math.abs(dy)*1.20f&&s.width>32){
     int direction=dx<0?-1:1;
     MotionEvent cancel=MotionEvent.obtain(e);cancel.setAction(MotionEvent.ACTION_CANCEL);
     s.grid.dispatchTouchEvent(cancel);cancel.recycle();
     preview(a,s,direction);
    }
    if(s.active){
     if(dx*s.direction<0)dx=0;
     move(s,dx);return true;
    }
    return false;
   }
   if(act==MotionEvent.ACTION_UP){
    if(!s.active){sessions.remove(a);return false;}
    float dx=e.getRawX()-s.x;
    boolean commit=Math.abs(dx)>s.width*.25f;
    settle(a,s,commit);return true;
   }
  }catch(Throwable error){
   android.util.Log.e("ELUQEN_DRAG","Drag error",error);
   Session s=sessions.get(a);if(s!=null)cleanup(a,s);
  }
  return false;
 }
}
