package cn.tee3.avei.main;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import cn.tee3.avei.Constants;
import cn.tee3.avei.R;
import cn.tee3.avei.avimport.EncodedImportActivity;
import cn.tee3.avei.avimport.RawDataImportActivity;
import cn.tee3.avei.avimport.AVImporterDemoActivity;
import cn.tee3.avei.avimport.RtspClientImportActivity;
import cn.tee3.avei.export.LocalRecordAndExportActivity;
import cn.tee3.avei.model.FunctionModel;
import cn.tee3.avei.utils.StringUtils;

/**
 * 主界面（起始页）
 * <p>
 * Created by shengf on 2017/6/3.
 */

public class MainActivity extends Activity implements View.OnClickListener, AdapterView.OnItemClickListener {
    private static final String TAG = "MainActivity";

    private ListView lvFunctions;//功能列表
    private ArrayList<FunctionModel> functionModels = new ArrayList<>();
    private FunctionAdapter fAdapter;
    private TextView tvExplain;//模块说明
    private TextView tvJoinroom;//加入房间
    private EditText etRoomid;//房间Id
    private int functionType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);
        initFunction();
        initView();
        initData();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_joinroom:
                String roomId = etRoomid.getText().toString().trim();
                if (StringUtils.isEmpty(roomId)) {
                    roomId = "r3355";
                }
                switch (functionType) {
                    case FunctionModel.RECORD://录制和导出
                        Intent intentRecord = new Intent(this, LocalRecordAndExportActivity.class);
                        intentRecord.putExtra("roomId", roomId);
                        startActivity(intentRecord);
                        break;
                    case FunctionModel.EXPORT://录制和导出
                        Intent intentExport = new Intent(this, LocalRecordAndExportActivity.class);
                        intentExport.putExtra("roomId", roomId);
                        startActivity(intentExport);
                        break;
                    case FunctionModel.AV_IMPORT://简单导入
                        Intent intentSimpleImport = new Intent(this, AVImporterDemoActivity.class);
                        intentSimpleImport.putExtra("roomId", roomId);
                        startActivity(intentSimpleImport);
                        break;
                    case FunctionModel.RAW_DATA_IMPORT://原始数据导入
                        Intent intentRawDataImport = new Intent(this, RawDataImportActivity.class);
                        intentRawDataImport.putExtra("roomId", roomId);
                        startActivity(intentRawDataImport);
                        break;
                    case FunctionModel.ENCODED_IMPORT://编码数据导入
                        Intent intentCodedDataImport = new Intent(this, EncodedImportActivity.class);
                        intentCodedDataImport.putExtra("roomId", roomId);
                        startActivity(intentCodedDataImport);
                        break;
                    case FunctionModel.RTSP_IMPORT://RTSP导入
                        Intent intentRTSPImport = new Intent(this, RtspClientImportActivity.class);
                        intentRTSPImport.putExtra("roomId", roomId);
                        startActivity(intentRTSPImport);
                        break;
                    default:
                        break;
                }
                break;
        }
    }

    public void initView() {
        etRoomid = (EditText) findViewById(R.id.et_roomid);
        etRoomid.setVisibility(View.VISIBLE);

        tvExplain = (TextView) findViewById(R.id.tv_explain);
        tvJoinroom = (TextView) findViewById(R.id.tv_joinroom);
        lvFunctions = (ListView) findViewById(R.id.lv_functions);
        lvFunctions.setOnItemClickListener(this);

        fAdapter = new FunctionAdapter(this, functionModels);
        lvFunctions.setAdapter(fAdapter);
        fAdapter.notifyDataSetChanged();

        tvJoinroom.setOnClickListener(this);
    }


    private void initFunction() {
        functionModels.clear();
        for (int i = 0; i < 8; i++) {//0与4为标题           1:简易数据导入; 2:音视频原始数据导入;3:音视频编码后数据;5:音视频录制成文件;6:音视频解码前导出
            FunctionModel functionModel = new FunctionModel();
            switch (i) {
                case 0:
                    functionModel.setType(FunctionModel.TITLE);
                    functionModel.setName("音视频导入");
                    functionModel.setDescribe("");
                    break;
                case 1:
                    functionModel.setType(FunctionModel.AV_IMPORT);
                    functionModel.setName(getResources().getString(R.string.simple_import));
                    functionModel.setDescribe(getResources().getString(R.string.simple_import_des));
                    break;
                case 2:
                    functionModel.setType(FunctionModel.RAW_DATA_IMPORT);
                    functionModel.setName(getResources().getString(R.string.raw_data_import));
                    functionModel.setDescribe(getResources().getString(R.string.raw_data_import_des));
                    break;
                case 3:
                    functionModel.setType(FunctionModel.ENCODED_IMPORT);
                    functionModel.setName(getResources().getString(R.string.coded_data_import));
                    functionModel.setDescribe(getResources().getString(R.string.coded_data_import_des));
                    break;
                case 4:
                    functionModel.setType(FunctionModel.RTSP_IMPORT);
                    functionModel.setName(getResources().getString(R.string.rtsp_data_import));
                    functionModel.setDescribe(getResources().getString(R.string.rtsp_data_import_des));
                    break;
                case 5:
                    functionModel.setType(FunctionModel.TITLE);
                    functionModel.setName("本地音视频录制和导出");
                    functionModel.setDescribe("");
                    break;
                case 6:
                    functionModel.setType(FunctionModel.RECORD);
                    functionModel.setName(getResources().getString(R.string.local_record));
                    functionModel.setDescribe(getResources().getString(R.string.local_record_des));
                    break;
                case 7:
                    functionModel.setType(FunctionModel.EXPORT);
                    functionModel.setName(getResources().getString(R.string.export));
                    functionModel.setDescribe(getResources().getString(R.string.export_des));
                    break;
            }
            functionModels.add(functionModel);
        }
        //默认选中第一个功能
        Constants.SELECT_FUNCTION_NAME = functionModels.get(1).getName();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        FunctionModel functionModel = functionModels.get(position);
        if (FunctionModel.TITLE != functionModel.getType()) {//非标题项
            tvExplain.setText(functionModel.getDescribe());
            functionType = functionModel.getType();
            Constants.SELECT_FUNCTION_NAME = functionModel.getName();
            fAdapter.notifyDataSetChanged();
        }
    }

    /**
     * 初始化数据
     * 默认选中第一个功能
     */
    private void initData() {
        functionType = functionModels.get(1).getType();
        tvExplain.setText(functionModels.get(1).getDescribe());
    }
}

