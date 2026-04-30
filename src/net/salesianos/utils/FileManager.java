package net.salesianos.utils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileManager {

    public static void writeSecretKey(byte[] encodedKey) {
        try {
            FileOutputStream writer = new FileOutputStream("./secret");
            writer.write(encodedKey);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            System.out.println("Error escribiendo la clave");
        }
    }

    public static byte[] readSecretKey() {
        byte[] encodedKey = null;
        try {
            FileInputStream reader = new FileInputStream("./secret");
            encodedKey = reader.readAllBytes();
            reader.close();
            return encodedKey;
        } catch (IOException e) {
            System.out.println("Error leyendo la clave");
        }

        return encodedKey;
    }
}

