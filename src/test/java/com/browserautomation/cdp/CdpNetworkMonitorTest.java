package com.browserautomation.cdp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CdpNetworkMonitorTest {

    @Test
    void testNetworkRequest() {
        var request = new CdpNetworkMonitor.NetworkRequest("req1", "https://api.com/data", "GET");
        assertEquals("req1", request.getRequestId());
        assertEquals("https://api.com/data", request.getUrl());
        assertEquals("GET", request.getMethod());
        assertFalse(request.isCompleted());
        assertEquals(0, request.getStatus());
        assertNull(request.getMimeType());

        request.setStatus(200);
        request.setMimeType("application/json");
        request.setCompleted(true);

        assertEquals(200, request.getStatus());
        assertEquals("application/json", request.getMimeType());
        assertTrue(request.isCompleted());
    }

    @Test
    void testNetworkRequestSetters() {
        var request = new CdpNetworkMonitor.NetworkRequest("req2", "https://cdn.com/image.png", "GET");
        request.setStatus(404);
        request.setMimeType("image/png");
        request.setCompleted(true);

        assertEquals(404, request.getStatus());
        assertEquals("image/png", request.getMimeType());
        assertTrue(request.isCompleted());
    }
}
