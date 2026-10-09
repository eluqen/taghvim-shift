package com.eluqen.shiftcalendar.ui;
import android.content.Context;
import java.lang.reflect.*;
import java.util.*;
public final class OfficialHoliday {
 private static final String DATA="com.eluqen.shiftcalendar.data.",CORE="com.eluqen.shiftcalendar.core.";
 private OfficialHoliday(){}
 public static Object db(Context c)throws Exception{return Class.forName(DATA+"AppDatabase").getMethod("get",Context.class).invoke(null,c);}
 public static Object active(Context c)throws Exception{return db(c).getClass().getMethod("activePlan").invoke(db(c));}
 public static Object date(int y,int m,int d)throws Exception{return Class.forName(CORE+"PersianDate").getConstructor(int.class,int.class,int.class).newInstance(y,m,d);}
 public static int num(Object o,String field)throws Exception{return o.getClass().getField(field).getInt(o);}
 public static boolean friday(Object d){try{return (Integer)d.getClass().getMethod("dayOfWeek").invoke(d)==6;}catch(Exception ex){return false;}}
 public static List<String> reasons(Context c,Object d){
  LinkedHashSet<String> titles=new LinkedHashSet<>();
  try{
   Class<?> dates=Class.forName(CORE+"PersianDate");
   Class<?> repo=Class.forName("com.eluqen.shiftcalendar.events.EventRepository");
   Object r=repo.getMethod("get",Context.class).invoke(null,c);
   List<?> events=(List<?>)repo.getMethod("forDate",dates).invoke(r,d);
   for(Object event:events)if(event.getClass().getField("holiday").getBoolean(event)){
    String title=String.valueOf(event.getClass().getField("title").get(event)).trim();
    if(!title.isEmpty()&&!title.equals("جمعه"))titles.add(title);
   }
   List<?> personal=(List<?>)db(c).getClass().getMethod("userEvents",dates).invoke(db(c),d);
   for(Object event:personal)if(event.getClass().getField("holiday").getBoolean(event)){
    String title=String.valueOf(event.getClass().getField("title").get(event)).trim();
    if(!title.isEmpty()&&!title.equals("جمعه"))titles.add(title);
   }
  }catch(Exception e){android.util.Log.e("ELUQEN_HOLIDAY","Holiday reasons",e);}
  return new ArrayList<>(titles);
 }
 public static boolean official(Context c,Object d){return !reasons(c,d).isEmpty();}
 public static String fa(Object n){String s=String.valueOf(n);StringBuilder b=new StringBuilder();
  for(int i=0;i<s.length();i++){char ch=s.charAt(i);b.append(ch>='0'&&ch<='9'?(char)(0x06f0+ch-'0'):ch);}return b.toString();}
 public static String token(Context c,Object day,Object plan)throws Exception{
  long jdn=(Long)day.getClass().getMethod("toJdn").invoke(day);
  long anchor=plan.getClass().getField("anchorJdn").getLong(plan);
  String cycle=(String)plan.getClass().getField("pattern").get(plan);
  Class<?> engine=Class.forName(CORE+"ShiftEngine");
  String t=(String)engine.getMethod("tokenFor",long.class,long.class,String.class).invoke(null,jdn,anchor,cycle);
  t=(String)engine.getMethod("normalize",String.class).invoke(null,t);
  return GeneralWork.adjust(c,day,plan,t);
 }
 public static boolean working(Context c,String code){
  try{
   Object profile=Class.forName(DATA+"ShiftPreferences").getMethod("get",Context.class,String.class).invoke(null,c,code);
   return profile.getClass().getField("working").getBoolean(profile);
  }catch(Exception e){return !"O".equals(code);}
 }
 public static final class Stats{
  public int day,evening,night,rest,other,fridayWork,officialWork,overlap,leaveExcluded;
  public int workingTotal(){return day+evening+night+other;}
  public void add(Stats o){day+=o.day;evening+=o.evening;night+=o.night;rest+=o.rest;other+=o.other;
   fridayWork+=o.fridayWork;officialWork+=o.officialWork;overlap+=o.overlap;leaveExcluded+=o.leaveExcluded;}
 }
 public static Stats calculate(Context c,Object plan,int year,int month,boolean excludeLeave){
  Stats output=new Stats();
  try{
   int from=month==0?1:month,to=month==0?12:month;

   Class<?> calendar=Class.forName(CORE+"JalaliCalendar");
   Method days=calendar.getMethod("daysInMonth",int.class,int.class);
   for(int m=from;m<=to;m++)for(int n=1;n<=(Integer)days.invoke(null,year,m);n++){
    Object d=date(year,m,n);String code=token(c,d,plan);
    boolean absence=false,work=working(c,code);
    if(excludeLeave){
     com.eluqen.shiftcalendar.work.DayWorkResolver.State state=com.eluqen.shiftcalendar.work.DayWorkResolver.resolve(c,
      (com.eluqen.shiftcalendar.core.PersianDate)d,(com.eluqen.shiftcalendar.data.ShiftPlan)plan);
     absence=state.shiftSuppressed;
     code=state.displayProfile().code;
     work=state.hasBaseWork()||state.hasOvertime;
    }
    if(absence){output.leaveExcluded++;continue;}
    if(!work){output.rest++;continue;}
    if("D".equals(code))output.day++;else if("E".equals(code))output.evening++;
    else if("N".equals(code))output.night++;else output.other++;
    boolean friday=friday(d),holiday=official(c,d);
    // Classification rule: a worked Friday is ALWAYS Friday work, even when an
    // official/event holiday also falls on it. Holiday work is non-Friday only.
    if(friday){
     output.fridayWork++;
     if(holiday)output.overlap++;
    }else if(holiday)output.officialWork++;
   }
  }catch(Exception e){android.util.Log.e("ELUQEN_HOLIDAY","Stats",e);}
  return output;
 }
}

