package com.fuelware.app.facedemo;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface APIInterface {

    @Multipart
    @POST("/api")
    Call<ResponseBody> uploadSnapImage(@Part MultipartBody.Part file1,
                                       @Part MultipartBody.Part file2
    );


    @Multipart
    @POST("/api/test")
    Call<ResponseBody> uploadSnapImage(@Part MultipartBody.Part file1
    );
}
