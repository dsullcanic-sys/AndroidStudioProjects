package com.ryuk.sispreventas;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private Button btnCliente, btnMisPedidos, btnAdmin;

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

        btnCliente = findViewById(R.id.btnCliente);
        btnMisPedidos = findViewById(R.id.btnMisPedidos);
        btnAdmin = findViewById(R.id.btnAdmin);

        btnCliente.setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, ClienteActivity.class);
            startActivity(i);
        });

        btnMisPedidos.setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, MisPedidosActivity.class);
            startActivity(i);
        });

        btnAdmin.setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, AdminActivity.class);
            startActivity(i);
        });
    }
}
