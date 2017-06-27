package cn.tee3.avei.view;

import android.app.AlertDialog;
import android.content.Context;
import android.view.Window;
import android.widget.EditText;

import cn.tee3.avei.R;

/**
 * 弹窗
 * Created by Administrator on 2017/6/23.
 */

public class AveiDialog {

    /**
     * 提示安排房间
     *
     * @param context
     */
    public static void ScheduleRoomDialog(final Context context) {
        final AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        alertDialog.setCancelable(true);
        alertDialog.setCanceledOnTouchOutside(true);
        alertDialog.setView(new EditText(context));
        alertDialog.show();
        Window window = alertDialog.getWindow();
        window.setContentView(R.layout.schedule_room_dialog);
    }
}
