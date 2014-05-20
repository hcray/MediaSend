package com.kraken.mediasend.gallery;

import java.io.File;

import com.kraken.mediasend.gallery.Constant.ImageFolderInfo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.kraken.mediasend.R;

public class GalleryActivity extends Activity
{
	//public static Activity mActivity;
	private LinearLayout data;
	//作用： 
	//1、对于一个没有被载入或者想要动态载入的界面, 都需要使用inflate来载入. 
	//2、对于一个已经载入的Activity, 就可以使用实现了这个Activiyt的的findViewById方法来获得其中的界面元素. 
	private LayoutInflater mInflater;

	private final static int UPDATELIST = 0;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_list_view);
		//mActivity = this;
		data = (LinearLayout) findViewById(R.id.data);
		mInflater = LayoutInflater.from(this);
		new ScanThread().start();
	}
	
	private class ScanThread extends Thread
	{
		@Override
		public void run()
		{
			final String mCardPath = Environment.getExternalStorageDirectory().getPath();
			getFiles(mCardPath);
		}
	}
	
	private void getFiles(String path)
	{
		File f = new File(path);
		File[] files = f.listFiles();
		ImageFolderInfo ifi = new ImageFolderInfo();
		ifi.path = path;
		if (files != null)
		{
			for (int i = 0; i < files.length; i++)
			{
				final File ff = files[i];
				if (ff.isDirectory())
				{
					getFiles(ff.getPath());
				} 
				else
				{
					String fName = ff.getName();
					if (fName.indexOf(".") > -1)
					{
						String end = fName.substring(fName.lastIndexOf(".") + 1, fName.length()).toUpperCase();
						if (Constant.getExtens().contains(end))
						{
							ifi.filePathes.add(ff.getPath());
						}
					}
				}
			}
		}
		//如果不是空的图片文件夹，则列出来
		if (!ifi.filePathes.isEmpty())
		{
			ifi.pisNum = ifi.filePathes.size();
			String imagePath = ifi.filePathes.get(0);
			//BitmapFactory可以从一个指定文件中，利用decodeFile()解出Bitmap
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inSampleSize = 7;
			Bitmap bm = BitmapFactory.decodeFile(imagePath, options);
			ifi.image = new BitmapDrawable(bm);
			//这个线程将发消息给主线程
			Message msg = new Message();
			msg.what = UPDATELIST;
			msg.obj = ifi;
			mHandler.sendMessage(msg);
		}
	}

	// 处理扫描线程扫描到的图片文件夹
	private Handler mHandler = new Handler()
	{
		public void handleMessage(android.os.Message msg)
		{
			switch (msg.what)
			{
			case UPDATELIST:
				ImageFolderInfo holder = (ImageFolderInfo) msg.obj;
				//通过list_item来布局每个文件夹
				View convertView = mInflater.inflate(R.layout.list_item, null);
				((ImageView) convertView.findViewById(R.id.icon)).setImageDrawable(holder.image);
				File file = new File(holder.path);
				((TextView) convertView.findViewById(R.id.name)).setText("文件夹名:"+file.getName());//文件夹名
				((TextView) convertView.findViewById(R.id.path)).setText("文件夹路径:"+holder.path);//文件夹路径
				((TextView) convertView.findViewById(R.id.picturecount)).setText("图片数量:"+holder.pisNum + "");//当前文件夹下图片数量
				//这里设置tag以便点击时候获取
				convertView.setTag(holder);
				convertView.setOnClickListener(new OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						ImageFolderInfo info = (ImageFolderInfo) v.getTag();
						Intent intent = new Intent(GalleryActivity.this,GridImageViewActivity.class);
						//将所有的图片文件路径数组传过去
						intent.putStringArrayListExtra("data", info.filePathes);
						startActivity(intent);
					}
				});
				data.addView(convertView);
				break;
			default:
				break;
			}
		}
	};



	

}