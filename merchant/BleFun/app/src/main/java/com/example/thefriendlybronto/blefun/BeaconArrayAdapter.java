package com.example.thefriendlybronto.blefun;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.List;

/**
 * Created by thefriendlybronto on 8/20/15.
 */
public class BeaconArrayAdapter extends ArrayAdapter<Beacon> {
    public BeaconArrayAdapter(Context context, int resource, List<Beacon> objects) {
        super(context, resource, objects);
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final Beacon beacon = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.beacon_list_item, parent, false);
        }
        return convertView;
    }
}
