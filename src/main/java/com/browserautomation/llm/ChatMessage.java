package com.browserautomation.llm;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a chat message for LLM communication.
 * Supports text and image content parts.
 */
public class ChatMessage {

    public enum Role {
        SYSTEM, USER, ASSISTANT, TOOL
    }

    private final Role role;
    private final List<ContentPart> content;
    private final String toolCallId;

    private ChatMessage(Role role, List<ContentPart> content, String toolCallId) {
        this.role = role;
        this.content = content;
        this.toolCallId = toolCallId;
    }

    public static ChatMessage system(String text) {
        return new ChatMessage(Role.SYSTEM, List.of(ContentPart.text(text)), null);
    }

    public static ChatMessage user(String text) {
        return new ChatMessage(Role.USER, List.of(ContentPart.text(text)), null);
    }

    public static ChatMessage userWithImage(String text, String base64Image) {
        List<ContentPart> parts = new ArrayList<>();
        parts.add(ContentPart.text(text));
        parts.add(ContentPart.image(base64Image));
        return new ChatMessage(Role.USER, parts, null);
    }

    public static ChatMessage assistant(String text) {
        return new ChatMessage(Role.ASSISTANT, List.of(ContentPart.text(text)), null);
    }

    public static ChatMessage tool(String text, String toolCallId) {
        return new ChatMessage(Role.TOOL, List.of(ContentPart.text(text)), toolCallId);
    }

    public Role getRole() {
        return role;
    }

    public List<ContentPart> getContent() {
        return content;
    }

    public String getToolCallId() {
        return toolCallId;
    }

    /**
     * Get the text content of this message (first text part).
     */
    public String getText() {
        for (ContentPart part : content) {
            if (part.getType() == ContentPart.Type.TEXT) {
                return part.getText();
            }
        }
        return "";
    }

    public String getRoleString() {
        return switch (role) {
            case SYSTEM -> "system";
            case USER -> "user";
            case ASSISTANT -> "assistant";
            case TOOL -> "tool";
        };
    }

    /**
     * A content part within a message (text or image).
     */
    public static class ContentPart {

        public enum Type {
            TEXT, IMAGE
        }

        private final Type type;
        private final String text;
        private final String base64Image;

        private ContentPart(Type type, String text, String base64Image) {
            this.type = type;
            this.text = text;
            this.base64Image = base64Image;
        }

        public static ContentPart text(String text) {
            return new ContentPart(Type.TEXT, text, null);
        }

        public static ContentPart image(String base64Image) {
            return new ContentPart(Type.IMAGE, null, base64Image);
        }

        public Type getType() {
            return type;
        }

        public String getText() {
            return text;
        }

        public String getBase64Image() {
            return base64Image;
        }
    }
}
