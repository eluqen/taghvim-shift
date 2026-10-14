package com.eluqen.shiftcalendar.ui;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;

public final class HubSections {
 private HubSections(){}
 private static void open(Activity a,String name){
  Intent child=new Intent().setClassName(a,"com.eluqen.shiftcalendar.ui."+name);
  child.putExtra(BackDock.EXTRA_FEATURES_CHILD,true);
  a.startActivity(child);
 }
 private static void planner(Activity a,String mode){
  Intent i=new Intent().setClassName(a,"com.eluqen.shiftcalendar.ui.PlannerActivity");
  i.putExtra("mode",mode);
  i.putExtra(BackDock.EXTRA_FEATURES_CHILD,true);
  a.startActivity(i);
 }
 public static void hub(Activity a,LinearLayout root){
  if(root.getChildCount()>0 && !(root.getChildAt(0) instanceof ScrollView))root.removeViewAt(0);
  ScrollView scroll=null;
  for(int i=0;i<root.getChildCount();i++)if(root.getChildAt(i) instanceof ScrollView){scroll=(ScrollView)root.getChildAt(i);break;}
  if(scroll==null || scroll.getChildCount()==0 || !(scroll.getChildAt(0) instanceof LinearLayout))return;
  LinearLayout body=(LinearLayout)scroll.getChildAt(0);body.removeAllViews();
  body.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
  body.setPadding(SettingsFeaturesUi.dp(a,16),0,SettingsFeaturesUi.dp(a,16),SettingsFeaturesUi.dp(a,28));
  LinearLayout box=SettingsFeaturesUi.section(a,body,"برنامه‌ریزی شخصی");
  SettingsFeaturesUi.row(a,box,"کارهای روزانه","ثبت کارها و علامت‌زدن موارد انجام‌شده",0,()->open(a,"TodoListActivity"));
  SettingsFeaturesUi.divider(a,box);
  SettingsFeaturesUi.row(a,box,"یادآوری‌ها","موعدهای شخصی با تاریخ، ساعت و صدای دلخواه",1,()->planner(a,"event"));
  SettingsFeaturesUi.divider(a,box);
  SettingsFeaturesUi.row(a,box,"دفترچه یادداشت","نوشتن، دست‌نویسی و مدیریت یادداشت‌ها",2,()->open(a,"NotebookActivity"));
 }
}
