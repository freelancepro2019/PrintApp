package com.app.printapp.uis;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Picture;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.app.printapp.R;
import com.app.printapp.databinding.ActivityMainBinding;
import com.app.printapp.printer.PrintPicture;
import com.zj.wfsdk.WifiCommunication;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private WifiCommunication wfComm = null;
    private RThread rThread = null;
    private static final int WFPRINTER_REVMSG = 0x06;
    private boolean isWifiConnected = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        initView();
    }

    private void initView() {
        wfComm = new WifiCommunication(mHandler);
        binding.btnCalc.setOnClickListener(view -> {
            double num1 = Double.parseDouble(binding.edtN1.getText().toString());
            double num2 = Double.parseDouble(binding.edtN2.getText().toString());
            double sub = num1+num2;
            binding.tvResult.setText(sub+"");
            String strAddressIp = binding.edtIpAddress.getText().toString();
            if (!strAddressIp.isEmpty()){
                wfComm.initSocket(strAddressIp,9100);

            }
            if (isWifiConnected){
                binding.btnCalc.setVisibility(View.GONE);
                binding.edtIpAddress.setVisibility(View.GONE);
                print();
            }

            


        });
    }

    private void print() {
        Bitmap bitmap = getBitMap();
        PrintPicture.POS_PrintBMP(bitmap,PrintPicture.PAPER_WIDTH1,0);

    }

    private Bitmap getBitMap(){
        Bitmap bitmap;
        binding.flData.setDrawingCacheEnabled(true);
        bitmap = Bitmap.createBitmap(binding.flData.getDrawingCache());
        binding.flData.setDrawingCacheEnabled(false);
        return bitmap;
    }

    @SuppressLint("HandlerLeak") private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case WifiCommunication.WFPRINTER_CONNECTED:
                    Toast.makeText(getApplicationContext(), "Connect the WIFI-printer successful",
                            Toast.LENGTH_SHORT).show();


                    rThread = new RThread();
                    rThread.start();
                    break;
                case WifiCommunication.WFPRINTER_DISCONNECTED:
                    Toast.makeText(getApplicationContext(), "Disconnect the WIFI-printer successful",
                            Toast.LENGTH_SHORT).show();
                    rThread.interrupt();
                    break;
                case WifiCommunication.SEND_FAILED:
                    Toast.makeText(getApplicationContext(), "Send Data Failed,please reconnect",
                            Toast.LENGTH_SHORT).show();
                    rThread.interrupt();
                    break;
                case WifiCommunication.WFPRINTER_CONNECTEDERR:
                    Toast.makeText(getApplicationContext(), "Connect the WIFI-printer error",
                            Toast.LENGTH_SHORT).show();
                    break;
                case WFPRINTER_REVMSG:
                    byte revData = (byte)Integer.parseInt(msg.obj.toString());
                    if(((revData >> 6) & 0x01) == 0x01)
                        Toast.makeText(getApplicationContext(), "The printer has no paper",Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    };


    class RThread extends Thread {
        @Override
        public void run() {
            try {
                Message msg = new Message();
                int revData;
                while(true)
                {
                    revData = wfComm.revByte();               //非阻塞单个字节接收数据，如需改成非阻塞接收字符串请参考手册
                    if(revData != -1){

                        msg = mHandler.obtainMessage(WFPRINTER_REVMSG);
                        msg.obj = revData;
                        mHandler.sendMessage(msg);
                    }
                    Thread.sleep(20);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                Log.d("wifi调试","退出线程");
            }
        }
    }


}