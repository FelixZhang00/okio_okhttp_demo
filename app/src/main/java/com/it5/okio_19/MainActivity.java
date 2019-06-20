package com.it5.okio_19;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends Activity
{

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button button = (Button) findViewById(R.id.start);
        button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                startNet();
            }
        });
    }

    // avoid creating several instances, should be singleon
    OkHttpClient client = new OkHttpClient();

    Request request = new Request.Builder()
            //            .url("http://felixzhang00.github.io/")
            .url("http://59.37.96.63/")
            .build();

    public void startNet()
    {

//        OkHttpClient client = new OkHttpClient();
//        Request request = new Request.Builder()
//                //            .url("http://felixzhang00.github.io/")
//                .url("http://59.37.96.63/")
//                .build();

        client.newCall(request).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException
            {
                if (!response.isSuccessful())
                {
                    throw new IOException("Unexpected code " + response);
                }

                Headers responseHeaders = response.headers();
                for (int i = 0, size = responseHeaders.size(); i < size; i++)
                {
                    Log.d(TAG, "onResponse,header: " + (responseHeaders.name(i) + ": " + responseHeaders.value(i)));
                }

                Log.d(TAG, "onResponse,body: " + response.body().string());
            }
        });

    }

}
