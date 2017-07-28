package cn.tee3.avei.main;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.UUID;

import cn.tee3.avd.AVDEngine;
import cn.tee3.avd.ErrorCode;
import cn.tee3.avd.RoomInfo;
import cn.tee3.avei.AveiApp;
import cn.tee3.avei.Constants;
import cn.tee3.avei.R;
import cn.tee3.avei.avimport.AVImporterDemoActivity;
import cn.tee3.avei.avimport.AudioMixExport;
import cn.tee3.avei.avimport.AudioMixExport1;
import cn.tee3.avei.avimport.EncodedImportActivity;
import cn.tee3.avei.avimport.RawDataImportActivity;
import cn.tee3.avei.avimport.RtspClientImportActivity;
import cn.tee3.avei.export.LocalRecordAndExportActivity;
import cn.tee3.avei.model.DemoOption;
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

    private String apptype = "";

    private Handler mHandler = new Handler(new Handler.Callback() {
        public boolean handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case 100:
                    /********引擎初始化，可以在AveiApp中直接写死相应的参数进行引擎初始化*******/
                    int ret = ((AveiApp) getApplication()).AVDEngineInit(Constants.DEMO_PARAMS.getServer_uri(), Constants.DEMO_PARAMS.getAccess_key(), Constants.DEMO_PARAMS.getSecret_key());
                    if (ErrorCode.AVD_OK != ret) {
                        Toast.makeText(MainActivity.this, "引擎初始化失败", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "onCreate, init AVDEngine failed. ret=" + ret);
                    } else {
                        Constants.APP_TYPE = apptype;
                    }
                    break;
                default:
                    break;
            }
            return true;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);
        initFunction();
        initView();
        initData();
        //获取叁体服务信息，并进行初始化
        //可以在AveiApp中直接写死相应的参数进行引擎初始化
        new Thread(networkTask).start();
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
//                Intent intent = new Intent(this, AudioMixExport1.class);
//                startActivity(intent);
                break;
            case R.id.iv_schedule_room:
                //安排房间,此处有两种形式，一种是通过UUID进行创建，房间号有系统分配，还有一种手动输入新的房间号，两种形式可在服务端配置
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

    Runnable networkTask = new Runnable() {
        @Override
        public void run() {
            avdGetParams();
        }
    };

    /**
     * 获取叁体服务信息，并进行引擎初始化
     *
     * @return
     */
    public String avdGetParams() {
//get的方式提交就是url拼接的方式
        apptype = Constants.SELECT_FUNCTION.getAppType();
        if (StringUtils.isEmpty(Constants.SELECT_FUNCTION.getAppType())) {
            apptype = "def";
        }
        String path = "http://demo.3tee.cn/demo/avd_get_params?apptype=" + apptype;
        try {
            URL url = new URL(path);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setRequestMethod("GET");
            //获得结果码
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                Log.i(TAG, "avdGetParams");
                //请求成功 获得返回的流
                InputStream is = connection.getInputStream();

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[1024];
                int len;
                while ((len = is.read(buf)) != -1) {
                    out.write(buf, 0, len);
                }
                String string = out.toString("UTF-8");
                JSONObject dataJson = new JSONObject(string);
                Constants.DEMO_PARAMS.setRet(Integer.parseInt(dataJson.getString("ret")));
                Constants.DEMO_PARAMS.setServer_uri(dataJson.getString("server_uri"));
                Constants.DEMO_PARAMS.setAccess_key(dataJson.getString("access_key"));
                Constants.DEMO_PARAMS.setSecret_key(dataJson.getString("secret_key"));
                String optionStr = dataJson.getString("option");
                if (StringUtils.isNotEmpty(optionStr) && !optionStr.equals("null")) {
                    String option = dataJson.getString("option");
                    JSONObject optionJson = new JSONObject(option);
                    DemoOption demoOption = new DemoOption();
                    demoOption.setUserAddress(optionJson.getString("userAddress"));
                    demoOption.setLogin_name(optionJson.getString("login_name"));
                    demoOption.setLogin_password(optionJson.getString("login_password"));
                    Constants.DEMO_PARAMS.setOption(demoOption);
                }
                out.close();
                is.close();
                /********引擎初始化，可以在AveiApp中直接写死相应的参数进行引擎初始化*******/
                mHandler.sendEmptyMessageDelayed(100, 50);
                return responseCode + "";
            } else {
                //请求失败
                Looper.prepare();
                Toast.makeText(MainActivity.this, "获取服务器信息失败", Toast.LENGTH_SHORT).show();
                Looper.loop();
                Log.e(TAG, "avdGetParams, failed. ret=" + responseCode);
                return null;
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            Looper.prepare();
            Toast.makeText(MainActivity.this, "网络请求失败，请检查网络", Toast.LENGTH_SHORT).show();
            Looper.loop();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
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
                    functionModel.setAppType("def");
                    break;
                case 2:
                    RawDataImportActivity rawDataImportActivity = new RawDataImportActivity();
                    functionModel.setFunctionType(FunctionModel.FunctionType.RAW_DATA_IMPORT);
                    functionModel.setName(getResources().getString(R.string.raw_data_import));
                    functionModel.setDescribe(getResources().getString(R.string.raw_data_import_des));
                    functionModel.setmActivity(rawDataImportActivity);
                    functionModel.setAppType("def");
                    break;
                case 3:
                    EncodedImportActivity encodedImportActivity = new EncodedImportActivity();
                    functionModel.setFunctionType(FunctionModel.FunctionType.ENCODED_IMPORT);
                    functionModel.setName(getResources().getString(R.string.coded_data_import));
                    functionModel.setDescribe(getResources().getString(R.string.coded_data_import_des));
                    functionModel.setmActivity(encodedImportActivity);
                    functionModel.setAppType("def");
                    break;
                case 4:
                    RtspClientImportActivity rtspClientImportActivity = new RtspClientImportActivity();
                    functionModel.setFunctionType(FunctionModel.FunctionType.RTSP_IMPORT);
                    functionModel.setName(getResources().getString(R.string.rtsp_data_import));
                    functionModel.setDescribe(getResources().getString(R.string.rtsp_data_import_des));
                    functionModel.setmActivity(rtspClientImportActivity);
                    functionModel.setAppType("rtsp");
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
                    functionModel.setAppType("def");
                    break;
                case 7:
                    LocalRecordAndExportActivity ExportActivity = new LocalRecordAndExportActivity();
                    functionModel.setFunctionType(FunctionModel.FunctionType.EXPORT);
                    functionModel.setName(getResources().getString(R.string.export));
                    functionModel.setDescribe(getResources().getString(R.string.export_des));
                    functionModel.setmActivity(ExportActivity);
                    functionModel.setAppType("def");
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
            if (!Constants.APP_TYPE.equals(Constants.SELECT_FUNCTION.getAppType())) {//如果不相同，重新初始化引擎
                new Thread(networkTask).start();
            }
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

