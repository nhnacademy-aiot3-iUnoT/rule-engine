package com.nhnacademy.ruleengine.engine.notification;
import com.nhnacademy.ruleengine.engine.dto.notification.NotificationChannel;
import com.nhnacademy.ruleengine.engine.dto.notification.NotificationPreference;
import com.nhnacademy.ruleengine.engine.dto.notification.NotificationRequest;

public interface NotificationSender {
    NotificationChannel channel();
    void send(NotificationRequest request, NotificationPreference preference);

}
