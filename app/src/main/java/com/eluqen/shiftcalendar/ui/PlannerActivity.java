package com.eluqen.shiftcalendar.ui;
import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.text.*;
import java.util.*;
import java.lang.reflect.*;
import com.eluqen.shiftcalendar.core.PersianDate;
import com.eluqen.shiftcalendar.core.PersianDigits;
public final class PlannerActivity extends Activity {
 private LinearLayout list;
 private TextView statusView;
 private String mode="all";
 private String searchText="";
 private int statusFilter=0;
 private boolean newestFirst=false;
 private PersianDate filterDate;
 private Button statusFilterButton,dateFilterButton,sortFilterButton;
 private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
 private GradientDrawable bg(int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(15));return d;}
 private TextView label(String s,float size){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(Ui.TEXT);v.setGravity(Gravity.RIGHT);v.setPadding(dp(6),dp(6),dp(6),dp(6));return v;}
 private Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(Ui.SECONDARY);b.setMinHeight(0);b.setMinWidth(0);b.setMinimumWidth(0);b.setPadding(0,0,0,0);b.setTextSize(14.5f);b.setBackground(bg(Ui.SURFACE_ALT));return b;}
 private Button accentButton(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(Color.WHITE);b.setTextSize(16);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(bg(Ui.SECONDARY));return b;}
 private String stamp(long at){
  if(at<=0)return "بدون موعد";
  return PlannerDateTime.summary(at,true,true,TimeZone.getDefault());
 }
 @Override public void onCreate(Bundle state){
  super.onCreate(state);AppTheme.apply(this);
  mode=getIntent().getStringExtra("mode");if(mode==null)mode="all";
  if(Build.VERSION.SDK_INT>=33&&checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=android.content.pm.PackageManager.PERMISSION_GRANTED)
   requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},911);
  build();
 }
 @Override protected void onResume(){super.onResume();PersonalAlarm.reschedule(this);render();BackDock.refresh(this);ScrollTopReset.top(this);}
 @Override public void onBackPressed(){BackDock.handleBack(this);}
 private void build(){
  LinearLayout root=new LinearLayout(this);root.setOrientation(android.widget.LinearLayout.VERTICAL);root.setBackgroundColor(Ui.SURFACE);
  LinearLayout hero=new LinearLayout(this);hero.setOrientation(android.widget.LinearLayout.VERTICAL);hero.setPadding(dp(14),dp(12),dp(14),dp(12));hero.setBackground(bg(Ui.SURFACE_ALT));
  TextView title=label(mode.equals("todo")?"فهرست کارها":mode.equals("event")?"یادآوری رخدادها":"برنامه‌ریزی شخصی",22);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);title.setTextColor(Ui.PRIMARY);hero.addView(title);
  TextView note=label(mode.equals("event")?"موعدهای شخصی با تاریخ، ساعت و اعلان مستقل":"برنامه‌ریزی شخصی",14.5f);note.setTextColor(Ui.MUTED);hero.addView(note);root.addView(hero);
  LinearLayout actions=new LinearLayout(this);actions.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
  Button todo=button("＋ کار جدید"),event=accentButton("＋ یادآوری جدید");
  actions.addView(todo,new LinearLayout.LayoutParams(0,dp(48),1));
  actions.addView(event,new LinearLayout.LayoutParams(0,dp(52),1));
  todo.setOnClickListener(v->edit(null,false));event.setOnClickListener(v->edit(null,true));
  todo.setVisibility(mode.equals("event")?View.GONE:View.VISIBLE);
  event.setVisibility(mode.equals("todo")?View.GONE:View.VISIBLE);
  LinearLayout.LayoutParams alp=new LinearLayout.LayoutParams(-1,-2);alp.setMargins(dp(12),dp(8),dp(12),dp(6));root.addView(actions,alp);
  EditText search=new EditText(this);
  search.setSingleLine(true);
  search.setHint("جست‌وجوی عنوان یا توضیحات...");
  search.setTextSize(15);
  search.setTextDirection(View.TEXT_DIRECTION_RTL);
  search.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
  search.setPadding(dp(12),0,dp(12),0);
  search.setBackground(Ui.roundedStroke(Ui.CARD,Ui.BORDER,12,this));
  LinearLayout.LayoutParams slp=new LinearLayout.LayoutParams(-1,dp(47));
  slp.setMargins(dp(13),dp(3),dp(13),dp(4));
  root.addView(search,slp);
  search.addTextChangedListener(new android.text.TextWatcher(){
   public void beforeTextChanged(CharSequence s,int start,int count,int after){}
   public void onTextChanged(CharSequence s,int start,int before,int count){
    searchText=s.toString().trim().toLowerCase(Locale.ROOT);
    render();
   }
   public void afterTextChanged(android.text.Editable e){}
  });
  LinearLayout filters=new LinearLayout(this);
  filters.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
  statusFilterButton=button("همه وضعیت‌ها");
  dateFilterButton=button("همه تاریخ‌ها");
  sortFilterButton=button("قدیمی اول");
  Button resetFilter=button("نمایش همه");
  for(Button b:new Button[]{statusFilterButton,sortFilterButton,resetFilter}){
   b.setTextSize(13);
   LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(0,dp(44),1);
   fp.setMargins(dp(2),0,dp(2),0);
   filters.addView(b,fp);
  }
  LinearLayout dateFilters=new LinearLayout(this);
  dateFilters.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
  Button clearDate=button("✕ لغو تاریخ");
  dateFilterButton.setTextSize(13.5f);
  clearDate.setTextSize(13);
  LinearLayout.LayoutParams dateLp=new LinearLayout.LayoutParams(0,dp(44),2);
  dateLp.setMargins(dp(2),0,dp(2),0);
  dateFilters.addView(dateFilterButton,dateLp);
  LinearLayout.LayoutParams clearLp=new LinearLayout.LayoutParams(0,dp(44),1);
  clearLp.setMargins(dp(2),0,dp(2),0);
  dateFilters.addView(clearDate,clearLp);
  clearDate.setVisibility(View.GONE);
  clearDate.setOnClickListener(v->{
   filterDate=null;
   dateFilterButton.setText("همه تاریخ‌ها");
   clearDate.setVisibility(View.GONE);
   render();
  });
  statusFilterButton.setOnClickListener(v->
   new AlertDialog.Builder(this)
    .setTitle("وضعیت یادآوری‌ها")
    .setSingleChoiceItems(new String[]{"همه وضعیت‌ها","در انتظار","انجام‌شده"},
      statusFilter,(dialog,which)->{
       statusFilter=which;
       statusFilterButton.setText(statusFilter==1?"در انتظار":statusFilter==2?"انجام‌شده":"همه وضعیت‌ها");
       dialog.dismiss();
       render();
      })
    .setNegativeButton("انصراف",null).show());
  dateFilterButton.setOnClickListener(v->DualDatePicker.show(this,
     filterDate==null?PersianDate.todayTehran():filterDate,
     new DualDatePicker.Result(){
      public void selected(Object chosen){
       if(!(chosen instanceof PersianDate))return;
       filterDate=(PersianDate)chosen;
       dateFilterButton.setText("تاریخ: "+PersianDigits.of(filterDate.toString()));
       clearDate.setVisibility(View.VISIBLE);
       render();
      }
     }));
  sortFilterButton.setOnClickListener(v->{
   newestFirst=!newestFirst;
   sortFilterButton.setText(newestFirst?"جدید اول":"قدیمی اول");
   render();
  });
  resetFilter.setOnClickListener(v->{
   statusFilter=0;newestFirst=false;filterDate=null;
   statusFilterButton.setText("همه وضعیت‌ها");
   dateFilterButton.setText("همه تاریخ‌ها");
   clearDate.setVisibility(View.GONE);
   sortFilterButton.setText("قدیمی اول");
   search.setText("");
   render();
  });
  LinearLayout.LayoutParams flp=new LinearLayout.LayoutParams(-1,dp(46));
  flp.setMargins(dp(11),0,dp(11),dp(6));
  root.addView(filters,flp);
  LinearLayout.LayoutParams dlp=new LinearLayout.LayoutParams(-1,dp(46));
  dlp.setMargins(dp(11),0,dp(11),dp(6));
  root.addView(dateFilters,dlp);
  ScrollView scroll=new ScrollView(this);list=new LinearLayout(this);list.setOrientation(android.widget.LinearLayout.VERTICAL);list.setPadding(dp(11),dp(5),dp(11),dp(22));
  scroll.addView(list);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  setContentView(root);BackDock.install(this,root);
 }
 private void render(){
  if(list==null)return;
  list.removeAllViews();
  JSONArray values=PersonalStore.all(this);
  ArrayList<JSONObject> all=new ArrayList<>();
  for(int i=0;i<values.length();i++){
   JSONObject item=values.optJSONObject(i);
   if(item==null)continue;
   if(!mode.equals("all")&&!mode.equals(item.optString("kind","todo")))continue;
   all.add(item);
  }
  // Completion does not relocate a row or alter the page dimensions.
  Collections.sort(all,(a,b)->{
   long x=a.optLong("when",0),y=b.optLong("when",0);
   int time=Long.compare(x==0?Long.MAX_VALUE:x,y==0?Long.MAX_VALUE:y);
   int ordered=time!=0?time:Integer.compare(a.optInt("id"),b.optInt("id"));
   return newestFirst?-ordered:ordered;
  });
  statusView=label("",15.5f);
  statusView.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
  statusView.setTextColor(Ui.SECONDARY);
  statusView.setBackground(bg(Ui.SURFACE_ALT));
  list.addView(statusView);
  updateStatus();
  if(all.isEmpty()){
   TextView empty=label(mode.equals("event")
      ?"برای افزودن اولین یادآوری، دکمه «＋ یادآوری جدید» را بزنید."
      :"موردی برای نمایش نیست.",14);
   empty.setBackground(bg(Ui.SURFACE_ALT));
   list.addView(empty);
  }
  int visibleCount=0;
  for(JSONObject item:all){
   boolean done=item.optBoolean("done");
   if(statusFilter==1&&done||statusFilter==2&&!done)continue;
   String hay=(item.optString("title","")+" "+item.optString("note",""))
       .toLowerCase(Locale.ROOT);
   if(!searchText.isEmpty()&&!hay.contains(searchText))continue;
   if(filterDate!=null){
    boolean hasDate=item.optBoolean("has_date",item.optLong("when")>0);
    if(!hasDate||item.optLong("when")<=0||
       !filterDate.equals(PlannerDateTime.jalali(item.optLong("when"),TimeZone.getDefault())))
       continue;
   }
   row(item,done);
   visibleCount++;
  }
  if(visibleCount==0&&!all.isEmpty()){
   TextView hint=label("موردی مطابق فیلترها پیدا نشد.",15);
   hint.setBackground(bg(Ui.SURFACE_ALT));
   list.addView(hint);
  }
 }
 private void updateStatus(){
  if(statusView==null)return;
  JSONArray rows=PersonalStore.all(this);
  int active=0,completed=0;
  for(int i=0;i<rows.length();i++){
   JSONObject item=rows.optJSONObject(i);
   if(item==null)continue;
   if(!mode.equals("all")&&!mode.equals(item.optString("kind","todo")))continue;
   if(item.optBoolean("done"))completed++;else active++;
  }
  statusView.setText((mode.equals("event")?"یادآوری‌های فعال: ":"فعال: ")
     +active+"   •   انجام‌شده: "+completed);
 }
 private GradientDrawable cardBackground(boolean done){
  GradientDrawable shape=bg(PlannerCompletionVisual.cardFill(done,Ui.CARD));
  shape.setStroke(dp(done?3:1),PlannerCompletionVisual.cardOutline(done,Ui.BORDER));
  return shape;
 }
 private void styleBadge(TextView badge,boolean done){
  badge.setText(PlannerCompletionVisual.badge(done));
  badge.setTextColor(PlannerCompletionVisual.badgeInk(done,Ui.TEXT));
  badge.setBackground(Ui.roundedStroke(
      PlannerCompletionVisual.badgeFill(done,Ui.SURFACE_ALT),
      done?PlannerCompletionVisual.DONE_BORDER:Ui.BORDER,10,this));
 }
 private void styleRowButton(Button b,int role,boolean done){
  int fill=PlannerCompletionVisual.actionFill(done,role,Ui.PRIMARY,Ui.SURFACE_ALT);
  int outline=PlannerCompletionVisual.actionOutline(done,role,Ui.PRIMARY,Ui.BORDER);
  GradientDrawable drawable=bg(fill);
  drawable.setCornerRadius(dp(11));
  drawable.setStroke(dp(2),outline);
  b.setBackgroundTintList(null);
  b.setBackground(drawable);
  b.setTextColor(PlannerCompletionVisual.actionInk(done,role,Ui.TEXT));
  b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
  b.setPadding(dp(2),0,dp(2),0);
  b.setMinHeight(0);
  b.setMinWidth(0);
 }
 private void updateRowVisuals(LinearLayout card,TextView badge,TextView title,
         TextView summary,TextView soundLine,Button[] controls,JSONObject item,boolean done){
  card.setBackground(cardBackground(done));
  styleBadge(badge,done);
  title.setTextColor(done?PlannerCompletionVisual.DONE_TEXT:Ui.TEXT);
  summary.setTextColor(done?PlannerCompletionVisual.DONE_TEXT:
      item.optLong("when")>0&&item.optLong("when")<System.currentTimeMillis()
        ?0xffb43c43:Ui.TEXT);
  soundLine.setTextColor(done?PlannerCompletionVisual.DONE_TEXT:Ui.MUTED);
  for(int i=0;i<controls.length;i++)styleRowButton(controls[i],i,done);
 }
 private void row(JSONObject item,boolean done){
  int id=item.optInt("id");boolean todo="todo".equals(item.optString("kind","todo"));
  LinearLayout card=new LinearLayout(this);card.setOrientation(android.widget.LinearLayout.VERTICAL);
  card.setPadding(dp(12),dp(10),dp(12),dp(10));
  LinearLayout statusLine=new LinearLayout(this);
  statusLine.setGravity(Gravity.CENTER_VERTICAL);
  statusLine.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
  TextView badge=label("",13.5f);
  badge.setGravity(Gravity.CENTER);
  badge.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
  statusLine.addView(badge,new LinearLayout.LayoutParams(dp(134),dp(32)));
  card.addView(statusLine,new LinearLayout.LayoutParams(-1,dp(35)));
  TextView title=label(item.optString("title"),18);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
  card.addView(title);
  boolean hasDate=item.optBoolean("has_date",item.optLong("when")>0),hasTime=item.optBoolean("has_time",item.optLong("when")>0);
  String whenText=PlannerDateTime.summary(item.optLong("when"),hasDate,hasTime,TimeZone.getDefault());
  // Keep the long date/time and sound labels in separate RTL rows, so the
  // final word of "صدای پیش‌فرض اعلان" is never orphaned by the metadata.
  TextView summary=label((todo?"کار":"یادآوری رخداد")+"  •  "+whenText,15);
  summary.setTextDirection(View.TEXT_DIRECTION_RTL);
  summary.setLineSpacing(dp(2),1.02f);
  card.addView(summary);
  TextView soundLine=label(item.optBoolean("alert")
      ?SoundPicker.title(item.optString("sound","default_notification"))
      :"بدون اعلان",14.5f);
  soundLine.setTextDirection(View.TEXT_DIRECTION_RTL);
  soundLine.setLineSpacing(dp(2),1.02f);
  card.addView(soundLine);
  // Only the list presentation is shortened. Saved text and the editor remain full.
  if(!item.optString("note").trim().isEmpty()){
   TextView preview=label(item.optString("note"),15);
   preview.setMaxLines(2);
   preview.setEllipsize(android.text.TextUtils.TruncateAt.END);
   preview.setTextDirection(View.TEXT_DIRECTION_RTL);
   card.addView(preview);
  }
  LinearLayout actions=new LinearLayout(this);
  actions.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
  Button edit=button("ویرایش");
  Button finish=button(done?"بازگردانی":"انجام شد");
  Button remove=button("حذف");
  final Button[] rowButtons={edit,finish,remove};
  for(Button b:rowButtons){
   LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,dp(46),1);
   params.setMargins(dp(3),0,dp(3),0);
   actions.addView(b,params);
  }
  updateRowVisuals(card,badge,title,summary,soundLine,rowButtons,item,done);
  edit.setOnClickListener(v->edit(item,!todo));
  final boolean[] completed={done};
  finish.setOnClickListener(v->{
   boolean next=!completed[0];
   PersonalStore.setDone(this,id,next);
   JSONObject persisted=PersonalStore.get(this,id);
   if(persisted==null||persisted.optBoolean("done")!=next){
    Toast.makeText(this,"تغییر وضعیت ذخیره نشد",Toast.LENGTH_SHORT).show();
    return;
   }
   completed[0]=next;
   finish.setText(next?"بازگردانی":"انجام شد");
   updateRowVisuals(card,badge,title,summary,soundLine,rowButtons,item,next);
   updateStatus();
  });
  remove.setOnClickListener(v->new AlertDialog.Builder(this)
   .setTitle("حذف مورد")
   .setMessage("«"+item.optString("title")+"» حذف شود؟")
   .setNegativeButton("انصراف",null)
   .setPositiveButton("حذف",(d,w)->{PersonalStore.remove(this,id);render();}).show());
  card.addView(actions);
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,dp(9));list.addView(card,lp);
 }
 private void edit(JSONObject original,boolean event){
  Intent editor=new Intent(this,ReminderEditorActivity.class);
  editor.putExtra(ReminderEditorActivity.EXTRA_IS_EVENT,event);
  if(original!=null)editor.putExtra(ReminderEditorActivity.EXTRA_ITEM_ID,
      original.optInt("id",0));
  startActivity(editor);
 }
 @Override protected void onActivityResult(int request,int result,Intent data){
  if(!SoundPicker.result(this,request,result,data))super.onActivityResult(request,result,data);
 }
}
