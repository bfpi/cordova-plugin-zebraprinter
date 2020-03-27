package de.bfpi.cordova.zebraprinter;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.pm.PackageManager;
import android.os.Looper;
import android.util.Log;
import com.zebra.sdk.comm.BluetoothConnection;
import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.printer.discovery.BluetoothDiscoverer;
import com.zebra.sdk.printer.discovery.DiscoveredPrinter;
import com.zebra.sdk.printer.discovery.DiscoveredPrinterBluetooth;
import com.zebra.sdk.printer.discovery.DiscoveryHandler;
import com.zebra.sdk.printer.SGD;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Zebraprinter extends CordovaPlugin {
  private static final String ACCESS_COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
  private static final String LOG_TAG = "Zebraprinter";
  private static final int SEARCH_REQ_CODE = 0;

  private CallbackContext callbackContext;
  private JSONArray args;
  private CordovaWebView webView;

  @Override
  public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);
    Log.d(LOG_TAG, "Plugin created");
    this.webView = webView;
  }

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
    this.args = args;
    this.callbackContext = callbackContext;
    if (action.equals("print")) {
      this.print();
      return true;
    }
    else if (action.equals("discover")) {
      this.discover();
      return true;
    }
    return false;
  }

  private void discover() {
    if (hasPermission()) {
      discoverWithPermission();
    }
    else {
      requestPermissionAndDiscover();
    }
  }

  private void print() {
    final CallbackContext callbackContext = this.callbackContext;
    if (!hasPermission()) {
      logAndCallCallbackError("Permission denied, check settings.");
      return;
    }
    String macAddress;
    String textToPrint;
    try {
      macAddress = this.args.getString(0);
      textToPrint = this.args.getString(1);
    }
    catch(JSONException e) {
      logAndCallCallbackError("Exception: "+ e.getMessage(), e);
      return;
    }
    cordova.getThreadPool().execute(new Runnable() {
      public void run() {
        try {
          Connection conn = new BluetoothConnection(macAddress);
          if (Looper.myLooper() == null) {
            Looper.prepare();
          }
          conn.open();
          Log.d(LOG_TAG, "Connection opened, setting printer language to line_print");
          SGD.SET("device.languages", "line_print", conn);
          Log.d(LOG_TAG, "Setting printer language finished");

          byte[] request = textToPrint.getBytes(Charset.forName("windows-1252"));
          String requestString = new String(request);

          Log.d(LOG_TAG, "Printer (" + macAddress + ") Request (" + requestString.length() + ")");
          Log.d(LOG_TAG, "Start sending to printer .... then wait");
          byte[] response = conn.sendAndWaitForResponse(request, 2000, 2000, null);
          Log.d(LOG_TAG, "Printer response received: " + response.length);

          if (response != null && response.length > 0) {
            String responseString = new String(response);
            Log.d(LOG_TAG, "Printer (" + macAddress + ") Response (" + responseString.length() + "): " + responseString);
          }

          //conn.write(textToPrint.getBytes(Charset.forName("windows-1252")));
          //Thread.sleep(500);

          conn.close();
          callbackContext.success();
        } catch (Exception e) {
          logAndCallCallbackError(e.getMessage(), e);
        }
        finally {
          Looper.myLooper().quit();
        }
      }
    });
  }

  private boolean hasPermission() {
    return cordova.hasPermission(ACCESS_COARSE_LOCATION);
  }

  private void requestPermissionAndDiscover() {
    cordova.requestPermission(this, SEARCH_REQ_CODE, ACCESS_COARSE_LOCATION);
  }

  public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults)
    throws JSONException {
    for(int r:grantResults) {
      if(r == PackageManager.PERMISSION_DENIED) {
        Log.d(LOG_TAG, "Permission denied.");
        this.callbackContext.error("Permission denied.");
        return;
      }
    }
    discoverWithPermission();
  }

  private void discoverWithPermission() {
    final CallbackContext callbackContext = this.callbackContext;
    cordova.getThreadPool().execute(new Runnable() {
      public void run() {
        if (Looper.myLooper() == null) {
          Looper.prepare();
        }
        try {
          BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
          if (bluetoothAdapter == null) {
            logAndCallCallbackError("No Bluetooth adapter found.");
          }
          else if (!bluetoothAdapter.isEnabled()) {
            logAndCallCallbackError("Bluetooth is disabled.");
          }
          else {
            Log.d(LOG_TAG, "Searching for printers...");
            JSONArray printer = new JSONArray();
            BluetoothDiscoverer.findPrinters(cordova.getActivity().getApplicationContext(),
              new DiscoveryHandler() {
                public void discoveryError(String message) {
                  Log.e(LOG_TAG, "An error occurred while searching for printers. Message: " + message);
                  callbackContext.success();
                }

                public void discoveryFinished() {
                  Log.d(LOG_TAG, "Finished searching for printers...");
                  callbackContext.success(printer);
                }

                public void foundPrinter(final DiscoveredPrinter p) {
                  Log.d(LOG_TAG, "Printer found: " + p.address);
                  printer.put(p.address);
                }
              });
          }
        } catch (ConnectionException e) {
          logAndCallCallbackError("Connection exception: " + e.getMessage(), e);
        } finally {
          Looper.myLooper().quit();
        }
      }
    });
  }

  private void logAndCallCallbackError(String message) {
    Log.e(LOG_TAG, message);
    callbackContext.error(message);
  }

  private void logAndCallCallbackError(String message, Throwable exception) {
    Log.e(LOG_TAG, message, exception);
    callbackContext.error(message);
  }
}
