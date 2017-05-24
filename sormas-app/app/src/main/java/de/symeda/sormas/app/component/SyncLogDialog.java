package de.symeda.sormas.app.component;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.design.widget.Snackbar;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.analytics.Tracker;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.symeda.sormas.api.utils.DateHelper;
import de.symeda.sormas.app.R;
import de.symeda.sormas.app.backend.common.DatabaseHelper;
import de.symeda.sormas.app.backend.user.SyncLog;
import de.symeda.sormas.app.util.ErrorReportingHelper;

/**
 * Created by Mate Strysewske on 24.05.2017.
 */

public class SyncLogDialog {

    private static int INITIAL_LOG_LIMIT = 50;

    private AlertDialog.Builder builder;
    private AlertDialog dialog;

    private int lastDisplayCount = 0;
    private int displayCount = INITIAL_LOG_LIMIT;
    private List<SyncLog> logs = new ArrayList<>();
    private StringBuilder content = new StringBuilder();
    private Date lastDate;

    public SyncLogDialog(Context context) {
        builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.headline_sync_log);
        builder.setPositiveButton(R.string.action_loadMore, null);
        builder.setNeutralButton(R.string.action_close, null);
    }

    public void show(final Context context) {
        // TODO needs to be removed!
        ConnectionSource connectionSource = DatabaseHelper.getSyncLogDao().getConnectionSource();
        try {
            TableUtils.clearTable(connectionSource, SyncLog.class);
        } catch (SQLException e) {
            throw new RuntimeException();
        }

        for (int i = 0; i <= 129; i++) {
            SyncLog newLog = DatabaseHelper.getSyncLogDao().create("Entity " + (i + 1), "ABCDE-" + (i + 1), "Sample Status, Shipment Status");
            if (i <= 10) {
                newLog.setCreationDate(DateHelper.subtractDays(new Date(), 5));
            }
            if (i >= 70) {
                newLog.setCreationDate(DateHelper.addDays(new Date(), 7));
            }
            logs.add(newLog);
        }
        // End of to do

        dialog = builder.create();
        dialog.setCancelable(true);

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                positiveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (logs.size() > lastDisplayCount) {
                            buildAndDisplayAlertContent(context);
                        } else {
                            Snackbar.make(dialog.getWindow().getDecorView(), R.string.snackbar_no_more_entries, Snackbar.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });

        buildAndDisplayAlertContent(context);
        dialog.show();
    }

    private void buildAndDisplayAlertContent(Context context) {
        if (logs.size() == 0) {
            dialog.setMessage(context.getString(R.string.hint_no_sync_errors));
        } else {
            for (int i = lastDisplayCount; i < displayCount; i++) {
                if (i >= logs.size()) {
                    break;
                }
                SyncLog log = logs.get(i);
                if (lastDate != null && DateHelper.isSameDay(lastDate, log.getCreationDate())) {
                    content.append("<p><b>" + log.getEntityName() + " (" + log.getEntityUuid() + ")</b><br/><i>" + context.getString(R.string.headline_overridden_fields) + " </i> " + log.getConflictText() + "</p>");
                } else {
                    if (lastDate != null) {
                        content.append("<br/>");
                    }
                    content.append("<p><b><u>" + DateHelper.formatDate(log.getCreationDate()) + "</u></b></p><p><b>" + log.getEntityName() + " (" + log.getEntityUuid() + ")</b><br/><i>" + context.getString(R.string.headline_overridden_fields) + " </i> " + log.getConflictText() + "</p>");
                }
                lastDate = log.getCreationDate();
            }

            lastDisplayCount = displayCount;
            displayCount += INITIAL_LOG_LIMIT;

            dialog.setMessage(Html.fromHtml(content.toString()));
        }
    }

}
