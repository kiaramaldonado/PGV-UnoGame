[← Volver al README](../../README.md)

# Verificación de encriptación con Wireshark

A continuación, se mostrará que el intercambio de información entre cliente y servidor ya no es legible en texto plano, sino que viaja encriptado.

![Wireshark tráfico encriptado](/docs/assets/wireshark-encrypted.png)
En esta captura de Wireshark se observa un nuevo intercambio de mensajes entre cliente y servidor al añadir un jugador nuevo a la sala de espera después de aplicar la encriptación. A diferencia de la captura anterior, el contenido de los paquetes no puede interpretarse directamente, ya que los datos se transmiten cifrados.

Esto implica que, aunque un tercero capture el tráfico, no podrá leer nombres de jugador, cartas, mensajes de chat ni estados de partida sin disponer de la clave de desencriptación guardada en `.secret`.

Observamos que:

- El tráfico TCP contiene datos cifrados.
- No aparecen textos comprensibles con la información del juego.
- Los paquetes muestran una secuencia de bytes o una cadena Base64 sin significado aparente.
- El contenido solo puede recuperarse si cliente y servidor usan la misma clave de encriptación.

## Conclusión
La captura confirma que la comunicación entre ambas aplicaciones está protegida. El tráfico ya no revela información sensible en claro y la encriptación cumple su función correctamente.

[← Volver al README](../../README.md)