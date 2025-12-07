package com.group.listtodo.utils;

import android.content.Context;

public class SyncHelper {

    /**
     * HÀM 1: TỰ ĐỘNG SAO LƯU (AUTO BACKUP)
     * Hàm này được gọi tự động ở khắp nơi trong App (khi Thêm/Sửa/Xóa Task, Timer, Countdown).
     * Nó sẽ gọi FirestoreHelper để đẩy dữ liệu lên Firebase Cloud.
     */
    public static void autoBackup(Context context) {
        // Chuyển việc sao lưu sang cho FirestoreHelper xử lý
        FirestoreHelper.backupToCloud(context);
    }

    /**
     * HÀM 2: TỰ ĐỘNG KHÔI PHỤC (AUTO RESTORE)
     * Hàm này được gọi duy nhất 1 lần ở màn hình LoginActivity sau khi đăng nhập thành công.
     * Nó sẽ tải dữ liệu từ Firebase về và nạp vào máy.
     */
    public static void restoreData(Context context, String userId, Runnable onSuccess) {
        // Chuyển việc khôi phục sang cho FirestoreHelper xử lý
        FirestoreHelper.restoreFromCloud(context, userId, onSuccess);
    }
}