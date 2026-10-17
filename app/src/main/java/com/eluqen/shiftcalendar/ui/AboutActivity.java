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

public final class AboutActivity extends BaseActivity {
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
  LinearLayout group=new LinearLayout(this);
  group.setOrientation(LinearLayout.VERTICAL);
  group.setPadding(dp(8),dp(12),dp(8),dp(10));
  parent.addView(group,new LinearLayout.LayoutParams(-1,-2));
  LinearLayout heading=new LinearLayout(this);
  heading.setOrientation(LinearLayout.HORIZONTAL);
  heading.setGravity(Gravity.CENTER_VERTICAL);
  heading.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
  View accent=new View(this);
  GradientDrawable dash=new GradientDrawable();
  dash.setColor(Ui.SECONDARY);dash.setCornerRadius(dp(3));
  accent.setBackground(dash);
  LinearLayout.LayoutParams accentLp=new LinearLayout.LayoutParams(dp(4),dp(24));
  accentLp.setMargins(0,0,dp(10),0);
  heading.addView(accent,accentLp);
  TextView header=label(title,16,Ui.TEXT,true);
  heading.addView(header,new LinearLayout.LayoutParams(0,-2,1));
  group.addView(heading);
  TextView details=label(body,14,Ui.MUTED,false);
  details.setLineSpacing(dp(3),1.12f);
  details.setPadding(dp(2),dp(7),dp(14),dp(3));
  group.addView(details);
  View divider=new View(this);
  divider.setBackgroundColor(Ui.BORDER);
  LinearLayout.LayoutParams line=new LinearLayout.LayoutParams(-1,dp(1));
  line.setMargins(dp(8),dp(5),dp(8),0);
  parent.addView(divider,line);
 }
 private void open(String uri,boolean email){
  try{
   Intent intent=new Intent(email?Intent.ACTION_SENDTO:Intent.ACTION_VIEW,Uri.parse(uri));
   startActivity(intent);
  }catch(Exception ignored){Toast.makeText(this,email?"برنامه ایمیل در دستگاه در دسترس نیست":"باز کردن پیوند ممکن نشد",Toast.LENGTH_LONG).show();}
 }
 private void link(LinearLayout parent,String title,String destination,String uri,boolean email){
  Button b=new Button(this);b.setText(title+"\n"+destination);b.setAllCaps(false);
  b.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);b.setTextDirection(View.TEXT_DIRECTION_RTL);
  b.setTextSize(13.5f);b.setTextColor(Ui.PRIMARY);
  b.setPadding(dp(13),0,dp(13),0);b.setMinHeight(0);
  GradientDrawable background=new GradientDrawable();
  background.setColor(Ui.SURFACE_ALT);background.setCornerRadius(dp(12));
  background.setStroke(dp(1),Ui.BORDER);
  b.setBackgroundTintList(null);b.setBackground(background);
  b.setContentDescription(title+"، "+destination);
  b.setOnClickListener(v->open(uri,email));
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(62));
  lp.setMargins(dp(4),dp(4),dp(4),0);parent.addView(b,lp);
 }
 private void styleStoreAction(Button b){
  b.setTextColor(Ui.PRIMARY);
  b.setTextSize(14);
  b.setGravity(Gravity.CENTER);
  b.setPadding(dp(10),0,dp(10),0);
  b.setAllCaps(false);b.setMinHeight(0);
  b.setBackgroundTintList(null);
  GradientDrawable background=new GradientDrawable();
  background.setColor(Ui.SURFACE_ALT);
  background.setCornerRadius(dp(12));
  background.setStroke(dp(1),Ui.BORDER);
  b.setBackground(background);
 }
 private void openReviewNow(){
  AppUpdateManager.OpenResult r=AppUpdateManager.openReview(this);
  if(!r.opened)Toast.makeText(this,r.message,Toast.LENGTH_LONG).show();
 }
 private void requestReview(){
  if(AppUpdateManager.selectedSource(this)==UpdateSource.MYKET){
   new AlertDialog.Builder(this)
    .setTitle("ثبت نظر در مایکت")
    .setMessage("مایلید صفحه ثبت نظر و امتیاز تقویم شیفت در مایکت باز شود؟")
    .setPositiveButton("ادامه",(dialog,which)->openReviewNow())
    .setNegativeButton("انصراف",null)
    .show();
   return;
  }
  openReviewNow();
 }
 @Override protected void onCreate(Bundle state){
  super.onCreate(state);
  // BackDock supplies a single fixed bottom Back action outside the ScrollView.
  LinearLayout root=new LinearLayout(this);
  root.setOrientation(LinearLayout.VERTICAL);
  root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
  root.setBackgroundColor(Ui.SURFACE);
  ScrollView scroll=new ScrollView(this);
  scroll.setFillViewport(false);
  scroll.setClipToPadding(false);
  LinearLayout content=new LinearLayout(this);
  content.setOrientation(LinearLayout.VERTICAL);
  content.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
  content.setPadding(dp(13),dp(8),dp(13),dp(14));
  scroll.addView(content);
  root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  LinearLayout panel=card(content);
  panel.setPadding(dp(12),dp(11),dp(12),dp(14));
  panel.setElevation(dp(2));
  panel.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
  // Absolute hero centering: neither the icon nor its width shifts the text center.
  FrameLayout hero=new FrameLayout(this);
  hero.setPadding(dp(16),0,dp(16),0);
  GradientDrawable heroFill=new GradientDrawable(
      GradientDrawable.Orientation.TL_BR,new int[]{Ui.PRIMARY,Ui.PRIMARY_DARK});
  heroFill.setCornerRadius(dp(15));
  hero.setBackground(heroFill);
  // Match the entire hero width, with balanced safe insets on both sides.
  LinearLayout identity=new LinearLayout(this);
  identity.setOrientation(LinearLayout.VERTICAL);
  identity.setGravity(Gravity.CENTER);
  identity.setPadding(dp(72),0,dp(72),0);
  FrameLayout.LayoutParams centered=new FrameLayout.LayoutParams(
      -1,-1,Gravity.CENTER);
  hero.addView(identity,centered);
  TextView name=label("تقویم شیفت",21,0xffffffff,true);
  name.setGravity(Gravity.CENTER);
  name.setMaxLines(1);
  if(android.os.Build.VERSION.SDK_INT>=26){
   name.setAutoSizeTextTypeUniformWithConfiguration(
       16,21,1,android.util.TypedValue.COMPLEX_UNIT_SP);
  }
  identity.addView(name,new LinearLayout.LayoutParams(-1,-2));
  String version="";try{version=getPackageManager().getPackageInfo(getPackageName(),0).versionName;}catch(Exception ignored){}
  TextView ver=label("نسخه "+version,13,0xffe3eff9,false);
  ver.setGravity(Gravity.CENTER);
  ver.setPadding(0,dp(4),0,0);
  identity.addView(ver,new LinearLayout.LayoutParams(-1,-2));
  ImageView appIcon=new ImageView(this);
  appIcon.setImageResource(com.eluqen.shiftcalendar.R.mipmap.ic_launcher);
  appIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
  appIcon.setContentDescription("آیکن تقویم شیفت");
  FrameLayout.LayoutParams iconLp=new FrameLayout.LayoutParams(
      dp(64),dp(64),Gravity.RIGHT|Gravity.CENTER_VERTICAL);
  hero.addView(appIcon,iconLp);
  panel.addView(hero,new LinearLayout.LayoutParams(-1,dp(90)));
  section(panel,"تقویم شیفت چیست؟","تقویم شیفت ELUQEN برای برنامه‌ریزی شیفت‌های کاری، ثبت کارکرد و مدیریت کارهای شخصی طراحی شده است. تقویم شمسی، برنامه کاری و مناسبت‌ها در یک محیط در دسترس‌اند.");
  section(panel,"تقویم و مناسبت‌ها","مشاهده تقویم ماهانه و روزانه، جابه‌جایی میان ماه‌ها، مناسبت‌ها و تعطیلات، انتخاب روز و دسترسی سریع به اطلاعات و برنامه همان روز. بانک مناسبت‌ها به‌صورت مستقل به‌روزرسانی می‌شود.");
  section(panel,"شیفت‌ها و الگوهای کاری","ساخت و ویرایش الگوهای چرخشی کار، تنظیم انواع شیفت، ساعت و رنگ هر شیفت، نمایش تقویم چهارشیفت و مشاهده برنامه کار در روزهای آینده. برای محل‌های کار مختلف می‌توانید الگوهای مجزا داشته باشید.");
  section(panel,"کارکرد و ثبت وضعیت روزانه","ثبت و مرور وضعیت روزهای کاری، استراحت، مرخصی، جابه‌جایی شیفت، اضافه‌کاری و تعطیل‌کاری، همراه با نمای کارکرد برای پیگیری سوابق کاری.");
  section(panel,"کارها و برنامه‌ریزی شخصی","ساخت و پیگیری فهرست کارها و ثبت رویدادها و برنامه‌های شخصی در کنار تقویم کاری، با امکان مراجعه و ویرایش اطلاعات ثبت‌شده.");
  section(panel,"یادآوری‌ها و اعلان‌ها","تنظیم یادآوری شیفت و رویدادهای شخصی، انتخاب صدای اعلان و مدیریت تنظیمات یادآوری. اجرای به‌موقع اعلان‌ها به مجوزها و تنظیمات دستگاه وابسته است.");
  section(panel,"یادداشت و دست‌نویسی","دفترچه یادداشت برای نوشتن متن و ثبت دست‌نویس با لمس صفحه یا قلم؛ مناسب ثبت نکته‌ها و اطلاعات روزمره.");
  section(panel,"ابزارهای کاربردی","محاسبه شیفت در بازه دلخواه، تهیه خروجی استاندارد تقویم با فرمت ICS و اشتراک خلاصه برنامه شیفت‌های ۳۰ روز آینده.");
  section(panel,"تنظیمات و شخصی‌سازی","مدیریت شیفت‌ها و الگوها، رنگ‌بندی و تنظیمات نمایش، تنظیم اعلان‌ها و انتخاب صفحه شروع برنامه بر اساس نیاز شخصی.");
  section(panel,"حریم خصوصی و نسخه پشتیبان","یادداشت‌ها و ثبت‌های شخصی در حافظه اختصاصی اپلیکیشن نگهداری می‌شوند. پشتیبان‌گیری و بازیابی از بخش تنظیمات در دسترس است. حذف داده‌های برنامه بدون نسخه پشتیبان ممکن است اطلاعات ذخیره‌شده را از بین ببرد.");
  section(panel,"اعلان‌ها و دسترسی‌ها","اجرای یادآورهای شیفت و شخصی به مجوزها و تنظیمات اعلان اندروید وابسته است.");
  section(panel,"راه‌های ارتباطی ELUQEN","راه‌های رسمی ارتباط و پیگیری اطلاعات برنامه");
  LinearLayout contact=panel;
  TextView caption=label("وب‌سایت، گیت‌هاب و ایمیل پشتیبانی اپلیکیشن",13,Ui.MUTED,false);
  caption.setPadding(0,dp(6),0,dp(7));contact.addView(caption);
  link(contact,"وب‌سایت رسمی","eluqen.com","https://eluqen.com/",false);
  link(contact,"سیاست حفظ حریم خصوصی","اطلاعات حریم خصوصی تقویم شیفت","https://eluqen.com/apps/taghvim-shift/privacy-policy/",false);
  link(contact,"GitHub پروژه","github.com/eluqen/taghvim-shift",GITHUB_URL,false);
  link(contact,"ایمیل پشتیبانی","dev@eluqen.com","mailto:dev@eluqen.com?subject=ELUQEN%20Taghvim%20Shift%20Support",true);
  // Galaxy review entry remains visible in an ADB-installed QA build.
  // Eligibility is enforced only when opening the review, so the user sees
  // a clear Galaxy-installation explanation instead of a missing action.
  boolean showReviewAction = AppUpdateManager.supportsReview(this)
      || AppUpdateManager.selectedSource(this) == UpdateSource.GALAXY;
  if(showReviewAction||AppUpdateManager.supportsDeveloperPage(this)){
   String store=AppUpdateManager.activeStoreLabel(this);
   section(panel,store,"دسترسی به صفحه برنامه و دیگر محصولات ELUQEN");
   LinearLayout storeCard=panel;
   TextView storeNote=label("نظر شما به بهتر شدن تقویم شیفت کمک می‌کند.",13,Ui.MUTED,false);
   storeNote.setPadding(0,dp(6),0,dp(7));storeCard.addView(storeNote);
   if(showReviewAction){
    Button review=new Button(this);review.setText("ثبت نظر و امتیاز در "+store);review.setAllCaps(false);
    styleStoreAction(review);review.setOnClickListener(v->requestReview());
    storeCard.addView(review,new LinearLayout.LayoutParams(-1,dp(58)));
   }
   if(AppUpdateManager.supportsDeveloperPage(this)){
    Button apps=new Button(this);apps.setText("سایر برنامه‌های ELUQEN در "+store);apps.setAllCaps(false);
    styleStoreAction(apps);apps.setOnClickListener(v->{
     AppUpdateManager.OpenResult r=AppUpdateManager.openDeveloperPage(this);
     if(!r.opened)Toast.makeText(this,r.message,Toast.LENGTH_LONG).show();});
    storeCard.addView(apps,new LinearLayout.LayoutParams(-1,dp(58)));
   }
  }
  TextView footer=label("© ELUQEN",13,Ui.MUTED,true);
  footer.setGravity(Gravity.CENTER);
  footer.setPadding(0,dp(18),0,dp(4));
  panel.addView(footer);
  setContentView(root);
 }
 @Override public void onBackPressed(){BackDock.handleBack(this);}
}
