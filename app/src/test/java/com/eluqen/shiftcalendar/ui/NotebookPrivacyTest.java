package com.eluqen.shiftcalendar.ui;

import org.junit.Test;
import static org.junit.Assert.*;

public final class NotebookPrivacyTest {
    @Test public void privateNoteNeverShowsItsTitleInList() {
        assertEquals("یادداشت خصوصی",
            NotebookPrivacy.listTitle("اطلاعات کاملاً محرمانه", true));
    }
    @Test public void privateNoteNeverShowsItsBodyInList() {
        assertEquals("", NotebookPrivacy.listPreview("متن محرمانه با چند خط", true));
    }
    @Test public void normalNoteStillShowsTitleAndPreview() {
        assertEquals("قرار ملاقات", NotebookPrivacy.listTitle(" قرار ملاقات ", false));
        assertEquals("توضیحات", NotebookPrivacy.listPreview(" توضیحات ", false));
    }
    @Test public void missingValuesDoNotLeakOrCrash() {
        assertEquals("یادداشت بدون عنوان", NotebookPrivacy.listTitle(null, false));
        assertEquals("", NotebookPrivacy.listPreview(null, false));
    }
}
