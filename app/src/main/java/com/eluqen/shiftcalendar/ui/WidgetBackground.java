package com.eluqen.shiftcalendar.ui;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.os.Bundle;
import android.widget.RemoteViews;

public final class WidgetBackground {
 public static final String FILE="eluqen_widget_studio";
 public static final String KEY_TRANSPARENT="bg_transparent";
 public static final String KEY_COLOR="bg_custom_color";
 private static final int[] PALETTE={0xff203a63,0xff117477,0xff175a73,0xff513986,0xff252b39,0xfff2f7fb,0xfffcecca,0xff86442c,0xff155b47,0xffdcf0fa};
 private static final int[] MANUAL={0xff203a63,0xff117477,0xff175a73,0xff513986,0xff252b39,0xfff2f7fb,0xfffcecca,0xff86442c,0xff155b47,0xffdcf0fa,0xffffffff,0xff000000,0xffffb347,0xffd95471,0xff7bd5e6,0xffa7b9dc};
 private WidgetBackground(){}
 private static int id(Context c,String name){return c.getResources().getIdentifier(name,"id",c.getPackageName());}
 public static int currentColor(Context c){
  SharedPreferences p=c.getSharedPreferences(FILE,0);
  if(p.contains(KEY_COLOR))return p.getInt(KEY_COLOR,PALETTE[0]);
  int palette=Math.max(0,Math.min(PALETTE.length-1,p.getInt("palette",0)));
  if(p.contains("bg_index")){
   int index=Math.max(0,Math.min(MANUAL.length-1,p.getInt("bg_index",palette)));
   return MANUAL[index];
  }
  return PALETTE[palette];
 }
 public static boolean transparent(Context c){return c.getSharedPreferences(FILE,0).getBoolean(KEY_TRANSPARENT,false);}
 public static void apply(Context c,RemoteViews rv,int appWidgetId){
  try{
   int root=id(c,"widget_root"),content=id(c,"widget_content"),layer=id(c,"widget_background_layer");
   if(root!=0){rv.setInt(root,"setBackgroundResource",0);rv.setInt(root,"setBackgroundColor",Color.TRANSPARENT);}
   if(content!=0){rv.setInt(content,"setBackgroundResource",0);rv.setInt(content,"setBackgroundColor",Color.TRANSPARENT);}
   if(layer==0)return;

   Bundle o=AppWidgetManager.getInstance(c).getAppWidgetOptions(appWidgetId);
   int w=o.getInt("appWidgetMinWidth",180),h=o.getInt("appWidgetMinHeight",92);
   float density=c.getResources().getDisplayMetrics().density;
   int pxW=Math.max(160,Math.min(480,Math.round(w*density)));
   int pxH=Math.max(70,Math.min(280,Math.round(h*density)));
   Bitmap bm=Bitmap.createBitmap(pxW,pxH,Bitmap.Config.ARGB_8888);
   Canvas canvas=new Canvas(bm);
   canvas.drawColor(Color.TRANSPARENT,PorterDuff.Mode.CLEAR);
   if(!transparent(c)){
    Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(currentColor(c));
    float r=Math.max(12f*density,Math.min(24f*density,Math.min(pxW,pxH)*0.18f));
    canvas.drawRoundRect(0,0,pxW,pxH,r,r,paint);
   }
   rv.setImageViewBitmap(layer,bm);
  }catch(Exception e){android.util.Log.e("ELUQEN_WIDGET","Background apply",e);}
 }
}
