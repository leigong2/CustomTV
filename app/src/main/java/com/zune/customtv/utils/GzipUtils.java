package com.zune.customtv.utils;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class GzipUtils {
    /**
     * 功能:压缩多个文件成一个zip文件
     *
     * @param srcfile：源文件列表
     * @param zipfile：压缩后的文件
     */
    public static void zipFiles(File[] srcfile, File zipfile) {
        byte[] buf = new byte[1024];
        try {
            //ZipOutputStream类：完成文件或文件夹的压缩
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipfile));
            for (int i = 0; i < srcfile.length; i++) {
                FileInputStream in = new FileInputStream(srcfile[i]);
                out.putNextEntry(new ZipEntry(srcfile[i].getName()));
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.closeEntry();
                in.close();
            }
            out.close();
            System.out.println("压缩完成.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 功能:解压缩
     *
     * @param zipfile：需要解压缩的文件
     * @param descDir：解压后的目标目录
     */
    public static void unZipFiles(File zipfile, String descDir) {
        try {
            ZipFile zf = new ZipFile(zipfile);
            for (Enumeration entries = zf.entries(); entries.hasMoreElements(); ) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                String zipEntryName = entry.getName();
                InputStream in = zf.getInputStream(entry);
                OutputStream out = new FileOutputStream(new File(descDir, zipEntryName));
                byte[] buf1 = new byte[1024];
                int len;
                while ( (len = in.read(buf1)) > 0){
                    out.write(buf1, 0, len);
                }
                in.close();
                out.close();
                System.out.println("解压缩完成.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * tar打包，GZip压缩
     *
     * @param file    待压缩的文件或文件夹
     * @param taos    压缩流
     * @param baseDir 相对压缩文件的相对路径
     */
    private static void tarGZip(File file, TarArchiveOutputStream taos, String baseDir) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File f : files) {
                tarGZip(f, taos, baseDir + file.getName() + File.separator);
            }
        } else {
            byte[] buffer = new byte[1024];
            int len = 0;
            FileInputStream fis = null;
            TarArchiveEntry tarArchiveEntry = null;
            try {
                fis = new FileInputStream(file);
                tarArchiveEntry = new TarArchiveEntry(baseDir + file.getName());
                tarArchiveEntry.setSize(file.length());
                taos.putArchiveEntry(tarArchiveEntry);
                while ((len = fis.read(buffer)) != -1) {
                    taos.write(buffer, 0, len);
                }
                taos.flush();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (fis != null) fis.close();
                    if (tarArchiveEntry != null) taos.closeArchiveEntry();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * tar打包，GZip压缩
     *
     * @param srcFile 待压缩的文件或文件夹
     * @param dstDir  压缩至该目录，保持原文件名，后缀改为zip
     */
    public static void tarGZip(File srcFile, String dstDir) {
        File file = new File(dstDir);
        //需要判断该文件存在，且是文件夹
        if (!file.exists() || !file.isDirectory()) file.mkdirs();
        //先打包成tar格式
        String dstTarPath = dstDir + File.separator + srcFile.getName() + ".tar";
        String dstPath = dstTarPath + ".gz";

        FileOutputStream fos = null;
        TarArchiveOutputStream taos = null;
        try {
            fos = new FileOutputStream(dstTarPath);
            taos = new TarArchiveOutputStream(fos);
            tarGZip(srcFile, taos, "");
            taos.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                //关闭数据流的时候要先关闭外层，否则会报Stream Closed的错误
                if (taos != null) taos.close();
                if (fos != null) fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        File tarFile = new File(dstTarPath);
        fos = null;
        GZIPOutputStream gzip = null;
        FileInputStream fis = null;
        try {
            //再压缩成gz格式
            fos = new FileOutputStream(dstPath);
            gzip = new GZIPOutputStream(fos);
            fis = new FileInputStream(tarFile);
            int len = 0;
            byte[] buffer = new byte[1024];
            while ((len = fis.read(buffer)) != -1) {
                gzip.write(buffer, 0, len);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fis != null) fis.close();
                //关闭数据流的时候要先关闭外层，否则会报Stream Closed的错误
                if (gzip != null) gzip.close();
                if (fos != null) fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //删除生成的tar临时文件
        if (tarFile.exists()) tarFile.delete();
    }

    /**
     * GZip解压，tar解包
     *
     * @param srcFile 待压缩的文件或文件夹
     * @param dstDir  压缩至该目录，保持原文件名，后缀改为zip
     */
    public static void untarGZip(File srcFile, String dstDir) {
        File file = new File(dstDir);
        //需要判断该文件存在，且是文件夹
        if (!file.exists() || !file.isDirectory()) file.mkdirs();
        byte[] buffer = new byte[1024];
        FileInputStream fis = null;
        GzipCompressorInputStream gcis = null;
        TarArchiveInputStream tais = null;
        try {
            fis = new FileInputStream(srcFile);
            gcis = new GzipCompressorInputStream(fis);
            tais = new TarArchiveInputStream(gcis);
            TarArchiveEntry tarArchiveEntry;
            int len = 0;
            while ((tarArchiveEntry = tais.getNextTarEntry()) != null) {
                File f = new File(dstDir + File.separator + tarArchiveEntry.getName());
                if (tarArchiveEntry.isDirectory()) f.mkdirs();
                else {
                    File parent = f.getParentFile();
                    if (!parent.exists()) parent.mkdirs();
                    FileOutputStream fos = new FileOutputStream(f);
                    while ((len = tais.read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                    }
                    fos.flush();
                    fos.close();
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(fis != null) fis.close();
                //关闭数据流的时候要先关闭外层，否则会报Stream Closed的错误
                if(tais != null) tais.close();
                if(gcis != null) gcis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void unGzip(File srcFile, String dstDir) {
        InputStream in = null;
        GZIPInputStream gzip = null;
        FileOutputStream fos = null;
        try {
            // 使用默认缓冲区大小创建新的输入流
            in = new FileInputStream(srcFile);
            String name = srcFile.getName();
            File outFile = new File(dstDir, name.replaceAll(".gz", ""));
            fos = new FileOutputStream(outFile);
            gzip = new GZIPInputStream(in);
            byte[] buffer = new byte[1024];
            int n = 0;
            // 将未压缩数据读入字节数组
            while ((n = gzip.read(buffer)) >= 0) {
                fos.write(buffer, 0, n);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (gzip != null) {
                    gzip.close();
                }
                if (fos != null) {
                    fos.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
