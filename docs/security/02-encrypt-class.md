[← Volver al README](../../README.md)

# Clase de Encriptación - SecureManager

Se ha implementado un sistema de encriptación y desencriptación basado en el algoritmo **AES (Advanced Encryption Standard)** con modo **CBC (Cipher Block Chaining)** y relleno **PKCS5**. Este sistema proporciona un nivel robusto de seguridad para proteger la información transmitida entre cliente y servidor.

## Algoritmo Criptográfico

- **Algoritmo:** AES/CBC/PKCS5Padding
- **Tamaño de clave:** 128 bits (16 bytes)
- **Modo de operación:** CBC (Cipher Block Chaining)
- **Relleno:** PKCS5Padding
- **Codificación:** Base64 para transmisión segura en texto

### Justificación

Se seleccionó AES debido a:
- Es el estándar actual de encriptación simétrica NIST
- Proporciona seguridad suficiente (128 bits = 2^128 posibilidades)
- CBC proporciona difusión de patrones (no genera patrones en el texto cifrado)
- PKCS5Padding maneja correctamente bloques incompletos
- Base64 permite transmitir datos binarios en formato texto

## Clases Implementadas

### 1. SecureManager

Clase principal que gestiona la encriptación y desencriptación de datos.

#### Constructores

```java
public SecureManager()
```
- Carga la clave desde archivo mediante `FileManager.readSecretKey()`
- Inicializa los ciphers con la clave guardada
- Uso: Para entornos donde la clave ya ha sido generada y guardada

```java
public SecureManager(String secretText)
```
- Acepta una cadena de texto como clave
- Ajusta automáticamente la longitud a 16 bytes (rellenando con ceros si es corta, truncando si es larga)
- Inicializa los ciphers con la clave derivada
- Uso: Para pruebas o distribución de clave entre cliente y servidor

#### Métodos de Encriptación

```java
public byte[] encript(String newText)
```
- Encripta una cadena de texto
- Retorna un arreglo de bytes encriptado
- Retorna `null` si hay error en la encriptación

```java
public String encriptString(String newText)
```
- Encripta una cadena de texto y la codifica en Base64
- Retorna la cadena codificada en Base64
- Útil para transmitir por red o almacenar como texto

#### Métodos de Desencriptación

```java
public String decript(byte[] encryptedMessage)
```
- Desencripta un arreglo de bytes
- Retorna la cadena original
- Retorna `null` si hay error en la desencriptación

```java
public String decriptString(String encryptedText)
```
- Desdecodifica una cadena Base64 y la desencripta
- Retorna la cadena original
- Útil para recibir mensajes encriptados de la red

#### Métodos Auxiliares

```java
public void printMessage(byte[] encryptedMessage)
```
- Imprime un mensaje encriptado en formato Base64
- Útil para debugging

```java
public byte[] generateAndSaveKey()
```
- Genera una nueva clave AES de 128 bits
- La guarda en archivo mediante `FileManager`
- Retorna los bytes de la clave generada
- Uso: Primera ejecución del sistema o regeneración de claves

#### Métodos Privados

```java
private SecretKey generateKey()
```
- Genera una nueva clave AES usando `KeyGenerator`
- Inicializa con 128 bits de longitud
- Retorna la `SecretKey` generada

```java
private void initializeCiphers(SecretKey secretKey)
```
- Inicializa los objetos `Cipher` para encriptación y desencriptación
- Crea un IV (Initialization Vector) con 16 bytes de ceros
- Prepara los ciphers para los modos ENCRYPT_MODE y DECRYPT_MODE

### 2. FileManager

Clase utilitaria para persistencia de claves en el sistema de archivos.

#### Métodos

```java
public static void writeSecretKey(byte[] encodedKey)
```
- Guarda la clave encriptada en el archivo `./secret`
- Ubicación: Raíz del proyecto Java
- Flujo: FileOutputStream → write → flush → close

```java
public static byte[] readSecretKey()
```
- Lee la clave desde el archivo `./secret`
- Retorna un arreglo de bytes con la clave
- Retorna `null` si el archivo no existe o hay error I/O
- Manejo de excepciones: IOException capturada silenciosamente

## Ejemplo de Uso

### Primera ejecución (generar y guardar clave)

```java
SecureManager manager = new SecureManager("DAM2526");
byte[] keyGenerated = manager.generateAndSaveKey();
```

### Encriptación

```java
SecureManager manager = new SecureManager();
String originalMessage = "¡Mensaje confidencial!";
String encryptedMessage = manager.encriptString(originalMessage);
System.out.println("Encriptado: " + encryptedMessage);
```

### Desencriptación

```java
SecureManager manager = new SecureManager();
String encryptedMessage = "base64encodedstring...";
String decryptedMessage = manager.decriptString(encryptedMessage);
System.out.println("Desencriptado: " + decryptedMessage);
```

### Uso con bytes directos

```java
SecureManager manager = new SecureManager();
byte[] encrypted = manager.encript("Texto plano");
manager.printMessage(encrypted);
String decrypted = manager.decript(encrypted);
```

## Flujo de Datos

### Encriptación (Transmisión)
```
Texto plano
    ↓
SecureManager.encriptString()
    ↓
Cipher.doFinal(bytes)
    ↓
Base64.encode()
    ↓
String cifrado (transmitible por red)
```

### Desencriptación (Recepción)
```
String cifrado (recibido de red)
    ↓
Base64.decode()
    ↓
Cipher.doFinal(bytes)
    ↓
SecureManager.decriptString()
    ↓
Texto plano recuperado
```

## Integración con Cliente/Servidor

Las clases `SecureManager` y `FileManager` están listas para ser integradas en:
- `SocketIOHandler`: Para encriptar/desencriptar mensajes antes de enviar
- `Client.java`: Para encriptar datos antes de transmitir
- `Server.java` y `ClientHandler.java`: Para desencriptar datos recibidos

Esta integración será realizada en la Actividad 3 (Modificación de cliente/servidor).

[← Volver al README](../../README.md)
