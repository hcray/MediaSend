package com.kraken.mediasend;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Random;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.kraken.mediasend.gallery.ImageSwitcherActivity;

public class SocketService extends Service
{
	private static final String TAG = "SocketService" ;  
	public static final String SOCKETSERVICE_REV_ACTION_CONN = "com.kraken.mediasend.SocketService.SOCKETSERVICE_REV_ACTION_CONN";
	public static final String SOCKETSERVICE_REV_ACTION_SEND = "com.kraken.mediasend.SocketService.SOCKETSERVICE_REV_ACTION_SEND"; 

	private Socket socket;
	private byte[] head = {123,33,123,33};
	private byte[] tail = {33,125,33,125};
	
	private boolean isconnected = false;
	private Handler mChildHandler;
	private SocketThread socketThread=null;
	
	
	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent) 
		{
			String action = intent.getAction();
			if(action.equals(SocketService.SOCKETSERVICE_REV_ACTION_CONN))
			{
				Log.v(TAG, "SOCKETSERVICE_REV_ACTION_CONN");  
				Bundle bundle = intent.getExtras();  		
				String ip = bundle.getString("edtServerIpValue");
				int port = bundle.getInt("edtServerPortValue");
				if(socketThread!=null)
				{
					isconnected = false;
					try
					{
						socket.close();
					}
					catch (IOException e)
					{
						// TODO Auto-generated catch block
						//e.printStackTrace();
					}
					socketThread.interrupt();
					//socketThread.stop();
				}
				//启动socket线程 连接并发送连接请求
				socketThread = new SocketThread(ip,port);
				socketThread.start();
            }
			if(action.equals(SocketService.SOCKETSERVICE_REV_ACTION_SEND))
			{
				Log.v(TAG, "SOCKETSERVICE_REV_ACTION_SEND"); 
				Bundle bundle = intent.getExtras();  		
				Message msg = mChildHandler.obtainMessage();
				msg.setData(bundle);
				mChildHandler.sendMessage(msg);
			}
		} 
    }; 
    
	public SocketService()
	{
		Log.d(TAG, "SocketService");  
	}

	private class SocketThread extends Thread
	{
		private String ip;
		private int port;
		public SocketThread(String _ip,int _port)
		{
			ip=_ip;
			port=_port;
		}
		
		@Override
		public void run()
		{
			try
			{
				Log.d(TAG, "new InetSocketAddress(ip, port);"); 
				SocketAddress socketAddress = new InetSocketAddress(ip, port);
				int timeout=8000; 
				socket = new Socket();
				socket.connect(socketAddress,timeout); 
				isconnected = true;
				Log.d(TAG, "isconnected");  
				sendConnectInfo();
				//初始化消息循环队列，需要在Handler创建之前
				Looper.prepare();
				mChildHandler = new Handler()
				{
					@Override
					public void handleMessage(Message msg)
					{
						Bundle bundle = msg.getData();
						String mediapath = bundle.getString("mediapath");
						sendDate(mediapath);
					}
				};
				//启动子线程消息循环队列
	            Looper.loop();
			} 
			catch (UnknownHostException e)
			{
				//e.printStackTrace();	
				Log.v(TAG, "isconnected error UnknownHostException");  
				Intent intnt = new Intent(CaptureActivity.CAPTUREACTIVITY_REV_ACTION_RESULT);  
				intnt.putExtra("result", false);
				intnt.putExtra("Message", "UnknownHostException");
				SocketService.this.sendBroadcast(intnt);
			} 
			catch (IOException e)
			{
				//e.printStackTrace();
				Log.v(TAG, "isconnected error IOException "+e.getMessage());  
				Intent intnt = new Intent(CaptureActivity.CAPTUREACTIVITY_REV_ACTION_RESULT);  
				intnt.putExtra("result", false);
				intnt.putExtra("Message", "IOException");
				SocketService.this.sendBroadcast(intnt);
			}
		}
		@Override
		public void destroy()
		{
			Log.d(TAG, "thread destroy");  
		
			super.destroy();
		}
	}
	//发送数据  成功返回true or false
	public void sendDate(String mediapath)
	{
		Intent intnt = new Intent(ImageSwitcherActivity.MEDIAACTIVITY_REV_ACTION_RESULT);  
		if(isconnected==false||socket.isConnected()==false) 
		{
			intnt.putExtra("result", false);
			SocketService.this.sendBroadcast(intnt);
			return;
		}
		try
		{
			DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
			DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
			// 读取文件输入流
			Log.d(TAG, "curPath:" + mediapath);
			FileInputStream reader = new FileInputStream(mediapath);
			dataOutputStream.write(head);//数据头
			dataOutputStream.writeInt(1);//meiti
			dataOutputStream.writeInt(0);//image
			// 取得文件名
			String[] curStrings = mediapath.split("/");
			String fileName = curStrings[curStrings.length - 1];
			Log.d(TAG, "fileName: " + fileName);
			byte[] fileNameBytes = fileName.getBytes();
			Log.d(TAG, "fileNameBytes.length: " + fileNameBytes.length);
			// 文件名的长度
			dataOutputStream.writeInt(fileNameBytes.length);
			dataOutputStream.write(fileNameBytes);
			int bufferSize = 1024; // 1K
			byte[] buf = new byte[bufferSize];
			int read = 0;
			// 将文件输入流 循环 读入 Socket的输出流中
			while ((read = reader.read(buf, 0, buf.length)) != -1)
			{
				dataOutputStream.write(buf, 0, read);
			}
			dataOutputStream.write(tail);//数据头
			Log.d(TAG, "socket执行完成");
			dataOutputStream.flush();
			reader.close();
			// 获取服务器端的相应返回
			int status = dataInputStream.readInt();
			intnt.putExtra("result", (status==1));
			SocketService.this.sendBroadcast(intnt);
			//0 失败 1 成功
			Log.d(TAG, "返回结果：" + status);
		} 
		catch (IOException e)
		{
			intnt.putExtra("result", false);
			SocketService.this.sendBroadcast(intnt);
			//e.printStackTrace();
		}
	}
	//发送连接基本信息
	private void sendConnectInfo()
	{
		Intent intnt = new Intent(CaptureActivity.CAPTUREACTIVITY_REV_ACTION_RESULT);  
		if(isconnected==false||socket.isConnected()==false) 	
		{
			intnt.putExtra("result", false);
			SocketService.this.sendBroadcast(intnt);
			return;
		}
		try
		{
			DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
			DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
			dataOutputStream.write(head);//数据头
			dataOutputStream.writeInt(0);
			dataOutputStream.writeInt(0);
			SharedPreferences sharedPreferences = getSharedPreferences("config", Context.MODE_PRIVATE);
			String username = sharedPreferences.getString("username","chuck");
			dataOutputStream.writeInt((username.getBytes()).length);//昵称长度 
			dataOutputStream.write(username.getBytes());//昵称
			// 得到androidId号
			String androidId;
			try
			{
				androidId = Settings.Secure.getString(getContentResolver(),Settings.Secure.ANDROID_ID);
			} 
			catch (Exception e)
			{
				Random ran = new Random();
				StringBuffer strb = new StringBuffer();
				int num1 = 0;
				for (int i = 0; i < 64 / 8; i++) {// 这里是产生9位的64/8=8次，
					while (true) {
						num1 = ran.nextInt(99999999);
						System.out.println(num1);
						if (num1 > 10000000) 
						{
							strb.append(num1);
							break;
						}
					}
				}
				androidId = strb.toString();
			}
			Log.d(TAG, "IMEI:" + androidId);
			
			dataOutputStream.writeInt((androidId.getBytes()).length);//设备id长度 
			dataOutputStream.write(androidId.getBytes());//设备id
			dataOutputStream.write(tail);//数据尾
			dataOutputStream.flush();//发送
			Log.d(TAG, "socket发送完成");
			// 一定要加上这句，否则收不到来自服务器端的消息返回
			//socket.shutdownOutput();
			// 获取服务器端的相应返回
			int status = dataInputStream.readInt();
			//0 失败 1 成功
			Log.d(TAG, "返回结果：" + status);
			intnt.putExtra("result", (status==1));
			SocketService.this.sendBroadcast(intnt);
		} 
		catch (IOException e)
		{
			intnt.putExtra("result", false);
			SocketService.this.sendBroadcast(intnt);
		}
		
	}
	@Override
	public IBinder onBind(Intent intent)
	{
		return null;
	}
	
	@Override 
    public void onCreate() 
	{ 
		Log.v(TAG, "SocketService onCreate");  
		super.onCreate(); 
		//注册广播 
        registerBoradcastReceiver(); 
    } 
	//注册广播      
	public void registerBoradcastReceiver()
	{
		IntentFilter myIntentFilter = new IntentFilter();
		myIntentFilter.addAction(SOCKETSERVICE_REV_ACTION_CONN);
		myIntentFilter.addAction(SOCKETSERVICE_REV_ACTION_SEND);
		registerReceiver(mBroadcastReceiver, myIntentFilter);
		Log.v(TAG, "SocketService registerReceiver");  
	}
	@Override 
    public void onStart(Intent intent, int startId) 
    { 
		Log.v(TAG, "SocketService onStart");  
    	super.onStart(intent, startId); 
    }
	@Override 
    public void onDestroy() 
	{ 
		Log.v(TAG, "SocketService onDestroy");  
		super.onDestroy(); 
    } 

    
}
