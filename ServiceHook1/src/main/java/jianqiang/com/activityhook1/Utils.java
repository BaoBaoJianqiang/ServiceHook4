package jianqiang.com.activityhook1;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Utils {
    /**
     * 把Assets里面得文件复制到 /data/data/files 目录下
     *
     * @param context
     * @param sourceName
     */
    public static void extractAssets(Context context, String sourceName) {
        AssetManager am = context.getAssets();
        InputStream is = null;
        FileOutputStream fos = null;
        try {
            is = am.open(sourceName);
            File extractFile = context.getFileStreamPath(sourceName);
            fos = new FileOutputStream(extractFile);
            byte[] buffer = new byte[1024];
            int count = 0;
            while ((count = is.read(buffer)) > 0) {
                fos.write(buffer, 0, count);
            }
            fos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeSilently(is);
            closeSilently(fos);
        }

    }

    private static void closeSilently(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Throwable e) {
            // ignore
        }
    }


    /**
     * 读取zip文件中某个文件为字符串，参看自Zeus的PluginUtil
     *
     * @param zipFile     压缩文件
     * @param fileNameReg 需要获取的文件名
     * @return 获取的字符串
     */
    public static String readZipFileString(String zipFile, String fileNameReg) {
        final int BUF_SIZE = 8192;

        String result = null;
        byte[] buffer = new byte[BUF_SIZE];
        InputStream in = null;
        ZipInputStream zipIn = null;
        ByteArrayOutputStream bos = null;
        try {
            File file = new File(zipFile);
            if (!file.exists()) return null;
            in = new FileInputStream(file);
            zipIn = new ZipInputStream(in);
            ZipEntry entry;
            while (null != (entry = zipIn.getNextEntry())) {
                String zipName = entry.getName();
                if (zipName.equals(fileNameReg)) {
                    int bytes;
                    int count = 0;
                    bos = new ByteArrayOutputStream();

                    while ((bytes = zipIn.read(buffer, 0, BUF_SIZE)) != -1) {
                        bos.write(buffer, 0, bytes);
                        count += bytes;
                    }
                    if (count > 0) {
                        result = bos.toString();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                close(in);
                close(zipIn);
                close(bos);
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        return result;
    }


    /**
     * 关闭流
     *
     * @param closeable closeable
     */
    public static void close(Closeable closeable) {
        try {
            if (closeable != null) closeable.close();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
