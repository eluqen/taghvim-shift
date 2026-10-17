package com.eluqen.shiftcalendar.ui;
import android.app.*;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import org.json.*;

public final class TodoListActivity extends Activity {
 private static final String PREF="eluqen_todo_v2", KEY_ITEMS="items", KEY_MIGRATED="legacy_migrated";
 private static final String K_NORMAL_SIZE="normal_size",K_DONE_SIZE="done_size",K_NORMAL_COLOR="normal_color",K_DONE_COLOR="done_color",K_STRIKE_COLOR="strike_color",K_BOLD_DONE="bold_done",K_BOLD_NORMAL="bold_normal",K_STRIKE="strike";
 private LinearLayout list;
 private int dp(float v){return Math.round(v*getResources().getDisplayMetrics().density);}
 private SharedPreferences prefs(){return getSharedPreferences(PREF,0);}
 private GradientDrawable bg(int color,float radius,int stroke){
  GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));if(stroke!=0)g.setStroke(dp(1),stroke);return g;
 }
 private Button softButton(String s,int color,int textColor){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(14);b.setTextColor(textColor);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(bg(color,14,0));return b;}
 private TextView text(String s,float size,int color,boolean bold){
  TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);v.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
  if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;
 }
 private JSONArray load(){
  try{return new JSONArray(prefs().getString(KEY_ITEMS,"[]"));}catch(Exception e){return new JSONArray();}
 }
 private void save(JSONArray a){prefs().edit().putString(KEY_ITEMS,a.toString()).apply();}
 private long newId(){return System.currentTimeMillis()*1000L+(long)(Math.random()*999);}
 private int normalSize(){return prefs().getInt(K_NORMAL_SIZE,16);}
 private int doneSize(){return prefs().getInt(K_DONE_SIZE,17);}
 private int normalColor(){return prefs().getInt(K_NORMAL_COLOR,Ui.TEXT);}
 private int doneColor(){return prefs().getInt(K_DONE_COLOR,0xff6b7d8f);}
 private int strikeColor(){return prefs().getInt(K_STRIKE_COLOR,Ui.SECONDARY);}
 private boolean boldDone(){return prefs().getBoolean(K_BOLD_DONE,true);}
 private boolean boldNormal(){return prefs().getBoolean(K_BOLD_NORMAL,false);}
 private boolean strikeDone(){return prefs().getBoolean(K_STRIKE,true);}

 @Override protected void onCreate(Bundle state){
  super.onCreate(state);AppTheme.apply(this);
  LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);root.setBackgroundColor(Ui.SURFACE);
  LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.VERTICAL);hero.setPadding(dp(16),dp(14),dp(16),dp(12));hero.setBackground(bg(Ui.SURFACE_ALT,18,0));
  TextView title=text("کارهای روزانه",22,Ui.PRIMARY,true);hero.addView(title);
  TextView subtitle=text("کارهایی که باید انجام شوند را ثبت کن و با یک تیک ببندشان.",13,Ui.MUTED,false);subtitle.setPadding(0,dp(2),0,dp(8));hero.addView(subtitle);
  LinearLayout controls=new LinearLayout(this);controls.setOrientation(LinearLayout.HORIZONTAL);controls.setGravity(Gravity.CENTER_VERTICAL);controls.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
  Button add=softButton("＋  کار جدید",Ui.SECONDARY,Color.WHITE);add.setContentDescription("افزودن کار روزانه");LiveTouch.bind(this,add,Ui.SECONDARY,Ui.PRIMARY,14,()->editTask(-1));
  Button gear=softButton("⚙  ظاهر",Ui.CARD,Ui.TEXT);gear.setContentDescription("تنظیم ظاهر کارهای روزانه");LiveTouch.bind(this,gear,Ui.CARD,Ui.SURFACE_ALT,14,()->settings());
  TextView stats=text("",12.5f,Ui.SECONDARY,true);stats.setTag("stats");stats.setGravity(Gravity.CENTER_VERTICAL|Gravity.RIGHT);
  controls.addView(stats,new LinearLayout.LayoutParams(0,dp(50),1));controls.addView(gear,new LinearLayout.LayoutParams(dp(98),dp(48)));controls.addView(add,new LinearLayout.LayoutParams(dp(132),dp(48)));hero.addView(controls);
  LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2);hp.setMargins(dp(12),dp(10),dp(12),dp(6));root.addView(hero,hp);
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(true);scroll.setScrollbarFadingEnabled(false);
  list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);list.setPadding(dp(12),dp(6),dp(12),dp(28));
  scroll.addView(list);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  setContentView(root);BackDock.install(this,root);migrateLegacyOnce();render();
 }
 @Override protected void onResume(){super.onResume();BackDock.refresh(this);}
 @Override public void onBackPressed(){if(!BackDock.handleBack(this))super.onBackPressed();}

 private void migrateLegacyOnce(){
  if(prefs().getBoolean(KEY_MIGRATED,false))return;
  JSONArray current=load();
  if(current.length()==0){
   try{
    JSONArray old=PersonalStore.all(this);
    for(int i=0;i<old.length();i++){
     JSONObject src=old.optJSONObject(i);if(src==null||!"todo".equals(src.optString("kind","todo")))continue;
     String value=src.optString("title","").trim();if(value.isEmpty())continue;
     JSONObject dst=new JSONObject();dst.put("id",newId()+i);dst.put("text",value);dst.put("done",src.optBoolean("done",false));current.put(dst);
    }
    if(current.length()>0)save(current);
   }catch(Exception ignored){}
  }
  prefs().edit().putBoolean(KEY_MIGRATED,true).apply();
 }
 private void render(){
  list.removeAllViews();JSONArray a=load();int done=0;for(int i=0;i<a.length();i++){JSONObject x=a.optJSONObject(i);if(x!=null&&x.optBoolean("done",false))done++;}
  View root=findViewById(android.R.id.content);TextView stats=findTaggedText(root,"stats");if(stats!=null)stats.setText("همه: "+a.length()+"   •   انجام‌شده: "+done+"   •   باقی‌مانده: "+(a.length()-done));
  if(a.length()==0){LinearLayout empty=new LinearLayout(this);empty.setOrientation(LinearLayout.VERTICAL);empty.setGravity(Gravity.CENTER);empty.setPadding(dp(18),dp(30),dp(18),dp(30));empty.setBackground(bg(0xffffffff,18,Ui.BORDER));
   TextView icon=text("✓",30,Ui.SECONDARY,true);icon.setGravity(Gravity.CENTER);empty.addView(icon,new LinearLayout.LayoutParams(-1,dp(50)));TextView e=text("هنوز کاری ثبت نشده",17,Ui.TEXT,true);e.setGravity(Gravity.CENTER);empty.addView(e);TextView d=text("با دکمه «کار جدید» اولین مورد را اضافه کن.",13,0xff718194,false);d.setGravity(Gravity.CENTER);d.setPadding(0,dp(4),0,0);empty.addView(d);list.addView(empty,new LinearLayout.LayoutParams(-1,-2));return;}
  for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null)row(a,o,i);}
 }
 private TextView findTaggedText(View v,String tag){if(v==null)return null;if(tag.equals(v.getTag())&&v instanceof TextView)return (TextView)v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){TextView r=findTaggedText(g.getChildAt(i),tag);if(r!=null)return r;}}return null;}
 private void row(JSONArray all,JSONObject o,int index){
  boolean done=o.optBoolean("done",false);long id=o.optLong("id");
  LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.HORIZONTAL);card.setGravity(Gravity.CENTER_VERTICAL);
  card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);card.setPadding(dp(10),dp(10),dp(10),dp(10));card.setBackground(bg(done?0xffeef5f4:0xffffffff,16,done?0xffcfe5e2:0xffdfe7ef));card.setElevation(dp(done?1:3));

  TextView num=text(String.valueOf(index+1),13,0xff7d8b99,true);num.setGravity(Gravity.CENTER);
  card.addView(num,new LinearLayout.LayoutParams(dp(34),dp(48)));

  FancyCheck check=new FancyCheck(this);check.setChecked(done);
  card.addView(check,new LinearLayout.LayoutParams(dp(58),dp(58)));

  TaskText title=new TaskText(this);title.setText(o.optString("text",""));title.setTextSize(done?doneSize():normalSize());
  title.setTextColor(done?doneColor():normalColor());title.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
  title.setTypeface(Typeface.DEFAULT,(done?boldDone():boldNormal())?Typeface.BOLD:Typeface.NORMAL);
  // The task editor retains the complete text; only this list preview is bounded.
  title.setPadding(dp(6),dp(4),dp(6),dp(4));
  title.setMaxLines(2);
  title.setEllipsize(android.text.TextUtils.TruncateAt.END);
  title.setTextDirection(View.TEXT_DIRECTION_RTL);
  title.style(done,strikeColor());
  title.setOnClickListener(v->editTaskById(id));
  card.addView(title,new LinearLayout.LayoutParams(0,-2,1));

  Button trash=softButton("✕",0xfffff1f1,0xffb33a44);trash.setContentDescription("حذف کار");LiveTouch.bind(this,trash,0xfffff1f1,0xffffdddd,12,()->deleteTask(id,o.optString("text","")));
  card.addView(trash,new LinearLayout.LayoutParams(dp(58),dp(50)));

  check.setOnCheckedChangeListener(on->setDone(id,on));
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,dp(9));list.addView(card,lp);
 }
 private int find(JSONArray a,long id){
  for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&o.optLong("id")==id)return i;}return -1;
 }
 private void setDone(long id,boolean done){
  JSONArray a=load();int i=find(a,id);if(i<0)return;
  try{a.getJSONObject(i).put("done",done);save(a);render();}catch(Exception ignored){}
 }
 private void deleteTask(long id,String label){
  new AlertDialog.Builder(this).setCustomTitle(Ui.rtlDialogTitle(this,"حذف کار")).setMessage("«"+label+"» حذف شود؟")
   .setNegativeButton("انصراف",null).setPositiveButton("حذف",(d,w)->{
    JSONArray a=load(),n=new JSONArray();for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&o.optLong("id")!=id)n.put(o);}
    save(n);render();
   }).show();
 }
 private void editTaskById(long id){
  JSONArray a=load();int i=find(a,id);if(i>=0)editTask(i);
 }
 private void editTask(int index){
  JSONArray a=load();JSONObject old=index>=0?a.optJSONObject(index):null;
  LinearLayout shell=new LinearLayout(this);shell.setOrientation(LinearLayout.VERTICAL);shell.setPadding(dp(18),dp(12),dp(18),dp(6));shell.setBackground(bg(0xfff8fbfc,18,0));
  TextView badge=text(old==null?"＋":"✎",28,Ui.SECONDARY,true);badge.setGravity(Gravity.CENTER);shell.addView(badge,new LinearLayout.LayoutParams(-1,dp(46)));
  TextView head=text(old==null?"کار جدید":"ویرایش کار",20,Ui.TEXT,true);head.setGravity(Gravity.CENTER);shell.addView(head);
  TextView sub=text(old==null?"کار موردنظرت را کوتاه یا با جزئیات بنویس.":"متن این کار را اصلاح کن.",12.5f,0xff6d7f90,false);sub.setGravity(Gravity.CENTER);sub.setPadding(0,dp(2),0,dp(10));shell.addView(sub);
  EditText input=new EditText(this);input.setHint("مثلاً: پیگیری سفارش قطعات");input.setText(old==null?"":old.optString("text",""));input.setMinLines(3);input.setMaxLines(10);input.setGravity(Gravity.TOP|Gravity.RIGHT);input.setTextSize(16);input.setPadding(dp(12),dp(12),dp(12),dp(12));input.setBackground(bg(0xffffffff,15,0xffccd9e3));shell.addView(input,new LinearLayout.LayoutParams(-1,dp(150)));
  TextView helper=text("برای ذخیره، متن کار نمی‌تواند خالی باشد.",11.5f,0xff7b8998,false);helper.setPadding(0,dp(7),0,0);shell.addView(helper);
  AlertDialog dlg=new AlertDialog.Builder(this).setView(shell).setNegativeButton("انصراف",null).setPositiveButton(old==null?"افزودن":"ذخیره تغییرات",null).create();
  dlg.setOnShowListener(x->{Button ok=dlg.getButton(AlertDialog.BUTTON_POSITIVE);ok.setTextColor(Ui.SECONDARY);ok.setTypeface(Typeface.DEFAULT,Typeface.BOLD);dlg.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Ui.MUTED);
   ok.setOnClickListener(v->{String value=input.getText().toString().trim();if(value.isEmpty()){input.setError("متن کار را وارد کنید");input.requestFocus();return;}try{if(old==null){JSONObject o=new JSONObject();o.put("id",newId());o.put("text",value);o.put("done",false);a.put(o);}else old.put("text",value);save(a);dlg.dismiss();render();}catch(Exception ignored){}});
   input.requestFocus();dlg.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
  });dlg.show();
 }
 private String hex(int c){return String.format("#%06X",0xFFFFFF&c);}
 private int contrast(int c){double y=(Color.red(c)*299+Color.green(c)*587+Color.blue(c)*114)/1000.0;return y>150?0xff203040:Color.WHITE;}
 private LinearLayout sizeControl(final int[] value){
  LinearLayout box=new LinearLayout(this);box.setGravity(Gravity.CENTER);box.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
  Button minus=new Button(this);minus.setText("−");minus.setTextSize(20);minus.setAllCaps(false);
  TextView current=text(String.valueOf(value[0]),15,Ui.TEXT,true);current.setGravity(Gravity.CENTER);current.setBackground(bg(0xffeef3f7,12,0xffd7e2eb));
  Button plus=new Button(this);plus.setText("+");plus.setTextSize(20);plus.setAllCaps(false);
  View.OnClickListener sync=v->{if(v==minus)value[0]=Math.max(12,value[0]-1);else value[0]=Math.min(28,value[0]+1);current.setText(String.valueOf(value[0]));};
  minus.setOnClickListener(sync);plus.setOnClickListener(sync);
  box.addView(minus,new LinearLayout.LayoutParams(dp(46),dp(46)));box.addView(current,new LinearLayout.LayoutParams(dp(48),dp(42)));box.addView(plus,new LinearLayout.LayoutParams(dp(46),dp(46)));return box;
 }
 private Button colorControl(final int[] value){
  Button b=new Button(this);b.setAllCaps(false);b.setText(hex(value[0]));b.setTextSize(12);paintColorButton(b,value[0]);
  b.setOnClickListener(v->showPalette(value,b));return b;
 }
 private void paintColorButton(Button b,int color){b.setText("انتخاب رنگ");b.setTextColor(contrast(color));b.setBackground(bg(color,12,0xffcfd9e2));}
 private void showPalette(final int[] value,final Button target){
  final int[] colors={Ui.TEXT,Ui.SECONDARY,0xff6b7d8f,0xff111827,0xff374151,0xff7c3aed,0xff2563eb,0xff0891b2,0xff059669,0xff65a30d,0xffca8a04,0xffea580c,0xffdc2626,0xffdb2777,0xff7f1d1d,0xff000000};
  GridLayout grid=new GridLayout(this);grid.setColumnCount(4);grid.setPadding(dp(12),dp(10),dp(12),dp(10));
  final AlertDialog[] holder=new AlertDialog[1];
  for(int c:colors){Button sw=new Button(this);sw.setText(value[0]==c?"✓":"");sw.setTextSize(22);sw.setTextColor(contrast(c));sw.setBackground(bg(c,14,0xffd4dde6));
   sw.setOnClickListener(v->{value[0]=c;paintColorButton(target,c);holder[0].dismiss();});GridLayout.LayoutParams lp=new GridLayout.LayoutParams();lp.width=dp(64);lp.height=dp(56);lp.setMargins(dp(5),dp(5),dp(5),dp(5));grid.addView(sw,lp);}
  holder[0]=new AlertDialog.Builder(this).setView(grid).setNegativeButton("بستن",null).create();holder[0].show();
 }
 private LinearLayout settingRow(String label,View control){
  LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.HORIZONTAL);r.setGravity(Gravity.CENTER_VERTICAL);r.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
  TextView t=text(label,14,Ui.TEXT,false);r.addView(t,new LinearLayout.LayoutParams(0,dp(62),1));r.addView(control,new LinearLayout.LayoutParams(dp(150),dp(54)));return r;
 }
 private void settings(){
  final int[] normalS={normalSize()},doneS={doneSize()},normalC={normalColor()},doneC={doneColor()},strikeC={strikeColor()};
  Switch boldNormalSwitch=new Switch(this);boldNormalSwitch.setChecked(boldNormal());
  Switch boldDoneSwitch=new Switch(this);boldDoneSwitch.setChecked(boldDone());
  Switch strikeSwitch=new Switch(this);strikeSwitch.setChecked(strikeDone());
  LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(16),dp(6),dp(16),dp(8));
  form.addView(settingRow("اندازه متن عادی",sizeControl(normalS)));form.addView(settingRow("رنگ متن عادی",colorControl(normalC)));form.addView(settingRow("Bold در حالت عادی",boldNormalSwitch));
  form.addView(settingRow("اندازه متن انجام‌شده",sizeControl(doneS)));form.addView(settingRow("رنگ متن انجام‌شده",colorControl(doneC)));form.addView(settingRow("Bold در حالت انجام‌شده",boldDoneSwitch));
  form.addView(settingRow("رنگ خط روی متن",colorControl(strikeC)));form.addView(settingRow("خط‌خوردگی در حالت انجام‌شده",strikeSwitch));
  ScrollView sc=new ScrollView(this);sc.addView(form);
  AlertDialog dlg=new AlertDialog.Builder(this).setView(sc).setNegativeButton("بستن",null).setNeutralButton("بازنشانی پیش‌فرض",null).setPositiveButton("ذخیره",null).create();
  dlg.setOnShowListener(x->{
   dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{prefs().edit().putInt(K_NORMAL_SIZE,normalS[0]).putInt(K_DONE_SIZE,doneS[0]).putInt(K_NORMAL_COLOR,normalC[0]).putInt(K_DONE_COLOR,doneC[0]).putInt(K_STRIKE_COLOR,strikeC[0]).putBoolean(K_BOLD_NORMAL,boldNormalSwitch.isChecked()).putBoolean(K_BOLD_DONE,boldDoneSwitch.isChecked()).putBoolean(K_STRIKE,strikeSwitch.isChecked()).apply();dlg.dismiss();render();});
   dlg.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v->confirmReset(dlg));
  });dlg.show();
 }
 private void confirmReset(AlertDialog parent){
  new AlertDialog.Builder(this).setCustomTitle(Ui.rtlDialogTitle(this,"بازنشانی تنظیمات")).setMessage("فقط ظاهر فهرست به حالت پیش‌فرض برمی‌گردد. هیچ کاری از لیست حذف نمی‌شود.")
   .setNegativeButton("انصراف",null).setPositiveButton("بازنشانی",(d,w)->{
    prefs().edit().remove(K_NORMAL_SIZE).remove(K_DONE_SIZE).remove(K_NORMAL_COLOR).remove(K_DONE_COLOR).remove(K_STRIKE_COLOR).remove(K_BOLD_NORMAL).remove(K_BOLD_DONE).remove(K_STRIKE).apply();
    parent.dismiss();render();Toast.makeText(this,"تنظیمات ظاهری بازنشانی شد",Toast.LENGTH_SHORT).show();
   }).show();
 }

 private interface CheckListener{void changed(boolean value);}
 private final class FancyCheck extends View {
  private boolean checked;private CheckListener listener;
  private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
  FancyCheck(Context c){super(c);setClickable(true);setContentDescription("انجام شده");}
  void setChecked(boolean v){checked=v;invalidate();}
  void setOnCheckedChangeListener(CheckListener l){listener=l;}
  @Override public boolean performClick(){super.performClick();checked=!checked;invalidate();if(listener!=null)listener.changed(checked);return true;}
  @Override public boolean onTouchEvent(MotionEvent e){if(e.getActionMasked()==MotionEvent.ACTION_UP)return performClick();return true;}
  @Override protected void onDraw(Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight();float l=dp(13),t=dp(15),r=w-dp(13),b=h-dp(13);
   p.setStyle(Paint.Style.FILL);p.setColor(checked?0xffe1f4f1:0xffffffff);c.drawRoundRect(l,t,r,b,dp(8),dp(8),p);
   p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2.3f));p.setStrokeCap(Paint.Cap.ROUND);p.setColor(checked?Ui.SECONDARY:0xff8292a3);c.drawRoundRect(l,t,r,b,dp(8),dp(8),p);
   if(checked){Path path=new Path();path.moveTo(dp(7),h*.52f);path.lineTo(w*.39f,h*.72f);path.lineTo(w-dp(3),h*.22f);
    p.setColor(Ui.SECONDARY);p.setStrokeWidth(dp(5.2f));p.setStrokeJoin(Paint.Join.ROUND);p.setStrokeCap(Paint.Cap.ROUND);c.drawPath(path,p);
    Path hi=new Path();hi.moveTo(dp(9),h*.49f);hi.lineTo(w*.40f,h*.67f);hi.lineTo(w-dp(6),h*.20f);p.setColor(0xff37b8ad);p.setStrokeWidth(dp(2.1f));c.drawPath(hi,p);}
  }
 }
 private final class TaskText extends TextView {
  private boolean completed;private int lineColor;
  TaskText(Context c){super(c);}
  void style(boolean done,int color){completed=done;lineColor=color;invalidate();}
  @Override protected void onDraw(Canvas c){
   super.onDraw(c);if(!completed||!strikeDone()||getLayout()==null)return;
   android.text.Layout lay=getLayout();Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(lineColor);p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeWidth(dp(2.15f));
   for(int line=0;line<lay.getLineCount();line++){float left=lay.getLineLeft(line),right=lay.getLineRight(line);if(right-left<dp(2))continue;float y=getTotalPaddingTop()+lay.getLineBaseline(line)-getTextSize()*.30f;left+=getTotalPaddingLeft();right+=getTotalPaddingLeft();
    Path path=new Path();path.moveTo(left,y);int steps=Math.max(3,(int)((right-left)/dp(18)));for(int i=1;i<=steps;i++){float x=left+(right-left)*i/steps;float jitter=((i%3)-1)*dp(.9f)+(float)Math.sin(i*1.7)*dp(.45f);path.lineTo(x,y+jitter);}c.drawPath(path,p);}
  }
 }
}
