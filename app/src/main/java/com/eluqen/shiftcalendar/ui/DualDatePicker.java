package com.eluqen.shiftcalendar.ui;
import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;
import java.lang.reflect.*;
import java.util.*;
public final class DualDatePicker {
 public interface Result { void selected(Object date); }
 private static final String CORE="com.eluqen.shiftcalendar.core.";
 private static final String[] GM={"January","February","March","April","May","June","July","August","September","October","November","December"};
 private DualDatePicker(){}
 private static int dp(Context c,float n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
 private static int v(Object item,String field)throws Exception{return item.getClass().getField(field).getInt(item);}
 private static Object persian(int y,int m,int d)throws Exception{return Class.forName(CORE+"PersianDate").getConstructor(int.class,int.class,int.class).newInstance(y,m,d);}
 private static Object greg(Object p)throws Exception{return Class.forName(CORE+"JalaliCalendar").getMethod("toGregorian",Class.forName(CORE+"PersianDate")).invoke(null,p);}
 private static Object fromGregorian(int y,int m,int d)throws Exception{
  GregorianCalendar check=new GregorianCalendar();check.setLenient(false);check.clear();check.set(y,m-1,d);check.getTime();
  return Class.forName(CORE+"JalaliCalendar").getMethod("fromGregorian",int.class,int.class,int.class).invoke(null,y,m,d);
 }
 private static Object hijri(Context c,Object p)throws Exception{
  Class<?> resolver=Class.forName("com.eluqen.shiftcalendar.hijri.HijriResolver");
  return resolver.getMethod("resolve",Context.class,Class.forName(CORE+"PersianDate")).invoke(null,c,p);
 }
 private static Object fromHijri(Context c,int y,int m,int d)throws Exception{
  Class<?> resolver=Class.forName("com.eluqen.shiftcalendar.hijri.HijriResolver");
  return resolver.getMethod("fromHijri",Context.class,int.class,int.class,int.class).invoke(null,c,y,m,d);
 }
 private static int days(Activity a,int mode,int y,int m)throws Exception{
  if(mode==0)return (Integer)Class.forName(CORE+"JalaliCalendar").getMethod("daysInMonth",int.class,int.class).invoke(null,y,m);
  if(mode==1)return new GregorianCalendar(y,m-1,1).getActualMaximum(Calendar.DAY_OF_MONTH);
  Class<?> resolver=Class.forName("com.eluqen.shiftcalendar.hijri.HijriResolver");
  return (Integer)resolver.getMethod("daysInMonth",Context.class,int.class,int.class).invoke(null,a,y,m);
 }
 private static GradientDrawable shape(int color,int radius,Context c){
  GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(c,radius));return d;
 }
 private static TextView label(Activity a,String value,int size,int color){
  TextView t=new TextView(a);t.setText(value);t.setTextSize(TypedValue.COMPLEX_UNIT_SP,size);t.setTextColor(color);
  t.setGravity(Gravity.CENTER);t.setIncludeFontPadding(false);return t;
 }
 private static NumberPicker wheel(Activity a,int min,int max,int current){
  NumberPicker n=new NumberPicker(a);n.setMinValue(min);n.setMaxValue(max);n.setValue(Math.max(min,Math.min(max,current)));
  n.setWrapSelectorWheel(false);n.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
  try{
   int id=a.getResources().getIdentifier("numberpicker_input","id","android");EditText input=n.findViewById(id);
   if(input!=null){input.setTextSize(TypedValue.COMPLEX_UNIT_SP,23);input.setTextColor(Ui.SECONDARY);input.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
    input.setGravity(Gravity.CENTER);input.setInputType(InputType.TYPE_NULL);input.setFocusable(false);}
  }catch(Exception ignored){}
  if(android.os.Build.VERSION.SDK_INT>=29){n.setTextColor(Ui.TEXT);n.setTextSize(dp(a,18));n.setSelectionDividerHeight(dp(a,2));}
  return n;
 }
 public static void show(Activity a,Object current,Object callback){
  if(current==null)return;
  try{new Dialog(a,current,callback).open();}
  catch(Exception e){android.util.Log.e("ELUQEN_DATE","Opening picker",e);Toast.makeText(a,"انتخابگر تاریخ در دسترس نیست",Toast.LENGTH_LONG).show();}
 }
 private static final class Dialog {
  final Activity a;final Object callback;Object date;
  int mode=0,hover=0;boolean binding=false;
  NumberPicker day,month,year;TextView equivalent,selected;LinearLayout tabs;
  TextView[] tabText=new TextView[3];View[] tabLine=new View[3];LinearLayout[] tabBox=new LinearLayout[3];
  int wheelHeight,frameHeight;Runnable refreshToday;
  Dialog(Activity a,Object date,Object callback){this.a=a;this.date=date;this.callback=callback;}
  void open()throws Exception{
   android.util.DisplayMetrics dm=a.getResources().getDisplayMetrics();
   int availableH=Math.max(dp(a,480),dm.heightPixels-dp(a,54));
   wheelHeight=Math.max(dp(a,154),Math.min(dp(a,220),(int)(availableH*.32f)));
   frameHeight=wheelHeight+dp(a,29);
   LinearLayout body=new LinearLayout(a);body.setOrientation(LinearLayout.VERTICAL);body.setBackground(shape(Ui.CARD,22,a));
   body.setPadding(dp(a,16),dp(a,13),dp(a,16),dp(a,10));
   LinearLayout head=new LinearLayout(a);head.setGravity(Gravity.CENTER_VERTICAL);head.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
   TextView heading=label(a,"انتخاب تاریخ",20,Ui.TEXT);heading.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
   head.addView(heading,new LinearLayout.LayoutParams(0,dp(a,36),1));
   TextView today=label(a,"⌖ امروز",12,Ui.SECONDARY);today.setBackground(shape(Ui.SURFACE_ALT,10,a));
   head.addView(today,new LinearLayout.LayoutParams(dp(a,76),dp(a,34)));
   body.addView(head,new LinearLayout.LayoutParams(-1,dp(a,38)));
   Runnable todayVisibility=()->{try{
    Object now=Class.forName(CORE+"PersianDate").getMethod("todayTehran").invoke(null);
    today.setVisibility(date.equals(now)?View.GONE:View.VISIBLE);
   }catch(Exception e){today.setVisibility(View.GONE);}};
   refreshToday=todayVisibility;today.setOnClickListener(v->{try{
    date=Class.forName(CORE+"PersianDate").getMethod("todayTehran").invoke(null);render();todayVisibility.run();
   }catch(Exception e){android.util.Log.e("ELUQEN_DATE","Today selection failed",e);}});

   TextView subtitle=label(a,"روز، ماه و سال را بالا یا پایین بکشید",12,Ui.MUTED);body.addView(subtitle,new LinearLayout.LayoutParams(-1,dp(a,26)));
   tabs=new LinearLayout(a);tabs.setOrientation(android.widget.LinearLayout.HORIZONTAL);tabs.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);tabs.setBackground(shape(Ui.SURFACE_ALT,13,a));
   LinearLayout.LayoutParams tlp=new LinearLayout.LayoutParams(-1,dp(a,53));tlp.setMargins(0,dp(a,7),0,dp(a,10));body.addView(tabs,tlp);
   String[] names={"شمسی","میلادی","قمری"};
   for(int i=0;i<3;i++){tabBox[i]=tab(names[i],i);tabs.addView(tabBox[i],new LinearLayout.LayoutParams(0,-1,1));}
   tabs.setOnTouchListener((view,event)->handleTabs(event));
   selected=label(a,"",17,Ui.TEXT);selected.setTypeface(Typeface.DEFAULT,Typeface.BOLD);body.addView(selected,new LinearLayout.LayoutParams(-1,dp(a,38)));
   FrameLayout wheelBox=new FrameLayout(a);body.addView(wheelBox,new LinearLayout.LayoutParams(-1,frameHeight));
   View highlight=new View(a);highlight.setBackground(shape(Ui.SURFACE_ALT,12,a));
   FrameLayout.LayoutParams hp=new FrameLayout.LayoutParams(-1,dp(a,52),Gravity.CENTER_VERTICAL);hp.setMargins(dp(a,2),0,dp(a,2),0);
   wheelBox.addView(highlight,hp);highlight.setTranslationY(dp(a,14.5f));
   LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.CENTER);row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);wheelBox.addView(row,new FrameLayout.LayoutParams(-1,-1));
   Object initial=date;day=wheel(a,1,31,v(initial,"day"));month=wheel(a,1,12,v(initial,"month"));year=wheel(a,1200,1700,v(initial,"year"));
   row.addView(column("روز",day),new LinearLayout.LayoutParams(0,-1,.82f));row.addView(column("ماه",month),new LinearLayout.LayoutParams(0,-1,1.46f));row.addView(column("سال",year),new LinearLayout.LayoutParams(0,-1,1.05f));
   equivalent=label(a,"",13,Ui.MUTED);equivalent.setBackground(shape(Ui.CARD,10,a));
   LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(-1,dp(a,68));ep.setMargins(0,dp(a,8),0,dp(a,9));body.addView(equivalent,ep);
   LinearLayout actions=new LinearLayout(a);actions.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);body.addView(actions,new LinearLayout.LayoutParams(-1,dp(a,50)));
   TextView accept=label(a,"تأیید تاریخ",16,Color.WHITE);accept.setTypeface(Typeface.DEFAULT,Typeface.BOLD);accept.setBackground(shape(Ui.SECONDARY,13,a));
   actions.addView(accept,new LinearLayout.LayoutParams(0,-1,1));
   TextView cancel=label(a,"انصراف",15,Ui.TEXT);cancel.setBackground(shape(Ui.SURFACE_ALT,13,a));
   LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,-1,1);cp.setMargins(dp(a,8),0,0,0);actions.addView(cancel,cp);
   ScrollView scroll=new ScrollView(a);scroll.setFillViewport(true);scroll.addView(body);
   AlertDialog dialog=new AlertDialog.Builder(a).setView(scroll).create();
   accept.setOnClickListener(v->{if(capture()&&deliver())dialog.dismiss();});cancel.setOnClickListener(v->dialog.dismiss());
   NumberPicker.OnValueChangeListener change=(picker,oldValue,newValue)->{if(!binding)capture();};
   day.setOnValueChangedListener(change);month.setOnValueChangedListener(change);year.setOnValueChangedListener(change);
   render();dialog.show();
   Window w=dialog.getWindow();if(w!=null){
    int maxW=Math.min(dm.widthPixels-dp(a,16),dp(a,450));
    int maxH=Math.min(dm.heightPixels-dp(a,34),dp(a,650));
    w.setBackgroundDrawableResource(android.R.color.transparent);w.setLayout(maxW,maxH);w.setGravity(Gravity.CENTER);
    w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
   }
  }
  LinearLayout tab(String name,int index){
   LinearLayout box=new LinearLayout(a);box.setOrientation(android.widget.LinearLayout.VERTICAL);box.setGravity(Gravity.CENTER);box.setClickable(false);
   TextView title=label(a,name,15,Ui.TEXT);View line=new View(a);line.setBackground(shape(Ui.SECONDARY,3,a));
   box.addView(title,new LinearLayout.LayoutParams(-1,dp(a,40)));box.addView(line,new LinearLayout.LayoutParams(dp(a,60),dp(a,3)));
   tabText[index]=title;tabLine[index]=line;return box;
  }
 boolean handleTabs(MotionEvent e){
   int action=e.getActionMasked();if(action!=MotionEvent.ACTION_DOWN&&action!=MotionEvent.ACTION_MOVE&&action!=MotionEvent.ACTION_UP&&action!=MotionEvent.ACTION_CANCEL)return true;
   if(action==MotionEvent.ACTION_CANCEL){hover=mode;paintTabs();return true;}
   int width=Math.max(1,tabs.getWidth()/3);float logical=tabs.getLayoutDirection()==View.LAYOUT_DIRECTION_RTL?tabs.getWidth()-e.getX():e.getX();
   int candidate=Math.max(0,Math.min(2,(int)(logical/width)));hover=candidate;paintTabs();
   if(action==MotionEvent.ACTION_UP){if(candidate!=mode&&capture()){mode=candidate;hover=mode;render();}else{hover=mode;paintTabs();}}
   return true;
  }
  void paintTabs(){
   for(int i=0;i<3;i++){
    boolean active=i==mode,pointed=i==hover;
    tabBox[i].setBackground(pointed?shape(active?Ui.SURFACE_ALT:Ui.CARD,11,a):null);
    tabText[i].setTextColor(pointed?Ui.SECONDARY:(active?Ui.SECONDARY:Ui.MUTED));
    tabText[i].setTypeface(Typeface.DEFAULT,(active||pointed)?Typeface.BOLD:Typeface.NORMAL);
    tabLine[i].setVisibility(active?View.VISIBLE:View.INVISIBLE);
    tabBox[i].setScaleX(pointed?1f:.98f);tabBox[i].setScaleY(pointed?1f:.98f);
   }
  }
  LinearLayout column(String name,NumberPicker n){
   LinearLayout box=new LinearLayout(a);box.setOrientation(android.widget.LinearLayout.VERTICAL);box.setGravity(Gravity.CENTER);
   box.addView(label(a,name,12,Ui.MUTED),new LinearLayout.LayoutParams(-1,dp(a,29)));box.addView(n,new LinearLayout.LayoutParams(-1,wheelHeight));return box;
  }
  void render(){
   try{
    binding=true;Object shown=mode==0?date:mode==1?greg(date):hijri(a,date);
    int yy=v(shown,"year"),mm=v(shown,"month"),dd=v(shown,"day");
    month.setDisplayedValues(null);month.setMinValue(1);month.setMaxValue(12);
    String[] names=mode==0?(String[])Class.forName(CORE+"PersianDate").getField("MONTH_NAMES").get(null):mode==1?GM:(String[])Class.forName(CORE+"HijriDate").getField("MONTH_NAMES").get(null);
    month.setDisplayedValues(names);
    if(mode==0){year.setMinValue(1200);year.setMaxValue(1700);}
    else if(mode==1){year.setMinValue(1821);year.setMaxValue(2321);}
    else{year.setMinValue(1200);year.setMaxValue(1900);}
    year.setValue(yy);month.setValue(mm);day.setMaxValue(days(a,mode,yy,mm));day.setValue(Math.min(dd,day.getMaxValue()));
    hover=mode;paintTabs();summary();if(refreshToday!=null)refreshToday.run();
   }catch(Exception e){android.util.Log.e("ELUQEN_DATE","Render",e);}finally{binding=false;}
  }
 boolean capture(){
   try{
    int yy=year.getValue(),mm=month.getValue(),max=days(a,mode,yy,mm),dd=Math.min(day.getValue(),max);
    binding=true;day.setMaxValue(max);day.setValue(dd);binding=false;
    date=mode==0?persian(yy,mm,dd):mode==1?fromGregorian(yy,mm,dd):fromHijri(a,yy,mm,dd);
    summary();if(refreshToday!=null)refreshToday.run();return true;
   }catch(Exception e){binding=false;android.util.Log.e("ELUQEN_DATE","Capture",e);Toast.makeText(a,"تاریخ انتخاب‌شده معتبر نیست",Toast.LENGTH_SHORT).show();return false;}
  }
  void summary()throws Exception{
   Object shown=mode==0?date:mode==1?greg(date):hijri(a,date);
   String prefix=mode==0?"شمسی":mode==1?"میلادی":"قمری";
   selected.setText(prefix+": "+v(shown,"day")+" / "+v(shown,"month")+" / "+v(shown,"year"));
   equivalent.setText("شمسی: "+date.toString()+"\nمیلادی: "+greg(date).toString()+"   ·   قمری: "+hijri(a,date).toString());
  }
  boolean deliver(){
   try{
    Method result=null;for(Method m:callback.getClass().getMethods())if(m.getName().equals("onPicked")||m.getName().equals("selected")){result=m;break;}
    if(result==null)for(Method m:callback.getClass().getDeclaredMethods())if(m.getName().equals("onPicked")||m.getName().equals("selected")){result=m;break;}
    if(result==null)throw new NoSuchMethodException("date callback");result.setAccessible(true);result.invoke(callback,date);return true;
   }catch(Exception e){android.util.Log.e("ELUQEN_DATE","Deliver",e);Toast.makeText(a,"اعمال تاریخ انجام نشد",Toast.LENGTH_LONG).show();return false;}
  }
 }
}
