/*
 *   Copyright © 2017-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.java.decryption;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.pspdfkit.document.providers.WritableDataProvider;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * This is a DataProvider that will open and decrypt an AES256-CTR encrypted file on the fly. It
 * won't store decrypted blocks anywhere and allows random seeking to prevent large PDF files from
 * causing OutOfMemoryExceptions.
 *
 * <p>The file itself has a 16-byte IV (also called nonce) prepended before the actual encrypted
 * payload. This serves as the initialization vector for decrypting the first byte. Subsequent bytes
 * use an incremented IV, for example block n uses IV+n.
 */
public class AesDataProvider implements WritableDataProvider, Parcelable {
    public static final Creator<AesDataProvider> CREATOR = new Creator<AesDataProvider>() {
        @Override
        public AesDataProvider createFromParcel(Parcel in) {
            return new AesDataProvider(in);
        }

        @Override
        public AesDataProvider[] newArray(int size) {
            return new AesDataProvider[size];
        }
    };
    private static final String LOG_TAG = "AesProvider";
    private static final int AES_BLOCK_SIZE = 16;
    private static final int IV_SIZE = 16;
    private static final int FILE_SIZE_NOT_SET = -1;

    @NonNull
    private final File encryptedFile;

    @NonNull
    private byte[] encryptedFileKey;

    private long decryptedFileSize = FILE_SIZE_NOT_SET;

    @Nullable
    private byte[] encryptedFileIv = null;

    private File temporaryOutputFile;
    private FileOutputStream fos;
    private CipherOutputStream cos;

    // Per Thread Data - When multi threaded rendering is activated read() can be called from
    // multiple threads at the
    // same time. In order to support this we store everything we need for reading on a per thread
    // basis, otherwise
    // conflicts might lead to an exception being thrown.
    // We don't use ThreadLocal here as we need to be able clean everything from a single thread.
    // This is open file information per thread.
    @NonNull
    private final ConcurrentHashMap<Thread, RandomAccessFile> openFileHandles = new ConcurrentHashMap<>();
    // We also need a Cipher for every thread.
    @NonNull
    private final ConcurrentHashMap<Thread, Cipher> aesCipherMap = new ConcurrentHashMap<>();

    public AesDataProvider(@NonNull String encryptedFilePath, @NonNull String base64Aes256Key) {
        encryptedFile = new File(encryptedFilePath);
        encryptedFileKey = Base64.decode(base64Aes256Key, Base64.DEFAULT);
    }

    /**
     * When parcelling, we only store the filepath and encryption key - everything else gets
     * restored from file.
     *
     * <p>Note: This will hand over the encryption key to the operating system! In order to keep the
     * key secret, you should consider persisting/retrieving it from a reliable source.
     */
    private AesDataProvider(Parcel in) {
        encryptedFile = new File(in.readString());
        encryptedFileKey = in.createByteArray();
    }

    /**
     * In AES-CTR mode, each AES block is encrypted with a key and IV. IV is incremented by number 1
     * for each next block, so to figure out the IV for block N, we need to add N to the initial IV.
     * Only lower four bytes of IV can change like this.
     */
    private static byte[] getIvForBlock(byte[] originalIv, long block) {
        long counter = ((long) originalIv[12] << 24 & 0xFF000000)
                | ((long) originalIv[13] << 16) & 0xFF0000
                | ((long) originalIv[14] << 8) & 0xFF00
                | ((long) originalIv[15] & 0xFF);
        counter += block;

        byte[] iv = Arrays.copyOf(originalIv, 16);
        iv[12] = (byte) ((counter >> 24) & 0xFF);
        iv[13] = (byte) ((counter >> 16) & 0xFF);
        iv[14] = (byte) ((counter >> 8) & 0xFF);
        iv[15] = (byte) (counter & 0xFF);
        return iv;
    }

    /**
     * This opens the file and reads the IV if it wasn't read yet. Every thread calling this gets
     * its own RandomAccessFile.
     */
    private RandomAccessFile openFile() throws IOException {
        if (openFileHandles.containsKey(Thread.currentThread())) {
            return openFileHandles.get(Thread.currentThread());
        } else {
            RandomAccessFile file = new RandomAccessFile(encryptedFile, "r");

            if (encryptedFileIv == null) {
                synchronized (this) {
                    if (encryptedFileIv == null) {
                        // Encrypted file IV is stored at the beginning of the file, read it.
                        encryptedFileIv = new byte[16];
                        file.read(encryptedFileIv, 0, 16);

                        decryptedFileSize = file.length() - IV_SIZE; // Don't take saved IV into account.
                    }
                }
            }

            Log.i(LOG_TAG, "Opened encrypted file " + encryptedFile.getAbsolutePath() + " size " + decryptedFileSize);
            openFileHandles.put(Thread.currentThread(), file);
            return file;
        }
    }

    /** Creates a Cipher set up for decrypting. Every thread calling this gets its own Cipher. */
    private Cipher getCipher() throws IOException {
        if (aesCipherMap.containsKey(Thread.currentThread())) {
            return aesCipherMap.get(Thread.currentThread());
        } else {
            try {
                Cipher aesCipher = Cipher.getInstance("AES/CTR/NoPadding");
                aesCipherMap.put(Thread.currentThread(), aesCipher);
                return aesCipher;
            } catch (GeneralSecurityException e) {
                throw new IOException("This device does not support AES-CTR!");
            }
        }
    }

    @NonNull
    @Override
    public byte[] read(long size, long offset) {
        try {
            // Grab the thread specific data needed for reading.
            RandomAccessFile file = openFile();
            Cipher aesCipher = getCipher();

            // AES is encrypted in 16B blocks which are the minimum we can read. So here we need to
            // figure out which block the offset falls in and start
            // decryption there.
            long block = offset / AES_BLOCK_SIZE;

            // Each block has different IV, so we need to calculate the IV of the first block to
            // start decrypting.
            IvParameterSpec ivParameterSpec = new IvParameterSpec(getIvForBlock(encryptedFileIv, block));
            aesCipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(encryptedFileKey, "AES"), ivParameterSpec);

            // Figure out the location of the encrypted block inside the file - we need to add 16 to
            // offset because we stored IV at the beginning of the file.
            long seekPos = (block * AES_BLOCK_SIZE) + IV_SIZE;
            file.seek(seekPos);

            // Initialize cipher stream from the set file location.
            final CipherInputStream cis = new CipherInputStream(Channels.newInputStream(file.getChannel()), aesCipher);

            // On some devices (primarily Samsung) CipherInputStream implementations suffer from a
            // bug that stops reading early once an internal buffer is hit.
            // Wrapping the cipher stream into a DataInputStream allows to reliably read the entire
            // chunk that was requested.
            DataInputStream input = new DataInputStream(cis);

            // Since we had to start on an AES block boundary, skip bytes which may not align with
            // it.
            int toSkip = (int) (offset % AES_BLOCK_SIZE);
            while (toSkip > 0) {
                toSkip -= input.skip(toSkip);
            }

            // Read and decrypt data into the byte array.
            byte[] decryptedData = new byte[(int) size];
            input.readFully(decryptedData);
            return decryptedData;
        } catch (GeneralSecurityException | IOException e) {
            Log.e(LOG_TAG, "Crypto exception: " + e.getMessage(), e);
            return new byte[0];
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception: " + e.getMessage(), e);
            return new byte[0];
        }
    }

    /** PSPDFKit expects size of decrypted PDF here. */
    @Override
    public long getSize() {
        if (decryptedFileSize == FILE_SIZE_NOT_SET) {
            // Initialize the size the first time it's needed.
            // We do it as late as possible since right after saving the size sometimes isn't
            // updated yet.
            try {
                openFile();
            } catch (IOException e) {
                decryptedFileSize = encryptedFile.length() - IV_SIZE;
            }
        }
        return decryptedFileSize;
    }

    @NonNull
    @Override
    public String getUid() {
        return encryptedFile.getAbsolutePath();
    }

    @Nullable
    @Override
    public String getTitle() {
        return null;
    }

    /** We need to close opened streams here. * */
    @Override
    public void release() {
        try {
            Log.e(LOG_TAG, "Closing file " + encryptedFile.getAbsolutePath());
            closeFiles();
        } catch (IOException ignored) {
        }
    }

    private void closeFiles() throws IOException {
        for (RandomAccessFile file : openFileHandles.values()) {
            file.close();
        }
        openFileHandles.clear();
        aesCipherMap.clear();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        release();
    }

    @Override
    public boolean canWrite() {
        return true;
    }

    @Override
    public boolean startWrite(@NonNull WriteMode writeMode) {
        // We return "supportsAppending()" as "false" so this shouldn't happen.
        if (writeMode == WriteMode.APPEND_TO_FILE)
            throw new IllegalArgumentException("Appending isn't supported by this provider.");

        // We need to save information into a temporary file since input file will probably be read
        // as saving is in progress.
        temporaryOutputFile = new File(encryptedFile.getParent(), "tmp-write.pdf");

        // To keep writing secure we must regenerate IV from random source for each file.
        byte[] outputAesIv = new byte[16];
        SecureRandom rnd = new SecureRandom();
        rnd.nextBytes(outputAesIv);

        try {
            // Write IV at the start of file just like we had at the input.
            fos = new FileOutputStream(temporaryOutputFile);
            fos.write(outputAesIv);

            // Setup encryption - use same key as for the input.
            Cipher c = Cipher.getInstance("AES/CTR/NoPadding");
            IvParameterSpec ivSpec = new IvParameterSpec(outputAesIv);
            SecretKeySpec outputKey = new SecretKeySpec(encryptedFileKey, "AES");
            c.init(Cipher.ENCRYPT_MODE, outputKey, ivSpec);
            cos = new CipherOutputStream(fos, c);
        } catch (IOException | GeneralSecurityException e) {
            Log.e(LOG_TAG, "Failed to open file for writing - " + e.getMessage(), e);
            return false;
        }

        Log.i(
                LOG_TAG,
                "Writing changes to "
                        + encryptedFile.getName()
                        + " to temporary file "
                        + temporaryOutputFile.getAbsolutePath());
        return true;
    }

    @Override
    public boolean write(@NonNull byte[] data) {
        try {
            cos.write(data);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Failed to write encrypted file - " + e.getMessage(), e);
            return false;
        }

        return true;
    }

    @Override
    public boolean finishWrite() {
        // File has finished writing. Now we need to close the input file and replace it with the
        // freshly written temporary file.
        // Any reads after that will expect the new file already.

        Throwable[] closeErrors = new Throwable[3];
        boolean success = true;
        closeErrors[0] = safelyClose(cos);
        closeErrors[1] = safelyClose(fos);

        try {
            closeFiles();
        } catch (Throwable ex) {
            closeErrors[2] = ex;
        }

        for (Throwable e : closeErrors) {
            if (e != null) {
                Log.e(LOG_TAG, "Error while closing output streams - " + e.getMessage(), e);
                success = false;
            }
        }

        // Delete original file
        if (!encryptedFile.delete()) success = false;
        if (!temporaryOutputFile.renameTo(encryptedFile)) success = false;

        if (!success) {
            return false;
        }

        temporaryOutputFile = null;
        cos = null;
        fos = null;
        // We need to reset the iv after writing since we always pick a new one when saving.
        encryptedFileIv = null;

        // We need to update the stored file size now as well.
        decryptedFileSize = encryptedFile.length() - IV_SIZE;
        Log.i(LOG_TAG, "Writing complete, replaced original file with new file of size " + decryptedFileSize);
        return true;
    }

    @Override
    public boolean supportsAppending() {
        // Even though AES-CTR mode allows us to actually append as well, this example will keep it
        // simple and always rewrite the full file on save.
        return false;
    }

    @Nullable
    private Throwable safelyClose(Closeable closeable) {
        try {
            closeable.close();
            return null;
        } catch (Throwable closeError) {
            return closeError;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(encryptedFile.getAbsolutePath());
        dest.writeByteArray(encryptedFileKey);
    }
}
