package com.eluqen.shiftcalendar.work;

import com.eluqen.shiftcalendar.data.ShiftPlan;
import org.junit.Test;
import static org.junit.Assert.*;

public class CardexCompanyPolicyTest {
    @Test
    public void recognizesZobAhanOnly() {
        ShiftPlan zob = new ShiftPlan();
        zob.company = "ذوب آهن";
        ShiftPlan mobarakeh = new ShiftPlan();
        mobarakeh.company = "فولاد مبارکه";
        assertTrue(CardexCompanyPolicy.isZobAhan(zob));
        assertFalse(CardexCompanyPolicy.isZobAhan(mobarakeh));
    }

    @Test
    public void monthlyLimitAllowsFirstAndSecondButBlocksThird() {
        assertTrue(CardexCompanyPolicy.transitionAllowed(0, false, true, 2));
        assertTrue(CardexCompanyPolicy.transitionAllowed(1, false, true, 2));
        assertFalse(CardexCompanyPolicy.transitionAllowed(2, false, true, 2));
    }

    @Test
    public void editingExistingLimitedRecordDoesNotConsumeAnotherSlot() {
        assertTrue(CardexCompanyPolicy.transitionAllowed(2, true, true, 2));
    }

    @Test
    public void removingLimitedRecordIsAlwaysAllowed() {
        assertTrue(CardexCompanyPolicy.transitionAllowed(2, true, false, 2));
    }
}
