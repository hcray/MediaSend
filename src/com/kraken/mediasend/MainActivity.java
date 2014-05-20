package com.kraken.mediasend;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


public class MainActivity extends Activity
{
	private static final String TAG = "MainActivity" ;  
	private Button btnScan; // 扫描二维码
	private EditText et_username;
	//去掉特殊字符
	public static String stringFilter(String str)throws PatternSyntaxException
	{
	         String dest = ""; 
	         if (str!=null) 
	         { 
	        	 String regEx="[`~!@#$%^&*()+=|{}':;',//[//].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？\\s*|\t|\r|\n]";   
	             Pattern p = Pattern.compile(regEx); //"\\s*|\t|\r|\n|"
	             Matcher m = p.matcher(str); 
	             dest = m.replaceAll("").trim();
	         } 
	         return dest; 
	     } 

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		Log.v(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		et_username = (EditText)this.findViewById(R.id.et_username);
		//判断本地是否有存储昵称
		SharedPreferences sharedPreferences = getSharedPreferences("config", Context.MODE_PRIVATE);
		String username = sharedPreferences.getString("username","");
		if(null!=username)
		{
			et_username.setText(username);
		}
		et_username.addTextChangedListener(new TextWatcher() 
		{
            int cou = 0;
            int selectionEnd = 0;
            int mMaxLenth = 15;
            @Override
            public void onTextChanged(CharSequence s, int start, int before,int count) 
            {
                cou = before + count;
                String editable = et_username.getText().toString();
                String str = stringFilter(editable);
                if (!editable.equals(str)) 
                {
                	et_username.setText(str);
                }
                et_username.setSelection(et_username.length());
                cou = et_username.length();
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,int after) {}
            @Override
            public void afterTextChanged(Editable s) 
            {
                if (cou > mMaxLenth) 
                {
                    selectionEnd = et_username.getSelectionEnd();
                    s.delete(mMaxLenth, selectionEnd);
                    //if(androidVersion.charAt(0)>='4')
                    {
                    	et_username.setText(s.toString());
                    }

                }

            }

        });
	
		//点击扫描按钮
		btnScan = (Button)this.findViewById(R.id.btn_scan);
		OnClickListener btnScanClick = new OnClickListener()
		{
			
			@Override
			public void onClick(View v)
			{
				if (et_username.getText().toString().trim().equals("")|| et_username.getText().toString().trim()==null)
				{
					Toast toast =Toast.makeText(MainActivity.this, "请输入用户名",Toast.LENGTH_LONG);
					toast.setGravity(Gravity.CENTER, 0, 0);
					toast.show(); 
					return;
				}
				if(isWifiEnabled()==false || isWifi()==false)
				{
					Toast toast =Toast.makeText(MainActivity.this, "请连接局域网wifi",Toast.LENGTH_LONG);
					toast.setGravity(Gravity.CENTER, 0, 0);
					toast.show(); 
					return;
				}
				//存到本地
				SharedPreferences sharedPreferences = getSharedPreferences("config", Context.MODE_PRIVATE);
				Editor editor = sharedPreferences.edit();
				editor.putString("username", et_username.getText().toString().trim());
				editor.commit();

				Intent mIntent = new Intent();
				mIntent.setClass(MainActivity.this, CaptureActivity.class);
				startActivity(mIntent);
			}
		};
		btnScan.setOnClickListener(btnScanClick);
		//开始后台服务
		startService(new Intent(this,SocketService.class));
		
		
	}

	//wifi是否可用
	public boolean isWifiEnabled()
	{
		ConnectivityManager mgrConn = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
		TelephonyManager mgrTel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
		return ((mgrConn.getActiveNetworkInfo() != null && mgrConn.getActiveNetworkInfo().getState() == NetworkInfo.State.CONNECTED) || mgrTel.getNetworkType() == TelephonyManager.NETWORK_TYPE_UMTS);
	}
	//是否wifi网络
	public boolean isWifi()
	{
		ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkINfo = cm.getActiveNetworkInfo();
		if (networkINfo != null&& networkINfo.getType() == ConnectivityManager.TYPE_WIFI)
		{
			return true;
		}
		return false;
	}
}
