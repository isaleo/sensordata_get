package com.example.sensordatasend;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;


import java.util.*;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "SensorDataSend";
    ServerSocket mServerSocket;
    Socket mClientSocket;
    SensorManager sensorManager;
    private ReadWriteLock rwl = new ReentrantReadWriteLock();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        SendSensorData();
    }

    public static byte[] float2byte(float f) {
        // 把float转换为byte[]
        int fbit = Float.floatToIntBits(f);

        byte[] b = new byte[4];
        for (int i = 0; i < 4; i++) {
            b[i] = (byte) (fbit >> (24 - i * 8));
        }

        // 翻转数组
        int len = b.length;
        // 建立一个与源数组元素类型相同的数组
        byte[] dest = new byte[len];
        // 为了防止修改源数组，将源数组拷贝一份副本
        System.arraycopy(b, 0, dest, 0, len);
        byte temp;
        // 将顺位第i个与倒数第i个交换
        for (int i = 0; i < len / 2; ++i) {
            temp = dest[i];
            dest[i] = dest[len - i - 1];
            dest[len - i - 1] = temp;
        }

        return dest;
    }

    public static byte[] long2byte(long num) {
        byte[] byteNum = new byte[8];
//        System.out.format("num: %#x\n", num);
        for (int ix = 0; ix < 8; ix++) {
            int offset = ix * 8;
            byteNum[ix] = (byte) ((num >> offset) & 0xff);
//            System.out.format("byteNum[%d]:%#x ", ix, byteNum[ix]);
        }
        System.out.format("\n");
        return byteNum;
    }

    public static long bytesToLong(byte[] input, int offset, boolean littleEndian) {
        ByteBuffer buffer = ByteBuffer.wrap(input,offset,8);
        if(littleEndian){            // ByteBuffer.order(ByteOrder) 方法指定字节序,即大小端模式(BIG_ENDIAN/LITTLE_ENDIAN)
            // ByteBuffer 默认为大端(BIG_ENDIAN)模式
            buffer.order(ByteOrder.LITTLE_ENDIAN);
        }
        return buffer.getLong();
    }

    private void SendSensorData() {
        Log.e(TAG, "SendSensorData enter");
        try {
            mServerSocket = new ServerSocket(9999);
            byte[] buffer = new byte[60];
            Arrays.fill(buffer, (byte) 0);

            Log.e(TAG, "Create socket");
            mClientSocket = mServerSocket.accept();

            Log.e(TAG, "Create output stream");
            DataOutputStream outputStream = new DataOutputStream(mClientSocket.getOutputStream());

            Log.e(TAG, "Enumerating sensors");
            List<Sensor> sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL);
            Log.e(TAG, "Sensors:");
            for (Sensor sensor: sensorList ){
                Log.e(TAG, "\tType: " + sensor.getStringType());
            }

            class SensorEventListenerT implements SensorEventListener {
                @Override
                synchronized
                public void onSensorChanged(SensorEvent event) {
                    float acc_x = 0, acc_y = 0, acc_z = 0;
                    float gyro_x = 0, gyro_y = 0, gyro_z = 0;
                    float mag_x = 0, mag_y = 0, mag_z = 0;
                    long ts = 0;

                    rwl.writeLock().lock();
                    if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                        Arrays.fill(buffer, 0, 19, (byte) 0);
                        acc_x = event.values[0];
                        acc_y = event.values[1];
                        acc_z = event.values[2];
                        ts = event.timestamp;

                        System.out.println("acc_x="+acc_x+" acc_y="+acc_y+" acc_z="+acc_z+" ts="+ts);

                        System.arraycopy(float2byte(acc_x), 0, buffer, 0, 4);
                        System.arraycopy(float2byte(acc_y), 0, buffer, 4, 4);
                        System.arraycopy(float2byte(acc_z), 0, buffer, 8, 4);
                        System.arraycopy(long2byte(ts), 0, buffer, 12, 8);
//                        System.out.format("buffer1: %#x%x%x%x%x%x%x%x\n",buffer[19], buffer[18], buffer[17], buffer[16], buffer[15], buffer[14], buffer[13], buffer[12]);
                    } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                        Arrays.fill(buffer, 20, 39, (byte) 0);
                        gyro_x = event.values[0];
                        gyro_y = event.values[1];
                        gyro_z = event.values[2];
                        ts = event.timestamp;
                        System.out.println("gyro_x="+gyro_x+" gyro_y="+gyro_y+" gyro_z="+gyro_z+" ts="+ts);
                        System.arraycopy(float2byte(gyro_x), 0, buffer, 20, 4);
                        System.arraycopy(float2byte(gyro_y), 0, buffer, 24, 4);
                        System.arraycopy(float2byte(gyro_z), 0, buffer, 28, 4);
                        System.arraycopy(long2byte(ts), 0, buffer, 32, 8);
                    } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                        Arrays.fill(buffer, 40, 59, (byte) 0);
                        mag_x = event.values[0];
                        mag_y = event.values[1];
                        mag_z = event.values[2];
                        ts = event.timestamp;
                        System.out.println("mag_x="+mag_x+" mag_y="+mag_y+" mag_z="+mag_z+" ts"+ts);
                        System.arraycopy(float2byte(mag_x), 0, buffer, 40, 4);
                        System.arraycopy(float2byte(mag_y), 0, buffer, 44, 4);
                        System.arraycopy(float2byte(mag_z), 0, buffer, 48, 4);
                        System.arraycopy(long2byte(ts), 0, buffer, 52, 4);
                    }
                    rwl.writeLock().unlock();
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {

                }
            };

            SensorEventListenerT sensorEventListener = new SensorEventListenerT();

            sensorManager.registerListener(sensorEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                                    SensorManager.SENSOR_DELAY_FASTEST);

            new Thread() {
                @Override
                public void run() {
                    super.run();
//                    Log.e(TAG, "Send sensor data");
                    boolean run = true;
                    while (run) {
                        try {
                            rwl.readLock().lock();
//                            System.out.format("buffer2: %#x%x%x%x%x%x%x%x\n",buffer[19], buffer[18], buffer[17], buffer[16], buffer[15], buffer[14], buffer[13], buffer[12]);
                            outputStream.write(buffer, 0, 48);
                            rwl.readLock().unlock();
                            Thread.sleep(1);
                        } catch (IOException e) {
                            Log.e(TAG, "IOException: " + e);
                        } catch (InterruptedException e){
                            Log.e(TAG, "InterruptedException: " + e);
                        } finally {
//                            run = false;
                        }
                    }
                }
            }.start();
        }catch (IOException e) {
            Log.e(TAG, "Exception: " + e);
        }
    }
}