package cn.tee3.avei.view;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;

import cn.tee3.avei.R;

/**
 * 弹窗
 * Created by Administrator on 2017/6/23.
 */

public class AveiDialog {
    private static boolean isShowing;

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

    /**
     * 两个按钮的弹窗
     *
     * @param context
     */
    public static void finishDialog(final Context context, String titleStr, final MCallBack callBack) {
        final AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        alertDialog.setCancelable(true);
        alertDialog.setView(new EditText(context));
        alertDialog.show();
        Window window = alertDialog.getWindow();
        window.setContentView(R.layout.finish_dialog);
        TextView tv_title = (TextView) window.findViewById(R.id.tv_title);
        tv_title.setText(titleStr);

        TextView tv_cancel = (TextView) window.findViewById(R.id.tv_cancel);
        tv_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });

        TextView tv_ok = (TextView) window.findViewById(R.id.tv_ok);
        tv_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callBack.OnCallBackDispath(true);
                alertDialog.dismiss();
                isShowing = false;
            }
        });
    }

    public interface MCallBack {
        boolean OnCallBackDispath(Boolean bSucceed);
    }
}
