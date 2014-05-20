package com.kraken.mediasend;

import java.io.IOException;
import java.util.Vector;

import android.R.string;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.kraken.mediasend.camera.CameraManager;
import com.kraken.mediasend.decoding.CaptureActivityHandler;
import com.kraken.mediasend.decoding.InactivityTimer;
import com.kraken.mediasend.gallery.GalleryActivity;
import com.kraken.mediasend.view.ViewfinderView;
//import android.content.SharedPreferences;

public class CaptureActivity extends Activity implements Callback
{

	private CaptureActivityHandler handler;
	private ViewfinderView viewfinderView;
	private boolean hasSurface;
	private Vector<BarcodeFormat> decodeFormats;
	private String characterSet;
	private TextView txtResult;
	private InactivityTimer inactivityTimer;
	private MediaPlayer mediaPlayer;
	private boolean playBeep;
	private static final float BEEP_VOLUME = 0.10f;
	private boolean vibrate;
	
	
	private static final String TAG = "CaptureActivity" ;  
	public static final String CAPTUREACTIVITY_REV_ACTION_RESULT = "com.kraken.mediasend.CaptureActivity.CAPTUREACTIVITY_REV_ACTION_RESULT";
	
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_capture);
		// 初始化 CameraManager
		CameraManager.init(getApplication());
		viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
		txtResult = (TextView) findViewById(R.id.txtResult);
		hasSurface = false;
		inactivityTimer = new InactivityTimer(this);
		registerBoradcastReceiver();
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
		SurfaceHolder surfaceHolder = surfaceView.getHolder();
		if (hasSurface)
		{
			initCamera(surfaceHolder);
		} else
		{
			surfaceHolder.addCallback(this);
			surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}
		decodeFormats = null;
		characterSet = null;

		playBeep = true;
		AudioManager audioService = (AudioManager) getSystemService(AUDIO_SERVICE);
		if (audioService.getRingerMode() != AudioManager.RINGER_MODE_NORMAL)
		{
			playBeep = false;
		}
		initBeepSound();
		vibrate = true;
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		if (handler != null)
		{
			handler.quitSynchronously();
			handler = null;
		}
		CameraManager.get().closeDriver();
	}

	@Override
	protected void onDestroy()
	{
		inactivityTimer.shutdown();
		unRegisterBoradcastReceiver();
		super.onDestroy();
	}

	private void initCamera(SurfaceHolder surfaceHolder)
	{
		try
		{
			CameraManager.get().openDriver(surfaceHolder);
		} catch (IOException ioe)
		{
			return;
		} catch (RuntimeException e)
		{
			return;
		}
		if (handler == null)
		{
			handler = new CaptureActivityHandler(this, decodeFormats,characterSet);
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,int height)
	{

	}

	@Override
	public void surfaceCreated(SurfaceHolder holder)
	{
		if (!hasSurface)
		{
			hasSurface = true;
			initCamera(holder);
		}

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder)
	{
		hasSurface = false;

	}

	public ViewfinderView getViewfinderView()
	{
		return viewfinderView;
	}

	public Handler getHandler()
	{
		return handler;
	}

	public void drawViewfinder()
	{
		viewfinderView.drawViewfinder();

	}
	public boolean isIp(String IP){//判断是否是一个IP   
        boolean b = false;   
        if(IP.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")){   
            String s[] = IP.split("\\.");   
            if(Integer.parseInt(s[0])<255)   
                if(Integer.parseInt(s[1])<255)   
                    if(Integer.parseInt(s[2])<255)   
                        if(Integer.parseInt(s[3])<255)   
                            b = true;   
        }   
        return b;   
    } 
	public void handleDecode(Result obj, Bitmap barcode)
	{
		inactivityTimer.onActivity();
		viewfinderView.drawResultBitmap(barcode);
		playBeepSoundAndVibrate();
		txtResult.setText(obj.getBarcodeFormat().toString() + ":" + obj.getText());

		String retStr = obj.getText().toString();
		String[] retStrs = retStr.split(":");
		if(retStrs.length!=2||isIp(retStrs[0])==false)
		{
			Toast toast =Toast.makeText(CaptureActivity.this, retStr+"\n请输入有效的二维码",Toast.LENGTH_LONG);
			toast.setGravity(Gravity.CENTER, 0, 0);
			toast.show(); 
			this.finish();
			return;
		}
		String edtServerIpValue = retStrs[0];
		int edtServerPortValue = Integer.parseInt(retStrs[1]);
		
		Intent intnt = new Intent(SocketService.SOCKETSERVICE_REV_ACTION_CONN);  
		intnt.putExtra("edtServerIpValue", edtServerIpValue);
		intnt.putExtra("edtServerPortValue", edtServerPortValue);
		CaptureActivity.this.sendBroadcast(intnt);
		
	}

	//注册广播      
	public void registerBoradcastReceiver()
	{
		IntentFilter myIntentFilter = new IntentFilter();
		myIntentFilter.addAction(CAPTUREACTIVITY_REV_ACTION_RESULT);
		registerReceiver(mBroadcastReceiver, myIntentFilter);
		Log.v(TAG, "registerReceiver CAPTUREACTIVITY_REV_ACTION_RESULT"); 
	}
	public void unRegisterBoradcastReceiver()
	{
		unregisterReceiver(mBroadcastReceiver);  
	}
	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent) 
		{
			String action = intent.getAction();
			if(action.equals(CaptureActivity.CAPTUREACTIVITY_REV_ACTION_RESULT))
			{
				Bundle bundle = intent.getExtras();  		
				boolean bl = bundle.getBoolean("result");
				String Message = bundle.getString("Message");
				Log.v(TAG, "CAPTUREACTIVITY_REV_ACTION_RESULT"+bl);  
				if(bl==true)
				{
					Toast toast =Toast.makeText(CaptureActivity.this, "连接服务器成功",Toast.LENGTH_SHORT);
					toast.setGravity(Gravity.CENTER, 0, 0);
					toast.show(); 
					Intent mIntent = new Intent();
					mIntent.setClass(CaptureActivity.this, GalleryActivity.class);
 					startActivity(mIntent);
				}
				else 
				{
					Toast toast =Toast.makeText(CaptureActivity.this, "连接服务器失败"+Message,Toast.LENGTH_LONG);
					toast.setGravity(Gravity.CENTER, 0, 0);
					toast.show(); 
					CaptureActivity.this.finish();
					return;	
				}
			}
		}
	};
	
	private void initBeepSound()
	{
		if (playBeep && mediaPlayer == null)
		{
			// The volume on STREAM_SYSTEM is not adjustable, and users found it
			// too loud,
			// so we now play on the music stream.
			setVolumeControlStream(AudioManager.STREAM_MUSIC);
			mediaPlayer = new MediaPlayer();
			mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mediaPlayer.setOnCompletionListener(beepListener);

			AssetFileDescriptor file = getResources().openRawResourceFd(R.raw.beep);
			try
			{
				mediaPlayer.setDataSource(file.getFileDescriptor(),
						file.getStartOffset(), file.getLength());
				file.close();
				mediaPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME);
				mediaPlayer.prepare();
			} catch (IOException e)
			{
				mediaPlayer = null;
			}
		}
	}

	private static final long VIBRATE_DURATION = 200L;

	private void playBeepSoundAndVibrate()
	{
		if (playBeep && mediaPlayer != null)
		{
			mediaPlayer.start();
		}
		if (vibrate)
		{
			Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
			vibrator.vibrate(VIBRATE_DURATION);
		}
	}

	/**
	 * When the beep has finished playing, rewind to queue up another one.
	 */
	private final OnCompletionListener beepListener = new OnCompletionListener()
	{
		public void onCompletion(MediaPlayer mediaPlayer)
		{
			mediaPlayer.seekTo(0);
		}
	};

}