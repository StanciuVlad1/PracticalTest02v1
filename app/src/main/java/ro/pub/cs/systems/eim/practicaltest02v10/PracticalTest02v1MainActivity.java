package ro.pub.cs.systems.eim.practicaltest02v10;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PracticalTest02v1MainActivity extends AppCompatActivity {
    private final String hostname = "10.0.2.15";
    private EditText port;

    private Button btnStartServer;

    private Button btnConnectClient;

    private TextView textView;

    private OkHttpClient okHttpClient;

    private EditText editText;
    private final BroadcastReceiver autoCompleteReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent ==null) {
                return;
            }
            String results = intent.getStringExtra("resultsAutocomplete");
            if (results != null) {
                Toast.makeText(getApplicationContext(), results, Toast.LENGTH_LONG).show();
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        ContextCompat.registerReceiver(
                this,
                autoCompleteReceiver,
                new IntentFilter("AUTOCOMPLETE_RESULTS"),
                ContextCompat.RECEIVER_NOT_EXPORTED
        );
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(autoCompleteReceiver);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_practical_test02v1_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        port = findViewById(R.id.port);

        btnStartServer = findViewById(R.id.btnStartServer);

        btnConnectClient = findViewById(R.id.btnConnectClient);

        textView = findViewById(R.id.textView);

        editText = findViewById(R.id.insertWord);

        btnStartServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ServerThread serverThread = new ServerThread();
                serverThread.startServer();
            }
        });

        btnConnectClient.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Runnable runnable = new ThreadClient(hostname,Integer.parseInt(port.getText().toString()));
                new Thread(runnable).start();
            }
        });

    }

    private class HttpThread implements Runnable {
        @Override
        public void run() {
            okHttpClient = new OkHttpClient();
            String url = "https://www.google.com/complete/search?client=chrome&q=" + editText.getText().toString();
            Request request = new Request.Builder().url(url).build();
            try {
                Response response = okHttpClient.newCall(request).execute();
                if(response.isSuccessful() && response.body()!=null) {
                    String jsonResponse = response.body().string();
                    JSONArray jsonObject = new JSONArray(jsonResponse);
                    var values = jsonObject.getJSONArray(1);
                    Log.d("TAG", jsonObject.toString());
                    Log.d("TAG",jsonResponse);
                    Log.d("TAG",values.toString());
                    StringBuilder result = new StringBuilder("");
                    for(int i = 0 ; i < values.length(); i++) {
                        Log.d("TAG",values.getString(i));
                        result.append( i + ") " + values.getString(i) + "\n");
                    }
                    Intent intent = new Intent("AUTOCOMPLETE_RESULTS");
                    intent.setPackage(getPackageName());
                    intent.putExtra("resultsAutocomplete", result.toString());
                    sendBroadcast(intent); // din Activity (inner class) merge
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            resultText.setText(result.toString());
//                        }
//                    });
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    }

    class ServerThread extends Thread implements Runnable {
        private boolean serverRunning;
        private ServerSocket serverSocket;

        private int count = 0;

        public void startServer() {
            if(!serverRunning) {
                serverRunning = true;
                start();
            }
        }

        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(Integer.parseInt(port.getText().toString()));

                while(serverRunning) {
                    Socket socket = serverSocket.accept();
                    count++;
                    Log.d("TAG","Client s-a conectat");
                    ;

                    Runnable runnable = new HttpThread();
                    new Thread(runnable).start();


                    PrintWriter output_Server = new PrintWriter(socket.getOutputStream());
                    output_Server.write("Welcome to Server: " + count);
                    output_Server.flush();

                    socket.close();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void stopServer() {
            serverRunning=false;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if(serverSocket!=null) {
                        try {
                            serverSocket.close();
//                            runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    textView.setText("Server stopped");
//                                }
//                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        }
    }
    private class ThreadClient implements  Runnable {
        private String hostname;
        private int port;

        public ThreadClient(String hostname, int port) {
            this.hostname = hostname;
            this.port = port;
        }

        @Override
        public void run() {
            try {
                Socket socket = new Socket(this.hostname, this.port);
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String line = bufferedReader.readLine();
                socket.close();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textView = findViewById(R.id.textView);
                        textView.setText("Connected! The message is : " + line);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}