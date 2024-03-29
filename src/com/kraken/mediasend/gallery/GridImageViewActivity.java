package com.kraken.mediasend.gallery;

import java.io.File;
import java.util.ArrayList;

import com.kraken.mediasend.gallery.Constant.gridItemEntity;
import com.kraken.mediasend.R;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class GridImageViewActivity extends Activity
{
	private static final String TAG = "GridImageView";
	private LayoutInflater mInflater;
	private int currentConlumID = -1;//当前列号
	private int currentCount = 1;//当前索引
	private int displayHeight;//屏幕分辨率
	private LinearLayout data;

	private int itemh = 150;//高
	private int itemw = 150;

	private ArrayList<String> imagePathes;//图片路径数组

	private boolean exit;
	private boolean isWait;

	private boolean firstRun = true;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		Log.v(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_horizontalview);
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		displayHeight = dm.heightPixels;
		mInflater = LayoutInflater.from(this);
		Intent intent = getIntent();
		imagePathes = intent.getStringArrayListExtra("data");
		data = (LinearLayout) findViewById(R.id.layout_webnav);
	}
	@Override
	protected void onResume()
	{
		Log.v(TAG, "onResume");
		if (mThread.isAlive())
		{
			synchronized (mThread)
			{
				mThread.notify();//唤醒线程，继续遍历显示
			}
		} 
		else
		{
			if (firstRun)
			{
				firstRun = !firstRun;
				mThread.start();
			}
		}
		super.onResume();
	}

	@Override
	protected void onDestroy()
	{
		Log.v(TAG, "onDestroy");
		exit = true;
		super.onDestroy();
	}
	
	private Thread mThread = new Thread()
	{
		public void run()
		{
			for (int i = 0; i < imagePathes.size() && !exit; i++)
			{
				if (isWait)
				{
					isWait = !isWait;
					synchronized (this)
					{
						try
						{
							this.wait();
						} 
						catch (InterruptedException e)
						{
							e.printStackTrace();
						}
					}
				}
				String path = imagePathes.get(i);
				if (new File(path).exists())
				{

					gridItemEntity gie = new gridItemEntity();
					Bitmap bm = getDrawable(i, 2);
					if (bm != null)
					{
						if (isWait)
						{
							isWait = !isWait;
							synchronized (this)
							{
								try
								{
									this.wait();
								} 
								catch (InterruptedException e)
								{
									e.printStackTrace();
								}
							}
						}
						gie.image = new BitmapDrawable(bm);
						gie.path = path;
						gie.index = i;
						android.os.Message msg = new Message();
						msg = new Message();
						msg.what = 0;
						msg.obj = gie;
						mHandler.sendMessage(msg);
					}
				}
			}
		}
	};

	private Handler mHandler = new Handler()
	{
		public void handleMessage(android.os.Message msg)
		{
			switch (msg.what)
			{
			case 0:
				gridItemEntity gie = (gridItemEntity) msg.obj;
				if (gie != null)
				{
					//竖向可以分成几份
					int num = displayHeight / itemh;
					num = num == 0 ? 1 : num;
					
					LinearLayout ll;
					if ((currentCount - 1) % num > 0)
					{
						ll = (LinearLayout) data.findViewWithTag("columnId_"+ currentConlumID);
					} 
					else
					{
						ll = (LinearLayout) mInflater.inflate(R.layout.item_column, null);
						currentConlumID--;
						ll.setTag("columnId_" + currentConlumID);
						for (int j = 0; j < num; j++)
						{
							LinearLayout child = new LinearLayout(GridImageViewActivity.this);
							child.setLayoutParams(new LayoutParams(itemw, itemh));
							child.setTag("item_" + j);
							ll.addView(child);
						}
						data.addView(ll);
					}

					int step = currentCount % num - 1;
					if (step == -1)
					{
						step = num - 1;
					}
					LinearLayout child = (LinearLayout) ll.findViewWithTag("item_" + step);
					child.setBackgroundResource(R.drawable.grid_selector);
					child.setTag(gie);
					child.setOnClickListener(imageClick);
					child.setPadding(10, 10, 10, 10);
					ImageView v = new ImageView(GridImageViewActivity.this);
					v.setImageDrawable(gie.image);
					child.addView(v);
					currentCount++;
				}
				break;
			default:
				break;
			}
		}
	};

	private OnClickListener imageClick = new OnClickListener()
	{
		@Override
		public void onClick(View view)
		{
			gridItemEntity gie = (gridItemEntity) view.getTag();
			Intent it = new Intent(GridImageViewActivity.this, ImageSwitcherActivity.class);
			it.putStringArrayListExtra("pathes", imagePathes);
			it.putExtra("index", gie.index);
			startActivity(it);
			if (mThread.isAlive())
			{
				//若没显示完就点了，则暂停遍历显示线程
				isWait = true;
			}
		}
	};


	private Bitmap getDrawable(int index, int zoom)
	{
		if (index >= 0 && index < imagePathes.size())
		{
			String path = imagePathes.get(index);

			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(path, options);
			int mWidth = options.outWidth;
			int mHeight = options.outHeight;
			int s = 1;
			while ((mWidth / s > itemw * 2 * zoom)|| (mHeight / s > itemh * 2 * zoom))
			{
				s *= 2;
			}

			options = new BitmapFactory.Options();
			options.inSampleSize = s;
			options.inPreferredConfig = Config.ARGB_8888;
			Bitmap bm = BitmapFactory.decodeFile(path, options);

			if (bm != null)
			{
				int h = bm.getHeight();
				int w = bm.getWidth();

				float ft = (float) ((float) w / (float) h);
				float fs = (float) ((float) itemw / (float) itemh);

				int neww = ft >= fs ? itemw * zoom : (int) (itemh * zoom * ft);
				int newh = ft >= fs ? (int) (itemw * zoom / ft) : itemh * zoom;

				float scaleWidth = ((float) neww) / w;
				float scaleHeight = ((float) newh) / h;

				Matrix matrix = new Matrix();
				matrix.postScale(scaleWidth, scaleHeight);
				bm = Bitmap.createBitmap(bm, 0, 0, w, h, matrix, true);

				// Bitmap bm1 = Bitmap.createScaledBitmap(bm, w, h, true);
				// if (!bm.isRecycled()) {// 先判断图片是否已释放了
				// bm.recycle();
				// }
				return bm;
			}
		}
		return null;
	}

}
