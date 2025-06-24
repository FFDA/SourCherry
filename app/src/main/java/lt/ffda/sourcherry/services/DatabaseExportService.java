/*
 * This file is part of SourCherry.
 *
 * SourCherry is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SourCherry is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SourCherry. If not, see <https://www.gnu.org/licenses/>.
 */

package lt.ffda.sourcherry.services;

import static lt.ffda.sourcherry.utils.Constants.DATABASE_EXPORT_NOTI;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import android.os.Looper;
import android.provider.DocumentsContract;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.ServiceCompat;
import androidx.preference.PreferenceManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lt.ffda.sourcherry.MainActivity;
import lt.ffda.sourcherry.R;
import lt.ffda.sourcherry.model.FileInfo;
import lt.ffda.sourcherry.utils.Constants;
import lt.ffda.sourcherry.utils.Files;
import lt.ffda.sourcherry.utils.PreferencesUtils;

/**
 * Service to automatically export internal database. Checks if it is still possible to export
 * database. If coping of the file succeeded and file was not corrupted. Stops processes if fail
 * is detected. Displays notification to user if it happens. In some circumstances can disable
 * MirrorDatabase settings.
 */
public class DatabaseExportService extends Service {

    private final String DB_RENAME_FORMATTING = "%1$s_%2$s.ctb";
    private final int ERROR_NOTIFICATION_ID = 2;
    private final String EXPORT_DB_EXTENSION = ".ctb";
    private final int PROGRESS_NOTIFICATION_ID = 1;
    private ServiceHandler serviceHandler;
    private Looper serviceLooper;

    /**
     * Creates notification with message with message about result of export task.
     * @param notificationTextId id of the text resource for the title of the notification.
     * @param contentText text for the notification to clarify the message for user. Can be null.
     */
    private void createNotification(@NonNull int notificationTextId, String contentText) {
        Context context = getApplicationContext();
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, DATABASE_EXPORT_NOTI)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(context.getString(notificationTextId))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setSilent(true)
                .setAutoCancel(true);
        if (contentText != null) {
            builder.setContentText(contentText);
        }
        NotificationManagerCompat.from(context).notify(ERROR_NOTIFICATION_ID, builder.build());
    }

    /**
     * Delete file
     * @param databaseUri uri of the file
     */
    private void deleteDatabase(Uri databaseUri) {
        try {
            DocumentsContract.deleteDocument(getContentResolver(), databaseUri);
        } catch (FileNotFoundException e) {
            // Silent fail. File was already deleted?
        }
    }

    /**
     * Deletes old backups of database leaving only set number. WILL DELETE ALL FILE WITH ".ctb" EXTENSION.
     * @param mirrorDbRoot Uri of mirrorDatabase root folder
     * @param dbFilename filnemae of the mirrorDatabase
     * @param toLeave count of backups to leave
     * @throws FileNotFoundException if it fails to delete a file
     */
    private void deleteExtraBackups(@NonNull Uri mirrorDbRoot, @NonNull String dbFilename, int toLeave) throws FileNotFoundException {
        Map<Long, String> filesToDelete = new HashMap<>();
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(mirrorDbRoot, DocumentsContract.getTreeDocumentId(mirrorDbRoot));
        try (Cursor cursor = getContentResolver().query(childrenUri, new String[]{"document_id", "_display_name", "last_modified"}, null, null, null)) {
            while (cursor != null && cursor.moveToNext()) {
                String fileName = cursor.getString(1);
                if (fileName.endsWith(EXPORT_DB_EXTENSION) && !dbFilename.equals(fileName)) {
                    filesToDelete.put(cursor.getLong(2), cursor.getString(0));
                }
            }
        }
        List<Long> ordererkeys = filesToDelete.keySet().stream().sorted().collect(Collectors.toList());
        for (int i = 0; i < ordererkeys.size() - toLeave; i++) {
            DocumentsContract.deleteDocument(getContentResolver(),
                    DocumentsContract.buildDocumentUriUsingTree(mirrorDbRoot, filesToDelete.get(ordererkeys.get(i))));
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Notification notification = new NotificationCompat.Builder(this, Constants.DATABASE_EXPORT_NOTI)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(getString(R.string.dialog_fragment_export_database_message_exporting_database))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSilent(true)
                .build();
        ServiceCompat.startForeground(this, PROGRESS_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        HandlerThread thread = new HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        serviceLooper = thread.getLooper();
        serviceHandler = new ServiceHandler(serviceLooper);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Message msg = serviceHandler.obtainMessage();
        msg.arg1 = startId;
        serviceHandler.sendMessage(msg);
        return START_REDELIVER_INTENT;
    }

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            Context context = DatabaseExportService.this;
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            String mirrorDbFilename = sharedPreferences.getString("mirrorDatabaseFilename", null);
            FileInfo mirrorDbData = Files.getFileUriAndModDate(getContentResolver(),
                    sharedPreferences.getString("mirrorDatabaseFolderUri", null), mirrorDbFilename);

            if (mirrorDbData.getUri() == null) {
                createNotification(R.string.toast_error_failed_to_export_database, getString(R.string.noti_database_export_fail_no_export_database_disable_mirror_db));
                PreferencesUtils.disableMirrorDatabase(sharedPreferences);
                stopSelf(msg.arg1);
                return;
            }

            File internalDB = new File(sharedPreferences.getString("databaseUri", null));
            long mirrorDatabaseLastModified = sharedPreferences.getLong("mirrorDatabaseLastModified", 0);

            // No changes detected - nothing to do.
            if (internalDB.lastModified() <= mirrorDatabaseLastModified) {
                stopSelf(msg.arg1);
                return;
            }

            // Newer version in MirrorDatabase folder. Will not overwrite.
            if (mirrorDatabaseLastModified < mirrorDbData.getModified()) {
                createNotification(R.string.toast_error_failed_to_export_database, getString(R.string.noti_database_export_fail_import_database_newer));
                stopSelf(msg.arg1);
                return;
            }

            Uri savedMirrorDbFolderUri = Uri.parse(sharedPreferences.getString("mirrorDatabaseFolderUri", null));
            Uri mirrorDbFolderUri; // This is real Uri that should have all permissions to create files in mirrorDB folder
            Uri exportDBUri = null;

            // If temp database file is in the folder - export was killed by OS. Retrying.
            FileInfo tmpExportDbData = Files.getFileUriAndModDate(getContentResolver(),
                    sharedPreferences.getString("mirrorDatabaseFolderUri", null), String.format("%1$s_tmp.ctb", Files.getFileName(mirrorDbFilename)));
            if (tmpExportDbData.getUri() != null) {
                exportDBUri = tmpExportDbData.getUri();
            }

            try {
                mirrorDbFolderUri = DocumentsContract.buildDocumentUriUsingTree(savedMirrorDbFolderUri,
                        DocumentsContract.getTreeDocumentId(savedMirrorDbFolderUri));
                if (exportDBUri == null) {
                    exportDBUri = DocumentsContract.createDocument(getContentResolver(), mirrorDbFolderUri, "*/*",
                            String.format("%1$s_tmp.ctb", Files.getFileName(mirrorDbFilename)));
                }
            } catch (FileNotFoundException e) {
                createNotification(R.string.toast_error_failed_to_export_database, getString(R.string.noti_database_export_fail_no_export_database));
                stopSelf(msg.arg1);
                return;
            }

            try (InputStream inputStream = new FileInputStream(internalDB);
                 OutputStream outputStream = getContentResolver().openOutputStream(exportDBUri, "wt");) {
                byte[] buf = new byte[8 * 1024];
                int len;
                while ((len = inputStream.read(buf)) > 0) {
                    outputStream.write(buf, 0, len);
                }
            } catch (IOException e) {
                createNotification(R.string.toast_error_failed_to_export_database, getString(R.string.noti_database_export_failed_to_copy));
                stopSelf(msg.arg1);
                return;
            }

            // Checking hash'es to make sure that files are the same
            try (InputStream inputStream = new FileInputStream(internalDB)) {
                String interanalSHA256Sum = Files.calculateFileSha256Sum(inputStream);
                String tempSHA256Sum = Files.calculateFileSha256Sum(context, exportDBUri);
                if (!interanalSHA256Sum.equals(tempSHA256Sum)) {
                    deleteDatabase(exportDBUri);
                    createNotification(R.string.toast_error_failed_to_export_database, getString(R.string.noti_database_export_fail_integrity_check));
                    stopSelf(msg.arg1);
                    return;
                }
            } catch (Exception e) {
                deleteDatabase(exportDBUri);
                createNotification(R.string.toast_error_failed_to_export_database, getString(R.string.noti_database_export_fail_integrity_check));
                stopSelf(msg.arg1);
                return;
            }

            // Renaming old DB to backup file and tmp DB to new export
            try {
                DocumentsContract.renameDocument(getContentResolver(), mirrorDbData.getUri(), String.format(DB_RENAME_FORMATTING, Files.getFileName(mirrorDbFilename), mirrorDbData.getModified()));
                DocumentsContract.renameDocument(getContentResolver(), exportDBUri, mirrorDbFilename);
            } catch (FileNotFoundException e) {
                PreferencesUtils.disableMirrorDatabase(sharedPreferences);
                createNotification(R.string.toast_error_failed_to_export_database, getString(R.string.noti_database_export_failed_rename_databases));
                stopSelf(msg.arg1);
                return;
            }

            try {
                deleteExtraBackups(savedMirrorDbFolderUri, mirrorDbFilename, 3);
            } catch (FileNotFoundException e) {
                createNotification(R.string.toast_error_failed_to_export_database, getString(R.string.noti_database_export_failed_delete_extra_backups));
                stopSelf(msg.arg1);
                return;
            }

            long newTimestamp;
            try {
                newTimestamp = Files.getFileTimestamp(getContentResolver(), mirrorDbData.getUri());
            } catch (Exception e) {
                newTimestamp = System.currentTimeMillis();
            }

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putLong("mirrorDatabaseLastModified", newTimestamp);
            editor.apply();

            stopSelf(msg.arg1);
        }
    }
}
