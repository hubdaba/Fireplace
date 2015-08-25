package com.example.thefriendlybronto.blefun;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by thefriendlybronto on 8/20/15.
 */
public class Beacon implements Parcelable {

    static final String STATUS_UNSPECIFIED = "STATUS_UNSPECIFIED";
    static final String STATUS_ACTIVE = "ACTIVE";
    static final String UNREGISTERED = "UNREGISTERED";
    static final String NOT_AUTHORIZED = "NOT_AUTHORIZED";
    static final String STABILITY_UNSPECIFIED = "STABILITY_UNSPECIFIED";

    String type;
    byte[] id;
    String status;
    String placeId;
    Double latitude;
    Double longitude;
    String expectedStability;
    String description;

    // Strength of beacon.
    int rssi;

    public Beacon(JSONObject response) {
        try {
            JSONObject json = response.getJSONObject("advertisedId");
            type = json.getString("type");
            id = Utils.base64Decode(json.getString("id"));
        } catch (JSONException e) {
            // NOP
        }

        try {
            status = response.getString("status");
        } catch (JSONException e) {
            status = STATUS_UNSPECIFIED;
        }

        try {
            placeId = response.getString("placeId");
        } catch (JSONException e) {
            // NOP
        }

        try {
            JSONObject latLngJson = response.getJSONObject("latLng");
            latitude = latLngJson.getDouble("latitude");
            longitude = latLngJson.getDouble("longitude");
        } catch (JSONException e) {
            latitude = null;
            longitude = null;
        }

        try {
            expectedStability = response.getString("expectedStability");
        } catch (JSONException e) {
            // NOP
        }

        try {
            description = response.getString("description");
        } catch (JSONException e) {
            // NOP
        }
    }

    public Beacon(String type, byte[] id, String status, int rssi) {
         this.type = type;
        this.id = id;
        this.status = status;
        this.rssi = rssi;
    }

    public String getHexId() {
        return Utils.toHexString(id);
    }

    public String getBeaconName() {
        return String.format("beacon/3!%s", getHexId());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        JSONObject advertisedId = new JSONObject()
                .put("type", type)
                .put("id", Utils.base64Encode(id));
        json.put("advertisedId", advertisedId);
        if (!status.equals(STATUS_UNSPECIFIED)) {
            json.put("status", status);
        }
        if (placeId != null) {
            json.put("placeId", placeId);
        }
        if (latitude != null && longitude != null) {
            JSONObject latLng = new JSONObject();
            latLng.put("latitude", latitude);
            latLng.put("longitude", longitude);
            json.put("latLng", latLng);
        }
        if (expectedStability != null && !expectedStability.equals(STABILITY_UNSPECIFIED)) {
            json.put("expectedStability", expectedStability);
        }
        if (description != null) {
            json.put("description", description);
        }
        // TODO: beacon properties
        return json;
    }

    private Beacon(Parcel source) {
        type = source.readString();
        int len = source.readInt();
        id = new byte[len];
        source.readByteArray(id);
        if (source.readInt() == 1) {
            placeId = source.readString();
        }
        if (source.readInt() == 1) {
            latitude = source.readDouble();
        }
        if (source.readInt() == 1) {
            longitude = source.readDouble();
        }
        if (source.readInt() == 1) {
            expectedStability = source.readString();
        }
        if (source.readInt() == 1) {
            description = source.readString();
        }

    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(type);
        dest.writeByteArray(id);
        dest.writeString(status);
        if (placeId != null) {
            dest.writeInt(1);
            dest.writeString(placeId);
        } else {
            dest.writeInt(0);
        }
        if (latitude != null) {
            dest.writeInt(1);
            dest.writeDouble(latitude);
        } else {
            dest.writeInt(0);
        }
        if (longitude != null) {
            dest.writeInt(1);
            dest.writeDouble(longitude);
        } else {
            dest.writeInt(0);
        }
        if (expectedStability != null) {
            dest.writeInt(1);
            dest.writeString(expectedStability);
        } else {
            dest.writeInt(0);
        }
        if (description != null) {
            dest.writeInt(1);
            dest.writeString(description);
        } else {
            dest.writeInt(0);
        }

    }

    public static final Parcelable.Creator<Beacon> Creator = new Parcelable.Creator<Beacon>() {

        @Override
        public Beacon createFromParcel(Parcel source) {
            return new Beacon(source);
        }

        @Override
        public Beacon[] newArray(int size) {
            return new Beacon[size];
        }
    };
}
