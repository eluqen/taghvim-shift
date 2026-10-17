package com.eluqen.shiftcalendar.ui;

/** View-only privacy policy. Saved note contents remain unchanged. */
public final class NotebookPrivacy {
    private NotebookPrivacy() {}

    public static String listTitle(String title, boolean privateNote) {
        if (privateNote) return "یادداشت خصوصی";
        String display = title == null ? "" : title.trim();
        return display.isEmpty() ? "یادداشت بدون عنوان" : display;
    }

    public static String listPreview(String body, boolean privateNote) {
        return privateNote || body == null ? "" : body.trim();
    }
}
