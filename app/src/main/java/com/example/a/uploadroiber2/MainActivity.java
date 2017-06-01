package com.example.a.uploadroiber2;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.kbeanie.multipicker.api.FilePicker;
import com.kbeanie.multipicker.api.Picker;
import com.kbeanie.multipicker.api.callbacks.FilePickerCallback;
import com.kbeanie.multipicker.api.entity.ChosenFile;

import java.util.Calendar;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    FilePicker filePicker;
    ProgressBar mProgress;

    public void executeSSHcommand(String sourceFile) {
        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(Consts.user, Consts.host, 22);
            session.setPassword(Consts.password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            Channel channel = session.openChannel("sftp");
            channel.connect();
            ChannelSftp sftp = (ChannelSftp) channel;
            sftp.cd(Consts.destFolder);

            Calendar c = Calendar.getInstance();
            StringBuilder targetfilename = new StringBuilder();
            targetfilename.append(c.get(Calendar.YEAR));
            targetfilename.append(String.format("%02d", c.get(Calendar.MONTH) + 1));
            targetfilename.append(String.format("%02d", c.get(Calendar.DAY_OF_MONTH)));
            targetfilename.append(String.format("%02d", c.get(Calendar.HOUR_OF_DAY)));
            targetfilename.append(String.format("%02d", c.get(Calendar.MINUTE)));
            targetfilename.append(String.format("%02d", c.get(Calendar.SECOND)));

            String randString = "0123456789abcdef";
            Random r = new Random();
            for (int i = 0; i < 8; i++) {
                targetfilename.append(randString.charAt(r.nextInt(randString.length())));
            }

            String extention = sourceFile.substring(sourceFile.lastIndexOf("."));
            String webAdress = "https://" + Consts.host + "/" + targetfilename + extention;

            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Adresse", webAdress);
            clipboard.setPrimaryClip(clip);

            sftp.put(sourceFile, targetfilename.toString() + extention, new ProgressMonitor(mProgress));
            sftp.disconnect();
            session.disconnect();

        } catch (JSchException e) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this,
                            "error - server not responding",
                            Toast.LENGTH_LONG).show();
                }
            });
        } catch (SftpException e) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this,
                            "error - sftp error",
                            Toast.LENGTH_LONG).show();
                }
            });
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);

        filePicker = new FilePicker(this);
        filePicker.setFilePickerCallback(new FilePickerCallback() {
            @Override
            public void onFilesChosen(List<ChosenFile> files) {
                if (files.size() != 1) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this,
                                    "Max 1 File", Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    final Uri myUri = Uri.parse(files.get(0).getQueryUri());
                    String RealPath = RealPathUtil.getRealPathFromURI(MainActivity.this, myUri);
                    if (RealPath != null && RealPath != "") {
                        new AsyncTask<Integer, Void, Void>() {
                            @Override
                            protected Void doInBackground(Integer... params) {
                                try {
                                    executeSSHcommand(RealPathUtil.getRealPathFromURI(MainActivity.this, myUri));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                return null;
                            }
                        }.execute(1);
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this,
                                        "Dont select images from external storage directly. " +
                                                "Use \"Files\"",
                                        Toast.LENGTH_LONG).show();
                            }
                        });
                    }

                }
            }

            @Override
            public void onError(String message) {
            }
        });

        final Button button = (Button) findViewById(R.id.button);
        //remove ugly AllCAPS SHIT
        button.setTransformationMethod(null);

        mProgress = (ProgressBar) findViewById(R.id.progressBar);

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                filePicker.pickFile();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Picker.PICK_FILE && resultCode == RESULT_OK) {
            filePicker.submit(data);
        }
    }
}
