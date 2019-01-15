package com.frohenk.amazfit;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.firebase.analytics.FirebaseAnalytics;

import me.dozen.dpreference.DPreference;

public class ControlsFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private RadioGroup radioGroup;
    private DPreference preferences;
    private SeekBar delaySeekBar;
    private TextView delayTextView;
    private AdView adView;
    private Switch longPressSwitch;
    private Switch googleAssistantSwitch;
    private FirebaseAnalytics firebaseAnalytics;


    public ControlsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ControlsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ControlsFragment newInstance(String param1, String param2) {
        ControlsFragment fragment = new ControlsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            firebaseAnalytics = FirebaseAnalytics.getInstance(getActivity());
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_controls, container, false);
        radioGroup = view.findViewById(R.id.actionsRadioGroup);
        preferences = new DPreference(getActivity(), getString(R.string.preference_file_key));
        radioGroup.check(preferences.getPrefInt(getString(R.string.multiple_click_action), R.id.action2Pause3Next));

        firebaseAnalytics.setUserProperty("multi_click_action", ((RadioButton) view.findViewById( preferences.getPrefInt(getString(R.string.multiple_click_action), R.id.action2Pause3Next))).getText().toString());

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                preferences.setPrefInt(getString(R.string.multiple_click_action), checkedId);
                firebaseAnalytics.setUserProperty("multi_click_action", ((RadioButton) view.findViewById( preferences.getPrefInt(getString(R.string.multiple_click_action), R.id.action2Pause3Next))).getText().toString());
            }
        });
        delaySeekBar = view.findViewById(R.id.delaySeekBar);
        delayTextView = view.findViewById(R.id.delayTextView);
        delaySeekBar.setProgress(preferences.getPrefInt(getString(R.string.multiple_delay), 1));
        delayTextView.setText("" + (ConnectionService.DELAY_STEP * (1 + preferences.getPrefInt(getString(R.string.multiple_delay), 1))) + " ms");
        firebaseAnalytics.setUserProperty("multi_click_interval", String.valueOf((ConnectionService.DELAY_STEP * (1 + preferences.getPrefInt(getString(R.string.multiple_delay), 1)))));
        delaySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                preferences.setPrefInt(getString(R.string.multiple_delay), progress);
                delayTextView.setText("" + (ConnectionService.DELAY_STEP * (1 + progress)) + " ms");
                firebaseAnalytics.setUserProperty("multi_click_interval", String.valueOf((ConnectionService.DELAY_STEP * (1 + progress))));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        longPressSwitch = view.findViewById(R.id.longPressSwitch);
        longPressSwitch.setChecked(preferences.getPrefBoolean(getString(R.string.long_press_control), false));
        firebaseAnalytics.setUserProperty("long_press_volume", String.valueOf(preferences.getPrefBoolean(getString(R.string.long_press_control), false)));
        longPressSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                preferences.setPrefBoolean(getString(R.string.long_press_control), isChecked);
                firebaseAnalytics.setUserProperty("long_press_volume", String.valueOf(isChecked));
                if (isChecked) {
                    preferences.setPrefBoolean(getString(R.string.long_press_googass), false);
                    googleAssistantSwitch.setChecked(false);
                    firebaseAnalytics.setUserProperty("long_press_assistant", String.valueOf(false));
                }
            }
        });

        googleAssistantSwitch = view.findViewById(R.id.googleAssistantSwitch);
        googleAssistantSwitch.setChecked(preferences.getPrefBoolean(getString(R.string.long_press_googass), false));
        firebaseAnalytics.setUserProperty("long_press_assistant", String.valueOf(preferences.getPrefBoolean(getString(R.string.long_press_googass), false)));
        googleAssistantSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                preferences.setPrefBoolean(getString(R.string.long_press_googass), isChecked);
                firebaseAnalytics.setUserProperty("long_press_assistant", String.valueOf(isChecked));
                if (isChecked) {
                    preferences.setPrefBoolean(getString(R.string.long_press_control), false);
                    longPressSwitch.setChecked(false);
                    firebaseAnalytics.setUserProperty("long_press_volume", String.valueOf(false));
                }
            }
        });

        adView = view.findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice("EA6F5EDBBC1D0C4A6088CB46CD5D9FE1")
                .build();
        adView.loadAd(adRequest);
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(int i) {
                Log.i("Ads", "no AdMob ad :(");
            }
        });


        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
    }


    @Override
    public void onDetach() {
        super.onDetach();
    }


}
