package com.bookmyroute.dto.response;

public class EmailDeliveryResponse {
    private boolean sent;
    private boolean configured;
    private String recipient;
    private String message;

    public EmailDeliveryResponse() {}

    public EmailDeliveryResponse(boolean sent, boolean configured, String recipient, String message) {
        this.sent = sent;
        this.configured = configured;
        this.recipient = recipient;
        this.message = message;
    }

    public static EmailDeliveryResponse sent(String recipient) {
        return new EmailDeliveryResponse(true, true, recipient, "Email sent successfully");
    }

    public static EmailDeliveryResponse skipped(String recipient, String message) {
        return new EmailDeliveryResponse(false, false, recipient, message);
    }

    public static EmailDeliveryResponse failed(String recipient, boolean configured, String message) {
        return new EmailDeliveryResponse(false, configured, recipient, message);
    }

    public boolean isSent() { return sent; }
    public void setSent(boolean sent) { this.sent = sent; }
    public boolean isConfigured() { return configured; }
    public void setConfigured(boolean configured) { this.configured = configured; }
    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
