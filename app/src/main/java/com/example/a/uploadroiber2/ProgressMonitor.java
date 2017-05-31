package com.example.a.uploadroiber2;

import android.widget.ProgressBar;
import com.jcraft.jsch.SftpProgressMonitor;

public class ProgressMonitor implements SftpProgressMonitor {

    ProgressBar bar;
    long count;

    public ProgressMonitor(ProgressBar bar){
    this.bar=bar;
    this.count=0;
    }

    public void init(int op, java.lang.String src, java.lang.String dest, long max)
    {
        bar.setMax((int)max);
    }

    public boolean count(long bytes)
    {
        count+=bytes;
        bar.setProgress((int)count);
        return(true);
    }

    public void end()
    {
        bar.setProgress(0);
    }
}
