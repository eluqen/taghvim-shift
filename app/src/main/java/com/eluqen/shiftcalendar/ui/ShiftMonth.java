package com.eluqen.shiftcalendar.ui;
import android.app.*;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.text.style.ForegroundColorSpan;
import android.view.*;
import android.widget.*;
import java.lang.reflect.*;
import java.util.*;
import com.eluqen.shiftcalendar.data.AppDatabase;
import com.eluqen.shiftcalendar.data.ShiftPlan;
public final class ShiftMonth {
 private static final WeakHashMap<Activity,Long> selected=new WeakHashMap<>();
 private static final String PICKER="eluqen_four_temporary_plan";
 private ShiftMonth(){}
 private static int dp(Activity a,int n){return Math.round(a.getResources().getDisplayMetrics().density*n);}
 public static void reset(Activity a){selected.remove(a);}
 public static Object selected(Activity a,Object active){
  Long id=selected.get(a);if(id==null)return active;
  try{List<?> plans=(List<?>)OfficialHoliday.db(a).getClass().getMethod("plans").invoke(OfficialHoliday.db(a));
   for(Object plan:plans)if(plan.getClass().getField("id").getLong(plan)==id)return plan;
  }catch(Exception error){android.util.Log.e("ELUQEN_FOUR","Temporary plan",error);}
  selected.remove(a);return active;
 }
 public static List<?> ensureIncluded(Activity a,List<?> shown,Object chosen){
  if(shown.contains(chosen))return shown;
  try{
   long key=chosen.getClass().getField("id").getLong(chosen);
   for(Object item:shown)if(item.getClass().getField("id").getLong(item)==key)return shown;
   ArrayList<Object> result=new ArrayList<>(shown);
   if(result.size()>=4)result.set(result.size()-1,chosen);else result.add(chosen);
   return result;
  }catch(Exception e){return shown;}
 }
 private static void invokeRender(Activity a){
  try{Method method=a.getClass().getDeclaredMethod("render");method.setAccessible(true);method.invoke(a);}
  catch(Exception e){android.util.Log.e("ELUQEN_FOUR","Refresh temporary plan",e);}
 }
 public static void picker(Activity a){
  try{
   Field f=a.getClass().getDeclaredField("companyTitle");f.setAccessible(true);
   TextView heading=(TextView)f.get(a);
   if(!(heading.getParent() instanceof LinearLayout))return;
   LinearLayout root=(LinearLayout)heading.getParent();

   // Clean any legacy nested full-width picker button from older builds.
   for(int i=root.getChildCount()-1;i>=0;i--){
    View child=root.getChildAt(i);
    if(PICKER.equals(child.getTag()))root.removeViewAt(i);
   }

   final String hintTag="eluqen_four_picker_hint";
   TextView hint=null;
   for(int i=0;i<root.getChildCount();i++){
    View child=root.getChildAt(i);
    if(hintTag.equals(child.getTag())&&child instanceof TextView){
     hint=(TextView)child;break;
    }
   }
   if(hint==null){
    hint=pickerText(a,"تغییر گروه  ▾",12.5f,Ui.SECONDARY,true);
    hint.setTag(hintTag);
    hint.setGravity(Gravity.CENTER);
    hint.setPadding(dp(a,8),0,dp(a,8),0);
    hint.setBackground(pickerShape(a,Ui.SURFACE_ALT,Ui.SECONDARY,11));
    LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(dp(a,94),dp(a,34));
    hp.setMargins(dp(a,8),0,0,0);
    root.addView(hint,hp);
   }

   heading.setPadding(dp(a,4),0,dp(a,8),0);
   root.setClickable(true);root.setFocusable(true);
   root.setContentDescription("انتخاب گروه نمایشی چهارشیفت");
   root.setBackground(pickerShape(a,Ui.CARD,Ui.SECONDARY,16));
   root.setElevation(dp(a,2));
   root.setOnClickListener(v->choose(a));
  }catch(Exception e){android.util.Log.e("ELUQEN_FOUR","Picker",e);}
 }
 private static GradientDrawable pickerShape(Activity a,int fill,int stroke,int radius){
  GradientDrawable g=new GradientDrawable();
  g.setColor(fill);g.setCornerRadius(dp(a,radius));g.setStroke(dp(a,1),stroke);return g;
 }
 private static TextView pickerText(Activity a,String value,float size,int color,boolean bold){
  TextView t=new TextView(a);t.setText(value);t.setTextSize(size);t.setTextColor(color);
  t.setGravity(Gravity.CENTER_VERTICAL);t.setIncludeFontPadding(false);
  if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
  return t;
 }
 private static void choose(Activity a){
  try{
   AppDatabase database=AppDatabase.get(a);
   ShiftPlan defaultPlan=database.activePlan();
   List<ShiftPlan> plans=new ArrayList<>();
   for(ShiftPlan plan:database.plans()){
    if(defaultPlan.company.equals(plan.company))plans.add(plan);
   }
   Collections.sort(plans,(left,right)->left.team.compareTo(right.team));
   if(plans.size()>4)plans=new ArrayList<>(plans.subList(0,4));

   Object temporary=selected(a,defaultPlan);
   long temporaryId=((ShiftPlan)temporary).id;

   final Dialog dialog=new Dialog(a);
   dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

   LinearLayout panel=new LinearLayout(a);
   panel.setOrientation(LinearLayout.VERTICAL);
   panel.setPadding(dp(a,12),dp(a,12),dp(a,12),dp(a,12));
   panel.setBackground(pickerShape(a,Ui.CARD,Ui.BORDER,20));

   for(ShiftPlan plan:plans){
    final boolean isDefault=plan.id==defaultPlan.id;
    final boolean isShown=plan.id==temporaryId;

    LinearLayout item=new LinearLayout(a);
    item.setOrientation(LinearLayout.HORIZONTAL);
    // Separate LTR code and RTL Persian badge prevent bidi reordering.
    item.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
    item.setGravity(Gravity.CENTER_VERTICAL);
    item.setPadding(dp(a,10),dp(a,7),dp(a,10),dp(a,7));
    final int defaultFill=0xffdff3e7;
    final int defaultStroke=0xff27865b;
    item.setBackground(pickerShape(
      a,isDefault?defaultFill:(isShown?Ui.SURFACE_ALT:Ui.SURFACE),
      isDefault?defaultStroke:(isShown?Ui.SECONDARY:Ui.BORDER),15));
    item.setElevation(dp(a,(isDefault||isShown)?3:1));
    item.setClickable(true);item.setFocusable(true);

    TextView code=pickerText(
      a,plan.team,22f,
      isDefault?0xff1f6f4a:(isShown?Ui.SECONDARY:Ui.TEXT),true);
    code.setGravity(Gravity.CENTER);
    code.setTextDirection(View.TEXT_DIRECTION_LTR);
    code.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
    code.setBackground(pickerShape(
      a,isDefault?0xffedf9f2:(isShown?Ui.CARD:Ui.SURFACE_ALT),
      isDefault?defaultStroke:(isShown?Ui.SECONDARY:Ui.BORDER),12));
    item.addView(code,new LinearLayout.LayoutParams(dp(a,64),dp(a,50)));

    final String badgeText=isDefault?"گروه پیش‌فرض":
      (isShown?"گروه انتخابی":"");
    TextView badge=pickerText(
      a,badgeText,18f,
      isDefault?0xff1f6f4a:(isShown?Ui.SECONDARY:Ui.MUTED),
      isDefault||isShown);
    badge.setTextDirection(View.TEXT_DIRECTION_RTL);
    badge.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
    badge.setGravity(Gravity.CENTER_VERTICAL|Gravity.RIGHT);
    badge.setPadding(dp(a,12),0,dp(a,12),0);
    if(isDefault){
     badge.setBackground(pickerShape(a,0xffedf9f2,defaultStroke,12));
    }else if(isShown){
     badge.setBackground(pickerShape(a,Ui.CARD,Ui.SECONDARY,12));
    }
    LinearLayout.LayoutParams badgeLp=new LinearLayout.LayoutParams(0,dp(a,50),1f);
    badgeLp.leftMargin=dp(a,12);
    item.addView(badge,badgeLp);

    item.setContentDescription(
      "گروه "+plan.team
      +(isDefault?"، گروه پیش‌فرض":(isShown?"، گروه انتخابی":"")));
    item.setOnClickListener(v->{
     selected.put(a,plan.id);
     dialog.dismiss();
     invokeRender(a);
    });

    LinearLayout.LayoutParams itemLp=new LinearLayout.LayoutParams(-1,dp(a,66));
    itemLp.setMargins(0,dp(a,4),0,dp(a,4));
    panel.addView(item,itemLp);
   }

   dialog.setContentView(panel);
   dialog.setCanceledOnTouchOutside(true);
   dialog.setOnShowListener(ignored->{
    Window w=dialog.getWindow();if(w==null)return;
    w.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
    WindowManager.LayoutParams attrs=w.getAttributes();
    attrs.width=(int)(a.getResources().getDisplayMetrics().widthPixels*.82f);
    attrs.height=WindowManager.LayoutParams.WRAP_CONTENT;
    attrs.dimAmount=.34f;
    w.setAttributes(attrs);
    w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
   });
   dialog.show();
  }catch(Exception e){android.util.Log.e("ELUQEN_FOUR","Choices",e);}
 }
 public static int monthHolidayCount(Activity a,int year,int month){
  int count=0;
  try{
   Class<?> calendar=Class.forName("com.eluqen.shiftcalendar.core.JalaliCalendar");
   int days=(Integer)calendar.getMethod("daysInMonth",int.class,int.class).invoke(null,year,month);
   for(int d=1;d<=days;d++){
    Object date=OfficialHoliday.date(year,month,d);
    if(!OfficialHoliday.friday(date)&&OfficialHoliday.official(a,date))count++;
   }
  }catch(Exception e){android.util.Log.e("ELUQEN_FOUR","Month holiday count",e);}
  return count;
 }
 private static GradientDrawable box(Activity a,int fill,int stroke){
  GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(a,11));g.setStroke(dp(a,1),stroke);return g;
 }
 private static TextView metric(Activity a,String title,String value,int fill,int ink){
  TextView t=new TextView(a);
  t.setText(OfficialHoliday.fa(title+": "+value));
  t.setTextSize(13.5f);t.setTextColor(ink);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
  t.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);t.setPadding(dp(a,10),dp(a,8),dp(a,10),dp(a,8));
  t.setBackground(box(a,fill,ink));return t;
 }
 private static void pair(Activity a,LinearLayout panel,View right,View left){
  LinearLayout row=new LinearLayout(a);row.setOrientation(LinearLayout.HORIZONTAL);row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
  LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(0,-2,1);rp.setMargins(dp(a,3),dp(a,3),dp(a,3),dp(a,3));
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMargins(dp(a,3),dp(a,3),dp(a,3),dp(a,3));
  row.addView(right,rp);row.addView(left,lp);panel.addView(row,new LinearLayout.LayoutParams(-1,-2));
 }
 private static void full(Activity a,LinearLayout panel,View item){
  LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(dp(a,3),dp(a,3),dp(a,3),dp(a,3));panel.addView(item,p);
 }
 private static TextView explanation(Activity a,String label,String detail,int color){
  String prefix=label+":";
  SpannableString s=new SpannableString(prefix+" "+detail);
  s.setSpan(new StyleSpan(Typeface.BOLD),0,prefix.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
  s.setSpan(new ForegroundColorSpan(color),0,prefix.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
  TextView t=new TextView(a);t.setText(s);t.setTextSize(11.5f);t.setGravity(Gravity.RIGHT);t.setTextColor(Ui.TEXT);
  t.setPadding(dp(a,5),dp(a,4),dp(a,5),0);return t;
 }
 public static void footer(Activity a,Object month,LinearLayout table){
  try{
   Object chosen=selected(a,OfficialHoliday.active(a));
   int y=OfficialHoliday.num(month,"year"),m=OfficialHoliday.num(month,"month");
   OfficialHoliday.Stats s=OfficialHoliday.calculate(a,chosen,y,m,true);
   int monthHoliday=monthHolidayCount(a,y,m);

   LinearLayout panel=new LinearLayout(a);panel.setOrientation(LinearLayout.VERTICAL);
   panel.setPadding(dp(a,9),dp(a,10),dp(a,9),dp(a,11));
   GradientDrawable bg=new GradientDrawable();bg.setColor(Ui.SURFACE);bg.setCornerRadius(dp(a,13));
   bg.setStroke(dp(a,1),Ui.BORDER);panel.setBackground(bg);

   TextView title=new TextView(a);title.setText("آمار ماه — "+
     chosen.getClass().getField("company").get(chosen)+" / "+chosen.getClass().getField("team").get(chosen));
   title.setTextSize(15);title.setTextColor(Ui.TEXT);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
   title.setGravity(Gravity.RIGHT);title.setPadding(dp(a,4),0,dp(a,4),dp(a,5));panel.addView(title);

   pair(a,panel,
    metric(a,"روزکاری",s.day+" روز",0xffe7f4ec,0xff397451),
    metric(a,"عصرکاری",s.evening+" روز",0xfffff5dc,0xff9a7017));
   pair(a,panel,
    metric(a,"شب‌کاری",s.night+" روز",0xffebeafa,0xff56528d),
    metric(a,"استراحت",s.rest+" روز",0xffedf4f7,0xff577281));
   pair(a,panel,
    metric(a,"جمعه‌کاری",s.fridayWork+" روز",0xffffe8e8,0xffa94444),
    metric(a,"شیفت‌های سفارشی",s.other+" روز",0xffeef1f4,0xff667585));

   // Keep these two directly under each other: shift subset first, then month total.
   full(a,panel,metric(a,"تعطیل‌کاری شیفت",s.officialWork+" روز",0xffffe2ed,0xff8f3158));
   full(a,panel,metric(a,"تعطیل‌کاری ماه",monthHoliday+" روز",0xfffff0f4,0xffa14369));

   TextView customNote=explanation(a,"شیفت‌های سفارشی","شیفت‌های کاری تعریف‌شده خارج از روزکار، عصرکار و شبکار.",0xffa94444);
   customNote.setPadding(dp(a,5),dp(a,6),dp(a,5),0);panel.addView(customNote);

   if(s.overlap>0){
    TextView overlap=new TextView(a);overlap.setText(OfficialHoliday.fa("جمعه‌های دارای مناسبت تعطیل: "+s.overlap+" روز — فقط جمعه‌کاری محسوب می‌شوند."));
    overlap.setTextSize(11.5f);overlap.setGravity(Gravity.RIGHT);overlap.setTextColor(Ui.MUTED);
    overlap.setPadding(dp(a,5),dp(a,6),dp(a,5),0);panel.addView(overlap);
   }
   TextView shiftNote=explanation(a,"تعطیل‌کاری شیفت","از تعطیلات غیرجمعه ماه، تعداد روزهایی که روی شیفت کاری الگوی انتخابی افتاده‌اند.",0xff8f3158);
   shiftNote.setPadding(dp(a,5),dp(a,8),dp(a,5),0);panel.addView(shiftNote);
   TextView monthNote=explanation(a,"تعطیل‌کاری ماه","کل تعطیلات مناسبتی غیرجمعه ماه، بدون توجه به الگوی شیفت.",0xffa14369);
   panel.addView(monthNote);

   LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(a,12),0,dp(a,8));table.addView(panel,lp);
  }catch(Exception e){android.util.Log.e("ELUQEN_FOUR","Monthly statistics",e);}
 }
}

