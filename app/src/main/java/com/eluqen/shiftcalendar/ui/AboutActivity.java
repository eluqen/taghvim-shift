package com.eluqen.shiftcalendar.ui;
import android.app.*;
import android.content.*;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import com.eluqen.shiftcalendar.update.AppUpdateManager;
import com.eluqen.shiftcalendar.update.UpdateSource;

public final class AboutActivity extends Activity {
 private static final String GITHUB_URL="https://github.com/eluqen/taghvim-shift";
 private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 private TextView label(String s,int sp,int color,boolean bold){
  TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(color);t.setGravity(Gravity.RIGHT);
  t.setTextDirection(View.TEXT_DIRECTION_RTL);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
  return t;
 }
 private LinearLayout card(LinearLayout parent){
  LinearLayout card=new LinearLayout(this);card.setOrientation(android.widget.LinearLayout.VERTICAL);card.setPadding(dp(16),dp(14),dp(16),dp(16));
  GradientDrawable bg=new GradientDrawable();bg.setColor(Ui.CARD);bg.setCornerRadius(dp(17));bg.setStroke(dp(1),Ui.BORDER);card.setBackground(bg);
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,dp(11));parent.addView(card,lp);
  return card;
 }
 private void section(LinearLayout parent,String title,String body){
  LinearLayout card=card(parent);card.addView(label(title,17,Ui.TEXT,true));
  TextView details=label(body,14,Ui.MUTED,false);details.setLineSpacing(dp(3),1.10f);details.setPadding(0,dp(7),0,0);card.addView(details);
 }
 private void open(String uri,boolean email){
  try{
   Intent intent=new Intent(email?Intent.ACTION_SENDTO:Intent.ACTION_VIEW,Uri.parse(uri));
   startActivity(intent);
  }catch(Exception ignored){Toast.makeText(this,email?"برنامه ایمیل در دستگاه در دسترس نیست":"باز کردن پیوند ممکن نشد",Toast.LENGTH_LONG).show();}
 }
 private void link(LinearLayout parent,String title,String destination,String uri,boolean email){
  Button b=new Button(this);b.setText(title+"\n"+destination);b.setAllCaps(false);
  b.setGravity(Gravity.CENTER);b.setTextSize(14);b.setTextColor(Ui.SECONDARY);
  b.setContentDescription(title+"، "+destination);
  b.setOnClickListener(v->open(uri,email));
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(70));lp.setMargins(0,dp(5),0,0);parent.addView(b,lp);
 }
 private void openReviewNow(){
  AppUpdateManager.OpenResult r=AppUpdateManager.openReview(this);
  if(!r.opened)Toast.makeText(this,r.message,Toast.LENGTH_LONG).show();
 }
 private void requestReview(){ openReviewNow(); }
 @Override protected void onCreate(Bundle state){
  super.onCreate(state);AppTheme.apply(this);BackDock.registerSystemBack(this);
  LinearLayout root=new LinearLayout(this);root.setOrientation(android.widget.LinearLayout.VERTICAL);root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);root.setBackgroundColor(Ui.SURFACE);
  Button back=new Button(this);back.setText("‹  بازگشت");back.setAllCaps(false);back.setTextColor(Ui.SECONDARY);back.setOnClickListener(v->BackDock.handleBack(this));
  root.addView(back,new LinearLayout.LayoutParams(-1,dp(47)));
  ScrollView scroll=new ScrollView(this);LinearLayout content=new LinearLayout(this);content.setOrientation(android.widget.LinearLayout.VERTICAL);content.setPadding(dp(16),dp(10),dp(16),dp(25));scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  TextView brand=label("ELUQEN",27,Ui.SECONDARY,true);brand.setGravity(Gravity.CENTER);content.addView(brand);
  TextView name=label("تقویم شیفت",23,Ui.TEXT,true);name.setGravity(Gravity.CENTER);name.setPadding(0,dp(5),0,dp(2));content.addView(name);
  String version="";try{version=getPackageManager().getPackageInfo(getPackageName(),0).versionName;}catch(Exception ignored){}
  TextView ver=label("نسخه "+version,13,Ui.MUTED,false);ver.setGravity(Gravity.CENTER);ver.setPadding(0,0,0,dp(17));content.addView(ver);
  section(content,"تقویم شیفت چیست؟","تقویم شیفت ELUQEN برای برنامه‌ریزی شیفت‌های کاری، ثبت کارکرد و مدیریت کارهای شخصی طراحی شده است. تقویم شمسی، برنامه کاری و مناسبت‌ها در یک محیط در دسترس‌اند.");
  section(content,"تقویم و مناسبت‌ها","مشاهده تقویم ماهانه و روزانه، جابه‌جایی میان ماه‌ها، مناسبت‌ها و تعطیلات، انتخاب روز و دسترسی سریع به اطلاعات و برنامه همان روز. بانک مناسبت‌ها به‌صورت مستقل به‌روزرسانی می‌شود.");
  section(content,"شیفت‌ها و الگوهای کاری","ساخت و ویرایش الگوهای چرخشی کار، تنظیم انواع شیفت، ساعت و رنگ هر شیفت، نمایش تقویم چهارشیفت و مشاهده برنامه کار در روزهای آینده. برای محل‌های کار مختلف می‌توانید الگوهای مجزا داشته باشید.");
  section(content,"کارکرد و ثبت وضعیت روزانه","ثبت و مرور وضعیت روزهای کاری، استراحت، مرخصی، جابه‌جایی شیفت، اضافه‌کاری و تعطیل‌کاری، همراه با نمای کارکرد برای پیگیری سوابق کاری.");
  section(content,"کارها و برنامه‌ریزی شخصی","ساخت و پیگیری فهرست کارها و ثبت رویدادها و برنامه‌های شخصی در کنار تقویم کاری، با امکان مراجعه و ویرایش اطلاعات ثبت‌شده.");
  section(content,"یادآوری‌ها و اعلان‌ها","تنظیم یادآوری شیفت و رویدادهای شخصی، انتخاب صدای اعلان و مدیریت تنظیمات یادآوری. اجرای به‌موقع اعلان‌ها به مجوزها و تنظیمات دستگاه وابسته است.");
  section(content,"یادداشت و دست‌نویسی","دفترچه یادداشت برای نوشتن متن و ثبت دست‌نویس با لمس صفحه یا قلم؛ مناسب ثبت نکته‌ها و اطلاعات روزمره.");
  section(content,"ابزارهای کاربردی","محاسبه شیفت در بازه دلخواه، تهیه خروجی استاندارد تقویم با فرمت ICS و اشتراک خلاصه برنامه شیفت‌های ۳۰ روز آینده.");
  section(content,"تنظیمات و شخصی‌سازی","مدیریت شیفت‌ها و الگوها، رنگ‌بندی و تنظیمات نمایش، تنظیم اعلان‌ها و انتخاب صفحه شروع برنامه بر اساس نیاز شخصی.");
  section(content,"حریم خصوصی و نسخه پشتیبان","یادداشت‌ها و ثبت‌های شخصی در حافظه اختصاصی اپلیکیشن نگهداری می‌شوند. پشتیبان‌گیری و بازیابی از بخش تنظیمات در دسترس است. حذف داده‌های برنامه بدون نسخه پشتیبان ممکن است اطلاعات ذخیره‌شده را از بین ببرد.");
  section(content,"اعلان‌ها و دسترسی‌ها","اجرای یادآورهای شیفت و شخصی به مجوزها و تنظیمات اعلان اندروید وابسته است.");
  LinearLayout contact=card(content);contact.addView(label("راه‌های ارتباطی ELUQEN",17,Ui.TEXT,true));
  TextView caption=label("وب‌سایت، گیت‌هاب و ایمیل پشتیبانی اپلیکیشن",13,Ui.MUTED,false);
  caption.setPadding(0,dp(6),0,dp(7));contact.addView(caption);
  link(contact,"وب‌سایت رسمی","eluqen.com","https://eluqen.com/",false);
  link(contact,"سیاست حفظ حریم خصوصی","سیاست داده‌های برنامه","https://eluqen.com/apps/taghvim-shift/privacy-policy/",false);
  link(contact,"GitHub پروژه","github.com/eluqen/taghvim-shift",GITHUB_URL,false);
  link(contact,"ایمیل پشتیبانی","dev@eluqen.com","mailto:dev@eluqen.com?subject=ELUQEN%20Taghvim%20Shift%20Support",true);
  if(AppUpdateManager.supportsReview(this)||AppUpdateManager.supportsDeveloperPage(this)){
   String store=AppUpdateManager.activeStoreLabel(this);
   LinearLayout storeCard=card(content);storeCard.addView(label(store,17,Ui.TEXT,true));
   TextView storeNote=label("نظر شما به بهتر شدن تقویم شیفت کمک می‌کند.",13,Ui.MUTED,false);
   storeNote.setPadding(0,dp(6),0,dp(7));storeCard.addView(storeNote);
   if(AppUpdateManager.supportsReview(this)){
    Button review=new Button(this);review.setText("ثبت نظر و امتیاز در "+store);review.setAllCaps(false);
    review.setTextColor(Ui.SECONDARY);review.setOnClickListener(v->requestReview());
    storeCard.addView(review,new LinearLayout.LayoutParams(-1,dp(58)));
   }
   if(AppUpdateManager.supportsDeveloperPage(this)){
    Button apps=new Button(this);apps.setText("سایر برنامه‌های ELUQEN در "+store);apps.setAllCaps(false);
    apps.setTextColor(Ui.SECONDARY);apps.setOnClickListener(v->{
     AppUpdateManager.OpenResult r=AppUpdateManager.openDeveloperPage(this);
     if(!r.opened)Toast.makeText(this,r.message,Toast.LENGTH_LONG).show();});
    storeCard.addView(apps,new LinearLayout.LayoutParams(-1,dp(58)));
   }
  }
  TextView footer=label("© ELUQEN",13,Ui.MUTED,true);footer.setGravity(Gravity.CENTER);content.addView(footer);
  setContentView(root);
 }
 @Override public void onBackPressed(){BackDock.handleBack(this);}
}
