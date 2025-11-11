package com.ryuk.comida;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class Postres extends AppCompatActivity {

    private ImageView imageViewPostre;
    private TextView textViewNombrePostre;
    private Button btnAnteriorPostres, btnSiguientePostres, btnVolverPostres;

    private int[] imagenesPostres = {
            R.drawable.brownie,
            R.drawable.cheesecake,
            R.drawable.helado
    };

    private String[] nombresPostres = {
            "Brownie de Chocolate",
            "Cheesecake Clásico",
            "Helado Artesanal"
    };

    private int indiceActual = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_postres);

        inicializarVistas();
        mostrarPostreActual();
        configurarBotones();
    }

    private void inicializarVistas() {
        imageViewPostre = findViewById(R.id.imagenPostres);
        textViewNombrePostre = findViewById(R.id.descripcionPostres);
        btnAnteriorPostres = findViewById(R.id.btnAnteriorPostres);
        btnSiguientePostres = findViewById(R.id.btnSiguientePostres);
        btnVolverPostres = findViewById(R.id.btnVolverPostres);
    }

    private void mostrarPostreActual() {
        imageViewPostre.setImageResource(imagenesPostres[indiceActual]);
        textViewNombrePostre.setText(nombresPostres[indiceActual]);
    }

    private void configurarBotones() {
        btnAnteriorPostres.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (indiceActual > 0) {
                    indiceActual--;
                } else {
                    indiceActual = imagenesPostres.length - 1;
                }
                mostrarPostreActual();
            }
        });

        btnSiguientePostres.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (indiceActual < imagenesPostres.length - 1) {
                    indiceActual++;
                } else {
                    indiceActual = 0;
                }
                mostrarPostreActual();
            }
        });

        btnVolverPostres.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}
