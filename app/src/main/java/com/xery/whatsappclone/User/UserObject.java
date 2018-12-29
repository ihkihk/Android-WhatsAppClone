package com.xery.whatsappclone.User;

import java.io.Serializable;

public class UserObject implements Serializable {

    private String name, phone, uid, notificationKey;

    public UserObject(String uid) {
        this.uid = uid;
    }

    public UserObject(String uid, String name, String phone) {
        this.name = name;
        this.phone = phone;
        this.uid = uid;
    }

    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getUid() { return uid; }
    public String getNotificationKey() { return notificationKey; }

    public void setName(String name) { this.name = name; }
    public void setNotificationKey(String notificationKey) { this.notificationKey = notificationKey; }
}
