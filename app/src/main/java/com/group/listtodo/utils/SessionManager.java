package com.group.listtodo.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private SharedPreferences prefs;
    private static final String KEY_USER_ID = "user_id";

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences("AppSession", Context.MODE_PRIVATE);
    }

    public void saveUser(String userId) {
        prefs.edit().putString(KEY_USER_ID, userId).apply();
    }

    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    public void logout() {
        prefs.edit().clear().apply();
    }
}