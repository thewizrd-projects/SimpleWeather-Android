package com.thewizrd.shared_resources.utils;

import android.content.Context;

import com.thewizrd.shared_resources.R;
import com.thewizrd.shared_resources.SimpleLibrary;

public class WeatherException extends Exception {
    private final ErrorStatus errorStatus;
    private final Context context;

    public WeatherException(ErrorStatus errorStatus) {
        this.errorStatus = errorStatus;
        this.context = SimpleLibrary.getInstance().getApp().getAppContext();
    }

    public ErrorStatus getErrorStatus() {
        return errorStatus;
    }

    @Override
    public String getMessage() {
        String errorMsg;

        switch (errorStatus) {
            case NOWEATHER:
                errorMsg = context.getString(R.string.werror_noweather);
                break;
            case NETWORKERROR:
                errorMsg = context.getString(R.string.werror_networkerror);
                break;
            case INVALIDAPIKEY:
                errorMsg = context.getString(R.string.werror_invalidkey);
                break;
            case QUERYNOTFOUND:
                errorMsg = context.getString(R.string.werror_querynotfound);
                break;
            case UNKNOWN:
            default:
                errorMsg = context.getString(R.string.werror_unknown);
                break;
        }

        return errorMsg;
    }
}
