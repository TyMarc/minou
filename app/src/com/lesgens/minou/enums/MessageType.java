package com.lesgens.minou.enums;

public enum MessageType {
	IMAGE("image/jpeg"),
    TEXT("text/plain"),
    VIDEO("video/mp4");
    
    private final String stringValue;
    
    MessageType(String stringValue) {
        this.stringValue = stringValue;
    }
    
    @Override
    public String toString() {
        return stringValue;
    }
    
    public static MessageType fromString(String msgType) {
        if (msgType == null) return null;
        else if (msgType.equals("image/jpeg")) return IMAGE;
        else if (msgType.equals("text/plain")) return TEXT;
        else if (msgType.equals("video/mp4")) return VIDEO;
        return TEXT;
    }

}
