package com.eluqen.shiftcalendar.ui;
import android.app.Activity;
import android.view.*;
import android.widget.GridLayout;
import java.lang.reflect.*;
import java.util.WeakHashMap;
public final class CalendarGestures {
 private CalendarGestures(){}
 private static final class Touch {float x,y;boolean start;int pointer;}
 private static final WeakHashMap<Activity,Touch> touches=new WeakHashMap<>();
 private static boolean inside(GridLayout grid,MotionEvent event){
  int[] pos=new int[2];grid.getLocationOnScreen(pos);
  return event.getRawX()>=pos[0]&&event.getRawX()<=pos[0]+grid.getWidth()&&event.getRawY()>=pos[1]&&event.getRawY()<=pos[1]+grid.getHeight();
 }
 public static boolean gesture(Activity a,MotionEvent e){
  try{
   Field f=a.getClass().getDeclaredField("calendarGrid");f.setAccessible(true);
   GridLayout grid=(GridLayout)f.get(a);if(grid==null)return false;
   Touch t=touches.get(a);if(t==null){t=new Touch();touches.put(a,t);}
   int action=e.getActionMasked();
   if(action==MotionEvent.ACTION_DOWN){
    t.start=inside(grid,e);t.x=e.getRawX();t.y=e.getRawY();t.pointer=e.getPointerId(0);return false;
   }
   if(action==MotionEvent.ACTION_CANCEL||action==MotionEvent.ACTION_POINTER_DOWN){t.start=false;return false;}
   if(action==MotionEvent.ACTION_UP&&t.start){
    t.start=false;float dx=e.getRawX()-t.x,dy=e.getRawY()-t.y;
    float density=a.getResources().getDisplayMetrics().density;
    if(Math.abs(dx)>70*density&&Math.abs(dx)>Math.abs(dy)*1.35f){
     MotionEvent cancel=MotionEvent.obtain(e);cancel.setAction(MotionEvent.ACTION_CANCEL);
     grid.dispatchTouchEvent(cancel);cancel.recycle();
     CalendarMotion.navigate(a,dx<0?1:-1);return true;
    }
   }
  }catch(Exception ex){android.util.Log.e("ELUQEN_CALENDAR","Swipe failed",ex);}
  return false;
 }
 public static Object selected(Activity a){
  try{Field f=a.getClass().getDeclaredField("selected");f.setAccessible(true);return f.get(a);}
  catch(Exception e){return null;}
 }
}
