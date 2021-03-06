package com.ca.waspHRM;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import com.npe_inc.wasp.hrm_collector.HRM_Collector;
import com.npe_inc.wasp.hrm_collector.WaspHeartRateMonitor;

public class WaspHRMCollectorBinding {
    /** Messenger for communicating with service. */
    Messenger mService = null;
    /** Flag indicating whether we have called bind on the service. */
    boolean mIsBound;
    /** Some text view we are using to show state information. */

    com.ca.waspHRM.WaspHRMCollectorMessageCallbackHandler messageCallbackHandler;

    public String getVersion() {

        return "Attached. Version: " + HRM_Collector.getVersionString();
    }

    /**
     * Handler of incoming messages from service.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            WaspHeartRateMonitor whrm;
            switch (msg.what) {
            case HRM_Collector.MSG_UPDATE_HRM:
                whrm = (WaspHeartRateMonitor) msg.obj;
                Log.d("Test WASP Collector", whrm.toString());
                messageCallbackHandler.HeartRateMonitorUpdate(whrm);
                break;
            case HRM_Collector.MSG_HRM_CONNECTION_LOST:
                whrm = (WaspHeartRateMonitor) msg.obj;
                Log.d("Test WASP Collector", "Connection lost to " + whrm.getDeviceNumber());
                messageCallbackHandler.HeartRateMonitorConnectionLost(whrm);
                break;
            default:
                super.handleMessage(msg);
            }
        }
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service. We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.
            mService = new Messenger(service);
            // We want to monitor the service for as long as we are
            // connected to it.
            try {
                Message msg = Message.obtain(null, HRM_Collector.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);
            } catch (RemoteException e) {
                // In this case the service has crashed before we could even
                // do anything with it; we can count on soon being
                // disconnected (and then reconnected if it can be restarted)
                // so there is no need to do anything here.
            }

            // As part of the sample, tell the user what happened.
            Log.d("onServiceConnected", "Connected to service");
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
            // As part of the sample, tell the user what happened.
            Log.d("onServiceDisconnected", "Disconnected from service");
        }
    };

    void doBindService(Context context, com.ca.waspHRM.WaspHRMCollectorMessageCallbackHandler callbackHandler) {
        messageCallbackHandler = callbackHandler;
        // Establish a connection with the service. We use an explicit
        // class name because there is no reason to be able to let other
        // applications replace our component.
        context.bindService(new Intent(context, HRM_Collector.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    void doUnbindService(Context context) {
        if (mIsBound) {
            // If we have received the service, and hence registered with
            // it, then now is the time to unregister.
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, HRM_Collector.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service
                    // has crashed.
                }
            }

            // Detach our existing connection.
            context.unbindService(mConnection);
            mIsBound = false;
        }
    }

}