package org.ahlab.wavesenselib.data;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import android.os.AsyncTask;

public class DataTransferTask extends AsyncTask<Void, Void, Boolean>{
	
	String javaServerHostAddress = "10.21.113.94";
	public static final int JAVA_SERVER_HOST_PORT = 3000;
	
	int[] dataarray;

	public DataTransferTask(String ipAddress){
		if(!ipAddress.equals(""))
			this.javaServerHostAddress = ipAddress;
	}
	
	public void setDatatoTransfer(int[] dataarray){
		this.dataarray = dataarray;
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		Socket socket = null;    
		try {
			SocketAddress sockaddr = new InetSocketAddress(javaServerHostAddress, JAVA_SERVER_HOST_PORT);
			socket = new Socket();
			int timeoutMs = 1000;   // 1 seconds
			socket.connect(sockaddr, timeoutMs);
		    
			DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
			out.writeInt(dataarray.length);
			for (int intdata : dataarray) out.writeInt(intdata);
			out.flush();
			out.close();
	        socket.close();
		} catch (IOException e) {
			//e.printStackTrace();
			return false;
		}		
		return true;
	}
	
	@Override
    protected void onPostExecute(final Boolean success){
		if(success)
			System.out.println("Data sent sucessfully");
	}

}
