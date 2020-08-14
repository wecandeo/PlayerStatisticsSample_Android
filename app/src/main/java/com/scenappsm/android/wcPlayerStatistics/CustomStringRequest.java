package com.scenappsm.android.wcPlayerStatistics;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.Nullable;

public class CustomStringRequest extends StringRequest {

    Context context;
    String userAgentInfo;

    public CustomStringRequest(Context context, int method, String url, Response.Listener<String> listener, @Nullable Response.ErrorListener errorListener) {
        super(method, url, listener, errorListener);
        this.context = context;
    }

    @Override
    protected Response<String> parseNetworkResponse(NetworkResponse response){
        try {
            String utf8String = new String(response.data, "UTF-8");
            return Response.success(utf8String, HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException e) {
            // log error
            return Response.error(new ParseError(e));
        } catch (Exception e) {
            // log error
            return Response.error(new ParseError(e));
        }
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        PackageInfo pi = null;
        try{
            pi = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            userAgentInfo = "(Android; Android " + Build.VERSION.RELEASE + ") App/" + pi.versionName;
        }catch(PackageManager.NameNotFoundException e){
            e.printStackTrace();
        }
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("User-agent", userAgentInfo);
        return headers;
    }
}
