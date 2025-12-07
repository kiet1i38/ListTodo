package com.group.listtodo.utils;

import android.content.Context;

public class SyncHelper {

    public static void autoBackup(Context context) {
        FirestoreHelper.backupToCloud(context);
    }

    public static void restoreData(Context context, String userId, Runnable onSuccess) {

        FirestoreHelper.restoreFromCloud(context, userId, onSuccess);
    }
}
