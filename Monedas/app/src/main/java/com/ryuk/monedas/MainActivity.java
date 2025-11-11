package com.ryuk.monedas;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private EditText amountInput;
    private Spinner fromCurrency, toCurrency;
    private TextView resultValue;
    private ImageView resultIcon;
    private Button convertButton, swapButton, openCustomRateButton;

    private Map<String, Double> exchangeRates = new HashMap<>();
    private Map<String, Integer> currencyIcons = new HashMap<>();
    private String[] currencyCodes = {"USD", "EUR", "GBP", "JPY", "MXN", "ARS", "BRL", "BOB"};

    private double customRate = -1; // si es -1 usamos tasas normales

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar vistas
        amountInput = findViewById(R.id.amountInput);
        fromCurrency = findViewById(R.id.fromCurrency);
        toCurrency = findViewById(R.id.toCurrency);
        resultValue = findViewById(R.id.resultValue);
        resultIcon = findViewById(R.id.resultIcon);
        convertButton = findViewById(R.id.convertButton);
        swapButton = findViewById(R.id.swapButton);
        openCustomRateButton = findViewById(R.id.openCustomRateButton);

        setupExchangeRates();
        setupCurrencyIcons();
        setupSpinners();

        convertButton.setOnClickListener(v -> convertCurrency());

        swapButton.setOnClickListener(v -> {
            int fromPos = fromCurrency.getSelectedItemPosition();
            int toPos = toCurrency.getSelectedItemPosition();
            fromCurrency.setSelection(toPos);
            toCurrency.setSelection(fromPos);
            convertCurrency();
        });

        openCustomRateButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, TasaActivity.class);
            startActivityForResult(intent, 100);
        });
    }

    private void setupExchangeRates() {
        exchangeRates.put("USD", 1.0);
        exchangeRates.put("EUR", 0.93);
        exchangeRates.put("GBP", 0.79);
        exchangeRates.put("JPY", 151.50);
        exchangeRates.put("MXN", 16.70);
        exchangeRates.put("ARS", 865.0);
        exchangeRates.put("BRL", 5.05);
        exchangeRates.put("BOB", 6.90);
    }

    private void setupCurrencyIcons() {
        currencyIcons.put("USD", R.drawable.sus);
        currencyIcons.put("EUR", R.drawable.eur);
        currencyIcons.put("GBP", R.drawable.gbp);
        currencyIcons.put("JPY", R.drawable.jpy);
        currencyIcons.put("MXN", R.drawable.mxn);
        currencyIcons.put("ARS", R.drawable.ars);
        currencyIcons.put("BRL", R.drawable.brl);
        currencyIcons.put("BOB", R.drawable.bob);
    }

    private void setupSpinners() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, currencyCodes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        fromCurrency.setAdapter(adapter);
        toCurrency.setAdapter(adapter);

        fromCurrency.setSelection(0);
        toCurrency.setSelection(7);
    }

    private void convertCurrency() {
        String amountStr = amountInput.getText().toString().trim();
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Ingrese un monto", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Monto inválido", Toast.LENGTH_SHORT).show();
            return;
        }

        String fromCode = currencyCodes[fromCurrency.getSelectedItemPosition()];
        String toCode = currencyCodes[toCurrency.getSelectedItemPosition()];

        double result;
        if (customRate > 0) {
            result = amount * customRate;
        } else {
            double fromRate = exchangeRates.get(fromCode);
            double toRate = exchangeRates.get(toCode);
            double amountInUSD = amount / fromRate;
            result = amountInUSD * toRate;
        }

        DecimalFormat df = new DecimalFormat("#,##0.00");
        resultValue.setText(df.format(result));

        Integer iconRes = currencyIcons.get(toCode);
        if (iconRes != null) {
            resultIcon.setImageResource(iconRes);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 && resultCode == RESULT_OK) {
            customRate = data.getDoubleExtra("CUSTOM_RATE", -1);
            if (customRate > 0) {
                Toast.makeText(this, "Tasa personalizada aplicada: " + customRate, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
