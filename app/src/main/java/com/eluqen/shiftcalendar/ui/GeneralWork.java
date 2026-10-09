package com.eluqen.shiftcalendar.ui;
import android.content.Context;
public final class GeneralWork {
 private GeneralWork(){}
 public static boolean isGeneral(Object plan){
  try{
   String company=String.valueOf(plan.getClass().getField("company").get(plan)).trim();
   String team=String.valueOf(plan.getClass().getField("team").get(plan)).trim();
   return "جنرال".equals(company)||"روزکار".equals(team);
  }catch(Exception e){return false;}
 }
 public static String adjust(Context c,Object day,Object plan,String raw){
  if(!isGeneral(plan))return raw;
  try{
   int dow=(Integer)day.getClass().getMethod("dayOfWeek").invoke(day);
   if(dow==5||dow==6||OfficialHoliday.official(c,day))return "O";
   return "D";
  }catch(Exception e){
   android.util.Log.e("ELUQEN_GENERAL","General schedule",e);return raw;
  }
 }
 public static String adjustJdn(Context c,long jdn,Object plan,String raw){
  if(!isGeneral(plan))return raw;
  try{
   Object day=Class.forName("com.eluqen.shiftcalendar.core.JalaliCalendar").getMethod("fromJdn",long.class).invoke(null,jdn);
   return adjust(c,day,plan,raw);
  }catch(Exception e){android.util.Log.e("ELUQEN_GENERAL","General JDN",e);return raw;}
 }
}
