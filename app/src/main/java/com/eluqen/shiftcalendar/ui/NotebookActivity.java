package com.eluqen.shiftcalendar.ui;
import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Build;
import android.window.OnBackInvokedDispatcher;
import android.text.*;
import android.text.method.ScrollingMovementMethod;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.text.SimpleDateFormat;
import java.util.*;

public final class NotebookActivity extends Activity implements BackDock.LocalBackHandler{
 private static final String STORE="eluqen_private_notebook",KEY="items";
 private LinearLayout base,list; private EditText title,body,search; private Ink ink; private String editing,initialTitle="",initialBody="",initialInk="[]"; private boolean initialPinned; private int initialColor; private boolean pinnedOnly,notePinned,selectionMode; private int noteColor=0xfffff7d6; private final HashSet<String> selected=new HashSet<>(); private Button bulkButton,selectButton;
 private int dp(float v){return Math.round(v*getResources().getDisplayMetrics().density);}
 private GradientDrawable bg(int color,float r,int stroke){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(r));if(stroke!=0)g.setStroke(dp(1),stroke);return g;}
 private TextView text(String s,float z,int c,boolean b){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(c);v.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);if(b)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}
 private Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(14);b.setTextColor(Ui.TEXT);b.setBackground(bg(Ui.CARD,13,Ui.BORDER));return b;}
 private static JSONArray items(Context c){try{return new JSONArray(c.getSharedPreferences(STORE,0).getString(KEY,"[]"));}catch(Exception e){return new JSONArray();}}
 private static void write(Context c,JSONArray a){c.getSharedPreferences(STORE,0).edit().putString(KEY,a.toString()).apply();}
 public static JSONArray exportJson(Context c){return items(c);}
 public static void importJson(Context c,JSONArray data){if(data==null)return;JSONArray r=new JSONArray();for(int i=0;i<data.length();i++){JSONObject o=data.optJSONObject(i);if(o!=null&&!o.optString("id","").isEmpty())r.put(o);}write(c,r);}
 @Override protected void onCreate(Bundle s){super.onCreate(s);AppTheme.apply(this);showList();if(Build.VERSION.SDK_INT>=33)getOnBackInvokedDispatcher().registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT,()->handleNotebookBack());}
 private void shell(String heading){
  base=new LinearLayout(this);base.setOrientation(android.widget.LinearLayout.VERTICAL);base.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);base.setBackgroundColor(Ui.SURFACE);
  LinearLayout head=new LinearLayout(this);head.setOrientation(android.widget.LinearLayout.VERTICAL);head.setPadding(dp(15),dp(10),dp(15),dp(9));head.setBackground(bg(Ui.SURFACE_ALT,16,0));
  head.addView(text(heading,21,Ui.PRIMARY,true));
  if(editing==null)head.addView(text("یادداشت‌های متنی و دست‌نویس، با جست‌وجو، سنجاق و رنگ‌بندی",12.5f,Ui.MUTED,false));
  base.addView(head);
  setContentView(base);BackDock.install(this,base);
 }
 private void showList(){
  editing=null;title=null;body=null;ink=null;shell("دفترچه یادداشت");
  LinearLayout tools=new LinearLayout(this);tools.setGravity(Gravity.CENTER_VERTICAL);tools.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);tools.setPadding(dp(10),dp(8),dp(10),dp(4));
  bulkButton=button(selectionMode?"حذف انتخاب‌شده":"＋ یادداشت");bulkButton.setOnClickListener(v->{if(selectionMode)bulkDelete();else showEditor(null);});tools.addView(bulkButton,new LinearLayout.LayoutParams(0,dp(48),1));
  Button pin=button(pinnedOnly?"همه":"سنجاق‌شده");pin.setOnClickListener(v->{pinnedOnly=!pinnedOnly;showList();});tools.addView(pin,new LinearLayout.LayoutParams(0,dp(48),1));
  selectButton=button(selectionMode?"لغو انتخاب":"انتخاب گروهی");selectButton.setOnClickListener(v->{selectionMode=!selectionMode;selected.clear();showList();});tools.addView(selectButton,new LinearLayout.LayoutParams(0,dp(48),1));base.addView(tools);
  search=new EditText(this);search.setSingleLine(true);search.setHint("جست‌وجو...");search.setTextSize(14);search.setBackground(bg(Ui.CARD,13,Ui.BORDER));search.setPadding(dp(12),0,dp(12),0);
  LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(48));sp.setMargins(dp(10),0,dp(10),dp(5));base.addView(search,sp);
  ScrollView sc=new ScrollView(this);sc.setFillViewport(false);sc.setVerticalScrollBarEnabled(true);sc.setScrollbarFadingEnabled(false);list=new LinearLayout(this);list.setOrientation(android.widget.LinearLayout.VERTICAL);list.setPadding(dp(12),dp(6),dp(12),dp(24));sc.addView(list);base.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
  search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){}public void onTextChanged(CharSequence s,int a,int b,int c){renderList();}public void afterTextChanged(Editable e){}});renderList();updateBulkState();
 }
 private void updateBulkState(){if(bulkButton==null)return;if(!selectionMode){bulkButton.setEnabled(true);return;}bulkButton.setText(selected.isEmpty()?"حذف انتخاب‌شده":"حذف انتخاب‌شده ("+selected.size()+")");bulkButton.setEnabled(!selected.isEmpty());}
 private void bulkDelete(){if(selected.isEmpty())return;new AlertDialog.Builder(this).setTitle("حذف گروهی").setMessage(selected.size()+" یادداشت انتخاب‌شده حذف شوند؟").setNegativeButton("انصراف",null).setPositiveButton("حذف",(d,w)->{JSONArray a=items(this),n=new JSONArray();for(int i=0;i<a.length();i++){JSONObject x=a.optJSONObject(i);if(x!=null&&!selected.contains(x.optString("id")))n.put(x);}write(this,n);selected.clear();selectionMode=false;showList();}).show();}
 private void renderList(){
  if(list==null)return;list.removeAllViews();JSONArray a=items(this);ArrayList<JSONObject> notes=new ArrayList<>();
  String q=search==null?"":search.getText().toString().trim().toLowerCase(Locale.ROOT);
  for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o==null)continue;if(pinnedOnly&&!o.optBoolean("pinned",false))continue;
   String hay=(o.optString("title")+" "+o.optString("body")).toLowerCase(Locale.ROOT);if(!q.isEmpty()&&!hay.contains(q))continue;notes.add(o);}
  Collections.sort(notes,(x,y)->{int p=Boolean.compare(y.optBoolean("pinned"),x.optBoolean("pinned"));return p!=0?p:Long.compare(y.optLong("updated"),x.optLong("updated"));});
  TextView count=text("تعداد یادداشت‌ها: "+notes.size(),13,Ui.MUTED,true);count.setPadding(dp(4),dp(3),dp(4),dp(8));list.addView(count);
  if(notes.isEmpty()){TextView e=text("چیزی برای نمایش نیست.",14,0xff718194,false);e.setGravity(Gravity.CENTER);e.setPadding(0,dp(35),0,0);list.addView(e);return;}
  for(int i=0;i<notes.size();i++)addCard(notes.get(i),i+1);
 }
 private void addCard(JSONObject n,int number){
  int color=n.optInt("color",0xfffff7d6);LinearLayout card=new LinearLayout(this);card.setOrientation(android.widget.LinearLayout.VERTICAL);card.setPadding(dp(13),dp(10),dp(13),dp(10));card.setBackground(bg(color,16,0xffd9e2e8));
  LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);top.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
  String id=n.optString("id","");if(selectionMode){CheckBox choose=new CheckBox(this);choose.setChecked(selected.contains(id));choose.setScaleX(1.15f);choose.setScaleY(1.15f);choose.setOnCheckedChangeListener((b,on)->{if(on)selected.add(id);else selected.remove(id);updateBulkState();});top.addView(choose,new LinearLayout.LayoutParams(dp(50),dp(50)));}
  String name=n.optString("title","").trim();if(name.isEmpty())name="یادداشت بدون عنوان";
  TextView h=text(number+".  "+(n.optBoolean("pinned",false)?"📌  ":"")+name,17,Ui.TEXT,true);top.addView(h,new LinearLayout.LayoutParams(0,-2,1));
  TextView date=text(formatTime(n.optLong("updated")),11.5f,0xff748292,false);date.setGravity(Gravity.CENTER);top.addView(date,new LinearLayout.LayoutParams(dp(105),dp(36)));card.addView(top);
  String preview=n.optString("body","").trim();if(!preview.isEmpty()){TextView p=text(preview,13,0xff56697d,false);p.setMaxLines(3);p.setEllipsize(TextUtils.TruncateAt.END);p.setPadding(0,dp(4),0,dp(7));card.addView(p);}
  LinearLayout actions=new LinearLayout(this);actions.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
  Button open=button("ویرایش");open.setOnClickListener(v->showEditor(n));actions.addView(open,new LinearLayout.LayoutParams(0,dp(42),1));
  Button pin=button(n.optBoolean("pinned",false)?"برداشتن سنجاق":"سنجاق");pin.setOnClickListener(v->togglePin(n));actions.addView(pin,new LinearLayout.LayoutParams(0,dp(42),1));
  Button del=button("حذف");del.setOnClickListener(v->delete(n));actions.addView(del,new LinearLayout.LayoutParams(0,dp(42),1));card.addView(actions);
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,dp(10));list.addView(card,lp);
 }
 private String formatTime(long t){if(t<=0)return "";return new SimpleDateFormat("yyyy/MM/dd  HH:mm",Locale.US).format(new Date(t));}
 private void togglePin(JSONObject note){try{note.put("pinned",!note.optBoolean("pinned",false));note.put("updated",System.currentTimeMillis());replace(note);showList();}catch(Exception ignored){}}
 private void delete(JSONObject note){new AlertDialog.Builder(this).setMessage("این یادداشت حذف شود؟").setNegativeButton("انصراف",null).setPositiveButton("حذف",(d,w)->{JSONArray a=items(this),n=new JSONArray();for(int i=0;i<a.length();i++){JSONObject x=a.optJSONObject(i);if(x!=null&&!note.optString("id").equals(x.optString("id")))n.put(x);}write(this,n);showList();}).show();}
 private void replace(JSONObject note){JSONArray a=items(this),n=new JSONArray();for(int i=0;i<a.length();i++){JSONObject x=a.optJSONObject(i);if(x!=null&&!note.optString("id").equals(x.optString("id")))n.put(x);}n.put(note);write(this,n);}
 private void showEditor(JSONObject note){
  editing=note==null?UUID.randomUUID().toString():note.optString("id",UUID.randomUUID().toString());notePinned=note!=null&&note.optBoolean("pinned",false);noteColor=note==null?0xfffff7d6:note.optInt("color",0xfffff7d6);initialTitle=note==null?"":note.optString("title","");initialBody=note==null?"":note.optString("body","");initialPinned=notePinned;initialColor=noteColor;initialInk=note==null?"[]":note.optJSONArray("ink")==null?"[]":note.optJSONArray("ink").toString();
  shell(note==null?"یادداشت جدید":"ویرایش یادداشت");
  LinearLayout actions=new LinearLayout(this);actions.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);actions.setPadding(dp(10),dp(6),dp(10),dp(4));
  Button save=button("✓ ذخیره");actions.addView(save,new LinearLayout.LayoutParams(0,dp(48),1));
  Button cancel=button("✕ لغو");cancel.setOnClickListener(v->requestCloseEditor());actions.addView(cancel,new LinearLayout.LayoutParams(0,dp(48),1));
  Button pin=button(notePinned?"📌 سنجاق‌شده":"📌 سنجاق");pin.setOnClickListener(v->{notePinned=!notePinned;pin.setText(notePinned?"📌 سنجاق‌شده":"📌 سنجاق");});actions.addView(pin,new LinearLayout.LayoutParams(0,dp(48),1));
  base.addView(actions);
  ScrollView sc=new ScrollView(this);sc.setVerticalScrollBarEnabled(true);sc.setScrollbarFadingEnabled(false);sc.setFillViewport(false);LinearLayout content=new LinearLayout(this);content.setOrientation(android.widget.LinearLayout.VERTICAL);content.setPadding(dp(12),dp(6),dp(12),dp(110));sc.addView(content);base.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
  LinearLayout paper=new LinearLayout(this);paper.setOrientation(android.widget.LinearLayout.VERTICAL);paper.setPadding(dp(12),dp(10),dp(12),dp(10));paper.setBackground(bg(noteColor,17,0xffd9e2e8));content.addView(paper);
  title=new EditText(this);title.setSingleLine(true);title.setHint("عنوان یادداشت — الزامی");title.setTextSize(19);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);title.setText(note==null?"":note.optString("title"));paper.addView(title);
  body=new EditText(this);body.setHint("اینجا بنویسید...");body.setGravity(Gravity.TOP|Gravity.RIGHT);body.setTextSize(16);body.setText(note==null?"":note.optString("body"));body.setVerticalScrollBarEnabled(true);body.setMovementMethod(new ScrollingMovementMethod());
  body.setOnTouchListener((v,e)->{v.getParent().requestDisallowInterceptTouchEvent(body.canScrollVertically(-1)||body.canScrollVertically(1));if(e.getActionMasked()==MotionEvent.ACTION_UP||e.getActionMasked()==MotionEvent.ACTION_CANCEL)v.getParent().requestDisallowInterceptTouchEvent(false);return false;});
  paper.addView(body,new LinearLayout.LayoutParams(-1,dp(270)));
  TextView style=text("رنگ برگه",13,Ui.MUTED,true);style.setPadding(0,dp(8),0,dp(4));content.addView(style);content.addView(notePalette(paper));
  TextView cap=text("دست‌نویس و طراحی",14,Ui.TEXT,true);cap.setPadding(0,dp(12),0,dp(5));content.addView(cap);
  LinearLayout drawTools=new LinearLayout(this);drawTools.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);content.addView(drawTools);
  Button pen=button("✎ قلم");Button erase=button("پاک‌کن");Button undo=button("↶ واگرد");Button clear=button("پاک‌کردن");
  for(Button b:new Button[]{pen,erase,undo,clear})drawTools.addView(b,new LinearLayout.LayoutParams(0,dp(46),1));
  ink=new Ink(this);ink.load(note==null?null:note.optJSONArray("ink"));
  LinearLayout.LayoutParams inkLp=new LinearLayout.LayoutParams(-1,dp(760));inkLp.setMargins(dp(26),0,dp(26),0);content.addView(ink,inkLp);
  Runnable inkState=()->{boolean has=ink.hasStrokes();undo.setEnabled(has);clear.setEnabled(has);};ink.setChangeListener(inkState);inkState.run();
  LinearLayout penOpts=new LinearLayout(this);penOpts.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);content.addView(penOpts);
  Button penColor=button("رنگ قلم");penColor.setOnClickListener(v->penColorPalette());penOpts.addView(penColor,new LinearLayout.LayoutParams(0,dp(44),1));
  Button thinner=button("قلم باریک‌تر");thinner.setOnClickListener(v->ink.setWidth(Math.max(1,ink.penWidth-1)));penOpts.addView(thinner,new LinearLayout.LayoutParams(0,dp(44),1));
  Button thicker=button("قلم ضخیم‌تر");thicker.setOnClickListener(v->ink.setWidth(Math.min(12,ink.penWidth+1)));penOpts.addView(thicker,new LinearLayout.LayoutParams(0,dp(44),1));
  pen.setOnClickListener(v->ink.erase=false);erase.setOnClickListener(v->ink.erase=true);undo.setOnClickListener(v->ink.undo());clear.setOnClickListener(v->new AlertDialog.Builder(this).setMessage("دست‌نوشته این یادداشت پاک شود؟").setNegativeButton("انصراف",null).setPositiveButton("پاک‌کردن",(d,w)->ink.clear()).show());
  save.setEnabled(true);
  save.setOnClickListener(v->{if(title.getText().toString().trim().isEmpty()){title.setError("برای ذخیره، یک عنوان برای یادداشت وارد کنید");title.requestFocus();Toast.makeText(this,"عنوان یادداشت الزامی است",Toast.LENGTH_LONG).show();return;}persist();showList();});
 }
 private View notePalette(final LinearLayout paper){
  final int[] colors={0xfffff7d6,0xffffe8e8,0xffffedf6,0xffeee8ff,0xffe8f1ff,0xffe6f8f4,0xffedf7df,0xfff3f4f6};
  LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER);row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);final TextView[] swatches=new TextView[colors.length];
  for(int i=0;i<colors.length;i++){final int idx=i,c=colors[i];TextView sw=new TextView(this);swatches[i]=sw;sw.setText(c==noteColor?"✓":"");sw.setTextSize(20);sw.setGravity(Gravity.CENTER);sw.setTextColor(0xff203a63);sw.setBackground(bg(c,12,0xffd5dde5));
   sw.setOnClickListener(v->{noteColor=c;paper.setBackground(bg(c,17,0xffd9e2e8));for(int j=0;j<swatches.length;j++)swatches[j].setText(j==idx?"✓":"");});LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(42),dp(42));lp.setMargins(dp(3),0,dp(3),0);row.addView(sw,lp);}
  return row;
 }
 private void penColorPalette(){
  final int[] colors={0xff17665c,0xff203a63,0xff111827,0xff2563eb,0xff7c3aed,0xffdc2626,0xffea580c,0xff059669};
  GridLayout g=new GridLayout(this);g.setColumnCount(4);g.setPadding(dp(12),dp(10),dp(12),dp(10));final AlertDialog[] h=new AlertDialog[1];
  for(int c:colors){Button b=new Button(this);b.setText(ink.penColor==c?"✓":"");b.setTextSize(22);b.setTextColor(Color.WHITE);b.setBackground(bg(c,13,0xffd5dde5));b.setOnClickListener(v->{ink.setColor(c);h[0].dismiss();});GridLayout.LayoutParams lp=new GridLayout.LayoutParams();lp.width=dp(64);lp.height=dp(54);lp.setMargins(dp(5),dp(5),dp(5),dp(5));g.addView(b,lp);}
  h[0]=new AlertDialog.Builder(this).setView(g).setNegativeButton("بستن",null).create();h[0].show();
 }
 private void persist(){
  if(editing==null||title==null||body==null||ink==null)return;String t=title.getText().toString().trim(),b=body.getText().toString().trim();JSONArray paths=ink.dump();
  if(t.isEmpty())return;
  try{JSONObject n=new JSONObject();n.put("id",editing);n.put("title",t);n.put("body",b);n.put("ink",paths);n.put("updated",System.currentTimeMillis());n.put("pinned",notePinned);n.put("color",noteColor);replace(n);}
  catch(Exception e){Toast.makeText(this,"ذخیره یادداشت انجام نشد",Toast.LENGTH_LONG).show();}
 }
 @Override protected void onResume(){super.onResume();BackDock.refresh(this);ScrollTopReset.top(this);}
 private boolean dirty(){if(editing==null||title==null||body==null||ink==null)return false;return !initialTitle.equals(title.getText().toString())||!initialBody.equals(body.getText().toString())||initialPinned!=notePinned||initialColor!=noteColor||!initialInk.equals(ink.dump().toString());}
 private void requestCloseEditor(){if(editing==null){showList();return;}if(!dirty()){showList();return;}new AlertDialog.Builder(this).setTitle("تغییرات ذخیره نشده").setMessage("این یادداشت تغییر کرده است. می‌خواهید قبل از بازگشت ذخیره شود؟").setNegativeButton("انصراف",null).setNeutralButton("بدون ذخیره",(d,w)->showList()).setPositiveButton("ذخیره و بازگشت",(d,w)->{if(title.getText().toString().trim().isEmpty()){title.setError("برای ذخیره، یک عنوان برای یادداشت وارد کنید");title.requestFocus();Toast.makeText(this,"عنوان یادداشت الزامی است",Toast.LENGTH_LONG).show();return;}persist();showList();}).show();}
 @Override public boolean onDockBack(){if(editing!=null){requestCloseEditor();return true;}return false;}
 private void handleNotebookBack(){if(editing!=null){requestCloseEditor();return;}BackDock.handleBack(this);}
 @Override public void onBackPressed(){handleNotebookBack();}
 private static final class Ink extends View{
  private final Paint p=new Paint(3);private JSONArray paths=new JSONArray();private JSONArray current;boolean erase;int penColor=0xff17665c,penWidth=3;private Runnable changeListener;
  Ink(Context c){super(c);setBackgroundColor(Color.WHITE);p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);}
  void setColor(int c){penColor=c;erase=false;} void setWidth(int w){penWidth=w;erase=false;} void setChangeListener(Runnable r){changeListener=r;} boolean hasStrokes(){return paths.length()>0||(current!=null&&!erase);} private void changed(){if(changeListener!=null)changeListener.run();}
  void load(JSONArray saved){paths=saved==null?new JSONArray():saved;invalidate();}JSONArray dump(){return paths;}
  void undo(){JSONArray n=new JSONArray();for(int i=0;i<paths.length()-1;i++)n.put(paths.opt(i));paths=n;invalidate();changed();}void clear(){paths=new JSONArray();invalidate();changed();}
  private void point(JSONArray stroke,float x,float y){try{if(stroke.length()<2500){JSONArray pt=new JSONArray();pt.put(Math.max(0,Math.min(1,x/Math.max(1,getWidth()))));pt.put(Math.max(0,Math.min(1,y/Math.max(1,getHeight()))));stroke.put(pt);}}catch(Exception ignored){}}
  @Override public boolean onTouchEvent(MotionEvent e){float x=e.getX(),y=e.getY();switch(e.getActionMasked()){
   case MotionEvent.ACTION_DOWN:getParent().requestDisallowInterceptTouchEvent(true);current=new JSONArray();point(current,x,y);invalidate();if(!erase)changed();return true;
   case MotionEvent.ACTION_MOVE:if(current!=null){for(int i=0;i<e.getHistorySize();i++)point(current,e.getHistoricalX(i),e.getHistoricalY(i));point(current,x,y);invalidate();}return true;
   case MotionEvent.ACTION_UP:if(current!=null){point(current,x,y);if(paths.length()<500&&(!erase||paths.length()>0)){JSONArray s=new JSONArray();s.put(erase?Color.WHITE:penColor);s.put(erase?24:penWidth);s.put(current);paths.put(s);changed();}current=null;invalidate();}getParent().requestDisallowInterceptTouchEvent(false);performClick();return true;
   case MotionEvent.ACTION_CANCEL:current=null;changed();getParent().requestDisallowInterceptTouchEvent(false);invalidate();return true;}return true;}
  @Override public boolean performClick(){super.performClick();return true;}
  @Override protected void onDraw(Canvas c){super.onDraw(c);for(int i=0;i<paths.length();i++)draw(c,paths.optJSONArray(i));if(current!=null){JSONArray s=new JSONArray();s.put(erase?Color.WHITE:penColor);s.put(erase?24:penWidth);s.put(current);draw(c,s);}}
  private void draw(Canvas c,JSONArray s){if(s==null)return;JSONArray pts=s.optJSONArray(2);if(pts==null||pts.length()==0)return;p.setColor(s.optInt(0,penColor));p.setStrokeWidth(s.optInt(1,3)*getResources().getDisplayMetrics().density);Path path=new Path();
   for(int i=0;i<pts.length();i++){JSONArray pt=pts.optJSONArray(i);if(pt==null)continue;float x=(float)pt.optDouble(0)*getWidth(),y=(float)pt.optDouble(1)*getHeight();if(i==0){path.moveTo(x,y);path.lineTo(x+.1f,y+.1f);}else path.lineTo(x,y);}c.drawPath(path,p);}
 }
}
