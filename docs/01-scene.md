[← Volver al README](../README.md)
---

# Escenario Práctico: UNO Game Multijugador

## Descripción del Escenario

La aplicación **UNO Game** es un juego de cartas multijugador que requiere comunicación cliente-servidor para funcionar correctamente. Se trata de un escenario distribuido donde múltiples jugadores, conectados desde diferentes dispositivos o máquinas en la red, juegan simultáneamente una misma partida.

### Justificación de la Necesidad de Comunicación de Red

1. **Separación de Roles Físicos:**
   - Los jugadores están distribuidos en **máquinas diferentes** e idealmente en localizaciones distintas
   - Cada jugador necesita una interfaz gráfica independiente para ver su mano de cartas sin ser visto por otros
   - La información del juego debe sincronizarse constantemente entre todos los participantes

2. **Estado Centralizado:**
   - Es **crítico** que exista una única fuente de verdad (el servidor) para evitar conflictos
   - El servidor valida todas las jugadas antes de aceptarlas

3. **Escalabilidad y Concurrencia:**
   - El servidor debe gestionar **múltiples partidas simultáneas** con diferentes grupos de jugadores
   - Cada partida (GameRoom) es independiente pero comparte infraestructura de red
   - Se necesita threading para que múltiples clientes no se bloqueen entre sí

4. **Comunicación en Tiempo Real:**
   - Las acciones de un jugador (jugar carta, robar, presionar "UNO") deben actualizarse **instantáneamente** en las pantallas de todos los demás
   - Las reglas del UNO exigen turnos ordenados y sincronización precisa

### Ciclo Completo de Comunicación

```
FASE 1: Conexión
├─ Cliente se conecta al servidor (Socket)
└─ Servidor acepta conexión en nueva thread (ClientHandler)

FASE 2: Lobby
├─ Múltiples clientes se unen a la misma sala
├─ Cada jugador marca "Listo"
└─ Servidor sincroniza el estado del lobby a todos

FASE 3: Juego
├─ Servidor inicia la partida cuando condiciones se cumplen (≥2, ≤4 jugadores, todos listos)
├─ Clientes intercambian acciones (PLAY_CARD, DRAW_CARD)
├─ Servidor valida y sincroniza en UPDATE_STATE masivo
└─ Sistema de penalizaciones UNO requiere coordinación instantánea

FASE 4: Finalización
├─ Cliente gana cuando su mano llega a 0 cartas
├─ Servidor notifica a TODOS simultáneamente (GAME_OVER)
└─ Desconexión ordenada con cierre de recursos
```

## Conclusión

El juego UNO es un caso de uso **fundamental** para aprender arquitectura cliente-servidor porque:
- Hay múltiples usuarios conectados simultáneamente
- Cada usuario tiene información privada (su mano de cartas)
- El estado global (turno, dirección, carta visible) debe replicarse a todos
- La validación centralizada es **esencial** para la integridad del juego

Este escenario es completamente **no divisible** sin sacrificar seguridad, funcionalidad o experiencia del usuario.

---
[← Volver al README](../README.md)

