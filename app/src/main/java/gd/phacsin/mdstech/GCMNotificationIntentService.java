package gd.phacsin.mdstech;

/**
 * Created by SGD on 10/4/2015.
 */

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.scottyab.aescrypt.AESCrypt;
import com.snappydb.DB;
import com.snappydb.DBFactory;
import com.snappydb.SnappydbException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.security.GeneralSecurityException;

public class GCMNotificationIntentService extends IntentService {
    // Sets an ID for the notification, so it can be updated
    public static final int notifyID = 9001;
    NotificationCompat.Builder builder;
    boolean flag=false;
    RequestParams params = new RequestParams();
    String data;
    String file_id[];
    String password = "mdstech";
    JSONObject jsonObject;
    private String piece_id;

    public GCMNotificationIntentService() {
        super("GcmIntentService");
    }
    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) {
            if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR
                    .equals(messageType)) {
                sendNotification("Alert","Send error: " + extras.toString());
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED
                    .equals(messageType)) {
                sendNotification("Alert","Deleted messages on server: "
                        + extras.toString());
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE
                    .equals(messageType)) {
                //sendNotification(String.valueOf(extras.get(ApplicationConstants.MSG_KEY)));
            }
        }
        String json_string = String.valueOf(extras.get(ApplicationConstants.MSG_KEY));
        try {
            jsonObject = new JSONObject(json_string);
            Log.e("JSON",json_string);
            if (jsonObject.getString("type").equals("Request")) {
                try {
                    sendNotification("Data Request",json_string);
                    DB snappydb = DBFactory.open(getApplicationContext());
                    file_id=snappydb.findKeys("file_id");
                    String fileid=jsonObject.getString("file_id");
                    String pieceid=jsonObject.getString("piece_id");
                    for(int i=0;i<file_id.length;i++)
                        if(jsonObject.getString("file_id").equals(snappydb.get(file_id[i]))&&jsonObject.getString("piece_id").equals(snappydb.get("piece_id:"+i))) {
                            Log.e("File ID",file_id[i]);
                            /*
                            data = AESCrypt.decrypt(password, snappydb.get("data:" + i));
                            */
                            data=snappydb.get("data:" + i);
                            Log.d("Data", data);

                        }
                    registerInBackground(fileid,pieceid,data);
                } catch (SnappydbException e) {
                    Log.d("snappy", e.toString());
                }
            }
            // check the phone alive or not
            // checkphone.phph call all phones which are alive by  ending gcm notifications.
            else if(jsonObject.getString("type").equals("Checkphone")){
                sendNotification("Data Request",json_string);
                DB snappydb = DBFactory.open(getApplicationContext());
                int count = snappydb.countKeys("file_id");
                if(count<=20){
                    // function alive phones  sends imei and space in phone into alivephones.php and store in table phones
                    alivephones();
                }
                else {
                    sendNotification("Alert","No more insertion of data");
                }
            }
            // Used to insert data to phone. sendgcm.php  sends file id and filepiece id that want to be stored on phone,
            else if(jsonObject.getString("type").equals("Insert")) {
                sendNotification("Insert",json_string);
                /* this function used to fetch data from encrypted table. function sends the fileid and piece id to get data and insert_data.php fetch data
                from encrypted table and sent to phone db and it is stored */
                sendToServer();
            }
        } catch (JSONException e) {
            Log.e("JSON",e.toString());
        } catch (SnappydbException e) {
            e.printStackTrace();
        }

    }

    private void alivephones() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... pars) {
                AsyncHttpClient client = new AsyncHttpClient();
                try {
                    // url
                    String URL = "http://mdsnew.hol.es/php/alivephones.php";
                    // to get imei from phone
                    TelephonyManager telephonyManager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
                    // store imei on string IMEI
                    String IMEI = telephonyManager.getDeviceId();
                    // snappy db use
                    DB snappydb = DBFactory.open(getApplicationContext());
                    // find number of files
                    int count = snappydb.countKeys("file_id");
                    // append imei and count with url (url+imei+count)
                    String uri = Uri.parse(URL)
                            .buildUpon()
                            .appendQueryParameter("imei", IMEI)
                            .appendQueryParameter("count", String.valueOf(count))
                            .build().toString();
                    Log.d("URL", uri);
                    client.get(uri, params, new AsyncHttpResponseHandler() {
                        // When the response returned by REST has Http
                        // response code '200'
                        @Override
                        public void onSuccess(String response) {
                            // display result on android studio monitor
                            Log.d("key", "Success");
                            Log.d("op", response);
                            /*try {
                                JSONArray jsonAray = new JSONArray(response);
                                JSONObject jsonObject = jsonAray.getJSONObject(0);
                                String file_id = jsonObject.getString("fileid");
                                String piece_id = jsonObject.getString("pieceid");
                                String data = jsonObject.getString("data");
                                Log.d("op",data);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }*/
                        }

                        @Override
                        public void onFailure(int statusCode, Throwable error,
                                              String content) {

                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return "";
            }

            @Override
            protected void onPostExecute(String msg) {
            }
        }.execute(null, null, null);
    }


    private void sendToServer()
    {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... pars) {
                AsyncHttpClient client = new AsyncHttpClient();
                try {
                    // url
                    String URL = "http://mdsnew.hol.es/php/insert_data.php";
                    TelephonyManager telephonyManager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
                    String IMEI = telephonyManager.getDeviceId();
                    // append iemi,fileid,pieceid with url
                    String uri = Uri.parse(URL)
                            .buildUpon()
                            .appendQueryParameter("imei", IMEI)
                            .appendQueryParameter("fileid", jsonObject.getString("file_id"))
                            .appendQueryParameter("pieceid", jsonObject.getString("piece_id"))
                            .build().toString();
                    Log.d("URL", uri);
                    // response from request
                    client.get(uri, params, new AsyncHttpResponseHandler() {
                        // When the response returned by REST has Http
                        // response code '200'
                        @Override
                        public void onSuccess(String response) {
                            Log.d("key", "Success");
                            Log.d("op", response);
                            try {
                                // data is obtained from table encrypted using insert_data.php
                                //array is obtained
                                JSONArray jsonAray = new JSONArray(response);
                                // array jason object splitted into diffrent variables.
                                JSONObject jsonObject1 = jsonAray.getJSONObject(0);
                                JSONObject jsonObject2 = jsonAray.getJSONObject(0);
                                JSONObject jsonObject3 = jsonAray.getJSONObject(0);
                                // store the value from json object to difffrent strings
                                String fileids = jsonObject2.getString("fileid");
                                String pieceids = jsonObject3.getString("pieceid");
                                String data = jsonObject1.getString("data");

                                Log.d("op",data);
                                Log.d("op",fileids);
                                Log.d("op",pieceids);
                                DB snappydb = DBFactory.open(getApplicationContext());
                                int count = snappydb.countKeys("file_id");
                                //storing in snappydb
                                if (count == 0) {
                                    snappydb.put("file_id:0", fileids);
                                    snappydb.put("piece_id:0", pieceids);
                                    snappydb.put("data:0",data);
                                    Log.e("data is stored",data);
                                }
                                else if(count==4)
                                {
                                    sendNotification("Alert","Maximum Data Size Reached");
                                }
                                else {
                                    file_id = snappydb.findKeys("file_id");
                                    for(int i=0;i<file_id.length;i++)
                                        if(fileids.equals(snappydb.get(file_id[i]))&&pieceids.equals(snappydb.get("piece_id:"+i)))
                                        {
                                            flag=true;
                                            break;
                                        }
                                    if(!flag) {
                                        snappydb.put("file_id:" + String.valueOf(count), fileids);
                                        snappydb.put("piece_id:" + String.valueOf(count), pieceids);
                                        snappydb.put("data:" + String.valueOf(count), data);

                                    }
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            } catch (SnappydbException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(int statusCode, Throwable error,
                                              String content) {

                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return "";
            }

            @Override
            protected void onPostExecute(String msg) {
            }
        }.execute(null, null, null);
    }
 // fileid piece id and data passsed to this function
    private void registerInBackground(final String fileid, final String pieceid, final String data_file) {
        new AsyncTask<Void, Void, String>() {
            //storing arguments to another string
            String fileid1=fileid;
            String pieceid1=pieceid;
            String data1=data_file;
            @Override
            protected String doInBackground(Void... pars) {
                AsyncHttpClient client = new AsyncHttpClient();
                try {
                    SharedPreferences prefs = getSharedPreferences("UserDetails",
                            Context.MODE_PRIVATE);
                    Log.d("pref",prefs.getString("regID","")); //regid -gcmid (not using)
                    String URL="http://mdsnew.hol.es/php/send_data.php";
                    Log.e("data",data_file);  // can see the data in andoid monitor
                    TelephonyManager telephonyManager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
                    // store imei on string IMEI
                    String IMEI = telephonyManager.getDeviceId();
                    String uri = Uri.parse(URL)
                            .buildUpon()
                            .appendQueryParameter("imei", IMEI)
                            .appendQueryParameter("fileid",fileid1) // fileid passed as argument
                            .appendQueryParameter("pieceid", pieceid1)// piece id passed as argument
                            .appendQueryParameter("data", data1)// data id passed as argument
                            .build().toString();
                    Log.d("URL",uri); // link is shown in ansroid monitor
                    client.get(uri, params, new AsyncHttpResponseHandler() {
                        // When the response returned by REST has Http
                        // response code '200'
                        @Override
                        public void onSuccess(String response) {
                            Log.d("key", "Success");
                        }

                        @Override
                        public void onFailure(int statusCode, Throwable error,
                                              String content) {

                        }
                    });
                }catch (Exception e)
                {
                    e.printStackTrace();
                }
                return "";
            }

            @Override
            protected void onPostExecute(String msg) {
            }
        }.execute(null, null, null);
    }

    private void sendNotification(String title,String msg) {
        Intent resultIntent = new Intent(this, MainActivity.class);
        resultIntent.putExtra("msg", msg);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0,
                resultIntent, PendingIntent.FLAG_ONE_SHOT);

        NotificationCompat.Builder mNotifyBuilder;
        NotificationManager mNotificationManager;

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mNotifyBuilder = new NotificationCompat.Builder(this)
                .setContentTitle(title)
                .setContentText("You've received new message.")
                .setSmallIcon(R.drawable.ic_launcher);
        // Set pending intent
        mNotifyBuilder.setContentIntent(resultPendingIntent);

        // Set Vibrate, Sound and Light
        int defaults = 0;
        defaults = defaults | Notification.DEFAULT_LIGHTS;
        defaults = defaults | Notification.DEFAULT_VIBRATE;
        defaults = defaults | Notification.DEFAULT_SOUND;

        mNotifyBuilder.setDefaults(defaults);
        // Set the content for Notification
        mNotifyBuilder.setContentText(msg);
        // Set autocancel
        mNotifyBuilder.setAutoCancel(true);
        // Post a notification
        mNotificationManager.notify(notifyID, mNotifyBuilder.build());
    }

}