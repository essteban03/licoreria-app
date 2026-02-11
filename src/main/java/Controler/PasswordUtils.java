/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Controler;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordUtils {
    private static final SecureRandom random = new SecureRandom();

    private PasswordUtils() {
    }

  
    public static String generarSalt() {
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * Hashea password usando SHA-256 y un salt Base64.
     * Retorna el hash en Base64.
     */
    public static String hashPassword(String password, String saltBase64) {
        try {
            byte[] saltBytes = Base64.getDecoder().decode(saltBase64);

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(saltBytes);
            byte[] hashed = md.digest(password.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(hashed);

        } catch (Exception e) {
            throw new RuntimeException("Error al hashear password: " + e.getMessage());
        }
    }

  
    public static String generarPasswordSeguro(String password) {
        String salt = generarSalt();
        String hash = hashPassword(password, salt);
        return salt + ":" + hash;
    }

    /**
     * Verifica si el password ingresado coincide con el guardado.
     * El guardado debe ser salt:hash
     */
    public static boolean verificarPassword(String passwordIngresado, String passwordGuardado) {
        if (passwordGuardado == null) return false;

        // Si no contiene ":" entonces es texto plano (usuario viejo)
        if (!passwordGuardado.contains(":")) {
            return passwordIngresado.equals(passwordGuardado);
        }

        String[] parts = passwordGuardado.split(":");
        if (parts.length != 2) return false;

        String salt = parts[0];
        String hashGuardado = parts[1];

        String hashIngresado = hashPassword(passwordIngresado, salt);

        return hashGuardado.equals(hashIngresado);
    }

    /**
     * Detecta si el password ya está hasheado.
     */
    public static boolean esHash(String passwordGuardado) {
        return passwordGuardado != null && passwordGuardado.contains(":");
    }
}
