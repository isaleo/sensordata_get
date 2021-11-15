import java.util.*;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class sensordatareceive {
    private static final String TAG = "sensordatareceive";

	public static float byte2float(byte[] b, int index) {    
		int l;                                             
		l = b[index + 0];                                  
		l &= 0xff;                                         
		l |= ((long) b[index + 1] << 8);                   
		l &= 0xffff;                                       
		l |= ((long) b[index + 2] << 16);                  
		l &= 0xffffff;                                     
		l |= ((long) b[index + 3] << 24);                  
		return Float.intBitsToFloat(l);                    
	}

	public static long bytes2long(byte[] b, int index) {    
		long l = 0;

		l |= (((b[index + 0] & 0x00000000000000ffl) << 0));
		l &= 0x00000000000000ffl;
		l |= (((b[index + 1] & 0x00000000000000ffl) << 8));                   
		l &= 0x000000000000ffffl;                             
		l |= (((b[index + 2] & 0x00000000000000ffl) << 16));                  
		l &= 0x0000000000ffffffl;                          
		l |= (((b[index + 3] & 0x00000000000000ffl) << 24));
		l &= 0x00000000ffffffffl;         
		l |= (((b[index + 4] & 0x00000000000000ffl) << 32));                   
		l &= 0x000000ffffffffffl;     
		l |= (((b[index + 5] & 0x00000000000000ffl) << 40));                  
		l &= 0x0000ffffffffffffl;  
		l |= (((b[index + 6] & 0x00000000000000ffl) << 48));
		l &= 0x00ffffffffffffffl;
		l |= (((b[index + 7] & 0x00000000000000ffl) << 56));
		l &= 0xffffffffffffffffl;

		return l;                    
	}

    public static long bytesToLong(byte[] input, int offset, boolean littleEndian) {
        ByteBuffer buffer = ByteBuffer.wrap(input,offset,4);
        if(littleEndian){
            buffer.order(ByteOrder.LITTLE_ENDIAN);
        }
		return buffer.getLong();
    }

    public static void main(String[] args) {
        boolean mForwardSuccess = false;
        String localPort = "8888";
        String serverPort = "9999";
        try {
            Runtime.getRuntime().exec("adb forward tcp:8888 tcp:9999");
            Process process = Runtime.getRuntime().exec("adb forward --list");
            DataInputStream dis = new DataInputStream(process.getInputStream());
            byte[] buf = new byte[8];
            int len = -1;
            StringBuilder sb = new StringBuilder();
            while ((len = dis.read(buf)) != -1) {
                String str = new String(buf, 0, len);
                sb.append(str);
            }
            String adbList = sb.toString().toString();
            System.out.println("adb forward list=" + adbList);
            String[] forwardArr = adbList.split("\n");
            for (String forward : forwardArr) {
                System.out.println("forward=" + forward);
                if (forward.contains(localPort) && forward.contains(serverPort)) {
                    mForwardSuccess = true;
                }
            }

            if (!mForwardSuccess) return;

            Socket mSocket = new Socket("127.0.0.1", 8888);
            byte[] buffer = new byte[256];
            DataInputStream diss = new DataInputStream(mSocket.getInputStream());

            new Thread(){
                @Override
                public void run() {
                    super.run();
					while(true){
						try {
							float acc_x = 0, acc_y = 0, acc_z = 0;
							long ts = 0;
							diss.read(buffer);
							acc_x = byte2float(buffer, 0);
							acc_y = byte2float(buffer, 4);
							acc_z = byte2float(buffer, 8);
							//System.out.format("buffer: %#x%x%x%x%x%x%x%x\n",buffer[19], buffer[18], buffer[17], buffer[16], buffer[15], buffer[14], buffer[13], buffer[12]);
							ts = bytes2long(buffer, 12);
							
							Thread.sleep(1);

							System.out.println("acc_x:"+ acc_x +" acc_y:"+ acc_y +" acc_z:"+ acc_z + " ts:" + ts);
						} catch (InterruptedException e) {
							System.out.println("InterruptedException: " + e);
						} catch (IOException e) {
							System.out.println("IOException: " + e);
						}
					}
                }
            }.start();
        } catch (IOException e) {
            System.out.println("IOException " + e);
        }
    }
}
