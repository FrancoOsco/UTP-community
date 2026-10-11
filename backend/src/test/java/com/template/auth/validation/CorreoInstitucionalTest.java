package com.template.auth.validation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CorreoInstitucionalTest {
    @Test void aceptaCorreosInstitucionales() {
        assertTrue(CorreoInstitucional.valido("u22208011@utp.edu.pe"));
        assertTrue(CorreoInstitucional.valido("  Nombre.Apellido@UTP.EDU.PE  "));
        assertEquals("u22208011@utp.edu.pe", CorreoInstitucional.normalizar(" U22208011@UTP.EDU.PE "));
    }
    @Test void rechazaDominiosExternosYSuplantados() {
        assertFalse(CorreoInstitucional.valido("u@gmail.com"));
        assertFalse(CorreoInstitucional.valido("u@utp.edu.pe.otro.com"));
        assertFalse(CorreoInstitucional.valido("u@fakeutp.edu.pe"));
        assertFalse(CorreoInstitucional.valido("@utp.edu.pe"));
    }
}
