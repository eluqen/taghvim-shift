package com.eluqen.shiftcalendar.ui;

import org.junit.Test;
import static org.junit.Assert.*;

public final class PlannerCompletionVisualTest {
    @Test public void completedCardContrastsWithAllNineThemeCardColors() {
        int[] themes={0xfffbfdff,0xffeaf6f9,0xfff1ebf8,0xffeaf4ee,0xfff8eaef,
                      0xfff7f9fa,0xfffffaf4,0xfff2f9fb,0xfffff6f2};
        for(int card:themes) {
            assertNotEquals(card, PlannerCompletionVisual.cardFill(true,card));
            assertEquals(card,PlannerCompletionVisual.cardFill(false,card));
            assertEquals(PlannerCompletionVisual.DONE_BORDER,
                         PlannerCompletionVisual.cardOutline(true,0xffcccccc));
        }
    }
    @Test public void completedBadgeIsExplicitAndReadable() {
        assertTrue(PlannerCompletionVisual.badge(true).contains("انجام‌شده"));
        assertTrue(PlannerCompletionVisual.badge(true).contains("✓"));
        assertTrue(PlannerCompletionVisual.badge(false).contains("در انتظار"));
        assertTrue(PlannerCompletionVisual.contrast(0xffffffff,PlannerCompletionVisual.DONE_BADGE)>=4.5);
        assertTrue(PlannerCompletionVisual.contrast(PlannerCompletionVisual.DONE_TEXT,PlannerCompletionVisual.DONE_CARD)>=7);
    }
    @Test public void restoreIsBrightAndLegibleInsteadOfDisabledLooking() {
        int ink=PlannerCompletionVisual.actionInk(true,PlannerCompletionVisual.TOGGLE,0);
        int bg=PlannerCompletionVisual.actionFill(true,PlannerCompletionVisual.TOGGLE,0,0);
        assertTrue(PlannerCompletionVisual.contrast(ink,bg)>=7);
        assertNotEquals(bg,PlannerCompletionVisual.actionFill(false,PlannerCompletionVisual.TOGGLE,0xff203a63,0xffedf3fb));
    }
    @Test public void editDeleteAndRestoreRemainDifferentFunctions() {
        int primary=0xff203a63, neutral=0xffedf3fb;
        for(boolean done:new boolean[]{false,true}) {
            int edit=PlannerCompletionVisual.actionFill(done,PlannerCompletionVisual.EDIT,primary,neutral);
            int finish=PlannerCompletionVisual.actionFill(done,PlannerCompletionVisual.TOGGLE,primary,neutral);
            assertNotEquals(edit,finish);
            assertNotEquals(PlannerCompletionVisual.actionInk(done,PlannerCompletionVisual.EDIT,0xff182b45),
                            PlannerCompletionVisual.actionInk(done,PlannerCompletionVisual.DELETE,0xff182b45));
        }
    }
}
