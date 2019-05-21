package com.frohenk.amazfit;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.greenrobot.eventbus.EventBus;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import me.dozen.dpreference.DPreference;

public class MainActivity extends AppCompatActivity implements PurchasesUpdatedListener {

    public static final String REMOVE_ADS_SKU = "remove_ads";
    public static final String REMOVE_ADS_PURCHASED = "remove_ads_purchased";
    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;
    private FloatingActionButton fab;
    private FirebaseAnalytics firebaseAnalytics;
    private BillingClient billingClient;
    private Menu menu;
    private MenuItem removeAdsMenuItem;
    private SkuDetails removeAdsSkuDetails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        firebaseAnalytics = FirebaseAnalytics.getInstance(this);
        MobileAds.initialize(this, "ca-app-pub-5911662140305016~6930012303");

        final DPreference preference = new DPreference(this, getString(R.string.preference_file_key));
        if (preference.getPrefString(getString(R.string.preferences_watch_address), "").isEmpty()) {
            Intent intent = new Intent(this, ChooseWatchActivity.class);
            startActivity(intent);
            finish();
        } else {
            Intent intent = new Intent(this, ConnectionService.class);
            startService(intent);
            if (preference.getPrefBoolean(REMOVE_ADS_PURCHASED, false))
                disableAds();
            billingClient = BillingClient.newBuilder(this).enablePendingPurchases().setListener(this).build();//TODO support pending stuff
            billingClient.startConnection(new BillingClientStateListener() {
                @Override
                public void onBillingSetupFinished(BillingResult billingResult) {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        Log.i("kek", "onBillingSetupFinished() response: " + billingResult.getResponseCode());
                        handleManagerAndUiReady();
                        List<Purchase> purchasesList = billingClient.queryPurchases(BillingClient.SkuType.INAPP).getPurchasesList();
                        for (Purchase purchase :
                                purchasesList) {
                            handlePurchase(purchase);
                        }
                    } else {
                        Log.w("kek", "onBillingSetupFinished() error code: " + billingResult.getResponseCode());
                    }
                }

                @Override
                public void onBillingServiceDisconnected() {
                    Log.w("kek", "onBillingServiceDisconnected()");
                }
            });
        }
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = findViewById(R.id.tabs);

        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager));
        fab = findViewById(R.id.fab);
        fab.hide();
        tabLayout.addOnTabSelectedListener(new TabLayout.BaseOnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() != 1)
                    fab.hide();
                else
                    fab.show();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        if (preference.getPrefInt(getString(R.string.num_uses), 0) >= 100 && preference.getPrefBoolean(getString(R.string.can_rate), true)) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle(getString(R.string.rate_app_question));
            builder.setNeutralButton(getString(R.string.rate_later), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    preference.setPrefInt(getString(R.string.num_uses), 0);
                    Bundle bundle = new Bundle();
                    bundle.putString("action", "later");
                    firebaseAnalytics.logEvent("rating_action", bundle);
                }
            });
            builder.setPositiveButton(getString(R.string.rate_rate), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    preference.setPrefBoolean(getString(R.string.can_rate), false);
                    Uri uri = Uri.parse("market://details?id=" + MainActivity.this.getPackageName());
                    Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
                    // To count with Play market backstack, After pressing back button,
                    // to taken back to our application, we need to add following flags to intent.
                    goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                            Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                            Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                    Bundle bundle = new Bundle();
                    bundle.putString("action", "rate");
                    firebaseAnalytics.logEvent("rating_action", bundle);
                    try {
                        startActivity(goToMarket);
                    } catch (ActivityNotFoundException e) {
                        startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse("http://play.google.com/store/apps/details?id=" + MainActivity.this.getPackageName())));
                    }
                }
            });
            builder.setNegativeButton(getString(R.string.rate_never), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    preference.setPrefBoolean(getString(R.string.can_rate), false);
                    Bundle bundle = new Bundle();
                    bundle.putString("action", "never");
                    firebaseAnalytics.logEvent("rating_action", bundle);
                }
            });

            builder.show();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            if (billingClient.isReady()) {
                List<Purchase> purchasesList = billingClient.queryPurchases(BillingClient.SkuType.INAPP).getPurchasesList();
                for (Purchase purchase :
                        purchasesList) {
                    handlePurchase(purchase);
                }
            }
        } catch (Exception e) {
            Log.e("kek", "error on crap", e);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        this.menu = menu;
        getMenuInflater().inflate(R.menu.menu_main, this.menu);
        removeAdsMenuItem = menu.findItem(R.id.action_remove_ads);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_disconnect) {
            DPreference preferences = new DPreference(MainActivity.this, getString(R.string.preference_file_key));
            preferences.removePreference(getString(R.string.preferences_watch_address));
            Intent intent = new Intent(MainActivity.this, ChooseWatchActivity.class);
            startActivity(intent);
            finish();
            Intent serviceKillIntent = new Intent(this, ConnectionService.class);
            stopService(serviceKillIntent);
            return true;
        }

        if (id == R.id.action_remove_ads) {
            Log.i("kek", "remove ads pressed");
            BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                    .setSkuDetails(removeAdsSkuDetails)
                    .build();
            BillingResult billingResult = billingClient.launchBillingFlow(this, flowParams);
        }

        return super.onOptionsItemSelected(item);
    }

    public void disableAds() {
        Log.i("kek", "disableAds()");
        if (removeAdsMenuItem != null) {
            removeAdsMenuItem.setVisible(false);
            removeAdsMenuItem.setEnabled(false);
        }
        DPreference preference = new DPreference(this, getString(R.string.preference_file_key));
        preference.setPrefBoolean(REMOVE_ADS_PURCHASED, true);

        try {
            EventBus.getDefault().post("UPDATE");
        } catch (Exception e) {
            Log.e("kek", "error on crap", e);
        }
    }

    @Override
    public void onPurchasesUpdated(BillingResult billingResult, @Nullable List<Purchase> purchases) {
        Log.d("kek", "onPurchasesUpdated() response: " + billingResult.getResponseCode());
        if (purchases != null) {
            for (Purchase purchase :
                    purchases) {
                handlePurchase(purchase);

            }
        }
    }

    private void handlePurchase(Purchase purchase) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED && purchase.getSku().equals(REMOVE_ADS_SKU)) {
            disableAds();
            Log.i("kek", "purchase is acknowledged: " + purchase.isAcknowledged());
            if (!purchase.isAcknowledged()) {
                AcknowledgePurchaseParams acknowledgePurchaseParams =
                        AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.getPurchaseToken())
                                .build();
                billingClient.acknowledgePurchase(acknowledgePurchaseParams, new AcknowledgePurchaseResponseListener() {
                    @Override
                    public void onAcknowledgePurchaseResponse(BillingResult billingResult) {
                        Log.i("kek", "onAcknowledgePurchaseResponse() result: " + billingResult.getResponseCode() + " " + billingResult.getDebugMessage());
                    }
                });
            }
        }
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING && purchase.getSku().equals(REMOVE_ADS_SKU))
            Toast.makeText(this, "No idea, how you managed to make a pending purchase, I thought it was impossible. I need to know, write me an email or leave a review!", Toast.LENGTH_LONG).show();
    }

    private static final HashMap<String, List<String>> SKUS;

    static {
        SKUS = new HashMap<>();
        SKUS.put(BillingClient.SkuType.INAPP, Arrays.asList("remove_ads"));
    }

    public List<String> getSkus(@BillingClient.SkuType String type) {
        return SKUS.get(type);
    }

    public void querySkuDetailsAsync(@BillingClient.SkuType final String itemType,
                                     final List<String> skuList, final SkuDetailsResponseListener listener) {
        SkuDetailsParams skuDetailsParams = SkuDetailsParams.newBuilder()
                .setSkusList(skuList).setType(itemType).build();
        billingClient.querySkuDetailsAsync(skuDetailsParams,
                new SkuDetailsResponseListener() {
                    @Override
                    public void onSkuDetailsResponse(BillingResult billingResult, List<SkuDetails> skuDetailsList) {
                        listener.onSkuDetailsResponse(billingResult, skuDetailsList);
                    }
                });
    }

    private void handleManagerAndUiReady() {
        List<String> inAppSkus = getSkus(BillingClient.SkuType.INAPP);
        querySkuDetailsAsync(BillingClient.SkuType.INAPP,
                inAppSkus,
                new SkuDetailsResponseListener() {
                    @Override
                    public void onSkuDetailsResponse(BillingResult billingResult, List<SkuDetails> skuDetailsList) {
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                                && skuDetailsList != null) {
                            for (SkuDetails details : skuDetailsList) {
                                Log.w("kek", "Got a SKU: " + details);
                                if (details.getSku().equals(REMOVE_ADS_SKU)) {
                                    DPreference preference = new DPreference(MainActivity.this, getString(R.string.preference_file_key));
                                    boolean adsRemoved = preference.getPrefBoolean(REMOVE_ADS_PURCHASED, false);

                                    if (!adsRemoved) {
                                        removeAdsMenuItem.setVisible(true);
                                        removeAdsMenuItem.setEnabled(true);
                                    }
                                    removeAdsSkuDetails = details;
                                }
                            }
                        }
                    }
                });
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {


        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            if (position == 0) {
                return ControlsFragment.newInstance("1", "2");
            } else
                return AlarmsFragment.newInstance("1", "2");
        }


        @Override
        public int getCount() {
            return 1;
        }
    }
}
