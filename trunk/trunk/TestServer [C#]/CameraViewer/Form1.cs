using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Windows.Forms;
using System.Net.Sockets;
using System.Net;
using System.IO;
using System.Drawing.Drawing2D;
using System.Drawing.Imaging;
using System.Runtime.InteropServices;


namespace CameraViewer
{
    public partial class Form1 : Form
    {
        IPAddress ipAddress = IPAddress.Any;
        TcpListener listener;// = new TcpListener(ipAddress, 8210);
        string lpIP = "";
        int lpPort = 8234;
        int rfact = 178;
        int gfact = -178;
        int bfact = 215;
        int rShift = 0;
        int gShift = 0;
        int bShift = 0;
        int frames = 0;
        int w;
        int h;
        int dataLength = 0;
        bool isSizeRecieved = false;
        bool isSizeSent = false;
        bool decoding = true;
        List<Bitmap> buffer = new List<Bitmap>();
        Bitmap bmp=null;
        Image.GetThumbnailImageAbort myCallback;// = new Image.GetThumbnailImageAbort(CallBack);
        Image Gimg;// = pictureBox1.Image.GetThumbnailImage(100, 100, myCallback, IntPtr.Zero);

        System.Net.IPAddress remoteIPAddress;
        System.Net.IPEndPoint remoteEndPoint;
        Socket m_socClient;
        NetworkStream ns = null;

        int playPos = 0;
        int bufferPos = 0;

        public Form1()
        {
            InitializeComponent();
            CheckForIllegalCrossThreadCalls = false;
            myCallback = new Image.GetThumbnailImageAbort(CallBack);

            listener = new TcpListener(ipAddress, 8210);
        }

        private void Form1_Load(object sender, EventArgs e)
        {
            listener.Start();
            label1.Text = "Service started...";
            
            w = Convert.ToInt32(textBox1.Text);
            h = Convert.ToInt32(textBox2.Text);
            backgroundWorker1.RunWorkerAsync();
            backgroundWorker2.RunWorkerAsync();
            backgroundWorker3.RunWorkerAsync();
            label5.Text = "Factors:\nR:" + rfact.ToString() + "\nG:" + gfact.ToString() + "\nB:" + bfact.ToString();
        }
        
        public Bitmap decodeYUV420SP( byte[] yuv420sp, int width, int height) 
        {
    	    int frameSize = width * height;
            Bitmap bmp = new Bitmap(width, height);
            
    	    for (int j = 0, yp = 0; j < height; j++)
            {
    		    int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
    		    for (int i = 0; i < width; i++, yp++)
                {
    			    int y = (0xff & ((int) yuv420sp[yp])) - 16;
    			    if (y < 0) y = 0;
    			    if ((i & 1) == 0) {
    				    v = (0xff & yuv420sp[uvp++]) - 128;
    				    u = (0xff & yuv420sp[uvp++]) - 128;
    			}
    			
    			
                    int C = y - 16;
                    int D = u - 128;
                    int E = v - 128;
                
                    int R = (int)(( 298 * C + 409 * E + 128) >> 8);
                    int G = (int)(( 298 * C - 100 * D - 208 * E + 128) >> 8);
                    int B = (int)((298 * C + 516 * D + 128) >> 8);

                    //B = B > 255 ? 255 : 0;
                    //G = G > 255 ? 255 : 0;
                    //R = R > 255 ? 255 : 0;

                    R = R + rfact;
                    G = G + gfact;
                    B = B + bfact;

                    if (B > 255)
                    {
                        B = 255;
                    }
                    else if (B < 0)
                    {
                        B = 0;
                    }

                    if (G > 255)
                    {
                        G = 255;
                    }
                    else if (G < 0)
                    {
                        G = 0;
                    }

                    if (R > 255)
                    {
                        R = 255;
                    }
                    else if (R < 0)
                    {
                        R = 0;
                    }
                    
                    bmp.SetPixel(i, j, Color.FromArgb(255, R, G, B));
    		}
    	}
            return bmp; 
    }

        private void backgroundWorker1_DoWork(object sender, DoWorkEventArgs e)
        {
            int i = 0;

            try
            {
                while (true)
                {
                    Socket s = listener.AcceptSocket();
                    NetworkStream ns = new NetworkStream(s);
                    if (isSizeRecieved)
                    {
                        int d;
                        byte[] b = new byte[dataLength];
                        while ((d = ns.ReadByte()) >= 0)
                        {
                            b[i] = (byte)d;
                            i++;
                            if (i >= b.Length)
                            {
                                pictureBox1.Image = decodeYUV420SP(b, w, h);
                                Gimg = pictureBox1.Image.GetThumbnailImage(100, 100, myCallback, IntPtr.Zero);
                                //loopBack(img);
                                //System.Threading.Thread.Sleep(5);
                                b = new byte[dataLength];
                                i = 0;  
                            }
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                MessageBox.Show(ex.Message + ": here : " + i.ToString() + " : " + dataLength + " : " + ex.StackTrace);
            }
        }
        
        public bool CallBack()
        {
            return false;
        }
        
        private void backgroundWorker2_DoWork(object sender, DoWorkEventArgs e)
        {

            TcpListener l = new TcpListener(ipAddress, 8211);
            l.Start();
            while (true)
            {
                Socket s = l.AcceptSocket();
                NetworkStream ns = new NetworkStream(s);
                int d;
                List<byte> bl = new List<byte>();
                while ((d = ns.ReadByte()) >= 0)
                {
                    bl.Add((byte)d);
                }
                byte[] b = new byte[bl.Count];
                for (int i = 0; i < b.Length; i++)
                {
                    b[i] = bl[i];
                }
                //int j = s.Receive(b);
                System.Text.Encoding enc = System.Text.Encoding.ASCII;
                string myString = enc.GetString(b);
                string[] sz = myString.Split(';');
                w = Convert.ToInt32(sz[0]);
                h = Convert.ToInt32(sz[1]);
                lpIP = sz[2];
                lpPort = Convert.ToInt32(sz[3]);
                dataLength = Convert.ToInt32(sz[4]);
                //buffer = new byte[75, dataLength];
                isSizeRecieved = true;
                label11.Text = myString;
                MessageBox.Show(lpIP + " " + lpPort.ToString() + " " + dataLength.ToString());
            }
        }

       
        void LoopBack(NetworkStream ns,Image img)
        {
            
            MemoryStream ms = new MemoryStream();
            //pictureBox2.Image = img;
            img.Save(ms, System.Drawing.Imaging.ImageFormat.Bmp);
            
            byte[] byData = ms.ToArray();
            ns.Write(byData, 0, byData.Length);
            ns.Flush();
            
            listBox1.Items.Add("data send.... " + byData.Length.ToString());
        }

        private void backgroundWorker3_DoWork(object sender, DoWorkEventArgs e)
        {
            while (true)
            {
                loopBack(Gimg);
            }
        }

        bool connectToLoopBack()
        {
            try
            {
                remoteIPAddress = System.Net.IPAddress.Parse("192.168.2.3");//lpIP);
                remoteEndPoint = new System.Net.IPEndPoint(remoteIPAddress, 8234);//lpPort);
                m_socClient = new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp);
                m_socClient.Connect(remoteEndPoint);
                ns = new NetworkStream(m_socClient);
                return true;
            }
            catch (Exception ex)
            {
                listBox1.Items.Clear();
                listBox1.Items.Add(ex.Message);
                return false;
            }
        }

        void loopBack(Image img)
        {
            if (!isSizeSent)
            {
                isSizeSent = connectToLoopBack();
            }
            else
            {
                try
                {
                    LoopBack(ns, img);
                }
                catch (Exception ex)
                {
                    listBox1.Items.Clear();
                    listBox1.Items.Add(ex.Message);
                }
            }
         }

     }
}

