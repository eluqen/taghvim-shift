package com.eluqen.shiftcalendar.ui;
import android.app.Activity;
import android.view.*;
import android.widget.*;
import java.lang.reflect.Field;

public final class CardexScroll {
 private static final String TAG="eluqen_cardex_full_scroll";
 private CardexScroll(){}
 private static int dp(Activity a,int n){return Math.round(n*a.getResources().getDisplayMetrics().density);}
 private static Object field(Activity a,String name)throws Exception{
  Field f=a.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(a);
 }
 public static void apply(Activity a){
  try{
   View selected=(View)field(a,"selectedButton");
   TextView recordsTitle=(TextView)field(a,"recordsTitle");
   LinearLayout records=(LinearLayout)field(a,"records");
   if(selected==null||recordsTitle==null||records==null)return;

   ViewParent cp=selected.getParent();
   if(!(cp instanceof LinearLayout))return;
   LinearLayout card=(LinearLayout)cp;
   ViewParent rpCard=card.getParent();
   if(!(rpCard instanceof LinearLayout))return;
   LinearLayout root=(LinearLayout)rpCard;

   for(int i=0;i<root.getChildCount();i++)if(TAG.equals(root.getChildAt(i).getTag()))return;

   ViewParent recordsParent=records.getParent();
   if(!(recordsParent instanceof ScrollView))return;
   ScrollView oldScroll=(ScrollView)recordsParent;

   LinearLayout.LayoutParams cardLp=card.getLayoutParams() instanceof LinearLayout.LayoutParams
    ?new LinearLayout.LayoutParams((LinearLayout.LayoutParams)card.getLayoutParams())
    :new LinearLayout.LayoutParams(-1,-2);
   LinearLayout.LayoutParams titleLp=recordsTitle.getLayoutParams() instanceof LinearLayout.LayoutParams
    ?new LinearLayout.LayoutParams((LinearLayout.LayoutParams)recordsTitle.getLayoutParams())
    :new LinearLayout.LayoutParams(-1,-2);

   oldScroll.removeView(records);
   root.removeView(oldScroll);
   root.removeView(recordsTitle);
   root.removeView(card);

   ScrollView all=new ScrollView(a);
   all.setTag(TAG);
   all.setFillViewport(false);
   all.setClipToPadding(false);
   all.setVerticalScrollBarEnabled(true);
   all.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
   all.setNestedScrollingEnabled(true);

   LinearLayout body=new LinearLayout(a);
   body.setOrientation(LinearLayout.VERTICAL);
   body.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
   body.setPadding(0,0,0,dp(a,24));
   all.addView(body,new ScrollView.LayoutParams(-1,-2));

   body.addView(card,cardLp);
   body.addView(recordsTitle,titleLp);
   body.addView(records,new LinearLayout.LayoutParams(-1,-2));

   root.addView(all,new LinearLayout.LayoutParams(-1,0,1f));
   root.requestLayout();
   all.post(()->{body.requestLayout();all.requestLayout();});
  }catch(Exception e){android.util.Log.e("ELUQEN_CARDEX","Full scroll",e);}
 }
}
