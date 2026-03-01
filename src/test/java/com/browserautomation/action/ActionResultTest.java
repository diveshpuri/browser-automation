package com.browserautomation.action;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ActionResultTest {

    @Test
    void testSuccess() {
        ActionResult result = ActionResult.success();
        assertTrue(result.isSuccess());
        assertNull(result.getExtractedContent());
        assertNull(result.getError());
        assertFalse(result.isDone());
    }

    @Test
    void testSuccessWithContent() {
        ActionResult result = ActionResult.success("extracted data");
        assertTrue(result.isSuccess());
        assertEquals("extracted data", result.getExtractedContent());
        assertNull(result.getError());
        assertFalse(result.isDone());
    }

    @Test
    void testError() {
        ActionResult result = ActionResult.error("something went wrong");
        assertFalse(result.isSuccess());
        assertNull(result.getExtractedContent());
        assertEquals("something went wrong", result.getError());
        assertFalse(result.isDone());
    }

    @Test
    void testDone() {
        ActionResult result = ActionResult.done("task completed");
        assertTrue(result.isSuccess());
        assertEquals("task completed", result.getExtractedContent());
        assertNull(result.getError());
        assertTrue(result.isDone());
    }

    @Test
    void testToString() {
        assertTrue(ActionResult.success().toString().contains("SUCCESS"));
        assertTrue(ActionResult.error("err").toString().contains("ERROR"));
        assertTrue(ActionResult.done("done").toString().contains("DONE"));
    }
}
