//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class Disk extends Thread {
    public static final int blockSize = 512;
    private final int trackSize = 10;
    private final int transferTime = 20;
    private final int delayPerTrack = 1;
    private int diskSize;
    private byte[] data;
    private int command;
    private final int IDLE = 0;
    private final int READ = 1;
    private final int WRITE = 2;
    private final int SYNC = 3;
    private boolean readyBuffer;
    private byte[] buffer;
    private int currentBlockId;
    private int targetBlockId;

    public Disk(int var1) {
        this.diskSize = var1 > 0 ? var1 : 1;
        this.data = new byte[this.diskSize * 512];
        this.command = 0;
        this.readyBuffer = false;
        this.buffer = null;
        this.currentBlockId = 0;
        this.targetBlockId = 0;

        try {
            FileInputStream var2 = new FileInputStream("DISK");
            int var3 = var2.available() < this.data.length ? var2.available() : this.data.length;
            var2.read(this.data, 0, var3);
            var2.close();
        } catch (FileNotFoundException var4) {
            SysLib.cerr("threadOS: DISK created\n");
        } catch (IOException var5) {
            SysLib.cerr(var5.toString() + "\n");
        }

    }

    public synchronized boolean read(int var1, byte[] var2) {
        if (var1 >= 0 && var1 <= this.diskSize) {
            if (this.command == 0 && !this.readyBuffer) {
                this.buffer = var2;
                this.targetBlockId = var1;
                this.command = 1;
                this.notify();
                return true;
            } else {
                return false;
            }
        } else {
            SysLib.cerr("threadOS: a wrong blockId for read\n");
            return false;
        }
    }

    public synchronized boolean write(int var1, byte[] var2) {
        if (var1 >= 0 && var1 <= this.diskSize) {
            if (this.command == 0 && !this.readyBuffer) {
                this.buffer = var2;
                this.targetBlockId = var1;
                this.command = 2;
                this.notify();
                return true;
            } else {
                return false;
            }
        } else {
            SysLib.cerr("threadOS: a wrong blockId for write\n");
            return false;
        }
    }

    public synchronized boolean sync() {
        if (this.command == 0 && !this.readyBuffer) {
            this.command = 3;
            this.notify();
            return true;
        } else {
            return false;
        }
    }

    public synchronized boolean testAndResetReady() {
        if (this.command == 0 && this.readyBuffer) {
            this.readyBuffer = false;
            return true;
        } else {
            return false;
        }
    }

    public synchronized boolean testReady() {
        return this.command == 0 && this.readyBuffer;
    }

    private synchronized void waitCommand() {
        for(; this.command == 0; this.readyBuffer = false) {
            try {
                this.wait();
            } catch (InterruptedException var2) {
                SysLib.cerr(var2.toString() + "\n");
            }
        }

    }

    private void seek() {

        int var1 = 20 + 1 * Math.abs(this.targetBlockId / 10 - this.currentBlockId / 10);

        try {
            Thread.sleep((long)var1);
        } catch (InterruptedException var3) {
            SysLib.cerr(var3.toString() + "\n");
        }


        this.currentBlockId = this.targetBlockId;
    }

    private synchronized void finishCommand() {
        this.command = 0;
        this.readyBuffer = true;
        SysLib.disk();
    }

    public void run() {
        while(true) {
            this.waitCommand();
            this.seek();
            switch(this.command) {
                case 1:
                    System.arraycopy(this.data, this.targetBlockId * 512, this.buffer, 0, 512);
                    break;
                case 2:
                    System.arraycopy(this.buffer, 0, this.data, this.targetBlockId * 512, 512);
                    break;
                case 3:
                    try {
                        FileOutputStream var1 = new FileOutputStream("DISK");
                        var1.write(this.data);
                        var1.close();
                    } catch (FileNotFoundException var2) {
                        SysLib.cerr(var2.toString());
                    } catch (IOException var3) {
                        SysLib.cerr(var3.toString());
                    }
            }

            this.finishCommand();
        }
    }
}
