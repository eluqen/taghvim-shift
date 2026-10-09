package com.eluqen.shiftcalendar.ui;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import java.lang.reflect.*;
import java.util.*;

public final class CompanyPlans {
 private static final String TAG="eluqen_company_selector";
 private static final String COUNT_TAG="eluqen_company_plan_count";
 private static final String ACTION_TAG="eluqen_company_actions";
 private static final String SELECT_TAG="eluqen_plan_selection_box";
 private static final String SAVE_TAG="eluqen_plan_save_button";
 private static final String STATUS_TAG="eluqen_plan_save_status";

 private static final WeakHashMap<Activity,Boolean> NEW_COMPANY=new WeakHashMap<>();
 private static final WeakHashMap<Activity,String> VIEW_COMPANY=new WeakHashMap<>();
 private static final WeakHashMap<Activity,Long> VIEW_PLAN=new WeakHashMap<>();

 private CompanyPlans(){}

 private static int dp(Activity a,int n){return Math.round(n*a.getResources().getDisplayMetrics().density);}
 private static Object db(Activity a)throws Exception{
  return Class.forName("com.eluqen.shiftcalendar.data.AppDatabase").getMethod("get",android.content.Context.class).invoke(null,a);
 }
 private static List<?> all(Activity a)throws Exception{
  Object d=db(a);return (List<?>)d.getClass().getMethod("plans").invoke(d);
 }
 private static Object savedActive(Activity a){
  try{return PlanManagement.activePlan(a);}catch(Exception e){return null;}
 }
 private static String company(Object p)throws Exception{return String.valueOf(p.getClass().getField("company").get(p));}
 private static String team(Object p)throws Exception{return String.valueOf(p.getClass().getField("team").get(p));}
 private static long id(Object p)throws Exception{return p.getClass().getField("id").getLong(p);}

 public static String label(Object plan){
  try{return team(plan);}catch(Exception e){return String.valueOf(plan);}
 }

 public static String selectedCompany(Activity a){
  try{
   String staged=VIEW_COMPANY.get(a);
   if(staged!=null&&!staged.trim().isEmpty()){
    for(Object p:all(a))if(staged.equals(company(p)))return staged;
   }
   Object active=savedActive(a);
   if(active!=null){
    String c=company(active);VIEW_COMPANY.put(a,c);return c;
   }
   for(Object p:all(a)){
    String c=company(p);
    if(!"جنرال".equals(c)){VIEW_COMPANY.put(a,c);return c;}
   }
  }catch(Exception e){android.util.Log.e("ELUQEN_COMPANY","Selected company",e);}
  return "";
 }

 public static Object shownPlan(Activity a){
  try{
   List<?> plans=all(a);
   Long staged=VIEW_PLAN.get(a);
   if(staged!=null){
    for(Object p:plans)if(id(p)==staged.longValue()){
     VIEW_COMPANY.put(a,company(p));return p;
    }
    VIEW_PLAN.remove(a);
   }
   String selected=selectedCompany(a);
   Object active=savedActive(a);
   if(active!=null&&selected.equals(company(active))){
    VIEW_PLAN.put(a,id(active));return active;
   }
   for(Object p:plans)if(selected.equals(company(p))){
    VIEW_PLAN.put(a,id(p));return p;
   }
   if(active!=null){
    VIEW_COMPANY.put(a,company(active));VIEW_PLAN.put(a,id(active));return active;
   }
   if(!plans.isEmpty()){
    Object p=plans.get(0);VIEW_COMPANY.put(a,company(p));VIEW_PLAN.put(a,id(p));return p;
   }
  }catch(Exception e){android.util.Log.e("ELUQEN_COMPANY","Shown plan",e);}
  return null;
 }

 public static boolean isShown(Activity a,Object plan){
  try{
   Object shown=shownPlan(a);return shown!=null&&plan!=null&&id(shown)==id(plan);
  }catch(Exception e){return false;}
 }

 public static void stagePlan(Activity a,Object plan){
  if(plan==null)return;
  try{
   VIEW_COMPANY.put(a,company(plan));VIEW_PLAN.put(a,id(plan));
   updateSelectionUi(a);
  }catch(Exception e){android.util.Log.e("ELUQEN_COMPANY","Stage plan",e);}
 }

 public static String defaultCompany(Activity a){
  try{
   if(Boolean.TRUE.equals(NEW_COMPANY.remove(a)))return "";
   return selectedCompany(a);
  }catch(Exception e){return "";}
 }

 public static List<?> filter(Activity a,List<?> plans){
  try{
   String selected=selectedCompany(a);
   ArrayList<Object> out=new ArrayList<>();
   for(Object p:plans)if(selected.equals(company(p)))out.add(p);
   if(!out.isEmpty())return out;

   Object active=savedActive(a);
   if(active!=null){
    String c=company(active);VIEW_COMPANY.put(a,c);VIEW_PLAN.put(a,id(active));
    for(Object p:plans)if(c.equals(company(p)))out.add(p);
   }
   if(!out.isEmpty())return out;

   if(!plans.isEmpty()){
    Object first=plans.get(0);String c=company(first);VIEW_COMPANY.put(a,c);VIEW_PLAN.put(a,id(first));
    for(Object p:plans)if(c.equals(company(p)))out.add(p);
   }
   return out;
  }catch(Exception e){android.util.Log.e("ELUQEN_COMPANY","Filter",e);return plans;}
 }

 private static void invoke(Activity a,String name){
  try{Method m=a.getClass().getDeclaredMethod(name);m.setAccessible(true);m.invoke(a);}
  catch(Exception e){android.util.Log.e("ELUQEN_COMPANY",name,e);}
 }

 private static void openEditor(Activity a,boolean newCompany){
  try{
   if(newCompany)NEW_COMPANY.put(a,true);else NEW_COMPANY.remove(a);
   Class<?> plan=Class.forName("com.eluqen.shiftcalendar.data.ShiftPlan");
   Method m=a.getClass().getDeclaredMethod("editPlan",plan);m.setAccessible(true);m.invoke(a,new Object[]{null});
  }catch(Exception e){NEW_COMPANY.remove(a);android.util.Log.e("ELUQEN_COMPANY","Open plan editor",e);}
 }
 private static void createCompany(Activity a){openEditor(a,true);}
 public static void createPattern(Activity a){openEditor(a,false);}

 public static void editShown(Activity a){
  try{
   Object plan=shownPlan(a);if(plan==null)return;
   Method m=a.getClass().getDeclaredMethod("editPlan",Class.forName("com.eluqen.shiftcalendar.data.ShiftPlan"));
   m.setAccessible(true);m.invoke(a,plan);
  }catch(Exception e){android.util.Log.e("ELUQEN_COMPANY","Edit shown",e);}
 }

 public static void afterPlanMutation(Activity a,Object plan){
  try{
   stagePlan(a,plan);
   invoke(a,"bindPlans");invoke(a,"refreshDetails");attach(a);
  }catch(Exception e){android.util.Log.e("ELUQEN_COMPANY","After mutation",e);}
 }

 public static void afterDelete(Activity a,String deletedCompany){
  try{
   VIEW_PLAN.remove(a);
   Object active=savedActive(a);
   List<?> plans=all(a);
   Object next=null;
   String wanted=deletedCompany;
   if(active!=null&&(wanted==null||wanted.equals(company(active))))next=active;
   if(next==null)for(Object p:plans)if(wanted!=null&&wanted.equals(company(p))){next=p;break;}
   if(next==null&&active!=null)next=active;
   if(next==null&&!plans.isEmpty())next=plans.get(0);
   if(next!=null)stagePlan(a,next);
   else{VIEW_COMPANY.remove(a);VIEW_PLAN.remove(a);}
   invoke(a,"bindPlans");invoke(a,"refreshDetails");attach(a);
  }catch(Exception e){android.util.Log.e("ELUQEN_COMPANY","After delete",e);}
 }

 private static Button findButton(View v,String contains){
  if(v instanceof Button&&((Button)v).getText().toString().contains(contains))return (Button)v;
  if(v instanceof ViewGroup){
   ViewGroup g=(ViewGroup)v;
   for(int i=0;i<g.getChildCount();i++){Button b=findButton(g.getChildAt(i),contains);if(b!=null)return b;}
  }
  return null;
 }

 private static View findTag(View v,String tag){
  if(tag.equals(v.getTag()))return v;
  if(v instanceof ViewGroup){
   ViewGroup g=(ViewGroup)v;
   for(int i=0;i<g.getChildCount();i++){View x=findTag(g.getChildAt(i),tag);if(x!=null)return x;}
  }
  return null;
 }

 private static GradientDrawable shape(Activity a,int fill,int stroke){
  GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(a,11));g.setStroke(dp(a,1),stroke);return g;
 }
 private static Button actionButton(Activity a,String title,int textColor,int fill,int stroke){
  Button b=new Button(a);b.setAllCaps(false);b.setText(title);b.setTextSize(12.5f);b.setTextColor(textColor);
  b.setMinHeight(dp(a,48));b.setMinimumHeight(dp(a,48));b.setMinimumWidth(0);b.setPadding(dp(a,10),dp(a,4),dp(a,10),dp(a,4));b.setGravity(Gravity.CENTER);b.setSingleLine(false);b.setBackground(shape(a,fill,stroke));return b;
 }
 private static void stylePair(Activity a,Button b){
  if(b==null)return;
  b.setAllCaps(false);b.setTextSize(12.5f);b.setGravity(Gravity.CENTER);b.setSingleLine(false);
  b.setMinHeight(0);b.setMinimumHeight(0);b.setTextColor(0xff17665c);b.setBackground(shape(a,0xffe7f1f0,0xff87b9b1));
  ViewGroup.LayoutParams raw=b.getLayoutParams();
  if(raw instanceof LinearLayout.LayoutParams){
   LinearLayout.LayoutParams lp=(LinearLayout.LayoutParams)raw;lp.height=dp(a,60);lp.setMargins(dp(a,3),0,dp(a,3),0);b.setLayoutParams(lp);
  }
 }

 private static void ensureSelectionBox(Activity a,LinearLayout card,Spinner spinner){
  if(findTag(card,SELECT_TAG)!=null){updateSelectionUi(a);return;}
  LinearLayout box=new LinearLayout(a);box.setTag(SELECT_TAG);box.setOrientation(LinearLayout.VERTICAL);
  box.setPadding(dp(a,6),dp(a,8),dp(a,6),dp(a,8));box.setBackground(shape(a,0xfff7fafb,0xffd7e2e8));

  TextView status=new TextView(a);status.setTag(STATUS_TAG);status.setGravity(Gravity.RIGHT);status.setTextSize(13);status.setMinLines(2);status.setTextColor(0xff526676);
  status.setPadding(dp(a,4),0,dp(a,4),dp(a,5));box.addView(status);

  Button save=actionButton(a,"ذخیره انتخاب الگو",0xffffffff,0xff17665c,0xff17665c);save.setTag(SAVE_TAG);
  save.setOnClickListener(v->saveSelection(a));box.addView(save,new LinearLayout.LayoutParams(-1,dp(a,50)));

  TextView note=new TextView(a);
  note.setText("ذخیره انتخاب، الگوی فعال را تغییر می‌دهد؛ افزودن، ویرایش و حذف همان لحظه اعمال می‌شوند.");
  note.setGravity(Gravity.RIGHT);note.setTextSize(11.5f);note.setTextColor(0xff6a7884);note.setPadding(dp(a,4),dp(a,6),dp(a,4),0);box.addView(note);

  int spinnerIndex=card.indexOfChild(spinner);
  int insert=Math.min(card.getChildCount(),Math.max(0,spinnerIndex+2));
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(a,6),0,dp(a,8));card.addView(box,insert,lp);
  updateSelectionUi(a);
 }

 private static void saveSelection(Activity a){
  try{
   Object shown=shownPlan(a);if(shown==null)return;
   Object d=db(a);d.getClass().getMethod("setActivePlan",long.class).invoke(d,id(shown));
   Class.forName("com.eluqen.shiftcalendar.data.Prefs").getMethod("setOnboardingComplete",android.content.Context.class,boolean.class).invoke(null,a,true);
   try{Class.forName("com.eluqen.shiftcalendar.widget.ShiftWidgetProvider").getMethod("updateAll",android.content.Context.class).invoke(null,a);}catch(Exception ignored){}
   try{
    boolean enabled=(Boolean)Class.forName("com.eluqen.shiftcalendar.data.Prefs").getMethod("reminderEnabled",android.content.Context.class).invoke(null,a);
    if(enabled)new Thread(()->{try{Class.forName("com.eluqen.shiftcalendar.reminder.ReminderScheduler").getMethod("sync",android.content.Context.class).invoke(null,a);}catch(Exception ignored){}}, "eluqen-save-selection").start();
   }catch(Exception ignored){}
   updateSelectionUi(a);refreshSelector(a);
   Toast.makeText(a,"انتخاب الگوی کاری ذخیره شد",Toast.LENGTH_SHORT).show();
  }catch(Exception e){android.util.Log.e("ELUQEN_COMPANY","Save selection",e);Toast.makeText(a,"ذخیره انتخاب ناموفق بود",Toast.LENGTH_LONG).show();}
 }

 private static void updateSelectionUi(Activity a){
  try{
   Field f=a.getClass().getDeclaredField("planSpinner");f.setAccessible(true);
   Spinner spinner=(Spinner)f.get(a);if(spinner==null||!(spinner.getParent() instanceof LinearLayout))return;
   LinearLayout card=(LinearLayout)spinner.getParent();
   Button save=(Button)findTag(card,SAVE_TAG);TextView status=(TextView)findTag(card,STATUS_TAG);
   if(save==null||status==null)return;

   Object shown=shownPlan(a),active=savedActive(a);
   String shownName=shown==null?"—":company(shown)+" / "+team(shown);
   String activeName=active==null?"انتخاب نشده":company(active)+" / "+team(active);
   boolean saved=shown!=null&&active!=null&&id(shown)==id(active);

   status.setText("در حال نمایش: "+shownName+"\nانتخاب ذخیره‌شده: "+activeName);
   if(saved){
    save.setText("انتخاب ذخیره‌شده");save.setEnabled(false);save.setAlpha(0.72f);
    save.setTextColor(0xff315e50);save.setBackground(shape(a,0xffe3f1ec,0xff9bc6b7));
   }else{
    save.setText("ذخیره انتخاب الگو");save.setEnabled(true);save.setAlpha(1f);
    save.setTextColor(0xffffffff);save.setBackground(shape(a,0xff17665c,0xff17665c));
   }
  }catch(Exception e){android.util.Log.e("ELUQEN_COMPANY","Update selection UI",e);}
 }

 private static void wirePatternAction(Activity a,LinearLayout card){
  Button add=findButton(card,"الگوی جدید");
  if(add!=null){add.setText("افزودن الگوی جدید");add.setOnClickListener(v->createPattern(a));stylePair(a,add);}

  Button edit=findButton(card,"ویرایش");
  if(edit!=null){edit.setText("ویرایش الگوی فعلی");edit.setOnClickListener(v->editShown(a));stylePair(a,edit);}

  Button del=findButton(card,"حذف الگوی نمایش‌داده‌شده");
  if(del==null)del=findButton(card,"حذف الگوی فعال");
  if(del==null)del=findButton(card,"حذف الگوی انتخاب‌شده");
  if(del!=null){del.setText("حذف الگوی نمایش‌داده‌شده");del.setOnClickListener(v->PlanManagement.confirmDeleteActive(a));}

  LinearLayout actions=null;
  for(int i=0;i<card.getChildCount();i++)if(ACTION_TAG.equals(card.getChildAt(i).getTag())&&card.getChildAt(i) instanceof LinearLayout)actions=(LinearLayout)card.getChildAt(i);
  if(actions==null){
   actions=new LinearLayout(a);actions.setTag(ACTION_TAG);actions.setOrientation(LinearLayout.VERTICAL);
   actions.setPadding(dp(a,10),dp(a,12),dp(a,10),dp(a,10));
   actions.setBackground(shape(a,0xfffaf8f8,0xffe5dddd));
   TextView dangerTitle=new TextView(a);dangerTitle.setText("حذف و بازنشانی الگوها");dangerTitle.setTextColor(0xff80535b);dangerTitle.setTextSize(13);dangerTitle.setTypeface(Typeface.DEFAULT,Typeface.BOLD);dangerTitle.setGravity(Gravity.RIGHT);dangerTitle.setPadding(0,0,0,dp(a,10));actions.addView(dangerTitle);
   Button deleteCompany=actionButton(a,"حذف این محل کار و همه الگوهایش",0xff9d2e2e,0xffffeeee,0xffd88989);
   deleteCompany.setOnClickListener(v->PlanManagement.confirmDeleteCompany(a));actions.addView(deleteCompany,new LinearLayout.LayoutParams(-1,dp(a,56)));
   Button reset=actionButton(a,"بازگردانی الگوهای پیش‌فرض",0xff704500,0xfffff4df,0xffd8b16e);
   reset.setOnClickListener(v->PlanManagement.confirmReset(a,null));LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,dp(a,56));rp.setMargins(0,dp(a,7),0,0);actions.addView(reset,rp);
   TextView note=new TextView(a);note.setText("کارکرد، یادداشت‌ها و تنظیمات یادآوری حفظ می‌شوند. هر عملیات نیاز به تأیید دارد.");
   note.setTextSize(11.5f);note.setTextColor(0xff6b7887);note.setGravity(Gravity.RIGHT);note.setPadding(dp(a,4),dp(a,6),dp(a,4),0);actions.addView(note);
   LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,-2);ap.setMargins(0,dp(a,16),0,0);card.addView(actions,ap);
  }
  if(del!=null){
   del.setTextColor(0xff9d2e2e);del.setTextSize(12.5f);del.setAllCaps(false);
   del.setBackground(shape(a,0xffffeeee,0xffe1bfc3));
   del.setPadding(dp(a,8),dp(a,4),dp(a,8),dp(a,4));del.setSingleLine(false);
   if(del.getParent()!=actions){
    if(del.getParent() instanceof ViewGroup)((ViewGroup)del.getParent()).removeView(del);
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(a,56));lp.setMargins(0,0,0,dp(a,7));actions.addView(del,1,lp);
   }
  }
 }

 private static void countLabel(Activity a,LinearLayout card,String selected,int count){
  TextView t=null;
  for(int i=0;i<card.getChildCount();i++)if(COUNT_TAG.equals(card.getChildAt(i).getTag())&&card.getChildAt(i) instanceof TextView)t=(TextView)card.getChildAt(i);
  if(t==null){
   t=new TextView(a);t.setTag(COUNT_TAG);t.setGravity(Gravity.RIGHT);t.setTextSize(12.5f);t.setTextColor(0xff617586);t.setPadding(dp(a,4),dp(a,3),dp(a,4),dp(a,5));
   card.addView(t,Math.min(3,card.getChildCount()));
  }
  t.setText(com.eluqen.shiftcalendar.core.PersianDigits.of("الگوهای این محل کار: "+count+" مورد"));
 }

 private static void choose(Activity a,String target){
  try{
   List<?> plans=all(a);Object active=savedActive(a),first=null;
   if(active!=null&&target.equals(company(active)))first=active;
   if(first==null)for(Object p:plans)if(target.equals(company(p))){first=p;break;}
   if(first==null)return;
   VIEW_COMPANY.put(a,target);VIEW_PLAN.put(a,id(first));
   invoke(a,"bindPlans");invoke(a,"refreshDetails");attach(a);
  }catch(Exception e){android.util.Log.e("ELUQEN_COMPANY","Browse company",e);}
 }

 private static void refreshSelector(Activity a){
  try{
   Field f=a.getClass().getDeclaredField("planSpinner");f.setAccessible(true);
   Spinner spinner=(Spinner)f.get(a);
   if(spinner!=null&&spinner.getParent() instanceof LinearLayout){
    LinearLayout card=(LinearLayout)spinner.getParent();
    for(int i=0;i<card.getChildCount();i++)if(TAG.equals(card.getChildAt(i).getTag())&&card.getChildAt(i) instanceof HorizontalScrollView){
     refreshButtons(a,(HorizontalScrollView)card.getChildAt(i));break;
    }
   }
  }catch(Exception ignored){}
 }

 public static void attach(Activity a){
  try{
   Field f=a.getClass().getDeclaredField("planSpinner");f.setAccessible(true);
   Spinner spinner=(Spinner)f.get(a);if(spinner==null||!(spinner.getParent() instanceof LinearLayout))return;
   LinearLayout card=(LinearLayout)spinner.getParent();

   HorizontalScrollView existing=null;
   for(int i=0;i<card.getChildCount();i++)if(TAG.equals(card.getChildAt(i).getTag())&&card.getChildAt(i) instanceof HorizontalScrollView)existing=(HorizontalScrollView)card.getChildAt(i);

   if(existing==null){
    TextView title=new TextView(a);title.setText("شرکت / محل کار");title.setGravity(Gravity.RIGHT);title.setTextSize(14);title.setTextColor(0xff53677f);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
    title.setPadding(dp(a,4),dp(a,2),dp(a,4),dp(a,5));card.addView(title,0);
    HorizontalScrollView sc=new HorizontalScrollView(a);sc.setTag(TAG);sc.setHorizontalScrollBarEnabled(false);
    LinearLayout row=new LinearLayout(a);row.setOrientation(LinearLayout.HORIZONTAL);row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);sc.addView(row);
    card.addView(sc,1,new LinearLayout.LayoutParams(-1,dp(a,56)));existing=sc;
   }

   ensureSelectionBox(a,card,spinner);
   wirePatternAction(a,card);
   refreshButtons(a,existing);
   updateSelectionUi(a);
  }catch(Exception e){android.util.Log.e("ELUQEN_COMPANY","Attach selector",e);}
 }

 private static void refreshButtons(Activity a,HorizontalScrollView sc){
  try{
   LinearLayout row=(LinearLayout)sc.getChildAt(0);row.removeAllViews();
   List<?> plans=all(a);String selected=selectedCompany(a);
   LinkedHashSet<String> companies=new LinkedHashSet<>();
   for(Object p:plans)companies.add(company(p));
   int selectedCount=0;for(Object p:plans)if(selected.equals(company(p)))selectedCount++;

   if(sc.getParent() instanceof LinearLayout){
    LinearLayout card=(LinearLayout)sc.getParent();wirePatternAction(a,card);countLabel(a,card,selected,selectedCount);
   }

   for(String name:companies){
    if("جنرال".equals(name))continue;
    Button b=new Button(a);b.setAllCaps(false);b.setText(name);b.setTextSize(12);
    boolean on=name.equals(selected);b.setTextColor(on?0xffffffff:0xff17665c);b.setBackground(shape(a,on?0xff17665c:0xffe7f1f0,0xff87b9b1));
    b.setOnClickListener(v->choose(a,name));
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,dp(a,48));lp.setMargins(dp(a,3),dp(a,3),dp(a,3),dp(a,3));row.addView(b,lp);
   }

   Button add=new Button(a);add.setAllCaps(false);add.setText("+ محل کار جدید");add.setTextSize(12);add.setTextColor(0xff7b4c19);add.setBackground(shape(a,0xfffff4e5,0xffd9aa70));
   add.setOnClickListener(v->createCompany(a));
   LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,dp(a,48));lp.setMargins(dp(a,3),dp(a,3),dp(a,3),dp(a,3));row.addView(add,lp);
  }catch(Exception e){android.util.Log.e("ELUQEN_COMPANY","Refresh companies",e);}
 }
}
