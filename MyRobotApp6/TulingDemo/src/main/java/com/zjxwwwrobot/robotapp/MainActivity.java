package com.zjxwwwrobot.robotapp;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.baidu.aip.asrwakeup3.core.mini.AutoCheck;
import com.baidu.speech.EventManager;
import com.baidu.speech.EventManagerFactory;
import com.baidu.speech.asr.SpeechConstant;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EventListener;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends Activity implements HttpGetDataListener,
		OnClickListener, EventListener, com.baidu.speech.EventListener {

	private HttpData httpData;
	private List<ListData> lists;
	private ListView lv;
	private EditText sendtext;
	private Button send_btn;
	private String content_str;
	private TextAdapter adapter;
	private String[] welcome_array;
	private double currentTime=0, oldTime = 0;
	private String myresult;// 语音识别最终答案


	////////////////////////////////////////////////
//	百度语音
	protected Button yuyin_begin;
	protected Button yuyin_stop;

	protected TextView txtLog;  //txt 文本提醒信息
	protected TextView txtResult;


	private EventManager asr;

	private boolean logTime = true;

	protected boolean enableOffline = false; // 测试离线命令词，需要改成true

//////////////////////////////////////////////////////////
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initPermission();  //请求权限
		initView();   //打开app 先初始化
//////////////////////////////////////////////////
		txtLog = findViewById(R.id.textView);
		yuyin_begin = findViewById(R.id.yuyin_begin);
		yuyin_stop = findViewById(R.id.yuyin_stop);
		// 基于sdk集成1.1 初始化EventManager对象
		asr = EventManagerFactory.create(this, "asr");
		// 基于sdk集成1.3 注册自己的输出事件类
		asr.registerListener(this); //  EventListener 中 onEvent方法



		yuyin_begin.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				start();
			}
		});
		yuyin_stop.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				stop();
			}
		});
		if (enableOffline) {
			loadOfflineEngine(); // 测试离线命令词请开启, 测试 ASR_OFFLINE_ENGINE_GRAMMER_FILE_PATH 参数时开启
		}
	}

/////////////////////////////////////////////////
	/**
	 * 基于SDK集成2.2 发送开始事件
	 * 点击开始按钮
	 * 测试参数填在这里
	 */
	private void start() {
//        txtLog.setText("");
		Map<String, Object> params = new LinkedHashMap<String, Object>();
		String event = null;
		event = SpeechConstant.ASR_START; // 替换成测试的event

		if (enableOffline) {
			params.put(SpeechConstant.DECODER, 2);
		}
		// 基于SDK集成2.1 设置识别参数
		params.put(SpeechConstant.ACCEPT_AUDIO_VOLUME, false);
		// params.put(SpeechConstant.NLU, "enable");
		// params.put(SpeechConstant.VAD_ENDPOINT_TIMEOUT, 0); // 长语音

		// params.put(SpeechConstant.IN_FILE, "res:///com/baidu/android/voicedemo/16k_test.pcm");
		// params.put(SpeechConstant.VAD, SpeechConstant.VAD_DNN);
		// params.put(SpeechConstant.PID, 1537); // 中文输入法模型，有逗号

		/* 语音自训练平台特有参数 */
		// params.put(SpeechConstant.PID, 8002);
		// 语音自训练平台特殊pid，8002：模型类似开放平台 1537  具体是8001还是8002，看自训练平台页面上的显示
		// params.put(SpeechConstant.LMID,1068); // 语音自训练平台已上线的模型ID，https://ai.baidu.com/smartasr/model
		// 注意模型ID必须在你的appId所在的百度账号下
		/* 语音自训练平台特有参数 */

		/* 测试InputStream*/
		// InFileStream.setContext(this);
		// params.put(SpeechConstant.IN_FILE, "#com.baidu.aip.asrwakeup3.core.inputstream.InFileStream.createMyPipedInputStream()");

		// 请先使用如‘在线识别’界面测试和生成识别参数。 params同ActivityRecog类中myRecognizer.start(params);
		// 复制此段可以自动检测错误
		(new AutoCheck(getApplicationContext(), new Handler() {
			public void handleMessage(Message msg) {
				if (msg.what == 100) {
					AutoCheck autoCheck = (AutoCheck) msg.obj;
					synchronized (autoCheck) {
						String message = autoCheck.obtainErrorMessage(); // autoCheck.obtainAllMessage();
						txtLog.append(message + "\n");
						; // 可以用下面一行替代，在logcat中查看代码
						// Log.w("AutoCheckMessage", message);
					}
				}
			}
		}, enableOffline)).checkAsr(params);
		String json = null; // 可以替换成自己的json
		json = new JSONObject(params).toString(); // 这里可以替换成你需要测试的json
		asr.send(event, json, null, 0, 0);
		printLog("输入参数：" + json);
	}

	/**
	 * 点击停止按钮
	 * 基于SDK集成4.1 发送停止事件
	 */
	private void stop() {
		printLog("停止识别：ASR_STOP");
		asr.send(SpeechConstant.ASR_STOP, null, null, 0, 0);
		///////////////////
		// 以下  实现  语音识别文本自动发送
		///////////////////
		content_str = myresult; //app中输入的问题
//		sendtext.setText("");
		String dropk = content_str.replace(" ", ""); //把问题中的 空格 全部替换删除
		String droph = dropk.replace("\n", "");      //把问题中的 回车 全部替换删除
		String drol = 	droph.replace("，","");		//把 "，"去掉
		String droq = 	drol.replace("。","");		//把 "。"去掉
		ListData listData;
		listData = new ListData(content_str, ListData.SEND, getTime());  //将问题输入的 content 封装到 listdata中
		lists.add(listData);
		if (lists.size() > 30) {
			for (int i = 0; i < lists.size(); i++) {
				lists.remove(i);
			}
		}
		adapter.notifyDataSetChanged();
//		请求服务器数据的端口：
//	    阿里云服务器端口	  http://120.26.174.161:8080/chat
		httpData = (HttpData) new HttpData(    //函数名 httpdata 对应到 httpdata类中的一个函数
				"http://120.26.174.161:8080/chat"+"?msg="
						+ droq, this).execute();   //droph 是 内容 最终处理后的版本
	}


	/**
	 * enableOffline设为true时，在onCreate中调用
	 * 基于SDK离线命令词1.4 加载离线资源(离线时使用)
	 */
	private void loadOfflineEngine() {
		Map<String, Object> params = new LinkedHashMap<String, Object>();
		params.put(SpeechConstant.DECODER, 2);
		params.put(SpeechConstant.ASR_OFFLINE_ENGINE_GRAMMER_FILE_PATH, "assets://baidu_speech_grammar.bsg");
		asr.send(SpeechConstant.ASR_KWS_LOAD_ENGINE, new JSONObject(params).toString(), null, 0, 0);
	}

	/**
	 * enableOffline为true时，在onDestory中调用，与loadOfflineEngine对应
	 * 基于SDK集成5.1 卸载离线资源步骤(离线时使用)
	 */
	private void unloadOfflineEngine() {
		asr.send(SpeechConstant.ASR_KWS_UNLOAD_ENGINE, null, null, 0, 0); //
	}



	@Override
	protected void onPause() {
		super.onPause();
		asr.send(SpeechConstant.ASR_CANCEL, "{}", null, 0, 0);
		Log.i("ActivityMiniRecog", "On pause");
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		// 基于SDK集成4.2 发送取消事件
		asr.send(SpeechConstant.ASR_CANCEL, "{}", null, 0, 0);
		if (enableOffline) {
			unloadOfflineEngine(); // 测试离线命令词请开启, 测试 ASR_OFFLINE_ENGINE_GRAMMER_FILE_PATH 参数时开启
		}

		// 基于SDK集成5.2 退出事件管理器
		// 必须与registerListener成对出现，否则可能造成内存泄露
		asr.unregisterListener((com.baidu.speech.EventListener) this);
	}

	private void maketext(String str){
		Toast.makeText(MainActivity.this,str,Toast.LENGTH_LONG).show();
	}


	// 基于sdk集成1.2 自定义输出事件类 EventListener 回调方法
	// 基于SDK集成3.1 开始回调事件
	public void onEvent(String name, String params, byte[] data, int offset, int length) {
//        String logTxt = "name: " + name;
		maketext("name"+name);
		if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_PARTIAL)) {
			// 识别相关的结果都在这里
			if (params == null || params.isEmpty()) {
				return;
			}
			if (params.contains("\"nlu_result\"")) {
				// 一句话的语义解析结果
				if (length > 0 && data.length > 0) {
//                    logTxt += ", 语义解析结果：" + new String(data, offset, length);
				}
			} else if (params.contains("\"partial_result\"")) {
				// 一句话的临时识别结果
//                logTxt += ", 临时识别结果：" + params;
			}  else if (params.contains("\"final_result\""))  {
				// 一句话的最终识别结果
//                logTxt += ", 最终识别结果：" + params;

////////////////////////////////////////////////////////
// 解析
///////////////////////////////////////////////////////
				try {
					JSONObject jsonObject = new JSONObject(params);
					String backtext =  jsonObject.optString("results_recognition");
					String a = backtext.replace("[","").replace("]","");
					String bast = a.replaceAll("\"","");
					txtLog.setText(bast);  // textview  显示最终答案为  bast
					myresult = bast;
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}  else {
				// 一般这里不会运行
//                logTxt += " ;params :" + params;
				if (data != null) {
//                    logTxt += " ;data length=" + data.length;
				}
			}
		} else {
			// 识别开始，结束，音量，音频数据回调
			if (params != null && !params.isEmpty()){
//                logTxt += " ;params :" + params;
			}
			if (data != null) {
//                logTxt += " ;data length=" + data.length;
			}
		}


//        printLog(logTxt);
	}

	private void printLog(String text) {
		if (logTime) {
			text += "  ;time=" + System.currentTimeMillis();
		}
		text += "\n";
		Log.i(getClass().getName(), text);
		txtLog.append(text + "\n");
	}
///////////////////////////////////////////////////
//语音功能为以上
//////////////////////////////////////////////////

	private void initView() {    //初始化 app的时候
		lv = (ListView) findViewById(R.id.lv);  //对话栏
		sendtext = (EditText) findViewById(R.id.sendText);  //内容
		send_btn = (Button) findViewById(R.id.send_btn);    //发送按钮
		lists = new ArrayList<ListData>();
		send_btn.setOnClickListener(this);
		adapter = new TextAdapter(lists, this);
		lv.setAdapter(adapter);
		ListData listData;
		listData = new ListData(getRandomWelcomeTips(), ListData.RECEIVER,   //初始化时，将 随机欢迎语 封装到listdata 中
				getTime());
		lists.add(listData);

	}
//生成随机问候语
	private String getRandomWelcomeTips() {
		String welcome_tip = null;
		welcome_array = this.getResources()
				.getStringArray(R.array.welcome_tips);
		int index = (int) (Math.random() * (welcome_array.length - 1));
		welcome_tip = welcome_array[index];
		return welcome_tip;
	}

	@Override
	public void getDataUrl(String data) {  //监听到服务器发送来的信息
		//System.out.println(data);
		parseText(data);   //装进parsetext 函数中
	}

	public void parseText(String str) {   //对服务器接受到的数据进行处理，通常是json数据，需要将内容提取出来
		try {
			JSONObject jb = new JSONObject(str);
			//System.out.println("输出");
			//System.out.println(jb.getString("code"));
			//System.out.println(jb.getString("text"));
			ListData listData;
			listData = new ListData(jb.getString("text"), ListData.RECEIVER,  //将  服务器接受到的内容 放进 list data 中
					getTime());
			lists.add(listData);
			adapter.notifyDataSetChanged();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	////////////////////////////
//	发送文本
	////////////////////////////

	@Override
	public void onClick(View v) {
		getTime();
		content_str = sendtext.getText().toString(); //app中输入的问题
		sendtext.setText("");
		String dropk = content_str.replace(" ", ""); //把问题中的 空格 全部替换删除
		String droph = dropk.replace("\n", "");      //把问题中的 回车 全部替换删除
		ListData listData;
		listData = new ListData(content_str, ListData.SEND, getTime());  //将问题输入的 content 封装到 listdata中
		lists.add(listData);
		if (lists.size() > 30) {
			for (int i = 0; i < lists.size(); i++) {
				lists.remove(i);
			}
		}
		adapter.notifyDataSetChanged();
//		请求服务器数据的端口：
//	    阿里云服务器端口	  http://120.26.174.161:8080/chat
		httpData = (HttpData) new HttpData(    //函数名 httpdata 对应到 httpdata类中的一个函数
				"http://120.26.174.161:8080/chat"+"?msg="
						+ droph, this).execute();   //droph 是 内容 最终处理后的版本
	}

	private String getTime() {
		currentTime = System.currentTimeMillis();
		SimpleDateFormat format = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss");
		Date curDate = new Date();
		String str = format.format(curDate);
		if (currentTime - oldTime >= 500) {
			oldTime = currentTime;
			return str;
		} else {
			return "";
		}
	}




	/**
	 * android 6.0 以上需要动态申请权限
	 */
	private void initPermission() {
		String permissions[] = {Manifest.permission.RECORD_AUDIO,
				Manifest.permission.ACCESS_NETWORK_STATE,
				Manifest.permission.INTERNET,
				Manifest.permission.WRITE_EXTERNAL_STORAGE
		};

		ArrayList<String> toApplyList = new ArrayList<String>();

		for (String perm : permissions) {
			if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, perm)) {
				toApplyList.add(perm);
				// 进入到这里代表没有权限.

			}
		}
		String tmpList[] = new String[toApplyList.size()];
		if (!toApplyList.isEmpty()) {
			ActivityCompat.requestPermissions(this, toApplyList.toArray(tmpList), 123);
		}

	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		// 此处为android 6.0以上动态授权的回调，用户自行实现。
	}

	@Override
	public void onPointerCaptureChanged(boolean hasCapture) {

	}
}
