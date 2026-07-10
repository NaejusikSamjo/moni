package com.moni.notification.common.exception;

import com.moni.common.error.exception.CustomException;

public class NotificationException extends CustomException {

    public NotificationException(NotificationErrorCode errorCode) {
        super(errorCode);
    }
}
