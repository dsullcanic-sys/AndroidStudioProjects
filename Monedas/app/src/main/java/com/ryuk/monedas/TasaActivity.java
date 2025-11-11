package com.ryuk.monedas;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class TasaActivity extends AppCompatActivity {

    private EditText amountInput, customRateInput;
    private Button applyButton, cancelButton;
    private TextView resultValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tasa);

        amountInput = findViewById(R.id.amountInput);
        customRateInput = findViewById(R.id.customRateInput);
        applyButton = findViewById(R.id.applyButton);
        cancelButton = findViewById(R.id.cancelButton);
        resultValue = findViewById(R.id.resultValue);

        applyButton.setOnClickListener(v -> {
            String amountStr = amountInput.getText().toString().trim();
            String rateStr = customRateInput.getText().toString().trim();

            if (amountStr.isEmpty() || rateStr.isEmpty()) {
                Toast.makeText(this, "Ingrese monto y tasa", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double amount = Double.parseDouble(amountStr);
                double rate = Double.parseDouble(rateStr);
                double result = amount * rate;

                resultValue.setText("Resultado: " + String.format("%.2f", result));

                // también enviamos la tasa de vuelta a MainActivity si se quiere
                Intent resultIntent = new Intent();
                resultIntent.putExtra("CUSTOM_RATE", rate);
                setResult(RESULT_OK, resultIntent);

            } catch (NumberFormatException e) {
                Toast.makeText(this, "Número inválido", Toast.LENGTH_SHORT).show();
            }
        });

        cancelButton.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });
    }
}
