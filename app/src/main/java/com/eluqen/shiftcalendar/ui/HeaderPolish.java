package com.eluqen.shiftcalendar.ui;
import android.app.Activity;
import android.view.View;
import android.widget.*;
public final class HeaderPolish {
 private HeaderPolish(){}
 private static int dp(Activity a,int n){return Math.round(n*a.getResources().getDisplayMetrics().density);}
 public static void trim(LinearLayout bar){
  if(bar==null)return;Activity a=(Activity)bar.getContext();
  bar.setPadding(dp(a,5),0,dp(a,5),0);
  bar.setBackgroundColor(Ui.SURFACE);
  if(bar.getChildCount()>0&&bar.getChildAt(0) instanceof TextView){
   TextView title=(TextView)bar.getChildAt(0);title.setVisibility(View.GONE);
  }
  for(int i=1;i<bar.getChildCount();i++)if(bar.getChildAt(i) instanceof Button){
   Button back=(Button)bar.getChildAt(i);back.setText("‹");back.setTextColor(Ui.PRIMARY);
   back.setTextSize(25);back.setMinHeight(0);
   back.setBackgroundColor(0);
   back.setLayoutParams(new LinearLayout.LayoutParams(dp(a,42),dp(a,40)));
  }
  // Remove the entire unused toolbar slot, not only the title text.
  bar.setVisibility(View.GONE);
 }
}
