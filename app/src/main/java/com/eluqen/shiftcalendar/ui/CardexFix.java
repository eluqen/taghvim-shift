package com.eluqen.shiftcalendar.ui;
import android.app.Activity;
import android.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
public final class CardexFix {
 private CardexFix(){}
 private static int dp(Activity a,int n){return Math.round(a.getResources().getDisplayMetrics().density*n);}
 private static void collect(View v,List<EditText> inputs,List<CheckBox> ticks){
  if(v instanceof EditText)inputs.add((EditText)v);
  if(v instanceof CheckBox)ticks.add((CheckBox)v);
  if(v instanceof ViewGroup){
   ViewGroup g=(ViewGroup)v;
   for(int i=0;i<g.getChildCount();i++)collect(g.getChildAt(i),inputs,ticks);
  }
 }
 private static boolean hasDraft(List<EditText> fields,List<CheckBox> flags){
  for(EditText e:fields)if(e.getText()!=null&&e.getText().toString().trim().length()>0)return true;
  for(CheckBox c:flags)if(c.isChecked())return true;
  return false;
 }
 public static void defer(Activity a,AlertDialog dialog,Object date,Object callback){
  Button neutral=dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
  if(neutral!=null)neutral.post(()->install(a,dialog,date,callback));
 }
 public static void install(Activity a,AlertDialog dialog,Object date,Object callback){
  try{
   String dateKey=date.toString();
   Class<?> klass=Class.forName("com.eluqen.shiftcalendar.data.AppDatabase");
   Object db=klass.getMethod("get",android.content.Context.class).invoke(null,a);
   boolean persisted=klass.getMethod("cardex",String.class).invoke(db,dateKey)!=null;
   List<EditText> fields=new ArrayList<>();List<CheckBox> flags=new ArrayList<>();
   collect(dialog.getWindow().getDecorView(),fields,flags);
   Button delete=dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
   if(delete==null)return;
   EditText noteTime=null;
   for(EditText field:fields){
    CharSequence hint=field.getHint();
    if(hint!=null&&hint.toString().contains("ساعت یادداشت"))noteTime=field;
   }
   if(noteTime!=null)installClear(a,noteTime);
   Runnable update=()->{
    boolean enabled=persisted||hasDraft(fields,flags);
    delete.setEnabled(enabled);delete.setAlpha(enabled?1f:.36f);
   };
   for(EditText edit:fields)edit.addTextChangedListener(new TextWatcher(){
    public void beforeTextChanged(CharSequence s,int start,int count,int after){}
    public void onTextChanged(CharSequence s,int start,int before,int count){update.run();}
    public void afterTextChanged(Editable e){}
   });
   for(CheckBox check:flags)check.setOnTouchListener((v,e)->{
    if(e.getActionMasked()==android.view.MotionEvent.ACTION_UP)v.post(update);
    return false;
   });
   update.run();
   delete.setOnClickListener(v->{
    if(!persisted&&!hasDraft(fields,flags))return;
    String question=persisted?"اطلاعات ذخیره‌شده کارکرد این روز حذف شود؟":"اطلاعات واردشده و ذخیره‌نشده پاک شوند؟";
    new AlertDialog.Builder(a).setTitle(persisted?"حذف ثبت کارکرد":"پاک کردن فرم")
     .setMessage(question).setNegativeButton("انصراف",null)
     .setPositiveButton(persisted?"حذف":"پاک کردن",(which,clicked)->{
      if(!persisted){
       for(EditText field:fields)field.setText("");
       for(CheckBox checked:flags)checked.setChecked(false);
       update.run();return;
      }
      try{
       klass.getMethod("deleteCardex",String.class).invoke(db,dateKey);
       dialog.dismiss();
       if(callback!=null)callback.getClass().getMethod("onSaved").invoke(callback);
      }catch(Exception failure){
       android.util.Log.e("ELUQEN_CARDEX","Delete failed",failure);
       Toast.makeText(a,"حذف اطلاعات انجام نشد",Toast.LENGTH_LONG).show();
      }
     }).show();
   });
  }catch(Exception error){android.util.Log.e("ELUQEN_CARDEX","Editor polish",error);}
 }
 private static void installClear(Activity a,EditText time){
  if(!(time.getParent() instanceof LinearLayout))return;
  LinearLayout parent=(LinearLayout)time.getParent();Button clear=new Button(a);
  clear.setText("✕  پاک‌کردن ساعت یادداشت");clear.setAllCaps(false);
  clear.setTextSize(12);clear.setTextColor(0xff995066);
  clear.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xffedf2f7));
  int index=parent.indexOfChild(time);
  LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,dp(a,38));
  params.setMargins(0,0,0,dp(a,4));parent.addView(clear,index+1,params);
  Runnable refresh=()->{
   boolean filled=time.getText()!=null&&time.getText().length()>0;
   clear.setEnabled(filled);clear.setVisibility(filled?View.VISIBLE:View.GONE);
  };
  time.addTextChangedListener(new TextWatcher(){
   public void beforeTextChanged(CharSequence s,int start,int count,int after){}
   public void onTextChanged(CharSequence s,int start,int before,int count){refresh.run();}
   public void afterTextChanged(Editable e){}
  });
  clear.setOnClickListener(v->time.setText(""));refresh.run();
 }
}
