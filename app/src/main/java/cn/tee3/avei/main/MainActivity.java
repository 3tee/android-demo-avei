package cn.tee3.avei.main;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.UUID;

import cn.tee3.avd.AVDEngine;
import cn.tee3.avd.RoomInfo;
import cn.tee3.avei.AveiApp;
import cn.tee3.avei.Constants;
import cn.tee3.avei.R;
import cn.tee3.avei.avimport.EncodedImportActivity;
import cn.tee3.avei.avimport.RawDataImportActivity;
import cn.tee3.avei.avimport.AVImporterDemoActivity;
import cn.tee3.avei.avimport.RtspClientImportActivity;
import cn.tee3.avei.export.LocalRecordAndExportActivity;
import cn.tee3.avei.model.FunctionModel;
import cn.tee3.avei.utils.StringUtils;
import cn.tee3.avei.view.AveiDialog;

/**
 * 主界面（起始页）
 * <p>
 * Created by shengf on 2017/6/3.
 */

public class MainActivity extends Activity implements View.OnClickListener, AdapterView.OnItemClickListener, AveiApp.RoomListener {
    private static final String TAG = "MainActivity";

    private ListView lvFunctions;//功能列表
    private ArrayList<FunctionModel> functionModels = new ArrayList<>();
    private FunctionAdapter fAdapter;
    private TextView tvExplain;//模块说明
    private TextView tvJoinroom;//加入房间
    private EditText etRoomid;//房间Id
    private ImageView ivScheduleRoom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);
        initFunction();
        initView();
        initData();
        //设置房间查询、安排回调
        ((AveiApp) getApplication()).setRoomListener(this);
    }

    //房间查询、安排回调
    @Override
    public void RoomEvent(AveiApp.RoomEventType roomEventType, int ret, String roomIdResult) {
        if (roomEventType == AveiApp.RoomEventType.GetRoomResult) {//查询房间返回
            if (ret == 0) {
                Intent intent = new Intent(MainActivity.this, Constants.SELECT_FUNCTION.getmActivity().getClass());
                intent.putExtra("roomId", roomIdResult);
                startActivity(intent);
            } else {
                AveiDialog.ScheduleRoomDialog(this);
                Log.e(TAG, "GetRoomResult failed:" + ret);
            }
        } else {//安排房间返回
            if (ret == 0) {
                Intent intent = new Intent(MainActivity.this, Constants.SELECT_FUNCTION.getmActivity().getClass());
                intent.putExtra("roomId", roomIdResult);
                startActivity(intent);
            } else {
                Toast.makeText(MainActivity.this, "安排房间失败 ErrorCode:" + ret, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "ScheduleRoomResult failed:" + ret);
            }
        }
    }

    @Override
    public void onClick(View v) {
        String roomId = "";
        roomId = etRoomid.getText().toString().trim();
        if (StringUtils.isEmpty(roomId)) {
            roomId = "r2";
        }
        switch (v.getId()) {
            case R.id.tv_joinroom:
                //查询房间是否存在
                AVDEngine.instance().getRoomByRoomId(roomId);
                break;
            case R.id.iv_schedule_room:
                //安排房间
                RoomInfo roomInfo = new RoomInfo(UUID.randomUUID().toString(), RoomInfo.RoomMode_mcu, 5);
//                RoomInfo roomInfo = new RoomInfo(roomId, RoomInfo.RoomMode_mcu, 5);
                AVDEngine.instance().scheduleRoom(roomInfo);
                break;
            default:
                break;
        }
    }

    public void initView() {
        etRoomid = (EditText) findViewById(R.id.et_roomid);
        ivScheduleRoom = (ImageView) findViewById(R.id.iv_schedule_room);
        etRoomid.setVisibility(View.VISIBLE);
        ivScheduleRoom.setVisibility(View.VISIBLE);
        tvExplain = (TextView) findViewById(R.id.tv_explain);
        tvJoinroom = (TextView) findViewById(R.id.tv_joinroom);
        lvFunctions = (ListView) findViewById(R.id.lv_functions);
        lvFunctions.setOnItemClickListener(this);

        fAdapter = new FunctionAdapter(this, functionModels);
        lvFunctions.setAdapter(fAdapter);
        fAdapter.notifyDataSetChanged();

        ivScheduleRoom.setOnClickListener(this);
        tvJoinroom.setOnClickListener(this);
    }


    private void initFunction() {
        functionModels.clear();
        for (int i = 0; i < 8; i++) {//0与4为标题           1:简易数据导入; 2:音视频原始数据导入;3:音视频编码后数据;5:音视频录制成文件;6:音视频解码前导出
            FunctionModel functionModel = new FunctionModel();
            switch (i) {
                case 0:
                    functionModel.setFunctionType(FunctionModel.FunctionType.TITLE);
                    functionModel.setName("音视频导入");
                    functionModel.setDescribe("");
                    break;
                case 1:
                    AVImporterDemoActivity avImporterDemoActivity = new AVImporterDemoActivity();
                    functionModel.setFunctionType(FunctionModel.FunctionType.AV_IMPORT);
                    functionModel.setName(getResources().getString(R.string.simple_import));
                    functionModel.setDescribe(getResources().getString(R.string.simple_import_des));
                    functionModel.setmActivity(avImporterDemoActivity);
                    break;
                case 2:
                    RawDataImportActivity rawDataImportActivity = new RawDataImportActivity();
                    functionModel.setFunctionType(FunctionModel.FunctionType.RAW_DATA_IMPORT);
                    functionModel.setName(getResources().getString(R.string.raw_data_import));
                    functionModel.setDescribe(getResources().getString(R.string.raw_data_import_des));
                    functionModel.setmActivity(rawDataImportActivity);
                    break;
                case 3:
                    EncodedImportActivity encodedImportActivity = new EncodedImportActivity();
                    functionModel.setFunctionType(FunctionModel.FunctionType.ENCODED_IMPORT);
                    functionModel.setName(getResources().getString(R.string.coded_data_import));
                    functionModel.setDescribe(getResources().getString(R.string.coded_data_import_des));
                    functionModel.setmActivity(encodedImportActivity);
                    break;
                case 4:
                    RtspClientImportActivity rtspClientImportActivity = new RtspClientImportActivity();
                    functionModel.setFunctionType(FunctionModel.FunctionType.RTSP_IMPORT);
                    functionModel.setName(getResources().getString(R.string.rtsp_data_import));
                    functionModel.setDescribe(getResources().getString(R.string.rtsp_data_import_des));
                    functionModel.setmActivity(rtspClientImportActivity);
                    break;
                case 5:
                    functionModel.setFunctionType(FunctionModel.FunctionType.TITLE);
                    functionModel.setName("本地音视频录制和导出");
                    functionModel.setDescribe("");
                    break;
                case 6:
                    LocalRecordAndExportActivity localRecordActivity = new LocalRecordAndExportActivity();
                    functionModel.setFunctionType(FunctionModel.FunctionType.RECORD);
                    functionModel.setName(getResources().getString(R.string.local_record));
                    functionModel.setDescribe(getResources().getString(R.string.local_record_des));
                    functionModel.setmActivity(localRecordActivity);
                    break;
                case 7:
                    LocalRecordAndExportActivity ExportActivity = new LocalRecordAndExportActivity();
                    functionModel.setFunctionType(FunctionModel.FunctionType.EXPORT);
                    functionModel.setName(getResources().getString(R.string.export));
                    functionModel.setDescribe(getResources().getString(R.string.export_des));
                    functionModel.setmActivity(ExportActivity);
                    break;
            }
            functionModels.add(functionModel);
        }
        //默认选中第一个功能
        Constants.SELECT_FUNCTION = functionModels.get(1);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        FunctionModel functionModel = functionModels.get(position);
        if (FunctionModel.FunctionType.TITLE != functionModel.getFunctionType()) {//非标题项
            tvExplain.setText(functionModel.getDescribe());
            Constants.SELECT_FUNCTION = functionModel;
            fAdapter.notifyDataSetChanged();
        }
    }

    /**
     * 初始化数据
     * 默认选中第一个功能
     */
    private void initData() {
        tvExplain.setText(functionModels.get(1).getDescribe());
    }
}

