package com.thefriendlybronto.fireplace;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by thefriendlybronto on 8/25/15.
 */
public class BeaconArrayAdapter extends ArrayAdapter<Beacon> {

    public BeaconArrayAdapter(Context activity, int resource, List<Beacon> beacons) {
        super(activity, resource, beacons);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final Beacon beacon = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.beacon_list_item, parent, false);
        }
        TextView textView = (TextView) convertView.findViewById(R.id.BeaconName);
        textView.setText(beacon.getHexId());
        TextView statusTextView = (TextView) convertView.findViewById(R.id.beaconStatus);
        statusTextView.setText(beacon.getStatus().name());
        return convertView;
    }
}
