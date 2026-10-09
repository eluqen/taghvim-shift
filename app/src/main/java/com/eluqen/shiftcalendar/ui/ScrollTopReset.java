package com.eluqen.shiftcalendar.ui;
import android.app.Activity;
import android.view.*;
import android.widget.ScrollView;
public final class ScrollTopReset{
 private ScrollTopReset(){}
 private static ScrollView find(View v){
  if(v instanceof ScrollView)return (ScrollView)v;
  if(v instanceof ViewGroup){
   ViewGroup g=(ViewGroup)v;
   for(int i=0;i<g.getChildCount();i++){ScrollView s=find(g.getChildAt(i));if(s!=null)return s;}
  }
  return null;
 }
 public static void top(Activity a){
  final ScrollView s=find(a.findViewById(android.R.id.content));
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
