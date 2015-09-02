package com.thefriendlybronto.fireplace;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by thefriendlybronto on 9/1/15.
 */
public class BeaconAttachmentFragment extends Fragment {

    private String accountName;
    private String beaconName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle b = this.getArguments();
        this.accountName = b.getString(Constants.BUNDLE_ACCOUNTNAME);
        this.beaconName = b.getParcelable(Constants.BUNDLE_BEACON);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.beacon_attachments, container, false);
        return rootView;
    }
}
