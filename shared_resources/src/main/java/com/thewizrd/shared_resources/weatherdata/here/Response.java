package com.thewizrd.shared_resources.weatherdata.here;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Response {

    @SerializedName("metaInfo")
    private MetaInfo metaInfo;

    @SerializedName("view")
    private List<ViewItem> view;

    public void setMetaInfo(MetaInfo metaInfo) {
        this.metaInfo = metaInfo;
    }

    public MetaInfo getMetaInfo() {
        return metaInfo;
    }

    public void setView(List<ViewItem> view) {
        this.view = view;
    }

    public List<ViewItem> getView() {
        return view;
    }
}