package com.demo.app;

import com.demo.utils.DataProcessor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class VulnerableController {

    /**
     * VULNERABLE ENDPOINT - CVE-2015-6420
     *
     * Recibe datos binarios y los deserializa sin validación.
     * Usa DataProcessor del utilities wrapper.
     *
     * Si el payload contiene un gadget chain de Apache Commons Collections,
     * se ejecutará código arbitrario.
     */
    @PostMapping("/deserialize")
    public ResponseEntity<?> deserializeData(@RequestBody byte[] data) {
        try {
            // VULNERABLE: Deserializar sin validar
            // DataProcessor.deserializeData() usa ObjectInputStream sin validación
            Object obj = DataProcessor.deserializeData(data);

            return ResponseEntity.ok("Deserialized successfully: " + obj.getClass().getName());
        } catch (ClassNotFoundException e) {
            return ResponseEntity.status(400).body("Class not found: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error during deserialization: " + e.getMessage());
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok("App is running");
    }

}
