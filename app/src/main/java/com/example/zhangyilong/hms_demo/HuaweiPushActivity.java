package com.example.zhangyilong.hms_demo;

import com.huawei.hms.api.ConnectionResult;
import com.huawei.hms.api.HuaweiApiAvailability;
import com.huawei.hms.api.HuaweiApiClient;
import com.huawei.hms.api.HuaweiApiClient.ConnectionCallbacks;
import com.huawei.hms.api.HuaweiApiClient.OnConnectionFailedListener;
import com.huawei.hms.support.api.client.PendingResult;
import com.huawei.hms.support.api.client.ResultCallback;
import com.huawei.hms.support.api.push.*;
import com.example.zhangyilong.hms_demo.logger.Log;
import com.example.zhangyilong.hms_demo.logger.LoggerActivity;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.TextView;

public class HuaweiPushActivity extends LoggerActivity implements ConnectionCallbacks, OnConnectionFailedListener, OnClickListener{

	public static final String TAG = "HuaweiPushActivity";
	//华为移动服务Client
	private HuaweiApiClient client;
    
	private UpdateUIBroadcastReceiver broadcastReceiver;  
	
    //调用HuaweiApiAvailability.getInstance().resolveError传入的第三个参数
    //作用同startactivityforresult方法中的requestcode
    private static final int REQUEST_HMS_RESOLVE_ERROR = 1000;

	//如果CP在onConnectionFailed调用了resolveError接口，那么错误结果会通过onActivityResult返回
	//具体的返回码通过该字段获取
    public static final String EXTRA_RESULT = "intent.extra.RESULT";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_huaweipush);
        
        findViewById(R.id.push_gettoken_sync).setOnClickListener(this);
        findViewById(R.id.push_gettoken_asyn).setOnClickListener(this);
        findViewById(R.id.push_get_status).setOnClickListener(this);
        findViewById(R.id.push_msg_checkbox).setOnClickListener(this);
        findViewById(R.id.delete_token).setOnClickListener(this);
        //sample apk在界面上显示日志提示信息的窗口，与接口和业务功能无关，请忽略   
        addLogFragment();

        //创建华为移动服务client实例用以使用华为push服务
        //需要指定api为HuaweiId.PUSH_API
        //连接回调以及连接失败监听
        client = new HuaweiApiClient.Builder(this)
        		.addApi(HuaweiPush.PUSH_API)
        		.addConnectionCallbacks(this)
        		.addOnConnectionFailedListener(this)
        		.build();
        
    	//建议在oncreate的时候连接华为移动服务
        //业务可以根据自己业务的形态来确定client的连接和断开的时机，但是确保connect和disconnect必须成对出现
    	client.connect();     		
    	
    	//和业务不相关，请忽略
    	registerBroadcast();
    }  

    @Override
    protected void onStart() {
    	super.onStart();
    }
      
    @Override
    protected void onStop() {
    	super.onStop();
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	//建议在onDestroy的时候停止连接华为移动服务
    	//业务可以根据自己业务的形态来确定client的连接和断开的时机，但是确保connect和disconnect必须成对出现
    	client.disconnect();
    	unregisterReceiver(broadcastReceiver); 
    }
    
	@Override
	public void onConnected() {
		//华为移动服务client连接成功，在这边处理业务自己的事件
		Log.i(TAG, "HuaweiApiClient 连接成功");
	}

	@Override
	public void onConnectionSuspended(int arg0) {
		//HuaweiApiClient断开连接的时候，业务可以处理自己的事件
		Log.i(TAG, "HuaweiApiClient 连接断开");
		client.connect();
	}

	@Override
	public void onConnectionFailed(ConnectionResult arg0) {
		Log.i(TAG, "HuaweiApiClient连接失败，错误码：" + arg0.getErrorCode());
		if(HuaweiApiAvailability.getInstance().isUserResolvableError(arg0.getErrorCode())) {
			HuaweiApiAvailability.getInstance().resolveError(this, arg0.getErrorCode(), REQUEST_HMS_RESOLVE_ERROR);
		} else {
			//其他错误码请参见开发指南或者API文档
		}
	}

	/**
	 * 使用同步接口来获取pushtoken
	 * 结果通过广播的方式发送给应用，不通过标准接口的pendingResul返回
	 * CP可以自行处理获取到token
	 * 同步获取token和异步获取token的方法CP只要根据自身需要选取一种方式即可
	 */
	private void getTokenSync() {
    	if(!client.isConnected()) {
    		Log.i(TAG, "获取token失败，原因：HuaweiApiClient未连接");
    		return;
    	}
    	
    	//需要在子线程中调用函数
    	new Thread() {
    		
    		public void run() {
    			Log.i(TAG, "同步接口获取push token");
    			PendingResult<TokenResult> tokenResult = HuaweiPush.HuaweiPushApi.getToken(client);
    			TokenResult result = tokenResult.await();
    			if(result.getTokenRes().getRetCode() == 0) {
					//当返回值为0的时候表明获取token结果调用成功
					Log.i(TAG, "获取push token 成功，等待广播");
    			}
    		};
    	}.start();
	}
	
	/**
	 * 使用异步接口来获取pushtoken
	 * 结果通过广播的方式发送给应用，不通过标准接口的pendingResul返回
	 * CP可以自行处理获取到token
	 * 同步获取token和异步获取token的方法CP只要根据自身需要选取一种方式即可
	 */
	private void getTokenAsyn() {
    	if(!client.isConnected()) {
    		Log.i(TAG, "获取token失败，原因：HuaweiApiClient未连接");
    		return;
    	}
    	
		Log.i(TAG, "异步接口获取push token");
		PendingResult<TokenResult> tokenResult = HuaweiPush.HuaweiPushApi.getToken(client);
		tokenResult.setResultCallback(new ResultCallback<TokenResult>() {

			@Override
			public void onResult(TokenResult result) {
				//这边的结果只表明接口调用成功，是否能收到响应结果只在广播中接收
			}	
		});
	}
	
	/**
	 * 异步方式获取PUSH的连接状态
	 * 结果会通过通知发送出来
	 */
	private void getPushStatus() {
    	if(!client.isConnected()) {
    		Log.i(TAG, "获取PUSH连接状态失败，原因：HuaweiApiClient未连接");
    		return;
    	}
    	
    	//需要在子线程中调用函数
    	new Thread() {
    		public void run() {
    			Log.i(TAG, "开始获取PUSH连接状态");
    	    	HuaweiPush.HuaweiPushApi.getPushState(client);
    	    	// 状态结果通过广播返回
    		};
    	}.start();
	}
   
	/**
	 * 设置是否允许应用接收PUSH透传消息
	 * 若不调用该方法则默认为开启
	 * 在开发者网站上发送push消息分为通知和透传消息
	 * 通知为直接在通知栏收到通知，通过点击可以打开网页，应用 或者富媒体，不会收到onPushMsg消息
	 * 透传消息不会展示在通知栏，应用会收到onPushMsg
	 * 此开关只对透传消息有效
	 * @param flag true 允许  false 不允许
	 */
    private void setReceiveNormalMsg(boolean flag) {
    	if(!client.isConnected()) {
    		Log.i(TAG, "设置是否接收push消息失败，原因：HuaweiApiClient未连接");
    		return;
    	}
    	if(flag == true) {
        	Log.i(TAG, "允许应用接收push消息");
    	} else {
    		Log.i(TAG, "禁止应用接收push消息");
    	}
    	HuaweiPush.HuaweiPushApi.enableReceiveNormalMsg(client, flag);
    }
    
    /**
     * 应用删除通过getToken接口获取到的token
     * 应用调用注销token接口成功后，客户端就不会再接收到PUSH消息
     * CP应该在调用该方法后，自行处理本地保存的通过gettoken接口获取到的TOKEN
     */
    private void deleteToken() {
    	
    	if(!client.isConnected()) {
    		Log.i(TAG, "注销token失败，原因：HuaweiApiClient未连接");
    		return;
    	}
    	
    	//需要在子线程中执行删除token操作
    	new Thread() {
    		@Override
    		public void run() {
    			//调用删除token需要传入通过getToken接口获取到token，并且需要对token进行非空判断
    			String token = ((TextView)findViewById(R.id.push_token_view)).getText().toString();
    			Log.i(TAG, "删除Token：" + token);
    			if (!TextUtils.isEmpty(token)){
    				try {
        				HuaweiPush.HuaweiPushApi.deleteToken(client, token);
					} catch (PushException e) {
						Log.i(TAG, "删除Token失败:" + e.getMessage());
					}
    			}
    			
    		}
    	}.start();
    }
    
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.push_gettoken_asyn:
			getTokenAsyn();
			break;
			
		case R.id.push_gettoken_sync:
			getTokenSync();		
			break;
			
		case R.id.push_get_status:
			getPushStatus();
			break;
			
		case R.id.push_msg_checkbox:
			Boolean flag = ((CheckBox)findViewById(R.id.push_msg_checkbox)).isChecked();
			setReceiveNormalMsg(flag);
			break;
			
		case R.id.delete_token:
			deleteToken();
			break;
			
		default:
			break;
		}
	}

	/**
	 * 当调用HuaweiApiAvailability.getInstance().resolveError方法的时候，会通过onActivityResult
	 * 将实际处理结果返回给CP。
	 */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == REQUEST_HMS_RESOLVE_ERROR) {
        	if(resultCode == Activity.RESULT_OK) {
        		
        		int result = data.getIntExtra(EXTRA_RESULT, 0);
        		
        		if(result == ConnectionResult.SUCCESS) {
        			Log.i(TAG, "错误成功解决");
    			    if (!client.isConnecting() && !client.isConnected()) {
    				 client.connect();
    			    }
        		} else if(result == ConnectionResult.CANCELED) {
        			Log.i(TAG, "解决错误过程被用户取消");
        		} else if(result == ConnectionResult.INTERNAL_ERROR) {
        			Log.i(TAG, "发生内部错误，重试可以解决");
        			//CP可以在此处重试连接华为移动服务等操作，导致失败的原因可能是网络原因等
        		} else {
        			Log.i(TAG, "未知返回码");
        		}
        	} else {
        		Log.i(TAG, "调用解决方案发生错误");
        	}
        }
    }
    
    /**
     * sample apk在界面上显示日志提示信息的窗口，与接口和业务功能无关，请忽略
     */
    private void addLogFragment() {
        final FragmentTransaction transaction = getFragmentManager().beginTransaction();
        final LogFragment fragment = new LogFragment();
        transaction.replace(R.id.framelog, fragment);
        transaction.commit();
    }
    
    /**
     * 以下代码为sample自身逻辑，和业务能力不相关
     * 作用仅仅为了在sample界面上显示push相关信息
     */
    private void registerBroadcast() {
    	String ACTION_UPDATEUI = "action.updateUI"; 

	    IntentFilter filter = new IntentFilter();  
	    filter.addAction(ACTION_UPDATEUI);  
	    broadcastReceiver = new UpdateUIBroadcastReceiver();  
	    registerReceiver(broadcastReceiver, filter);
    }
    
    /** 
     * 定义广播接收器（内部类） 
     */  
    private class UpdateUIBroadcastReceiver extends BroadcastReceiver {  

		@Override
		public void onReceive(Context context, Intent intent) {
			int type = intent.getExtras().getInt("type"); 
			if(type == 1) {
				String token = intent.getExtras().getString("token"); 
				((TextView)findViewById(R.id.push_token_view)).setText(token);
			} else if (type == 2) {
				boolean status = intent.getExtras().getBoolean("pushState"); 
				if(status == true) {
					((TextView)findViewById(R.id.push_status_view)).setText("已连接");
				} else {
					((TextView)findViewById(R.id.push_status_view)).setText("未连接");
				}
			}
		}  

    }   
}
