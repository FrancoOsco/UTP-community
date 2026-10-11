package com.template.auth.validation;
import java.util.Locale;
import java.util.regex.Pattern;
public final class CorreoInstitucional {
    private static final Pattern UTP = Pattern.compile("^[a-zA-Z0-9._%+\\-]+@utp\\.edu\\.pe$", Pattern.CASE_INSENSITIVE);
    private CorreoInstitucional() {}
    public static String normalizar(String correo) { return correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT); }
    public static boolean valido(String correo) { return UTP.matcher(normalizar(correo)).matches(); }
}
