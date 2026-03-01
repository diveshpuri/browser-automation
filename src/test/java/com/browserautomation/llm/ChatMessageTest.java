package com.browserautomation.llm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChatMessageTest {

    @Test
    void testSystemMessage() {
        ChatMessage msg = ChatMessage.system("You are helpful");
        assertEquals(ChatMessage.Role.SYSTEM, msg.getRole());
        assertEquals("system", msg.getRoleString());
        assertEquals("You are helpful", msg.getText());
        assertNull(msg.getToolCallId());
    }

    @Test
    void testUserMessage() {
        ChatMessage msg = ChatMessage.user("Hello");
        assertEquals(ChatMessage.Role.USER, msg.getRole());
        assertEquals("user", msg.getRoleString());
        assertEquals("Hello", msg.getText());
    }

    @Test
    void testAssistantMessage() {
        ChatMessage msg = ChatMessage.assistant("Response");
        assertEquals(ChatMessage.Role.ASSISTANT, msg.getRole());
        assertEquals("assistant", msg.getRoleString());
        assertEquals("Response", msg.getText());
    }

    @Test
    void testToolMessage() {
        ChatMessage msg = ChatMessage.tool("result", "call-123");
        assertEquals(ChatMessage.Role.TOOL, msg.getRole());
        assertEquals("tool", msg.getRoleString());
        assertEquals("result", msg.getText());
        assertEquals("call-123", msg.getToolCallId());
    }

    @Test
    void testUserWithImage() {
        ChatMessage msg = ChatMessage.userWithImage("describe this", "base64data");
        assertEquals(ChatMessage.Role.USER, msg.getRole());
        assertEquals(2, msg.getContent().size());
        assertEquals(ChatMessage.ContentPart.Type.TEXT, msg.getContent().get(0).getType());
        assertEquals(ChatMessage.ContentPart.Type.IMAGE, msg.getContent().get(1).getType());
        assertEquals("base64data", msg.getContent().get(1).getBase64Image());
    }

    @Test
    void testContentPart() {
        ChatMessage.ContentPart textPart = ChatMessage.ContentPart.text("hello");
        assertEquals(ChatMessage.ContentPart.Type.TEXT, textPart.getType());
        assertEquals("hello", textPart.getText());
        assertNull(textPart.getBase64Image());

        ChatMessage.ContentPart imagePart = ChatMessage.ContentPart.image("img-data");
        assertEquals(ChatMessage.ContentPart.Type.IMAGE, imagePart.getType());
        assertNull(imagePart.getText());
        assertEquals("img-data", imagePart.getBase64Image());
    }
}
