package com.thewizrd.shared_resources.locationdata.locationiq;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class AutoCompleteQuery {

    @SerializedName("osm_id")
    private String osmId;

    @SerializedName("licence")
    private String licence;

    @SerializedName("boundingbox")
    private List<String> boundingbox;

    @SerializedName("address")
    private Address address;

    @SerializedName("display_address")
    private String displayAddress;

    @SerializedName("lon")
    private String lon;

    @SerializedName("type")
    private String type;

    @SerializedName("display_name")
    private String displayName;

    @SerializedName("display_place")
    private String displayPlace;

    @SerializedName("osm_type")
    private String osmType;

    @SerializedName("class")
    private String jsonMemberClass;

    @SerializedName("place_id")
    private String placeId;

    @SerializedName("lat")
    private String lat;

    public void setOsmId(String osmId) {
        this.osmId = osmId;
    }

    public String getOsmId() {
        return osmId;
    }

    public void setLicence(String licence) {
        this.licence = licence;
    }

    public String getLicence() {
        return licence;
    }

    public void setBoundingbox(List<String> boundingbox) {
        this.boundingbox = boundingbox;
    }

    public List<String> getBoundingbox() {
        return boundingbox;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public Address getAddress() {
        return address;
    }

    public void setDisplayAddress(String displayAddress) {
        this.displayAddress = displayAddress;
    }

    public String getDisplayAddress() {
        return displayAddress;
    }

    public void setLon(String lon) {
        this.lon = lon;
    }

    public String getLon() {
        return lon;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayPlace(String displayPlace) {
        this.displayPlace = displayPlace;
    }

    public String getDisplayPlace() {
        return displayPlace;
    }

    public void setOsmType(String osmType) {
        this.osmType = osmType;
    }

    public String getOsmType() {
        return osmType;
    }

    public void setJsonMemberClass(String jsonMemberClass) {
        this.jsonMemberClass = jsonMemberClass;
    }

    public String getJsonMemberClass() {
        return jsonMemberClass;
    }

    public void setPlaceId(String placeId) {
        this.placeId = placeId;
    }

    public String getPlaceId() {
        return placeId;
    }

    public void setLat(String lat) {
        this.lat = lat;
    }

    public String getLat() {
        return lat;
    }
}