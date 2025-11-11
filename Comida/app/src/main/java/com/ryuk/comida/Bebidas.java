package com.ryuk.comida;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class Bebidas extends AppCompatActivity {

    private ImageView imageViewBebidas;
    private TextView textViewNombreBebidas;
    private Button btnAnteriorBebidas, btnSiguienteBebidas, btnVolverBebidas; 

    
    private int[] imagenesBebidas = {
            R.drawable.batido,
            R.drawable.cafe,
            R.drawable.cerveza,
            R.drawable.limonada
    };

    private String[] nombresBebidas = {
            "Batido de Frutas",
            "Café Premium",
            "Cerveza Artesanal",
            "Limonada Natural"
    };

    private int indiceActual = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bebidas);

        inicializarVistas();
        mostrarBebidaActual();
        configurarBotones();
    }

    private void inicializarVistas() {
        imageViewBebidas = findViewById(R.id.imagenBebidas);
        textViewNombreBebidas = findViewById(R.id.descripcionBebidas);
        btnAnteriorBebidas = findViewById(R.id.btnAnteriorBebidas);
        btnSiguienteBebidas = findViewById(R.id.btnSiguienteBebidas);
        btnVolverBebidas = findViewById(R.id.btnVolverBebidas);
    }

    private void mostrarBebidaActual() {
        imageViewBebidas.setImageResource(imagenesBebidas[indiceActual]);
        textViewNombreBebidas.setText(nombresBebidas[indiceActual]);
    }

    private void configurarBotones() {
        btnAnteriorBebidas.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (indiceActual > 0) {
                    indiceActual--;
                } else {
                    indiceActual = imagenesBebidas.length - 1; 
                }
                mostrarBebidaActual();
            }
        });

        btnSiguienteBebidas.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (indiceActual < imagenesBebidas.length - 1) {
                    indiceActual++;
                } else {
                    indiceActual = 0; 
                }
                mostrarBebidaActual();
            }
        });

        btnVolverBebidas.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); 
            }
        });
    }
}
