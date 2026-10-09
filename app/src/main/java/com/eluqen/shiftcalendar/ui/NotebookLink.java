package com.eluqen.shiftcalendar.ui;
import android.app.*;
import android.content.*;
import android.view.*;
import android.widget.*;
public class NotebookLink {
 public static void attach(Activity a,LinearLayout root){
  if(!"personal".equals(a.getIntent().getStringExtra("section")))return;
  ScrollView scroll=(ScrollView)root.getChildAt(2);
  LinearLayout list=(LinearLayout)scroll.getChildAt(0);
  Button b=new Button(a);b.setAllCaps(false);b.setText("دفترچه یادداشت — متن و دست‌نویس با قلم");
  list.addView(b);b.setOnClickListener(v->a.startActivity(new Intent(a,NotebookActivity.class)));
 }
}
