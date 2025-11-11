package com.ryuk.comida;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    private Button btnPlatos;
    private Button btnBebida;
    private Button btnPostre;
    private Button btnSalir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        inicializarBotones();
        configurarEventosClick();
    }

    private void inicializarBotones(){
        btnPlatos = findViewById(R.id.btnPlatos);
        btnBebida = findViewById(R.id.btnBebidas);
        btnPostre = findViewById(R.id.btnPostres);
        btnSalir = findViewById(R.id.btnSalir);
    }

    private void configurarEventosClick(){
        btnPlatos.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, Platos.class);
            startActivity(intent);
        });

        btnBebida.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, Bebidas.class);
            startActivity(intent);
        });

        btnPostre.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, Postres.class);
            startActivity(intent);
        });

        btnSalir.setOnClickListener(v -> {
            finish();
        });
    }
}