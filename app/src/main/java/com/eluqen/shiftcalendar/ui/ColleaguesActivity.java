package com.eluqen.shiftcalendar.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.eluqen.shiftcalendar.data.AppDatabase;
import com.eluqen.shiftcalendar.data.Colleague;
import java.util.Iterator;
import java.util.List;

public final class ColleaguesActivity extends BaseActivity {
    private AppDatabase database;
    private LinearLayout list;

    @Override
    protected void onActivityResult(int i, int i2, Intent intent) {
        if (ColleagueImport.result(this, i, i2, intent)) {
            return;
        }
        super.onActivityResult(i, i2, intent);
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.database = AppDatabase.get(this);
        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void buildUi() {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
        linearLayout.setBackgroundColor(Ui.SURFACE);
        linearLayout.addView(Ui.toolbar(this, "هم\u200cشیفت\u200cها", true));
        Button button = Ui.button(this, "افزودن همکار");
        button.setOnClickListener(ColleaguesActivity.createOnClickCallback8(this));
        linearLayout.addView(button, Ui.margins(this, -1, Ui.dp(this, 50.0f), 14, 12, 14, 6));
        ScrollView scrollView = new ScrollView(this);
        linearLayout.addView(scrollView, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        LinearLayout linearLayout2 = new LinearLayout(this);
        this.list = linearLayout2;
        linearLayout2.setOrientation(android.widget.LinearLayout.VERTICAL);
        this.list.setPadding(Ui.dp(this, 12.0f), Ui.dp(this, 5.0f), Ui.dp(this, 12.0f), Ui.dp(this, 24.0f));
        scrollView.addView(this.list);
        setContentView(linearLayout);
    }

    void handleBuildUiCallback0(View view) {
        edit(null);
    }

    private void refresh() {
        this.list.removeAllViews();
        List<Colleague> listColleagues = this.database.colleagues();
        if (listColleagues.isEmpty()) {
            TextView textViewText = Ui.text(this, "هنوز همکاری ثبت نشده است.", 15.0f, Ui.MUTED);
            textViewText.setGravity(17);
            this.list.addView(textViewText, new LinearLayout.LayoutParams(-1, Ui.dp(this, 130.0f)));
        } else {
            Iterator<Colleague> it = listColleagues.iterator();
            while (it.hasNext()) {
                this.list.addView(card(it.next()), Ui.margins(this, -1, -2, 0, 0, 0, 10));
            }
        }
    }

    private View card(Colleague colleague) {
        LinearLayout linearLayoutCard = Ui.card(this);
        linearLayoutCard.addView(Ui.title(this, colleague.name, 18.0f));
        String strJoinNonEmpty = joinNonEmpty(" — ", colleague.team, colleague.role);
        if (!strJoinNonEmpty.isEmpty()) {
            linearLayoutCard.addView(Ui.text(this, strJoinNonEmpty, 14.0f, Ui.MUTED));
        }
        String strJoinNonEmpty2 = joinNonEmpty("  |  ", colleague.mobile1, colleague.mobile2, colleague.phone);
        if (!strJoinNonEmpty2.isEmpty()) {
            linearLayoutCard.addView(Ui.text(this, strJoinNonEmpty2, 14.0f, Ui.MUTED), Ui.margins(this, -1, -2, 0, 5, 0, 0));
        }
        if (!colleague.info.trim().isEmpty()) {
            linearLayoutCard.addView(Ui.text(this, colleague.info, 13.0f, Ui.MUTED));
        }
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        linearLayout.setLayoutDirection(android.view.View.LAYOUT_DIRECTION_RTL);
        Button button = Ui.tonalButton(this, "تماس");
        button.setEnabled(!firstPhone(colleague).isEmpty());
        button.setOnClickListener(ColleaguesActivity.createOnClickCallback2(this, colleague));
        Button button2 = Ui.tonalButton(this, "پیامک");
        button2.setEnabled(true ^ firstMobile(colleague).isEmpty());
        button2.setOnClickListener(ColleaguesActivity.createOnClickCallback3(this, colleague));
        Button button3 = Ui.tonalButton(this, "ویرایش");
        button3.setOnClickListener(ColleaguesActivity.createOnClickCallback4(this, colleague));
        linearLayout.addView(button, actionParams());
        linearLayout.addView(button2, actionParams());
        linearLayout.addView(button3, actionParams());
        linearLayoutCard.addView(linearLayout, Ui.margins(this, -1, Ui.dp(this, 48.0f), 0, 10, 0, 0));
        linearLayoutCard.setOnLongClickListener(ColleaguesActivity.createOnLongClickCallback5(this, colleague));
        return linearLayoutCard;
    }

    void handleCardCallback1(Colleague colleague, View view) {
        dial(firstPhone(colleague));
    }

    void handleCardCallback2(Colleague colleague, View view) {
        sms(firstMobile(colleague));
    }

    void handleCardCallback3(Colleague colleague, View view) {
        edit(colleague);
    }

    boolean handleCardCallback4(Colleague colleague, View view) {
        confirmDelete(colleague);
        return true;
    }

    private void edit(Colleague colleague) {
        Colleague colleague2 = colleague == null ? new Colleague() : colleague;
        ScrollView scrollView = new ScrollView(this);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
        linearLayout.setPadding(Ui.dp(this, 18.0f), Ui.dp(this, 8.0f), Ui.dp(this, 18.0f), Ui.dp(this, 8.0f));
        scrollView.addView(linearLayout);
        EditText editTextField = field(linearLayout, "نام و نام خانوادگی", colleague2.name);
        EditText editTextField2 = field(linearLayout, "گروه یا شیفت", colleague2.team);
        EditText editTextField3 = field(linearLayout, "سمت", colleague2.role);
        EditText editTextPhoneField = phoneField(linearLayout, "موبایل اول", colleague2.mobile1);
        ColleagueImport.attach(this, linearLayout, editTextField, editTextPhoneField);
        EditText editTextPhoneField2 = phoneField(linearLayout, "موبایل دوم", colleague2.mobile2);
        EditText editTextPhoneField3 = phoneField(linearLayout, "تلفن", colleague2.phone);
        EditText editTextField4 = field(linearLayout, "توضیحات", colleague2.info);
        AlertDialog alertDialogCreate = new AlertDialog.Builder(this).setTitle(colleague == null ? "همکار جدید" : "ویرایش همکار").setView(scrollView).setPositiveButton("ذخیره", (DialogInterface.OnClickListener) null).setNeutralButton(colleague == null ? null : "حذف", (DialogInterface.OnClickListener) null).setNegativeButton("انصراف", (DialogInterface.OnClickListener) null).create();
        alertDialogCreate.setOnShowListener(ColleaguesActivity.createOnShowCallback7(this, alertDialogCreate, editTextField, colleague2, editTextField2, editTextField3, editTextPhoneField, editTextPhoneField2, editTextPhoneField3, editTextField4, colleague));
        alertDialogCreate.show();
    }

    void handleEditCallback7(AlertDialog alertDialog, EditText editText, Colleague colleague, EditText editText2, EditText editText3, EditText editText4, EditText editText5, EditText editText6, EditText editText7, Colleague colleague2, DialogInterface dialogInterface) {
        alertDialog.getButton(-1).setOnClickListener(ColleaguesActivity.createOnClickCallback0(this, editText, colleague, editText2, editText3, editText4, editText5, editText6, editText7, alertDialog));
        if (colleague2 != null) {
            alertDialog.getButton(-3).setOnClickListener(ColleaguesActivity.createOnClickCallback1(this, alertDialog, colleague));
        }
    }

    void handleEditCallback5(EditText editText, Colleague colleague, EditText editText2, EditText editText3, EditText editText4, EditText editText5, EditText editText6, EditText editText7, AlertDialog alertDialog, View view) {
        String strText = text(editText);
        if (strText.isEmpty()) {
            Ui.toast(this, "نام همکار را وارد کنید");
            return;
        }
        colleague.name = strText;
        colleague.team = text(editText2);
        colleague.role = text(editText3);
        colleague.mobile1 = text(editText4);
        colleague.mobile2 = text(editText5);
        colleague.phone = text(editText6);
        colleague.info = text(editText7);
        this.database.saveColleague(colleague);
        alertDialog.dismiss();
        refresh();
    }

    void handleEditCallback6(AlertDialog alertDialog, Colleague colleague, View view) {
        alertDialog.dismiss();
        confirmDelete(colleague);
    }

    private EditText field(LinearLayout linearLayout, String str, String str2) {
        EditText editTextInput = Ui.input(this, str);
        if (str2 == null) {
            str2 = "";
        }
        editTextInput.setText(str2);
        linearLayout.addView(editTextInput, Ui.margins(this, -1, -2, 0, 5, 0, 5));
        return editTextInput;
    }

    private EditText phoneField(LinearLayout linearLayout, String str, String str2) {
        EditText editTextField = field(linearLayout, str, str2);
        editTextField.setInputType(3);
        editTextField.setTextDirection(3);
        return editTextField;
    }

    private void confirmDelete(Colleague colleague) {
        new AlertDialog.Builder(this).setMessage("«" + colleague.name + "» حذف شود؟").setPositiveButton("حذف", ColleaguesActivity.createOnClickCallback6(this, colleague)).setNegativeButton("انصراف", (DialogInterface.OnClickListener) null).show();
    }

    void handleCallback9(Colleague colleague, DialogInterface dialogInterface, int i) {
        this.database.deleteColleague(colleague.f0id);
        refresh();
    }

    private void dial(String str) {
        if (str.isEmpty()) return;
        try {
            startActivity(new Intent(
                    Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(str))));
        } catch (Exception e) {
            Ui.toast(this, "برنامه تماس در دستگاه پیدا نشد");
        }
    }

    private void sms(String str) {
        if (str.isEmpty()) return;
        try {
            startActivity(new Intent(
                    Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(str))));
        } catch (Exception e) {
            Ui.toast(this, "برنامه پیامک در دستگاه پیدا نشد");
        }
    }

    private LinearLayout.LayoutParams actionParams() {
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, Ui.dp(this, 44.0f), 1.0f);
        layoutParams.setMargins(Ui.dp(this, 2.0f), 0, Ui.dp(this, 2.0f), 0);
        return layoutParams;
    }

    private static String text(EditText editText) {
        return editText.getText().toString().trim();
    }

    private static String firstMobile(Colleague colleague) {
        return (!colleague.mobile1.trim().isEmpty() ? colleague.mobile1 : colleague.mobile2).trim();
    }

    private static String firstPhone(Colleague colleague) {
        String strFirstMobile = firstMobile(colleague);
        return !strFirstMobile.isEmpty() ? strFirstMobile : colleague.phone.trim();
    }

    private static String joinNonEmpty(String str, String... strArr) {
        StringBuilder sb = new StringBuilder();
        for (String str2 : strArr) {
            if (str2 != null && !str2.trim().isEmpty()) {
                if (sb.length() > 0) {
                    sb.append(str);
                }
                sb.append(str2.trim());
            }
        }
        return sb.toString();
    }

    private static View.OnClickListener createOnClickCallback1(final ColleaguesActivity capture0, final AlertDialog capture1, final Colleague capture2) {
        return (view) -> {
        capture0.handleEditCallback6(capture1, capture2, view);
        };
    }

    private static DialogInterface.OnShowListener createOnShowCallback7(final ColleaguesActivity capture0, final AlertDialog capture1, final EditText capture2, final Colleague capture3, final EditText capture4, final EditText capture5, final EditText capture6, final EditText capture7, final EditText capture8, final EditText capture9, final Colleague capture10) {
        return (dialogInterface) -> {
        capture0.handleEditCallback7(capture1, capture2, capture3, capture4, capture5, capture6, capture7, capture8, capture9, capture10, dialogInterface);
        };
    }

    private static View.OnClickListener createOnClickCallback4(final ColleaguesActivity capture0, final Colleague capture1) {
        return (view) -> {
        capture0.handleCardCallback3(capture1, view);
        };
    }

    private static View.OnClickListener createOnClickCallback0(final ColleaguesActivity capture0, final EditText capture1, final Colleague capture2, final EditText capture3, final EditText capture4, final EditText capture5, final EditText capture6, final EditText capture7, final EditText capture8, final AlertDialog capture9) {
        return (view) -> {
        capture0.handleEditCallback5(capture1, capture2, capture3, capture4, capture5, capture6, capture7, capture8, capture9, view);
        };
    }

    private static View.OnClickListener createOnClickCallback2(final ColleaguesActivity capture0, final Colleague capture1) {
        return (view) -> {
        capture0.handleCardCallback1(capture1, view);
        };
    }

    private static View.OnLongClickListener createOnLongClickCallback5(final ColleaguesActivity capture0, final Colleague capture1) {
        return (view) -> {
        return capture0.handleCardCallback4(capture1, view);
        };
    }

    private static DialogInterface.OnClickListener createOnClickCallback6(final ColleaguesActivity capture0, final Colleague capture1) {
        return (dialogInterface, i) -> {
        capture0.handleCallback9(capture1, dialogInterface, i);
        };
    }

    private static View.OnClickListener createOnClickCallback3(final ColleaguesActivity capture0, final Colleague capture1) {
        return (view) -> {
        capture0.handleCardCallback2(capture1, view);
        };
    }

    private static View.OnClickListener createOnClickCallback8(final ColleaguesActivity capture0) {
        return (view) -> {
        capture0.handleBuildUiCallback0(view);
        };
    }
}
