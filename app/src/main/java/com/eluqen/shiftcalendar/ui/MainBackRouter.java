package com.eluqen.shiftcalendar.ui;

import android.app.Activity;

/** Compatibility wrapper: root BACK behavior is owned by BackDock. */
public final class MainBackRouter {
 private MainBackRouter(){}
 public static boolean handle(Activity a){return BackDock.handleBack(a);}
}
