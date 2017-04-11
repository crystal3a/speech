package com.besta.speechdemo;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.gotev.speech.GoogleVoiceTypingDisabledException;
import com.gotev.speech.Speech;
import com.gotev.speech.SpeechDelegate;
import com.gotev.speech.SpeechRecognitionNotAvailable;
import com.gotev.speech.SpeechUtil;
import com.gotev.speech.TextToSpeechCallback;
import com.gotev.speech.ui.SpeechProgressView;
import com.tbruyelle.rxpermissions.RxPermissions;

import net.gotev.toyproject.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static android.view.View.VISIBLE;
import static net.gotev.toyproject.R.id.speak;


public class MainActivity extends AppCompatActivity implements SpeechDelegate {

    private ImageButton button;
    private Button restart;
    private TextView text;
    private TextView textcontent;
    private SpeechProgressView progress;
    private QueryRequest qrreq;
    private LinearLayout linearLayout;
    private int langflag=1;
    private int hintcount=0;
    private float fPeak;
    private boolean bBegin;
    private long lCheckTime;
    private long lTimeout = 4000;
    private boolean bStatus;
    //private AudioManager audioManager;
    //private int current_volume;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


     }

    @Override
    protected void onStart() {
        super.onStart();
        bStatus = true;
        bBegin = false;
        fPeak = -999; //Only to be sure it's under ambient RmsDb.

        //audioManager=(AudioManager)getSystemService(Context.AUDIO_SERVICE);
        //current_volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        qrreq = new QueryRequest();

        linearLayout = (LinearLayout) findViewById(R.id.linearLayout);

        button = (ImageButton) findViewById(R.id.button);
        button.setOnClickListener(view -> onButtonClick());
        button.setVisibility(View.GONE);

        text = (TextView) findViewById(R.id.text);
        text.setMovementMethod(new ScrollingMovementMethod());
        textcontent = (TextView) findViewById(R.id.textcontent);
        textcontent.setMovementMethod(new ScrollingMovementMethod());
        textcontent.setVisibility(View.GONE);

        restart = (Button) findViewById(speak);
        restart.setOnClickListener(view -> onRestartClick());
        restart.setVisibility(View.GONE);

        progress = (SpeechProgressView) findViewById(R.id.progress);

        int[] colors = {
                ContextCompat.getColor(this, android.R.color.holo_blue_dark),
                ContextCompat.getColor(this, android.R.color.holo_red_dark),
                ContextCompat.getColor(this, android.R.color.holo_purple),
                ContextCompat.getColor(this, android.R.color.holo_orange_dark),
                ContextCompat.getColor(this, android.R.color.holo_green_dark)
        };
        progress.setColors(colors);

        Log.d("StartRecognition", "Auto Start");

        if (isConnected()) {
            Log.d("NetworkConnection", "Network Connected.");
            _onButtonClick();
            //StartRecognition();
            //button.performClick();
        }else{
            Log.d("NetworkConnection", "No network connection available.");
            //告訴使用者網路無法使用
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setMessage("無法使用")
                    .setTitle("請開啟網路連線功能")
                    .setCancelable(false)
                    .setPositiveButton("確定",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,int id) {
                                    finish(); // exit program
                                }
                            });
            dialog.show();
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("StartRecognition", "onPause");
        bStatus = false;
        Speech.getInstance().stopListening();
        Speech.getInstance().stopTextToSpeech();
        //Speech.getInstance().unregisterDelegate();
        //android.os.Process.killProcess(android.os.Process.myPid());
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("StartRecognition", "onStop");
        bStatus = false;
        Speech.getInstance().stopListening();
        Speech.getInstance().stopTextToSpeech();
        //Speech.getInstance().unregisterDelegate();
        //android.os.Process.killProcess(android.os.Process.myPid());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("onDestroy", "onDestroy");
        bStatus = false;
        Speech.getInstance().stopListening();
        Speech.getInstance().stopTextToSpeech();
        Speech.getInstance().unregisterDelegate();
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    private boolean isConnected(){
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        }
        return false;
    }

    private void StartRecognition()
    {
        button.setVisibility(VISIBLE);
        textcontent.setVisibility(View.GONE);
        text.setText("");

        String sayString="";
        if(hintcount%5==1)
        {
            sayString = "請唸出你要查詢的單字";
        }
        Log.i("speech", String.format("hintcount = %d(%d)",hintcount,hintcount%5 ));

        Speech.getInstance().setLocale(Locale.TRADITIONAL_CHINESE);
        Speech.getInstance().say(sayString, new TextToSpeechCallback() {
            @Override
            public void onStart() {
                Log.i("speech", "speech started");
            }

            @Override
            public void onCompleted() {
                Log.i("speech", "speech completed");
                _onButtonClick();
            }

            @Override
            public void onError() {
                Log.i("speech", "speech error");
            }
        });

    }

    private void _onButtonClick() {

        if(!bStatus)
        {
         return;
        }

        bBegin = false;
        fPeak = -999; //Only to be sure it's under ambient RmsDb.
        restart.setVisibility(View.GONE);
        //current_volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        //audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
        Log.d("onButtonClick","_onButtonClick");
        if (Speech.getInstance().isListening())
        {
            Log.d("onButtonClick","stopListening");
            Speech.getInstance().stopListening();
        }
        else
        {
            RxPermissions.getInstance(this)
                    .request(Manifest.permission.RECORD_AUDIO)
                    .subscribe(granted -> {
                        if (granted) { // Always true pre-M
                            onRecordAudioPermissionGranted();
                        } else {
                            Toast.makeText(MainActivity.this, R.string.permission_required, Toast.LENGTH_LONG);
                        }
                    });
        }

    }

    private void onButtonClick() {
        Log.d("onButtonClick","onButtonClick");
        if (Speech.getInstance().isListening()) {
            Speech.getInstance().stopListening();

        } else {
            RxPermissions.getInstance(this)
                    .request(Manifest.permission.RECORD_AUDIO)
                    .subscribe(granted -> {
                        if (granted) { // Always true pre-M
                            onRecordAudioPermissionGranted();
                        } else {
                            Toast.makeText(MainActivity.this, R.string.permission_required, Toast.LENGTH_LONG);
                        }
                    });
        }
    }



    private void onRecordAudioPermissionGranted() {
        button.setVisibility(View.GONE);
        linearLayout.setVisibility(VISIBLE);

        try {
            Speech.getInstance().stopTextToSpeech();
            Speech.getInstance().startListening(progress, MainActivity.this);
            if (Speech.getInstance().isListening())
            {
                Log.d("PermissionGranted", "isListening");
            }
            else
            {
                Log.d("PermissionGranted", " not isListening");
            }

            //Speech.getInstance().setGetPartialResults(false);

        } catch (SpeechRecognitionNotAvailable exc) {
            showSpeechNotSupportedDialog();

        } catch (GoogleVoiceTypingDisabledException exc) {
            showEnableGoogleVoiceTyping();
        }
    }

    private void onRestartClick() {
        if (isConnected()) {
            Log.d("NetworkConnection", "Network Connected.");
            hintcount = 0;
            _onButtonClick();
            //button.performClick();
        }else{
            Log.d("NetworkConnection", "No network connection available.");
            //告訴使用者網路無法使用
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setMessage("無法使用")
                    .setTitle("請開啟網路連線功能")
                    .setCancelable(false)
                    .setPositiveButton("確定",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,int id) {
                                    finish(); // exit program
                                }
                            });
            dialog.show();
        }
    }


    @Override
    public void onSpeechBeginning()
    {
        bBegin = true;
        Log.i("onSpeechBeginning", "onSpeechBeginning");
    }

    @Override
    public void onStartOfSpeech()
    {

    }



    //http://howtoprogram.eu/question/android-speech-recognition-google-speech-recognition-timeout,8715
    @Override
    public void onSpeechRmsChanged(float value) {

        if(bBegin==true) {
            Log.d(getClass().getSimpleName(), "Speech recognition rms is now " + value + "dB");
        }

        if(bBegin) {
            if (value > fPeak) {
                fPeak = value;
                lCheckTime = System.currentTimeMillis();
            }
            if (System.currentTimeMillis() > lCheckTime + lTimeout) {
                Log.d("onSpeechRmsChanged", "DONE");
                Speech.getInstance().stopListening();
                Speech.getInstance().stopTextToSpeech();
                StartRecognition();
            }
        }
    }

    @Override
    public void onSpeechError(int code)
    {
        Log.d("onSpeechError","Error code = " + Integer.toString(code));
    }

    @Override
    public void onSpeechEnd()
    {
        Log.d("RECOGNIZER", "onEndOfSpeech");
        /*
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Do something after 5s = 5000ms

                Log.d("RECOGNIZER","done");
                StartRecognition();
            }
        }, 1000);*/
    }

    @Override
    public void onSpeechResult(String result)
    {
        linearLayout.setVisibility(View.GONE);
        //audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, current_volume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
        if (result.isEmpty())
        {
            Log.d("onSpeechResult","result is Empty");
            hintcount++;
            StartRecognition();

        }
        else {
            Log.d("onSpeechResult","result is　" + result);
            hintcount = 0;
            if (result.substring(0, 1).matches("[\\u4E00-\\u9FA5]+")) {
                langflag = 2;
                result = result.replace(" ","");
            }
            else
            {
                langflag = 1;
                result = PinYinFilter(result);
            }
            restart.setVisibility(View.VISIBLE);
            text.setText(result);
            saytext(result);
        }
    }

    @Override
    public void onSpeechPartialResults(List<String> results) {
        lCheckTime = System.currentTimeMillis();
        text.setText("");
        for (String partial : results) {
            text.append(partial + " ");
        }
    }

    private void showSpeechNotSupportedDialog() {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        SpeechUtil.redirectUserToGoogleAppOnPlayStore(MainActivity.this);
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.speech_not_available)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, dialogClickListener)
                .setNegativeButton(R.string.no, dialogClickListener)
                .show();
    }

    private void showEnableGoogleVoiceTyping() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.enable_google_voice_typing)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // do nothing
                    }
                })
                .show();
    }



    private void saytext(String result)
    {
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);
        String url;
        if (langflag==1)
        {
            url = qrreq.QueryJson(result,3);//VOC0012
        }
        else
        {
            url = qrreq.QueryJson(result,4);//TTD0005
        }
        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>()
        {
            @Override
            public void onResponse(String response) {
                Log.i("playsound", "onResponse");
                String [] resp = response.split("\\n");
                if(resp.length>0)//length of array
                {
                    Log.d("play url",resp[0]);
                    playsound(result,resp[0]);
                    //String [] resp = response.split("\\n");
                    //playsound(result,resp[0]);
                }
                else
                {
                    loadtextview(result);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.d("saytext", "Error: " + error.getMessage());
            }
        });
        // Add the request to the RequestQueue.
        queue.add(stringRequest);



    }

    private void playsound(String result, String url)
    {
        MediaPlayer mp = new MediaPlayer();
        try
        {
            mp.setDataSource(getApplicationContext(), Uri.parse(url));
            mp.prepare();
            mp.start();
            mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                public void onCompletion(MediaPlayer mp) {
                    Log.i("Completion Listener","onCompletion");
                    mp.stop();
                    mp.release();
                    loadtextview(result);
                }
            });
        }
        catch(Exception e)
        {
            Log.d("playsoundException",e.getMessage());
            loadtextview(result);
        }
    }


    private void loadtextview(String result)
    {
        textcontent.setVisibility(VISIBLE);
        String jsonurl = qrreq.QueryJson(result, langflag);
        // Tag used to cancel the request
        String tag_json_obj = "json_obj_req";
        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.GET,jsonurl, null,
                new Response.Listener<JSONObject>() {

                @Override
                public void onResponse(JSONObject response) {


                    Log.d(tag_json_obj, response.toString());
                    String res = jsonparser(response);
                    textcontent.setText(res);

                    //"查無此字"
                    if(res=="")
                    {

                        if(langflag==1 && text.getText().toString().contains(" "))
                        {
                            // TODO Auto-generated method stub
                            MainActivity.this.runOnUiThread(new Runnable() {

                                @Override
                                public void run()
                                {
                                    String res = text.getText().toString().replace(" ","");
                                    text.setText(res);
                                    loadtextview2(res);
                                }
                            });

                        }
                        else
                        {
                            Speech.getInstance().setLocale(Locale.TRADITIONAL_CHINESE);
                            Speech.getInstance().say("查無此字", new TextToSpeechCallback() {
                                @Override
                                public void onStart() {
                                    Log.i("speech", "speech started");
                                }

                                @Override
                                public void onCompleted() {
                                    Log.i("speech", "speech completed");
                                    try {
                                        Thread.sleep(1000);
                                    } catch (InterruptedException e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                    }
                                    StartRecognition();
                                }

                                @Override
                                public void onError() {
                                    Log.i("speech", "speech error");
                                }
                            });
                        }

                    }
                    else
                    {
                        if(langflag==1 || langflag==3)
                        {
                            //DIC0001
                            Speech.getInstance().setLocale(Locale.TRADITIONAL_CHINESE);
                            res=StringFilter(res);
                        }
                        else if(langflag==2 || langflag==4)
                        {
                            //DIC0002
                            Speech.getInstance().setLocale(Locale.ENGLISH);
                        }



                        Speech.getInstance().say(res, new TextToSpeechCallback() {
                            @Override
                            public void onStart() {
                                Log.i("speech", "speech started");
                            }

                            @Override
                            public void onCompleted() {
                                Log.i("speech", "speech completed");
                                try {
                                    Thread.sleep(3000);
                                } catch (InterruptedException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                                StartRecognition();
                            }
                            @Override
                            public void onError() {
                                Log.i("speech", "speech error");
                            }
                        });
                    }
                }
            }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.d(tag_json_obj, "Error: {0}" , error.getMessage());
            }
        });
        Volley.newRequestQueue(getApplicationContext()).add(jsonObjReq);
    }

    private void loadtextview2(String result)
    {
        textcontent.setVisibility(VISIBLE);
        //webView_out.setVisibility(View.GONE);
        String jsonurl = qrreq.QueryJson(result, langflag);
        // Tag used to cancel the request
        String tag_json_obj = "json_obj_req";
        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.GET,jsonurl, null,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(tag_json_obj, response.toString());
                        String res = jsonparser(response);
                        textcontent.setText(res);

                        //"查無此字"
                        if(res=="")
                        {
                            Speech.getInstance().setLocale(Locale.TRADITIONAL_CHINESE);
                            Speech.getInstance().say("查無此字", new TextToSpeechCallback() {
                                @Override
                                public void onStart() {
                                    Log.i("speech", "speech started");
                                }

                                @Override
                                public void onCompleted() {
                                    Log.i("speech", "speech completed");
                                    try {
                                        Thread.sleep(1000);
                                    } catch (InterruptedException e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                    }
                                    StartRecognition();
                                }

                                @Override
                                public void onError() {
                                    Log.i("speech", "speech error");
                                }
                            });
                        }
                        else
                        {
                            if(langflag==1 || langflag==3)
                            {
                                //DIC0001
                                Speech.getInstance().setLocale(Locale.TRADITIONAL_CHINESE);
                                res=StringFilter(res);
                            }
                            else if(langflag==2 || langflag==4)
                            {
                                //DIC0002
                                Speech.getInstance().setLocale(Locale.ENGLISH);
                            }



                            Speech.getInstance().say(res, new TextToSpeechCallback() {
                                @Override
                                public void onStart() {
                                    Log.i("speech", "speech started");
                                }

                                @Override
                                public void onCompleted() {
                                    Log.i("speech", "speech completed");
                                    try {
                                        Thread.sleep(3000);
                                    } catch (InterruptedException e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                    }
                                    StartRecognition();
                                }
                                @Override
                                public void onError() {
                                    Log.i("speech", "speech error");
                                }
                            });
                        }
                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.d(tag_json_obj, "Error: " + error.getMessage());
            }
        });
        Volley.newRequestQueue(getApplicationContext()).add(jsonObjReq);
    }


    private String jsonparser(JSONObject response)
    {
        String qr_result="";
        String qrword = text.getText().toString();
        //qr_result = qrword + "\n 解釋 \n";
        Map<String, String> interpretation_map = new HashMap<>();
        try{
            Object entry_temp = response.getJSONObject("root").get("entry");
            Log.d("Entry",qrword+"取到entry");
            Object fedword_xml_temp;
            //JSONArray fedword_xml;
            if(entry_temp instanceof JSONArray){
                JSONArray entry = (JSONArray)entry_temp;
                fedword_xml_temp = entry.getJSONObject(0).get("fedword-xml"); //有複數entry只取第一個
            }
            else {
                JSONObject entry = (JSONObject)entry_temp;
                fedword_xml_temp = entry.get("fedword-xml");
            }
            Log.d("fedword-xml",qrword+"取到fedword-xml");
            if(fedword_xml_temp instanceof JSONArray){
                JSONArray fedword_xml = (JSONArray) fedword_xml_temp;
                for (int i = 0; i < fedword_xml.length(); i++){
                    JSONObject f = fedword_xml.getJSONObject(i);
                    String fedword_xml_att = f.getString("@att");
                    if(fedword_xml_att.indexOf("interpretation-ref-") != -1) {
                        String def_tmp = "";
                        Object interpretation_ref_temp = f.getJSONObject("row").get("row-value");
                        if(interpretation_ref_temp instanceof JSONObject){
                            def_tmp = (f.getJSONObject("row").getJSONObject("row-value").isNull("#text")) ?
                                    f.getJSONObject("row").getJSONObject("row-value").getString("#cdata-section") :
                                    f.getJSONObject("row").getJSONObject("row-value").getString("#text");
                            interpretation_map.put(fedword_xml_att, def_tmp);
                        } else{
                            JSONArray row = f.getJSONObject("row").getJSONArray("row-value");
                            for (int j = 0; j < row.length(); j++) {
                                if (row.getJSONObject(j).getString("@att").equals("def")) {
                                    def_tmp = (row.getJSONObject(j).isNull("#text")) ?
                                            row.getJSONObject(j).getString("#cdata-section") :
                                            row.getJSONObject(j).getString("#text");
                                    interpretation_map.put(fedword_xml_att, def_tmp);
                                }
                            }

                        }
                        Log.d("解釋內容", def_tmp);
                    }
                }
                for (int i = 0; i < fedword_xml.length(); i++){
                    JSONObject f = fedword_xml.getJSONObject(i);
                    String fedword_xml_att = f.getString("@att");
                    if(fedword_xml_att.equals("interpretation")){
                        Object row_temp = f.get("row");
                        if(row_temp instanceof JSONArray){
                            JSONArray row = (JSONArray) row_temp;
                            for (int j = 0;j < row.length(); j++){
                                JSONObject row_iter = row.getJSONObject(j);
                                Object row_value_tmp = row_iter.get("row-value");
                                if(row_value_tmp instanceof JSONObject){
                                    JSONObject row_value = (JSONObject) row_value_tmp;
                                    String interpretation_id = row_value.getString("#text");
                                    qr_result += (interpretation_map.get(interpretation_id) + "\n");
                                }
                                else{
                                    JSONArray row_value = (JSONArray) row_value_tmp;
                                    for (int k = 0; k < row_value.length(); k++){
                                        JSONObject row_value_iter = row_value.getJSONObject(k);
                                        String row_value_iter_att = row_value_iter.getString("@att");
                                        if (row_value_iter_att.equals("seg")){
                                            qr_result += (row_value_iter.getString("#text") + "\n");
                                        }
                                        else if(row_value_iter_att.equals("id")){
                                            String interpretation_id = row_value_iter.getString("#text");
                                            qr_result += (interpretation_map.get(interpretation_id)+ "\n");
                                        }
                                    }

                                }
                            }
                        }
                        else{
                            JSONObject row = (JSONObject) row_temp;
                            Object row_value_tmp = row.get("row-value");
                            if (row_value_tmp instanceof JSONObject){
                                JSONObject row_value = (JSONObject) row_value_tmp;
                                String interpretation_id = row_value.getString("#text");
                                qr_result += (interpretation_map.get(interpretation_id)+ "\n");
                            }
                            else{
                                JSONArray row_value = (JSONArray) row_value_tmp;
                                for(int k = 0; k < row_value.length();k++){
                                    JSONObject row_value_iter = row_value.getJSONObject(k);
                                    String row_value_iter_att = row_value_iter.getString("@att");
                                    if (row_value_iter_att.equals("seg")){
                                        qr_result += (row_value_iter.getString("#text") + "\n");
                                    }
                                    else if(row_value_iter_att.equals("id")){
                                        String interpretation_id = row_value_iter.getString("#text");
                                        qr_result += (interpretation_map.get(interpretation_id)+ "\n");
                                    }
                                }
                            }
                        }

                    }
                }
            }
            /*
            else{
                JSONObject fedword_xml = (JSONObject) fedword_xml_temp;
                qr_result += fedword_xml.getJSONObject("row").getJSONObject("row-value").getString("#text");
            }
            */


        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        return qr_result;

    }


    public   static  String StringFilter(String   str) throws PatternSyntaxException {
        // 只允许字母和数字
        // String   regEx  =  "[^a-zA-Z0-9]";
        // 清除掉所有特殊字符
        String regEx="[`~!@#$%^&*()+=|{}':'\\[\\].<>/~！@#￥%……&*（）——+|{}【】‘：”“’a-zA-Z]";
        Pattern   p   =   Pattern.compile(regEx);
        Matcher m   =   p.matcher(str);
        return   m.replaceAll("").trim();
    }

    public  String PinYinFilter(String   str) throws PatternSyntaxException {
        // ex:a p p l e
        String regEx="^([a-zA-Z]\\s)+[a-zA-Z]$";
        Pattern   p   =   Pattern.compile(regEx);
        Matcher m   =   p.matcher(str.trim());
        Log.d("PinYinFilter",str);
        if (m.find()) {
            return str.replace(" ","");
        }
        else
            return str;
    }

}
