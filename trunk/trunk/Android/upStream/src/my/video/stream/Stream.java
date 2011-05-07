package my.video.stream;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;

import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import java.util.Enumeration;
import java.net.NetworkInterface;

import android.graphics.Bitmap;
import android.graphics.Canvas;

import android.app.AlertDialog;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;

import android.widget.ImageView;
import android.widget.LinearLayout;
import 	android.content.res.Configuration;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.List;
import android.util.*;

import java.net.InetAddress;
import java.net.Socket;

import java.io.OutputStream;
import java.io.PrintWriter;

import android.content.Intent;
import android.widget.Toast;
import 	android.os.PowerManager.WakeLock;
import 	android.os.PowerManager;
import java.util.Arrays;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;

import android.os.Environment;
import 	java.util.Timer;
import 	java.util.TimerTask;
import 	java.lang.Thread;
import 	java.io.DataInputStream;

public class Stream extends Activity
{
    private CamView mPreview;
    Camera mCamera;
	ImageView pause;
	ImageView play;
	ImageView close;
	ImageView minimize;
	PowerManager pm;
	PowerManager.WakeLock wl;
    LinearLayout ll;
	String TAG="My Camera App";
	int visibility=0;
	
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stream_view);
		
		
		
		pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "StreamApp");
		wl.acquire();
		Intent i=getIntent();
		
		mPreview = (CamView)findViewById(R.id.cam);
		mPreview.setIp(i.getStringExtra("SERVER_IP"));
		mCamera = Camera.open();
        mPreview.setCamera(mCamera);
		//mPreview.iv=(ImageView)findViewById(R.id.lpimg);
        mPreview.Initialize();
		mCamera.startPreview();
		
		
		
		ll=(LinearLayout)findViewById(R.id.ll);
		
		minimize=(ImageView)findViewById(R.id.minimize);
		minimize.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				mPreview.Resume();
				mPreview.isUpstreaming=!mPreview.isUpstreaming;
				Log.i(TAG,"Mode switched...........");
			}
		});
		
		
		pause=(ImageView)findViewById(R.id.pause);
		pause.setOnClickListener(new View.OnClickListener()
		{
            public void onClick(View view) 
			{
                mPreview.Pause();
				Log.i(TAG,"paused...........");
            }
        });
		
		play=(ImageView)findViewById(R.id.play);
		play.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				mPreview.Resume();
				Log.i(TAG,"resumed...........");
			}
		});
		
		close=(ImageView)findViewById(R.id.close);
		close.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				mPreview.releaseAll();
				Stream.this.finish();
				Log.i(TAG,"closed...........");
			}
		});		
	}
	
	 @Override
    protected void onResume() 
	{
        super.onResume();
		wl.acquire();
		if(mCamera==null)
		{
			mCamera = Camera.open();
			mPreview.setCamera(mCamera);
			mCamera.startPreview();
		}
    }

    @Override
    protected void onPause() 
	{
        super.onPause();
		wl.release();
        mPreview.releaseAll();
        if (mCamera != null) 
		{
            mCamera.release();
            mCamera = null;
        }
    }
	
	
}

class CamView extends SurfaceView implements SurfaceHolder.Callback
{

    SurfaceHolder mHolder;
	Camera mCamera;	
	boolean isPaused=false;
	boolean isIpSet=false;
	public boolean isUpstreaming=true;
	
	DataSender sender;
	
	private static final String TAG = "CamView [UPSTREAM]";
    
	int pH,pW;
	
	CamView(Context context) 
	{
        super(context);      
	}
	
	public CamView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }
  
    public CamView(Context context, AttributeSet attrs, int defStyle)
	{
       super(context, attrs, defStyle);
    }
	
	public void setIp(String ip)
	{
		//ipAddress=ip;
		sender = new DataSender(ip);
		new Thread(sender).start();
		isIpSet=true;
	}
	
	public void setCamera(Camera camera) 
	{
		mCamera=camera;
		if(mCamera != null)
		{
			//mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
			Camera.Parameters parameters = mCamera.getParameters();
			parameters.setPreviewSize(320, 240);
			mCamera.setParameters(parameters);
			Camera.Size s=parameters.getPreviewSize();
			pW=s.width;
			pH=s.height;
			sender.sendParams(pW,pH,(int)(320 * 240 * 1.5));
		}
    }
	
	public void Initialize()
	{
		mHolder=getHolder();
		mHolder.addCallback(this);
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);		
	}

	public void Pause()
	{
		if(isUpstreaming)
			isPaused=true;
		
	}
	
	public void Resume()
	{
		if(isUpstreaming)
			isPaused=false;
		
	}
	

	public void surfaceCreated(SurfaceHolder holder)
	{
		if (mCamera != null)
		{
			
			mCamera.setPreviewCallback(new PreviewCallback()
			{
				public void onPreviewFrame(byte[] data, Camera c)
				{
					Log.i("Preview [UPSTREAM]","Data length: " + data.length);
					if(isIpSet && !isPaused)
					{
						sender.sendByteArray(data);
					}
					
				}
			});
			try
			{
				mCamera.setPreviewDisplay(holder);
			}
			catch(Exception ex)
			{
			
			}
	
		}
	}
	
	
	public void releaseAll()
	{
		if(mCamera!=null)
		{
			mCamera.stopPreview();
			mCamera.setPreviewCallback(null);
			mCamera.release();
			mCamera = null;
			Log.i("Camera","released...........");
		}
	}
	
	public void surfaceDestroyed(SurfaceHolder holder) 
	{
		// empty stub
	}
	
	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) 
	{
		// empty stub
	}
}

class DataSender implements Runnable
{
	
	public int DataPort=8210;
	public int ParamPort=8211;
	
	String IP="";
	Socket dataSocket;
	Socket paramSocket;
	OutputStream dataOutStream;
	
	final String TAG="DataSender [UPSTREAM]";
	
	boolean autoFlush=false;
	public boolean isParamSent=false;
	
	public DataSender(String ipAddress)
	{
		IP=ipAddress;
		try
		{
			Log.i(TAG, "Creating connection to " + IP + " on " + DataPort + "...");
			dataSocket=new Socket(InetAddress.getByName(IP),DataPort);
			Log.i(TAG,"Connected! Getting output stream...");
			dataOutStream=dataSocket.getOutputStream();
			Log.i(TAG,"Done.");
		}
		catch(Exception ex)
		{
			Log.e(TAG,"Exception: " + ex.toString());
		}
	}
	
	@Override
	public void run()
	{
		//empty stub
	}
	
	public void autoFlushStream(boolean af)
	{
		autoFlush=af;
	}
	
	public boolean sendByteArray(byte[] data)
	{
		try
		{
			dataOutStream.write(data);
			
			if(autoFlush)
			{
				dataOutStream.flush();
			}
			
			return true;
		}
		catch(Exception ex)
		{
			Log.e(TAG,"Exception in write data to stream...\n" + ex.toString());
			return false;
		}
	}
	
	public boolean sendParams(int width,int height,int dataLength)
	{
		try
		{
			Log.i(TAG,"Sending parameters...");
			paramSocket=new Socket(InetAddress.getByName(IP),ParamPort);
			OutputStream paramOutputStream=paramSocket.getOutputStream();
			
			String params=width + ";" + height + ";" + getLocalIpAddress() + ";" + ParamPort + ";" + dataLength;
			
			paramOutputStream.write(params.getBytes());
			paramOutputStream.flush();
			
			paramOutputStream.close();
			paramSocket.close();
			Log.i(TAG,"Parameters sent.");
			isParamSent=true;
			return true;
		}
		catch(Exception ex)
		{
			Log.e(TAG,"Exception in sending params...\n" + ex.toString());
			isParamSent=false;;
			return false;
		}
		
	}
	
	public String getLocalIpAddress() 
	{
		try 
		{
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) 
			{
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) 
				{
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress()) 
					{
						return inetAddress.getHostAddress().toString();
					}
				}
			}
		} 
		catch (Exception ex) 
		{
			Log.e(TAG, "Exception in getting local ip...\n" + ex.toString());
		}
		
		return null;
	}
}