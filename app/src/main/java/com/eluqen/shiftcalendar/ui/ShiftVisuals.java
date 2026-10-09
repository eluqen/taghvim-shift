package com.eluqen.shiftcalendar.ui;
import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.*;
public final class ShiftVisuals {
 public interface Selection{void selected(int color);}
 public static final int[] COLORS={0xffd8942f,0xffc9537a,0xff5367c7,0xff177e79,0xff7e57c2,0xff00897b,0xffe65e4f,0xff3f51b5,0xff2e7d32,0xff607080,0xff795548,0xff00796b,0xffd8b4fe,0xff48a6ce,0xfff59a55,0xff444b61};
 private ShiftVisuals(){}
 private static int dp(Activity a,int x){return (int)(x*a.getResources().getDisplayMetrics().density+.5f);}
 public static int contrast(int c){return (Color.red(c)*.2126+Color.green(c)*.7152+Color.blue(c)*.0722)/255.>.56?0xff213149:Color.WHITE;}
 private static GradientDrawable chip(Activity a,int color,boolean selected){
  GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(a,13));
  g.setStroke(dp(a,selected?4:1),selected?0xff172d50:0xffccd7e5);return g;
 }
 public static void selection(Button b,int color){
  b.setText("●  رنگ انتخاب‌شده");b.setBackground(chip((Activity)b.getContext(),color,true));
  b.setTextColor(contrast(color));b.setBackgroundTintList(null);
 }
 public static void choose(Activity a,int[] colors,int initial,Selection callback){
  int[] chosen={initial};LinearLayout main=new LinearLayout(a);main.setOrientation(LinearLayout.VERTICAL);
  main.setPadding(dp(a,12),dp(a,9),dp(a,12),dp(a,7));
  TextView status=new TextView(a);status.setText("رنگ موردنظر را لمس کنید؛ سپس «تأیید» را بزنید.");
  status.setTextColor(0xff254164);status.setGravity(Gravity.CENTER);status.setTextSize(14);
  main.addView(status);
  main.setGravity(Gravity.CENTER_HORIZONTAL);
  GridLayout grid=new GridLayout(a);grid.setColumnCount(4);main.addView(grid,new LinearLayout.LayoutParams(-1,-2));
  TextView[] cells=new TextView[colors.length];
  Runnable paint=()->{
   for(int i=0;i<colors.length;i++){
    boolean active=colors[i]==chosen[0];cells[i].setText(active?"✓":"");
    cells[i].setTextColor(contrast(colors[i]));cells[i].setTextSize(21);
    cells[i].setGravity(Gravity.CENTER);cells[i].setBackground(chip(a,colors[i],active));
   }
  };
  for(int i=0;i<colors.length;i++){
   int n=i;TextView cell=new TextView(a);cells[i]=cell;
   GridLayout.LayoutParams lp=new GridLayout.LayoutParams();
   lp.width=dp(a,58);lp.height=dp(a,53);lp.columnSpec=GridLayout.spec(i%4);
   lp.rowSpec=GridLayout.spec(i/4);lp.setMargins(dp(a,3),dp(a,3),dp(a,3),dp(a,3));
   grid.addView(cell,lp);cell.setOnClickListener(v->{chosen[0]=colors[n];paint.run();status.setText("رنگ انتخاب شد ✓");});
  }
  paint.run();
  new AlertDialog.Builder(a).setTitle("انتخاب تصویری رنگ").setView(main)
   .setNegativeButton("انصراف",null).setPositiveButton("تأیید",(dialog,which)->callback.selected(chosen[0])).show();
 }
 public static void open(Activity a,int[] colors,int initial,Object listener){
  choose(a,colors,initial,color->{
   try{
    java.lang.reflect.Method m=listener.getClass().getDeclaredMethod("selected",int.class);
    m.setAccessible(true);m.invoke(listener,color);
   }catch(Exception e){android.util.Log.e("ELUQEN_Color","Callback failed",e);}
  });
 }
}
