package com.thefriendlybronto.fireplace;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by thefriendlybronto on 8/24/15.
 */
public class Beacon implements Parcelable {
    private static final String TAG = Beacon.class.getSimpleName();

    private byte[] id;
    private BeaconType type;
    private BeaconStatus status;
    private int rssi;
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();
    private String placeId = null;
    private Double latitude = null;
    private Double longitude = null;
    private BeaconStability expectedStability = BeaconStability.UNKNOWN;
    private String description = null;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(type.name());
        dest.writeByteArray(id);
        dest.writeString(status.name());
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
        if (expectedStability != BeaconStability.UNKNOWN) {
            dest.writeInt(1);
            dest.writeString(expectedStability.name());
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

    public enum BeaconType {
        EDDYSTONE;
    }

    public enum BeaconStatus {
        UNREGISTERED,
        UNKNOWN,
        UNAUTHORIZED,
        ACTIVE,
        DECOMISSIONED,
        INACTIVE;
    }

    public enum BeaconStability {
        STABILITY_UNSPECIFIED,
        STABLE,
        PORTABLE,
        MOBILE,
        ROVING,
        UNKNOWN
    }

    public Beacon(BeaconType type, byte[] id, BeaconStatus status, int rssi) {
        this.type = type;
        this.id = id;
        this.status = status;
        this.rssi = rssi;
    }

    public Beacon(JSONObject response) {
        Log.d(TAG, response.toString());
        try {
            JSONObject json = response.getJSONObject("advertisedId");
            String jsonType = json.getString("type");
            switch (jsonType) {
                case "EDDYSTONE":
                    type = BeaconType.EDDYSTONE;
                    break;
                default:
                    Log.d(TAG, "dont' know type");
            }
            id = Utils.base64Decode(json.getString("id"));
        } catch (JSONException e) {
            // NOP
        }

        try {
            String jsonStatus = response.getString("status");
            switch (jsonStatus) {
                case "STATUS_UNSPECIFIED":
                    status = BeaconStatus.UNKNOWN;
                    break;
                case    "ACTIVE":
                    status = BeaconStatus.ACTIVE;
                    break;
                case "DECOMMISSIONED":
                    status = BeaconStatus.DECOMISSIONED;
                    break;
                case "INACTIVE":
                    status = BeaconStatus.INACTIVE;
                    break;
                default:
                    status = BeaconStatus.UNKNOWN;
            }
        } catch (JSONException e) {
            status = BeaconStatus.UNKNOWN;
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
            expectedStability = Enum.valueOf(BeaconStability.class, response.getString("expectedStability"));
        } catch (JSONException e) {
            // NOP
        }

        try {
            description = response.getString("description");
        } catch (JSONException e) {
            // NOP
        }

    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        JSONObject advertisedId = new JSONObject()
                .put("type", type)
                .put("id", Utils.base64Encode(id));
        json.put("advertisedId", advertisedId);
        switch (status) {
            case ACTIVE:
            case INACTIVE:
            case DECOMISSIONED:
                json.put("status", status.name());
                break;
            default:
                // don't put
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
        if (expectedStability != null && !expectedStability.equals(BeaconStability.UNKNOWN)) {
            json.put("expectedStability", expectedStability);
        }
        if (description != null) {
            json.put("description", description);
        }
        // TODO: beacon properties
        return json;
    }

    public String getBeaconName() {
        return String.format("beacons/3!%s", getHexId());
    }

    public byte[] getId() {
        return id;
    }

    public BeaconStatus getStatus() {
        return status;
    }

    public BeaconType getType() {
        return type;
    }

    public String getHexId() {
        return toHexString(id);
    }

    public int getRssi() {
        return rssi;
    }

    /**
     * Set the status of the beacon.
     * @param status
     */
    public void setStatus(BeaconStatus status) {this.status = status;}

    private  String toHexString(byte[] bytes) {
        char[] chars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int c = bytes[i] & 0xFF;
            chars[i * 2] = HEX[c >>> 4];
            chars[i * 2 + 1] = HEX[c & 0x0F];
        }
        return new String(chars).toLowerCase();
    }

    private Beacon(Parcel source) {
        switch (source.readString()) {
            case "EDDYSTONE":
                this.type = BeaconType.EDDYSTONE;
                break;
            default:
                Log.e(TAG, "unknow type");
        }
        int len = source.readInt();
        id = new byte[len];
        source.readByteArray(id);
        this.status = Enum.valueOf(BeaconStatus.class, source.readString());
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
            expectedStability = Enum.valueOf(BeaconStability.class, source.readString());
        }
        if (source.readInt() == 1) {
            description = source.readString();
        }
    }

    public static final Parcelable.Creator<Beacon> CREATOR = new Parcelable.Creator<Beacon>() {

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
