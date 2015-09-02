package com.thefriendlybronto.fireplace;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by thefriendlybronto on 9/1/15.
 */
public class MerchantBeaconArrayAdapter extends ArrayAdapter<String> {

    public MerchantBeaconArrayAdapter(Context activity, int resource, List<String> beacons) {
        super(activity, resource, beacons);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final String beacon = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.merchant_beacon_list_item, parent, false);
        }
        TextView textView = (TextView) convertView.findViewById(R.id.merchantBeaconId);
        textView.setText(beacon);
        return convertView;
    }
}
