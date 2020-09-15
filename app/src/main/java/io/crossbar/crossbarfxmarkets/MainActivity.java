///////////////////////////////////////////////////////////////////////////
//
//   CrossbarFX Markets
//   Copyright (C) Crossbar.io Technologies GmbH. All rights reserved.
//
///////////////////////////////////////////////////////////////////////////

package io.crossbar.crossbarfxmarkets;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import butterknife.BindView;
import butterknife.ButterKnife;


public class MainActivity extends AppCompatActivity {

    private static final String DELEGATE_ETH_KEY = "d99b5b29e6da2528bf458b26237a6cf8655a3e3276c1cdc0de1f98cefee81c01";
    private static final String MEMBER_ETH_KEY = "2eac15546def97adc6d69ca6e28eec831189baa2533e7910755d15403a0749e8";
    private static final String CS_KEY = "0db085a389c1216ad62b88b408e1d830abca9c9f9dad67eb8c8f8734fe7575eb";

    @BindView(R.id.buttonStartSelling)
    Button mButtonSell;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mButtonSell.setOnClickListener(v -> startSellerService());
    }

    private void startSellerService() {
        Intent intent = new Intent(this, XbrSellerService.class);
        intent.putExtra("uri", "ws://10.0.2.2:8070/ws");
        intent.putExtra("realm", "idma");
        intent.putExtra("delegate_eth_key", DELEGATE_ETH_KEY);
        intent.putExtra("member_eth_key", MEMBER_ETH_KEY);
        intent.putExtra("cryptosign_key", CS_KEY);
        startService(intent);
    }
}
