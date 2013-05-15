package net.majorkernelpanic.spydroid.ui;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.ws4d.coap.Constants;
import org.ws4d.coap.connection.BasicCoapChannelManager;
import org.ws4d.coap.interfaces.CoapChannelManager;
import org.ws4d.coap.interfaces.CoapClientChannel;
import org.ws4d.coap.interfaces.CoapRequest;
import org.ws4d.coap.interfaces.CoapResponse;
import org.ws4d.coap.messages.CoapRequestCode;

import android.util.Base64;
import android.util.Log;

public class CoapClientCustom implements org.ws4d.coap.interfaces.CoapClient {
    private static final String SERVER_ADDRESS = "82.130.13.181";
    private static final int PORT = Constants.COAP_DEFAULT_PORT;
    CoapChannelManager channelManager = BasicCoapChannelManager.getInstance();
    CoapClientChannel clientChannel = null;

   
    public void post(String uri, String url){
        try {
            Log.i("URL obtained", url);
            clientChannel = channelManager.connect(this, InetAddress.getByName(SERVER_ADDRESS), PORT);
            CoapRequest coapRequest = clientChannel.createRequest(true, CoapRequestCode.POST);
            coapRequest.setUriPath(uri);
            //byte[] decoded = Base64.decode(url, Base64.DEFAULT );      
            coapRequest.setPayload(url);
            
        //    coapRequest.setPayload("SOmme".getBytes());
            clientChannel.sendMessage(coapRequest);
        } catch (UnknownHostException e) {
        //    Log.e("Error: ", "Uknown host");
            e.printStackTrace();
        }
       
    }
//    public void postMessage(String postuUrl, String android_id) {
//        Log.d("Coap", "got here");
//        try {
//            clientChannel = channelManager.connect(this,
//                    InetAddress.getByName(SERVER_ADDRESS), PORT);
//            CoapRequest coapRequest = clientChannel.createRequest(true,
//                    CoapRequestCode.POST);
//            coapRequest.setUriPath("/devices/" + android_id + "/camera");
//            coapRequest.setPayload(postuUrl);
//            clientChannel.sendMessage(coapRequest);
//            Log.d("Coap", "Message sent");
//        } catch (UnknownHostException e) {
//            // TODO Auto-generated catch block
//            Log.d("Coap", "Host unknown");
//        }
//    }

    @Override
    public void onResponse(CoapClientChannel channel, CoapResponse response) {
        // TODO Auto-generated method stub
        Log.i("Coap", "response came");
    }

    @Override
    public void onConnectionFailed(CoapClientChannel channel,
            boolean notReachable, boolean resetByServer) {
        // TODO Auto-generated method stub
        Log.i("Coap", "connection failed");

    }
}


