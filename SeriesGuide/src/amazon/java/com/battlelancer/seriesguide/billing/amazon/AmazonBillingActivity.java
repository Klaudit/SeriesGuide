/*
 * Copyright 2014 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.battlelancer.seriesguide.billing.amazon;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.amazon.device.iap.PurchasingService;
import com.amazon.device.iap.model.Price;
import com.amazon.device.iap.model.RequestId;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.ui.BaseActivity;
import com.battlelancer.seriesguide.ui.ShowsActivity;
import java.util.Locale;
import timber.log.Timber;

public class AmazonBillingActivity extends BaseActivity {

    @InjectView(R.id.textViewAmazonBillingTitle) TextView title;
    @InjectView(R.id.progressBarAmazonBilling) View progressBar;
    @InjectView(R.id.buttonAmazonBillingSubscribe) Button buttonSubscribe;
    @InjectView(R.id.textViewAmazonBillingPrice) TextView textViewSubPrice;
    @InjectView(R.id.textViewAmazonBillingExisting) TextView textViewSubscribed;
    @InjectView(R.id.buttonAmazonBillingDismiss) Button buttonDismiss;

    private boolean isSubscribed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_amazon_billing);

        setupViews();

        AmazonIapManager.setup(this);
    }

    private void setupViews() {
        ButterKnife.inject(this);

        buttonSubscribe.setEnabled(false);
        buttonSubscribe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                subscribe();
            }
        });

        buttonDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // no need to get product data every time we were hidden, so do it in onStart
        AmazonIapManager.get().requestProductData();
    }

    @Override
    protected void onResume() {
        super.onResume();

        AmazonIapManager.get().activate();
        AmazonIapManager.get().requestUserDataAndPurchaseUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();

        AmazonIapManager.get().deactivate();
    }

    private void subscribe() {
        final RequestId requestId = PurchasingService.purchase(AmazonSku.SERIESGUIDE_SUB.getSku());
        Timber.d("subscribe: requestId (" + requestId + ")");
    }

    private void dismiss() {
        // launch into main activity if user is subscribed
        if (isSubscribed) {
            startActivity(new Intent(this, ShowsActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        }
        // prevent billing from auto-showing on launch again
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putBoolean(AmazonBillingSettings.KEY_BILLING_DISMISSED, true)
                .apply();
        finish();
    }

    public void onEventMainThread(AmazonIapManager.AmazonIapMessageEvent event) {
        Toast.makeText(this, event.messageResId, Toast.LENGTH_LONG).show();
    }

    public void onEventMainThread(AmazonIapManager.AmazonIapAvailabilityEvent event) {
        if (progressBar == null || buttonSubscribe == null || textViewSubscribed == null) {
            return;
        }
        progressBar.setVisibility(View.GONE);

        isSubscribed = event.productAvailable && !event.userCanSubscribe;

        // subscribe button
        buttonSubscribe.setEnabled(event.productAvailable && event.userCanSubscribe);

        // title and status text
        if (!event.productAvailable) {
            title.setText(R.string.trial_title);
            textViewSubscribed.setText(R.string.subscription_not_signed_in);
        } else {
            title.setText(
                    isSubscribed ? R.string.subscription_active : R.string.trial_title);
            textViewSubscribed.setText(
                    isSubscribed ? R.string.subscription_active : R.string.subscription_expired);
        }
    }

    public void onEventMainThread(AmazonIapManager.AmazonIapPriceEvent event) {
        if (textViewSubPrice != null) {
            // display price like "1.23 C"
            Price price = event.product.getPrice();
            String priceString = price.getValue()
                    + " " + price.getCurrency().getSymbol(Locale.getDefault());
            textViewSubPrice.setText(getString(R.string.billing_price_subscribe, priceString));
        }
    }
}
