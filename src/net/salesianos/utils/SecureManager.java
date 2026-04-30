package net.salesianos.utils;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class SecureManager {

    private Cipher cipher;
    private Cipher decipher;

    public SecureManager() {
        byte[] keyBytes = FileManager.readSecretKey();
        if (keyBytes == null || keyBytes.length == 0) {
            keyBytes = generateAndSaveKey();
        }

        if (keyBytes == null || keyBytes.length == 0) {
            throw new IllegalStateException("No se pudo obtener la clave secreta");
        }

        SecretKey savedKey = new SecretKeySpec(keyBytes, "AES");
        this.initializeCiphers(savedKey);
    }

    public SecureManager(String secretText) {
        byte[] keyBytes = secretText.getBytes();
        while (keyBytes.length < 16) {
            byte[] temp = new byte[keyBytes.length + 1];
            System.arraycopy(keyBytes, 0, temp, 0, keyBytes.length);
            temp[keyBytes.length] = 0;
            keyBytes = temp;
        }
        if (keyBytes.length > 16) {
            byte[] temp = new byte[16];
            System.arraycopy(keyBytes, 0, temp, 0, 16);
            keyBytes = temp;
        }
        SecretKey key = new SecretKeySpec(keyBytes, 0, 16, "AES");
        this.initializeCiphers(key);
    }

    private SecretKey generateKey() {
        KeyGenerator keyGenerator;
        try {
            keyGenerator = KeyGenerator.getInstance("AES");
        } catch (NoSuchAlgorithmException e) {
            System.out.println("No valid algorithm");
            return null;
        }

        keyGenerator.init(128);
        return keyGenerator.generateKey();
    }

    private void initializeCiphers(SecretKey secretKey) {
        try {
            this.cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            this.decipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
        }

        byte[] iv = new byte[16];
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

        try {
            this.cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);
            this.decipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void printMessage(byte[] encryptedMessage) {
        System.out.println("Mensaje encriptado: " + Base64.getEncoder().encodeToString(encryptedMessage));
    }

    public byte[] encript(String newText) {
        byte[] encryptedMessage;

        try {
            encryptedMessage = cipher.doFinal(newText.getBytes());
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            System.out.println("¡Error al encriptar mensaje!");
            return null;
        }

        return encryptedMessage;
    }

    public String decript(byte[] encryptedMessage) {
        byte[] decryptedMessage;

        try {
            decryptedMessage = decipher.doFinal(encryptedMessage);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            System.out.println("¡Error al desencriptar mensaje!");
            return null;
        }

        return new String(decryptedMessage);
    }

    public String encriptString(String newText) {
        byte[] encryptedBytes = encript(newText);
        if (encryptedBytes == null) {
            return null;
        }
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    public String decriptString(String encryptedText) {
        byte[] encryptedBytes = Base64.getDecoder().decode(encryptedText);
        return decript(encryptedBytes);
    }

    public byte[] generateAndSaveKey() {
        SecretKey key = generateKey();
        if (key != null) {
            FileManager.writeSecretKey(key.getEncoded());
            return key.getEncoded();
        }
        return null;
    }
}

