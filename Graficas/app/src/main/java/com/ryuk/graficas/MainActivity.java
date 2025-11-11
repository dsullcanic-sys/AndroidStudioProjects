package com.ryuk.graficas;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.database.Cursor;
import android.provider.OpenableColumns;

import androidx.appcompat.app.AppCompatActivity;


import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    Button btnCargarArchivos, btnAbrirClientes, btnAbrirSaldos, btnProfesiones, btnDepartamentos;
    public static ArrayList<Cliente> listaClientes = new ArrayList<>();
    public static ArrayList<Cuenta> listaCuentas = new ArrayList<>();
    public static ArrayList<Departamento> listaDepartamentos = new ArrayList<>();
    public static ArrayList<Profesion> listaProfesiones = new ArrayList<>();
    public static ArrayList<Movimiento> listaMovimientos = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnCargarArchivos = findViewById(R.id.btnCargarArchivos);
        btnAbrirClientes = findViewById(R.id.btnAbrirClientes);
        btnAbrirSaldos = findViewById(R.id.btnAbrirSaldos);
        btnDepartamentos = findViewById(R.id.btnDepartamentos);
        btnProfesiones = findViewById(R.id.btnProfesiones);

        btnCargarArchivos.setOnClickListener(v -> {
            // Selecciona archivos CSV y decide cuál es clientes y cuál es cuentas según el nombre
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("text/csv");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(intent, 101);
        });

        btnAbrirClientes.setOnClickListener(v -> {
            startActivity(new Intent(this, Clientes.class));
        });

        btnAbrirSaldos.setOnClickListener(v -> {
            startActivity(new Intent(this, Saldos.class));
        });

        btnDepartamentos.setOnClickListener(v -> {
            startActivity(new Intent(this, Departamentos.class));
        });
        btnProfesiones.setOnClickListener(v -> {
            startActivity(new Intent(this, Profesiones.class));
        });

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && resultCode == RESULT_OK && data != null) {
            listaClientes.clear();
            listaCuentas.clear();

            if (data.getClipData() != null) {
                int cantidad = data.getClipData().getItemCount();
                for (int i = 0; i < cantidad; i++) {
                    Uri uri = data.getClipData().getItemAt(i).getUri();
                    procesarArchivoCsv(uri);
                }
            } else if (data.getData() != null) {
                procesarArchivoCsv(data.getData());
            }
        }
    }

    // Procesa cada archivo por separado, detecta si es clientes o cuentas
    private void procesarArchivoCsv(Uri archivoUri) {
        String nombreArchivo = getFileNameFromUri(archivoUri);
        try {
            InputStream is = getContentResolver().openInputStream(archivoUri);
            BufferedReader br = new BufferedReader(new InputStreamReader(is, "windows-1252"));

            String cabecera = br.readLine(); // Salta la cabecera

            String linea;
            if (nombreArchivo.toLowerCase().contains("cliente")) {
                while ((linea = br.readLine()) != null) {
                    String[] campos = linea.split(";");
                    if (campos.length >= 4) {
                        // CARNET;NOMBRES;CODPROF;CODEPTO
                        Cliente cl = new Cliente(campos[0], campos[1], campos[2], campos[3]);
                        listaClientes.add(cl);
                    }
                }
            }
            else if (nombreArchivo.toLowerCase().contains("cuenta")) {
                while ((linea = br.readLine()) != null) {
                    String[] campos = linea.split(";");
                    if (campos.length >= 3) {
                        // CUENTA;CARNET;FAPERTURA
                        Cuenta c = new Cuenta(campos[0], campos[1], campos[2]);
                        listaCuentas.add(c);
                    }
                }
            }
            if (nombreArchivo.toLowerCase().contains("departamento")) {
                while ((linea = br.readLine()) != null) {
                    String[] campos = linea.split(";");
                    if (campos.length >= 2) {
                        Departamento d = new Departamento(campos[0], campos[1]);
                        listaDepartamentos.add(d);
                    }
                }
            }
            else if (nombreArchivo.toLowerCase().contains("profesion")) {
                while ((linea = br.readLine()) != null) {
                    String[] campos = linea.split(";");
                    if (campos.length >= 2) {
                        Profesion p = new Profesion(campos[0], campos[1]);
                        listaProfesiones.add(p);
                    }
                }
            }
            else if (nombreArchivo.toLowerCase().contains("movimiento")) {
                while ((linea = br.readLine()) != null) {
                    String[] campos = linea.split(";");
                    if (campos.length >= 3) {
                        Movimiento m = new Movimiento(campos[0], campos[1], campos[2]);
                        listaMovimientos.add(m);
                    }
                }
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Helper para obtener el nombre del archivo desde Uri
    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) result = cursor.getString(idx);
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }

    // Clases internas para compartir datos
    public static class Cliente {
        public String carnet, nombres, codprof, codepto;
        public Cliente(String carnet, String nombres, String codprof, String codepto) {
            this.carnet = carnet;
            this.nombres = nombres;
            this.codprof = codprof;
            this.codepto = codepto;
        }
    }

    public static class Cuenta {
        public String cuenta, carnet, fechaApertura;
        public Cuenta(String cuenta, String carnet, String fechaApertura) {
            this.cuenta = cuenta;
            this.carnet = carnet;
            this.fechaApertura = fechaApertura;
        }
    }
    public static class Departamento {
        public String codepto, descripcion;
        public Departamento(String codepto, String descripcion) {
            this.codepto = codepto;
            this.descripcion = descripcion;
        }
    }
    public static class Profesion {
        public String codprof, descripcion;
        public Profesion(String codprof, String descripcion) {
            this.codprof = codprof;
            this.descripcion = descripcion;
        }
    }
    public static class Movimiento {
        public String cuenta, monto, fecha;
        public Movimiento(String cuenta, String monto, String fecha) {
            this.cuenta = cuenta;
            this.monto = monto;
            this.fecha = fecha;
        }
    }

}
