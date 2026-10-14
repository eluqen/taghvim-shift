package com.eluqen.shiftcalendar.ui;
import android.app.Activity;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.eluqen.shiftcalendar.data.Prefs;
public final class BackDock {
 public interface LocalBackHandler{boolean onDockBack();}
 private static int NAVY=0xff203a63, TEAL=0xff108b86, WHITE=Color.WHITE, MUTED=0xff63758d;
 private static int SURFACE=0xfff5f8fc, BORDER=0xffe0e8f2;
 private static final String PKG="com.eluqen.shiftcalendar.ui.";
 private static final String[] TABS={"MainActivity","FourShiftActivity","CardexActivity","GeneralSettingsActivity","FeatureHubActivity"};
 private static final String[] LABELS={"تقویم","چهارشیفت","کارکرد","تنظیمات","امکانات"};
 private static final String[] SETTINGS_CHILDREN={
   "SettingsActivity",
   "ShiftTypesActivity",
   "ReminderSettingsActivity",
   "ColleaguesActivity",
   "ThemeSettingsActivity",
   "WidgetSettingsActivity",
   "UpdateCenterActivity",
   "AboutActivity"
 };
 static final String EXTRA_ROOT_ENTRY="eluqen_root_tab_entry";
 static final String EXTRA_FEATURES_CHILD="eluqen_features_child";
 private BackDock(){}
 static void applyTheme(AppTheme.Palette p){
  NAVY=p.primary;TEAL=p.secondary;WHITE=p.card;MUTED=p.muted;
  SURFACE=p.surface;BORDER=p.border;
 }
 private static int dp(Activity a,float n){return Math.round(a.getResources().getDisplayMetrics().density*n);}
 private static GradientDrawable rounded(Activity a,int color,int radius,int border){
  GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(a,radius));
  if(border!=0)g.setStroke(dp(a,1),border);return g;
 }
 static int activeTab(Activity a){
  String type=a.getClass().getName();
  for(int i=0;i<TABS.length;i++)if(type.equals(PKG+TABS[i]))return i;
  return -1;
 }
 public static void registerSystemBack(Activity activity){
  if(Build.VERSION.SDK_INT<33)return;
  activity.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
    0,()->handleBack(activity));
 }
 private static int startTab(Activity a){
  String start=Prefs.defaultScreen(a);
  if(Prefs.SCREEN_FOUR_SHIFT.equals(start))return 1;
  if(Prefs.SCREEN_CARDEX.equals(start))return 2;
  if("settings".equals(start))return 3;
  if(Prefs.SCREEN_FEATURES.equals(start))return 4;
  return 0;
 }
 static void goToStart(Activity activity){
  int start=startTab(activity);
  int active=activeTab(activity);
  if(active==start){
   activity.finishAffinity();
   activity.overridePendingTransition(0,0);
   return;
  }
  Intent home=new Intent();
  home.setClassName(activity,PKG+TABS[start]);
  home.putExtra(EXTRA_ROOT_ENTRY,true);
  if(start==0)home.putExtra("skip_default_screen",true);
  if(start==3)home.putExtra(GeneralSettingsActivity.EXTRA_RESET_SCROLL,true);
  home.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION
    |Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
  activity.startActivity(home);
  activity.finish();
  activity.overridePendingTransition(0,0);
 }
 private static boolean isSettingsChild(Activity activity){
  String type=activity.getClass().getName();
  for(String child:SETTINGS_CHILDREN){
   if(type.equals(PKG+child))return true;
  }
  return false;
 }
 public static void goToSettingsHome(Activity activity){
  Intent settings=new Intent();
  settings.setClassName(activity,PKG+"GeneralSettingsActivity");
  settings.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION
    |Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
  activity.startActivity(settings);
  activity.finish();
  activity.overridePendingTransition(0,0);
 }
 private static boolean isFeaturesChild(Activity activity){
  return activeTab(activity)<0
      && activity.getIntent()!=null
      && activity.getIntent().getBooleanExtra(EXTRA_FEATURES_CHILD,false);
 }
 public static void goToFeaturesHome(Activity activity){
  Intent features=new Intent(activity,FeatureHubActivity.class);
  features.putExtra(EXTRA_ROOT_ENTRY,true);
  features.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION
    |Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
  activity.startActivity(features);
  activity.finish();
  activity.overridePendingTransition(0,0);
 }
 public static boolean handleBack(Activity activity){
  // Nested editor state wins; root Back semantics are intentionally unchanged.
  if(activity instanceof LocalBackHandler
      && ((LocalBackHandler)activity).onDockBack())return true;
  if(isSettingsChild(activity)){
   goToSettingsHome(activity);
   return true;
  }
  if(isFeaturesChild(activity)){
   goToFeaturesHome(activity);
   return true;
  }
  goToStart(activity);
  return true;
 }
 public static void install(Activity activity,View original){
  // Mount the common shell synchronously, before Android draws the old screen.
  mount(activity,original);
 }
 private static void mount(Activity activity,View original){
  if(activity.isFinishing()||activity.isDestroyed())return;
  FrameLayout content=activity.findViewById(android.R.id.content);
  if(content==null||original.getParent()!=content)return;
  int tab=activeTab(activity);
  if(tab==0 && original instanceof LinearLayout){
   LinearLayout root=(LinearLayout)original;
   if(root.getChildCount()>=3){
    View oldNav=root.getChildAt(root.getChildCount()-1);
    if(oldNav instanceof LinearLayout && ((LinearLayout)oldNav).getChildCount()==5)
     root.removeView(oldNav);
   }
  }
  content.removeView(original);
  LinearLayout holder=new LinearLayout(activity);
  holder.setOrientation(LinearLayout.VERTICAL);
  holder.setTag("eluqen_root_holder");
  holder.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
  holder.setBackgroundColor(SURFACE);
  if(tab>=0){
   hideTopBack(original,0);
   FrameLayout viewport=new FrameLayout(activity);
   viewport.setTag("eluqen_page_viewport");
   viewport.setClipChildren(true);
   viewport.setClipToPadding(true);
   viewport.setBackgroundColor(SURFACE);
   viewport.addView(original,new FrameLayout.LayoutParams(-1,-1));
   holder.addView(viewport,new LinearLayout.LayoutParams(-1,0,1f));
   holder.addView(mainNav(activity,tab),new LinearLayout.LayoutParams(-1,dp(activity,81)));
  }else{
   holder.addView(original,new LinearLayout.LayoutParams(-1,0,1f));
   holder.addView(backBar(activity),new LinearLayout.LayoutParams(-1,dp(activity,72)));
   hideTopBack(original,0);
  }
  content.addView(holder,new FrameLayout.LayoutParams(-1,-1));
  if(Build.VERSION.SDK_INT>=35){
   holder.setOnApplyWindowInsetsListener((v,insets)->{
    int bottom=insets.getInsets(WindowInsets.Type.systemBars()).bottom;
    v.setPadding(0,0,0,bottom);
    return insets;
   });
   holder.requestApplyInsets();
  }
 }
 private static void navigate(Activity a,int index,int active){
  if(index<0||index>=TABS.length||index==active)return;
  Intent intent=new Intent();
  intent.setClassName(a,PKG+TABS[index]);
  intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
  intent.putExtra(EXTRA_ROOT_ENTRY,true);
  if(index==0)intent.putExtra("skip_default_screen",true);
  if(index==3)intent.putExtra(GeneralSettingsActivity.EXTRA_RESET_SCROLL,true);
  a.startActivity(intent);
  // Root tabs replace each other. This keeps one root Activity alive, so
  // scroll/dock state from an old root page can never race the new page.
  a.finish();
  a.overridePendingTransition(0,0);
 }
 static void navigateRelative(Activity a,int delta){
  int active=activeTab(a);
  if(active<0||delta==0)return;
  int target=(active+(delta>0?1:-1)+TABS.length)%TABS.length;
  navigate(a,target,active);
 }
 static int targetTab(Activity a,int delta){
  int active=activeTab(a);
  if(active<0||delta==0)return -1;
  return (active+(delta>0?1:-1)+TABS.length)%TABS.length;
 }
 static String tabLabel(int index){
  return index>=0&&index<LABELS.length?LABELS[index]:"";
 }
 static View rootHolder(Activity a){
  FrameLayout content=a.findViewById(android.R.id.content);
  if(content==null)return null;
  for(int i=content.getChildCount()-1;i>=0;i--){
   View child=content.getChildAt(i);
   if("eluqen_root_holder".equals(child.getTag()))return child;
  }
  return null;
 }
 static FrameLayout pageViewport(Activity a){
  View holder=rootHolder(a);
  if(!(holder instanceof LinearLayout))return null;
  LinearLayout h=(LinearLayout)holder;
  if(h.getChildCount()<1)return null;
  View first=h.getChildAt(0);
  if(first instanceof FrameLayout && "eluqen_page_viewport".equals(first.getTag()))
   return (FrameLayout)first;
  return null;
 }
 static View pageContent(Activity a){
  FrameLayout viewport=pageViewport(a);
  return viewport!=null&&viewport.getChildCount()>0?viewport.getChildAt(viewport.getChildCount()-1):null;
 }
 private static NavState navState(Activity a){
  View holder=rootHolder(a);
  if(!(holder instanceof LinearLayout))return null;
  LinearLayout h=(LinearLayout)holder;
  if(h.getChildCount()<2)return null;
  Object tag=h.getChildAt(h.getChildCount()-1).getTag();
  return tag instanceof NavState?(NavState)tag:null;
 }
 static void previewSwipe(Activity a,int fromTab,int toTab,float progress){
  NavState state=navState(a);
  if(state!=null)state.preview(fromTab,toTab,progress);
 }
 static void settleSwipe(Activity a,int fromTab,int toTab,boolean commit,long duration){
  NavState state=navState(a);
  if(state!=null)state.settle(fromTab,toTab,commit,duration);
 }
 static void resetSwipePreview(Activity a){
  NavState state=navState(a);
  if(state!=null)state.reset(false);
 }
 static boolean inBottomNav(Activity a,float rawX,float rawY){
  NavState state=navState(a);
  if(state==null)return false;
  int[] pos=new int[2];state.shell.getLocationOnScreen(pos);
  return rawX>=pos[0]&&rawX<pos[0]+state.shell.getWidth()
    &&rawY>=pos[1]&&rawY<pos[1]+state.shell.getHeight();
 }

 private static final class NavState {
  final Activity a;
  final FrameLayout shell;
  final LinearLayout bar;
  final View indicator;
  final LinearLayout[] buttons;
  final TextView[] titles;
  final NavGlyph[] glyphs;
  final TextView[] bells;
  int swipeBase=-1;
  int dockHoverTab=-1;
  float swipeProgress=0f;
  ValueAnimator indicatorAnimator;

  NavState(Activity a,FrameLayout shell,LinearLayout bar,View indicator,
           LinearLayout[] buttons,TextView[] titles,NavGlyph[] glyphs,TextView[] bells){
   this.a=a;this.shell=shell;this.bar=bar;this.indicator=indicator;
   this.buttons=buttons;this.titles=titles;this.glyphs=glyphs;this.bells=bells;
  }

  float xFor(int index){
   if(index<0||index>=buttons.length||buttons[index]==null)return 0f;
   int[] shellPos=new int[2];
   int[] buttonPos=new int[2];
   shell.getLocationOnScreen(shellPos);
   buttons[index].getLocationOnScreen(buttonPos);
   // FrameLayout applies its own padding in addition to leftMargin.
   // Subtract it here so the indicator's visual left exactly matches
   // the real button left in both RTL and LTR layouts.
   return buttonPos[0]-shellPos[0]-shell.getPaddingLeft();
  }
  int widthFor(int index){
   if(index<0||index>=buttons.length||buttons[index]==null)return 1;
   return Math.max(1,buttons[index].getWidth());
  }
  float yFor(int index){
   if(index<0||index>=buttons.length||buttons[index]==null)return 0f;
   int[] shellPos=new int[2];
   int[] buttonPos=new int[2];
   shell.getLocationOnScreen(shellPos);
   buttons[index].getLocationOnScreen(buttonPos);
   return buttonPos[1]-shellPos[1]-shell.getPaddingTop();
  }
  int heightFor(int index){
   if(index<0||index>=buttons.length||buttons[index]==null)return dp(a,63);
   return Math.max(1,buttons[index].getHeight());
  }
  float lerp(float from,float to,float p){return from+(to-from)*p;}
  boolean wraps(int from,int to){
   return (from==0&&to==buttons.length-1)||(from==buttons.length-1&&to==0);
  }
  float leftExitX(){
   return -shell.getPaddingLeft()-Math.max(1,indicator.getWidth())-dp(a,4);
  }
  float rightExitX(){
   return shell.getWidth()-shell.getPaddingLeft()+dp(a,4);
  }
  float pathX(int from,int to,float p){
   p=Math.max(0f,Math.min(1f,p));
   if(!wraps(from,to))return lerp(xFor(from),xFor(to),p);
   if(from==0&&to==buttons.length-1){
    // Calendar is the visual right edge. Exit right, re-enter from left.
    if(p<.5f)return lerp(xFor(from),rightExitX(),p*2f);
    return lerp(leftExitX(),xFor(to),(p-.5f)*2f);
   }
   // Features -> Calendar: exit left, re-enter from right.
   if(p<.5f)return lerp(xFor(from),leftExitX(),p*2f);
   return lerp(rightExitX(),xFor(to),(p-.5f)*2f);
  }
  void applySwipeProgress(int from,int to,float p){
   if(swipeBase!=from)layoutIndicator(from);
   swipeProgress=Math.max(0f,Math.min(1f,p));
   indicator.setTranslationX(pathX(from,to,swipeProgress)-xFor(from));
   indicator.setTranslationY(lerp(yFor(from),yFor(to),swipeProgress)-yFor(from));
  }
  void cancelIndicatorAnimator(){
   if(indicatorAnimator!=null){
    indicatorAnimator.cancel();
    indicatorAnimator=null;
   }
   indicator.animate().cancel();
  }
  int closestTab(float rawX){
   int best=0;
   float bestDist=Float.MAX_VALUE;
   for(int i=0;i<buttons.length;i++){
    int[] pos=new int[2];buttons[i].getLocationOnScreen(pos);
    float center=pos[0]+buttons[i].getWidth()/2f;
    float dist=Math.abs(rawX-center);
    if(dist<bestDist){bestDist=dist;best=i;}
   }
   return best;
  }
  void beginDockDrag(int origin){
   cancelIndicatorAnimator();
   layoutIndicator(origin);
   paintLabels(origin);
   dockHoverTab=origin;
  }
  void followDockTouch(float rawX){
   int target=closestTab(rawX);
   if(target==dockHoverTab)return;
   dockHoverTab=target;
   // Bottom-bar drag is intentionally discrete: jump box-to-box.
   // Smooth interpolation is reserved for page swipes.
   layoutIndicator(target);
   paintLabels(target);
  }
  int dockHoverTarget(int fallback){
   return dockHoverTab>=0?dockHoverTab:fallback;
  }
  void finishDockDrag(int origin,int target,Runnable after){
   if(origin<0||target<0)return;
   cancelIndicatorAnimator();
   layoutIndicator(target);
   paintLabels(target);
   dockHoverTab=-1;
   if(after!=null)after.run();
  }
  void layoutIndicator(int index){
   if(shell.getWidth()<=0||widthFor(index)<=1){
    shell.post(()->layoutIndicator(index));return;
   }
   FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)indicator.getLayoutParams();
   lp.width=widthFor(index);
   lp.height=heightFor(index);
   // The indicator is positioned by absolute margins, never View.setX().
   // This avoids RTL relayout changing the indicator's base left after its
   // width changes, which previously shifted the box exactly one tab.
   lp.gravity=Gravity.LEFT|Gravity.TOP;
   lp.leftMargin=Math.round(xFor(index));
   lp.topMargin=Math.round(yFor(index));
   indicator.setLayoutParams(lp);
   indicator.setTranslationX(0f);
   indicator.setTranslationY(0f);
   swipeBase=index;
   swipeProgress=0f;
  }
  void paintLabels(int selected){
   for(int i=0;i<buttons.length;i++){
    boolean on=i==selected;
    titles[i].setTextColor(on?NAVY:MUTED);
    glyphs[i].setColor(on?NAVY:MUTED);
    titles[i].setTypeface(Typeface.DEFAULT,on?Typeface.BOLD:Typeface.NORMAL);
    if(i==3&&bells[i]!=null){
     boolean enabled=Prefs.reminderEnabled(a);
     bells[i].setText(enabled?"🔔":"");
     bells[i].setTextColor(on?NAVY:TEAL);
     bells[i].setVisibility(enabled?View.VISIBLE:View.GONE);
    }
    buttons[i].setScaleX(on?1.035f:1f);
    buttons[i].setScaleY(on?1.035f:1f);
   }
  }
  void reset(boolean animate){
   int actual=activeTab(a);
   if(actual<0)return;
   cancelIndicatorAnimator();
   layoutIndicator(actual);
   paintLabels(actual);
  }
  void preview(int from,int to,float progress){
   if(from<0||from>=buttons.length||to<0||to>=buttons.length)return;
   if(indicatorAnimator!=null){indicatorAnimator.cancel();indicatorAnimator=null;}
   indicator.animate().cancel();
   applySwipeProgress(from,to,progress);
  }
  void settle(int from,int to,boolean commit,long duration){
   if(from<0||from>=buttons.length||to<0||to>=buttons.length)return;
   cancelIndicatorAnimator();
   if(swipeBase!=from)layoutIndicator(from);
   float start=swipeProgress;
   float end=commit?1f:0f;
   indicatorAnimator=ValueAnimator.ofFloat(start,end);
   indicatorAnimator.setDuration(duration);
   indicatorAnimator.setInterpolator(new android.view.animation.DecelerateInterpolator());
   indicatorAnimator.addUpdateListener(anim->
     applySwipeProgress(from,to,(Float)anim.getAnimatedValue()));
   indicatorAnimator.addListener(new android.animation.AnimatorListenerAdapter(){
    @Override public void onAnimationEnd(android.animation.Animator animation){
     indicatorAnimator=null;
     if(!commit){
      layoutIndicator(from);
      paintLabels(from);
     }
    }
   });
   indicatorAnimator.start();
  }
 }

 private static FrameLayout mainNav(Activity a,int active){
  FrameLayout shell=new FrameLayout(a);
  shell.setPadding(dp(a,8),dp(a,7),dp(a,8),dp(a,9));
  shell.setBackground(rounded(a,WHITE,18,BORDER));
  shell.setElevation(dp(a,9));

  View indicator=new View(a);
  indicator.setAlpha(0f);
  indicator.setBackground(rounded(a,0xffe4f2f4,13,TEAL));
  FrameLayout.LayoutParams indicatorLp=new FrameLayout.LayoutParams(1,dp(a,63),Gravity.LEFT|Gravity.TOP);
  shell.addView(indicator,indicatorLp);

  LinearLayout bar=new LinearLayout(a);
  bar.setOrientation(LinearLayout.HORIZONTAL);
  bar.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
  bar.setGravity(Gravity.CENTER_VERTICAL);
  FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(-1,-1);
  bp.leftMargin=0;bp.rightMargin=0;bp.topMargin=0;bp.bottomMargin=0;
  shell.addView(bar,bp);

  LinearLayout[] buttons=new LinearLayout[TABS.length];
  TextView[] titles=new TextView[TABS.length];
  NavGlyph[] glyphs=new NavGlyph[TABS.length];
  TextView[] bells=new TextView[TABS.length];

  for(int i=0;i<TABS.length;i++){
   LinearLayout b=new LinearLayout(a);buttons[i]=b;b.setOrientation(LinearLayout.VERTICAL);
   b.setGravity(Gravity.CENTER);b.setPadding(dp(a,1),dp(a,3),dp(a,1),dp(a,2));
   b.setBackgroundColor(Color.TRANSPARENT);
   NavGlyph icon=new NavGlyph(a,i);glyphs[i]=icon;
   b.addView(icon,new LinearLayout.LayoutParams(dp(a,24),dp(a,24)));
   TextView label=new TextView(a);titles[i]=label;label.setText(LABELS[i]);label.setTextSize(11);
   label.setGravity(Gravity.CENTER);label.setIncludeFontPadding(false);
   if(i==3){
    LinearLayout lower=new LinearLayout(a);lower.setOrientation(LinearLayout.HORIZONTAL);
    lower.setGravity(Gravity.CENTER);lower.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
    TextView bell=new TextView(a);bells[i]=bell;bell.setTextSize(10);bell.setGravity(Gravity.CENTER);
    bell.setIncludeFontPadding(false);
    lower.addView(label,new LinearLayout.LayoutParams(-2,dp(a,26)));
    LinearLayout.LayoutParams bellLp=new LinearLayout.LayoutParams(dp(a,18),dp(a,26));
    bellLp.setMargins(dp(a,3),0,0,0);lower.addView(bell,bellLp);
    b.addView(lower,new LinearLayout.LayoutParams(-1,dp(a,26)));
   }else b.addView(label,new LinearLayout.LayoutParams(-1,dp(a,26)));
   LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(a,63),1);
   lp.setMargins(dp(a,2),0,dp(a,2),0);bar.addView(b,lp);
   b.setContentDescription(LABELS[i]);
  }

  NavState state=new NavState(a,shell,bar,indicator,buttons,titles,glyphs,bells);
  shell.setTag(state);

  class DockDragTouch implements View.OnTouchListener{
   int origin=-1;
   @Override public boolean onTouch(View v,MotionEvent e){
    int action=e.getActionMasked();
    if(action==MotionEvent.ACTION_DOWN){
     origin=activeTab(a);
     if(origin<0)return false;
     state.beginDockDrag(origin);
     state.followDockTouch(e.getRawX());
     return true;
    }
    if(origin<0)return false;
    if(action==MotionEvent.ACTION_MOVE){
     state.followDockTouch(e.getRawX());
     return true;
    }
    if(action==MotionEvent.ACTION_UP){
     int target=state.dockHoverTarget(state.closestTab(e.getRawX()));
     int source=origin;origin=-1;
     if(target==source){
      state.finishDockDrag(source,source,null);
     }else{
      state.finishDockDrag(source,target,()->navigate(a,target,source));
     }
     return true;
    }
    if(action==MotionEvent.ACTION_CANCEL){
     int source=origin;origin=-1;
     state.finishDockDrag(source,source,null);
     return true;
    }
    return true;
   }
  }
  DockDragTouch dockDragTouch=new DockDragTouch();
  for(LinearLayout button:buttons){
   button.setClickable(true);
   button.setOnTouchListener(dockDragTouch);
  }
  shell.post(()->{
   state.reset(false);
   indicator.setAlpha(1f);
  });
  return shell;
 }
 public static void refresh(Activity a){
  NavState state=navState(a);
  if(state!=null)state.reset(false);
 }
 private static LinearLayout backBar(Activity a){
  LinearLayout bar=new LinearLayout(a);bar.setOrientation(LinearLayout.HORIZONTAL);
  bar.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);bar.setGravity(Gravity.CENTER_VERTICAL);
  bar.setPadding(dp(a,16),dp(a,8),dp(a,16),dp(a,8));
  bar.setBackground(rounded(a,WHITE,18,BORDER));bar.setElevation(dp(a,8));
  Button back=new Button(a);back.setAllCaps(false);back.setText("‹  بازگشت");
  back.setTextSize(15);back.setTextColor(WHITE);back.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
  back.setBackgroundTintList(null);back.setBackground(rounded(a,NAVY,14,0));
  back.setOnTouchListener((v,e)->{
   int type=e.getActionMasked();
   if(type==MotionEvent.ACTION_DOWN)v.animate().scaleX(.97f).scaleY(.97f).setDuration(90).start();
   if(type==MotionEvent.ACTION_UP||type==MotionEvent.ACTION_CANCEL)v.animate().scaleX(1).scaleY(1).setDuration(120).start();
   return false;
  });
  back.setOnClickListener(v->handleBack(a));
  LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,dp(a,52));
  bp.setMargins(dp(a,22),0,dp(a,22),0);bar.addView(back,bp);
  return bar;
 }
 private static void hideTopBack(View v,int depth){
  if(depth>4)return;
  if(v instanceof Button){
   Button b=(Button)v;String s=b.getText().toString().trim();
   if("‹".equals(s)||"←".equals(s)||"❮".equals(s))b.setVisibility(View.GONE);
  }else if(v instanceof ViewGroup){
   ViewGroup parent=(ViewGroup)v;
   for(int i=0;i<parent.getChildCount();i++)hideTopBack(parent.getChildAt(i),depth+1);
  }
 }
}
