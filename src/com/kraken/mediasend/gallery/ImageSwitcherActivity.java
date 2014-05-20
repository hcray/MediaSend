
package com.kraken.mediasend.gallery;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.kraken.mediasend.CaptureActivity;
import com.kraken.mediasend.MainActivity;
import com.kraken.mediasend.R;
import com.kraken.mediasend.SocketService;
import com.kraken.mediasend.gallery.zoom.ImageZoomView;
import com.kraken.mediasend.gallery.zoom.SimpleZoomListener;
import com.kraken.mediasend.gallery.zoom.SimpleZoomListener.ControlType;
import com.kraken.mediasend.gallery.zoom.ZoomState;


public class ImageSwitcherActivity extends Activity
{
	private static final String TAG = "ImageSwitcher";
	public static final String MEDIAACTIVITY_REV_ACTION_RESULT = "com.kraken.mediasend.Gallery.ImageSwitcher.MEDIAACTIVITY_REV_ACTION_RESULT";
	private int mIndex;

	private int mItemwidth;
	private int mItemHerght;

	private ArrayList<String> pathes;

	private ProgressBar mProgressBar;
	private Button btSend;

	/** Image zoom view */
	private ImageZoomView mZoomView;

	/** Zoom state */
	private ZoomState mZoomState;

	/** On touch listener for zoom view */
	private SimpleZoomListener mZoomListener;

	private Bitmap zoomBitmap;

	private ImageView mMovedItem;
	private boolean isMoved;

	private FlingGallery mFlingGallery;
	
	private ProgressDialog progressDialog;  
	//注册广播      
		public void registerBoradcastReceiver()
		{
			IntentFilter myIntentFilter = new IntentFilter();
			myIntentFilter.addAction(MEDIAACTIVITY_REV_ACTION_RESULT);
			registerReceiver(mBroadcastReceiver, myIntentFilter);
			Log.v(TAG, "registerReceiver MEDIAACTIVITY_REV_ACTION_RESULT"); 
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
				if(action.equals(ImageSwitcherActivity.MEDIAACTIVITY_REV_ACTION_RESULT))
				{
					Bundle bundle = intent.getExtras();  		
					boolean bl = bundle.getBoolean("result");
					Log.v(TAG, "MEDIAACTIVITY_REV_ACTION_RESULT"+bl);  
					if(progressDialog.isShowing())
						progressDialog.dismiss();
					if(bl==true)
					{
						/*Dialog alertDialog = new AlertDialog.Builder(ImageSwitcherActivity.this). 
				                 setMessage("发送成功"). 
				                 setPositiveButton("确定", new DialogInterface.OnClickListener() { 
				                     @Override 
				                     public void onClick(DialogInterface dialog, int which) { 
				                         // TODO Auto-generated method stub  
				                     } 
				                 }). 
				                 create(); 
				         alertDialog.show(); */
						Toast toast =Toast.makeText(ImageSwitcherActivity.this, "发送成功",Toast.LENGTH_SHORT);
						toast.setGravity(Gravity.CENTER, 0, 0);
						toast.show(); 
					}
					else 
					{
						Dialog alertDialog = new AlertDialog.Builder(ImageSwitcherActivity.this). 
				                 setMessage("发送失败"). 
				                 setPositiveButton("返回主页", new DialogInterface.OnClickListener() { 
				                     @Override 
				                     public void onClick(DialogInterface dialog, int which) { 
				                    	Intent mIntent = new Intent();
				         				mIntent.setClass(ImageSwitcherActivity.this, MainActivity.class);
				         				startActivity(mIntent);
				         				ImageSwitcherActivity.this.finish();
				                     } 
				                 }). 
				                 create(); 
				         alertDialog.show(); 
					}
				}
			}
		};
		
	public int getmIndex()
	{
		return mIndex;
	}

	public void updateState(int visibility)
	{
		mProgressBar.setVisibility(visibility);
		mFlingGallery.setCanTouch(View.GONE == visibility);
	}

	private boolean isViewIntent()
	{
		String action = getIntent().getAction();
		return Intent.ACTION_VIEW.equals(action);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		Log.v(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		mItemwidth = dm.widthPixels;
		mItemHerght = dm.heightPixels;
		// mInflater = LayoutInflater.from(this);
		if (!isViewIntent())
		{
			pathes = intent.getStringArrayListExtra("pathes");
			mIndex = intent.getIntExtra("index", 0);
		} else
		{
			pathes = new ArrayList<String>();
			pathes.add(intent.getData().getPath());
			mIndex = 0;
		}

		setContentView(R.layout.activity_myhorizontalview);
		mProgressBar = (ProgressBar) findViewById(R.id.progress_circular);
		mMovedItem = (ImageView) findViewById(R.id.removed);
		mFlingGallery = (FlingGallery) findViewById(R.id.horizontalview);
		mZoomView = (ImageZoomView) findViewById(R.id.zoomview);
		
		mZoomState = new ZoomState();
		mZoomListener = new SimpleZoomListener();
		mZoomListener.setmGestureDetector(new GestureDetector(this,	new MyGestureListener()));

		mZoomListener.setZoomState(mZoomState);
		mZoomListener.setControlType(ControlType.ZOOM);
		mZoomView.setZoomState(mZoomState);
		mZoomView.setOnTouchListener(mZoomListener);

		// hsv = (MyHorizontalScrollView)
		mFlingGallery.setAdapter(new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1,pathes)
		{
			@Override
			public View getView(int position, View convertView, ViewGroup parent)
			{
				return new GalleryViewItem(getApplicationContext(), position);
			}
		}, mIndex);
		
		btSend = (Button)findViewById(R.id.btn_send);
		btSend.setOnClickListener(new OnClickListener()
		{
		    @Override
		    public void onClick(View v)
		    {
		    	snedMedia();
		    }
		});
		registerBoradcastReceiver();
	}
	public void snedMedia()
	{
		Intent intnt = new Intent(SocketService.SOCKETSERVICE_REV_ACTION_SEND); 
		String pathString = pathes.get(mFlingGallery.getCurrentPosition());
		intnt.putExtra("mediapath", pathString);
		ImageSwitcherActivity.this.sendBroadcast(intnt);
		openDialog();
	}
	private void openDialog()
	{
		 progressDialog=new ProgressDialog(ImageSwitcherActivity.this);  
		 progressDialog.setMessage("正在发送数据...");  
		 progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		 progressDialog.show();  
	}
	public void goneTempImage()
	{
	}

	private Bitmap getDrawable(int index, int zoom)
	{
		if (index >= 0 && index < pathes.size())
		{
			String path = pathes.get(index);

			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(path, options);
			int mWidth = options.outWidth;
			int mHeight = options.outHeight;
			int s = 1;
			while ((mWidth / s > mItemwidth * 2 * zoom)
					|| (mHeight / s > mItemHerght * 2 * zoom))
			{
				s *= 2;
			}

			options = new BitmapFactory.Options();
			options.inPreferredConfig = Config.ARGB_8888;
			options.inSampleSize = s;
			Bitmap bm = BitmapFactory.decodeFile(path, options);

			if (bm != null)
			{
				int h = bm.getHeight();
				int w = bm.getWidth();

				float ft = (float) ((float) w / (float) h);
				float fs = (float) ((float) mItemwidth / (float) mItemHerght);

				int neww = ft >= fs ? mItemwidth * zoom : (int) (mItemHerght
						* zoom * ft);
				int newh = ft >= fs ? (int) (mItemwidth * zoom / ft)
						: mItemHerght * zoom;

				float scaleWidth = ((float) neww) / w;
				float scaleHeight = ((float) newh) / h;

				Matrix matrix = new Matrix();
				matrix.postScale(scaleWidth, scaleHeight);
				bm = Bitmap.createBitmap(bm, 0, 0, w, h, matrix, true);
				return bm;
			}
		}
		return null;
	}

	private void resetZoomState()
	{
		Log.v(TAG, "resetZoomState");
		int currentIndex = mFlingGallery.getCurrentPosition();
		if (zoomBitmap != null)
		{
			zoomBitmap.recycle();
		}
		Log.v(TAG, "resetZoomState2");
		zoomBitmap = getDrawable(currentIndex, 3);
		Log.v(TAG, "resetZoomState3");
		mZoomView.setImage(zoomBitmap);
		Log.v(TAG, "resetZoomState4");
		mZoomListener.setControlType(ControlType.ZOOM);
		mZoomState.setPanX(0.5f);
		mZoomState.setPanY(0.5f);
		mZoomState.setZoom(3f);
		mZoomState.notifyObservers();
		
	}

	/**
	 * �鿴ģʽ
	 * 
	 * @Description:
	 * @Author: wanghb
	 * @Email: wanghb@foryouge.com.cn
	 * @Others:
	 */
	public void goToZoomPage()
	{
		Log.v(TAG, "goToZoomPage");
		resetZoomState();
		mFlingGallery.setVisibility(View.GONE);
		isMoved = false;
		mZoomView.setVisibility(View.VISIBLE);
		mMovedItem.setBackgroundColor(0x0000);
		mMovedItem.setVisibility(View.VISIBLE);
	}

	public void goToSwicherPage()
	{
		Log.v(TAG, "goToSwicherPage");
		mMovedItem.setVisibility(View.GONE);
		mFlingGallery.setVisibility(View.VISIBLE);
		mZoomView.setVisibility(View.GONE);
	}

	private class MyGestureListener extends GestureDetector.SimpleOnGestureListener
	{
		@Override
		public boolean onDoubleTap(MotionEvent e)
		{
			Log.v(TAG, "onDoubleTap");
			goToSwicherPage();
			return true;
		}
		//@Override  
		// e1：第1个ACTION_DOWN MotionEvent   
		// e2：最后一个ACTION_MOVE MotionEvent   
		// velocityX：X轴上的移动速度，像素/秒   
		// velocityY：Y轴上的移动速度，像素/秒   
		//public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,float velocityY) 
		//{
			//openDialog();
			//return true;  
		//}
	}

	public void movedClick(View v)
	{
		Log.v(TAG, "movedClick");
		isMoved = !isMoved;
		if (isMoved)
		{
			mZoomListener.setControlType(ControlType.PAN);
			mMovedItem
					.setBackgroundColor(R.drawable.pressed_application_background);
		} else
		{
			mZoomListener.setControlType(ControlType.ZOOM);
			mMovedItem.setBackgroundColor(0x0000);
		}
	}

	@Override
	protected void onDestroy()
	{
		Log.v(TAG, "onDestroy");
		if (zoomBitmap != null)
		{
			zoomBitmap.recycle();
		}
		unRegisterBoradcastReceiver();
		super.onDestroy();
	}

	private class GalleryViewItem extends LinearLayout
	{

		public GalleryViewItem(Context context, int position)
		{
			
			super(context);
			Log.v(TAG, "GalleryViewItem con");
			this.setOrientation(LinearLayout.VERTICAL);

			this.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.MATCH_PARENT));

			ImageView iv = new ImageView(context);
			iv.setImageBitmap(getDrawable(position, 1));

			this.addView(iv, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.MATCH_PARENT));
		}
	}
}
