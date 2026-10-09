package com.eluqen.shiftcalendar.ui;
import android.app.*;
import android.graphics.Typeface;
import android.view.*;
import android.widget.*;
import java.util.*;
public final class CardexChoice {
 private CardexChoice(){}
 private static final String[] NAMES={"استراحت","مرخصی","تعویض شیفت","اضافه‌کاری"};
 private static int dp(Activity a,int n){return Math.round(a.getResources().getDisplayMetrics().density*n);}
 private static CheckBox find(View v,String name){
  if(v instanceof CheckBox && name.equals(((CheckBox)v).getText().toString()))return (CheckBox)v;
  if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){CheckBox x=find(g.getChildAt(i),name);if(x!=null)return x;}}
  return null;
 }
 private static ArrayList<EditText> fields(CheckBox box){
  ArrayList<EditText> out=new ArrayList<>();
  if(!(box.getParent() instanceof ViewGroup))return out;
  ViewGroup p=(ViewGroup)box.getParent();int at=p.indexOfChild(box);
  String name=box.getText().toString();
  if("تعویض شیفت".equals(name)){
   for(int i=at+1;i<p.getChildCount()&&i<=at+2;i++)collectEdits(p.getChildAt(i),out);
  }else if(at+1<p.getChildCount())collectEdits(p.getChildAt(at+1),out);
  return out;
 }
 private static void collectEdits(View v,List<EditText> out){
  if(v instanceof EditText){out.add((EditText)v);return;}
  if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)collectEdits(g.getChildAt(i),out);}
 }
 private static void enabled(CheckBox box,boolean on){
  for(EditText e:fields(box)){e.setEnabled(on);e.setAlpha(on?1f:.45f);}
 }
 private static void clear(CheckBox box){
  for(EditText e:fields(box))e.setText("");
 }
 public static void install(Activity a,AlertDialog dialog){
  View root=dialog.getWindow()==null?null:dialog.getWindow().getDecorView();if(root==null)return;
  final ArrayList<CheckBox> boxes=new ArrayList<>();
  for(String name:NAMES){CheckBox b=find(root,name);if(b!=null)boxes.add(b);}
  if(boxes.size()!=4)return;
  // Visually use radio controls while retaining the useful ability to tap the
  // selected item again to return to "no category" / note-only.
  for(CheckBox b:boxes)try{b.setButtonDrawable(android.R.drawable.btn_radio);}catch(Exception ignored){}
  CheckBox first=boxes.get(0);
  if(first.getParent() instanceof LinearLayout){
   LinearLayout parent=(LinearLayout)first.getParent();int at=parent.indexOfChild(first);
   TextView hint=new TextView(a);hint.setText("نوع ثبت (اختیاری — فقط یک مورد قابل انتخاب است)");
   hint.setTextSize(13);hint.setTextColor(0xff53677f);hint.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
   hint.setGravity(Gravity.RIGHT);hint.setPadding(dp(a,2),dp(a,7),dp(a,2),dp(a,3));parent.addView(hint,at);
  }
  // Repair old multi-category records only in the editor draft; the database is
  // untouched unless the user explicitly presses Save.
  int keeper=-1;for(int i=0;i<boxes.size();i++)if(boxes.get(i).isChecked()){if(keeper<0)keeper=i;else{boxes.get(i).setChecked(false);clear(boxes.get(i));}}
  for(int i=0;i<boxes.size();i++)enabled(boxes.get(i),boxes.get(i).isChecked());
  final boolean[] busy={false};
  for(CheckBox b:boxes)b.setOnCheckedChangeListener((button,on)->{
   if(busy[0])return;busy[0]=true;
   if(on){
    for(CheckBox other:boxes)if(other!=button){if(other.isChecked())other.setChecked(false);clear(other);enabled(other,false);}
    enabled((CheckBox)button,true);
   }else{
    clear((CheckBox)button);enabled((CheckBox)button,false);
   }
   busy[0]=false;
  });
 }
}
