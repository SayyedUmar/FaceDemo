package com.fuelware.app.facedemo;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class APIClient {


    private static Retrofit retrofit = null;


    private static Retrofit getClient(String url) {

        OkHttpClient.Builder okHttpBuilder = new OkHttpClient.Builder()
//                .cache(new Cache(new File(App.getContext().getCacheDir(), "http"), 1024 * 1024 * 10))
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS);


        return new Retrofit.Builder()
                .client(okHttpBuilder.build()).baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    public static APIInterface getApiService(String url) {
        return getClient(url).create(APIInterface.class);
    }

    public static APIInterface getApiService() {
        return getApiService(Const.ROOT_URL);
    }
}
