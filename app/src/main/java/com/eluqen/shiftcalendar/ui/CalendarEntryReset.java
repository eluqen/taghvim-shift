package com.eluqen.shiftcalendar.ui;
import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

public final class CalendarEntryReset {
 private CalendarEntryReset(){}

 private static ScrollView find(View v){
  if(v instanceof ScrollView)return (ScrollView)v;
  if(v instanceof ViewGroup){
   ViewGroup g=(ViewGroup)v;
   for(int i=0;i<g.getChildCount();i++){
    ScrollView s=find(g.getChildAt(i));
    if(s!=null)return s;
   }
  }
  return null;
 }

 public static void top(Activity a){
  View root=a.findViewById(android.R.id.content);
  final ScrollView s=find(root);
  if(s==null)return;
  s.scrollTo(0,0);
  final android.view.ViewTreeObserver.OnPreDrawListener listener=
    new android.view.ViewTreeObserver.OnPreDrawListener(){
     @Override public boolean onPreDraw(){
      android.view.ViewTreeObserver observer=s.getViewTreeObserver();
      if(observer.isAlive())observer.removeOnPreDrawListener(this);
      s.scrollTo(0,0);s.fullScroll(View.FOCUS_UP);
      return true;
     }
    };
  s.getViewTreeObserver().addOnPreDrawListener(listener);
 }
}
